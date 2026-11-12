from datetime import date
from unittest.mock import MagicMock

import pytest

from app.services.dose_service import DoseService


@pytest.fixture
def user():
    return MagicMock(uid="test-user")


def test_schedule_is_active_on_configured_weekday():
    schedule = {
        "enabled": True,
        "startDate": "2026-11-09",
        "endDate": "2026-11-30",
        "weekdays": [1, 2, 3, 4, 5],
    }

    # 12/11/2026 là thứ Năm, weekday = 4.
    assert DoseService._validate_schedule_date(
        schedule,
        date(2026, 11, 12),
    ) is True


def test_schedule_is_inactive_on_unconfigured_weekday():
    schedule = {
        "enabled": True,
        "startDate": "2026-11-09",
        "endDate": "2026-11-30",
        "weekdays": [1, 2, 3],
    }

    assert DoseService._validate_schedule_date(
        schedule,
        date(2026, 11, 12),
    ) is False


def test_disabled_schedule_is_not_active():
    schedule = {
        "enabled": False,
        "startDate": "2026-11-09",
        "endDate": None,
        "weekdays": [1, 2, 3, 4, 5, 6, 7],
    }

    assert DoseService._validate_schedule_date(
        schedule,
        date(2026, 11, 12),
    ) is False


def test_schedule_outside_date_range_is_not_active():
    schedule = {
        "enabled": True,
        "startDate": "2026-11-13",
        "endDate": "2026-11-30",
        "weekdays": [1, 2, 3, 4, 5, 6, 7],
    }

    assert DoseService._validate_schedule_date(
        schedule,
        date(2026, 11, 12),
    ) is False


def test_generate_doses_creates_one_dose_per_time(
    user,
    monkeypatch,
):
    schedule = {
        "id": "schedule-001",
        "version": 1,
        "medicineId": "medicine-001",
        "doseQuantity": 1,
        "doseUnit": "TABLET",
        "times": [480, 720],
        "weekdays": [1, 2, 3, 4, 5, 6, 7],
        "startDate": "2026-11-12",
        "endDate": None,
        "timeZone": "Asia/Ho_Chi_Minh",
        "mealInstruction": "AFTER_MEAL",
        "enabled": True,
    }

    monkeypatch.setattr(
        "app.services.dose_service.MedicationRepository.list_schedules",
        lambda *args, **kwargs: [schedule],
    )

    monkeypatch.setattr(
        "app.services.dose_service.MedicationRepository.get_medicine",
        lambda *args, **kwargs: {
            "medicineName": "Vitamin C",
            "strength": "500",
            "strengthUnit": "mg",
            "dosageForm": "TABLET",
        },
    )

    created_ids = []

    def create_dose(user, dose_id, data):
        created_ids.append(dose_id)
        return True

    monkeypatch.setattr(
        "app.services.dose_service.MedicationRepository.create_dose_if_absent",
        create_dose,
    )

    def get_dose(user, dose_id):
        return {
            "doseId": dose_id,
            "minuteOfDay": 480 if len(created_ids) == 1 else 720,
            "status": "PENDING",
        }

    monkeypatch.setattr(
        "app.services.dose_service.MedicationRepository.get_dose",
        get_dose,
    )

    result = DoseService.generate_doses_for_date(
        user,
        date(2026, 11, 12),
    )

    assert len(result) == 2
    assert len(created_ids) == 2
    assert result[0]["status"] == "PENDING"


def test_snooze_rejects_invalid_minutes(user):
    with pytest.raises(ValueError):
        DoseService.snooze_dose(
            user,
            "dose-001",
            7,
        )


def test_snooze_rejects_boolean_minutes(user):
    with pytest.raises(ValueError):
        DoseService.snooze_dose(
            user,
            "dose-001",
            True,
        )


def test_snooze_rejects_completed_dose(user, monkeypatch):
    monkeypatch.setattr(
        "app.services.dose_service.MedicationRepository.snooze_dose_if_pending",
        lambda **kwargs: (_ for _ in ()).throw(
            ValueError("Chỉ có thể snooze Dose đang ở trạng thái PENDING.")
        ),
    )

    with pytest.raises(ValueError):
        DoseService.snooze_dose(
            user,
            "dose-001",
            10,
        )
