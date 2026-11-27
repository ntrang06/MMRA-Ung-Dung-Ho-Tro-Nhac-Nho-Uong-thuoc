
import logging
import time
from datetime import datetime, timezone
from typing import Annotated

from fastapi import (
    APIRouter,
    Depends,
    HTTPException,
    Response,
)
from fastapi.security import HTTPAuthorizationCredentials
from firebase_admin import auth, firestore
from google.api_core.exceptions import (
    AlreadyExists,
    GoogleAPICallError,
)
from pydantic import BaseModel, ConfigDict, Field
from sqlalchemy.orm import Session

from app.api.deps import AuthenticatedUser, bearer
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

RECENT_LOGIN_SECONDS = 300
ALLOWED_CLOCK_SKEW_SECONDS = 60


# ============================================================
# 1. SCHEMAS
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


class ConfirmDeletionRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    confirmation: str = Field(
        min_length=1,
        max_length=100,
    )


class ConfirmDeletionResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    status: str
    confirmedAt: datetime
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
        "Account deletion service unavailable"
    )

    return HTTPException(
        status_code=503,
        detail={
            "code": "deletion_unavailable",
            "message": (
                "Dịch vụ xóa tài khoản tạm thời "
                "không khả dụng. Vui lòng thử lại."
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
        "confirmed",
        "processing",
        "failed",
        "completed",
    }:
        raise deletion_unavailable()

    return AccountDeletionResponse(
        status=state,
        requestedAt=requested_at,
    )


def is_recent_login(
    auth_time: object,
    now_seconds: int,
) -> bool:
    if (
        type(auth_time) is not int
        or auth_time <= 0
    ):
        return False

    age = now_seconds - auth_time

    return (
        -ALLOWED_CLOCK_SKEW_SECONDS
        <= age
        <= RECENT_LOGIN_SECONDS
    )


def verify_recent_login(
    authorization: str,
    expected_uid: str,
) -> None:
    """
    Xác minh lại Firebase ID Token và thời điểm
    đăng nhập thật (auth_time).

    Không ghi token vào logs.
    """
    try:
        firebase_app = get_firebase_app()

        claims = auth.verify_id_token(
            authorization,
            app=firebase_app,
            check_revoked=True,
        )

    except Exception:
        logger.warning(
            "Account deletion token verification failed"
        )

        raise HTTPException(
            status_code=401,
            detail={
                "code": "reauthentication_required",
                "message": (
                    "Vui lòng đăng nhập lại để "
                    "xác nhận xóa tài khoản."
                ),
            },
        ) from None

    if claims.get("uid") != expected_uid:
        raise HTTPException(
            status_code=401,
            detail={
                "code": "invalid_user",
                "message": (
                    "Phiên xác thực không hợp lệ."
                ),
            },
        )

    if not is_recent_login(
        claims.get("auth_time"),
        int(time.time()),
    ):
        raise HTTPException(
            status_code=401,
            detail={
                "code": "recent_login_required",
                "message": (
                    "Bạn cần đăng nhập lại trong "
                    "5 phút gần nhất để xác nhận."
                ),
            },
        )


# ============================================================
# 3. POST - GỬI YÊU CẦU XÓA
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
            return validate_request_snapshot(
                reference.get(),
                user.uid,
            )

    except GoogleAPICallError:
        raise deletion_unavailable() from None


# ============================================================
# 4. GET - TRẠNG THÁI YÊU CẦU
# ============================================================

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


# ============================================================
# 5. GET - KIỂM KÊ DỮ LIỆU
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
                    "Không thể kiểm kê đầy đủ "
                    "dữ liệu trong một yêu cầu."
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


# ============================================================
# 6. POST - XÁC NHẬN YÊU CẦU XÓA
# ============================================================

@router.post(
    "/confirm",
    response_model=ConfirmDeletionResponse,
)
def confirm_account_deletion(
    payload: ConfirmDeletionRequest,
    user: AuthenticatedUser,
    response: Response,
    credentials: Annotated[
        HTTPAuthorizationCredentials | None,
        Depends(bearer),
    ],
):
    """
    Xác nhận ý định xóa tài khoản.

    Chưa xóa Firebase Auth hoặc dữ liệu.
    """
    response.headers["Cache-Control"] = "no-store"

    if payload.confirmation != "XOA TAI KHOAN":
        raise HTTPException(
            status_code=422,
            detail={
                "code": "confirmation_mismatch",
                "message": (
                    "Vui lòng nhập chính xác "
                    "XOA TAI KHOAN."
                ),
            },
        )

    if credentials is None:
        raise HTTPException(
            status_code=401,
            detail={
                "code": "unauthenticated",
                "message": "Vui lòng đăng nhập lại.",
            },
        )

    verify_recent_login(
        credentials.credentials,
        user.uid,
    )

    now = datetime.now(timezone.utc)

    try:
        reference = get_deletion_collection().document(
            user.uid
        )

        client = firestore.client(
            app=get_firebase_app()
        )

        @firestore.transactional
        def confirm(transaction):
            snapshot = reference.get(
                transaction=transaction
            )

            if not snapshot.exists:
                raise HTTPException(
                    status_code=404,
                    detail={
                        "code": "request_not_found",
                        "message": (
                            "Vui lòng gửi yêu cầu xóa "
                            "tài khoản trước."
                        ),
                    },
                )

            data = snapshot.to_dict() or {}

            if data.get("uid") != user.uid:
                raise HTTPException(
                    status_code=403,
                    detail={
                        "code": "permission_denied",
                    },
                )

            state = data.get("status")

            if state == "confirmed":
                confirmed_at = data.get(
                    "confirmedAt"
                )

                if (
                    not isinstance(
                        confirmed_at,
                        datetime,
                    )
                    or confirmed_at.tzinfo is None
                ):
                    raise ValueError(
                        "Invalid confirmation state"
                    )

                return confirmed_at

            if state != "pending":
                raise HTTPException(
                    status_code=409,
                    detail={
                        "code": "invalid_deletion_state",
                        "message": (
                            "Yêu cầu đang được xử lý "
                            "hoặc đã hoàn tất."
                        ),
                    },
                )

            transaction.update(
                reference,
                {
                    "status": "confirmed",
                    "confirmedAt": now,
                    "updatedAt": now,
                },
            )

            return now

        confirmed_at = confirm(
            client.transaction()
        )

        return ConfirmDeletionResponse(
            status="confirmed",
            confirmedAt=confirmed_at,
            deletionExecuted=False,
        )

    except HTTPException:
        raise

    except (GoogleAPICallError, ValueError):
        raise deletion_unavailable() from None
