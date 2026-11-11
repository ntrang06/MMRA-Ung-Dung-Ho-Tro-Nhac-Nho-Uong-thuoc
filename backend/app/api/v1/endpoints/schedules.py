from fastapi import APIRouter, HTTPException, Response
from google.api_core.exceptions import GoogleAPICallError

from app.api.deps import AuthenticatedUser
from app.repositories import schedule_repository
from app.schemas.medication import DocumentId, ScheduleResponse


router = APIRouter(
    prefix="/schedules",
    tags=["Schedules"],
)


def _unavailable() -> HTTPException:
    return HTTPException(
        status_code=503,
        detail={
            "code": "schedules_unavailable",
            "message": "Chưa tải được lịch uống. Vui lòng thử lại.",
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