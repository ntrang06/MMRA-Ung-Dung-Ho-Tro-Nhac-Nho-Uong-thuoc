
from dataclasses import dataclass
from typing import Any


# Thu tu xu ly phai duoc giu nguyen.
DELETION_STEPS = (
    "lock_writes",
    "delete_user_documents",
    "delete_family_links",
    "delete_family_invitations",
    "delete_password_reset_records",
    "verify_data_removed",
    "delete_firebase_auth",
)

VALID_STATUSES = {
    "confirmed",
    "processing",
    "completed",
}


class WorkflowStateError(ValueError):
    pass


@dataclass(frozen=True)
class DeletionProgress:
    uid: str
    status: str
    completed_steps: tuple[str, ...]


def validate_uid(uid: str) -> str:
    if (
        not isinstance(uid, str)
        or not uid
        or "/" in uid
        or uid in {".", ".."}
    ):
        raise WorkflowStateError(
            "UID khong hop le."
        )

    return uid


def validate_progress(
    progress: DeletionProgress,
) -> None:
    validate_uid(progress.uid)

    if progress.status not in VALID_STATUSES:
        raise WorkflowStateError(
            "Trang thai workflow khong hop le."
        )

    completed = progress.completed_steps

    if not isinstance(completed, tuple):
        raise WorkflowStateError(
            "completed_steps phai la tuple."
        )

    if len(completed) > len(DELETION_STEPS):
        raise WorkflowStateError(
            "So buoc da hoan thanh khong hop le."
        )

    # Chi cho phep mot prefix lien tuc:
    # khong duoc nhay qua buoc chua xu ly.
    if completed != DELETION_STEPS[:len(completed)]:
        raise WorkflowStateError(
            "Thu tu cac buoc xoa khong hop le."
        )

    if progress.status == "confirmed" and completed:
        raise WorkflowStateError(
            "Workflow chua bat dau nhung da co buoc xong."
        )

    if (
        progress.status == "completed"
        and completed != DELETION_STEPS
    ):
        raise WorkflowStateError(
            "Khong the hoan tat khi con buoc chua xong."
        )

    if (
        progress.status == "processing"
        and completed == DELETION_STEPS
    ):
        raise WorkflowStateError(
            "Workflow da het buoc nhung chua completed."
        )


def new_workflow(uid: str) -> DeletionProgress:
    progress = DeletionProgress(
        uid=validate_uid(uid),
        status="confirmed",
        completed_steps=(),
    )

    validate_progress(progress)
    return progress


def next_step(
    progress: DeletionProgress,
) -> str | None:
    validate_progress(progress)

    if progress.status == "completed":
        return None

    return DELETION_STEPS[
        len(progress.completed_steps)
    ]


def mark_step_completed(
    progress: DeletionProgress,
    step: str,
) -> DeletionProgress:
    """
    Chi cap nhat trang thai trong bo nho.

    Ham nay KHONG tu goi Firestore,
    PostgreSQL hoac Firebase Auth.

    Bo dieu phoi sau nay chi duoc goi ham nay
    sau khi mot thao tac thuc te thanh cong.
    """
    validate_progress(progress)

    if progress.status == "completed":
        if step in progress.completed_steps:
            return progress

        raise WorkflowStateError(
            "Workflow da hoan tat."
        )

    if step in progress.completed_steps:
        # Retry buoc da ghi nhan thanh cong.
        return progress

    expected_step = next_step(progress)

    if step != expected_step:
        raise WorkflowStateError(
            f"Sai thu tu. Buoc tiep theo: {expected_step}"
        )

    updated_steps = (
        *progress.completed_steps,
        step,
    )

    new_status = (
        "completed"
        if updated_steps == DELETION_STEPS
        else "processing"
    )

    result = DeletionProgress(
        uid=progress.uid,
        status=new_status,
        completed_steps=updated_steps,
    )

    validate_progress(result)
    return result


def to_firestore_data(
    progress: DeletionProgress,
) -> dict[str, Any]:
    """
    Chuyen workflow thanh du lieu co the luu.

    Khong tu ghi vao Firestore.
    """
    validate_progress(progress)

    return {
        "uid": progress.uid,
        "workflowStatus": progress.status,
        "completedSteps": list(
            progress.completed_steps
        ),
    }


def from_firestore_data(
    data: dict[str, Any],
    expected_uid: str,
) -> DeletionProgress:
    """
    Doc workflow tu document da duoc xac minh.

    Khong tin UID tu du lieu nguoi dung
    neu khong trung UID dang xu ly.
    """
    validate_uid(expected_uid)

    if not isinstance(data, dict):
        raise WorkflowStateError(
            "Du lieu workflow khong hop le."
        )

    if data.get("uid") != expected_uid:
        raise WorkflowStateError(
            "UID khong khop."
        )

    steps = data.get(
        "completedSteps",
        [],
    )

    if (
        not isinstance(steps, list)
        or not all(
            isinstance(step, str)
            for step in steps
        )
    ):
        raise WorkflowStateError(
            "Danh sach buoc khong hop le."
        )

    progress = DeletionProgress(
        uid=expected_uid,
        status=data.get(
            "workflowStatus",
            "confirmed",
        ),
        completed_steps=tuple(steps),
    )

    validate_progress(progress)
    return progress
