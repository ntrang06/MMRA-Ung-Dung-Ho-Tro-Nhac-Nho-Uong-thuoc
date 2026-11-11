import logging
from dataclasses import dataclass
from typing import Annotated

from fastapi import Depends, HTTPException
from fastapi.security import (
    HTTPAuthorizationCredentials,
    HTTPBearer,
)
from firebase_admin import auth

from app.core.firebase import get_firebase_app


logger = logging.getLogger(__name__)

bearer = HTTPBearer(auto_error=False)


@dataclass(frozen=True)
class CurrentUser:
    uid: str


def unauthorized() -> HTTPException:
    return HTTPException(
        status_code=401,
        detail={
            "code": "unauthenticated",
            "message": "Vui lòng đăng nhập lại.",
        },
        headers={"WWW-Authenticate": "Bearer"},
    )


def get_current_user(
    credentials: Annotated[
        HTTPAuthorizationCredentials | None,
        Depends(bearer),
    ],
) -> CurrentUser:
    if (
        credentials is None
        or credentials.scheme.lower() != "bearer"
    ):
        raise unauthorized()

    try:
        firebase_app = get_firebase_app()

    except Exception as error:
        logger.error(
            "Firebase initialization failed: %s",
            type(error).__name__,
        )

        raise HTTPException(
            status_code=503,
            detail={
                "code": "auth_unavailable",
                "message": (
                    "Chưa xác thực được. "
                    "Vui lòng thử lại."
                ),
            },
        ) from None

    try:
        # Xác minh Firebase ID token.
        # Đồng thời kiểm tra token bị thu hồi,
        # tài khoản bị khóa hoặc đã bị xóa.
        claims = auth.verify_id_token(
            credentials.credentials,
            app=firebase_app,
            check_revoked=True,
        )

    except (
        auth.InvalidIdTokenError,
        auth.RevokedIdTokenError,
        auth.UserDisabledError,
        auth.UserNotFoundError,
        ValueError,
    ):
        raise unauthorized() from None

    except Exception as error:
        # Chỉ ghi loại lỗi, không ghi token.
        logger.error(
            "Firebase verification unavailable: %s",
            type(error).__name__,
        )

        raise HTTPException(
            status_code=503,
            detail={
                "code": "auth_unavailable",
                "message": (
                    "Chưa xác thực được. "
                    "Vui lòng thử lại."
                ),
            },
        ) from None

    uid = claims.get("uid")

    if (
        not isinstance(uid, str)
        or not uid
        or "/" in uid
    ):
        raise unauthorized()

    return CurrentUser(uid=uid)


AuthenticatedUser = Annotated[
    CurrentUser,
    Depends(get_current_user),
]