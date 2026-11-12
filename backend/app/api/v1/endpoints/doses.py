from datetime import date, datetime, timezone
from uuid import uuid4

from fastapi import APIRouter, HTTPException, Query, status

from app.api.deps import AuthenticatedUser, CurrentUser
from app.repositories.medication_repository import MedicationRepository
from app.schemas.medication import (
    DoseActionRequest,
    DoseResponse,
    ScheduleInput,
    ScheduleResponse,
    ScheduleUpdate,
    SnoozeRequest,
)
from app.services.dose_service import DoseService


router = APIRouter(tags=["Medication Schedules and Doses"])


# =============================================================
# SCHEDULE ENDPOINTS
# =============================================================

@router.post(
    "/schedules",
    response_model=ScheduleResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_schedule(
    payload: ScheduleInput,
    user: AuthenticatedUser,
):
    """Tạo lịch uống thuốc mới."""

    medicine = MedicationRepository.get_medicine(
        user,
        payload.medicineId,
    )

    if medicine is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Không tìm thấy thuốc.",
        )

    now = datetime.now(timezone.utc)
    schedule_id = uuid4().hex

    schedule_data = payload.model_dump(mode="json")

    schedule_data.update({
        "scheduleId": schedule_id,
        "version": 1,
        "createdAt": now,
        "updatedAt": now,
    })

    try:
        MedicationRepository.create_schedule(
            user,
            schedule_id,
            schedule_data,
        )
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Không thể tạo lịch uống thuốc.",
        )

    return schedule_data


@router.get(
    "/schedules",
    response_model=list[ScheduleResponse],
)
def get_schedules(
    user: AuthenticatedUser,
    enabled_only: bool = Query(default=False),
):
    """Lấy danh sách lịch uống thuốc."""

    schedules = MedicationRepository.list_schedules(
        user,
        enabled_only=enabled_only,
    )

    return schedules


@router.get(
    "/schedules/{schedule_id}",
    response_model=ScheduleResponse,
)
def get_schedule(
    schedule_id: str,
    user: AuthenticatedUser,
):
    """Lấy thông tin một lịch uống thuốc."""

    schedule = MedicationRepository.get_schedule(
        user,
        schedule_id,
    )

    if schedule is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Không tìm thấy lịch uống thuốc.",
        )

    return schedule


@router.put(
    "/schedules/{schedule_id}",
    response_model=ScheduleResponse,
)
def update_schedule(
    schedule_id: str,
    payload: ScheduleUpdate,
    user: AuthenticatedUser,
):
    """Cập nhật lịch uống thuốc theo phiên bản hiện tại."""

    existing = MedicationRepository.get_schedule(
        user,
        schedule_id,
    )

    if existing is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Không tìm thấy lịch uống thuốc.",
        )

    expected_version = payload.expectedVersion
    current_version = existing.get("version", 1)

    if expected_version != current_version:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=(
                "Lịch uống thuốc đã được cập nhật. "
                "Vui lòng tải lại dữ liệu trước khi chỉnh sửa."
            ),
        )

    medicine = MedicationRepository.get_medicine(
        user,
        payload.medicineId,
    )

    if medicine is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Không tìm thấy thuốc.",
        )

    now = datetime.now(timezone.utc)

    updated_data = payload.model_dump(
        mode="json",
        exclude={"expectedVersion"},
    )

    updated_data.update({
        "scheduleId": schedule_id,
        "version": current_version + 1,
        "createdAt": existing.get("createdAt"),
        "updatedAt": now,
    })


    result = MedicationRepository.update_schedule_if_version(
        user=user,
        schedule_id=schedule_id,
        expected_version=expected_version,
        data=updated_data,
    )

    if result is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Không tìm thấy lịch uống thuốc.",
        )

    if result is False:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=(
                "Lịch uống thuốc đã được cập nhật bởi một thiết bị khác. "
                "Vui lòng tải lại dữ liệu."
            ),
        )

    return result



# =============================================================
# DOSE ENDPOINTS
# =============================================================

@router.get(
    "/doses",
    response_model=list[DoseResponse],
)
def get_doses(
    user: AuthenticatedUser,
    target_date: date = Query(alias="date"),
):
    """
    Lấy danh sách Dose theo ngày.

    Dose còn thiếu sẽ được sinh tự động từ lịch uống thuốc.
    """

    try:
        doses = DoseService.get_doses_for_date(
            user,
            target_date,
        )

        return doses

    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=str(exc),
        )


@router.patch(
    "/doses/{dose_id}/action",
    response_model=DoseResponse,
)
def update_dose_action(
    dose_id: str,
    payload: DoseActionRequest,
    user: AuthenticatedUser,
):
    """Đánh dấu Dose đã uống hoặc bỏ qua."""

    try:
        return DoseService.update_dose_action(
            user,
            dose_id,
            payload.action,
        )

    except LookupError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(exc),
        )

    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=str(exc),
        )


@router.post(
    "/doses/{dose_id}/snooze",
    response_model=DoseResponse,
)
def snooze_dose(
    dose_id: str,
    payload: SnoozeRequest,
    user: AuthenticatedUser,
):
    """Hoãn thời gian nhắc uống thuốc."""

    try:
        return DoseService.snooze_dose(
            user,
            dose_id,
            payload.minutes,
        )

    except LookupError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(exc),
        )

    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=str(exc),
        )
