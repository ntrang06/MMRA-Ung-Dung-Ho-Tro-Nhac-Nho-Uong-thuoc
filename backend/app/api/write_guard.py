
import logging
from typing import Annotated

from fastapi import Depends, HTTPException
from firebase_admin import firestore
from google.api_core.exceptions import GoogleAPICallError

from app.api.deps import CurrentUser, get_current_user
from app.core.firebase import get_firebase_app


logger = logging.getLogger(__name__)

# Pending: nguoi dung moi gui yeu cau xoa.
# Cac trang thai con lai khong duoc ghi du lieu.
BLOCKED_STATUSES = {
    "confirmed",
    "processing",
    "failed",
    "completed",
}

VALID_STATUSES = {
    "pending",
    "confirmed",
    "processing",
    "failed",
    "completed",
}


def get_deletion_status_reference(uid: str):
    if (
        not isinstance(uid, str)
        or not uid
        or "/" in uid
    ):
        raise ValueError("Invalid Firebase UID")

    client = firestore.client(
        app=get_firebase_app()
    )

    return (
        client.collection("accountDeletionRequests")
        .document(uid)
    )


def account_write_blocked(uid: str) -> bool:
    """
    Kiem tra trang thai xoa tai khoan.

    - Khong co yeu cau: cho phep
    - Pending: cho phep
    - Confirmed/Processing/Failed/Completed: chan

    Khong thuc hien thao tac ghi.
    """
    snapshot = get_deletion_status_reference(
        uid
    ).get()

    if not snapshot.exists:
        return False

    data = snapshot.to_dict() or {}

    if data.get("uid") != uid:
        raise ValueError(
            "Account deletion UID mismatch"
        )

    state = data.get("status")

    if state not in VALID_STATUSES:
        raise ValueError(
            "Invalid account deletion status"
        )

    return state in BLOCKED_STATUSES


def get_writable_user(
    user: Annotated[
        CurrentUser,
        Depends(get_current_user),
    ],
) -> CurrentUser:
    """
    Dependency cho API thay doi du lieu.

    Fail closed: neu khong xac dinh duoc
    trang thai tai khoan thi tu choi ghi.
    """
    try:
        blocked = account_write_blocked(
            user.uid
        )

    except (GoogleAPICallError, ValueError):
        logger.exception(
            "Account deletion write guard failed"
        )

        raise HTTPException(
            status_code=503,
            detail={
                "code": "account_state_unavailable",
                "message": (
                    "Chua kiem tra duoc trang thai "
                    "tai khoan. Vui long thu lai."
                ),
            },
        ) from None

    except Exception:
        logger.exception(
            "Unexpected account write guard error"
        )

        raise HTTPException(
            status_code=503,
            detail={
                "code": "account_state_unavailable",
                "message": (
                    "Tam thoi khong the thay doi "
                    "du lieu tai khoan."
                ),
            },
        ) from None

    if blocked:
        raise HTTPException(
            status_code=409,
            detail={
                "code": "account_deletion_in_progress",
                "message": (
                    "Tai khoan da xac nhan xoa "
                    "hoac dang duoc xu ly xoa. "
                    "Khong the thay doi du lieu."
                ),
            },
        )

    return user


WritableUser = Annotated[
    CurrentUser,
    Depends(get_writable_user),
]
