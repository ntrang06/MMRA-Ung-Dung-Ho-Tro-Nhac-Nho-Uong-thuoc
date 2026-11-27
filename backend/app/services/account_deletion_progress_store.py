
from datetime import datetime

from firebase_admin import firestore

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
    from_firestore_data,
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
    Ghi nhận một bước đã hoàn thành.

    Chỉ gọi sau khi thao tác thực tế của bước đó
    đã thành công và được kiểm tra.

    Không tự thực hiện xóa dữ liệu.
    """
    if client is None:
        client = get_lease_client()

    if now is None:
        now = utc_now()

    if now.tzinfo is None:
        raise ValueError(
            "Thời gian phải có timezone."
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
                "Không tìm thấy yêu cầu xóa."
            )

        data = snapshot.to_dict() or {}

        if data.get("uid") != lease.uid:
            raise DeletionLeaseInvalid(
                "UID không khớp."
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
                "Yêu cầu không còn xử lý được."
            )

        try:
            current = from_firestore_data(
                data,
                expected_uid=lease.uid,
            )

            if current.status == "completed":
                raise DeletionProgressConflict(
                    "Workflow đã hoàn tất."
                )

            updated = mark_step_completed(
                current,
                step,
            )

        except WorkflowStateError as error:
            raise DeletionProgressConflict(
                str(error)
            ) from error

        # Bước đã ghi nhận ở lần trước:
        # không cần ghi lại.
        if updated == current:
            return current

        changes = {
            **to_firestore_data(updated),
            "status": updated.status,
            "updatedAt": now,
            "lastCompletedStep": step,
        }

        # Bước cuối: không còn worker xử lý.
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

    return save(client.transaction())
