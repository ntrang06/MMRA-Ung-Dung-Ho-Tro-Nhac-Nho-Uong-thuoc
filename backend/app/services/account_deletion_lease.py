
import secrets
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone

from firebase_admin import firestore

from app.core.firebase import get_firebase_app
from app.services.account_deletion_consistency import (
    DeletionConsistencyError,
    validate_deletion_consistency,
)
from app.services.account_deletion_workflow import (
    DeletionProgress,
    WorkflowStateError,
    next_step,
    validate_uid,
)


LEASE_SECONDS = 180


class DeletionLeaseError(Exception):
    pass


class DeletionLeaseBusy(DeletionLeaseError):
    pass


class DeletionLeaseInvalid(DeletionLeaseError):
    pass


@dataclass(frozen=True)
class DeletionLease:
    uid: str
    token: str
    expires_at: datetime
    progress: DeletionProgress


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


def get_lease_client():
    return firestore.client(
        app=get_firebase_app()
    )


def get_request_reference(client, uid: str):
    validate_uid(uid)

    return (
        client.collection("accountDeletionRequests")
        .document(uid)
    )


def lease_is_active(
    data: dict,
    now: datetime,
) -> bool:
    token = data.get("leaseToken")
    expires_at = data.get("leaseExpiresAt")

    return (
        isinstance(token, str)
        and bool(token)
        and isinstance(expires_at, datetime)
        and expires_at.tzinfo is not None
        and expires_at > now
    )


def require_valid_lease(
    data: dict,
    token: str,
    now: datetime,
) -> None:
    if (
        not isinstance(token, str)
        or not token
        or data.get("leaseToken") != token
        or not lease_is_active(data, now)
    ):
        raise DeletionLeaseInvalid(
            "Quyen xu ly khong hop le hoac da het han."
        )


def read_progress(
    data: dict,
    uid: str,
) -> DeletionProgress:
    """
    Validate persisted deletion state before
    allowing a worker to continue.

    Read-only. Does not delete any data.
    """
    try:
        result = validate_deletion_consistency(
            data,
            expected_uid=uid,
        )
    except (
        DeletionConsistencyError,
        WorkflowStateError,
    ) as error:
        raise WorkflowStateError(
            "Trang thai xoa tai khoan khong nhat quan."
        ) from error

    return result.progress


def acquire_deletion_lease(
    uid: str,
    client=None,
    now: datetime | None = None,
) -> DeletionLease:
    """
    Acquire an exclusive processing lease.

    This operation never deletes user data
    and never marks a workflow step complete.
    """
    validate_uid(uid)

    if client is None:
        client = get_lease_client()

    if now is None:
        now = utc_now()

    if now.tzinfo is None:
        raise ValueError(
            "now phai co timezone."
        )

    reference = get_request_reference(
        client,
        uid,
    )

    token = secrets.token_hex(24)
    expires_at = now + timedelta(
        seconds=LEASE_SECONDS
    )

    @firestore.transactional
    def acquire(transaction):
        snapshot = reference.get(
            transaction=transaction
        )

        if not snapshot.exists:
            raise DeletionLeaseInvalid(
                "Chua co yeu cau xoa tai khoan."
            )

        data = snapshot.to_dict() or {}

        if data.get("uid") != uid:
            raise DeletionLeaseInvalid(
                "UID khong khop."
            )

        if data.get("status") not in {
            "confirmed",
            "processing",
        }:
            raise DeletionLeaseInvalid(
                "Yeu cau chua san sang xu ly."
            )

        try:
            progress = read_progress(
                data,
                uid,
            )
        except WorkflowStateError as error:
            raise DeletionLeaseInvalid(
                "Trang thai workflow khong hop le."
            ) from error

        if progress.status == "completed":
            raise DeletionLeaseInvalid(
                "Workflow da hoan tat."
            )

        if lease_is_active(data, now):
            raise DeletionLeaseBusy(
                "Da co tien trinh khac dang xu ly."
            )

        transaction.update(
            reference,
            {
                "leaseToken": token,
                "leaseExpiresAt": expires_at,
                "updatedAt": now,
            },
        )

        return progress

    progress = acquire(
        client.transaction()
    )

    return DeletionLease(
        uid=uid,
        token=token,
        expires_at=expires_at,
        progress=progress,
    )


