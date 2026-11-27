
import logging
from datetime import datetime, timezone

from fastapi import APIRouter, HTTPException, Response
from firebase_admin import firestore
from google.api_core.exceptions import (
    AlreadyExists,
    GoogleAPICallError,
)
from pydantic import BaseModel, ConfigDict

from app.api.deps import AuthenticatedUser
from app.core.firebase import get_firebase_app


logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/auth/account-deletion",
    tags=["Account Deletion"],
)


class AccountDeletionResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    status: str
    requestedAt: datetime


def get_deletion_collection():
    client = firestore.client(
        app=get_firebase_app()
    )

    return client.collection(
        "accountDeletionRequests"
    )


def deletion_unavailable() -> HTTPException:
    logger.exception(
        "Account deletion request unavailable"
    )

    return HTTPException(
        status_code=503,
        detail={
            "code": "deletion_unavailable",
            "message": (
                "Chưa ghi nhận được yêu cầu. "
                "Vui lòng thử lại."
            ),
        },
    )


def validate_request_snapshot(
    snapshot,
    uid: str,
) -> AccountDeletionResponse:
    if not snapshot.exists:
        raise deletion_unavailable()

    data = snapshot.to_dict() or {}

    if data.get("uid") != uid:
        raise deletion_unavailable()

    requested_at = data.get("requestedAt")

    if (
        not isinstance(requested_at, datetime)
        or requested_at.tzinfo is None
    ):
        raise deletion_unavailable()

    state = data.get("status")

    if state not in {
        "pending",
        "processing",
        "failed",
        "completed",
    }:
        raise deletion_unavailable()

    return AccountDeletionResponse(
        status=state,
        requestedAt=requested_at,
    )


@router.post(
    "/requests",
    response_model=AccountDeletionResponse,
    status_code=202,
)
def request_account_deletion(
    user: AuthenticatedUser,
    response: Response,
):
    """
    Tiếp nhận yêu cầu xóa tài khoản.

    Chưa xóa dữ liệu ngay tại endpoint này.
    Giai đoạn xử lý xóa được triển khai riêng.
    """
    response.headers["Cache-Control"] = "no-store"

    now = datetime.now(timezone.utc)

    try:
        reference = get_deletion_collection().document(
            user.uid
        )

        try:
            reference.create({
                "uid": user.uid,
                "status": "pending",
                "requestedAt": now,
                "updatedAt": now,
            })

            return AccountDeletionResponse(
                status="pending",
                requestedAt=now,
            )

        except AlreadyExists:
            # Gọi lại không tạo yêu cầu trùng.
            snapshot = reference.get()

            return validate_request_snapshot(
                snapshot,
                user.uid,
            )

    except GoogleAPICallError:
        raise deletion_unavailable() from None


@router.get(
    "/requests/me",
    response_model=AccountDeletionResponse,
)
def get_my_deletion_request(
    user: AuthenticatedUser,
    response: Response,
):
    response.headers["Cache-Control"] = "no-store"

    try:
        reference = get_deletion_collection().document(
            user.uid
        )

        snapshot = reference.get()

    except GoogleAPICallError:
        raise deletion_unavailable() from None

    if not snapshot.exists:
        raise HTTPException(
            status_code=404,
            detail={
                "code": "deletion_request_not_found",
                "message": (
                    "Chưa có yêu cầu xóa tài khoản."
                ),
            },
        )

    return validate_request_snapshot(
        snapshot,
        user.uid,
    )
