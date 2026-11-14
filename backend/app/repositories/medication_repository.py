from datetime import date, datetime, timedelta, timezone
from typing import Any

from firebase_admin import firestore
from google.api_core.exceptions import AlreadyExists
from google.cloud import firestore as google_firestore
from google.cloud.firestore_v1.base_query import FieldFilter

from app.api.deps import CurrentUser
from app.core.firebase import get_firebase_app
from app.repositories.user_scope import (
    user_collection,
    user_document,
)


def _snapshot_to_dict(
    snapshot: Any,
    *,
    id_field: str = "id",
) -> dict[str, Any] | None:
    """Chuyển Firestore DocumentSnapshot thành dictionary."""
    if not snapshot.exists:
        return None

    data = snapshot.to_dict() or {}

    if id_field == "doseId":
        # DoseResponse không cho phép trường metadata "id".
        data.pop("id", None)
        data["doseId"] = snapshot.id
    else:
        data.setdefault(id_field, snapshot.id)

    return data


class MedicationRepository:
    """Quản lý medicines, schedules và doses trong Firestore."""

    @staticmethod
    def create_dose_if_absent(
        user: CurrentUser,
        dose_id: str,
        data: dict[str, Any],
    ) -> bool:
        reference = user_document(
            user,
            "doses",
            dose_id,
        )

        medicine_ref = user_document(
            user,
            "medicines",
            data["medicineId"],
        )

        schedule_ref = user_document(
            user,
            "schedules",
            data["scheduleId"],
        )

        client = firestore.client(
            app=get_firebase_app()
        )

        @google_firestore.transactional
        def create(transaction):
            medicine = medicine_ref.get(
                transaction=transaction
            )

            schedule = schedule_ref.get(
                transaction=transaction
            )

            medicine_data = medicine.to_dict() or {}
            schedule_data = schedule.to_dict() or {}

            if (
                not medicine.exists
                or medicine_data.get("deletedAt") is not None
            ):
                return False

            if (
                not schedule.exists
                or schedule_data.get("deletedAt") is not None
            ):
                return False

            if not schedule_data.get("enabled", False):
                return False

            if (
                schedule_data.get("medicineId")
                != data["medicineId"]
            ):
                return False

            if (
                schedule_data.get("version")
                != data["scheduleVersion"]
            ):
                return False

            scheduled_at = data.get("scheduledAt")

            effective_at = (
                schedule_data.get("updatedAt")
                or schedule_data.get("createdAt")
            )

            if (
                not isinstance(scheduled_at, datetime)
                or scheduled_at.utcoffset() is None
            ):
                return False

            if (
                not isinstance(effective_at, datetime)
                or effective_at.utcoffset() is None
            ):
                return False

            if scheduled_at < effective_at:
                return False

            transaction.create(reference, data)
            return True

        try:
            return create(client.transaction())
        except AlreadyExists:
            return False
    # ---------------------------------------------------------
    # MEDICINES
    # ---------------------------------------------------------

    @staticmethod
    def get_medicine(
        user: CurrentUser,
        medicine_id: str,
    ) -> dict[str, Any] | None:
        snapshot = user_document(
            user,
            "medicines",
            medicine_id,
        ).get()

        return _snapshot_to_dict(snapshot)

    # ---------------------------------------------------------
    # SCHEDULES
    # ---------------------------------------------------------

    @staticmethod
    def get_schedule(
        user: CurrentUser,
        schedule_id: str,
    ) -> dict[str, Any] | None:
        snapshot = user_document(
            user,
            "schedules",
            schedule_id,
        ).get()

        return _snapshot_to_dict(snapshot)

    @staticmethod
    def list_schedules(
        user: CurrentUser,
        enabled_only: bool = False,
    ) -> list[dict[str, Any]]:
        snapshots = user_collection(
            user,
            "schedules",
        ).stream()

        schedules = []

        for snapshot in snapshots:
            data = _snapshot_to_dict(snapshot)

            if data is None:
                continue

            if enabled_only and not data.get("enabled", False):
                continue

            schedules.append(data)

        return schedules

    @staticmethod
    def create_schedule(
        user: CurrentUser,
        schedule_id: str,
        data: dict[str, Any],
    ) -> dict[str, Any]:
        reference = user_document(
            user,
            "schedules",
            schedule_id,
        )

        reference.create(data)

        return {
            **data,
            "id": schedule_id,
        }

    @staticmethod
    def should_cancel_future_dose(
        dose_data: dict[str, Any],
        current_version: int,
        now: datetime,
    ) -> bool:
        """
        Chỉ hủy liều PENDING thuộc phiên bản hiện tại
        và có giờ uống nằm trong tương lai.
        """
        if dose_data.get("scheduleVersion") != current_version:
            return False

        if dose_data.get("status") != "PENDING":
            return False

        scheduled_at = dose_data.get("scheduledAt")

        if not isinstance(scheduled_at, datetime):
            return False

        if scheduled_at.tzinfo is None:
            scheduled_at = scheduled_at.replace(
                tzinfo=timezone.utc,
            )

        if now.tzinfo is None:
            now = now.replace(tzinfo=timezone.utc)

        return scheduled_at > now

    @staticmethod
    def update_schedule_if_version(
        user: CurrentUser,
        schedule_id: str,
        expected_version: int,
        data: dict[str, Any],
    ) -> dict[str, Any] | None | bool:
        """
        Cập nhật lịch và hủy liều tương lai của phiên bản cũ
        trong cùng transaction.

        None: không tìm thấy lịch.
        False: phiên bản không khớp.
        dict: cập nhật thành công.
        """
        schedule_ref = user_document(
            user,
            "schedules",
            schedule_id,
        )

        client = firestore.client(app=get_firebase_app())
        transaction = client.transaction()

        @google_firestore.transactional
        def _update(transaction: Any):
            schedule_snapshot = schedule_ref.get(
                transaction=transaction,
            )

            current_schedule = _snapshot_to_dict(
                schedule_snapshot,
            )

            if current_schedule is None:
                return None

            current_version = current_schedule.get("version", 1)

            if current_version != expected_version:
                return False

            doses_query = user_collection(
                user,
                "doses",
            ).where(
                filter=FieldFilter(
                    "scheduleId",
                    "==",
                    schedule_id,
                )
            )

            dose_snapshots = transaction.get(doses_query)
            now = datetime.now(timezone.utc)

            for dose_snapshot in dose_snapshots:
                dose_data = dose_snapshot.to_dict() or {}

                if not MedicationRepository.should_cancel_future_dose(
                    dose_data,
                    current_version,
                    now,
                ):
                    continue

                transaction.update(
                    dose_snapshot.reference,
                    {
                        "status": "CANCELLED",
                        "actionAt": now,
                        "updatedAt": now,
                    },
                )

            transaction.update(schedule_ref, data)

            return {
                **current_schedule,
                **data,
            }

        return _update(transaction)

    # ---------------------------------------------------------
    # DOSES
    # ---------------------------------------------------------

    @staticmethod
    def get_dose(
        user: CurrentUser,
        dose_id: str,
    ) -> dict[str, Any] | None:
        snapshot = user_document(
            user,
            "doses",
            dose_id,
        ).get()

        return _snapshot_to_dict(
            snapshot,
            id_field="doseId",
        )

    @staticmethod
    def list_doses(
        user: CurrentUser,
        local_date: date,
    ) -> list[dict[str, Any]]:
        snapshots = (
            user_collection(user, "doses")
            .where(
                filter=FieldFilter(
                    "localDate",
                    "==",
                    local_date.isoformat(),
                )
            )
            .stream()
        )

        doses = []

        for snapshot in snapshots:
            data = _snapshot_to_dict(
                snapshot,
                id_field="doseId",
            )

            if data is not None:
                doses.append(data)

        return sorted(
            doses,
            key=lambda dose: dose.get("minuteOfDay", 0),
        )

        @staticmethod
        def create_dose_if_absent(
            user: CurrentUser,
            dose_id: str,
            data: dict[str, Any],
        ) -> bool:
            reference = user_document(
                user,
                "doses",
                dose_id,
            )

            medicine_ref = user_document(
                user,
                "medicines",
                data["medicineId"],
            )

            schedule_ref = user_document(
                user,
                "schedules",
                data["scheduleId"],
            )

            client = firestore.client(
                app=get_firebase_app()
            )

            @google_firestore.transactional
            def create(transaction):
                medicine = medicine_ref.get(
                    transaction=transaction
                )

                schedule = schedule_ref.get(
                    transaction=transaction
                )

                medicine_data = medicine.to_dict() or {}
                schedule_data = schedule.to_dict() or {}

                if (
                    not medicine.exists
                    or medicine_data.get("deletedAt") is not None
                ):
                    return False

                if (
                    not schedule.exists
                    or schedule_data.get("deletedAt") is not None
                ):
                    return False

                if not schedule_data.get("enabled", False):
                    return False

                if (
                    schedule_data.get("medicineId")
                    != data["medicineId"]
                ):
                    return False

                if (
                    schedule_data.get("version")
                    != data["scheduleVersion"]
                ):
                    return False

                scheduled_at = data.get("scheduledAt")

                effective_at = (
                    schedule_data.get("updatedAt")
                    or schedule_data.get("createdAt")
                )

                if (
                    not isinstance(scheduled_at, datetime)
                    or scheduled_at.utcoffset() is None
                ):
                    return False

                if (
                    not isinstance(effective_at, datetime)
                    or effective_at.utcoffset() is None
                ):
                    return False

                # Không sinh liều trước thời điểm phiên bản
                # lịch này bắt đầu có hiệu lực.
                if scheduled_at < effective_at:
                    return False

                transaction.create(
                    reference,
                    data,
                )

                return True

            try:
                return create(client.transaction())

            except AlreadyExists:
                return False

    @staticmethod
    def update_dose_status(
        user: CurrentUser,
        dose_id: str,
        status: str,
    ) -> dict[str, Any] | None:
        """
        Chuyển PENDING sang TAKEN hoặc SKIPPED.

        Nếu gửi lại cùng hành động đã lưu,
        trả về kết quả hiện tại.
        """
        if status not in {"TAKEN", "SKIPPED"}:
            raise ValueError("Trạng thái Dose không hợp lệ.")

        reference = user_document(
            user,
            "doses",
            dose_id,
        )

        client = firestore.client(app=get_firebase_app())
        transaction = client.transaction()

        @google_firestore.transactional
        def _update(
            transaction: Any,
        ) -> dict[str, Any] | None:
            snapshot = reference.get(
                transaction=transaction,
            )

            current = _snapshot_to_dict(
                snapshot,
                id_field="doseId",
            )

            if current is None:
                return None

            current_status = current.get("status")

            if current_status == status:
                return current

            if current_status != "PENDING":
                raise ValueError(
                    "Không thể thay đổi trạng thái Dose đã kết thúc."
                )

            now = datetime.now(timezone.utc)

            updates = {
                "status": status,
                "actionAt": now,
                "updatedAt": now,
            }

            transaction.update(reference, updates)

            return {
                **current,
                **updates,
            }

        return _update(transaction)

    @staticmethod
    def snooze_dose_if_pending(
        user: CurrentUser,
        dose_id: str,
        minutes: int,
    ) -> dict[str, Any] | None:
        """Lưu thời điểm nhắc lại nếu liều vẫn PENDING."""
        reference = user_document(
            user,
            "doses",
            dose_id,
        )

        client = firestore.client(app=get_firebase_app())
        transaction = client.transaction()

        @google_firestore.transactional
        def _snooze(transaction: Any):
            snapshot = reference.get(
                transaction=transaction,
            )

            current = _snapshot_to_dict(
                snapshot,
                id_field="doseId",
            )

            if current is None:
                return None

            if current.get("status") != "PENDING":
                raise ValueError(
                    "Chỉ có thể snooze Dose đang ở trạng thái PENDING."
                )

            now = datetime.now(timezone.utc)

            updates = {
                "nextReminderAt": now + timedelta(minutes=minutes),
                "updatedAt": now,
            }

            transaction.update(reference, updates)

            return {
                **current,
                **updates,
            }

        return _snooze(transaction)