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
    app.include_router(schedules.router, prefix="/api/v1")

    app.dependency_overrides[get_current_user] = (
        lambda: CurrentUser("user-A")
    )

    now = datetime.now(timezone.utc)

    old = {
        "medicineId": "medicine-A",
        "doseQuantity": "1",
        "doseUnit": "TABLET",
        "times": [480],
        "weekdays": [1],
        "startDate": "2026-11-12",
        "version": 2,
        "createdAt": now - timedelta(days=2),
        "updatedAt": now - timedelta(days=1),
    }

    snapshot = SimpleNamespace(
        id="schedule-A",
        exists=True,
        to_dict=lambda: old,
    )

    events = []

    def read(**kwargs):
        events.append("read")
        return snapshot

    reference = SimpleNamespace(get=read)
    document = Mock(return_value=reference)
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

    collection = Mock(return_value=query)
    transaction = Mock()

    transaction.update.side_effect = (
        lambda *args: events.append("write")
    )

    monkeypatch.setattr(service, "user_document", document)
    monkeypatch.setattr(service, "user_collection", collection)
    monkeypatch.setattr(service, "get_firebase_app", lambda: None)

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
            old=old,
            snapshot=snapshot,
            reference=reference,
            document=document,
            collection=collection,
            doses=doses,
            query=query,
            transaction=transaction,
            events=events,
        )


def test_soft_delete_keeps_history_and_uses_token_uid(setup):
    s = setup

    response = s.client.delete(
        "/api/v1/schedules/schedule-A"
        "?expectedVersion=2&uid=user-B"
    )

    assert response.status_code == 204
    assert response.content == b""
    assert response.headers["cache-control"] == "no-store"

    s.document.assert_called_once_with(
        CurrentUser("user-A"),
        "schedules",
        "schedule-A",
    )

    s.collection.assert_called_once_with(
        CurrentUser("user-A"),
        "doses",
    )

    query_filter = s.query.where.call_args.kwargs["filter"]

    assert query_filter.field_path == "scheduleId"
    assert query_filter.value == "schedule-A"

    assert s.events == ["read", "read", "write", "write"]

    calls = s.transaction.update.call_args_list

    # Chỉ cập nhật lịch và một liều PENDING tương lai.
    # Các liều quá khứ và trạng thái đã ghi nhận giữ nguyên.
    assert len(calls) == 2

    reference, stored = calls[0].args

    assert reference is s.reference

    assert set(stored) == {
        "enabled",
        "deletedAt",
        "updatedAt",
        "version",
    }

    assert stored["enabled"] is False
    assert stored["version"] == 3
    assert stored["deletedAt"] == stored["updatedAt"]
    assert stored["deletedAt"].utcoffset() == timedelta(0)

    assert calls[1].args == (
        "dose-PENDING",
        {
            "status": "CANCELLED",
            "nextReminderAt": None,
            "updatedAt": stored["updatedAt"],
        },
    )

    # Không xóa document thật.
    s.transaction.delete.assert_not_called()


def test_stale_version_returns_409(setup):
    response = setup.client.delete(
        "/api/v1/schedules/schedule-A?expectedVersion=1"
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
        setup.snapshot.exists = False

    response = setup.client.delete(
        "/api/v1/schedules/schedule-A?expectedVersion=2"
    )

    assert response.status_code == 404

    assert (
        response.json()["detail"]["code"]
        == "schedule_not_found"
    )

    setup.transaction.update.assert_not_called()


@pytest.mark.parametrize(
    "suffix",
    [
        "",
        "?expectedVersion=0",
        "?expectedVersion=abc",
    ],
)
def test_invalid_version_returns_422(setup, suffix):
    response = setup.client.delete(
        "/api/v1/schedules/schedule-A" + suffix
    )

    assert response.status_code == 422
    setup.transaction.update.assert_not_called()


def test_delete_requires_login(setup):
    setup.app.dependency_overrides.clear()

    response = setup.client.delete(
        "/api/v1/schedules/schedule-A?expectedVersion=2"
    )

    assert response.status_code == 401
    setup.transaction.update.assert_not_called()


@pytest.mark.parametrize("operation", ["read", "write"])
def test_firestore_failure_returns_503(setup, operation):
    if operation == "read":
        target = setup.query.stream
    else:
        target = setup.transaction.update

    target.side_effect = ServiceUnavailable("offline")

    response = setup.client.delete(
        "/api/v1/schedules/schedule-A?expectedVersion=2"
    )

    assert response.status_code == 503

    if operation == "read":
        setup.transaction.update.assert_not_called()


def test_schedule_without_doses_can_be_deleted(setup):
    setup.doses.clear()

    response = setup.client.delete(
        "/api/v1/schedules/schedule-A?expectedVersion=2"
    )

    assert response.status_code == 204
    setup.transaction.update.assert_called_once()


def test_invalid_pending_timestamp_prevents_all_writes(setup):
    setup.doses[0].to_dict = lambda: {
        "status": "PENDING",
        "scheduledAt": "not-a-timestamp",
    }

    with pytest.raises(ValueError, match="scheduledAt"):
        setup.client.delete(
            "/api/v1/schedules/schedule-A?expectedVersion=2"
        )

    setup.transaction.update.assert_not_called()