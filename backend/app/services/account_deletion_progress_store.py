
from datetime import datetime

from firebase_admin import firestore

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
    utc_now,
)
from app.services.account_deletion_workflow import (
    DeletionProgress,
    WorkflowStateError,
    mark_step_completed,
    to_firestore_data,
)


class DeletionProgressError(Exception):
    pass


class DeletionProgressConflict(DeletionProgressError):
    pass


def save_completed_step(
    lease: DeletionLease,
    step: str,
    *,
    client=None,
    now: datetime | None = None,
) -> DeletionProgress:
    """
    Save a completed workflow step.

    The actual deletion operation must
    succeed before calling this function.

    A valid lease and consistent persisted
    state are both required.

    This function does not delete user data.
    """
    if client is None:
        client = get_lease_client()

    if now is None:
        now = utc_now()

    if now.tzinfo is None:
        raise ValueError(
            "Thoi gian phai co timezone."
        )

    reference = get_request_reference(
        client,
        lease.uid,
    )

    @firestore.transactional
    def save(transaction):
        snapshot = reference.get(
            transaction=transaction
        )

        if not snapshot.exists:
            raise DeletionLeaseInvalid(
                "Khong tim thay yeu cau xoa."
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
                "Yeu cau khong con xu ly duoc."
            )

        try:
            consistency = (
                validate_deletion_consistency(
                    data,
                    expected_uid=lease.uid,
                )
            )

            current = consistency.progress

            if current.status == "completed":
                raise DeletionProgressConflict(
                    "Workflow da hoan tat."
                )

            updated = mark_step_completed(
                current,
                step,
            )

        except (
            DeletionConsistencyError,
            WorkflowStateError,
        ) as error:
            raise DeletionProgressConflict(
                "Trang thai hoac thu tu "
                "workflow khong hop le."
            ) from error

        # A repeated successful step should
        # not create another Firestore write.
        if updated == current:
            return current

        changes = {
            **to_firestore_data(updated),
            "status": updated.status,
            "updatedAt": now,
            "lastCompletedStep": step,
        }

        if updated.status == "completed":
            changes.update({
                "completedAt": now,
                "leaseToken": None,
                "leaseExpiresAt": None,
            })

        transaction.update(
            reference,
            changes,
        )

        return updated

    return save(
        client.transaction()
    )
