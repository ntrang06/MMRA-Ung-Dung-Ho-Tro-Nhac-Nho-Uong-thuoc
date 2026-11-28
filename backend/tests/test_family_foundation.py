
from datetime import datetime, timedelta, timezone
from unittest.mock import Mock

from fastapi.routing import APIRoute, iter_route_contexts
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


def get_api_operations():
    operations = set()

    # FastAPI hiện tại sử dụng router lồng nhau.
    # iter_route_contexts lấy đúng đường dẫn đầy đủ.
    for context in iter_route_contexts(app.routes):
        if not isinstance(context.route, APIRoute):
            continue

        for method in context.methods:
            operations.add(
                (method, context.path)
            )

    return operations


def test_family_routes_are_registered():
    operations = get_api_operations()

    required = {
        ("POST", "/api/v1/family/invitations"),
        ("POST", "/api/v1/family/invitations/accept"),
        ("POST", "/api/v1/family/invitations/decline"),
        ("DELETE", "/api/v1/family/invitations/{invitation_id}"),
        ("GET", "/api/v1/family/members"),
    }

    missing = required - operations

    assert not missing, (
        f"Thiếu API Family: {missing}"
    )


def test_family_api_requires_login():
    with TestClient(app) as client:
        response = client.get(
            "/api/v1/family/members"
        )

    assert response.status_code == 401


def _build_in_memory_family_db():
    collections: dict[str, dict[str, dict]] = {
        "familyInvitations": {},
        "familyLinks": {},
    }

    class FakeSnapshot:
        def __init__(self, doc_id: str, data: dict | None):
            self.id = doc_id
            self._data = dict(data) if data is not None else None

        @property
        def exists(self) -> bool:
            return self._data is not None

        def to_dict(self):
            return dict(self._data) if self._data is not None else None

    class FakeDocRef:
        def __init__(self, col_name: str, doc_id: str):
            self.col_name = col_name
            self.id = doc_id

        def get(self, transaction=None):
            data = collections.setdefault(self.col_name, {}).get(self.id)
            return FakeSnapshot(self.id, data)

        def create(self, data: dict):
            col = collections.setdefault(self.col_name, {})
            if self.id in col:
                raise ValueError("Already exists")
            col[self.id] = dict(data)

    class FakeTransaction:
        def create(self, ref: FakeDocRef, data: dict):
            ref.create(data)

        def set(self, ref: FakeDocRef, data: dict):
            collections.setdefault(ref.col_name, {})[ref.id] = dict(data)

        def update(self, ref: FakeDocRef, updates: dict):
            col = collections.setdefault(ref.col_name, {})
            existing = col.get(ref.id)
            if existing is None:
                raise KeyError(ref.id)
            existing.update(updates)

    class FakeQuery:
        def __init__(self, col_name: str, filters=None):
            self.col_name = col_name
            self.filters = list(filters or [])

        def where(self, *, filter):
            return FakeQuery(self.col_name, self.filters + [filter])

        def stream(self):
            docs = []
            for doc_id, data in collections.setdefault(self.col_name, {}).items():
                matched = True
                for flt in self.filters:
                    if flt.op_string == "==" and data.get(flt.field_path) != flt.value:
                        matched = False
                        break
                if matched:
                    docs.append(FakeSnapshot(doc_id, data))
            return iter(docs)

    class FakeCollection:
        def __init__(self, col_name: str):
            self.col_name = col_name

        def document(self, doc_id: str):
            return FakeDocRef(self.col_name, doc_id)

        def where(self, *, filter):
            return FakeQuery(self.col_name, [filter])

    class FakeDB:
        def collection(self, col_name: str):
            return FakeCollection(col_name)

        def transaction(self):
            return FakeTransaction()

    return FakeDB(), collections


