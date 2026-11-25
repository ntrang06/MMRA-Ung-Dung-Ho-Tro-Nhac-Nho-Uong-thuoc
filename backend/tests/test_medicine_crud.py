
from datetime import datetime, timezone
from types import SimpleNamespace
from unittest.mock import Mock

from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.api.deps import (
    CurrentUser,
    get_current_user,
)
from app.api.v1.endpoints import medicines


TEST_KEY = "d909fd35-40e9-4b64-bada-f32d6cfece87"
OTHER_KEY = "a000fd35-40e9-4b64-bada-f32d6cfece87"


def make_client(uid="user-A"):
    app = FastAPI()

    app.include_router(
        medicines.router,
        prefix="/api/v1",
    )

    app.dependency_overrides[get_current_user] = (
        lambda: CurrentUser(uid=uid)
    )

    return TestClient(app)


def sample_medicine():
    return {
        "medicineName": "APO-METFORMIN",
        "strength": "500",
        "strengthUnit": "mg",
        "dosageForm": "Viên nén",
        "note": "",
        "imageUrl": None,
    }


def create_headers(key=TEST_KEY):
    return {
        "Idempotency-Key": key,
    }


def fake_create_environment(monkeypatch):
    """
    Giả lập Firestore cho thao tác POST.

    Có lưu trạng thái giữa nhiều HTTP request để
    kiểm tra khi Android gửi lại cùng khóa.

    Đây là test logic API, không kết nối Firebase thật.
    """
    medicines_store = {}
    requests_store = {}

    counters = {
        "medicine_created": 0,
        "request_created": 0,
    }

    scopes = []

    class Snapshot:
        def __init__(self, document_id, data):
            self.id = document_id
            self.exists = data is not None
            self._data = (
                None if data is None else dict(data)
            )

        def to_dict(self):
            if self._data is None:
                return None

            return dict(self._data)

    class Reference:
        def __init__(
            self,
            collection_name,
            document_id,
        ):
            self.collection_name = collection_name
            self.id = document_id

        def get(self, transaction=None):
            assert transaction is not None

            store = (
                medicines_store
                if self.collection_name == "medicines"
                else requests_store
            )

            return Snapshot(
                self.id,
                store.get(self.id),
            )

    class Collection:
        def __init__(self, name):
            self.name = name

        def document(self, document_id=None):
            if document_id is None:
                document_id = (
                    f"medicine-{len(medicines_store) + 1}"
                )

            return Reference(
                self.name,
                document_id,
            )

    class Transaction:
        def create(self, reference, fields):
            store = (
                medicines_store
                if reference.collection_name == "medicines"
                else requests_store
            )

            assert reference.id not in store

            store[reference.id] = dict(fields)

            if reference.collection_name == "medicines":
                counters["medicine_created"] += 1
            else:
                counters["request_created"] += 1

    class Client:
        def transaction(self):
            return Transaction()

    def fake_user_collection(user, name):
        scopes.append(
            (user.uid, name)
        )

        return Collection(name)

    def fake_user_document(
        user,
        name,
        document_id,
    ):
        scopes.append(
            (user.uid, name, document_id)
        )

        return Reference(
            name,
            document_id,
        )

    monkeypatch.setattr(
        medicines,
        "user_collection",
        fake_user_collection,
    )

    monkeypatch.setattr(
        medicines,
        "user_document",
        fake_user_document,
    )

    monkeypatch.setattr(
        medicines.firestore,
        "client",
        lambda **kwargs: Client(),
    )

    monkeypatch.setattr(
        medicines,
        "get_firebase_app",
        lambda: object(),
    )

    monkeypatch.setattr(
        medicines.firestore,
        "transactional",
        lambda callback: callback,
    )

    return (
        medicines_store,
        requests_store,
        counters,
        scopes,
    )


# =========================================================
# POST - B3.3
# =========================================================

def test_create_medicine_uses_authenticated_uid(
    monkeypatch,
):
    (
        medicine_store,
        request_store,
        counters,
        scopes,
    ) = fake_create_environment(monkeypatch)

    with make_client() as client:
        result = client.post(
            "/api/v1/medicines",
            headers=create_headers(),
            json=sample_medicine(),
        )

    assert result.status_code == 201

    data = result.json()

    assert data["medicineId"] == "medicine-1"
    assert data["version"] == 1
    assert data["medicineName"] == "APO-METFORMIN"

    assert counters["medicine_created"] == 1
    assert counters["request_created"] == 1

    assert len(medicine_store) == 1
    assert len(request_store) == 1

    saved = medicine_store["medicine-1"]

    assert saved["medicineName"] == "APO-METFORMIN"
    assert saved["strength"] == "500"
    assert saved["version"] == 1
    assert "createdAt" in saved
    assert "updatedAt" in saved
    assert "uid" not in saved

    assert any(
        scope[0] == "user-A"
        for scope in scopes
    )


