
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

    assert (
        "GET",
        "/api/v1/family/alerts",
    ) in operations


def test_family_shared_requires_login():
    with TestClient(app) as client:
        response = client.get(
            f"/api/v1/family/members/{LINK_ID}/doses",
            params={"date": TODAY.isoformat()},
        )
        alerts_response = client.get("/api/v1/family/alerts")

    assert response.status_code == 401
    assert alerts_response.status_code == 401


def _make_dose(dose_id: str, status: str, scheduled_at: datetime):
    return {
        "doseId": dose_id,
        "localDate": TODAY.isoformat(),
        "scheduledAt": scheduled_at,
        "status": status,
        "actionAt": scheduled_at if status == "MISSED" else None,
        "nextReminderAt": None,
        "medicineSnapshot": {"medicineName": "Metformin Private"},
    }


def test_family_alerts_returns_only_confirmed_missed_doses(monkeypatch):
    link_allowed = "a" * 64
    link_blocked = "b" * 64
    link_revoked = "c" * 64

    links_store = {
        link_allowed: {
            "ownerUid": OWNER_UID,
            "caregiverUid": CAREGIVER_UID,
            "status": "active",
            "permissions": {
                "viewSchedule": False,
                "viewDoseStatus": False,
                "receiveMissedDoseAlerts": True,
            },
        },
        link_blocked: {
            "ownerUid": "owner-002",
            "caregiverUid": CAREGIVER_UID,
            "status": "active",
            "permissions": {
                "viewSchedule": True,
                "viewDoseStatus": True,
                "receiveMissedDoseAlerts": False,
            },
        },
        link_revoked: {
            "ownerUid": "owner-003",
            "caregiverUid": CAREGIVER_UID,
            "status": "revoked",
            "permissions": {
                "receiveMissedDoseAlerts": True,
            },
        },
    }

    def make_snap(doc_id, data):
        snap = Mock()
        snap.exists = data is not None
        snap.id = doc_id
        snap.to_dict.return_value = data
        return snap

    query = Mock()
    query.stream.return_value = iter(
        [make_snap(k, v) for k, v in links_store.items()]
    )

    collection = Mock()
    collection.where.return_value = query
    collection.document.side_effect = (
        lambda doc_id: Mock(get=lambda: make_snap(doc_id, links_store.get(doc_id)))
    )

    db = Mock()
    db.collection.return_value = collection
    monkeypatch.setattr(family_shared.family, "get_firestore_client", lambda: db)

    monkeypatch.setattr(
        family_shared.MedicationRepository,
        "list_doses",
        lambda user, target_date: (
            [
                _make_dose("d" * 64, "PENDING", NOW),
                _make_dose("e" * 64, "TAKEN", NOW),
                _make_dose("f" * 64, "MISSED", NOW),
                _make_dose("0" * 64, "SKIPPED", NOW),
                _make_dose("1" * 64, "CANCELLED", NOW),
            ]
            if user.uid == OWNER_UID
            else [_make_dose("2" * 64, "MISSED", NOW)]
        ),
    )

    response = Response()
    result = family_shared.get_family_alerts(
        user=CurrentUser(uid=CAREGIVER_UID),
        response=response,
        target_date=TODAY,
        link_id=None,
    )

    assert response.headers["cache-control"] == "no-store"
    assert len(result.items) == 1
    alert = result.items[0]
    assert alert.linkId == link_allowed
    assert alert.ownerUid == OWNER_UID
    assert alert.doseId == "f" * 64
    assert alert.status == "MISSED"
    dumped = alert.model_dump()
    assert "medicineName" not in dumped
    assert "medicineSnapshot" not in dumped


