from datetime import datetime, timezone
from types import SimpleNamespace
from unittest.mock import Mock

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient
from google.api_core.exceptions import ServiceUnavailable

from app.api.deps import CurrentUser, get_current_user
from app.api.v1.endpoints import schedules
from app.repositories import schedule_repository


def snapshot(schedule_id, **changes):
    data = {
        "medicineId": "medicine-1",
        "doseQuantity": "1",
        "doseUnit": "TABLET",
        "times": [480],
        "weekdays": [1, 2, 3, 4, 5, 6, 7],
        "startDate": "2026-11-12",
        "version": 1,
        "createdAt": datetime(
            2026, 11, 11, tzinfo=timezone.utc
        ),
        "updatedAt": datetime(
            2026, 11, 11, tzinfo=timezone.utc
        ),
    }

    data.update(changes)

    return SimpleNamespace(
        id=schedule_id,
        exists=True,
        to_dict=lambda: data,
    )


@pytest.fixture
def app():
    application = FastAPI()

    application.include_router(
        schedules.router,
        prefix="/api/v1",
    )

    return application


@pytest.fixture
def client(app):
    app.dependency_overrides[get_current_user] = (
        lambda: CurrentUser("user-A")
    )

    with TestClient(app) as test_client:
        yield test_client


def test_missing_token_is_rejected(app):
    with TestClient(app) as client:
        response = client.get("/api/v1/schedules")

    assert response.status_code == 401


def test_list_uses_token_uid_and_ignores_query_uid(
    client,
    monkeypatch,
):
    collection = Mock(
        return_value=Mock(
            stream=lambda: [snapshot("schedule-A")]
        )
    )

    monkeypatch.setattr(
        schedule_repository,
        "user_collection",
        collection,
    )

    response = client.get(
        "/api/v1/schedules?uid=user-B"
    )

    assert response.status_code == 200
    assert response.headers["cache-control"] == "no-store"
    assert response.json()[0]["scheduleId"] == "schedule-A"

    collection.assert_called_once_with(
        CurrentUser("user-A"),
        "schedules",
    )


def test_empty_list_returns_200(client, monkeypatch):
    monkeypatch.setattr(
        schedule_repository,
        "user_collection",
        lambda *args: Mock(stream=lambda: []),
    )

    response = client.get("/api/v1/schedules")

    assert response.status_code == 200
    assert response.json() == []


def test_list_hides_deleted_but_keeps_paused(
    client,
    monkeypatch,
):
    items = [
        snapshot("paused", enabled=False),
        snapshot("deleted", deletedAt="set"),
    ]

    monkeypatch.setattr(
        schedule_repository,
        "user_collection",
        lambda *args: Mock(stream=lambda: items),
    )

    response = client.get("/api/v1/schedules")

    assert response.status_code == 200

    assert [
        row["scheduleId"]
        for row in response.json()
    ] == ["paused"]


def test_detail_uses_current_user_scope(
    client,
    monkeypatch,
):
    document = Mock(
        return_value=Mock(
            get=lambda: snapshot("schedule-A")
        )
    )

    monkeypatch.setattr(
        schedule_repository,
        "user_document",
        document,
    )

    response = client.get(
        "/api/v1/schedules/schedule-A?uid=user-B"
    )

    assert response.status_code == 200

    document.assert_called_once_with(
        CurrentUser("user-A"),
        "schedules",
        "schedule-A",
    )


@pytest.mark.parametrize(
    "item",
    [
        SimpleNamespace(exists=False),
        snapshot("deleted", deletedAt="set"),
    ],
)
def test_missing_or_deleted_detail_returns_404(
    client,
    monkeypatch,
    item,
):
    monkeypatch.setattr(
        schedule_repository,
        "user_document",
        lambda *args: Mock(get=lambda: item),
    )

    response = client.get(
        "/api/v1/schedules/unknown"
    )

    assert response.status_code == 404

    assert (
        response.json()["detail"]["code"]
        == "schedule_not_found"
    )


def test_firestore_failure_returns_503(
    client,
    monkeypatch,
):
    collection = Mock(
        stream=Mock(
            side_effect=ServiceUnavailable("offline")
        )
    )

    monkeypatch.setattr(
        schedule_repository,
        "user_collection",
        lambda *args: collection,
    )

    response = client.get("/api/v1/schedules")

    assert response.status_code == 503

    assert (
        response.json()["detail"]["code"]
        == "schedules_unavailable"
    )