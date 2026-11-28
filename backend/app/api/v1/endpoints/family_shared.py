
import logging
from datetime import date, datetime, timezone
from typing import Annotated

from fastapi import (
    APIRouter,
    HTTPException,
    Path,
    Query,
    Response,
)
from google.api_core.exceptions import GoogleAPICallError
from google.cloud.firestore_v1.base_query import FieldFilter
from pydantic import BaseModel, ConfigDict

from app.api.deps import (
    AuthenticatedUser,
    CurrentUser,
)
from app.api.v1.endpoints import family
from app.repositories import schedule_repository
from app.repositories.medication_repository import (
    MedicationRepository,
)
from app.schemas.medication import (
    DoseStatus,
    ScheduleResponse,
)


logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/family",
    tags=["Family"],
)

LinkId = Annotated[
    str,
    Path(pattern=r"^[0-9a-f]{64}$"),
]


# ============================================================
# 1. RESPONSE SCHEMAS
# ============================================================

class SharedSchedulesResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    items: list[ScheduleResponse]


class SharedDose(BaseModel):
    model_config = ConfigDict(extra="forbid")

    doseId: str
    localDate: date
    scheduledAt: datetime
    status: DoseStatus
    actionAt: datetime | None = None
    nextReminderAt: datetime | None = None


class SharedDosesResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    items: list[SharedDose]


class FamilyAlert(BaseModel):
    model_config = ConfigDict(extra="forbid")

    alertId: str
    linkId: str
    ownerUid: str
    doseId: str
    localDate: date
    scheduledAt: datetime
    status: DoseStatus
    actionAt: datetime | None = None


class FamilyAlertsResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    items: list[FamilyAlert]


# ============================================================
# 2. HELPERS
# ============================================================

def shared_data_error(
    code: str,
    message: str,
    status_code: int,
) -> HTTPException:
    return HTTPException(
        status_code=status_code,
        detail={
            "code": code,
            "message": message,
        },
    )


def get_shared_owner(
    link_id: str,
    current_user: CurrentUser,
    permission: str,
) -> CurrentUser:
    """
    Kiểm tra quan hệ Family và quyền chia sẻ.

    Trả về chủ sở hữu dữ liệu, không sử dụng UID
    do người chăm sóc tự nhập trong request.
    """
    db = family.get_firestore_client()

    reference = (
        db.collection("familyLinks")
        .document(link_id)
    )

    snapshot = reference.get()

    if not snapshot.exists:
        raise shared_data_error(
            "family_not_found",
            "Không tìm thấy liên kết Family.",
            404,
        )

    data = snapshot.to_dict() or {}

    if data.get("status") != "active":
        raise shared_data_error(
            "family_not_found",
            "Không tìm thấy liên kết Family.",
            404,
        )

    owner_uid = data.get("ownerUid")
    caregiver_uid = data.get("caregiverUid")

    if (
        not isinstance(owner_uid, str)
        or not owner_uid
        or "/" in owner_uid
        or not isinstance(caregiver_uid, str)
        or not caregiver_uid
        or "/" in caregiver_uid
        or owner_uid == caregiver_uid
    ):
        raise shared_data_error(
            "family_not_found",
            "Liên kết Family không hợp lệ.",
            404,
        )

    if current_user.uid not in {
        owner_uid,
        caregiver_uid,
    }:
        # Không tiết lộ liên kết có tồn tại hay không.
        raise shared_data_error(
            "family_not_found",
            "Không tìm thấy liên kết Family.",
            404,
        )

    if current_user.uid == caregiver_uid:
        permissions = data.get("permissions") or {}

        if (
            not isinstance(permissions, dict)
            or permissions.get(permission) is not True
        ):
            raise shared_data_error(
                "permission_denied",
                "Chưa được cấp quyền xem thông tin này.",
                403,
            )

    return CurrentUser(uid=owner_uid)


def ensure_permission_still_active(
    link_id: str,
    current_user: CurrentUser,
    permission: str,
) -> None:
    """
    Kiểm tra lại sau khi đọc dữ liệu, nhằm hạn chế
    trả dữ liệu khi quyền vừa bị thu hồi.
    """
    get_shared_owner(
        link_id,
        current_user,
        permission,
    )


def family_read_unavailable() -> HTTPException:
    logger.exception(
        "Cannot read shared Family information."
    )

    return shared_data_error(
        "family_data_unavailable",
        "Chưa tải được dữ liệu người thân. "
        "Vui lòng thử lại.",
        503,
    )


def _extract_missed_alerts(
    link_id: str,
    owner_uid: str,
    saved_doses: list[dict],
) -> list[FamilyAlert]:
    """Chỉ tạo cảnh báo cho các liều đã được xác nhận trạng thái MISSED."""
    alerts: list[FamilyAlert] = []

    for dose in saved_doses:
        if dose.get("status") != DoseStatus.MISSED:
            continue

        dose_id = str(dose.get("doseId") or "")
        if not dose_id:
            continue

        alerts.append(
            FamilyAlert.model_validate({
                "alertId": f"{link_id}:{dose_id}",
                "linkId": link_id,
                "ownerUid": owner_uid,
                "doseId": dose_id,
                "localDate": dose.get("localDate"),
                "scheduledAt": dose.get("scheduledAt"),
                "status": dose.get("status"),
                "actionAt": dose.get("actionAt"),
            })
        )

    return alerts


# ============================================================
# 3. GET - LỊCH UỐNG CỦA NGƯỜI THÂN
# ============================================================

