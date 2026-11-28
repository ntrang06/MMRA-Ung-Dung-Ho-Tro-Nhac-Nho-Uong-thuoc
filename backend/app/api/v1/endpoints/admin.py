import logging
from datetime import datetime, timezone
from typing import Annotated, Any
from uuid import uuid4

from fastapi import (
    APIRouter,
    HTTPException,
    Path,
    Query,
    Response,
    status,
)
from firebase_admin import firestore
from google.api_core.exceptions import GoogleAPICallError
from google.cloud import firestore as google_firestore
from pydantic import (
    BaseModel,
    ConfigDict,
    Field,
    field_validator,
    model_validator,
)

from app.api.deps import AdminUser
from app.core.firebase import get_firebase_app


logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/admin",
    tags=["Admin"],
)

MEDICINE_CATALOG_COLLECTION = "medicineCatalog"
ADMIN_AUDIT_LOGS_COLLECTION = "adminAuditLogs"

CatalogId = Annotated[
    str,
    Path(
        min_length=1,
        max_length=128,
        pattern=r"^[A-Za-z0-9_-]+$",
    ),
]


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


class CatalogMedicineInput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    name: str = Field(min_length=1, max_length=200)
    strength: str = Field(default="", max_length=100)
    dosageForm: str = Field(default="", max_length=100)
    source: str = Field(default="admin", max_length=100)

    @field_validator("name", "strength", "dosageForm", "source")
    @classmethod
    def strip_text(cls, value: str) -> str:
        return value.strip()

    @field_validator("name")
    @classmethod
    def validate_name(cls, value: str) -> str:
        if not value:
            raise ValueError("Tên thuốc không được để trống.")
        return value

    @field_validator("source")
    @classmethod
    def normalize_source(cls, value: str) -> str:
        return value or "admin"


class CatalogMedicineUpdateInput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    name: str | None = Field(default=None, min_length=1, max_length=200)
    strength: str | None = Field(default=None, max_length=100)
    dosageForm: str | None = Field(default=None, max_length=100)
    source: str | None = Field(default=None, max_length=100)
    active: bool | None = None

    @field_validator("name", "strength", "dosageForm", "source")
    @classmethod
    def strip_optional_text(cls, value: str | None) -> str | None:
        if value is None:
            return None
        return value.strip()

    @field_validator("name")
    @classmethod
    def validate_optional_name(cls, value: str | None) -> str | None:
        if value is not None and not value:
            raise ValueError("Tên thuốc không được để trống.")
        return value

    @field_validator("source")
    @classmethod
    def validate_optional_source(cls, value: str | None) -> str | None:
        if value is not None and not value:
            raise ValueError("Nguồn thuốc không được để trống.")
        return value

    @model_validator(mode="after")
    def ensure_at_least_one_field(self):
        if (
            self.name is None
            and self.strength is None
            and self.dosageForm is None
            and self.source is None
            and self.active is None
        ):
            raise ValueError("Cần ít nhất một trường để cập nhật.")
        return self


class CatalogMedicineResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    catalogId: str
    name: str
    strength: str
    dosageForm: str
    source: str
    active: bool
    createdAt: datetime
    updatedAt: datetime
    updatedByUid: str


class AdminMedicinesResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    items: list[CatalogMedicineResponse]


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


def _snapshot_to_catalog_medicine(
    catalog_id: str,
    data: dict[str, Any],
) -> CatalogMedicineResponse:
    created_at = _normalize_timestamp(data.get("createdAt"))
    updated_at = _normalize_timestamp(data.get("updatedAt"), created_at)
    return CatalogMedicineResponse(
        catalogId=catalog_id,
        name=str(data.get("name") or ""),
        strength=str(data.get("strength") or ""),
        dosageForm=str(data.get("dosageForm") or ""),
        source=str(data.get("source") or ""),
        active=bool(data.get("active", True)),
        createdAt=created_at,
        updatedAt=updated_at,
        updatedByUid=str(data.get("updatedByUid") or ""),
    )


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


