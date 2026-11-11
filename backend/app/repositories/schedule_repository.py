from google.cloud.firestore_v1 import DocumentSnapshot

from app.api.deps import CurrentUser
from app.repositories.user_scope import user_collection, user_document
from app.schemas.medication import ScheduleResponse


def _to_schedule(snapshot: DocumentSnapshot) -> ScheduleResponse | None:
    if not snapshot.exists:
        return None

    data = snapshot.to_dict() or {}

    # Lịch xóa mềm không xuất hiện trong danh sách hoặc chi tiết.
    # enabled=False chỉ là tạm dừng, vẫn được xem lịch.
    if data.get("deletedAt") is not None:
        return None

    payload = {
        key: value
        for key, value in data.items()
        if key in ScheduleResponse.model_fields
    }
    payload["scheduleId"] = snapshot.id

    return ScheduleResponse.model_validate(payload)


def list_schedules(user: CurrentUser) -> list[ScheduleResponse]:
    snapshots = user_collection(user, "schedules").stream()
    schedules = []

    for snapshot in snapshots:
        schedule = _to_schedule(snapshot)

        if schedule is not None:
            schedules.append(schedule)

    return sorted(
        schedules,
        key=lambda schedule: (
            schedule.createdAt,
            schedule.scheduleId,
        ),
        reverse=True,
    )


def get_schedule(
    user: CurrentUser,
    schedule_id: str,
) -> ScheduleResponse | None:
    snapshot = user_document(
        user,
        "schedules",
        schedule_id,
    ).get()

    return _to_schedule(snapshot)