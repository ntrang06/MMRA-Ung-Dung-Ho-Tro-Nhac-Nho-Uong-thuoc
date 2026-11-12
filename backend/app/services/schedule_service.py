from datetime import datetime, timezone

from firebase_admin import firestore
from google.cloud.firestore_v1.base_query import FieldFilter

from app.api.deps import CurrentUser
from app.core.firebase import get_firebase_app
from app.repositories import schedule_repository
from app.repositories.user_scope import user_collection, user_document
from app.schemas.medication import ScheduleResponse, ScheduleUpdate


class ScheduleNotFoundError(Exception):
    pass


class ScheduleVersionConflictError(Exception):
    pass


def update_schedule(
    user: CurrentUser,
    schedule_id: str,
    body: ScheduleUpdate,
) -> ScheduleResponse:
    schedule_ref = user_document(user, "schedules", schedule_id)
    medicine_ref = user_document(user, "medicines", body.medicineId)

    doses_query = user_collection(user, "doses").where(
        filter=FieldFilter("scheduleId", "==", schedule_id)
    )

    client = firestore.client(app=get_firebase_app())

    @firestore.transactional
    def save(transaction):
        snapshot = schedule_ref.get(transaction=transaction)
        current = schedule_repository._to_schedule(snapshot)

        if current is None:
            raise ScheduleNotFoundError()

        if current.version != body.expectedVersion:
            raise ScheduleVersionConflictError()

        medicine = medicine_ref.get(transaction=transaction)
        medicine_data = medicine.to_dict() or {}

        if not medicine.exists or medicine_data.get("deletedAt") is not None:
            raise schedule_repository.MedicineNotFoundError()

        # Đọc hết dữ liệu trước khi ghi.
        doses = list(doses_query.stream(transaction=transaction))

        now = datetime.now(timezone.utc)

        values = body.model_dump(exclude={"expectedVersion"})

        result = ScheduleResponse(
            **values,
            scheduleId=schedule_id,
            version=current.version + 1,
            createdAt=current.createdAt,
            updatedAt=now,
        )

        stored = body.model_dump(
            mode="json",
            exclude={"expectedVersion"},
        )

        stored.update(
            version=result.version,
            updatedAt=now,
        )

        cancelled = []

        for dose in doses:
            data = dose.to_dict() or {}

            if data.get("status") != "PENDING":
                continue

            scheduled_at = data.get("scheduledAt")

            if (
                not isinstance(scheduled_at, datetime)
                or scheduled_at.tzinfo is None
                or scheduled_at.utcoffset() is None
            ):
                raise ValueError(
                    "Liều PENDING thiếu scheduledAt có múi giờ."
                )

            if scheduled_at >= now:
                cancelled.append(dose.reference)

        transaction.update(schedule_ref, stored)

        for reference in cancelled:
            transaction.update(
                reference,
                {
                    "status": "CANCELLED",
                    "nextReminderAt": None,
                    "updatedAt": now,
                },
            )

        return result

    return save(client.transaction())


def delete_schedule(
    user: CurrentUser,
    schedule_id: str,
    expected_version: int,
) -> None:
    schedule_ref = user_document(user, "schedules", schedule_id)

    doses_query = user_collection(user, "doses").where(
        filter=FieldFilter("scheduleId", "==", schedule_id)
    )

    client = firestore.client(app=get_firebase_app())

    @firestore.transactional
    def remove(transaction):
        snapshot = schedule_ref.get(transaction=transaction)
        current = schedule_repository._to_schedule(snapshot)

        if current is None:
            raise ScheduleNotFoundError()

        if current.version != expected_version:
            raise ScheduleVersionConflictError()

        # Đọc hết dữ liệu trước khi ghi.
        doses = list(doses_query.stream(transaction=transaction))

        now = datetime.now(timezone.utc)
        cancelled = []

        for dose in doses:
            data = dose.to_dict() or {}

            if data.get("status") != "PENDING":
                continue

            scheduled_at = data.get("scheduledAt")

            if (
                not isinstance(scheduled_at, datetime)
                or scheduled_at.tzinfo is None
                or scheduled_at.utcoffset() is None
            ):
                raise ValueError(
                    "Liều PENDING thiếu scheduledAt có múi giờ."
                )

            if scheduled_at >= now:
                cancelled.append(dose.reference)

        # Xóa mềm, giữ dữ liệu phục vụ lịch sử.
        transaction.update(
            schedule_ref,
            {
                "enabled": False,
                "deletedAt": now,
                "updatedAt": now,
                "version": current.version + 1,
            },
        )

        for reference in cancelled:
            transaction.update(
                reference,
                {
                    "status": "CANCELLED",
                    "nextReminderAt": None,
                    "updatedAt": now,
                },
            )

    remove(client.transaction())