def _build_audit_log_data(
    actor_uid: str,
    action: str,
    resource_id: str,
    created_at: datetime,
) -> dict[str, Any]:
    return {
        "actorUid": actor_uid,
        "action": action,
        "resourceType": MEDICINE_CATALOG_COLLECTION,
        "resourceId": resource_id,
        "createdAt": created_at,
    }


def _matches_search(
    medicine: CatalogMedicineResponse,
    search: str | None,
) -> bool:
    if not search:
        return True
    needle = search.strip().casefold()
    if not needle:
        return True
    haystack = " ".join(
        [
            medicine.name,
            medicine.strength,
            medicine.dosageForm,
            medicine.source,
        ]
    ).casefold()
    return needle in haystack


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
# 5. W03 - DANH SÁCH & TÌM KIẾM DANH MỤC THUỐC
# ============================================================


@router.get(
    "/medicines",
    response_model=AdminMedicinesResponse,
)
def list_catalog_medicines(
    admin: AdminUser,
    response: Response,
    search: Annotated[
        str | None,
        Query(max_length=100),
    ] = None,
    active: Annotated[
        bool | None,
        Query(),
    ] = None,
):
    """W03: Tìm kiếm và liệt kê danh mục thuốc dùng chung."""
    response.headers["Cache-Control"] = "no-store"

    try:
        db = get_firestore_client()
        snapshots = list(
            db.collection(MEDICINE_CATALOG_COLLECTION).stream()
        )
    except GoogleAPICallError:
        raise admin_unavailable() from None

    items: list[CatalogMedicineResponse] = []

    for snapshot in snapshots:
        if not getattr(snapshot, "exists", True):
            continue
        data = snapshot.to_dict() or {}
        medicine = _snapshot_to_catalog_medicine(snapshot.id, data)

        if active is not None and medicine.active is not active:
            continue
        if not _matches_search(medicine, search):
            continue

        items.append(medicine)

    items.sort(key=lambda item: (item.name.casefold(), item.catalogId))

    return AdminMedicinesResponse(items=items)


# ============================================================
# 6. W04 - THÊM THUỐC VÀO DANH MỤC
# ============================================================


