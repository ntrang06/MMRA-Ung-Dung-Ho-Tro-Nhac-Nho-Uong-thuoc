from datetime import datetime, timedelta, timezone
from app.repositories.medication_repository import MedicationRepository

def test_taken_dose_must_be_preserved():
    now = datetime.now(timezone.utc)
    dose = {
        "scheduleVersion": 1,
        "status": "TAKEN",
        "scheduledAt": now + timedelta(hours=2),
    }
    assert MedicationRepository.should_cancel_future_dose(
        dose,
        current_version=1,
        now=now,
    ) is False

def test_future_pending_dose_must_be_cancelled():
    now = datetime.now(timezone.utc)
    dose = {
        "scheduleVersion": 1,
        "status": "PENDING",
        "scheduledAt": now + timedelta(hours=2),
    }
    assert MedicationRepository.should_cancel_future_dose(
        dose,
        current_version=1,
        now=now,
    ) is True

def test_past_pending_dose_must_be_preserved():
    now = datetime.now(timezone.utc)
    dose = {
        "scheduleVersion": 1,
        "status": "PENDING",
        "scheduledAt": now - timedelta(hours=2),
    }
    assert MedicationRepository.should_cancel_future_dose(
        dose,
        current_version=1,
        now=now,
    ) is False

def test_current_time_pending_dose_must_be_preserved():
    now = datetime.now(timezone.utc)
    dose = {
        "scheduleVersion": 1,
        "status": "PENDING",
        "scheduledAt": now,
    }
    assert MedicationRepository.should_cancel_future_dose(
        dose,
        current_version=1,
        now=now,
    ) is False

def test_old_schedule_version_dose_must_be_preserved():
    now = datetime.now(timezone.utc)
    dose = {
        "scheduleVersion": 0,
        "status": "PENDING",
        "scheduledAt": now + timedelta(hours=2),
    }
    assert MedicationRepository.should_cancel_future_dose(
        dose,
        current_version=1,
        now=now,
    ) is False

def test_skipped_dose_must_be_preserved():
    now = datetime.now(timezone.utc)
    dose = {
        "scheduleVersion": 1,
        "status": "SKIPPED",
        "scheduledAt": now + timedelta(hours=2),
    }
    assert MedicationRepository.should_cancel_future_dose(
        dose,
        current_version=1,
        now=now,
    ) is False

def test_missed_dose_must_be_preserved():
    now = datetime.now(timezone.utc)
    dose = {
        "scheduleVersion": 1,
        "status": "MISSED",
        "scheduledAt": now - timedelta(hours=1),
    }
    assert MedicationRepository.should_cancel_future_dose(
        dose,
        current_version=1,
        now=now,
    ) is False

def test_dose_without_scheduled_at_must_be_preserved():
    now = datetime.now(timezone.utc)
    dose = {
        "scheduleVersion": 1,
        "status": "PENDING",
    }
    assert MedicationRepository.should_cancel_future_dose(
        dose,
        current_version=1,
        now=now,
    ) is False

def test_naive_datetime_is_treated_as_utc():
    now = datetime.now(timezone.utc)
    dose = {
        "scheduleVersion": 1,
        "status": "PENDING",
        "scheduledAt": (
            now + timedelta(hours=2)
        ).replace(tzinfo=None),
    }
    assert MedicationRepository.should_cancel_future_dose(
        dose,
        current_version=1,
        now=now,
    ) is True
