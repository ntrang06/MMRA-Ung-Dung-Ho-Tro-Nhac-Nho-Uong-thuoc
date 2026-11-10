import logging

from fastapi import APIRouter, Depends, HTTPException, Response, status
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.schemas.password_reset import (
    PasswordResetRequest,
    PasswordResetVerifyRequest,
    PasswordResetCompleteRequest,
    PasswordResetRequestResponse,
    PasswordResetVerifyResponse,
    PasswordResetCompleteResponse,
)
from app.services.password_reset_service import (
    request_password_reset,
    verify_password_reset_otp,
    complete_password_reset,
    InvalidOTPError,
    InvalidResetTokenError,
    PasswordPolicyError,
    PasswordResetUnavailableError,
)


logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/auth/password-reset",
    tags=["Password Reset"],
)


@router.post(
    "/request",
    response_model=PasswordResetRequestResponse,
    status_code=status.HTTP_202_ACCEPTED,
    summary="Request password reset OTP",
)
def request_reset(
    payload: PasswordResetRequest,
    db: Session = Depends(get_db),
):
    """
    Request an OTP through email.

    The response does not reveal whether the account exists.
    """

    try:
        result = request_password_reset(
            db,
            str(payload.email),
        )

        return PasswordResetRequestResponse(
            message=result.message,
            expires_in_seconds=result.expires_in_seconds,
            resend_after_seconds=result.resend_after_seconds,
        )

    except PasswordResetUnavailableError:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Dịch vụ hiện không khả dụng. Vui lòng thử lại sau.",
        ) from None

    except Exception as exc:
        logger.error(
            "Password reset request endpoint failed: %s",
            type(exc).__name__,
        )

        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Đã xảy ra lỗi. Vui lòng thử lại sau.",
        ) from None


@router.post(
    "/verify",
    response_model=PasswordResetVerifyResponse,
    status_code=status.HTTP_200_OK,
    summary="Verify password reset OTP",
)
def verify_reset_otp(
    payload: PasswordResetVerifyRequest,
    response: Response,
    db: Session = Depends(get_db),
):
    """
    Verify OTP and return a short-lived reset token.
    """

    response.headers["Cache-Control"] = "no-store"
    response.headers["Pragma"] = "no-cache"

    try:
        result = verify_password_reset_otp(
            db,
            email=str(payload.email),
            otp_code=payload.otp_code.get_secret_value(),
        )

        return PasswordResetVerifyResponse(
            reset_token=result.reset_token,
            expires_in_seconds=result.expires_in_seconds,
        )

    except InvalidOTPError as exc:
        detail = {
            "code": exc.error_code,
            "message": "Mã xác minh không hợp lệ hoặc đã hết hạn.",
        }

        if exc.attempts_remaining is not None:
            detail["attempts_remaining"] = exc.attempts_remaining

        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=detail,
        ) from None

    except PasswordResetUnavailableError:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Không thể xác minh mã lúc này. Vui lòng thử lại.",
        ) from None

    except Exception as exc:
        logger.error(
            "OTP verification endpoint failed: %s",
            type(exc).__name__,
        )

        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Đã xảy ra lỗi khi xác minh mã.",
        ) from None


@router.post(
    "/complete",
    response_model=PasswordResetCompleteResponse,
    status_code=status.HTTP_200_OK,
    summary="Complete password reset",
)
def complete_reset(
    payload: PasswordResetCompleteRequest,
    response: Response,
    db: Session = Depends(get_db),
):
    """
    Update Firebase Authentication password
    using a verified reset token.
    """

    response.headers["Cache-Control"] = "no-store"
    response.headers["Pragma"] = "no-cache"

    try:
        result = complete_password_reset(
            db,
            reset_token=payload.reset_token.get_secret_value(),
            new_password=payload.new_password.get_secret_value(),
        )

        return PasswordResetCompleteResponse(
            message=result.message,
        )

    except InvalidResetTokenError:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Mã đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.",
        ) from None

    except PasswordPolicyError as exc:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=str(exc),
        ) from None

    except PasswordResetUnavailableError:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Không thể cập nhật mật khẩu. Vui lòng thử lại.",
        ) from None

    except Exception as exc:
        logger.error(
            "Password reset completion endpoint failed: %s",
            type(exc).__name__,
        )

        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Đã xảy ra lỗi khi cập nhật mật khẩu.",
        ) from None
