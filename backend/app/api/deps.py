
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
    is_admin: bool = False


def unauthorized() -> HTTPException:
    return HTTPException(
        status_code=401,
        detail={
            "code": "unauthenticated",
            "message": "Vui lòng đăng nhập lại.",
        },
        headers={
            "WWW-Authenticate": "Bearer",
        },
    )


def forbidden() -> HTTPException:
    return HTTPException(
        status_code=403,
        detail={
            "code": "permission_denied",
            "message": (
                "Bạn không có quyền thực hiện "
                "thao tác này."
            ),
        },
    )


def get_current_user(
    credentials: Annotated[
        HTTPAuthorizationCredentials | None,
        Depends(bearer),
    ],
) -> CurrentUser:
    # Không cho phép truy cập nếu thiếu Firebase ID Token.
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
        # Firebase xác minh chữ ký, thời hạn token,
        # tài khoản bị vô hiệu hóa và token bị thu hồi.
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
        # Không ghi ID Token vào log.
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

    # Chỉ tin tưởng custom claim đã được Firebase
    # xác minh trong ID Token.
    #
    # Không đọc quyền Admin từ dữ liệu do Android
    # hoặc Web gửi lên.
    #
    # Chỉ chấp nhận giá trị boolean True.
    is_admin = claims.get("admin") is True

    return CurrentUser(
        uid=uid,
        is_admin=is_admin,
    )


def get_admin_user(
    user: Annotated[
        CurrentUser,
        Depends(get_current_user),
    ],
) -> CurrentUser:
    # Backend kiểm tra quyền Admin,
    # không phụ thuộc kiểm tra trên giao diện.
    if not user.is_admin:
        raise forbidden()

    return user


# Dependency cho tất cả API yêu cầu đăng nhập.
AuthenticatedUser = Annotated[
    CurrentUser,
    Depends(get_current_user),
]


# Dependency chỉ dành cho các API Admin.
AdminUser = Annotated[
    CurrentUser,
    Depends(get_admin_user),
]
