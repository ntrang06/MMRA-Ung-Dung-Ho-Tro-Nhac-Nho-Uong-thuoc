from fastapi import APIRouter, HTTPException, Response
from google.api_core.exceptions import GoogleAPICallError

from app.api.deps import AuthenticatedUser
from app.repositories import schedule_repository
from app.schemas.medication import (
    DocumentId,
    ScheduleInput,
    ScheduleResponse,
)


router = APIRouter(
    prefix="/schedules",
    tags=["Schedules"],
)


def _unavailable(
    message: str = "Chưa tải được lịch uống. Vui lòng thử lại.",
) -> HTTPException:
    return HTTPException(
        status_code=503,
        detail={
            "code": "schedules_unavailable",
            "message": message,
        },
    )


@router.get("", response_model=list[ScheduleResponse])
def list_schedules(
    user: AuthenticatedUser,
    response: Response,
):
    response.headers["Cache-Control"] = "no-store"

    try:
        return schedule_repository.list_schedules(user)

    except GoogleAPICallError:
        raise _unavailable() from None


@router.get(
    "/{schedule_id}",
    response_model=ScheduleResponse,
)
def get_schedule(
    schedule_id: DocumentId,
    user: AuthenticatedUser,
    response: Response,
):
    response.headers["Cache-Control"] = "no-store"

    try:
        schedule = schedule_repository.get_schedule(
            user,
            schedule_id,
        )

    except GoogleAPICallError:
        raise _unavailable() from None

    if schedule is None:
        raise HTTPException(
            status_code=404,
            detail={
                "code": "schedule_not_found",
                "message": "Không tìm thấy lịch uống.",
            },
        )

    return schedule


@router.post(
    "",
    response_model=ScheduleResponse,
    status_code=201,
)
def create_schedule(
    body: ScheduleInput,
    user: AuthenticatedUser,
    response: Response,
):
    response.headers["Cache-Control"] = "no-store"

    try:
        return schedule_repository.create_schedule(
            user,
            body,
        )

    except schedule_repository.MedicineNotFoundError:
        raise HTTPException(
            status_code=404,
            detail={
                "code": "medicine_not_found",
                "message": (
                    "Không tìm thấy thuốc trong tài khoản của bạn."
                ),
            },
        ) from None

    except GoogleAPICallError:
        raise _unavailable(
            "Chưa xác nhận được kết quả lưu. "
            "Vui lòng tải lại danh sách trước khi thử lại."
        ) from None