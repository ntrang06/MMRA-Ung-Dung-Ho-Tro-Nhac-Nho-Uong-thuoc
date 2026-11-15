import uuid
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
import pytest
from google.api_core.exceptions import GoogleAPICallError
from app.api.deps import CurrentUser
from app.core.firebase import get_firebase_app
from app.repositories.medication_repository import MedicationRepository
from app.repositories.user_scope import user_collection, user_document
from firebase_admin import firestore

@pytest.fixture
def test_user():
    uid = f"test-{uuid.uuid4().hex}"
    return CurrentUser(uid=uid)

@pytest.fixture
def firestore_db():
    app = get_firebase_app()
    return firestore.client(app=app)


def seed_schedule(user, schedule_id="schedule-001"):
    now = datetime.now(timezone.utc)

    data = {
        "scheduleId": schedule_id,
        "version": 1,
        "medicineId": "medicine-001",
        "doseQuantity": 1,
        "doseUnit": "TABLET",
        "times": [480, 1200],
        "weekdays": [1, 2, 3, 4, 5, 6, 7],
        "startDate": "2026-11-12",
        "endDate": None,
        "timeZone": "Asia/Ho_Chi_Minh",
        "enabled": True,
        "createdAt": now,
        "updatedAt": now,
    }

    user_document(user, "schedules", schedule_id).set(data)
    return data


def seed_dose(
    user,
    dose_id,
    schedule_version,
    status,
    scheduled_at,
):
    data = {
        "doseId": dose_id,
        "scheduleId": "schedule-001",
        "scheduleVersion": schedule_version,
        "medicineId": "medicine-001",
        "localDate": "2026-11-12",
        "minuteOfDay": 1200,
        "timeZone": "Asia/Ho_Chi_Minh",
        "scheduledAt": scheduled_at,
        "doseQuantity": 1,
        "doseUnit": "TABLET",
        "status": status,
        "createdAt": datetime.now(timezone.utc),
        "updatedAt": datetime.now(timezone.utc),
    }

    user_document(user, "doses", dose_id).set(data)
    return data


def test_update_schedule_cancels_only_future_pending_doses(
    test_user,
):
    now = datetime.now(timezone.utc)

    seed_schedule(test_user)

    # Dose đã uống: phải được giữ nguyên.
    seed_dose(
        test_user,
        "dose-taken",
        1,
        "TAKEN",
        now + timedelta(hours=2),
    )

    # Dose PENDING trong tương lai: phải bị hủy.
    seed_dose(
        test_user,
        "dose-future",
        1,
        "PENDING",
        now + timedelta(hours=3),
    )

    # Dose PENDING đã đến giờ: phải được giữ nguyên.
    seed_dose(
        test_user,
        "dose-past",
        1,
        "PENDING",
        now - timedelta(hours=1),
    )

    # Dose thuộc version khác: phải được giữ nguyên.
    seed_dose(
        test_user,
        "dose-old-version",
        0,
        "PENDING",
        now + timedelta(hours=4),
    )

    result = MedicationRepository.update_schedule_if_version(
        user=test_user,
        schedule_id="schedule-001",
        expected_version=1,
        data={
            "version": 2,
            "updatedAt": datetime.now(timezone.utc),
        },
    )

    assert result is not None
    assert result is not False

    schedule = MedicationRepository.get_schedule(
        test_user,
        "schedule-001",
    )

    assert schedule["version"] == 2

    doses = user_collection(test_user, "doses")

    assert doses.document("dose-taken").get().to_dict()["status"] == "TAKEN"
    assert doses.document("dose-future").get().to_dict()["status"] == "CANCELLED"
    assert doses.document("dose-past").get().to_dict()["status"] == "PENDING"
    assert doses.document("dose-old-version").get().to_dict()["status"] == "PENDING"


def test_stale_schedule_version_is_rejected(test_user):
    seed_schedule(test_user)

    result = MedicationRepository.update_schedule_if_version(
        user=test_user,
        schedule_id="schedule-001",
        expected_version=99,
        data={
            "version": 100,
            "updatedAt": datetime.now(timezone.utc),
        },
    )

    assert result is False

    schedule = MedicationRepository.get_schedule(
        test_user,
        "schedule-001",
    )

    assert schedule["version"] == 1