def test_family_alerts_with_link_id_checks_permission_and_revocation(monkeypatch):
    mock_family_link(
        monkeypatch,
        permissions={
            "viewSchedule": True,
            "viewDoseStatus": True,
            "receiveMissedDoseAlerts": False,
        },
    )

    with pytest.raises(HTTPException) as exc:
        family_shared.get_family_alerts(
            user=CurrentUser(uid=CAREGIVER_UID),
            response=Response(),
            target_date=TODAY,
            link_id=LINK_ID,
        )
    assert exc.value.status_code == 403

    # Revoked mid-read must reject
    ref = mock_family_link(
        monkeypatch,
        permissions={"receiveMissedDoseAlerts": True},
    )
    revoked_snap = Mock()
    revoked_snap.exists = True
    revoked_snap.to_dict.return_value = {
        "ownerUid": OWNER_UID,
        "caregiverUid": CAREGIVER_UID,
        "status": "revoked",
        "permissions": {},
    }
    ref.get.side_effect = [ref.get.return_value, revoked_snap]
    monkeypatch.setattr(
        family_shared.MedicationRepository,
        "list_doses",
        lambda user, target_date: [_make_dose("f" * 64, "MISSED", NOW)],
    )

    with pytest.raises(HTTPException) as exc2:
        family_shared.get_family_alerts(
            user=CurrentUser(uid=CAREGIVER_UID),
            response=Response(),
            target_date=TODAY,
            link_id=LINK_ID,
        )
    assert exc2.value.status_code == 404


