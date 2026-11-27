
from datetime import datetime, timedelta, timezone

import pytest

from app.services import (
    account_deletion_progress_store as store,
)
from app.services.account_deletion_lease import (
    DeletionLease,
    DeletionLeaseInvalid,
)
from app.services.account_deletion_workflow import (
    DELETION_STEPS,
    mark_step_completed,
    new_workflow,
)


NOW = datetime(
    2026, 11, 27, 9, 0,
    tzinfo=timezone.utc,
)


class FakeSnapshot:
    def __init__(self, data):
        self.data = data
        self.exists = data is not None

    def to_dict(self):
        return dict(self.data) if self.exists else None


class FakeReference:
    def __init__(self, data):
        self.data = data

    def get(self, transaction=None):
        return FakeSnapshot(self.data)


class FakeTransaction:
    def __init__(self):
        self.updates = []

    def update(self, reference, changes):
        self.updates.append(dict(changes))
        reference.data.update(changes)


class FakeClient:
    def __init__(self, data):
        self.reference = FakeReference(data)
        self.transactions = []

    def collection(self, name):
        assert name == "accountDeletionRequests"
        return self

    def document(self, uid):
        assert uid == "user-001"
        return self.reference

    def transaction(self):
        transaction = FakeTransaction()
        self.transactions.append(transaction)
        return transaction


@pytest.fixture(autouse=True)
def fake_transactional(monkeypatch):
    monkeypatch.setattr(
        store.firestore,
        "transactional",
        lambda function: function,
    )


def make_data():
    return {
        "uid": "user-001",
        "status": "confirmed",
        "workflowStatus": "confirmed",
        "completedSteps": [],
        "leaseToken": "worker-token",
        "leaseExpiresAt": NOW + timedelta(
            seconds=180
        ),
    }


def make_lease(token="worker-token"):
    return DeletionLease(
        uid="user-001",
        token=token,
        expires_at=NOW + timedelta(
            seconds=180
        ),
        progress=new_workflow("user-001"),
    )


def save(client, step, lease=None, now=NOW):
    return store.save_completed_step(
        lease or make_lease(),
        step,
        client=client,
        now=now,
    )


def test_first_step_saved():
    client = FakeClient(make_data())

    result = save(
        client,
        "lock_writes",
    )

    assert result.status == "processing"
    assert result.completed_steps == (
        "lock_writes",
    )

    data = client.reference.data

    assert data["status"] == "processing"
    assert data["workflowStatus"] == "processing"
    assert data["completedSteps"] == [
        "lock_writes"
    ]
    assert data["lastCompletedStep"] == "lock_writes"


def test_repeat_step_is_idempotent():
    client = FakeClient(make_data())

    first = save(client, "lock_writes")
    second = save(client, "lock_writes")

    assert second == first

    total_updates = sum(
        len(t.updates)
        for t in client.transactions
    )

    assert total_updates == 1


def test_cannot_skip_step():
    client = FakeClient(make_data())

    with pytest.raises(
        store.DeletionProgressConflict
    ):
        save(
            client,
            "delete_family_links",
        )

    assert client.reference.data[
        "completedSteps"
    ] == []


def test_wrong_lease_rejected():
    client = FakeClient(make_data())

    with pytest.raises(
        DeletionLeaseInvalid
    ):
        save(
            client,
            "lock_writes",
            lease=make_lease("wrong-token"),
        )

    assert client.reference.data[
        "completedSteps"
    ] == []


def test_expired_lease_rejected():
    client = FakeClient(make_data())

    later = NOW + timedelta(
        seconds=181
    )

    with pytest.raises(
        DeletionLeaseInvalid
    ):
        save(
            client,
            "lock_writes",
            now=later,
        )

    assert client.reference.data[
        "completedSteps"
    ] == []


def test_missing_request_rejected():
    client = FakeClient(None)

    with pytest.raises(
        DeletionLeaseInvalid
    ):
        save(
            client,
            "lock_writes",
        )


def test_unconfirmed_request_rejected():
    data = make_data()
    data["status"] = "pending"

    client = FakeClient(data)

    with pytest.raises(
        DeletionLeaseInvalid
    ):
        save(
            client,
            "lock_writes",
        )


def test_full_workflow_completes():
    client = FakeClient(make_data())

    progress = new_workflow("user-001")

    for step in DELETION_STEPS:
        result = save(client, step)
        progress = mark_step_completed(
            progress,
            step,
        )

        assert result == progress

    data = client.reference.data

    assert data["status"] == "completed"
    assert data["workflowStatus"] == "completed"
    assert data["completedSteps"] == list(
        DELETION_STEPS
    )
    assert data["leaseToken"] is None
    assert data["leaseExpiresAt"] is None
    assert data["completedAt"] == NOW


def test_completed_request_cannot_restart():
    client = FakeClient(make_data())

    for step in DELETION_STEPS:
        save(client, step)

    with pytest.raises(
        DeletionLeaseInvalid
    ):
        save(
            client,
            "lock_writes",
        )


def test_wrong_uid_rejected():
    data = make_data()
    data["uid"] = "another-user"

    client = FakeClient(data)

    with pytest.raises(
        DeletionLeaseInvalid
    ):
        save(
            client,
            "lock_writes",
        )


def test_invalid_timestamp_rejected():
    client = FakeClient(make_data())

    with pytest.raises(ValueError):
        store.save_completed_step(
            make_lease(),
            "lock_writes",
            client=client,
            now=datetime(2026, 11, 27),
        )
