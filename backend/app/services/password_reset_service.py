import logging
from firebase_admin import auth as firebase_auth_errors
from dataclasses import dataclass
from datetime import datetime, timezone

from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.firebase import get_firebase_auth
from app.core.security import hash_secret
from app.repositories.otp_repository import (
    get_challenge_by_reset_token,
    invalidate_challenge,
    mark_challenge_consumed,
    normalize_email,
)
from app.services.email_service import (
    EmailDeliveryError,
    send_password_reset_otp,
)
from app.services.otp_service import (
    OTPServiceError,
    ResendCooldownError,
    create_otp_challenge,
    verify_otp_code,
)


logger = logging.getLogger(__name__)

GENERIC_REQUEST_MESSAGE = (
    "Nếu email thuộc về một tài khoản hợp lệ, "
    "mã xác minh sẽ được gửi đến hộp thư của bạn."
)


class PasswordResetError(Exception):
    """Base exception for password reset operations."""


class InvalidOTPError(PasswordResetError):
    def __init__(
        self,
        error_code: str = "invalid_otp",
        attempts_remaining: int | None = None,
    ):
        self.error_code = error_code
        self.attempts_remaining = attempts_remaining
        super().__init__("The verification code is invalid or expired.")


class InvalidResetTokenError(PasswordResetError):
    pass


class PasswordPolicyError(PasswordResetError):
    pass


class PasswordResetUnavailableError(PasswordResetError):
    pass


@dataclass(frozen=True)
class PasswordResetRequestResult:
    message: str
    expires_in_seconds: int
    resend_after_seconds: int


@dataclass(frozen=True)
class PasswordResetVerificationResult:
    reset_token: str
    expires_in_seconds: int


@dataclass(frozen=True)
class PasswordResetCompletionResult:
    message: str


def _utc_now() -> datetime:
    return datetime.now(timezone.utc)


def _generic_request_result() -> PasswordResetRequestResult:
    settings = get_settings()

    return PasswordResetRequestResult(
        message=GENERIC_REQUEST_MESSAGE,
        expires_in_seconds=settings.otp_ttl_seconds,
        resend_after_seconds=settings.otp_resend_cooldown_seconds,
    )


def request_password_reset(
    db: Session,
    email: str,
) -> PasswordResetRequestResult:
    """
    Request a password reset by email.

    The response is identical whether the Firebase account exists
    or not, to reduce account enumeration.
    """
    normalized_email = normalize_email(email)

    if not normalized_email:
        return _generic_request_result()

    # Step 1: Find the Firebase account.
    try:
        firebase_auth, firebase_app = get_firebase_auth()

        user = firebase_auth.get_user_by_email(
            normalized_email,
            app=firebase_app,
        )

    except firebase_auth_errors.UserNotFoundError:
        return _generic_request_result()

    except Exception as exc:
        logger.error(
            "Firebase user lookup failed: %s",
            type(exc).__name__,
        )
        raise PasswordResetUnavailableError(
            "Dịch vụ xác thực hiện không khả dụng. Vui lòng thử lại sau."
        ) from None

    # Do not send reset codes to disabled or invalid accounts.
    if user.disabled or not user.email:
        return _generic_request_result()

    # Step 2: Create and persist the OTP challenge.
    try:
        challenge = create_otp_challenge(
            db,
            firebase_uid=user.uid,
            email=normalized_email,
        )

    except ResendCooldownError:
        # Return the same public response.
        return _generic_request_result()

    except Exception as exc:
        logger.error(
            "OTP challenge creation failed: %s",
            type(exc).__name__,
        )
        raise PasswordResetUnavailableError(
            "Không thể tạo yêu cầu xác minh. Vui lòng thử lại sau."
        ) from None

    # Step 3: Send the OTP through SMTP.
    try:
        send_password_reset_otp(
            to_email=normalized_email,
            otp_code=challenge.otp_code,
            expires_in_minutes=5,
        )

    except Exception as exc:
        # The challenge must not remain usable if delivery fails.
        try:
            invalidate_challenge(
                db,
                challenge.challenge_id,
            )
            db.commit()

        except Exception:
            db.rollback()
            logger.error(
                "Failed to invalidate undelivered OTP challenge."
            )

        logger.error(
            "Password reset email delivery failed: %s",
            type(exc).__name__,
        )

        # Do not reveal the email's registration status.
        return _generic_request_result()

    return _generic_request_result()