def test_two_concurrent_updates_only_one_succeeds(test_user):
    seed_schedule(test_user)

    def update(version):
        return MedicationRepository.update_schedule_if_version(
            user=test_user,
            schedule_id="schedule-001",
            expected_version=1,
            data={
                "version": 2,
                "updatedAt": datetime.now(timezone.utc),
                "testWriter": version,
            },
        )

    with ThreadPoolExecutor(max_workers=2) as executor:
        results = list(executor.map(update, [1, 2]))

    successful = [
        result
        for result in results
        if isinstance(result, dict)
    ]

    rejected = [
        result
        for result in results
        if result is False
    ]

    assert len(successful) == 1
    assert len(rejected) == 1

    schedule = MedicationRepository.get_schedule(
        test_user,
        "schedule-001",
    )

    assert schedule["version"] == 2


def test_failed_transaction_does_not_partially_update(test_user):
    now = datetime.now(timezone.utc)

    seed_schedule(test_user)

    seed_dose(
        test_user,
        "dose-future",
        1,
        "PENDING",
        now + timedelta(hours=3),
    )

    # object() không thể được Firestore serialize.
    # Transaction phải thất bại khi commit.
    with pytest.raises(Exception):
        MedicationRepository.update_schedule_if_version(
            user=test_user,
            schedule_id="schedule-001",
            expected_version=1,
            data={
                "version": 2,
                "invalidField": object(),
            },
        )

    schedule = MedicationRepository.get_schedule(
        test_user,
        "schedule-001",
    )

    dose = MedicationRepository.get_dose(
        test_user,
        "dose-future",
    )

    assert schedule["version"] == 1
    assert dose["status"] == "PENDING"

def test_overdue_pending_dose_is_marked_missed(test_user):
    now = datetime.now(timezone.utc)

    seed_dose(
        test_user,
        "dose-overdue",
        1,
        "PENDING",
        now - timedelta(minutes=31),
    )

    result = MedicationRepository.mark_dose_missed_if_overdue(
        user=test_user,
        dose_id="dose-overdue",
        now=now,
    )

    assert result is not None
    assert result["status"] == "MISSED"


def test_pending_dose_within_grace_period_is_preserved(test_user):
    now = datetime.now(timezone.utc)

    seed_dose(
        test_user,
        "dose-within-grace",
        1,
        "PENDING",
        now - timedelta(minutes=29),
    )

    result = MedicationRepository.mark_dose_missed_if_overdue(
        user=test_user,
        dose_id="dose-within-grace",
        now=now,
    )

    assert result is not None
    assert result["status"] == "PENDING"


def test_pending_dose_at_exact_grace_deadline_is_preserved(test_user):
    now = datetime.now(timezone.utc)

    seed_dose(
        test_user,
        "dose-exact-deadline",
        1,
        "PENDING",
        now - timedelta(minutes=30),
    )

    result = MedicationRepository.mark_dose_missed_if_overdue(
        user=test_user,
        dose_id="dose-exact-deadline",
        now=now,
    )

    assert result is not None
    assert result["status"] == "PENDING"


def test_snoozed_dose_uses_next_reminder_at(test_user):
    now = datetime.now(timezone.utc)

    data = seed_dose(
        test_user,
        "dose-snoozed",
        1,
        "PENDING",
        now - timedelta(hours=2),
    )

    data["nextReminderAt"] = now - timedelta(minutes=31)

    user_document(
        test_user,
        "doses",
        "dose-snoozed",
    ).set(data)

    result = MedicationRepository.mark_dose_missed_if_overdue(
        user=test_user,
        dose_id="dose-snoozed",
        now=now,
    )

    assert result is not None
    assert result["status"] == "MISSED"


def test_snoozed_dose_within_grace_period_is_preserved(test_user):
    now = datetime.now(timezone.utc)

    data = seed_dose(
        test_user,
        "dose-snoozed-within-grace",
        1,
        "PENDING",
        now - timedelta(hours=2),
    )

    data["nextReminderAt"] = now - timedelta(minutes=29)

    user_document(
        test_user,
        "doses",
        "dose-snoozed-within-grace",
    ).set(data)

    result = MedicationRepository.mark_dose_missed_if_overdue(
        user=test_user,
        dose_id="dose-snoozed-within-grace",
        now=now,
    )

    assert result is not None
    assert result["status"] == "PENDING"


@pytest.mark.parametrize(
    "status",
    ["TAKEN", "SKIPPED", "CANCELLED", "MISSED"],
)
def test_finalized_dose_status_is_preserved(test_user, status):
    now = datetime.now(timezone.utc)

    seed_dose(
        test_user,
        f"dose-{status.lower()}",
        1,
        status,
        now - timedelta(hours=2),
    )

    result = MedicationRepository.mark_dose_missed_if_overdue(
        user=test_user,
        dose_id=f"dose-{status.lower()}",
        now=now,
    )

    assert result is not None
    assert result["status"] == status
