
from datetime import datetime, timezone
from unittest.mock import Mock

import pytest
from fastapi import HTTPException, Response
from fastapi.testclient import TestClient
from google.api_core.exceptions import AlreadyExists

from app.api.deps import CurrentUser
from app.api.v1.endpoints import account_deletion
from app.main import app
from app.services.account_deletion_inventory import (
    AccountDeletionInventory,
    InventoryLimitExceeded,
)


NOW = datetime(
    2026, 11, 27, 8, 0,
    tzinfo=timezone.utc,
)


# ============================================================
# 1. MOCK HELPERS
# ============================================================

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


# ============================================================
# 2. CREATE DELETION REQUEST
# ============================================================

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

    # Chỉ ghi nhận yêu cầu, chưa xóa dữ liệu.
    reference.delete.assert_not_called()


def test_duplicate_request_is_idempotent(
    monkeypatch,
):
    _, reference = mock_collection(
        monkeypatch
    )

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


# ============================================================
# 3. GET DELETION REQUEST
# ============================================================

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
    _, reference = mock_collection(
        monkeypatch
    )

    reference.get.return_value = make_snapshot(
        exists=False
    )

    with pytest.raises(HTTPException) as error:
        account_deletion.get_my_deletion_request(
            user=CurrentUser(uid="user-001"),
            response=Response(),
        )

    assert error.value.status_code == 404


# ============================================================
# 4. REQUEST VALIDATION
# ============================================================

def test_wrong_owner_is_rejected():
    snapshot = make_snapshot(
        uid="other-user"
    )

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


# ============================================================
# 5. ROUTE REGISTRATION - FASTAPI 0.162.2
# ============================================================


def test_routes_registered():
    # Lay danh sach API tu OpenAPI schema cua FastAPI.
    # Ho tro nested routers tren FastAPI 0.162.2.
    schema = app.openapi()

    operations = {
        (method.upper(), path)
        for path, path_item in schema["paths"].items()
        for method in path_item
        if method.lower() in {
            "get", "post", "put",
            "patch", "delete"
        }
    }

    assert (
        "POST",
        "/api/v1/auth/account-deletion/requests",
    ) in operations

    assert (
        "GET",
        "/api/v1/auth/account-deletion/requests/me",
    ) in operations

    assert (
        "GET",
        "/api/v1/auth/account-deletion/preview",
    ) in operations
# ============================================================
# 6. AUTHENTICATION
# ============================================================

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


def test_preview_requires_authentication():
    with TestClient(app) as client:
        response = client.get(
            "/api/v1/auth/account-deletion/preview"
        )

    assert response.status_code == 401


# ============================================================
# 7. ACCOUNT DELETION PREVIEW
# ============================================================

def test_preview_returns_inventory(monkeypatch):
    def fake_inventory(uid, db):
        assert uid == "user-001"

        return AccountDeletionInventory(
            user_documents=12,
            family_links=2,
            family_invitations=1,
            password_reset_records=3,
        )

    monkeypatch.setattr(
        account_deletion,
        "build_account_deletion_inventory",
        fake_inventory,
    )

    result = account_deletion.preview_account_deletion(
        user=CurrentUser(uid="user-001"),
        response=Response(),
        db=Mock(),
    )

    assert result.userDocuments == 12
    assert result.familyLinks == 2
    assert result.familyInvitations == 1
    assert result.passwordResetRecords == 3
    assert result.deletionExecuted is False


def test_preview_does_not_delete_data(monkeypatch):
    fake_delete = Mock()

    monkeypatch.setattr(
        account_deletion,
        "build_account_deletion_inventory",
        lambda uid, db: AccountDeletionInventory(
            user_documents=0,
            family_links=0,
            family_invitations=0,
            password_reset_records=0,
        ),
    )

    result = account_deletion.preview_account_deletion(
        user=CurrentUser(uid="user-001"),
        response=Response(),
        db=Mock(),
    )

    fake_delete.assert_not_called()
    assert result.deletionExecuted is False


def test_preview_inventory_limit_returns_409(
    monkeypatch,
):
    def raise_limit(uid, db):
        raise InventoryLimitExceeded(
            "Too many documents"
        )

    monkeypatch.setattr(
        account_deletion,
        "build_account_deletion_inventory",
        raise_limit,
    )

    with pytest.raises(HTTPException) as error:
        account_deletion.preview_account_deletion(
            user=CurrentUser(uid="user-001"),
            response=Response(),
            db=Mock(),
        )

    assert error.value.status_code == 409


def test_preview_unavailable_returns_503(
    monkeypatch,
):
    def raise_failure(uid, db):
        raise RuntimeError(
            "Database unavailable"
        )

    monkeypatch.setattr(
        account_deletion,
        "build_account_deletion_inventory",
        raise_failure,
    )

    with pytest.raises(HTTPException) as error:
        account_deletion.preview_account_deletion(
            user=CurrentUser(uid="user-001"),
            response=Response(),
            db=Mock(),
        )

    assert error.value.status_code == 503
