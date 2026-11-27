
import pytest

from app.services.account_deletion_workflow import (
    DELETION_STEPS,
    DeletionProgress,
    WorkflowStateError,
    from_firestore_data,
    mark_step_completed,
    new_workflow,
    next_step,
    to_firestore_data,
    validate_progress,
)


def test_new_workflow():
    progress = new_workflow("user-001")

    assert progress.uid == "user-001"
    assert progress.status == "confirmed"
    assert progress.completed_steps == ()
    assert next_step(progress) == "lock_writes"


def test_invalid_uid_rejected():
    with pytest.raises(WorkflowStateError):
        new_workflow("../other-user")


def test_cannot_skip_first_step():
    progress = new_workflow("user-001")

    with pytest.raises(WorkflowStateError):
        mark_step_completed(
            progress,
            "delete_user_documents",
        )


def test_lock_writes_is_first_step():
    progress = new_workflow("user-001")

    updated = mark_step_completed(
        progress,
        "lock_writes",
    )

    assert updated.status == "processing"
    assert updated.completed_steps == (
        "lock_writes",
    )
    assert next_step(
        updated
    ) == "delete_user_documents"


def test_completed_step_is_idempotent():
    progress = new_workflow("user-001")
    progress = mark_step_completed(
        progress,
        "lock_writes",
    )

    retried = mark_step_completed(
        progress,
        "lock_writes",
    )

    assert retried == progress


def test_cannot_finish_early():
    progress = DeletionProgress(
        uid="user-001",
        status="completed",
        completed_steps=("lock_writes",),
    )

    with pytest.raises(WorkflowStateError):
        validate_progress(progress)


def test_full_workflow():
    progress = new_workflow("user-001")

    for step in DELETION_STEPS:
        assert next_step(progress) == step

        progress = mark_step_completed(
            progress,
            step,
        )

    assert progress.status == "completed"
    assert next_step(progress) is None
    assert progress.completed_steps == DELETION_STEPS


def test_firestore_round_trip():
    original = new_workflow("user-001")
    original = mark_step_completed(
        original,
        "lock_writes",
    )

    data = to_firestore_data(original)

    restored = from_firestore_data(
        data,
        "user-001",
    )

    assert restored == original


def test_wrong_firestore_owner_rejected():
    data = {
        "uid": "other-user",
        "workflowStatus": "confirmed",
        "completedSteps": [],
    }

    with pytest.raises(WorkflowStateError):
        from_firestore_data(
            data,
            "user-001",
        )


def test_invalid_step_order_rejected():
    data = {
        "uid": "user-001",
        "workflowStatus": "processing",
        "completedSteps": [
            "delete_family_links"
        ],
    }

    with pytest.raises(WorkflowStateError):
        from_firestore_data(
            data,
            "user-001",
        )


def test_duplicate_steps_rejected():
    data = {
        "uid": "user-001",
        "workflowStatus": "processing",
        "completedSteps": [
            "lock_writes",
            "lock_writes",
        ],
    }

    with pytest.raises(WorkflowStateError):
        from_firestore_data(
            data,
            "user-001",
        )


def test_completed_workflow_is_idempotent():
    progress = new_workflow("user-001")

    for step in DELETION_STEPS:
        progress = mark_step_completed(
            progress,
            step,
        )

    result = mark_step_completed(
        progress,
        "delete_firebase_auth",
    )

    assert result == progress


def test_completed_cannot_accept_unknown_step():
    progress = new_workflow("user-001")

    for step in DELETION_STEPS:
        progress = mark_step_completed(
            progress,
            step,
        )

    with pytest.raises(WorkflowStateError):
        mark_step_completed(
            progress,
            "unknown_step",
        )
