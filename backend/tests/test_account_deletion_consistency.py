
import pytest

from app.services.account_deletion_consistency import (
    DeletionConsistencyError,
    validate_deletion_consistency,
)
from app.services.account_deletion_workflow import (
    DELETION_STEPS,
)


UID = "user-001"


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
    }


def test_new_confirmed_request():
    data = {
        "uid": UID,
        "status": "confirmed",
    }

    result = validate_deletion_consistency(
        data,
        UID,
    )

    assert result.status == "confirmed"
    assert result.next_step == "lock_writes"
    assert result.is_complete is False


def test_confirmed_with_explicit_workflow():
    result = validate_deletion_consistency(
        make_data(),
        UID,
    )

    assert result.next_step == "lock_writes"


def test_processing_with_first_step():
    result = validate_deletion_consistency(
        make_data(
            status="processing",
            workflow_status="processing",
            steps=["lock_writes"],
        ),
        UID,
    )

    assert result.next_step == "delete_user_documents"
    assert result.is_complete is False


def test_completed_with_all_steps():
    result = validate_deletion_consistency(
        make_data(
            status="completed",
            workflow_status="completed",
            steps=DELETION_STEPS,
        ),
        UID,
    )

    assert result.is_complete is True
    assert result.next_step is None


def test_wrong_uid_rejected():
    data = make_data()
    data["uid"] = "other-user"

    with pytest.raises(DeletionConsistencyError):
        validate_deletion_consistency(
            data,
            UID,
        )


@pytest.mark.parametrize(
    "status",
    [
        "pending",
        "failed",
        "unknown",
        None,
    ],
)
def test_non_runnable_status_rejected(status):
    data = make_data(status=status)

    with pytest.raises(DeletionConsistencyError):
        validate_deletion_consistency(
            data,
            UID,
        )


def test_confirmed_with_completed_steps_rejected():
    data = make_data(
        steps=["lock_writes"],
    )

    with pytest.raises(DeletionConsistencyError):
        validate_deletion_consistency(
            data,
            UID,
        )


def test_status_mismatch_rejected():
    data = make_data(
        status="processing",
        workflow_status="confirmed",
        steps=["lock_writes"],
    )

    with pytest.raises(DeletionConsistencyError):
        validate_deletion_consistency(
            data,
            UID,
        )


def test_processing_without_steps_rejected():
    data = make_data(
        status="processing",
        workflow_status="processing",
        steps=[],
    )

    with pytest.raises(DeletionConsistencyError):
        validate_deletion_consistency(
            data,
            UID,
        )


def test_missing_processing_workflow_rejected():
    data = {
        "uid": UID,
        "status": "processing",
    }

    with pytest.raises(DeletionConsistencyError):
        validate_deletion_consistency(
            data,
            UID,
        )


def test_out_of_order_steps_rejected():
    data = make_data(
        status="processing",
        workflow_status="processing",
        steps=["delete_family_links"],
    )

    with pytest.raises(DeletionConsistencyError):
        validate_deletion_consistency(
            data,
            UID,
        )


def test_duplicate_steps_rejected():
    data = make_data(
        status="processing",
        workflow_status="processing",
        steps=[
            "lock_writes",
            "lock_writes",
        ],
    )

    with pytest.raises(DeletionConsistencyError):
        validate_deletion_consistency(
            data,
            UID,
        )


def test_completed_without_all_steps_rejected():
    data = make_data(
        status="completed",
        workflow_status="completed",
        steps=["lock_writes"],
    )

    with pytest.raises(DeletionConsistencyError):
        validate_deletion_consistency(
            data,
            UID,
        )


def test_completed_missing_steps_rejected():
    data = {
        "uid": UID,
        "status": "completed",
        "workflowStatus": "completed",
    }

    with pytest.raises(DeletionConsistencyError):
        validate_deletion_consistency(
            data,
            UID,
        )


def test_processing_with_all_steps_rejected():
    data = make_data(
        status="processing",
        workflow_status="processing",
        steps=DELETION_STEPS,
    )

    with pytest.raises(DeletionConsistencyError):
        validate_deletion_consistency(
            data,
            UID,
        )


def test_invalid_input_rejected():
    with pytest.raises(DeletionConsistencyError):
        validate_deletion_consistency(
            None,
            UID,
        )
