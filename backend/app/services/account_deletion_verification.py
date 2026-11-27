
from dataclasses import dataclass

from sqlalchemy.orm import Session

from app.services.account_deletion_inventory import (
    AccountDeletionInventory,
    build_account_deletion_inventory,
)
from app.services.account_deletion_workflow import (
    validate_uid,
)


class DeletionVerificationError(Exception):
    """Cannot safely verify account data removal."""


class DeletionDataRemaining(DeletionVerificationError):
    """Some account data has not been removed."""


@dataclass(frozen=True)
class DeletionVerificationResult:
    uid: str
    user_documents: int
    family_links: int
    family_invitations: int
    password_reset_records: int
    verified: bool


def verify_account_data_removed(
    uid: str,
    db: Session,
) -> DeletionVerificationResult:
    """
    Verify that known account data has been removed.

    Read-only:
    - Does not delete Firestore documents.
    - Does not delete PostgreSQL records.
    - Does not delete Firebase Authentication.

    Must succeed before the Auth deletion step.
    """

    try:
        validate_uid(uid)
    except ValueError as error:
        raise DeletionVerificationError(
            "Invalid account UID."
        ) from error

    try:
        inventory: AccountDeletionInventory = (
            build_account_deletion_inventory(
                uid,
                db,
            )
        )
    except Exception as error:
        raise DeletionVerificationError(
            "Unable to verify account data removal."
        ) from error

    counts = {
        "user_documents": inventory.user_documents,
        "family_links": inventory.family_links,
        "family_invitations": inventory.family_invitations,
        "password_reset_records": (
            inventory.password_reset_records
        ),
    }

    # Invalid counts must not be treated as zero.
    for value in counts.values():
        if (
            type(value) is not int
            or value < 0
        ):
            raise DeletionVerificationError(
                "Invalid deletion inventory result."
            )

    if any(value > 0 for value in counts.values()):
        raise DeletionDataRemaining(
            "Account data still exists. "
            "Firebase Authentication must not be deleted."
        )

    return DeletionVerificationResult(
        uid=uid,
        user_documents=0,
        family_links=0,
        family_invitations=0,
        password_reset_records=0,
        verified=True,
    )
