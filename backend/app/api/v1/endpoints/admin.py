import logging
from datetime import datetime, timezone
from typing import Annotated, Any

from fastapi import (
    APIRouter,
    HTTPException,
    Query,
    Response,
)
from firebase_admin import firestore
from google.api_core.exceptions import GoogleAPICallError
from pydantic import BaseModel, ConfigDict

from app.api.deps import AdminUser
from app.core.firebase import get_firebase_app


logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/admin",
    tags=["Admin"],
)

MEDICINE_CATALOG_COLLECTION = "medicineCatalog"
ADMIN_AUDIT_LOGS_COLLECTION = "adminAuditLogs"


# ============================================================
# 1. SCHEMAS
# ============================================================


class AdminMeResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    uid: str
    isAdmin: bool


class AdminSummaryResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    totalMedicines: int
    activeMedicines: int
    inactiveMedicines: int
    totalAuditLogs: int
    apiStatus: str = "healthy"


class AdminAuditLogResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    logId: str
    actorUid: str
    action: str
    resourceType: str
    resourceId: str
    createdAt: datetime


class AdminAuditLogsResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    items: list[AdminAuditLogResponse]


# ============================================================
# 2. HELPERS
# ============================================================


def get_firestore_client():
    return firestore.client(app=get_firebase_app())


def now_utc() -> datetime:
    return datetime.now(timezone.utc)


def admin_error(
    code: str,
    message: str,
    http_status: int,
) -> HTTPException:
    return HTTPException(
        status_code=http_status,
        detail={
            "code": code,
            "message": message,
        },
    )


def admin_unavailable() -> HTTPException:
    logger.exception("Admin Firestore operation failed.")
    return admin_error(
        "admin_unavailable",
        "Dịch vụ quản trị tạm thời không khả dụng.",
        503,
    )


def _normalize_timestamp(value: Any, fallback: datetime | None = None) -> datetime:
    if isinstance(value, datetime):
        if value.tzinfo is None:
            return value.replace(tzinfo=timezone.utc)
        return value
    return fallback or datetime.min.replace(tzinfo=timezone.utc)


def _snapshot_to_audit_log(
    log_id: str,
    data: dict[str, Any],
) -> AdminAuditLogResponse:
    return AdminAuditLogResponse(
        logId=log_id,
        actorUid=str(data.get("actorUid") or ""),
        action=str(data.get("action") or ""),
        resourceType=str(data.get("resourceType") or ""),
        resourceId=str(data.get("resourceId") or ""),
        createdAt=_normalize_timestamp(data.get("createdAt")),
    )


# ============================================================
# 3. W01 - TÀI KHOẢN ADMIN
# ============================================================


@router.get(
    "/me",
    response_model=AdminMeResponse,
)
def get_admin_profile(
    admin: AdminUser,
    response: Response,
):
    """W01: Xác minh tài khoản có quyền Admin hợp lệ."""
    response.headers["Cache-Control"] = "no-store"
    return AdminMeResponse(
        uid=admin.uid,
        isAdmin=admin.is_admin,
    )


# ============================================================
# 4. W02 - DASHBOARD TỔNG QUAN HỆ THỐNG
# ============================================================


@router.get(
    "/summary",
    response_model=AdminSummaryResponse,
)
def get_admin_summary(
    admin: AdminUser,
    response: Response,
):
    """W02: Trả về thống kê danh mục thuốc và nhật ký quản trị."""
    response.headers["Cache-Control"] = "no-store"

    try:
        db = get_firestore_client()
        medicine_snapshots = list(
            db.collection(MEDICINE_CATALOG_COLLECTION).stream()
        )
        audit_snapshots = list(
            db.collection(ADMIN_AUDIT_LOGS_COLLECTION).stream()
        )
    except GoogleAPICallError:
        raise admin_unavailable() from None

    total_medicines = 0
    active_medicines = 0

    for snapshot in medicine_snapshots:
        if not getattr(snapshot, "exists", True):
            continue
        data = snapshot.to_dict() or {}
        total_medicines += 1
        if data.get("active", True) is True:
            active_medicines += 1

    total_audit_logs = sum(
        1 for snapshot in audit_snapshots if getattr(snapshot, "exists", True)
    )

    return AdminSummaryResponse(
        totalMedicines=total_medicines,
        activeMedicines=active_medicines,
        inactiveMedicines=total_medicines - active_medicines,
        totalAuditLogs=total_audit_logs,
        apiStatus="healthy",
    )


# ============================================================
# 5. W07 - NHẬT KÝ QUẢN TRỊ
# ============================================================


@router.get(
    "/audit-logs",
    response_model=AdminAuditLogsResponse,
)
def list_admin_audit_logs(
    admin: AdminUser,
    response: Response,
    limit: Annotated[
        int,
        Query(ge=1, le=200),
    ] = 50,
):
    """W07: Lấy danh sách nhật ký thao tác của quản trị viên."""
    response.headers["Cache-Control"] = "no-store"

    try:
        db = get_firestore_client()
        snapshots = list(
            db.collection(ADMIN_AUDIT_LOGS_COLLECTION).stream()
        )
    except GoogleAPICallError:
        raise admin_unavailable() from None

    logs = [
        _snapshot_to_audit_log(snapshot.id, snapshot.to_dict() or {})
        for snapshot in snapshots
        if getattr(snapshot, "exists", True)
    ]

    logs.sort(key=lambda log: (log.createdAt, log.logId), reverse=True)

    return AdminAuditLogsResponse(items=logs[:limit])
