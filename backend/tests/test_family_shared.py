
from datetime import date, datetime, timezone
from unittest.mock import Mock

import pytest
from fastapi import HTTPException, Response
from fastapi.routing import APIRoute, iter_route_contexts
from fastapi.testclient import TestClient

from app.api.deps import CurrentUser
from app.api.v1.endpoints import family_shared
from app.main import app


LINK_ID = "a" * 64
OWNER_UID = "owner-001"
CAREGIVER_UID = "caregiver-001"

TODAY = date(2026, 11, 27)
NOW = datetime(
    2026, 11, 27, 8, 0,
    tzinfo=timezone.utc,
)


def mock_family_link(
    monkeypatch,
    *,
    status="active",
    permissions=None,
):
    if permissions is None:
        permissions = {
            "viewSchedule": False,
            "viewDoseStatus": False,
            "receiveMissedDoseAlerts": False,
        }

    snapshot = Mock()
    snapshot.exists = True
    snapshot.to_dict.return_value = {
        "ownerUid": OWNER_UID,
        "caregiverUid": CAREGIVER_UID,
        "status": status,
        "permissions": permissions,
    }

    reference = Mock()
    reference.get.return_value = snapshot

    db = Mock()
    db.collection.return_value.document.return_value = (
        reference
    )

    monkeypatch.setattr(
        family_shared.family,
        "get_firestore_client",
        lambda: db,
    )

    return reference


def test_stranger_cannot_read_family_data(monkeypatch):
    mock_family_link(
        monkeypatch,
        permissions={"viewSchedule": True},
    )

    with pytest.raises(HTTPException) as exc:
        family_shared.get_shared_owner(
            LINK_ID,
            CurrentUser(uid="stranger-001"),
            "viewSchedule",
        )

    assert exc.value.status_code == 404


def test_revoked_family_link_cannot_be_read(monkeypatch):
    mock_family_link(
        monkeypatch,
        status="revoked",
        permissions={"viewDoseStatus": True},
    )

    with pytest.raises(HTTPException) as exc:
        family_shared.get_shared_owner(
            LINK_ID,
            CurrentUser(uid=CAREGIVER_UID),
            "viewDoseStatus",
        )

    assert exc.value.status_code == 404


def test_caregiver_without_permission_gets_403(
    monkeypatch,
):
    mock_family_link(monkeypatch)

    with pytest.raises(HTTPException) as exc:
        family_shared.get_shared_owner(
            LINK_ID,
            CurrentUser(uid=CAREGIVER_UID),
            "viewDoseStatus",
        )

    assert exc.value.status_code == 403


def test_caregiver_with_permission_can_read(
    monkeypatch,
):
    mock_family_link(
        monkeypatch,
        permissions={
            "viewSchedule": True,
            "viewDoseStatus": True,
        },
    )

    owner = family_shared.get_shared_owner(
        LINK_ID,
        CurrentUser(uid=CAREGIVER_UID),
        "viewSchedule",
    )

    assert owner.uid == OWNER_UID


def test_owner_can_read_own_link(monkeypatch):
    mock_family_link(monkeypatch)

    owner = family_shared.get_shared_owner(
        LINK_ID,
        CurrentUser(uid=OWNER_UID),
        "viewSchedule",
    )

    assert owner.uid == OWNER_UID


def test_shared_dose_has_no_medicine_details():
    dose = family_shared.SharedDose(
        doseId="b" * 64,
        localDate=TODAY,
        scheduledAt=NOW,
        status="TAKEN",
        actionAt=NOW,
    )

    data = dose.model_dump()

    assert data["status"] == "TAKEN"
    assert "medicineName" not in data
    assert "medicineSnapshot" not in data
    assert "medicineId" not in data


def test_shared_doses_reads_owner_records(monkeypatch):
    mock_family_link(
        monkeypatch,
        permissions={
            "viewDoseStatus": True,
        },
    )

    called = []

    def fake_list_doses(user, target_date):
        called.append((user.uid, target_date))

        return [{
            "doseId": "b" * 64,
            "localDate": TODAY.isoformat(),
            "scheduledAt": NOW,
            "status": "TAKEN",
            "actionAt": NOW,
            "nextReminderAt": None,
            "medicineSnapshot": {
                "medicineName": "Private medicine"
            },
        }]

    monkeypatch.setattr(
        family_shared.MedicationRepository,
        "list_doses",
        fake_list_doses,
    )

    result = family_shared.get_shared_doses(
        link_id=LINK_ID,
        user=CurrentUser(uid=CAREGIVER_UID),
        response=Response(),
        target_date=TODAY,
    )

    assert called == [(OWNER_UID, TODAY)]
    assert len(result.items) == 1
    assert result.items[0].status == "TAKEN"

    data = result.items[0].model_dump()

    assert "medicineSnapshot" not in data


def test_reading_doses_does_not_generate_new_doses(
    monkeypatch,
):
    mock_family_link(
        monkeypatch,
        permissions={
            "viewDoseStatus": True,
        },
    )

    monkeypatch.setattr(
        family_shared.MedicationRepository,
        "list_doses",
        lambda user, target_date: [],
    )

    result = family_shared.get_shared_doses(
        link_id=LINK_ID,
        user=CurrentUser(uid=CAREGIVER_UID),
        response=Response(),
        target_date=TODAY,
    )

    assert result.items == []


def test_permission_revoked_during_read_is_rejected(
    monkeypatch,
):
    reference = mock_family_link(
        monkeypatch,
        permissions={
            "viewDoseStatus": True,
        },
    )

    revoked_snapshot = Mock()
    revoked_snapshot.exists = True
    revoked_snapshot.to_dict.return_value = {
        "ownerUid": OWNER_UID,
        "caregiverUid": CAREGIVER_UID,
        "status": "revoked",
        "permissions": {},
    }

    reference.get.side_effect = [
        reference.get.return_value,
        revoked_snapshot,
    ]

    monkeypatch.setattr(
        family_shared.MedicationRepository,
        "list_doses",
        lambda user, target_date: [],
    )

    with pytest.raises(HTTPException) as exc:
        family_shared.get_shared_doses(
            link_id=LINK_ID,
            user=CurrentUser(uid=CAREGIVER_UID),
            response=Response(),
            target_date=TODAY,
        )

    assert exc.value.status_code == 404


def test_family_shared_routes_registered():
    operations = {
        (method, context.path)
        for context in iter_route_contexts(app.routes)
        if isinstance(context.route, APIRoute)
        for method in context.methods
    }

    assert (
        "GET",
        "/api/v1/family/members/{link_id}/schedules",
    ) in operations

    assert (
        "GET",
        "/api/v1/family/members/{link_id}/doses",
    ) in operations


def test_family_shared_requires_login():
    with TestClient(app) as client:
        response = client.get(
            f"/api/v1/family/members/{LINK_ID}/doses",
            params={"date": TODAY.isoformat()},
        )

    assert response.status_code == 401
