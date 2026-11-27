
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Callable

from app.services.account_deletion_lease import (
    DeletionLease,
)
from app.services.account_deletion_preflight import (
    DeletionPreflightError,
    check_deletion_preflight,
)


class DeletionWorkerError(Exception):
    pass


class DeletionWorkerStopped(DeletionWorkerError):
    pass


@dataclass(frozen=True)
class SimulatedStepResult:
    uid: str
    step: str
    validated_before: bool
    validated_after: bool
    data_deleted: bool = False
    progress_saved: bool = False


def require_aware_time(
    value: datetime,
) -> datetime:
    if (
        not isinstance(value, datetime)
        or value.tzinfo is None
        or value.utcoffset() is None
    ):
        raise DeletionWorkerStopped(
            "Worker time must be timezone-aware."
        )

    return value


def simulate_deletion_step(
    lease: DeletionLease,
    *,
    client,
    now: datetime | None = None,
    after_time: datetime | None = None,
    on_simulation: Callable[[], None] | None = None,
) -> SimulatedStepResult:
    """
    Simulate a deletion step without executing
    any real cleanup operation.

    The optional callback is ONLY for tests
    that simulate worker interruption or
    Firestore state changes.

    Never pass actual delete functions to
    on_simulation.

    This function does NOT:
    - Delete Firestore data
    - Delete PostgreSQL data
    - Delete Firebase Auth
    - Save completed steps
    - Renew or release a lease

    A passing simulation does not guarantee
    exclusive execution during real deletion.
    """
    if now is None:
        now = datetime.now(timezone.utc)

    require_aware_time(now)

    if after_time is None:
        after_time = now

    require_aware_time(after_time)

    if after_time < now:
        raise DeletionWorkerStopped(
            "Completion time cannot precede start time."
        )

    try:
        before = check_deletion_preflight(
            lease,
            client=client,
            now=now,
        )
    except DeletionPreflightError as error:
        raise DeletionWorkerStopped(
            "Worker cannot start: invalid lease or state."
        ) from error

    if on_simulation is not None:
        if not callable(on_simulation):
            raise DeletionWorkerStopped(
                "Simulation callback must be callable."
            )

        try:
            on_simulation()
        except Exception as error:
            raise DeletionWorkerStopped(
                "Simulated step failed."
            ) from error

    try:
        after = check_deletion_preflight(
            lease,
            client=client,
            now=after_time,
        )
    except DeletionPreflightError as error:
        raise DeletionWorkerStopped(
            "Worker lost permission during simulation."
        ) from error

    if (
        before.uid != after.uid
        or before.next_step != after.next_step
        or before.completed_steps != after.completed_steps
        or before.status != after.status
    ):
        raise DeletionWorkerStopped(
            "Deletion state changed during simulation."
        )

    return SimulatedStepResult(
        uid=lease.uid,
        step=before.next_step,
        validated_before=True,
        validated_after=True,
    )