def _auth_headers(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def test_invitation_decline_and_cancel_flows(monkeypatch):
    from app.api import deps
    from app.api.v1.endpoints import family

    db, store = _build_in_memory_family_db()
    monkeypatch.setattr(family, "get_firestore_client", lambda: db)
    monkeypatch.setattr(
        family.google_firestore,
        "transactional",
        lambda fn: fn,
    )
    monkeypatch.setattr(deps, "get_firebase_app", lambda: object())
    monkeypatch.setattr(
        deps.auth,
        "verify_id_token",
        lambda token, app=None, check_revoked=False: {
            "uid": "owner-001" if token == "tok-owner" else "caregiver-001",
        },
    )

    with TestClient(app) as client:
        # 1. Caregiver declines an invite -> marked revoked and cannot be accepted later.
        created = client.post(
            "/api/v1/family/invitations",
            headers=_auth_headers("tok-owner"),
        )
        assert created.status_code == 201
        code = created.json()["code"]
        inv_id = created.json()["invitationId"]

        self_decline = client.post(
            "/api/v1/family/invitations/decline",
            headers=_auth_headers("tok-owner"),
            json={"code": code},
        )
        assert self_decline.status_code == 400
        assert self_decline.json()["detail"]["code"] == "self_invitation"

        declined = client.post(
            "/api/v1/family/invitations/decline",
            headers=_auth_headers("tok-caregiver"),
            json={"code": code.lower()},
        )
        assert declined.status_code == 200
        assert declined.headers["cache-control"] == "no-store"
        assert declined.json() == {
            "invitationId": inv_id,
            "status": "revoked",
        }
        assert store["familyInvitations"][inv_id]["revokedByUid"] == "caregiver-001"

        accept_after_decline = client.post(
            "/api/v1/family/invitations/accept",
            headers=_auth_headers("tok-caregiver"),
            json={"code": code},
        )
        assert accept_after_decline.status_code == 409
        assert (
            accept_after_decline.json()["detail"]["code"]
            == "invitation_unavailable"
        )

        # 2. Owner cancels an unused pending invite; non-owner gets 403.
        created2 = client.post(
            "/api/v1/family/invitations",
            headers=_auth_headers("tok-owner"),
        )
        assert created2.status_code == 201
        code2 = created2.json()["code"]
        inv_id2 = created2.json()["invitationId"]

        forbidden_cancel = client.delete(
            f"/api/v1/family/invitations/{inv_id2}",
            headers=_auth_headers("tok-caregiver"),
        )
        assert forbidden_cancel.status_code == 403
        assert forbidden_cancel.json()["detail"]["code"] == "permission_denied"

        canceled = client.delete(
            f"/api/v1/family/invitations/{inv_id2}",
            headers=_auth_headers("tok-owner"),
        )
        assert canceled.status_code == 200
        assert canceled.headers["cache-control"] == "no-store"
        assert canceled.json() == {
            "invitationId": inv_id2,
            "status": "revoked",
        }

        cancel_again = client.delete(
            f"/api/v1/family/invitations/{inv_id2}",
            headers=_auth_headers("tok-owner"),
        )
        assert cancel_again.status_code == 409

        accept_after_cancel = client.post(
            "/api/v1/family/invitations/accept",
            headers=_auth_headers("tok-caregiver"),
            json={"code": code2},
        )
        assert accept_after_cancel.status_code == 409

        # 3. Missing invitation returns 404.
        missing_cancel = client.delete(
            f"/api/v1/family/invitations/{'0' * 64}",
            headers=_auth_headers("tok-owner"),
        )
        assert missing_cancel.status_code == 404
        assert missing_cancel.json()["detail"]["code"] == "invitation_not_found"

        missing_decline = client.post(
            "/api/v1/family/invitations/decline",
            headers=_auth_headers("tok-caregiver"),
            json={"code": "A" * 32},
        )
        assert missing_decline.status_code == 404


def test_expired_invitation_is_marked_expired_on_cancel_and_decline(monkeypatch):
    from app.api import deps
    from app.api.v1.endpoints import family

    db, store = _build_in_memory_family_db()
    monkeypatch.setattr(family, "get_firestore_client", lambda: db)
    monkeypatch.setattr(
        family.google_firestore,
        "transactional",
        lambda fn: fn,
    )
    monkeypatch.setattr(deps, "get_firebase_app", lambda: object())
    monkeypatch.setattr(
        deps.auth,
        "verify_id_token",
        lambda token, app=None, check_revoked=False: {
            "uid": "owner-001" if token == "tok-owner" else "caregiver-001",
        },
    )

    with TestClient(app) as client:
        c1 = client.post("/api/v1/family/invitations", headers=_auth_headers("tok-owner")).json()
        c2 = client.post("/api/v1/family/invitations", headers=_auth_headers("tok-owner")).json()

        expired_time = datetime.now(timezone.utc) - timedelta(minutes=5)
        store["familyInvitations"][c1["invitationId"]]["expiresAt"] = expired_time
        store["familyInvitations"][c2["invitationId"]]["expiresAt"] = expired_time

        r_decline = client.post(
            "/api/v1/family/invitations/decline",
            headers=_auth_headers("tok-caregiver"),
            json={"code": c1["code"]},
        )
        assert r_decline.status_code == 400
        assert r_decline.json()["detail"]["code"] == "invitation_expired"
        assert store["familyInvitations"][c1["invitationId"]]["status"] == "expired"

        r_cancel = client.delete(
            f"/api/v1/family/invitations/{c2['invitationId']}",
            headers=_auth_headers("tok-owner"),
        )
        assert r_cancel.status_code == 400
        assert r_cancel.json()["detail"]["code"] == "invitation_expired"
        assert store["familyInvitations"][c2["invitationId"]]["status"] == "expired"


def test_accept_invitation_rejects_expired_self_and_duplicate_links(monkeypatch):
    from app.api import deps
    from app.api.v1.endpoints import family

    db, store = _build_in_memory_family_db()
    monkeypatch.setattr(family, "get_firestore_client", lambda: db)
    monkeypatch.setattr(
        family.google_firestore,
        "transactional",
        lambda fn: fn,
    )
    monkeypatch.setattr(deps, "get_firebase_app", lambda: object())
    monkeypatch.setattr(
        deps.auth,
        "verify_id_token",
        lambda token, app=None, check_revoked=False: {
            "uid": "owner-001" if token == "tok-owner" else "caregiver-001",
        },
    )

    with TestClient(app) as client:
        inv1 = client.post("/api/v1/family/invitations", headers=_auth_headers("tok-owner")).json()
        inv2 = client.post("/api/v1/family/invitations", headers=_auth_headers("tok-owner")).json()
        inv3 = client.post("/api/v1/family/invitations", headers=_auth_headers("tok-owner")).json()

        # Self-invitation rejected
        r_self = client.post(
            "/api/v1/family/invitations/accept",
            headers=_auth_headers("tok-owner"),
            json={"code": inv1["code"]},
        )
        assert r_self.status_code == 400
        assert r_self.json()["detail"]["code"] == "self_invitation"

        # Expired invitation rejected
        store["familyInvitations"][inv3["invitationId"]]["expiresAt"] = (
            datetime.now(timezone.utc) - timedelta(seconds=10)
        )
        r_exp = client.post(
            "/api/v1/family/invitations/accept",
            headers=_auth_headers("tok-caregiver"),
            json={"code": inv3["code"]},
        )
        assert r_exp.status_code == 400
        assert r_exp.json()["detail"]["code"] == "invitation_expired"

        # First valid accept succeeds; second invite between same pair gets 409 duplicate link
        r_ok = client.post(
            "/api/v1/family/invitations/accept",
            headers=_auth_headers("tok-caregiver"),
            json={"code": inv1["code"]},
        )
        assert r_ok.status_code == 200
        assert r_ok.json()["status"] == "active"

        # Reusing accepted invitation fails
        r_reuse = client.post(
            "/api/v1/family/invitations/accept",
            headers=_auth_headers("tok-caregiver"),
            json={"code": inv1["code"]},
        )
        assert r_reuse.status_code == 409
        assert r_reuse.json()["detail"]["code"] == "invitation_unavailable"

        # Duplicate active link rejected
        r_dup = client.post(
            "/api/v1/family/invitations/accept",
            headers=_auth_headers("tok-caregiver"),
            json={"code": inv2["code"]},
        )
        assert r_dup.status_code == 409
        assert r_dup.json()["detail"]["code"] == "family_already_linked"
