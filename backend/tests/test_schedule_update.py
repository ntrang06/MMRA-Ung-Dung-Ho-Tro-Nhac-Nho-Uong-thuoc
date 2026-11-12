from datetime import datetime, timedelta, timezone
from types import SimpleNamespace
from unittest.mock import Mock

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient
from google.api_core.exceptions import ServiceUnavailable

from app.api.deps import CurrentUser, get_current_user
from app.api.v1.endpoints import schedules
from app.services import schedule_service as service


@pytest.fixture
def setup(monkeypatch):
    app = FastAPI()

    app.include_router(
        schedules.router,
        prefix="/api/v1",
    )

    app.dependency_overrides[get_current_user] = (
        lambda: CurrentUser("user-A")
    )

    now = datetime.now(timezone.utc)

    body = {
        "medicineId": "medicine-A",
        "doseQuantity": "1",
        "doseUnit": "TABLET",
        "times": [1200, 480],
        "weekdays": [7, 1],
        "startDate": "2026-11-12",
        "expectedVersion": 1,
    }

    old = {
        **{
            key: value
            for key, value in body.items()
            if key != "expectedVersion"
        },
        "version": 1,
        "createdAt": now - timedelta(days=2),
        "updatedAt": now - timedelta(days=1),
    }

    schedule = SimpleNamespace(
        id="schedule-A",
        exists=True,
        to_dict=lambda: old,
    )

    medicine_data = {}

    medicine = SimpleNamespace(
        exists=True,
        to_dict=lambda: medicine_data,
    )

    refs = []
    events = []

    def document(user, collection, document_id):
        refs.append(
            (user.uid, collection, document_id)
        )

        def get(**kwargs):
            events.append("read")

            if collection == "schedules":
                return schedule

            return medicine

        return SimpleNamespace(
            path=(
                f"users/{user.uid}/"
                f"{collection}/{document_id}"
            ),
            get=get,
        )

    doses = []

    for status in [
        "PENDING",
        "TAKEN",
        "SKIPPED",
        "MISSED",
        "CANCELLED",
    ]:
        data = {
            "status": status,
            "scheduledAt": now + timedelta(days=1),
        }

        doses.append(
            SimpleNamespace(
                reference=f"dose-{status}",
                to_dict=lambda data=data: data,
            )
        )

    doses.append(
        SimpleNamespace(
            reference="past-pending",
            to_dict=lambda: {
                "status": "PENDING",
                "scheduledAt": now - timedelta(days=1),
            },
        )
    )

    def stream(**kwargs):
        events.append("read")
        return iter(doses)

    query = Mock()
    query.where.return_value = query
    query.stream.side_effect = stream

    transaction = Mock()

    transaction.update.side_effect = (
        lambda *args: events.append("write")
    )

    scopes = []

    def collection(user, name):
        scopes.append((user.uid, name))
        return query

    monkeypatch.setattr(
        service,
        "user_document",
        document,
    )

    monkeypatch.setattr(
        service,
        "user_collection",
        collection,
    )

    monkeypatch.setattr(
        service,
        "get_firebase_app",
        lambda: None,
    )

    # Mô phỏng callback giao dịch, không gọi Firebase thật.
    monkeypatch.setattr(
        service.firestore,
        "transactional",
        lambda fn: fn,
    )

    monkeypatch.setattr(
        service.firestore,
        "client",
        lambda **kwargs: SimpleNamespace(
            transaction=lambda: transaction
        ),
    )

    with TestClient(app) as client:
        yield SimpleNamespace(
            client=client,
            app=app,
            body=body,
            old=old,
            schedule=schedule,
            medicine=medicine,
            medicine_data=medicine_data,
            refs=refs,
            scopes=scopes,
            query=query,
            transaction=transaction,
            events=events,
        )