@router.post(
    "/medicines",
    response_model=CatalogMedicineResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_catalog_medicine(
    payload: CatalogMedicineInput,
    admin: AdminUser,
    response: Response,
):
    """W04: Tạo thuốc mới trong danh mục dùng chung và ghi audit log."""
    response.headers["Cache-Control"] = "no-store"

    catalog_id = uuid4().hex
    log_id = uuid4().hex
    now = now_utc()

    medicine_data = {
        "name": payload.name,
        "strength": payload.strength,
        "dosageForm": payload.dosageForm,
        "source": payload.source,
        "active": True,
        "createdAt": now,
        "updatedAt": now,
        "updatedByUid": admin.uid,
    }
    audit_data = _build_audit_log_data(
        actor_uid=admin.uid,
        action="CREATE_MEDICINE",
        resource_id=catalog_id,
        created_at=now,
    )

    try:
        db = get_firestore_client()
        medicine_ref = db.collection(MEDICINE_CATALOG_COLLECTION).document(
            catalog_id
        )
        audit_ref = db.collection(ADMIN_AUDIT_LOGS_COLLECTION).document(log_id)

        @google_firestore.transactional
        def create_with_audit(transaction):
            transaction.create(medicine_ref, medicine_data)
            transaction.create(audit_ref, audit_data)

        create_with_audit(db.transaction())
    except GoogleAPICallError:
        raise admin_unavailable() from None

    return _snapshot_to_catalog_medicine(catalog_id, medicine_data)


# ============================================================
# 7. W05 - CẬP NHẬT THUỐC TRONG DANH MỤC
# ============================================================


@router.put(
    "/medicines/{catalog_id}",
    response_model=CatalogMedicineResponse,
)
def update_catalog_medicine(
    catalog_id: CatalogId,
    payload: CatalogMedicineUpdateInput,
    admin: AdminUser,
    response: Response,
):
    """W05: Cập nhật thông tin thuốc trong danh mục dùng chung và ghi audit log."""
    response.headers["Cache-Control"] = "no-store"

    log_id = uuid4().hex
    now = now_utc()

    try:
        db = get_firestore_client()
        medicine_ref = db.collection(MEDICINE_CATALOG_COLLECTION).document(
            catalog_id
        )
        audit_ref = db.collection(ADMIN_AUDIT_LOGS_COLLECTION).document(log_id)

        @google_firestore.transactional
        def update_with_audit(transaction):
            snapshot = medicine_ref.get(transaction=transaction)

            if not snapshot.exists:
                raise admin_error(
                    "catalog_medicine_not_found",
                    "Không tìm thấy thuốc trong danh mục.",
                    404,
                )

            current_data = snapshot.to_dict() or {}
            updates: dict[str, Any] = {
                "updatedAt": now,
                "updatedByUid": admin.uid,
            }
            for field_name in ("name", "strength", "dosageForm", "source", "active"):
                field_value = getattr(payload, field_name)
                if field_value is not None:
                    updates[field_name] = field_value

            merged_data = {**current_data, **updates}
            transaction.update(medicine_ref, updates)
            transaction.create(
                audit_ref,
                _build_audit_log_data(
                    actor_uid=admin.uid,
                    action="UPDATE_MEDICINE",
                    resource_id=catalog_id,
                    created_at=now,
                ),
            )
            return merged_data

        updated_data = update_with_audit(db.transaction())
    except HTTPException:
        raise
    except GoogleAPICallError:
        raise admin_unavailable() from None

    return _snapshot_to_catalog_medicine(catalog_id, updated_data)


# ============================================================
# 8. W06 - ẨN THUỐC KHỎI DANH MỤC (SOFT DELETE)
# ============================================================


@router.delete(
    "/medicines/{catalog_id}",
    response_model=CatalogMedicineResponse,
)
def hide_catalog_medicine(
    catalog_id: CatalogId,
    admin: AdminUser,
    response: Response,
):
    """W06: Ẩn mềm thuốc trong danh mục (active = false) và ghi audit log."""
    response.headers["Cache-Control"] = "no-store"

    log_id = uuid4().hex
    now = now_utc()

    try:
        db = get_firestore_client()
        medicine_ref = db.collection(MEDICINE_CATALOG_COLLECTION).document(
            catalog_id
        )
        audit_ref = db.collection(ADMIN_AUDIT_LOGS_COLLECTION).document(log_id)

        @google_firestore.transactional
        def hide_with_audit(transaction):
            snapshot = medicine_ref.get(transaction=transaction)

            if not snapshot.exists:
                raise admin_error(
                    "catalog_medicine_not_found",
                    "Không tìm thấy thuốc trong danh mục.",
                    404,
                )

            current_data = snapshot.to_dict() or {}
            updates = {
                "active": False,
                "updatedAt": now,
                "updatedByUid": admin.uid,
            }
            merged_data = {**current_data, **updates}

            transaction.update(medicine_ref, updates)
            transaction.create(
                audit_ref,
                _build_audit_log_data(
                    actor_uid=admin.uid,
                    action="HIDE_MEDICINE",
                    resource_id=catalog_id,
                    created_at=now,
                ),
            )
            return merged_data

        hidden_data = hide_with_audit(db.transaction())
    except HTTPException:
        raise
    except GoogleAPICallError:
        raise admin_unavailable() from None

    return _snapshot_to_catalog_medicine(catalog_id, hidden_data)


# ============================================================
# 9. W07 - NHẬT KÝ QUẢN TRỊ
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
