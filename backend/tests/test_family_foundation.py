
from datetime import datetime, timedelta, timezone
from unittest.mock import Mock

from fastapi.routing import APIRoute
from fastapi.testclient import TestClient

from app.main import app
from app.api.v1.endpoints.family import (
    FamilyPermissions,
    family_link_id,
    generate_invite_code,
    invite_hash,
    is_invitation_expired,
    snapshot_to_member,
)


def test_invite_code_is_random_and_secure():
    first = generate_invite_code()
    second = generate_invite_code()

    assert len(first) == 32
    assert first != second
    assert all(
        char in "0123456789ABCDEF"
        for char in first
    )


def test_invite_hash_is_stable():
    code = generate_invite_code()

    assert invite_hash(code) == invite_hash(code.lower())
    assert invite_hash(code) != code


def test_family_link_id_is_stable():
    first = family_link_id(
        "owner-001",
        "caregiver-001",
    )

    second = family_link_id(
        "owner-001",
        "caregiver-001",
    )

    assert first == second
    assert len(first) == 64


def test_family_link_id_changes_with_users():
    assert family_link_id(
        "owner-001",
        "caregiver-001",
    ) != family_link_id(
        "owner-002",
        "caregiver-001",
    )


def test_invitation_expiration():
    now = datetime.now(timezone.utc)

    assert is_invitation_expired(
        now - timedelta(seconds=1),
        now,
    )

    assert not is_invitation_expired(
        now + timedelta(hours=1),
        now,
    )


def test_invitation_rejects_invalid_timestamp():
    now = datetime.now(timezone.utc)

    assert is_invitation_expired(None, now)
    assert is_invitation_expired(
        datetime(2026, 11, 27, 8, 0),
        now,
    )


def test_family_permissions_default_to_private():
    permissions = FamilyPermissions()

    assert permissions.viewSchedule is False
    assert permissions.viewDoseStatus is False
    assert permissions.receiveMissedDoseAlerts is False


def test_member_owner_role():
    snapshot = Mock()
    snapshot.exists = True
    snapshot.id = "link-001"
    snapshot.to_dict.return_value = {
        "ownerUid": "owner-001",
        "caregiverUid": "caregiver-001",
        "status": "active",
        "permissions": {
            "viewSchedule": True,
            "viewDoseStatus": False,
            "receiveMissedDoseAlerts": False,
        },
    }

    member = snapshot_to_member(
        snapshot,
        "owner-001",
    )

    assert member is not None
    assert member.role == "owner"
    assert member.memberUid == "caregiver-001"


def test_member_caregiver_role():
    snapshot = Mock()
    snapshot.exists = True
    snapshot.id = "link-001"
    snapshot.to_dict.return_value = {
        "ownerUid": "owner-001",
        "caregiverUid": "caregiver-001",
        "status": "active",
        "permissions": {},
    }

    member = snapshot_to_member(
        snapshot,
        "caregiver-001",
    )

    assert member is not None
    assert member.role == "caregiver"
    assert member.memberUid == "owner-001"


def test_unrelated_user_cannot_read_family_link():
    snapshot = Mock()
    snapshot.exists = True
    snapshot.id = "link-001"
    snapshot.to_dict.return_value = {
        "ownerUid": "owner-001",
        "caregiverUid": "caregiver-001",
        "status": "active",
        "permissions": {},
    }

    assert snapshot_to_member(
        snapshot,
        "stranger-001",
    ) is None


def test_revoked_link_is_hidden():
    snapshot = Mock()
    snapshot.exists = True
    snapshot.id = "link-001"
    snapshot.to_dict.return_value = {
        "ownerUid": "owner-001",
        "caregiverUid": "caregiver-001",
        "status": "revoked",
        "permissions": {},
    }

    assert snapshot_to_member(
        snapshot,
        "owner-001",
    ) is None


def test_family_routes_are_registered():
    operations = {
        (method, route.path)
        for route in app.routes
        if isinstance(route, APIRoute)
        for method in route.methods
    }

    required = {
        ("POST", "/api/v1/family/invitations"),
        ("POST", "/api/v1/family/invitations/accept"),
        ("GET", "/api/v1/family/members"),
    }

    assert required.issubset(operations)


def test_family_api_requires_login():
    with TestClient(app) as client:
        response = client.get(
            "/api/v1/family/members"
        )

    assert response.status_code == 401
