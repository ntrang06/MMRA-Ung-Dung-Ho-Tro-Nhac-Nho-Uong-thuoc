
import logging
from datetime import datetime, timezone

from fastapi import (
    APIRouter,
    Depends,
    HTTPException,
    Response,
)
from firebase_admin import firestore
from google.api_core.exceptions import (
    AlreadyExists,
    GoogleAPICallError,
)
from pydantic import BaseModel, ConfigDict
from sqlalchemy.orm import Session

from app.api.deps import AuthenticatedUser
from app.core.firebase import get_firebase_app
from app.db.session import get_db
from app.services.account_deletion_inventory import (
    InventoryLimitExceeded,
    build_account_deletion_inventory,
)


logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/auth/account-deletion",
    tags=["Account Deletion"],
)


# ============================================================
# 1. RESPONSE SCHEMAS
# ============================================================

class AccountDeletionResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    status: str
    requestedAt: datetime


class AccountDeletionPreviewResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    userDocuments: int
    familyLinks: int
    familyInvitations: int
    passwordResetRecords: int

    deletionExecuted: bool = False


# ============================================================
# 2. HELPERS
# ============================================================

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


# ============================================================
# 3. POST - GỬI YÊU CẦU XÓA TÀI KHOẢN
# ============================================================

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

    Chỉ ghi nhận yêu cầu.
    Chưa thực hiện xóa dữ liệu hoặc Firebase Auth.
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
            # Gửi lại yêu cầu không tạo bản ghi trùng.
            snapshot = reference.get()

            return validate_request_snapshot(
                snapshot,
                user.uid,
            )

    except GoogleAPICallError:
        raise deletion_unavailable() from None


# ============================================================
# 4. GET - KIỂM TRA TRẠNG THÁI YÊU CẦU
# ============================================================

@router.get(
    "/requests/me",
    response_model=AccountDeletionResponse,
)
def get_my_deletion_request(
    user: AuthenticatedUser,
    response: Response,
):
    """
    Chỉ xem yêu cầu xóa của tài khoản
    đã xác thực bằng Firebase ID Token.
    """
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


# ============================================================
# 5. GET - XEM TRƯỚC DỮ LIỆU CẦN XÓA
# ============================================================

@router.get(
    "/preview",
    response_model=AccountDeletionPreviewResponse,
)
def preview_account_deletion(
    user: AuthenticatedUser,
    response: Response,
    db: Session = Depends(get_db),
):
    """
    Kiểm kê dữ liệu liên quan đến tài khoản.

    Bao gồm:
    - Firestore users/{uid} và các subcollection
    - Firestore familyLinks
    - Firestore familyInvitations
    - PostgreSQL password_reset_challenges

    Chỉ đọc và đếm dữ liệu.
    Không thực hiện thao tác xóa.
    """
    response.headers["Cache-Control"] = "no-store"

    try:
        inventory = build_account_deletion_inventory(
            user.uid,
            db,
        )

    except InventoryLimitExceeded:
        raise HTTPException(
            status_code=409,
            detail={
                "code": "inventory_too_large",
                "message": (
                    "Không thể kiểm kê đầy đủ dữ liệu "
                    "trong một yêu cầu."
                ),
            },
        ) from None

    except Exception:
        logger.exception(
            "Account deletion inventory failed"
        )

        raise HTTPException(
            status_code=503,
            detail={
                "code": "inventory_unavailable",
                "message": (
                    "Chưa kiểm tra được dữ liệu tài khoản."
                ),
            },
        ) from None

    return AccountDeletionPreviewResponse(
        userDocuments=inventory.user_documents,
        familyLinks=inventory.family_links,
        familyInvitations=inventory.family_invitations,
        passwordResetRecords=inventory.password_reset_records,
        deletionExecuted=False,
    )
