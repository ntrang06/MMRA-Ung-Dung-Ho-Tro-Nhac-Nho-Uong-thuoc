
import logging
from dataclasses import dataclass

from firebase_admin import auth, firestore
from google.cloud.firestore_v1.base_query import FieldFilter
from sqlalchemy import delete
from sqlalchemy.orm import Session

from app.core.firebase import get_firebase_app
from app.db.models.password_reset import (
    PasswordResetChallenge,
)


logger = logging.getLogger(__name__)

MAX_FIRESTORE_DEPTH = 50


@dataclass(frozen=True)
class CleanupResult:
    user_documents: int = 0
    family_links: int = 0
    family_invitations: int = 0
    password_reset_records: int = 0


def validate_uid(uid: str) -> str:
    if (
        not isinstance(uid, str)
        or not uid
        or "/" in uid
        or uid in {".", ".."}
    ):
        raise ValueError("Firebase UID không hợp lệ.")

    return uid


# ============================================================
# 1. DELETE FIRESTORE DOCUMENT TREE
# ============================================================

def delete_document_tree(
    reference,
    depth: int = 0,
) -> int:
    """
    Xóa document và tất cả subcollection.

    Xóa từ document con trước rồi đến cha.
    Có thể gọi lại nếu lần trước bị gián đoạn.
    """
    if depth >= MAX_FIRESTORE_DEPTH:
        raise ValueError(
            "Cấu trúc Firestore lồng quá sâu."
        )

    deleted = 0

    for collection in reference.collections():
        for snapshot in collection.stream():
            deleted += delete_document_tree(
                snapshot.reference,
                depth + 1,
            )

    snapshot = reference.get()

    if snapshot.exists:
        reference.delete()
        deleted += 1

    return deleted


def delete_user_documents(
    uid: str,
    client,
) -> int:
    uid = validate_uid(uid)

    reference = (
        client.collection("users")
        .document(uid)
    )

    return delete_document_tree(reference)


# ============================================================
# 2. DELETE FAMILY RELATIONSHIPS
# ============================================================

def delete_related_documents(
    uid: str,
    client,
    collection_name: str,
    fields: tuple[str, ...],
) -> int:
    uid = validate_uid(uid)

    references = {}

    for field in fields:
        query = (
            client.collection(collection_name)
            .where(
                filter=FieldFilter(
                    field,
                    "==",
                    uid,
                )
            )
        )

        for snapshot in query.stream():
            references[
                snapshot.reference.path
            ] = snapshot.reference

    deleted = 0

    for reference in references.values():
        # Xóa liên kết, không xóa tài khoản
        # hoặc hồ sơ của người còn lại.
        reference.delete()
        deleted += 1

    return deleted


def delete_family_links(
    uid: str,
    client,
) -> int:
    return delete_related_documents(
        uid,
        client,
        "familyLinks",
        ("ownerUid", "caregiverUid"),
    )


def delete_family_invitations(
    uid: str,
    client,
) -> int:
    return delete_related_documents(
        uid,
        client,
        "familyInvitations",
        ("ownerUid", "acceptedByUid"),
    )


# ============================================================
# 3. DELETE POSTGRESQL OTP
# ============================================================

def delete_password_reset_records(
    uid: str,
    db: Session,
) -> int:
    uid = validate_uid(uid)

    try:
        statement = (
            delete(PasswordResetChallenge)
            .where(
                PasswordResetChallenge.firebase_uid
                == uid
            )
        )

        result = db.execute(statement)
        db.commit()

        return max(
            0,
            result.rowcount or 0,
        )

    except Exception:
        db.rollback()
        logger.exception(
            "Cannot delete password reset records"
        )
        raise


# ============================================================
# 4. DELETE FIREBASE AUTH
# ============================================================

def delete_firebase_auth_user(
    uid: str,
    firebase_app,
) -> None:
    uid = validate_uid(uid)

    try:
        auth.delete_user(
            uid,
            app=firebase_app,
        )

    except auth.UserNotFoundError:
        # Nếu đã xóa ở lần chạy trước,
        # không coi đó là lỗi.
        return


# ============================================================
# 5. CREATE SERVICE CLIENTS
# ============================================================

def get_cleanup_firestore_client():
    return firestore.client(
        app=get_firebase_app()
    )
