
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


def _setup_admin_test_env(monkeypatch):
    from fastapi.routing import APIRoute, iter_route_contexts
    from google.api_core.exceptions import AlreadyExists
    from app.api.v1.endpoints import admin
    from app.main import app

    store: dict[str, dict[str, dict]] = {
        "medicineCatalog": {},
        "adminAuditLogs": {},
    }
    accessed_collections: list[str] = []

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
            return Snap(
                self.id,
                store.setdefault(self.col_name, {}).get(self.id),
            )

        def create(self, data: dict):
            col = store.setdefault(self.col_name, {})
            if self.id in col:
                raise AlreadyExists("Document already exists")
            col[self.id] = dict(data)

    class Tx:
        def create(self, ref: DocRef, data: dict):
            ref.create(data)

        def update(self, ref: DocRef, updates: dict):
            col = store.setdefault(ref.col_name, {})
            if ref.id not in col:
                raise KeyError(ref.id)
            col[ref.id].update(updates)

    class Col:
        def __init__(self, col_name: str):
            self.col_name = col_name

        def document(self, doc_id: str):
            return DocRef(self.col_name, doc_id)

        def stream(self):
            col = store.setdefault(self.col_name, {})
            return iter([Snap(doc_id, data) for doc_id, data in col.items()])

    class DB:
        def collection(self, col_name: str):
            accessed_collections.append(col_name)
            return Col(col_name)

        def transaction(self):
            return Tx()

    db = DB()
    monkeypatch.setattr(admin, "get_firestore_client", lambda: db)
    monkeypatch.setattr(
        admin.google_firestore,
        "transactional",
        lambda fn: fn,
    )

    tokens = {
        "tok-admin": {"uid": "admin-001", "admin": True},
        "tok-user": {"uid": "user-001"},
        "tok-spoof": {"uid": "user-002", "admin": "true"},
    }
    monkeypatch.setattr(deps, "get_firebase_app", lambda: object())
    monkeypatch.setattr(
        deps.auth,
        "verify_id_token",
        lambda token, app=None, check_revoked=False: tokens[token],
    )

    return app, store, accessed_collections, (APIRoute, iter_route_contexts)


def test_admin_routes_are_registered(monkeypatch):
    app, _, _, (api_route_cls, iter_ctx) = _setup_admin_test_env(monkeypatch)
    operations = {
        (method, context.path)
        for context in iter_ctx(app.routes)
        if isinstance(context.route, api_route_cls)
        for method in context.methods
    }

    required = {
        ("GET", "/api/v1/admin/me"),
        ("GET", "/api/v1/admin/summary"),
        ("GET", "/api/v1/admin/medicines"),
        ("POST", "/api/v1/admin/medicines"),
        ("PUT", "/api/v1/admin/medicines/{catalog_id}"),
        ("DELETE", "/api/v1/admin/medicines/{catalog_id}"),
        ("GET", "/api/v1/admin/audit-logs"),
    }
    missing = required - operations
    assert not missing, f"Thiếu Admin API: {missing}"


def test_admin_endpoints_enforce_auth_and_role_on_backend(monkeypatch):
    from fastapi.testclient import TestClient

    app, store, _, _ = _setup_admin_test_env(monkeypatch)

    with TestClient(app) as client:
        endpoints = [
            ("GET", "/api/v1/admin/me", None),
            ("GET", "/api/v1/admin/summary", None),
            ("GET", "/api/v1/admin/medicines", None),
            (
                "POST",
                "/api/v1/admin/medicines",
                {"name": "Metformin", "strength": "500mg", "dosageForm": "Viên nén"},
            ),
            (
                "PUT",
                "/api/v1/admin/medicines/med-1",
                {"name": "Metformin", "strength": "850mg", "dosageForm": "Viên nén"},
            ),
            ("DELETE", "/api/v1/admin/medicines/med-1", None),
            ("GET", "/api/v1/admin/audit-logs", None),
        ]

        for method, path, body in endpoints:
            unauth = client.request(method, path, json=body)
            assert unauth.status_code == 401

            for tok in ("tok-user", "tok-spoof"):
                forbidden_resp = client.request(
                    method,
                    path,
                    headers={"Authorization": f"Bearer {tok}"},
                    json=body,
                )
                assert forbidden_resp.status_code == 403
                assert forbidden_resp.json()["detail"]["code"] == "permission_denied"

    assert store["medicineCatalog"] == {}
    assert store["adminAuditLogs"] == {}


