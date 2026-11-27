
from dataclasses import dataclass
from typing import Any

from app.services.account_deletion_workflow import (
    DELETION_STEPS,
    DeletionProgress,
    WorkflowStateError,
    from_firestore_data,
    validate_uid,
)


class DeletionConsistencyError(ValueError):
    """Account deletion state is inconsistent."""


@dataclass(frozen=True)
class DeletionConsistencyResult:
    uid: str
    status: str
    progress: DeletionProgress
    next_step: str | None
    is_complete: bool


def validate_deletion_consistency(
    data: dict[str, Any],
    expected_uid: str,
) -> DeletionConsistencyResult:
    """
    Check the relationship between:
    - status
    - workflowStatus
    - completedSteps

    This function is read-only.
    It does not modify Firestore or delete data.
    """

    try:
        validate_uid(expected_uid)
    except WorkflowStateError as error:
        raise DeletionConsistencyError(
            "Invalid expected UID."
        ) from error

    if not isinstance(data, dict):
        raise DeletionConsistencyError(
            "Deletion request must be a dictionary."
        )

    if data.get("uid") != expected_uid:
        raise DeletionConsistencyError(
            "Deletion request UID mismatch."
        )

    status = data.get("status")

    if status not in {
        "confirmed",
        "processing",
        "completed",
    }:
        raise DeletionConsistencyError(
            "Deletion request is not in a runnable state."
        )

    # A newly confirmed request may not have
    # workflow fields yet.
    if status == "confirmed":
        if data.get(
            "workflowStatus",
            "confirmed",
        ) != "confirmed":
            raise DeletionConsistencyError(
                "Confirmed request has invalid workflow status."
            )

        if data.get("completedSteps", []) != []:
            raise DeletionConsistencyError(
                "Confirmed request cannot have completed steps."
            )

    # Processing and completed requests must
    # have persisted workflow information.
    if status in {"processing", "completed"}:
        if "workflowStatus" not in data:
            raise DeletionConsistencyError(
                "Missing workflow status."
            )

        if "completedSteps" not in data:
            raise DeletionConsistencyError(
                "Missing completed steps."
            )

        if data["workflowStatus"] != status:
            raise DeletionConsistencyError(
                "Request status and workflow status mismatch."
            )

    try:
        progress = from_firestore_data(
            data,
            expected_uid,
        )
    except WorkflowStateError as error:
        raise DeletionConsistencyError(
            "Invalid deletion workflow."
        ) from error

    if progress.status != status:
        raise DeletionConsistencyError(
            "Workflow status mismatch."
        )

    completed = progress.completed_steps

    if status == "processing" and not completed:
        raise DeletionConsistencyError(
            "Processing request has no completed steps."
        )

    if status == "completed":
        if completed != DELETION_STEPS:
            raise DeletionConsistencyError(
                "Completed request has unfinished steps."
            )

        return DeletionConsistencyResult(
            uid=expected_uid,
            status=status,
            progress=progress,
            next_step=None,
            is_complete=True,
        )

    if len(completed) >= len(DELETION_STEPS):
        raise DeletionConsistencyError(
            "All steps finished but request is not completed."
        )

    return DeletionConsistencyResult(
        uid=expected_uid,
        status=status,
        progress=progress,
        next_step=DELETION_STEPS[
            len(completed)
        ],
        is_complete=False,
    )
