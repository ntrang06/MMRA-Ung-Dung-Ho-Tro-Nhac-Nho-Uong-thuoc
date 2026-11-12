from datetime import date, datetime, time, timedelta, timezone
from typing import Any
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

from app.api.deps import CurrentUser
from app.core.dose_identity import make_dose_id
from app.repositories.medication_repository import MedicationRepository
from app.repositories.user_scope import user_document


class DoseService:
    """Xử lý nghiệp vụ lịch uống thuốc và các liều thuốc."""

    DEFAULT_TIME_ZONE = "Asia/Ho_Chi_Minh"
    ALLOWED_SNOOZE_MINUTES = {5, 10, 15, 30}

    # ---------------------------------------------------------
    # HELPERS
    # ---------------------------------------------------------

    @staticmethod
    def _parse_date(value: Any) -> date:
        """Chuyển giá trị ngày từ Firestore hoặc schema thành date."""
        if isinstance(value, datetime):
            return value.date()

        if isinstance(value, date):
            return value

        if isinstance(value, str):
            try:
                return date.fromisoformat(value)
            except ValueError as exc:
                raise ValueError("Ngày không hợp lệ.") from exc

        raise ValueError("Không thể chuyển giá trị thành ngày.")

    @staticmethod
    def _get_timezone(time_zone: str) -> ZoneInfo:
        """Lấy múi giờ từ tên timezone."""
        try:
            return ZoneInfo(time_zone)
        except ZoneInfoNotFoundError as exc:
            raise ValueError("Múi giờ không hợp lệ.") from exc

    @staticmethod
    def _validate_schedule_date(
        schedule: dict[str, Any],
        target_date: date,
    ) -> bool:
        """Kiểm tra lịch có hiệu lực trong ngày được yêu cầu không."""
        if not schedule.get("enabled", False):
            return False

        start_date = DoseService._parse_date(
            schedule.get("startDate")
        )

        end_date_value = schedule.get("endDate")
        end_date = (
            DoseService._parse_date(end_date_value)
            if end_date_value
            else None
        )

        if target_date < start_date:
            return False

        if end_date is not None and target_date > end_date:
            return False

        # Python: Monday = 1, Sunday = 7.
        weekday = target_date.isoweekday()
        weekdays = schedule.get("weekdays", [])

        return weekday in weekdays

    @staticmethod
    def _get_medicine_snapshot(
        medicine: dict[str, Any],
    ) -> dict[str, Any]:
        """Tạo bản chụp thông tin thuốc tại thời điểm sinh Dose."""
        return {
            "medicineName": medicine.get("medicineName", ""),
            "strength": medicine.get("strength", ""),
            "strengthUnit": medicine.get("strengthUnit", ""),
            "dosageForm": medicine.get("dosageForm", ""),
        }

    # ---------------------------------------------------------
    # GENERATE DOSES
    # ---------------------------------------------------------

    @staticmethod
    def generate_doses_for_date(
        user: CurrentUser,
        target_date: date,
    ) -> list[dict[str, Any]]:
        """
        Sinh Dose cho một ngày.

        Mỗi Dose được định danh bằng:
        scheduleId + scheduleVersion + localDate + minuteOfDay.

        Việc tạo Dose có tính idempotent: gọi nhiều lần cũng không
        tạo thêm document trùng lặp hoặc ghi đè Dose đã tồn tại.
        """
        schedules = MedicationRepository.list_schedules(
            user,
            enabled_only=True,
        )

        generated_doses = []

        for schedule in schedules:
            if not DoseService._validate_schedule_date(
                schedule,
                target_date,
            ):
                continue

            schedule_id = schedule.get(
                "scheduleId",
                schedule.get("id"),
            )

            if not schedule_id:
                continue

            schedule_version = schedule.get("version", 1)

            medicine_id = schedule.get("medicineId")

            if not medicine_id:
                continue

            medicine = MedicationRepository.get_medicine(
                user,
                medicine_id,
            )

            if medicine is None:
                continue

            time_zone_name = schedule.get(
                "timeZone",
                DoseService.DEFAULT_TIME_ZONE,
            )

            zone = DoseService._get_timezone(time_zone_name)

            times = schedule.get("times", [])

            for minute_of_day in times:
                if (
                    not isinstance(minute_of_day, int)
                    or isinstance(minute_of_day, bool)
                    or not 0 <= minute_of_day <= 1439
                ):
                    continue

                hour = minute_of_day // 60
                minute = minute_of_day % 60

                scheduled_at = datetime.combine(
                    target_date,
                    time(hour=hour, minute=minute),
                    tzinfo=zone,
                )

                dose_id = make_dose_id(
                    schedule_id=schedule_id,
                    schedule_version=schedule_version,
                    local_date=target_date,
                    minute_of_day=minute_of_day,
                )

                now = datetime.now(timezone.utc)

                dose_data = {
                    "doseId": dose_id,
                    "scheduleId": schedule_id,
                    "scheduleVersion": schedule_version,
                    "medicineId": medicine_id,
                    "localDate": target_date.isoformat(),
                    "minuteOfDay": minute_of_day,
                    "timeZone": time_zone_name,
                    "scheduledAt": scheduled_at,
                    "doseQuantity": schedule.get("doseQuantity"),
                    "doseUnit": schedule.get("doseUnit"),
                    "mealInstruction": schedule.get(
                        "mealInstruction",
                        "",
                    ),
                    "medicineSnapshot": (
                        DoseService._get_medicine_snapshot(medicine)
                    ),
                    "status": "PENDING",
                    "actionAt": None,
                    "nextReminderAt": None,
                    "createdAt": now,
                    "updatedAt": now,
                }

                created = MedicationRepository.create_dose_if_absent(
                    user,
                    dose_id,
                    dose_data,
                )

                # Đọc lại dữ liệu hiện có để không trả về dữ liệu
                # mới sinh nếu Dose đã tồn tại và đã được cập nhật.
                existing_dose = MedicationRepository.get_dose(
                    user,
                    dose_id,
                )

                if existing_dose is not None:
                    generated_doses.append(existing_dose)

        return sorted(
            generated_doses,
            key=lambda dose: dose.get("minuteOfDay", 0),
        )

    # ---------------------------------------------------------
    # GET DAILY DOSES
    # ---------------------------------------------------------

    @staticmethod
    def get_doses_for_date(
        user: CurrentUser,
        target_date: date,
    ) -> list[dict[str, Any]]:
        """
        Lấy Dose của một ngày.

        Trước khi đọc danh sách, hệ thống sinh các Dose còn thiếu
        từ những lịch uống thuốc đang có hiệu lực.
        """
        DoseService.generate_doses_for_date(
            user,
            target_date,
        )

        return MedicationRepository.list_doses(
            user,
            target_date,
        )

    # ---------------------------------------------------------
    # TAKE / SKIP DOSE
    # ---------------------------------------------------------

    @staticmethod
    def update_dose_action(
        user: CurrentUser,
        dose_id: str,
        action: str,
    ) -> dict[str, Any]:
        """Đánh dấu một Dose đã uống hoặc bỏ qua."""
        if action not in {"TAKEN", "SKIPPED"}:
            raise ValueError(
                "Hành động chỉ được phép là TAKEN hoặc SKIPPED."
            )

        dose = MedicationRepository.update_dose_status(
            user,
            dose_id,
            action,
        )

        if dose is None:
            raise LookupError("Không tìm thấy Dose.")

        return dose

    # ---------------------------------------------------------
    # SNOOZE DOSE
    # ---------------------------------------------------------


    @staticmethod
    def snooze_dose(
        user: CurrentUser,
        dose_id: str,
        minutes: int,
    ) -> dict[str, Any]:
        """Hoãn nhắc uống thuốc một cách an toàn."""

        if (
            not isinstance(minutes, int)
            or isinstance(minutes, bool)
            or minutes not in DoseService.ALLOWED_SNOOZE_MINUTES
        ):
            raise ValueError(
                "Thời gian snooze phải là 5, 10, 15 hoặc 30 phút."
            )

        dose = MedicationRepository.snooze_dose_if_pending(
            user=user,
            dose_id=dose_id,
            minutes=minutes,
        )

        if dose is None:
            raise LookupError("Không tìm thấy Dose.")

        return dose
