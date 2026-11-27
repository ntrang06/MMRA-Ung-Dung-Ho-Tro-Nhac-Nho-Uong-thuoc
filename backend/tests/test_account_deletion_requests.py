
from datetime import datetime, timezone
from unittest.mock import Mock

import pytest
from fastapi import HTTPException, Response
from fastapi.routing import (
    APIRoute,
    iter_route_contexts,
)
from fastapi.testclient import TestClient
from google.api_core.exceptions import AlreadyExists

from app.api.deps import CurrentUser
from app.api.v1.endpoints import account_deletion
from app.main import app


NOW = datetime(
    2026, 11, 27, 8, 0,
    tzinfo=timezone.utc,
)


def make_snapshot(
    uid="user-001",
    status="pending",
    exists=True,
):
    snapshot = Mock()
    snapshot.exists = exists
    snapshot.to_dict.return_value = {
        "uid": uid,
        "status": status,
        "requestedAt": NOW,
    }

    return snapshot


def mock_collection(monkeypatch):
    reference = Mock()
    collection = Mock()
    collection.document.return_value = reference

    monkeypatch.setattr(
        account_deletion,
        "get_deletion_collection",
        lambda: collection,
    )

    return collection, reference


def test_create_deletion_request(monkeypatch):
    collection, reference = mock_collection(
        monkeypatch
    )

    result = account_deletion.request_account_deletion(
        user=CurrentUser(uid="user-001"),
        response=Response(),
    )

    assert result.status == "pending"
    assert result.requestedAt.tzinfo is not None

    collection.document.assert_called_once_with(
        "user-001"
    )

    reference.create.assert_called_once()

    data = reference.create.call_args.args[0]

    assert data["uid"] == "user-001"
    assert data["status"] == "pending"

    # Endpoint chỉ ghi nhận yêu cầu,
    # chưa xóa tài khoản hoặc dữ liệu.
    reference.delete.assert_not_called()


def test_duplicate_request_is_idempotent(
    monkeypatch,
):
    _, reference = mock_collection(monkeypatch)

    reference.create.side_effect = AlreadyExists(
        "Document already exists"
    )

    reference.get.return_value = make_snapshot()

    result = account_deletion.request_account_deletion(
        user=CurrentUser(uid="user-001"),
        response=Response(),
    )

    assert result.status == "pending"
    reference.get.assert_called_once()


def test_get_own_request(monkeypatch):
    collection, reference = mock_collection(
        monkeypatch
    )

    reference.get.return_value = make_snapshot()

    result = account_deletion.get_my_deletion_request(
        user=CurrentUser(uid="user-001"),
        response=Response(),
    )

    assert result.status == "pending"

    collection.document.assert_called_once_with(
        "user-001"
    )


def test_missing_request_returns_404(monkeypatch):
    _, reference = mock_collection(monkeypatch)

    reference.get.return_value = make_snapshot(
        exists=False
    )

    with pytest.raises(HTTPException) as error:
        account_deletion.get_my_deletion_request(
            user=CurrentUser(uid="user-001"),
            response=Response(),
        )

    assert error.value.status_code == 404


def test_wrong_owner_is_rejected():
    snapshot = make_snapshot(uid="other-user")

    with pytest.raises(HTTPException) as error:
        account_deletion.validate_request_snapshot(
            snapshot,
            "user-001",
        )

    assert error.value.status_code == 503


def test_invalid_status_is_rejected():
    snapshot = make_snapshot(
        status="unknown"
    )

    with pytest.raises(HTTPException) as error:
        account_deletion.validate_request_snapshot(
            snapshot,
            "user-001",
        )

    assert error.value.status_code == 503


def test_routes_registered():
    operations = {
        (method, context.path)
        for context in iter_route_contexts(app.routes)
        if isinstance(context.route, APIRoute)
        for method in context.methods
    }

    assert (
        "POST",
        "/api/v1/auth/account-deletion/requests",
    ) in operations

    assert (
        "GET",
        "/api/v1/auth/account-deletion/requests/me",
    ) in operations


def test_request_requires_authentication():
    with TestClient(app) as client:
        response = client.post(
            "/api/v1/auth/account-deletion/requests"
        )

    assert response.status_code == 401


def test_request_status_requires_authentication():
    with TestClient(app) as client:
        response = client.get(
            "/api/v1/auth/account-deletion/requests/me"
        )

    assert response.status_code == 401
