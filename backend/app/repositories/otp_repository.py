
from datetime import datetime
from hashlib import sha256
from uuid import UUID

from sqlalchemy import select, text
from sqlalchemy.orm import Session

from app.db.models import PasswordResetChallenge


def normalize_email(email: str) -> str:
    """Normalize email before storing or querying."""
    return email.strip().lower()


def lock_email_scope(db: Session, email: str) -> None:
    """
    Acquire a PostgreSQL transaction-level advisory lock
    for this email to serialize concurrent reset requests.
    """
    normalized_email = normalize_email(email)

    digest = sha256(
        normalized_email.encode("utf-8")
    ).digest()

    lock_key = int.from_bytes(
        digest[:8],
        byteorder="big",
        signed=True,
    )

    db.execute(
        text("SELECT pg_advisory_xact_lock(:lock_key)"),
        {"lock_key": lock_key},
    )


def get_latest_challenge(
    db: Session,
    email: str,
    for_update: bool = False,
) -> PasswordResetChallenge | None:
    """Get the most recently created challenge for an email."""
    statement = (
        select(PasswordResetChallenge)
        .where(
            PasswordResetChallenge.email == normalize_email(email)
        )
        .order_by(
            PasswordResetChallenge.created_at.desc(),
            PasswordResetChallenge.id.desc(),
        )
        .limit(1)
    )

    if for_update:
        statement = statement.with_for_update()

    return db.scalar(statement)


def create_challenge(
    db: Session,
    *,
    firebase_uid: str,
    email: str,
    otp_hash: str,
    otp_expires_at: datetime,
) -> PasswordResetChallenge:
    """
    Create a new OTP challenge.

    The caller must acquire lock_email_scope() first,
    then manage the transaction and commit.
    """
    challenge = PasswordResetChallenge(
        firebase_uid=firebase_uid,
        email=normalize_email(email),
        otp_hash=otp_hash,
        otp_expires_at=otp_expires_at,
        attempt_count=0,
        status="pending",
    )

    db.add(challenge)
    db.flush()

    return challenge


def get_challenge_for_verification(
    db: Session,
    *,
    challenge_id: UUID,
    email: str,
    for_update: bool = True,
) -> PasswordResetChallenge | None:
    """Find a challenge by ID and email."""
    statement = select(PasswordResetChallenge).where(
        PasswordResetChallenge.id == challenge_id,
        PasswordResetChallenge.email == normalize_email(email),
    )

    if for_update:
        statement = statement.with_for_update()

    return db.scalar(statement)


def increment_attempt_count(
    db: Session,
    challenge: PasswordResetChallenge,
    max_attempts: int,
) -> PasswordResetChallenge:
    """
    Record a failed OTP attempt.

    The challenge should already be locked with
    SELECT FOR UPDATE by the caller.
    """
    challenge.attempt_count += 1

    if challenge.attempt_count >= max_attempts:
        challenge.status = "locked"

    db.flush()

    return challenge


def mark_challenge_verified(
    db: Session,
    challenge: PasswordResetChallenge,
    *,
    reset_token_hash: str,
    reset_token_expires_at: datetime,
    verified_at: datetime,
) -> PasswordResetChallenge:
    """Mark OTP as verified and store the reset token hash."""
    challenge.status = "verified"
    challenge.verified_at = verified_at
    challenge.reset_token_hash = reset_token_hash
    challenge.reset_token_expires_at = reset_token_expires_at

    db.flush()

    return challenge


def get_challenge_by_reset_token(
    db: Session,
    reset_token_hash: str,
    for_update: bool = True,
) -> PasswordResetChallenge | None:
    """
    Find a verified, unused challenge by reset token hash.
    """
    statement = select(PasswordResetChallenge).where(
        PasswordResetChallenge.reset_token_hash == reset_token_hash,
        PasswordResetChallenge.status == "verified",
        PasswordResetChallenge.consumed_at.is_(None),
    )

    if for_update:
        statement = statement.with_for_update()

    return db.scalar(statement)



def mark_challenge_consumed(
    db: Session,
    challenge: PasswordResetChallenge,
    consumed_at: datetime,
) -> PasswordResetChallenge:
    """Mark a reset challenge as consumed and erase its token hash."""
    challenge.status = "consumed"
    challenge.consumed_at = consumed_at
    challenge.reset_token_hash = None
    challenge.reset_token_expires_at = None

    db.flush()
    return challenge

def lock_active_challenges(
    db: Session,
    email: str,
) -> int:
    """
    Invalidate previous pending or verified challenges
    before creating a new OTP challenge.

    The caller should acquire lock_email_scope() first.
    """
    statement = select(PasswordResetChallenge).where(
        PasswordResetChallenge.email == normalize_email(email),
        PasswordResetChallenge.status.in_(["pending", "verified"]),
        PasswordResetChallenge.consumed_at.is_(None),
    ).with_for_update()

    challenges = db.scalars(statement).all()

    for challenge in challenges:
        challenge.status = "locked"

    db.flush()

    return len(challenges)

def invalidate_challenge(
    db: Session,
    challenge_id,
) -> bool:
    """
    Invalidate a challenge when email delivery fails
    or when the service needs to cancel a reset request.
    """
    statement = (
        select(PasswordResetChallenge)
        .where(PasswordResetChallenge.id == challenge_id)
        .with_for_update()
    )

    challenge = db.scalar(statement)

    if challenge is None:
        return False

    challenge.status = "locked"
    challenge.reset_token_hash = None
    challenge.reset_token_expires_at = None

    db.flush()
    return True