def test_admin_catalog_crud_soft_delete_summary_and_audit_logs(monkeypatch):
    from fastapi.testclient import TestClient

    app, store, accessed_collections, _ = _setup_admin_test_env(monkeypatch)
    headers = {"Authorization": "Bearer tok-admin"}

    with TestClient(app) as client:
        # W01
        me = client.get("/api/v1/admin/me", headers=headers)
        assert me.status_code == 200
        assert me.json() == {"uid": "admin-001", "isAdmin": True}
        assert me.headers["cache-control"] == "no-store"

        # W04: Create 2 medicines
        r1 = client.post(
            "/api/v1/admin/medicines",
            headers=headers,
            json={
                "name": "Metformin Stella",
                "strength": "500mg",
                "dosageForm": "Viên nén bao phim",
                "source": "Cục Quản lý Dược",
            },
        )
        assert r1.status_code == 201
        med1 = r1.json()
        assert med1["active"] is True
        assert med1["updatedByUid"] == "admin-001"

        r2 = client.post(
            "/api/v1/admin/medicines",
            headers=headers,
            json={
                "name": "Amlodipine",
                "strength": "5mg",
                "dosageForm": "Viên nang",
            },
        )
        assert r2.status_code == 201
        med2 = r2.json()
        assert med2["source"] == "admin"

        # W03: Search & filter
        search_resp = client.get(
            "/api/v1/admin/medicines",
            headers=headers,
            params={"search": "metformin"},
        )
        assert search_resp.status_code == 200
        assert [m["catalogId"] for m in search_resp.json()["items"]] == [
            med1["catalogId"]
        ]

        # W05: Update medicine
        upd = client.put(
            f"/api/v1/admin/medicines/{med1['catalogId']}",
            headers=headers,
            json={"strength": "850mg"},
        )
        assert upd.status_code == 200
        assert upd.json()["strength"] == "850mg"
        assert upd.json()["name"] == "Metformin Stella"

        # W06: Soft hide medicine (never hard deletes)
        hide = client.delete(
            f"/api/v1/admin/medicines/{med1['catalogId']}",
            headers=headers,
        )
        assert hide.status_code == 200
        assert hide.json()["active"] is False
        assert med1["catalogId"] in store["medicineCatalog"]
        assert store["medicineCatalog"][med1["catalogId"]]["active"] is False

        active_only = client.get(
            "/api/v1/admin/medicines",
            headers=headers,
            params={"active": "true"},
        )
        assert [m["catalogId"] for m in active_only.json()["items"]] == [
            med2["catalogId"]
        ]

        # W02: Summary
        summary = client.get("/api/v1/admin/summary", headers=headers)
        assert summary.status_code == 200
        assert summary.json() == {
            "totalMedicines": 2,
            "activeMedicines": 1,
            "inactiveMedicines": 1,
            "totalAuditLogs": 4,
            "apiStatus": "healthy",
        }

        # W07: Audit logs ordered newest first
        logs_resp = client.get("/api/v1/admin/audit-logs", headers=headers)
        assert logs_resp.status_code == 200
        logs = logs_resp.json()["items"]
        assert len(logs) == 4
        assert {l["action"] for l in logs} == {
            "CREATE_MEDICINE",
            "UPDATE_MEDICINE",
            "HIDE_MEDICINE",
        }
        assert all(l["actorUid"] == "admin-001" for l in logs)
        assert all(l["resourceType"] == "medicineCatalog" for l in logs)
        timestamps = [l["createdAt"] for l in logs]
        assert timestamps == sorted(timestamps, reverse=True)

        # Missing resource -> 404, invalid payloads -> 422
        assert (
            client.put(
                "/api/v1/admin/medicines/missing-id",
                headers=headers,
                json={"strength": "10mg"},
            ).status_code
            == 404
        )
        assert (
            client.delete(
                "/api/v1/admin/medicines/missing-id",
                headers=headers,
            ).status_code
            == 404
        )
        assert (
            client.post(
                "/api/v1/admin/medicines",
                headers=headers,
                json={"name": "   "},
            ).status_code
            == 422
        )
        assert (
            client.put(
                f"/api/v1/admin/medicines/{med2['catalogId']}",
                headers=headers,
                json={},
            ).status_code
            == 422
        )

    assert set(accessed_collections) <= {"medicineCatalog", "adminAuditLogs"}


def test_admin_firestore_failure_returns_503(monkeypatch):
    from fastapi.testclient import TestClient
    from google.api_core.exceptions import ServiceUnavailable
    from app.api.v1.endpoints import admin

    app, _, _, _ = _setup_admin_test_env(monkeypatch)

    class BrokenDB:
        def collection(self, _name):
            raise ServiceUnavailable("offline")

        def transaction(self):
            return object()

    monkeypatch.setattr(admin, "get_firestore_client", lambda: BrokenDB())

    with TestClient(app) as client:
        resp = client.get(
            "/api/v1/admin/summary",
            headers={"Authorization": "Bearer tok-admin"},
        )
        assert resp.status_code == 503
        assert resp.json()["detail"]["code"] == "admin_unavailable"