def test_retry_same_key_returns_same_medicine(
    monkeypatch,
):
    (
        medicine_store,
        request_store,
        counters,
        _,
    ) = fake_create_environment(monkeypatch)

    with make_client() as client:
        first = client.post(
            "/api/v1/medicines",
            headers=create_headers(),
            json=sample_medicine(),
        )

        second = client.post(
            "/api/v1/medicines",
            headers=create_headers(),
            json=sample_medicine(),
        )

    assert first.status_code == 201
    assert second.status_code == 200

    assert (
        first.json()["medicineId"]
        == second.json()["medicineId"]
    )

    assert first.json()["version"] == 1
    assert second.json()["version"] == 1

    assert (
        second.headers["idempotency-replayed"]
        == "true"
    )

    assert counters["medicine_created"] == 1
    assert counters["request_created"] == 1

    assert len(medicine_store) == 1
    assert len(request_store) == 1


def test_same_key_with_different_payload_returns_409(
    monkeypatch,
):
    (
        medicine_store,
        _,
        counters,
        _,
    ) = fake_create_environment(monkeypatch)

    with make_client() as client:
        first = client.post(
            "/api/v1/medicines",
            headers=create_headers(),
            json=sample_medicine(),
        )

        second = client.post(
            "/api/v1/medicines",
            headers=create_headers(),
            json={
                **sample_medicine(),
                "strength": "850",
            },
        )

    assert first.status_code == 201
    assert second.status_code == 409

    assert (
        second.json()["detail"]["code"]
        == "idempotency_key_reused"
    )

    assert counters["medicine_created"] == 1
    assert len(medicine_store) == 1

    assert (
        medicine_store["medicine-1"]["strength"]
        == "500"
    )


def test_different_keys_create_different_medicines(
    monkeypatch,
):
    (
        medicine_store,
        request_store,
        counters,
        _,
    ) = fake_create_environment(monkeypatch)

    with make_client() as client:
        first = client.post(
            "/api/v1/medicines",
            headers=create_headers(TEST_KEY),
            json=sample_medicine(),
        )

        second = client.post(
            "/api/v1/medicines",
            headers=create_headers(OTHER_KEY),
            json=sample_medicine(),
        )

    assert first.status_code == 201
    assert second.status_code == 201

    assert (
        first.json()["medicineId"]
        != second.json()["medicineId"]
    )

    assert counters["medicine_created"] == 2
    assert len(medicine_store) == 2
    assert len(request_store) == 2


def test_post_requires_idempotency_key(
    monkeypatch,
):
    fake_create_environment(monkeypatch)

    with make_client() as client:
        result = client.post(
            "/api/v1/medicines",
            json=sample_medicine(),
        )

    assert result.status_code == 422


def test_post_rejects_invalid_idempotency_key(
    monkeypatch,
):
    (
        medicine_store,
        _,
        counters,
        _,
    ) = fake_create_environment(monkeypatch)

    with make_client() as client:
        result = client.post(
            "/api/v1/medicines",
            headers=create_headers("short"),
            json=sample_medicine(),
        )

    assert result.status_code == 422
    assert counters["medicine_created"] == 0
    assert len(medicine_store) == 0


def test_client_cannot_override_uid():
    body = sample_medicine()
    body["uid"] = "user-B"

    with make_client() as client:
        result = client.post(
            "/api/v1/medicines",
            headers=create_headers(),
            json=body,
        )

    assert result.status_code == 422


def test_client_cannot_override_created_at():
    body = sample_medicine()
    body["createdAt"] = (
        "2020-01-01T00:00:00Z"
    )

    with make_client() as client:
        result = client.post(
            "/api/v1/medicines",
            headers=create_headers(),
            json=body,
        )

    assert result.status_code == 422


def test_blank_medicine_name_is_rejected():
    body = sample_medicine()
    body["medicineName"] = "   "

    with make_client() as client:
        result = client.post(
            "/api/v1/medicines",
            headers=create_headers(),
            json=body,
        )

    assert result.status_code == 422


# =========================================================
# GET - GIỮ TEST CŨ
# =========================================================

