
from datetime import date
from typing import Annotated

from fastapi import (
    APIRouter,
    HTTPException,
    Path,
    Query,
    Response,
    status,
)

from app.api.deps import AuthenticatedUser
from app.repositories.medication_repository import MedicationRepository
from app.schemas.medication import (
    DoseActionRequest,
    DoseResponse,
    SnoozeRequest,
)
from app.services.dose_service import DoseService


# CRUD lịch uống chỉ nằm trong schedules.py.
# File này chỉ đăng ký các API /doses.
router = APIRouter(
    tags=["Medication Doses"],
)


@router.get(
    "/doses",
    response_model=list[DoseResponse],
)
def get_doses(
    user: AuthenticatedUser,
    target_date: date = Query(alias="date"),
):
    """
    Lấy danh sách liều theo ngày.

    Liều còn thiếu được sinh từ lịch uống
    đang có hiệu lực.
    """
    try:
        return DoseService.get_doses_for_date(
            user,
            target_date,
        )
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=str(exc),
        ) from exc


@router.get(
    "/doses/{dose_id}",
    response_model=DoseResponse,
)
def get_dose(
    dose_id: Annotated[
        str,
        Path(pattern=r"^[0-9a-f]{64}$"),
    ],
    user: AuthenticatedUser,
    response: Response,
):
    """Lấy liều đã lưu theo ID khi mở từ thông báo."""
    response.headers["Cache-Control"] = "no-store"

    # Chỉ đọc liều thuộc tài khoản đã đăng nhập.
    dose = MedicationRepository.get_dose(
        user,
        dose_id,
    )

    if dose is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={
                "code": "dose_not_found",
                "message": (
                    "Không tìm thấy liều thuốc "
                    "trong tài khoản của bạn."
                ),
            },
        )

    return dose


@router.patch(
    "/doses/{dose_id}/action",
    response_model=DoseResponse,
)
def update_dose_action(
    dose_id: str,
    payload: DoseActionRequest,
    user: AuthenticatedUser,
):
    """Đánh dấu liều đã uống hoặc bỏ qua."""
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
        ) from exc
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=str(exc),
        ) from exc


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
        ) from exc
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=str(exc),
        ) from exc
