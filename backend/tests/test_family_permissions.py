
from unittest.mock import Mock

import pytest
from fastapi import HTTPException, Response
from fastapi.routing import APIRoute, iter_route_contexts
from pydantic import ValidationError

from app.api.deps import CurrentUser
from app.api.v1.endpoints import family
from app.main import app


LINK_ID = "a" * 64
OWNER = "owner-001"
CAREGIVER = "caregiver-001"


def make_link_snapshot(
    status="active",
):
    snapshot = Mock()
    snapshot.exists = True
    snapshot.to_dict.return_value = {
        "ownerUid": OWNER,
        "caregiverUid": CAREGIVER,
        "status": status,
        "permissions": {
            "viewSchedule": False,
            "viewDoseStatus": False,
            "receiveMissedDoseAlerts": False,
        },
    }
    return snapshot


def test_family_permissions_default_private():
    permissions = family.FamilyPermissions()

    assert permissions.model_dump() == {
        "viewSchedule": False,
        "viewDoseStatus": False,
        "receiveMissedDoseAlerts": False,
    }


def test_permissions_reject_unknown_fields():
    with pytest.raises(ValidationError):
        family.FamilyPermissions.model_validate({
            "viewSchedule": True,
            "canEditMedicines": True,
        })


def test_permissions_reject_invalid_types():
    with pytest.raises(ValidationError):
        family.FamilyPermissions.model_validate({
            "viewSchedule": "not-a-boolean",
        })


def test_owner_can_read_active_link():
    reference = Mock()
    reference.get.return_value = make_link_snapshot()

    data = family.get_active_link(
        Mock(),
        reference,
        OWNER,
    )

    assert data["ownerUid"] == OWNER


def test_caregiver_can_read_active_link():
    reference = Mock()
    reference.get.return_value = make_link_snapshot()

    data = family.get_active_link(
        Mock(),
        reference,
        CAREGIVER,
    )

    assert data["caregiverUid"] == CAREGIVER


def test_stranger_cannot_read_active_link():
    reference = Mock()
    reference.get.return_value = make_link_snapshot()

    with pytest.raises(HTTPException) as error:
        family.get_active_link(
            Mock(),
            reference,
            "stranger-001",
        )

    assert error.value.status_code == 404


def test_revoked_link_cannot_be_accessed():
    reference = Mock()
    reference.get.return_value = make_link_snapshot(
        status="revoked"
    )

    with pytest.raises(HTTPException) as error:
        family.get_active_link(
            Mock(),
            reference,
            OWNER,
        )

    assert error.value.status_code == 404


def test_missing_link_returns_404():
    reference = Mock()
    snapshot = Mock()
    snapshot.exists = False
    reference.get.return_value = snapshot

    with pytest.raises(HTTPException) as error:
        family.get_active_link(
            Mock(),
            reference,
            OWNER,
        )

    assert error.value.status_code == 404


def make_fake_firestore(monkeypatch):
    reference = Mock()
    reference.get.return_value = make_link_snapshot()

    collection = Mock()
    collection.document.return_value = reference

    db = Mock()
    db.collection.return_value = collection

    monkeypatch.setattr(
        family,
        "get_firestore_client",
        lambda: db,
    )

    # Chạy trực tiếp callback transaction trong unit test.
    # Không cần Firebase thật.
    monkeypatch.setattr(
        family.google_firestore,
        "transactional",
        lambda callback: callback,
    )

    return db, reference


def test_owner_can_update_permissions(monkeypatch):
    db, reference = make_fake_firestore(
        monkeypatch
    )

    payload = family.FamilyPermissions(
        viewSchedule=True,
        viewDoseStatus=True,
        receiveMissedDoseAlerts=False,
    )

    response = Response()

    result = family.update_family_permissions(
        link_id=LINK_ID,
        payload=payload,
        user=CurrentUser(uid=OWNER),
        response=response,
    )

    assert result.status == "active"
    assert result.permissions.viewSchedule is True
    assert result.permissions.viewDoseStatus is True
    assert (
        result.permissions.receiveMissedDoseAlerts
        is False
    )

    transaction = db.transaction.return_value
    transaction.update.assert_called_once()

    updated_data = transaction.update.call_args.args[1]

    assert updated_data["permissions"] == (
        payload.model_dump()
    )
    assert response.headers["cache-control"] == "no-store"


def test_caregiver_cannot_update_permissions(monkeypatch):
    db, reference = make_fake_firestore(
        monkeypatch
    )

    with pytest.raises(HTTPException) as error:
        family.update_family_permissions(
            link_id=LINK_ID,
            payload=family.FamilyPermissions(
                viewSchedule=True
            ),
            user=CurrentUser(uid=CAREGIVER),
            response=Response(),
        )

    assert error.value.status_code == 403

    transaction = db.transaction.return_value
    transaction.update.assert_not_called()


def test_owner_can_revoke_link(monkeypatch):
    db, reference = make_fake_firestore(
        monkeypatch
    )

    result = family.revoke_family_link(
        link_id=LINK_ID,
        user=CurrentUser(uid=OWNER),
        response=Response(),
    )

    assert result.linkId == LINK_ID
    assert result.status == "revoked"

    transaction = db.transaction.return_value
    transaction.update.assert_called_once()

    updated_data = transaction.update.call_args.args[1]

    assert updated_data["status"] == "revoked"
    assert updated_data["revokedByUid"] == OWNER
    assert updated_data["permissions"] == {
        "viewSchedule": False,
        "viewDoseStatus": False,
        "receiveMissedDoseAlerts": False,
    }


def test_caregiver_can_leave_family(monkeypatch):
    db, reference = make_fake_firestore(
        monkeypatch
    )

    result = family.revoke_family_link(
        link_id=LINK_ID,
        user=CurrentUser(uid=CAREGIVER),
        response=Response(),
    )

    assert result.status == "revoked"

    transaction = db.transaction.return_value

    updated_data = transaction.update.call_args.args[1]
    assert updated_data["revokedByUid"] == CAREGIVER


def test_unrelated_user_cannot_revoke(monkeypatch):
    db, reference = make_fake_firestore(
        monkeypatch
    )

    with pytest.raises(HTTPException) as error:
        family.revoke_family_link(
            link_id=LINK_ID,
            user=CurrentUser(uid="stranger-001"),
            response=Response(),
        )

    assert error.value.status_code == 404

    db.transaction.return_value.update.assert_not_called()



def test_permission_routes_are_registered():
    operations = {
        (method, context.path)
        for context in iter_route_contexts(app.routes)
        if isinstance(context.route, APIRoute)
        for method in context.methods
    }

    required = {
        (
            "PUT",
            "/api/v1/family/members/{link_id}/permissions",
        ),
        (
            "DELETE",
            "/api/v1/family/members/{link_id}",
        ),
    }

    missing = required - operations

    assert not missing, (
        f"Thiếu API phân quyền Family: {missing}"
    )
