from google.cloud import firestore as google_firestore
from datetime import date, datetime, timezone
from typing import Any
from datetime import date, datetime, timedelta, timezone
from firebase_admin import firestore
from google.api_core.exceptions import AlreadyExists
from google.cloud.firestore_v1.base_query import FieldFilter
from app.api.deps import CurrentUser
from app.core.firebase import get_firebase_app
from app.repositories.user_scope import user_collection, user_document


def _snapshot_to_dict(snapshot: Any) -> dict[str, Any] | None:
    """Chuyển Firestore DocumentSnapshot thành dictionary."""
    if not snapshot.exists:
        return None

    data = snapshot.to_dict() or {}
    data.setdefault("id", snapshot.id)
    return data


class MedicationRepository:
    """Repository quản lý medicines, schedules và doses trong Firestore."""

    # ---------------------------------------------------------
    # MEDICINES
    # ---------------------------------------------------------

    @staticmethod
    def get_medicine(
        user: CurrentUser,
        medicine_id: str,
    ) -> dict[str, Any] | None:
        """Lấy thông tin một thuốc thuộc tài khoản hiện tại."""
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
        """Lấy một lịch uống thuốc."""
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
        """Lấy danh sách lịch uống thuốc của người dùng."""
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
        """Tạo một lịch uống thuốc mới."""
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
        Xác định Dose có phải chuyển sang CANCELLED không.

        Chỉ hủy khi:
        - Dose thuộc version hiện tại của Schedule.
        - Dose đang PENDING.
        - scheduledAt hợp lệ và nằm trong tương lai.
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
        Cập nhật Schedule và hủy các Dose PENDING thuộc version cũ
        có scheduledAt nằm trong tương lai, trong cùng transaction.

        Trả về:
        - None: không tìm thấy Schedule.
        - False: expected_version không khớp.
        - dict: cập nhật thành công.
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
            # 1. Đọc Schedule hiện tại.
            schedule_snapshot = schedule_ref.get(
                transaction=transaction,
            )

            current_schedule = _snapshot_to_dict(
                schedule_snapshot,
            )

            if current_schedule is None:
                return None

            current_version = current_schedule.get("version", 1)

            # 2. Kiểm tra optimistic locking.
            if current_version != expected_version:
                return False

            # 3. Lấy các Dose thuộc Schedule và version cũ.
            doses_query = (
                user_collection(user, "doses")
                .where(
                    filter=FieldFilter(
                        "scheduleId",
                        "==",
                        schedule_id,
                    )
                )
            )

            dose_snapshots = transaction.get(doses_query)

            now = datetime.now(timezone.utc)
            cancelled_count = 0

            # 4. Chỉ hủy Dose PENDING có giờ uống trong tương lai.
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

                cancelled_count += 1

            # 5. Cập nhật Schedule trong cùng transaction.
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
        """Lấy thông tin một liều thuốc."""
        snapshot = user_document(
            user,
            "doses",
            dose_id,
        ).get()

        return _snapshot_to_dict(snapshot)

    @staticmethod
    def list_doses(
        user: CurrentUser,
        local_date: date,
    ) -> list[dict[str, Any]]:
        """Lấy các liều thuốc của một ngày theo múi giờ địa phương."""
        snapshots = (
            user_collection(user, "doses")
            .where("localDate", "==", local_date.isoformat())
            .stream()
        )

        doses = []

        for snapshot in snapshots:
            data = _snapshot_to_dict(snapshot)

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
        """
        Tạo Dose nếu chưa tồn tại.

        DocumentReference.create() đảm bảo không ghi đè document
        đã tồn tại. Trả về True nếu tạo mới, False nếu đã tồn tại.
        """
        reference = user_document(
            user,
            "doses",
            dose_id,
        )

        try:
            reference.create(data)
            return True

        except AlreadyExists:
            return False

    @staticmethod
    def update_dose_status(
        user: CurrentUser,
        dose_id: str,
        status: str,
    ) -> dict[str, Any] | None:
        """
        Cập nhật trạng thái Dose một cách nguyên tử.

        Chỉ cho phép chuyển từ PENDING sang TAKEN hoặc SKIPPED.
        Nếu trạng thái đã được cập nhật thành cùng một giá trị,
        trả lại dữ liệu hiện tại để hỗ trợ request lặp lại.
        """
        allowed_statuses = {"TAKEN", "SKIPPED"}

        if status not in allowed_statuses:
            raise ValueError("Trạng thái Dose không hợp lệ.")

        reference = user_document(
            user,
            "doses",
            dose_id,
        )

        client = firestore.client(app=get_firebase_app())
        transaction = client.transaction()
        @google_firestore.transactional
        def _update(transaction: Any) -> dict[str, Any] | None:
            snapshot = reference.get(transaction=transaction)

            current = _snapshot_to_dict(snapshot)

            if current is None:
                return None

            current_status = current.get("status")

            # Request lặp lại cùng một hành động: không cập nhật lại.
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
        """
        Cập nhật nextReminderAt bằng transaction.

        Chỉ cho phép snooze khi Dose vẫn ở trạng thái PENDING.
        """
        reference = user_document(
            user,
            "doses",
            dose_id,
        )

        client = firestore.client(app=get_firebase_app())
        transaction = client.transaction()

        @google_firestore.transactional
        def _snooze(transaction: Any):
            snapshot = reference.get(transaction=transaction)

            current = _snapshot_to_dict(snapshot)

            if current is None:
                return None

            if current.get("status") != "PENDING":
                raise ValueError(
                    "Chỉ có thể snooze Dose đang ở trạng thái PENDING."
                )

            now = datetime.now(timezone.utc)
            next_reminder_at = now + timedelta(minutes=minutes)

            updates = {
                "nextReminderAt": next_reminder_at,
                "updatedAt": now,
            }

            transaction.update(reference, updates)

            return {
                **current,
                **updates,
            }

        return _snooze(transaction)

