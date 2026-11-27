
from datetime import datetime, timedelta, timezone

import pytest

from app.services.account_deletion_lease import (
    DeletionLease,
)
from app.services.account_deletion_preflight import (
    DeletionPreflightError,
    check_deletion_preflight,
)
from app.services.account_deletion_workflow import (
    DELETION_STEPS,
    new_workflow,
)


UID = "user-001"
TOKEN = "test-worker-token"

NOW = datetime(
    2026, 11, 27, 9, 0,
    tzinfo=timezone.utc,
)


class FakeSnapshot:
    def __init__(self, data):
        self.data = data
        self.exists = data is not None

    def to_dict(self):
        return (
            dict(self.data)
            if self.exists
            else None
        )


class FakeReference:
    def __init__(self, data):
        self.data = data
        self.read_count = 0

    def get(self):
        self.read_count += 1
        return FakeSnapshot(self.data)


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
    *,
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


def run_preflight(data, lease=None, now=NOW):
    client = FakeClient(data)

    result = check_deletion_preflight(
        lease or make_lease(),
        client=client,
        now=now,
    )

    return result, client


def test_confirmed_request_is_ready():
    data = make_data()

    result, client = run_preflight(data)

    assert result.uid == UID
    assert result.status == "confirmed"
    assert result.next_step == "lock_writes"
    assert result.completed_steps == ()

    # Read-only: no mutations.
    assert client.reference.data == data
    assert client.reference.read_count == 1


def test_processing_request_resumes_next_step():
    result, _ = run_preflight(
        make_data(
            status="processing",
            workflow_status="processing",
            steps=["lock_writes"],
        )
    )

    assert (
        result.next_step
        == "delete_user_documents"
    )
    assert result.completed_steps == (
        "lock_writes",
    )


def test_missing_request_is_rejected():
    with pytest.raises(DeletionPreflightError):
        run_preflight(None)


def test_wrong_uid_is_rejected():
    data = make_data()
    data["uid"] = "other-user"

    with pytest.raises(DeletionPreflightError):
        run_preflight(data)


def test_wrong_lease_token_is_rejected():
    with pytest.raises(DeletionPreflightError):
        run_preflight(
            make_data(),
            lease=make_lease(
                token="another-worker"
            ),
        )


def test_expired_lease_is_rejected():
    with pytest.raises(DeletionPreflightError):
        run_preflight(
            make_data(),
            now=NOW + timedelta(
                seconds=181
            ),
        )


def test_mismatched_status_is_rejected():
    with pytest.raises(DeletionPreflightError):
        run_preflight(
            make_data(
                status="processing",
                workflow_status="confirmed",
                steps=["lock_writes"],
            )
        )


def test_invalid_step_order_is_rejected():
    with pytest.raises(DeletionPreflightError):
        run_preflight(
            make_data(
                status="processing",
                workflow_status="processing",
                steps=["delete_family_links"],
            )
        )


def test_completed_request_is_rejected():
    with pytest.raises(DeletionPreflightError):
        run_preflight(
            make_data(
                status="completed",
                workflow_status="completed",
                steps=DELETION_STEPS,
            )
        )


def test_failed_request_is_rejected():
    with pytest.raises(DeletionPreflightError):
        run_preflight(
            make_data(
                status="failed",
                workflow_status="processing",
                steps=["lock_writes"],
            )
        )


def test_missing_workflow_fields_allowed_when_confirmed():
    data = {
        "uid": UID,
        "status": "confirmed",
        "leaseToken": TOKEN,
        "leaseExpiresAt": (
            NOW + timedelta(seconds=180)
        ),
    }

    result, _ = run_preflight(data)

    assert result.next_step == "lock_writes"


def test_naive_time_is_rejected():
    with pytest.raises(DeletionPreflightError):
        run_preflight(
            make_data(),
            now=datetime(2026, 11, 27),
        )


def test_expired_token_cannot_be_used_after_replacement():
    data = make_data(
        token="new-worker-token"
    )

    with pytest.raises(DeletionPreflightError):
        run_preflight(
            data,
            lease=make_lease(
                token="old-worker-token"
            ),
        )


def test_pending_request_is_rejected():
    with pytest.raises(DeletionPreflightError):
        run_preflight(
            make_data(
                status="pending",
                workflow_status="confirmed",
            )
        )