def test_get_medicine_uses_owner_scope(
    monkeypatch,
):
    now = datetime.now(timezone.utc)

    snapshot = SimpleNamespace(
        id="medicine-123",
        exists=True,
        to_dict=lambda: {
            **sample_medicine(),
            "createdAt": now,
            "updatedAt": now,
        },
    )

    reference = Mock()
    reference.get.return_value = snapshot

    scopes = []

    def fake_document(
        user,
        name,
        medicine_id,
    ):
        scopes.append(
            (user.uid, name, medicine_id)
        )

        return reference

    monkeypatch.setattr(
        medicines,
        "user_document",
        fake_document,
    )

    with make_client() as client:
        result = client.get(
            "/api/v1/medicines/medicine-123"
        )

    assert result.status_code == 200
    assert (
        result.json()["medicineName"]
        == "APO-METFORMIN"
    )

    # Thuốc cũ chưa có version vẫn đọc là 1.
    assert result.json()["version"] == 1

    assert scopes == [
        ("user-A", "medicines", "medicine-123")
    ]


# =========================================================
# PUT - B3.2 GIỮ NGUYÊN
# =========================================================

def fake_update_environment(
    monkeypatch,
):
    now = datetime.now(timezone.utc)

    stored = {
        **sample_medicine(),
        "createdAt": now,
        "updatedAt": now,
    }

    writes = []

    class Reference:
        def get(self, transaction=None):
            assert transaction is not None

            return SimpleNamespace(
                exists=True,
                to_dict=lambda: dict(stored),
            )

    class Transaction:
        def update(
            self,
            reference,
            fields,
        ):
            stored.update(fields)
            writes.append(dict(fields))

    class Client:
        def transaction(self):
            return Transaction()

    reference = Reference()

    monkeypatch.setattr(
        medicines,
        "user_document",
        lambda user, name, document_id: reference,
    )

    monkeypatch.setattr(
        medicines.firestore,
        "client",
        lambda **kwargs: Client(),
    )

    monkeypatch.setattr(
        medicines,
        "get_firebase_app",
        lambda: object(),
    )

    monkeypatch.setattr(
        medicines.firestore,
        "transactional",
        lambda callback: callback,
    )

    return stored, writes


def test_update_preserves_created_at(
    monkeypatch,
):
    stored, writes = fake_update_environment(
        monkeypatch
    )

    original_time = stored["createdAt"]

    body = {
        **sample_medicine(),
        "expectedVersion": 1,
    }

    with make_client() as client:
        result = client.put(
            "/api/v1/medicines/medicine-123",
            json=body,
        )

    assert result.status_code == 200
    assert result.json()["version"] == 2

    assert stored["version"] == 2
    assert stored["createdAt"] == original_time

    assert "createdAt" not in writes[0]
    assert "expectedVersion" not in writes[0]


def test_stale_version_cannot_overwrite_newer_medicine(
    monkeypatch,
):
    stored, writes = fake_update_environment(
        monkeypatch
    )

    with make_client() as client:
        first = client.put(
            "/api/v1/medicines/medicine-123",
            json={
                **sample_medicine(),
                "medicineName": "Thiết bị A",
                "expectedVersion": 1,
            },
        )

        second = client.put(
            "/api/v1/medicines/medicine-123",
            json={
                **sample_medicine(),
                "medicineName": "Thiết bị B",
                "expectedVersion": 1,
            },
        )

    assert first.status_code == 200
    assert first.json()["version"] == 2

    assert second.status_code == 409

    assert (
        second.json()["detail"]["code"]
        == "medicine_version_conflict"
    )

    assert stored["medicineName"] == "Thiết bị A"
    assert stored["version"] == 2
    assert len(writes) == 1


def test_put_requires_expected_version(
    monkeypatch,
):
    fake_update_environment(monkeypatch)

    with make_client() as client:
        result = client.put(
            "/api/v1/medicines/medicine-123",
            json=sample_medicine(),
        )

    assert result.status_code == 422


def test_put_rejects_invalid_expected_version(
    monkeypatch,
):
    fake_update_environment(monkeypatch)

    with make_client() as client:
        result = client.put(
            "/api/v1/medicines/medicine-123",
            json={
                **sample_medicine(),
                "expectedVersion": 0,
            },
        )

    assert result.status_code == 422


def test_missing_medicine_returns_404(
    monkeypatch,
):
    reference = Mock()

    reference.get.return_value = (
        SimpleNamespace(
            exists=False,
        )
    )

    monkeypatch.setattr(
        medicines,
        "user_document",
        lambda user, name, medicine_id: reference,
    )

    with make_client() as client:
        result = client.get(
            "/api/v1/medicines/unknown"
        )

    assert result.status_code == 404