def test_update_keeps_id_history_and_cancels_only_future_pending(
    setup,
):
    s = setup

    response = s.client.put(
        "/api/v1/schedules/schedule-A?uid=user-B",
        json=s.body,
    )

    assert response.status_code == 200

    result = response.json()

    assert result["scheduleId"] == "schedule-A"
    assert result["version"] == 2
    assert result["times"] == [480, 1200]
    assert result["weekdays"] == [1, 7]

    assert (
        datetime.fromisoformat(result["createdAt"])
        == s.old["createdAt"]
    )

    assert (
        datetime.fromisoformat(result["updatedAt"])
        > s.old["updatedAt"]
    )

    assert response.headers["cache-control"] == "no-store"

    assert s.refs == [
        ("user-A", "schedules", "schedule-A"),
        ("user-A", "medicines", "medicine-A"),
    ]

    assert s.scopes == [
        ("user-A", "doses"),
    ]

    query_filter = (
        s.query.where.call_args.kwargs["filter"]
    )

    assert query_filter.field_path == "scheduleId"
    assert query_filter.value == "schedule-A"

    # Mọi thao tác đọc phải hoàn tất trước khi ghi.
    assert s.events == [
        "read",
        "read",
        "read",
        "write",
        "write",
    ]

    calls = s.transaction.update.call_args_list
    stored = calls[0].args[1]

    assert stored["version"] == 2
    assert stored["startDate"] == "2026-11-12"

    assert "expectedVersion" not in stored
    assert "createdAt" not in stored
    assert "uid" not in stored

    assert calls[1].args == (
        "dose-PENDING",
        {
            "status": "CANCELLED",
            "nextReminderAt": None,
            "updatedAt": stored["updatedAt"],
        },
    )

    # Chỉ sửa lịch và hủy một liều tương lai.
    # Các liều còn lại không bị ghi đè.
    assert len(calls) == 2


def test_stale_version_returns_409_without_writes(setup):
    setup.old["version"] = 2

    response = setup.client.put(
        "/api/v1/schedules/schedule-A",
        json=setup.body,
    )

    assert response.status_code == 409

    assert (
        response.json()["detail"]["code"]
        == "version_conflict"
    )

    setup.transaction.update.assert_not_called()
    setup.query.stream.assert_not_called()


@pytest.mark.parametrize("deleted", [False, True])
def test_missing_or_deleted_schedule_returns_404(
    setup,
    deleted,
):
    if deleted:
        setup.old["deletedAt"] = datetime.now(timezone.utc)
    else:
        setup.schedule.exists = False

    response = setup.client.put(
        "/api/v1/schedules/schedule-A",
        json=setup.body,
    )

    assert response.status_code == 404

    assert (
        response.json()["detail"]["code"]
        == "schedule_not_found"
    )

    setup.transaction.update.assert_not_called()


@pytest.mark.parametrize("deleted", [False, True])
def test_missing_or_deleted_medicine_returns_404(
    setup,
    deleted,
):
    if deleted:
        setup.medicine_data["deletedAt"] = (
            datetime.now(timezone.utc)
        )
    else:
        setup.medicine.exists = False

    response = setup.client.put(
        "/api/v1/schedules/schedule-A",
        json=setup.body,
    )

    assert response.status_code == 404

    assert (
        response.json()["detail"]["code"]
        == "medicine_not_found"
    )

    setup.transaction.update.assert_not_called()


@pytest.mark.parametrize(
    "invalid",
    ["missing-version", "uid"],
)
def test_invalid_update_returns_422(setup, invalid):
    if invalid == "missing-version":
        setup.body.pop("expectedVersion")
    else:
        setup.body["uid"] = "user-B"

    response = setup.client.put(
        "/api/v1/schedules/schedule-A",
        json=setup.body,
    )

    assert response.status_code == 422
    setup.transaction.update.assert_not_called()


def test_update_requires_login(setup):
    setup.app.dependency_overrides.clear()

    response = setup.client.put(
        "/api/v1/schedules/schedule-A",
        json=setup.body,
    )

    assert response.status_code == 401
    setup.transaction.update.assert_not_called()


@pytest.mark.parametrize(
    "operation",
    ["read", "write"],
)
def test_firestore_failure_returns_503(setup, operation):
    if operation == "read":
        target = setup.query.stream
    else:
        target = setup.transaction.update

    target.side_effect = ServiceUnavailable("offline")

    response = setup.client.put(
        "/api/v1/schedules/schedule-A",
        json=setup.body,
    )

    assert response.status_code == 503

    if operation == "read":
        setup.transaction.update.assert_not_called()


def test_pause_schedule_also_cancels_future_pending(setup):
    setup.body["enabled"] = False

    response = setup.client.put(
        "/api/v1/schedules/schedule-A",
        json=setup.body,
    )

    assert response.status_code == 200
    assert response.json()["enabled"] is False

    assert (
        setup.transaction.update.call_args_list[1].args[0]
        == "dose-PENDING"
    )