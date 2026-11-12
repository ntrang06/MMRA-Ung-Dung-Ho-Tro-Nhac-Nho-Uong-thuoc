from datetime import datetime, timezone
from uuid import uuid4

from firebase_admin import firestore
from google.cloud.firestore_v1 import DocumentSnapshot

from app.api.deps import CurrentUser
from app.core.firebase import get_firebase_app
from app.repositories.user_scope import user_collection, user_document
from app.schemas.medication import ScheduleInput, ScheduleResponse


class MedicineNotFoundError(Exception):
    pass


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


def create_schedule(
    user: CurrentUser,
    schedule: ScheduleInput,
) -> ScheduleResponse:
    schedule_id = uuid4().hex

    medicine_ref = user_document(
        user,
        "medicines",
        schedule.medicineId,
    )

    schedule_ref = user_document(
        user,
        "schedules",
        schedule_id,
    )

    client = firestore.client(app=get_firebase_app())

    @firestore.transactional
    def save(transaction):
        # Đọc thuốc và tạo lịch trong cùng giao dịch.
        medicine = medicine_ref.get(transaction=transaction)
        medicine_data = medicine.to_dict() or {}

        if (
            not medicine.exists
            or medicine_data.get("deletedAt") is not None
        ):
            raise MedicineNotFoundError()

        now = datetime.now(timezone.utc)

        result = ScheduleResponse(
            **schedule.model_dump(),
            scheduleId=schedule_id,
            version=1,
            createdAt=now,
            updatedAt=now,
        )

        # Ngày lưu dạng yyyy-MM-dd.
        # createdAt/updatedAt lưu dạng Firestore Timestamp.
        data = schedule.model_dump(mode="json")

        data.update(
            version=1,
            createdAt=now,
            updatedAt=now,
            deletedAt=None,
        )

        transaction.create(schedule_ref, data)

        return result

    return save(client.transaction())