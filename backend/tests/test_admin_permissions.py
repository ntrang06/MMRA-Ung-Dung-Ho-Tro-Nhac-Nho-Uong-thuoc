
import pytest

from fastapi import HTTPException
from fastapi.security import (
    HTTPAuthorizationCredentials,
)

from app.api import deps


def make_credentials():
    return HTTPAuthorizationCredentials(
        scheme="Bearer",
        credentials="test-firebase-token",
    )


def mock_firebase(monkeypatch, claims):
    fake_app = object()

    monkeypatch.setattr(
        deps,
        "get_firebase_app",
        lambda: fake_app,
    )

    def fake_verify_id_token(
        token,
        app=None,
        check_revoked=False,
    ):
        assert token == "test-firebase-token"
        assert app is fake_app
        assert check_revoked is True

        return claims

    monkeypatch.setattr(
        deps.auth,
        "verify_id_token",
        fake_verify_id_token,
    )


def test_missing_token_returns_401():
    with pytest.raises(HTTPException) as error:
        deps.get_current_user(None)

    assert error.value.status_code == 401
    assert (
        error.value.detail["code"]
        == "unauthenticated"
    )


def test_normal_user_can_authenticate(monkeypatch):
    mock_firebase(
        monkeypatch,
        {
            "uid": "user-001",
        },
    )

    user = deps.get_current_user(
        make_credentials(),
    )

    assert user.uid == "user-001"
    assert user.is_admin is False


def test_normal_user_cannot_access_admin(monkeypatch):
    mock_firebase(
        monkeypatch,
        {
            "uid": "user-001",
        },
    )

    user = deps.get_current_user(
        make_credentials(),
    )

    with pytest.raises(HTTPException) as error:
        deps.get_admin_user(user)

    assert error.value.status_code == 403
    assert (
        error.value.detail["code"]
        == "permission_denied"
    )


def test_admin_claim_allows_access(monkeypatch):
    mock_firebase(
        monkeypatch,
        {
            "uid": "admin-001",
            "admin": True,
        },
    )

    user = deps.get_current_user(
        make_credentials(),
    )

    admin = deps.get_admin_user(user)

    assert admin.uid == "admin-001"
    assert admin.is_admin is True


@pytest.mark.parametrize(
    "invalid_admin_value",
    [
        False,
        None,
        "true",
        "admin",
        1,
        0,
        {},
        [],
    ],
)
def test_invalid_admin_claim_is_rejected(
    monkeypatch,
    invalid_admin_value,
):
    mock_firebase(
        monkeypatch,
        {
            "uid": "user-001",
            "admin": invalid_admin_value,
        },
    )

    user = deps.get_current_user(
        make_credentials(),
    )

    assert user.is_admin is False

    with pytest.raises(HTTPException) as error:
        deps.get_admin_user(user)

    assert error.value.status_code == 403


def test_invalid_uid_returns_401(monkeypatch):
    mock_firebase(
        monkeypatch,
        {
            "uid": "../another-user",
            "admin": True,
        },
    )

    with pytest.raises(HTTPException) as error:
        deps.get_current_user(
            make_credentials(),
        )

    assert error.value.status_code == 401


def test_existing_current_user_remains_compatible():
    user = deps.CurrentUser(
        uid="existing-user",
    )

    assert user.uid == "existing-user"
    assert user.is_admin is False