@router.get(
    "/members/{link_id}/schedules",
    response_model=SharedSchedulesResponse,
)
def get_shared_schedules(
    link_id: LinkId,
    user: AuthenticatedUser,
    response: Response,
):
    response.headers["Cache-Control"] = "no-store"

    try:
        owner = get_shared_owner(
            link_id,
            user,
            "viewSchedule",
        )

        # Chỉ đọc lịch; không tạo hay sửa dữ liệu.
        schedules = schedule_repository.list_schedules(
            owner
        )

        ensure_permission_still_active(
            link_id,
            user,
            "viewSchedule",
        )

        return SharedSchedulesResponse(
            items=schedules
        )

    except HTTPException:
        raise

    except GoogleAPICallError:
        raise family_read_unavailable() from None


# ============================================================
# 4. GET - TRẠNG THÁI LIỀU NGƯỜI THÂN
# ============================================================

@router.get(
    "/members/{link_id}/doses",
    response_model=SharedDosesResponse,
)
def get_shared_doses(
    link_id: LinkId,
    user: AuthenticatedUser,
    response: Response,
    target_date: Annotated[
        date,
        Query(alias="date"),
    ],
):
    response.headers["Cache-Control"] = "no-store"

    try:
        owner = get_shared_owner(
            link_id,
            user,
            "viewDoseStatus",
        )

        # Không gọi DoseService.get_doses_for_date()
        # vì hàm đó có thể tạo và thay đổi trạng thái liều.
        #
        # Family chỉ đọc các liều đã được lưu.
        saved_doses = MedicationRepository.list_doses(
            owner,
            target_date,
        )

        items = []

        for dose in saved_doses:
            # Không chia sẻ tên thuốc hoặc dữ liệu chi tiết
            # ngoài phạm vi cần thiết để theo dõi trạng thái.
            items.append(
                SharedDose.model_validate({
                    "doseId": dose.get("doseId"),
                    "localDate": dose.get("localDate"),
                    "scheduledAt": dose.get("scheduledAt"),
                    "status": dose.get("status"),
                    "actionAt": dose.get("actionAt"),
                    "nextReminderAt": dose.get(
                        "nextReminderAt"
                    ),
                })
            )

        ensure_permission_still_active(
            link_id,
            user,
            "viewDoseStatus",
        )

        return SharedDosesResponse(
            items=items
        )

    except HTTPException:
        raise

    except GoogleAPICallError:
        raise family_read_unavailable() from None


# ============================================================
# 5. GET - CẢNH BÁO CHO NGƯỜI CHĂM SÓC
# ============================================================


@router.get(
    "/alerts",
    response_model=FamilyAlertsResponse,
)
def get_family_alerts(
    user: AuthenticatedUser,
    response: Response,
    target_date: Annotated[
        date | None,
        Query(alias="date"),
    ] = None,
    link_id: Annotated[
        str | None,
        Query(
            alias="linkId",
            pattern=r"^[0-9a-f]{64}$",
        ),
    ] = None,
):
    response.headers["Cache-Control"] = "no-store"
    resolved_date = (
        target_date or datetime.now(timezone.utc).date()
    )

    try:
        alerts: list[FamilyAlert] = []

        if link_id is not None:
            owner = get_shared_owner(
                link_id,
                user,
                "receiveMissedDoseAlerts",
            )
            saved_doses = MedicationRepository.list_doses(
                owner,
                resolved_date,
            )
            ensure_permission_still_active(
                link_id,
                user,
                "receiveMissedDoseAlerts",
            )
            alerts = _extract_missed_alerts(
                link_id,
                owner.uid,
                saved_doses,
            )
        else:
            db = family.get_firestore_client()
            snapshots = (
                db.collection("familyLinks")
                .where(
                    filter=FieldFilter(
                        "caregiverUid",
                        "==",
                        user.uid,
                    )
                )
                .stream()
            )
            seen_links: set[str] = set()

            for snapshot in snapshots:
                if not getattr(snapshot, "exists", True):
                    continue

                current_link_id = snapshot.id
                if current_link_id in seen_links:
                    continue

                data = snapshot.to_dict() or {}
                if data.get("status") != "active":
                    continue

                owner_uid = data.get("ownerUid")
                caregiver_uid = data.get("caregiverUid")

                if (
                    caregiver_uid != user.uid
                    or not isinstance(owner_uid, str)
                    or not owner_uid
                    or "/" in owner_uid
                    or owner_uid == caregiver_uid
                ):
                    continue

                permissions = data.get("permissions") or {}
                if (
                    not isinstance(permissions, dict)
                    or permissions.get(
                        "receiveMissedDoseAlerts"
                    )
                    is not True
                ):
                    continue

                seen_links.add(current_link_id)

                saved_doses = (
                    MedicationRepository.list_doses(
                        CurrentUser(uid=owner_uid),
                        resolved_date,
                    )
                )

                ensure_permission_still_active(
                    current_link_id,
                    user,
                    "receiveMissedDoseAlerts",
                )

                alerts.extend(
                    _extract_missed_alerts(
                        current_link_id,
                        owner_uid,
                        saved_doses,
                    )
                )

        alerts.sort(
            key=lambda item: (
                item.scheduledAt,
                item.linkId,
                item.doseId,
            )
        )

        return FamilyAlertsResponse(items=alerts)

    except HTTPException:
        raise

    except GoogleAPICallError:
        raise family_read_unavailable() from None
