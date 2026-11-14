import logging
from typing import Annotated

from fastapi import APIRouter, HTTPException, Query, Response
from google.api_core.exceptions import GoogleAPICallError

from app.api.deps import AuthenticatedUser
from app.repositories import schedule_repository
from app.schemas.medication import (
    DocumentId,
    ScheduleInput,
    ScheduleResponse,
    ScheduleUpdate,
)
from app.services import schedule_service


logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/schedules",
    tags=["Schedules"],
)


def _unavailable(
    message: str = "Chưa tải được lịch uống. Vui lòng thử lại.",
) -> HTTPException:
    # In lỗi gốc ra terminal chạy Uvicorn.
    logger.exception(
        "Lỗi khi xử lý lịch uống trên Firestore"
    )

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


@router.put(
    "/{schedule_id}",
    response_model=ScheduleResponse,
)
def update_schedule(
    schedule_id: DocumentId,
    body: ScheduleUpdate,
    user: AuthenticatedUser,
    response: Response,
):
    response.headers["Cache-Control"] = "no-store"

    try:
        return schedule_service.update_schedule(
            user,
            schedule_id,
            body,
        )

    except schedule_service.ScheduleNotFoundError:
        raise HTTPException(
            status_code=404,
            detail={
                "code": "schedule_not_found",
                "message": "Không tìm thấy lịch uống.",
            },
        ) from None

    except schedule_service.ScheduleVersionConflictError:
        raise HTTPException(
            status_code=409,
            detail={
                "code": "version_conflict",
                "message": (
                    "Lịch đã thay đổi. "
                    "Vui lòng tải lại trước khi sửa."
                ),
            },
        ) from None

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


@router.delete(
    "/{schedule_id}",
    status_code=204,
    response_class=Response,
)
def delete_schedule(
    schedule_id: DocumentId,
    user: AuthenticatedUser,
    expected_version: Annotated[
        int,
        Query(alias="expectedVersion", ge=1),
    ],
) -> Response:
    try:
        schedule_service.delete_schedule(
            user,
            schedule_id,
            expected_version,
        )

    except schedule_service.ScheduleNotFoundError:
        raise HTTPException(
            status_code=404,
            detail={
                "code": "schedule_not_found",
                "message": "Không tìm thấy lịch uống.",
            },
        ) from None

    except schedule_service.ScheduleVersionConflictError:
        raise HTTPException(
            status_code=409,
            detail={
                "code": "version_conflict",
                "message": (
                    "Lịch đã thay đổi. "
                    "Vui lòng tải lại trước khi xóa."
                ),
            },
        ) from None

    except GoogleAPICallError:
        raise _unavailable(
            "Chưa xác nhận được kết quả xóa. "
            "Vui lòng tải lại danh sách trước khi thử lại."
        ) from None

    return Response(
        status_code=204,
        headers={"Cache-Control": "no-store"},
    )