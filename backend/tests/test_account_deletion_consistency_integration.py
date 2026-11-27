
from datetime import datetime, timedelta, timezone

import pytest

from app.services import (
    account_deletion_lease as lease_service,
)
from app.services import (
    account_deletion_progress_store as store,
)
from app.services.account_deletion_lease import (
    DeletionLeaseInvalid,
)


UID = "user-001"

NOW = datetime(
    2026, 11, 27, 9, 0,
    tzinfo=timezone.utc,
)


class FakeSnapshot:
    def __init__(self, data):
        self.data = data
        self.exists = data is not None

    def to_dict(self):
        if not self.exists:
            return None

        return dict(self.data)


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
        assert uid == UID
        return self.reference

    def transaction(self):
        transaction = FakeTransaction()
        self.transactions.append(transaction)
        return transaction


@pytest.fixture(autouse=True)
def fake_firestore_transactions(monkeypatch):
    monkeypatch.setattr(
        lease_service.firestore,
        "transactional",
        lambda function: function,
    )

    monkeypatch.setattr(
        store.firestore,
        "transactional",
        lambda function: function,
    )


def make_data(
    status="confirmed",
    workflow_status="confirmed",
    steps=None,
):
    return {
        "uid": UID,
        "status": status,
        "workflowStatus": workflow_status,
        "completedSteps": (
            [] if steps is None else list(steps)
        ),
        "leaseToken": None,
        "leaseExpiresAt": None,
    }


def test_valid_confirmed_request_can_acquire():
    client = FakeClient(make_data())

    lease = lease_service.acquire_deletion_lease(
        UID,
        client=client,
        now=NOW,
    )

    assert lease.uid == UID
    assert lease.progress.status == "confirmed"
    assert lease.progress.completed_steps == ()
    assert lease.token


def test_confirmed_request_without_workflow_fields():
    data = {
        "uid": UID,
        "status": "confirmed",
        "leaseToken": None,
        "leaseExpiresAt": None,
    }

    client = FakeClient(data)

    lease = lease_service.acquire_deletion_lease(
        UID,
        client=client,
        now=NOW,
    )

    assert lease.progress.status == "confirmed"
    assert lease.progress.completed_steps == ()


def test_mismatched_status_cannot_acquire():
    client = FakeClient(
        make_data(
            status="processing",
            workflow_status="confirmed",
            steps=["lock_writes"],
        )
    )

    with pytest.raises(DeletionLeaseInvalid):
        lease_service.acquire_deletion_lease(
            UID,
            client=client,
            now=NOW,
        )

    assert client.reference.data["leaseToken"] is None


def test_processing_without_steps_cannot_acquire():
    client = FakeClient(
        make_data(
            status="processing",
            workflow_status="processing",
            steps=[],
        )
    )

    with pytest.raises(DeletionLeaseInvalid):
        lease_service.acquire_deletion_lease(
            UID,
            client=client,
            now=NOW,
        )


def test_invalid_completed_progress_cannot_acquire():
    client = FakeClient(
        make_data(
            status="completed",
            workflow_status="completed",
            steps=["lock_writes"],
        )
    )

    with pytest.raises(DeletionLeaseInvalid):
        lease_service.acquire_deletion_lease(
            UID,
            client=client,
            now=NOW,
        )


def test_valid_first_step_can_be_saved():
    client = FakeClient(make_data())

    lease = lease_service.acquire_deletion_lease(
        UID,
        client=client,
        now=NOW,
    )

    progress = store.save_completed_step(
        lease,
        "lock_writes",
        client=client,
        now=NOW,
    )

    assert progress.status == "processing"
    assert progress.completed_steps == (
        "lock_writes",
    )

    assert (
        client.reference.data["status"]
        == "processing"
    )

    assert (
        client.reference.data["workflowStatus"]
        == "processing"
    )


def test_corrupted_status_blocks_progress_save():
    client = FakeClient(make_data())

    lease = lease_service.acquire_deletion_lease(
        UID,
        client=client,
        now=NOW,
    )

    # Simulate an inconsistent persisted state.
    client.reference.data.update({
        "status": "processing",
        "workflowStatus": "confirmed",
        "completedSteps": ["lock_writes"],
    })

    with pytest.raises(
        store.DeletionProgressConflict
    ):
        store.save_completed_step(
            lease,
            "delete_user_documents",
            client=client,
            now=NOW,
        )

    assert client.reference.data[
        "completedSteps"
    ] == ["lock_writes"]


def test_corrupted_step_order_blocks_progress_save():
    client = FakeClient(make_data())

    lease = lease_service.acquire_deletion_lease(
        UID,
        client=client,
        now=NOW,
    )

    client.reference.data.update({
        "status": "processing",
        "workflowStatus": "processing",
        "completedSteps": ["delete_family_links"],
    })

    with pytest.raises(
        store.DeletionProgressConflict
    ):
        store.save_completed_step(
            lease,
            "delete_user_documents",
            client=client,
            now=NOW,
        )


def test_invalid_state_blocks_lease_renewal():
    client = FakeClient(make_data())

    lease = lease_service.acquire_deletion_lease(
        UID,
        client=client,
        now=NOW,
    )

    client.reference.data.update({
        "status": "processing",
        "workflowStatus": "confirmed",
        "completedSteps": ["lock_writes"],
    })

    with pytest.raises(DeletionLeaseInvalid):
        lease_service.renew_deletion_lease(
            lease,
            client=client,
            now=NOW + timedelta(seconds=30),
        )


def test_invalid_state_blocks_status_read():
    client = FakeClient(
        make_data(
            status="processing",
            workflow_status="confirmed",
            steps=["lock_writes"],
        )
    )

    with pytest.raises(DeletionLeaseInvalid):
        lease_service.get_deletion_lease_status(
            UID,
            client=client,
        )
