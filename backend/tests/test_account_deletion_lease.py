
from datetime import datetime, timedelta, timezone
from types import SimpleNamespace
from unittest.mock import Mock

import pytest

from app.services import account_deletion_lease as lease_service
from app.services.account_deletion_lease import (
    DeletionLeaseBusy,
    DeletionLeaseInvalid,
    acquire_deletion_lease,
    get_deletion_lease_status,
    lease_is_active,
    release_deletion_lease,
    renew_deletion_lease,
)


NOW = datetime(
    2026, 11, 27, 9, 0,
    tzinfo=timezone.utc,
)


class FakeSnapshot:
    def __init__(self, data=None):
        self.data = data
        self.exists = data is not None

    def to_dict(self):
        return (
            dict(self.data)
            if self.data is not None
            else None
        )


class FakeReference:
    def __init__(self, data):
        self.data = data

    def get(self, transaction=None):
        return FakeSnapshot(self.data)


class FakeTransaction:
    def update(self, reference, changes):
        reference.data.update(changes)


class FakeCollection:
    def __init__(self, reference):
        self.reference = reference

    def document(self, uid):
        assert uid == "user-001"
        return self.reference


class FakeClient:
    def __init__(self, data):
        self.reference = FakeReference(data)

    def collection(self, name):
        assert name == "accountDeletionRequests"
        return FakeCollection(self.reference)

    def transaction(self):
        return FakeTransaction()


@pytest.fixture(autouse=True)
def fake_transactional(monkeypatch):
    monkeypatch.setattr(
        lease_service.firestore,
        "transactional",
        lambda fn: fn,
    )


def confirmed_data():
    return {
        "uid": "user-001",
        "status": "confirmed",
        "requestedAt": NOW,
        "workflowStatus": "confirmed",
        "completedSteps": [],
        "leaseToken": None,
        "leaseExpiresAt": None,
    }


def test_acquire_lease():
    client = FakeClient(confirmed_data())

    lease = acquire_deletion_lease(
        "user-001",
        client=client,
        now=NOW,
    )

    assert lease.uid == "user-001"
    assert lease.token
    assert lease.progress.status == "confirmed"

    assert (
        client.reference.data["leaseToken"]
        == lease.token
    )


def test_second_worker_rejected():
    client = FakeClient(confirmed_data())

    acquire_deletion_lease(
        "user-001",
        client=client,
        now=NOW,
    )

    with pytest.raises(DeletionLeaseBusy):
        acquire_deletion_lease(
            "user-001",
            client=client,
            now=NOW,
        )


def test_expired_lease_can_be_replaced():
    client = FakeClient(confirmed_data())

    old = acquire_deletion_lease(
        "user-001",
        client=client,
        now=NOW,
    )

    later = NOW + timedelta(seconds=181)

    new = acquire_deletion_lease(
        "user-001",
        client=client,
        now=later,
    )

    assert new.token != old.token


def test_old_worker_cannot_release_new_lease():
    client = FakeClient(confirmed_data())

    old = acquire_deletion_lease(
        "user-001",
        client=client,
        now=NOW,
    )

    later = NOW + timedelta(seconds=181)

    new = acquire_deletion_lease(
        "user-001",
        client=client,
        now=later,
    )

    assert release_deletion_lease(
        old,
        client=client,
    ) is False

    assert (
        client.reference.data["leaseToken"]
        == new.token
    )


def test_renew_lease():
    client = FakeClient(confirmed_data())

    lease = acquire_deletion_lease(
        "user-001",
        client=client,
        now=NOW,
    )

    later = NOW + timedelta(seconds=60)

    renewed = renew_deletion_lease(
        lease,
        client=client,
        now=later,
    )

    assert renewed.token == lease.token
    assert renewed.expires_at > lease.expires_at


def test_expired_lease_cannot_renew():
    client = FakeClient(confirmed_data())

    lease = acquire_deletion_lease(
        "user-001",
        client=client,
        now=NOW,
    )

    later = NOW + timedelta(seconds=181)

    with pytest.raises(DeletionLeaseInvalid):
        renew_deletion_lease(
            lease,
            client=client,
            now=later,
        )


def test_release_lease():
    client = FakeClient(confirmed_data())

    lease = acquire_deletion_lease(
        "user-001",
        client=client,
        now=NOW,
    )

    assert release_deletion_lease(
        lease,
        client=client,
    ) is True

    assert client.reference.data["leaseToken"] is None


def test_pending_request_cannot_acquire():
    data = confirmed_data()
    data["status"] = "pending"

    client = FakeClient(data)

    with pytest.raises(DeletionLeaseInvalid):
        acquire_deletion_lease(
            "user-001",
            client=client,
            now=NOW,
        )


def test_missing_request_rejected():
    client = FakeClient(None)

    with pytest.raises(DeletionLeaseInvalid):
        acquire_deletion_lease(
            "user-001",
            client=client,
            now=NOW,
        )


def test_lease_status_is_read_only():
    client = FakeClient(confirmed_data())

    result = get_deletion_lease_status(
        "user-001",
        client=client,
    )

    assert result["status"] == "confirmed"
    assert result["completedSteps"] == []
    assert result["nextStep"] == "lock_writes"
    assert result["isProcessing"] is False


def test_lease_expiration_logic():
    data = {
        "leaseToken": "abc",
        "leaseExpiresAt": NOW + timedelta(
            seconds=10
        ),
    }

    assert lease_is_active(data, NOW) is True

    assert lease_is_active(
        data,
        NOW + timedelta(seconds=10),
    ) is False


def test_wrong_uid_rejected():
    data = confirmed_data()
    data["uid"] = "other-user"

    client = FakeClient(data)

    with pytest.raises(DeletionLeaseInvalid):
        acquire_deletion_lease(
            "user-001",
            client=client,
            now=NOW,
        )
