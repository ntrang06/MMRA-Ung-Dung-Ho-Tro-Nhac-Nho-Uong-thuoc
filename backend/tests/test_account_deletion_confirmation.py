
import time
from unittest.mock import Mock

import pytest
from fastapi import HTTPException, Response

from app.api.deps import CurrentUser
from app.api.v1.endpoints import account_deletion
from app.api.v1.endpoints.account_deletion import (
    ConfirmDeletionRequest,
    is_recent_login,
    verify_recent_login,
)


def test_recent_login_is_accepted():
    now = int(time.time())

    assert is_recent_login(
        now - 60,
        now,
    ) is True


def test_old_login_is_rejected():
    now = int(time.time())

    assert is_recent_login(
        now - 600,
        now,
    ) is False


def test_invalid_auth_time_is_rejected():
    now = int(time.time())

    assert is_recent_login(None, now) is False
    assert is_recent_login("123", now) is False
    assert is_recent_login(True, now) is False
    assert is_recent_login(-1, now) is False


def test_future_auth_time_is_rejected():
    now = int(time.time())

    assert is_recent_login(
        now + 120,
        now,
    ) is False


def test_valid_firebase_token(monkeypatch):
    now = int(time.time())
    firebase_app = object()

    monkeypatch.setattr(
        account_deletion,
        "get_firebase_app",
        lambda: firebase_app,
    )

    def fake_verify(token, app, check_revoked):
        assert token == "test-token"
        assert app is firebase_app
        assert check_revoked is True

        return {
            "uid": "user-001",
            "auth_time": now - 30,
        }

    monkeypatch.setattr(
        account_deletion.auth,
        "verify_id_token",
        fake_verify,
    )

    verify_recent_login(
        "test-token",
        "user-001",
    )


def test_wrong_firebase_user_is_rejected(
    monkeypatch,
):
    monkeypatch.setattr(
        account_deletion,
        "get_firebase_app",
        lambda: object(),
    )

    monkeypatch.setattr(
        account_deletion.auth,
        "verify_id_token",
        lambda *args, **kwargs: {
            "uid": "other-user",
            "auth_time": int(time.time()),
        },
    )

    with pytest.raises(HTTPException) as error:
        verify_recent_login(
            "test-token",
            "user-001",
        )

    assert error.value.status_code == 401


def test_old_firebase_login_requires_reauth(
    monkeypatch,
):
    monkeypatch.setattr(
        account_deletion,
        "get_firebase_app",
        lambda: object(),
    )

    monkeypatch.setattr(
        account_deletion.auth,
        "verify_id_token",
        lambda *args, **kwargs: {
            "uid": "user-001",
            "auth_time": int(time.time()) - 3600,
        },
    )

    with pytest.raises(HTTPException) as error:
        verify_recent_login(
            "test-token",
            "user-001",
        )

    assert error.value.status_code == 401
    assert (
        error.value.detail["code"]
        == "recent_login_required"
    )


def test_confirmation_text_is_required():
    with pytest.raises(HTTPException) as error:
        account_deletion.confirm_account_deletion(
            payload=ConfirmDeletionRequest(
                confirmation="DELETE"
            ),
            user=CurrentUser(uid="user-001"),
            response=Response(),
            credentials=None,
        )

    assert error.value.status_code == 422


def test_confirm_requires_credentials():
    with pytest.raises(HTTPException) as error:
        account_deletion.confirm_account_deletion(
            payload=ConfirmDeletionRequest(
                confirmation="XOA TAI KHOAN"
            ),
            user=CurrentUser(uid="user-001"),
            response=Response(),
            credentials=None,
        )

    assert error.value.status_code == 401


def test_confirm_does_not_start_on_invalid_login(
    monkeypatch,
):
    collection = Mock()

    monkeypatch.setattr(
        account_deletion,
        "get_deletion_collection",
        lambda: collection,
    )

    def reject_login(*args, **kwargs):
        raise HTTPException(
            status_code=401,
            detail="Login expired",
        )

    monkeypatch.setattr(
        account_deletion,
        "verify_recent_login",
        reject_login,
    )

    credentials = Mock()
    credentials.credentials = "invalid-token"

    with pytest.raises(HTTPException) as error:
        account_deletion.confirm_account_deletion(
            payload=ConfirmDeletionRequest(
                confirmation="XOA TAI KHOAN"
            ),
            user=CurrentUser(uid="user-001"),
            response=Response(),
            credentials=credentials,
        )

    assert error.value.status_code == 401
    collection.document.assert_not_called()
