
from dataclasses import dataclass

from firebase_admin import firestore
from google.cloud.firestore_v1.base_query import FieldFilter
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.core.firebase import get_firebase_app
from app.db.models.password_reset import PasswordResetChallenge


MAX_DOCUMENTS = 10000


class InventoryLimitExceeded(Exception):
    pass


@dataclass(frozen=True)
class AccountDeletionInventory:
    user_documents: int
    family_links: int
    family_invitations: int
    password_reset_records: int


def count_user_documents(client, uid: str) -> int:
    """
    Đếm tất cả document dưới users/{uid},
    kể cả các subcollection lồng nhau.

    Không xóa hoặc thay đổi dữ liệu.
    """
    user_ref = client.collection("users").document(uid)

    total = 1 if user_ref.get().exists else 0
    pending = [user_ref]

    while pending:
        parent = pending.pop()

        for collection in parent.collections():
            for snapshot in collection.stream():
                total += 1

                if total > MAX_DOCUMENTS:
                    raise InventoryLimitExceeded(
                        "Tài khoản có quá nhiều document "
                        "để kiểm kê trong một yêu cầu."
                    )

                pending.append(snapshot.reference)

    return total


def count_related_documents(client, collection_name, fields, uid):
    """
    Đếm document liên quan đến UID.

    Dùng ID để loại bỏ kết quả trùng
    khi cùng document khớp nhiều trường.
    """
    matching_ids = set()

    for field in fields:
        query = client.collection(collection_name).where(
            filter=FieldFilter(field, "==", uid)
        )

        for snapshot in query.stream():
            matching_ids.add(snapshot.id)

            if len(matching_ids) > MAX_DOCUMENTS:
                raise InventoryLimitExceeded(
                    "Có quá nhiều document liên quan."
                )

    return len(matching_ids)


def build_account_deletion_inventory(
    uid: str,
    db_session: Session,
) -> AccountDeletionInventory:
    if not uid or "/" in uid:
        raise ValueError("UID không hợp lệ.")

    client = firestore.client(app=get_firebase_app())

    user_count = count_user_documents(client, uid)

    family_link_count = count_related_documents(
        client,
        "familyLinks",
        ("ownerUid", "caregiverUid"),
        uid,
    )

    invitation_count = count_related_documents(
        client,
        "familyInvitations",
        ("ownerUid", "acceptedByUid"),
        uid,
    )

    otp_count = db_session.scalar(
        select(func.count())
        .select_from(PasswordResetChallenge)
        .where(
            PasswordResetChallenge.firebase_uid == uid
        )
    )

    return AccountDeletionInventory(
        user_documents=user_count,
        family_links=family_link_count,
        family_invitations=invitation_count,
        password_reset_records=int(otp_count or 0),
    )