def test_two_account_family_lifecycle_grant_toggle_and_revoke(monkeypatch):
    from app.api import deps
    from app.api.v1.endpoints import family

    store: dict[str, dict[str, dict]] = {
        "familyInvitations": {},
        "familyLinks": {},
    }

    class Snap:
        def __init__(self, doc_id: str, data: dict | None):
            self.id = doc_id
            self._data = dict(data) if data is not None else None

        @property
        def exists(self) -> bool:
            return self._data is not None

        def to_dict(self):
            return dict(self._data) if self._data is not None else None

    class DocRef:
        def __init__(self, col_name: str, doc_id: str):
            self.col_name = col_name
            self.id = doc_id

        def get(self, transaction=None):
            return Snap(self.id, store[self.col_name].get(self.id))

        def create(self, data: dict):
            store[self.col_name][self.id] = dict(data)

    class Tx:
        def create(self, ref, data):
            ref.create(data)

        def set(self, ref, data):
            store[ref.col_name][ref.id] = dict(data)

        def update(self, ref, updates):
            store[ref.col_name][ref.id].update(updates)

    class Query:
        def __init__(self, col_name: str, filters=None):
            self.col_name = col_name
            self.filters = list(filters or [])

        def where(self, *, filter):
            return Query(self.col_name, self.filters + [filter])

        def stream(self):
            out = []
            for doc_id, data in store[self.col_name].items():
                if all(
                    flt.op_string == "==" and data.get(flt.field_path) == flt.value
                    for flt in self.filters
                ):
                    out.append(Snap(doc_id, data))
            return iter(out)

    class DB:
        def collection(self, col_name: str):
            return type(
                "Col",
                (),
                {
                    "document": lambda _s, doc_id: DocRef(col_name, doc_id),
                    "where": lambda _s, *, filter: Query(col_name, [filter]),
                },
            )()

        def transaction(self):
            return Tx()

    db = DB()
    monkeypatch.setattr(family, "get_firestore_client", lambda: db)
    monkeypatch.setattr(family.google_firestore, "transactional", lambda fn: fn)
    monkeypatch.setattr(
        family_shared.schedule_repository,
        "list_schedules",
        lambda user: [],
    )
    monkeypatch.setattr(
        family_shared.MedicationRepository,
        "list_doses",
        lambda user, target_date: (
            [
                _make_dose("a" * 64, "PENDING", NOW),
                _make_dose("b" * 64, "MISSED", NOW),
            ]
            if user.uid == OWNER_UID
            else []
        ),
    )

    tokens = {
        "tok-owner": {"uid": OWNER_UID},
        "tok-caregiver": {"uid": CAREGIVER_UID},
        "tok-stranger": {"uid": "stranger-001"},
    }
    monkeypatch.setattr(deps, "get_firebase_app", lambda: object())
    monkeypatch.setattr(
        deps.auth,
        "verify_id_token",
        lambda token, app=None, check_revoked=False: tokens[token],
    )

    owner_headers = {"Authorization": "Bearer tok-owner"}
    caregiver_headers = {"Authorization": "Bearer tok-caregiver"}
    stranger_headers = {"Authorization": "Bearer tok-stranger"}
    date_param = TODAY.isoformat()

    with TestClient(app) as client:
        # 1. Owner invites Caregiver; Caregiver accepts -> defaults all False.
        inv = client.post("/api/v1/family/invitations", headers=owner_headers)
        assert inv.status_code == 201
        acc = client.post(
            "/api/v1/family/invitations/accept",
            headers=caregiver_headers,
            json={"code": inv.json()["code"]},
        )
        assert acc.status_code == 200
        link_id = acc.json()["linkId"]

        # Caregiver initially blocked from schedules, doses, and linkId alerts.
        assert client.get(f"/api/v1/family/members/{link_id}/schedules", headers=caregiver_headers).status_code == 403
        assert client.get(f"/api/v1/family/members/{link_id}/doses?date={date_param}", headers=caregiver_headers).status_code == 403
        assert client.get(f"/api/v1/family/alerts?date={date_param}&linkId={link_id}", headers=caregiver_headers).status_code == 403
        assert client.get(f"/api/v1/family/alerts?date={date_param}", headers=caregiver_headers).json()["items"] == []

        # 2. Owner grants all 3 permissions -> Caregiver can view, Stranger still gets 404.
        grant = client.put(
            f"/api/v1/family/members/{link_id}/permissions",
            headers=owner_headers,
            json={
                "viewSchedule": True,
                "viewDoseStatus": True,
                "receiveMissedDoseAlerts": True,
            },
        )
        assert grant.status_code == 200

        assert client.get(f"/api/v1/family/members/{link_id}/schedules", headers=caregiver_headers).status_code == 200
        assert len(client.get(f"/api/v1/family/members/{link_id}/doses?date={date_param}", headers=caregiver_headers).json()["items"]) == 2
        alerts_resp = client.get(f"/api/v1/family/alerts?date={date_param}", headers=caregiver_headers)
        assert alerts_resp.status_code == 200
        assert [a["doseId"] for a in alerts_resp.json()["items"]] == ["b" * 64]

        assert client.get(f"/api/v1/family/members/{link_id}/doses?date={date_param}", headers=stranger_headers).status_code == 404
        assert client.get(f"/api/v1/family/alerts?date={date_param}&linkId={link_id}", headers=stranger_headers).status_code == 404

        # 3. Owner turns off alerts permission -> Caregiver loses alert access immediately.
        turn_off_alerts = client.put(
            f"/api/v1/family/members/{link_id}/permissions",
            headers=owner_headers,
            json={
                "viewSchedule": True,
                "viewDoseStatus": True,
                "receiveMissedDoseAlerts": False,
            },
        )
        assert turn_off_alerts.status_code == 200
        assert client.get(f"/api/v1/family/alerts?date={date_param}&linkId={link_id}", headers=caregiver_headers).status_code == 403
        assert client.get(f"/api/v1/family/alerts?date={date_param}", headers=caregiver_headers).json()["items"] == []

        # 4. Owner revokes family link -> Caregiver loses all access immediately.
        revoked = client.delete(f"/api/v1/family/members/{link_id}", headers=owner_headers)
        assert revoked.status_code == 200
        assert client.get(f"/api/v1/family/members/{link_id}/schedules", headers=caregiver_headers).status_code == 404
        assert client.get(f"/api/v1/family/members/{link_id}/doses?date={date_param}", headers=caregiver_headers).status_code == 404
        assert client.get(f"/api/v1/family/alerts?date={date_param}&linkId={link_id}", headers=caregiver_headers).status_code == 404
        assert client.get("/api/v1/family/members", headers=caregiver_headers).json()["items"] == []
        assert client.get(f"/api/v1/family/alerts?date={date_param}", headers=caregiver_headers).json()["items"] == []
