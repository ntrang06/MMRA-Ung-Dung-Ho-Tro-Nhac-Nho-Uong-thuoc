
from unittest.mock import Mock

import pytest

from app.services import account_deletion_cleanup as cleanup


# ============================================================
# 1. UID VALIDATION
# ============================================================

@pytest.mark.parametrize(
    "uid",
    [
        "",
        "/",
        "../other",
        ".",
        "..",
    ],
)
def test_invalid_uid_rejected(uid):
    with pytest.raises(ValueError):
        cleanup.validate_uid(uid)


def test_valid_uid():
    assert cleanup.validate_uid(
        "user-001"
    ) == "user-001"


# ============================================================
# 2. FIRESTORE TREE
# ============================================================

def test_delete_document_tree_children_first():
    operations = []

    child = Mock()
    child.get.return_value.exists = True
    child.collections.return_value = []
    child.delete.side_effect = lambda: (
        operations.append("child")
    )

    child_snapshot = Mock()
    child_snapshot.reference = child

    subcollection = Mock()
    subcollection.stream.return_value = [
        child_snapshot
    ]

    parent = Mock()
    parent.get.return_value.exists = True
    parent.collections.return_value = [
        subcollection
    ]
    parent.delete.side_effect = lambda: (
        operations.append("parent")
    )

    deleted = cleanup.delete_document_tree(
        parent
    )

    assert deleted == 2
    assert operations == [
        "child",
        "parent",
    ]


def test_delete_missing_document():
    reference = Mock()
    reference.get.return_value.exists = False
    reference.collections.return_value = []

    deleted = cleanup.delete_document_tree(
        reference
    )

    assert deleted == 0
    reference.delete.assert_not_called()


def test_deep_tree_rejected():
    reference = Mock()

    with pytest.raises(ValueError):
        cleanup.delete_document_tree(
            reference,
            depth=cleanup.MAX_FIRESTORE_DEPTH,
        )

    reference.delete.assert_not_called()


def test_user_scope():
    client = Mock()
    reference = (
        client.collection.return_value
        .document.return_value
    )

    reference.collections.return_value = []
    reference.get.return_value.exists = False

    cleanup.delete_user_documents(
        "user-001",
        client,
    )

    client.collection.assert_called_with(
        "users"
    )

    client.collection.return_value.document.assert_called_with(
        "user-001"
    )


# ============================================================
# 3. FAMILY
# ============================================================

def test_family_link_fields(monkeypatch):
    calls = []

    def fake_delete(
        uid,
        client,
        collection_name,
        fields,
    ):
        calls.append(
            (uid, collection_name, fields)
        )
        return 2

    monkeypatch.setattr(
        cleanup,
        "delete_related_documents",
        fake_delete,
    )

    deleted = cleanup.delete_family_links(
        "user-001",
        Mock(),
    )

    assert deleted == 2

    assert calls == [
        (
            "user-001",
            "familyLinks",
            ("ownerUid", "caregiverUid"),
        )
    ]


def test_family_invitations_fields(
    monkeypatch,
):
    calls = []

    def fake_delete(
        uid,
        client,
        collection_name,
        fields,
    ):
        calls.append(
            (collection_name, fields)
        )
        return 1

    monkeypatch.setattr(
        cleanup,
        "delete_related_documents",
        fake_delete,
    )

    assert cleanup.delete_family_invitations(
        "user-001",
        Mock(),
    ) == 1

    assert calls == [
        (
            "familyInvitations",
            ("ownerUid", "acceptedByUid"),
        )
    ]


def test_duplicate_family_reference_deleted_once():
    reference = Mock()
    reference.path = "familyLinks/link-001"

    snapshot = Mock()
    snapshot.reference = reference

    query = Mock()
    query.stream.return_value = [
        snapshot
    ]

    collection = Mock()
    collection.where.return_value = query

    client = Mock()
    client.collection.return_value = collection

    count = cleanup.delete_related_documents(
        "user-001",
        client,
        "familyLinks",
        ("ownerUid", "caregiverUid"),
    )

    assert count == 1
    reference.delete.assert_called_once()


# ============================================================
# 4. POSTGRESQL
# ============================================================

def test_delete_otp_commits():
    db = Mock()
    db.execute.return_value.rowcount = 3

    deleted = cleanup.delete_password_reset_records(
        "user-001",
        db,
    )

    assert deleted == 3
    db.commit.assert_called_once()
    db.rollback.assert_not_called()


def test_delete_otp_rolls_back_on_error():
    db = Mock()
    db.execute.side_effect = RuntimeError(
        "Database unavailable"
    )

    with pytest.raises(RuntimeError):
        cleanup.delete_password_reset_records(
            "user-001",
            db,
        )

    db.rollback.assert_called_once()
    db.commit.assert_not_called()


# ============================================================
# 5. FIREBASE AUTH
# ============================================================

def test_delete_firebase_auth(monkeypatch):
    delete_mock = Mock()

    monkeypatch.setattr(
        cleanup.auth,
        "delete_user",
        delete_mock,
    )

    firebase_app = object()

    cleanup.delete_firebase_auth_user(
        "user-001",
        firebase_app,
    )

    delete_mock.assert_called_once_with(
        "user-001",
        app=firebase_app,
    )
