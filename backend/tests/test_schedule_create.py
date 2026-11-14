import re
from types import SimpleNamespace
from unittest.mock import Mock

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient
from google.api_core.exceptions import ServiceUnavailable

from app.api.deps import CurrentUser, get_current_user
from app.api.v1.endpoints import schedules
from app.repositories import schedule_repository as repository


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

    transaction = Mock()
    references = []

    def document(user, collection, document_id):
        references.append(
            (user.uid, collection, document_id)
        )

        exists = (
            user.uid == "user-A"
            and document_id == "medicine-A"
        )

        medicine = SimpleNamespace(
            exists=exists,
            to_dict=lambda: {},
        )

        return SimpleNamespace(
            id=document_id,
            path=f"users/{user.uid}/{collection}/{document_id}",
            get=lambda **kwargs: medicine,
        )

    monkeypatch.setattr(
        repository,
        "user_document",
        document,
    )

    monkeypatch.setattr(
        repository,
        "get_firebase_app",
        lambda: None,
    )

    monkeypatch.setattr(
        repository.firestore,
        "client",
        lambda **kwargs: Mock(
            transaction=lambda: transaction
        ),
    )

    # Mô phỏng callback giao dịch; không gọi Firebase thật.
    monkeypatch.setattr(
        repository.firestore,
        "transactional",
        lambda function: function,
    )

    body = {
        "medicineId": "medicine-A",
        "doseQuantity": "0.5",
        "doseUnit": "TABLET",
        "times": [1200, 480],
        "weekdays": [7, 1],
        "startDate": "2026-11-12",
    }

    with TestClient(app) as client:
        yield client, app, body, transaction, references


def test_create_returns_201_and_server_fields(setup):
    client, _, body, transaction, references = setup

    response = client.post(
        "/api/v1/schedules?uid=user-B",
        json=body,
    )

    assert response.status_code == 201

    data = response.json()

    assert re.fullmatch(
        r"[0-9a-f]{32}",
        data["scheduleId"],
    )

    assert data["version"] == 1
    assert data["times"] == [480, 1200]
    assert data["weekdays"] == [1, 7]
    assert data["createdAt"] == data["updatedAt"]

    assert response.headers["cache-control"] == "no-store"

    assert references == [
        ("user-A", "medicines", "medicine-A"),
        ("user-A", "schedules", data["scheduleId"]),
    ]

    transaction.create.assert_called_once()

    reference, stored = transaction.create.call_args.args

    assert (
        reference.path
        == f"users/user-A/schedules/{data['scheduleId']}"
    )

    assert stored["startDate"] == "2026-11-12"
    assert stored["createdAt"].tzinfo is not None
    assert "uid" not in stored


@pytest.mark.parametrize(
    "medicine_id",
    ["missing", "medicine-B"],
)
def test_missing_or_other_users_medicine_is_rejected(
    setup,
    medicine_id,
):
    client, _, body, transaction, _ = setup

    body["medicineId"] = medicine_id

    response = client.post(
        "/api/v1/schedules",
        json=body,
    )

    assert response.status_code == 404

    assert (
        response.json()["detail"]["code"]
        == "medicine_not_found"
    )

    transaction.create.assert_not_called()


@pytest.mark.parametrize(
    "changes",
    [
        {"uid": "user-B"},
        {"createdAt": "2026-11-11T00:00:00Z"},
        {"doseQuantity": "0"},
    ],
)
def test_invalid_body_is_rejected_before_write(
    setup,
    changes,
):
    client, _, body, transaction, _ = setup

    response = client.post(
        "/api/v1/schedules",
        json={**body, **changes},
    )

    assert response.status_code == 422
    transaction.create.assert_not_called()


def test_create_requires_login(setup):
    client, app, body, transaction, _ = setup

    app.dependency_overrides.clear()

    response = client.post(
        "/api/v1/schedules",
        json=body,
    )

    assert response.status_code == 401
    transaction.create.assert_not_called()


def test_firestore_error_returns_503(setup):
    client, _, body, transaction, _ = setup

    transaction.create.side_effect = (
        ServiceUnavailable("offline")
    )

    response = client.post(
        "/api/v1/schedules",
        json=body,
    )

    assert response.status_code == 503

    assert (
        response.json()["detail"]["code"]
        == "schedules_unavailable"
    )