def verify_password_reset_otp(
    db: Session,
    *,
    email: str,
    otp_code: str,
) -> PasswordResetVerificationResult:
    """
    Verify the six-digit OTP and issue a short-lived reset token.
    """
    normalized_email = normalize_email(email)

    try:
        result = verify_otp_code(
            db,
            email=normalized_email,
            otp_code=otp_code,
        )

    except OTPServiceError:
        raise InvalidOTPError() from None

    except Exception as exc:
        logger.error(
            "OTP verification failed: %s",
            type(exc).__name__,
        )
        raise PasswordResetUnavailableError(
            "Không thể xác minh mã. Vui lòng thử lại."
        ) from None

    if not result.success or not result.reset_token:
        raise InvalidOTPError(
            error_code=result.error_code or "invalid_otp",
            attempts_remaining=result.attempts_remaining,
        )

    settings = get_settings()

    return PasswordResetVerificationResult(
        reset_token=result.reset_token,
        expires_in_seconds=settings.reset_token_ttl_seconds,
    )


def complete_password_reset(
    db: Session,
    *,
    reset_token: str,
    new_password: str,
) -> PasswordResetCompletionResult:
    """
    Consume the reset token and update the password in Firebase Auth.

    The reset token is consumed before the external Firebase update.
    If Firebase fails, the user must request a new OTP.
    """
    settings = get_settings()

    if not isinstance(new_password, str):
        raise PasswordPolicyError(
            "Mật khẩu mới không hợp lệ."
        )

    if len(new_password) < 8:
        raise PasswordPolicyError(
            "Mật khẩu phải có ít nhất 8 ký tự."
        )

    if len(new_password) > 128:
        raise PasswordPolicyError(
            "Mật khẩu không được vượt quá 128 ký tự."
        )

    if not new_password.strip():
        raise PasswordPolicyError(
            "Mật khẩu không được để trống."
        )

    token_hash = hash_secret(
        reset_token,
        settings.reset_token_pepper,
    )

    # Step 1: Lock and validate the reset challenge.
    try:
        challenge = get_challenge_by_reset_token(
            db,
            token_hash,
            for_update=True,
        )

        if challenge is None:
            db.rollback()
            raise InvalidResetTokenError(
                "Invalid or already used reset token."
            )

        now = _utc_now()
        token_expires_at = challenge.reset_token_expires_at

        if (
            token_expires_at is None
            or token_expires_at.tzinfo is None
            or now >= token_expires_at
        ):
            challenge.status = "locked"
            challenge.reset_token_hash = None
            challenge.reset_token_expires_at = None

            db.commit()

            raise InvalidResetTokenError(
                "The reset token has expired."
            )

        uid = challenge.firebase_uid
        expected_email = normalize_email(challenge.email)

        # Step 2: Verify that the Firebase account still matches.
        firebase_auth, firebase_app = get_firebase_auth()

        try:
            user = firebase_auth.get_user(
                uid,
                app=firebase_app,
            )

        except firebase_auth_errors.UserNotFoundError:
            challenge.status = "locked"
            challenge.reset_token_hash = None
            challenge.reset_token_expires_at = None

            db.commit()

            raise InvalidResetTokenError(
                "The account is no longer available."
            ) from None

        if (
            user.disabled
            or not user.email
            or normalize_email(user.email) != expected_email
        ):
            challenge.status = "locked"
            challenge.reset_token_hash = None
            challenge.reset_token_expires_at = None

            db.commit()

            raise InvalidResetTokenError(
                "The account no longer matches this reset request."
            )

        # Step 3: Consume the token before changing the password.
        mark_challenge_consumed(
            db,
            challenge,
            consumed_at=now,
        )

        db.commit()

    except PasswordResetError:
        if db.in_transaction():
            db.rollback()
        raise

    except Exception as exc:
        db.rollback()

        logger.error(
            "Reset token validation failed: %s",
            type(exc).__name__,
        )

        raise PasswordResetUnavailableError(
            "Không thể xử lý yêu cầu đặt lại mật khẩu."
        ) from None

    # Step 4: Update the real Firebase Authentication password.
    try:
        firebase_auth.update_user(
            uid,
            password=new_password,
            app=firebase_app,
        )

    except Exception as exc:
        logger.error(
            "Firebase password update failed: %s",
            type(exc).__name__,
        )

        # The reset token was already consumed.
        # The user must request a new OTP.
        raise PasswordResetUnavailableError(
            "Không thể cập nhật mật khẩu. "
            "Vui lòng yêu cầu mã xác minh mới."
        ) from None

    # Step 5: Revoke refresh tokens on other devices.
    try:
        firebase_auth.revoke_refresh_tokens(
            uid,
            app=firebase_app,
        )

    except Exception as exc:
        logger.error(
            "Firebase refresh token revocation failed: %s",
            type(exc).__name__,
        )

    return PasswordResetCompletionResult(
        message=(
            "Mật khẩu đã được cập nhật thành công. "
            "Vui lòng đăng nhập lại."
        )
    )
