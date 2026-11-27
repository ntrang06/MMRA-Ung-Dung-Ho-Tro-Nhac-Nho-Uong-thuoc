
from datetime import datetime, timedelta, timezone

import pytest

from app.services.account_deletion_lease import (
    DeletionLease,
)
from app.services.account_deletion_worker import (
    DeletionWorkerStopped,
    simulate_deletion_step,
)
from app.services.account_deletion_workflow import (
    new_workflow,
)


UID = "user-001"
TOKEN = "worker-001"

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
        self.reads = 0
        self.writes = 0

    def get(self):
        self.reads += 1
        return FakeSnapshot(self.data)

    def update(self, changes):
        self.writes += 1
        self.data.update(changes)


class FakeClient:
    def __init__(self, data):
        self.reference = FakeReference(data)

    def collection(self, name):
        assert name == "accountDeletionRequests"
        return self

    def document(self, uid):
        assert uid == UID
        return self.reference


def make_data(
    status="confirmed",
    workflow_status="confirmed",
    steps=None,
    token=TOKEN,
    expires_at=None,
):
    if steps is None:
        steps = []

    if expires_at is None:
        expires_at = NOW + timedelta(
            seconds=180
        )

    return {
        "uid": UID,
        "status": status,
        "workflowStatus": workflow_status,
        "completedSteps": list(steps),
        "leaseToken": token,
        "leaseExpiresAt": expires_at,
    }


def make_lease(token=TOKEN):
    return DeletionLease(
        uid=UID,
        token=token,
        expires_at=NOW + timedelta(
            seconds=180
        ),
        progress=new_workflow(UID),
    )


def simulate(
    client,
    *,
    lease=None,
    now=NOW,
    after_time=NOW,
    callback=None,
):
    return simulate_deletion_step(
        lease or make_lease(),
        client=client,
        now=now,
        after_time=after_time,
        on_simulation=callback,
    )


def test_valid_worker_simulates_first_step():
    client = FakeClient(make_data())
    original = dict(client.reference.data)

    result = simulate(client)

    assert result.uid == UID
    assert result.step == "lock_writes"
    assert result.validated_before
    assert result.validated_after
    assert result.data_deleted is False
    assert result.progress_saved is False

    assert client.reference.reads == 2
    assert client.reference.writes == 0
    assert client.reference.data == original


def test_processing_resumes_correct_step():
    client = FakeClient(
        make_data(
            status="processing",
            workflow_status="processing",
            steps=["lock_writes"],
        )
    )

    result = simulate(client)

    assert result.step == "delete_user_documents"


def test_expired_worker_cannot_start():
    client = FakeClient(
        make_data(
            expires_at=NOW - timedelta(seconds=1)
        )
    )

    with pytest.raises(DeletionWorkerStopped):
        simulate(client)

    assert client.reference.writes == 0


def test_worker_expiring_during_step_is_rejected():
    client = FakeClient(make_data())

    with pytest.raises(DeletionWorkerStopped):
        simulate(
            client,
            after_time=NOW + timedelta(
                seconds=181
            ),
        )

    assert client.reference.writes == 0


def test_replaced_worker_cannot_start():
    client = FakeClient(
        make_data(token="new-worker")
    )

    with pytest.raises(DeletionWorkerStopped):
        simulate(client)

    assert client.reference.writes == 0


def test_worker_replaced_during_step_is_rejected():
    client = FakeClient(make_data())

    def replace_worker():
        client.reference.data[
            "leaseToken"
        ] = "new-worker"

    with pytest.raises(DeletionWorkerStopped):
        simulate(
            client,
            callback=replace_worker,
        )


def test_state_corrupted_during_step_is_rejected():
    client = FakeClient(make_data())

    def corrupt_state():
        client.reference.data.update({
            "status": "processing",
            "workflowStatus": "confirmed",
            "completedSteps": ["lock_writes"],
        })

    with pytest.raises(DeletionWorkerStopped):
        simulate(
            client,
            callback=corrupt_state,
        )


def test_progress_changed_during_step_is_rejected():
    client = FakeClient(make_data())

    def change_progress():
        client.reference.data.update({
            "status": "processing",
            "workflowStatus": "processing",
            "completedSteps": ["lock_writes"],
        })

    with pytest.raises(DeletionWorkerStopped):
        simulate(
            client,
            callback=change_progress,
        )


def test_simulated_operation_failure_stops_worker():
    client = FakeClient(make_data())

    def failed_operation():
        raise RuntimeError(
            "Simulated interruption"
        )

    with pytest.raises(DeletionWorkerStopped):
        simulate(
            client,
            callback=failed_operation,
        )

    assert client.reference.writes == 0


def test_missing_request_is_rejected():
    client = FakeClient(None)

    with pytest.raises(DeletionWorkerStopped):
        simulate(client)


def test_completed_request_is_rejected():
    client = FakeClient(
        make_data(
            status="completed",
            workflow_status="completed",
            steps=[
                "lock_writes",
                "delete_user_documents",
                "delete_family_links",
                "delete_family_invitations",
                "delete_password_reset_records",
                "verify_data_removed",
                "delete_firebase_auth",
            ],
        )
    )

    with pytest.raises(DeletionWorkerStopped):
        simulate(client)


def test_naive_start_time_rejected():
    client = FakeClient(make_data())

    with pytest.raises(DeletionWorkerStopped):
        simulate(
            client,
            now=datetime(2026, 11, 27),
        )


def test_naive_completion_time_rejected():
    client = FakeClient(make_data())

    with pytest.raises(DeletionWorkerStopped):
        simulate(
            client,
            after_time=datetime(2026, 11, 27),
        )


def test_time_cannot_go_backwards():
    client = FakeClient(make_data())

    with pytest.raises(DeletionWorkerStopped):
        simulate(
            client,
            after_time=NOW - timedelta(seconds=1),
        )


def test_simulation_never_marks_step_completed():
    client = FakeClient(make_data())

    result = simulate(client)

    assert result.progress_saved is False
    assert client.reference.data[
        "completedSteps"
    ] == []
    assert client.reference.data[
        "status"
    ] == "confirmed"
    assert client.reference.writes == 0


def test_callback_is_only_invoked_once():
    client = FakeClient(make_data())
    calls = []

    def simulated_operation():
        calls.append("executed")

    simulate(
        client,
        callback=simulated_operation,
    )

    assert calls == ["executed"]