def renew_deletion_lease(
    lease: DeletionLease,
    client=None,
    now: datetime | None = None,
) -> DeletionLease:
    """
    Renew the lease only if:
    - its token is still valid;
    - the request remains runnable;
    - persisted progress is consistent.
    """
    validate_uid(lease.uid)

    if client is None:
        client = get_lease_client()

    if now is None:
        now = utc_now()

    if now.tzinfo is None:
        raise ValueError(
            "now phai co timezone."
        )

    reference = get_request_reference(
        client,
        lease.uid,
    )

    expires_at = now + timedelta(
        seconds=LEASE_SECONDS
    )

    @firestore.transactional
    def renew(transaction):
        snapshot = reference.get(
            transaction=transaction
        )

        if not snapshot.exists:
            raise DeletionLeaseInvalid(
                "Khong tim thay yeu cau."
            )

        data = snapshot.to_dict() or {}

        if data.get("uid") != lease.uid:
            raise DeletionLeaseInvalid(
                "UID khong khop."
            )

        require_valid_lease(
            data,
            lease.token,
            now,
        )

        if data.get("status") not in {
            "confirmed",
            "processing",
        }:
            raise DeletionLeaseInvalid(
                "Yeu cau khong con duoc xu ly."
            )

        try:
            progress = read_progress(
                data,
                lease.uid,
            )
        except WorkflowStateError as error:
            raise DeletionLeaseInvalid(
                "Trang thai workflow khong hop le."
            ) from error

        transaction.update(
            reference,
            {
                "leaseExpiresAt": expires_at,
                "updatedAt": now,
            },
        )

        return progress

    progress = renew(
        client.transaction()
    )

    return DeletionLease(
        uid=lease.uid,
        token=lease.token,
        expires_at=expires_at,
        progress=progress,
    )


def release_deletion_lease(
    lease: DeletionLease,
    client=None,
) -> bool:
    """
    Release only the matching lease token.

    Even if progress is inconsistent,
    the current worker may release its
    own token without deleting any data.
    """
    validate_uid(lease.uid)

    if client is None:
        client = get_lease_client()

    reference = get_request_reference(
        client,
        lease.uid,
    )

    @firestore.transactional
    def release(transaction):
        snapshot = reference.get(
            transaction=transaction
        )

        if not snapshot.exists:
            return False

        data = snapshot.to_dict() or {}

        if data.get("uid") != lease.uid:
            return False

        if data.get("leaseToken") != lease.token:
            return False

        transaction.update(
            reference,
            {
                "leaseToken": None,
                "leaseExpiresAt": None,
                "updatedAt": utc_now(),
            },
        )

        return True

    return release(
        client.transaction()
    )


def get_deletion_lease_status(
    uid: str,
    client=None,
) -> dict:
    """
    Read deletion progress without
    executing or modifying any step.
    """
    validate_uid(uid)

    if client is None:
        client = get_lease_client()

    reference = get_request_reference(
        client,
        uid,
    )

    snapshot = reference.get()

    if not snapshot.exists:
        raise DeletionLeaseInvalid(
            "Khong tim thay yeu cau."
        )

    data = snapshot.to_dict() or {}

    if data.get("uid") != uid:
        raise DeletionLeaseInvalid(
            "UID khong khop."
        )

    try:
        progress = read_progress(
            data,
            uid,
        )
    except WorkflowStateError as error:
        raise DeletionLeaseInvalid(
            "Trang thai workflow khong hop le."
        ) from error

    return {
        "status": progress.status,
        "completedSteps": list(
            progress.completed_steps
        ),
        "nextStep": next_step(progress),
        "isProcessing": lease_is_active(
            data,
            utc_now(),
        ),
    }
