
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


def make_client():
    app = FastAPI()

    app.include_router(
        medicines.router,
        prefix="/api/v1",
    )

    app.dependency_overrides[get_current_user] = (
        lambda: CurrentUser(uid="user-A")
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


def test_create_medicine_uses_authenticated_uid(
    monkeypatch,
):
    reference = Mock()
    reference.id = "medicine-123"

    collection = Mock()
    collection.document.return_value = reference

    scopes = []

    def fake_collection(user, name):
        scopes.append((user.uid, name))
        return collection

    monkeypatch.setattr(
        medicines,
        "user_collection",
        fake_collection,
    )

    with make_client() as client:
        result = client.post(
            "/api/v1/medicines",
            json=sample_medicine(),
        )

    assert result.status_code == 201
    assert result.json()["medicineId"] == "medicine-123"
    assert result.json()["version"] == 1

    assert scopes == [
        ("user-A", "medicines")
    ]

    reference.create.assert_called_once()

    saved = reference.create.call_args.args[0]

    assert saved["medicineName"] == "APO-METFORMIN"
    assert saved["strength"] == "500"
    assert saved["version"] == 1
    assert "createdAt" in saved
    assert "updatedAt" in saved
    assert "uid" not in saved


def test_client_cannot_override_uid():
    body = sample_medicine()
    body["uid"] = "user-B"

    with make_client() as client:
        result = client.post(
            "/api/v1/medicines",
            json=body,
        )

    assert result.status_code == 422


def test_client_cannot_override_created_at():
    body = sample_medicine()
    body["createdAt"] = "2020-01-01T00:00:00Z"

    with make_client() as client:
        result = client.post(
            "/api/v1/medicines",
            json=body,
        )

    assert result.status_code == 422


def test_blank_medicine_name_is_rejected():
    body = sample_medicine()
    body["medicineName"] = "   "

    with make_client() as client:
        result = client.post(
            "/api/v1/medicines",
            json=body,
        )

    assert result.status_code == 422


def test_get_medicine_uses_owner_scope(monkeypatch):
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

    def fake_document(user, name, medicine_id):
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
    assert result.json()["medicineName"] == "APO-METFORMIN"

    # Thuốc cũ chưa có version được đọc như version 1.
    assert result.json()["version"] == 1

    assert scopes == [
        ("user-A", "medicines", "medicine-123")
    ]


def fake_update_environment(monkeypatch):
    """
    Firestore giả lập dùng chung dữ liệu cho nhiều
    request, mô phỏng hai thiết bị sửa cùng một thuốc.

    Không gọi Firebase thật.
    """
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
        def update(self, reference, fields):
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

    # Giả lập transaction, không kết nối Firestore thật.
    monkeypatch.setattr(
        medicines.firestore,
        "transactional",
        lambda callback: callback,
    )

    return stored, writes


def test_update_preserves_created_at(monkeypatch):
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
        # Thiết bị A sửa thuốc dựa trên version 1.
        first = client.put(
            "/api/v1/medicines/medicine-123",
            json={
                **sample_medicine(),
                "medicineName": "Thiết bị A",
                "expectedVersion": 1,
            },
        )

        # Thiết bị B vẫn dùng version 1.
        second = client.put(
            "/api/v1/medicines/medicine-123",
            json={
                **sample_medicine(),
                "medicineName": "Thiết bị B",
                "expectedVersion": 1,
            },
        )

    # A được lưu.
    assert first.status_code == 200
    assert first.json()["version"] == 2

    # B bị từ chối vì version đã cũ.
    assert second.status_code == 409

    assert (
        second.json()["detail"]["code"]
        == "medicine_version_conflict"
    )

    # Dữ liệu của A không bị B ghi đè.
    assert stored["medicineName"] == "Thiết bị A"
    assert stored["version"] == 2

    # Chỉ một lần cập nhật được chấp nhận.
    assert len(writes) == 1


def test_put_requires_expected_version(monkeypatch):
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


def test_missing_medicine_returns_404(monkeypatch):
    reference = Mock()

    reference.get.return_value = SimpleNamespace(
        exists=False,
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
