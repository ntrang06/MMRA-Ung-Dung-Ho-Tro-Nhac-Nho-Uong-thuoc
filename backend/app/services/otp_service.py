from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from math import ceil
from re import fullmatch
from uuid import UUID

from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.security import (
    generate_otp,
    generate_reset_token,
    hash_secret,
    verify_secret,
)
from app.repositories.otp_repository import (
    create_challenge,
    get_challenge_for_verification,
    get_latest_challenge,
    increment_attempt_count,
    lock_active_challenges,
    lock_email_scope,
    mark_challenge_verified,
    normalize_email,
)


class OTPServiceError(Exception):
    """Base exception for OTP business errors."""


class ResendCooldownError(OTPServiceError):
    def __init__(self, retry_after: int):
        self.retry_after = retry_after
        super().__init__(
            f"Please wait {retry_after} seconds before requesting another code."
        )


@dataclass(frozen=True)
class OTPChallengeCreated:
    challenge_id: UUID
    otp_code: str
    expires_at: datetime


@dataclass(frozen=True)
class OTPVerificationResult:
    success: bool
    reset_token: str | None = None
    error_code: str | None = None
    attempts_remaining: int | None = None


def _utc_now() -> datetime:
    return datetime.now(timezone.utc)


def _ensure_utc(value: datetime) -> datetime:
    """Normalize database timestamps to timezone-aware UTC."""
    if value.tzinfo is None:
        return value.replace(tzinfo=timezone.utc)

    return value.astimezone(timezone.utc)


def _otp_digest(email: str, otp: str, pepper: str) -> str:
    """
    Bind the OTP hash to the normalized email.

    The original OTP is never stored in PostgreSQL.
    """
    normalized_email = normalize_email(email)

    return hash_secret(
        f"{normalized_email}:{otp}",
        pepper,
    )

def create_otp_challenge(
    db: Session,
    *,
    firebase_uid: str,
    email: str,
) -> OTPChallengeCreated:
    """
    Create a new OTP challenge.

    The caller is responsible for sending the returned OTP
    through the email service. Never return otp_code through
    a public API response.
    """
    settings = get_settings()
    normalized_email = normalize_email(email)

    if not firebase_uid.strip():
        raise ValueError("Firebase UID is required.")

    if not normalized_email:
        raise ValueError("Email is required.")

    now = _utc_now()

    try:
        # Serialize requests for the same email.
        lock_email_scope(db, normalized_email)

        latest = get_latest_challenge(
            db,
            normalized_email,
            for_update=True,
        )

        # Enforce resend cooldown.
        if latest is not None and latest.created_at is not None:
            created_at = _ensure_utc(latest.created_at)

            elapsed = (now - created_at).total_seconds()

            remaining = (
                settings.otp_resend_cooldown_seconds - elapsed
            )

            if remaining > 0:
                raise ResendCooldownError(ceil(remaining))

        # Invalidate previous pending or verified challenges.
        lock_active_challenges(db, normalized_email)

        otp_code = generate_otp()

        otp_hash = _otp_digest(
            normalized_email,
            otp_code,
            settings.otp_pepper,
        )

        expires_at = now + timedelta(
            seconds=settings.otp_ttl_seconds
        )

        challenge = create_challenge(
            db,
            firebase_uid=firebase_uid,
            email=normalized_email,
            otp_hash=otp_hash,
            otp_expires_at=expires_at,
        )

        db.commit()

        return OTPChallengeCreated(
            challenge_id=challenge.id,
            otp_code=otp_code,
            expires_at=expires_at,
        )

    except Exception:
        db.rollback()
        raise

def verify_otp_code(
    db: Session,
    *,
    email: str,
    otp_code: str,
) -> OTPVerificationResult:
    """
    Verify the latest OTP challenge for an email.

    The database challenge ID is kept internal and is
    not exposed to the Android client.
    """
    settings = get_settings()
    normalized_email = normalize_email(email)

    try:
        lock_email_scope(db, normalized_email)

        challenge = get_latest_challenge(
            db,
            normalized_email,
            for_update=True,
        )

        if challenge is None or challenge.status != "pending":
            db.commit()
            return OTPVerificationResult(
                success=False,
                error_code="invalid_otp",
            )

        now = _utc_now()
        expires_at = _ensure_utc(challenge.otp_expires_at)

        if now >= expires_at:
            challenge.status = "locked"
            db.commit()

            return OTPVerificationResult(
                success=False,
                error_code="otp_expired",
            )

        valid_format = (
            isinstance(otp_code, str)
            and fullmatch(r"\d{6}", otp_code) is not None
        )

        is_valid = valid_format and verify_secret(
            f"{normalized_email}:{otp_code}",
            challenge.otp_hash,
            settings.otp_pepper,
        )

        if not is_valid:
            increment_attempt_count(
                db,
                challenge,
                settings.otp_max_attempts,
            )

            attempts_remaining = max(
                0,
                settings.otp_max_attempts - challenge.attempt_count,
            )

            db.commit()

            return OTPVerificationResult(
                success=False,
                error_code="invalid_otp",
                attempts_remaining=attempts_remaining,
            )

        reset_token = generate_reset_token()

        reset_token_hash = hash_secret(
            reset_token,
            settings.reset_token_pepper,
        )

        reset_token_expires_at = now + timedelta(
            seconds=settings.reset_token_ttl_seconds
        )

        mark_challenge_verified(
            db,
            challenge,
            reset_token_hash=reset_token_hash,
            reset_token_expires_at=reset_token_expires_at,
            verified_at=now,
        )

        db.commit()

        return OTPVerificationResult(
            success=True,
            reset_token=reset_token,
        )

    except Exception:
        db.rollback()
        raise
