
from dataclasses import dataclass
from datetime import datetime, timezone

from app.services.account_deletion_consistency import (
    DeletionConsistencyError,
    validate_deletion_consistency,
)
from app.services.account_deletion_lease import (
    DeletionLease,
    DeletionLeaseInvalid,
    get_lease_client,
    get_request_reference,
    require_valid_lease,
)
from app.services.account_deletion_workflow import (
    validate_uid,
)


class DeletionPreflightError(ValueError):
    pass


@dataclass(frozen=True)
class DeletionPreflightResult:
    uid: str
    status: str
    next_step: str
    completed_steps: tuple[str, ...]


def check_deletion_preflight(
    lease: DeletionLease,
    *,
    client=None,
    now: datetime | None = None,
) -> DeletionPreflightResult:
    """
    Read-only check before executing a deletion step.

    Checks:
    - The request exists and belongs to the user.
    - The lease token is currently active.
    - The request state is consistent.
    - A deletion step remains to be executed.

    Does NOT:
    - Delete data.
    - Mark any step completed.
    - Renew or acquire a lease.
    - Guarantee the lease stays active afterward.
    """
    try:
        validate_uid(lease.uid)
    except ValueError as error:
        raise DeletionPreflightError(
            "Invalid account UID."
        ) from error

    if now is None:
        now = datetime.now(timezone.utc)

    if (
        not isinstance(now, datetime)
        or now.tzinfo is None
    ):
        raise DeletionPreflightError(
            "Current time must have a timezone."
        )

    if client is None:
        client = get_lease_client()

    reference = get_request_reference(
        client,
        lease.uid,
    )

    snapshot = reference.get()

    if not snapshot.exists:
        raise DeletionPreflightError(
            "Deletion request not found."
        )

    data = snapshot.to_dict() or {}

    if data.get("uid") != lease.uid:
        raise DeletionPreflightError(
            "Deletion request UID mismatch."
        )

    try:
        require_valid_lease(
            data,
            lease.token,
            now,
        )
    except DeletionLeaseInvalid as error:
        raise DeletionPreflightError(
            "Deletion lease is invalid or expired."
        ) from error

    try:
        result = validate_deletion_consistency(
            data,
            expected_uid=lease.uid,
        )
    except DeletionConsistencyError as error:
        raise DeletionPreflightError(
            "Deletion workflow is inconsistent."
        ) from error

    if result.status not in {
        "confirmed",
        "processing",
    }:
        raise DeletionPreflightError(
            "Deletion request is not runnable."
        )

    if (
        result.is_complete
        or result.next_step is None
    ):
        raise DeletionPreflightError(
            "Deletion workflow has no remaining step."
        )

    return DeletionPreflightResult(
        uid=lease.uid,
        status=result.status,
        next_step=result.next_step,
        completed_steps=(
            result.progress.completed_steps
        ),
    )
