
from unittest.mock import Mock

import pytest

from app.services import (
    account_deletion_verification as verification,
)
from app.services.account_deletion_inventory import (
    AccountDeletionInventory,
)


UID = "user-001"


def make_inventory(
    user_documents=0,
    family_links=0,
    family_invitations=0,
    password_reset_records=0,
):
    return AccountDeletionInventory(
        user_documents=user_documents,
        family_links=family_links,
        family_invitations=family_invitations,
        password_reset_records=password_reset_records,
    )


def mock_inventory(monkeypatch, inventory):
    mocked = Mock(return_value=inventory)

    monkeypatch.setattr(
        verification,
        "build_account_deletion_inventory",
        mocked,
    )

    return mocked


def test_all_data_removed(monkeypatch):
    mocked = mock_inventory(
        monkeypatch,
        make_inventory(),
    )

    db = Mock()

    result = verification.verify_account_data_removed(
        UID,
        db,
    )

    assert result.uid == UID
    assert result.verified is True
    assert result.user_documents == 0
    assert result.family_links == 0
    assert result.family_invitations == 0
    assert result.password_reset_records == 0

    mocked.assert_called_once_with(UID, db)


@pytest.mark.parametrize(
    "field",
    [
        "user_documents",
        "family_links",
        "family_invitations",
        "password_reset_records",
    ],
)
def test_remaining_data_blocks_verification(
    monkeypatch,
    field,
):
    counts = {
        "user_documents": 0,
        "family_links": 0,
        "family_invitations": 0,
        "password_reset_records": 0,
    }

    counts[field] = 1

    mock_inventory(
        monkeypatch,
        make_inventory(**counts),
    )

    with pytest.raises(
        verification.DeletionDataRemaining
    ):
        verification.verify_account_data_removed(
            UID,
            Mock(),
        )


def test_multiple_data_types_remaining(
    monkeypatch,
):
    mock_inventory(
        monkeypatch,
        make_inventory(
            user_documents=3,
            family_links=2,
            family_invitations=1,
            password_reset_records=4,
        ),
    )

    with pytest.raises(
        verification.DeletionDataRemaining
    ):
        verification.verify_account_data_removed(
            UID,
            Mock(),
        )


def test_inventory_error_fails_closed(
    monkeypatch,
):
    def unavailable(uid, db):
        raise RuntimeError(
            "Firestore unavailable"
        )

    monkeypatch.setattr(
        verification,
        "build_account_deletion_inventory",
        unavailable,
    )

    with pytest.raises(
        verification.DeletionVerificationError
    ):
        verification.verify_account_data_removed(
            UID,
            Mock(),
        )


def test_invalid_uid_rejected(monkeypatch):
    mocked = mock_inventory(
        monkeypatch,
        make_inventory(),
    )

    with pytest.raises(
        verification.DeletionVerificationError
    ):
        verification.verify_account_data_removed(
            "../other-user",
            Mock(),
        )

    mocked.assert_not_called()


@pytest.mark.parametrize(
    "invalid_count",
    [
        -1,
        None,
        "0",
        0.0,
        True,
    ],
)
def test_invalid_count_rejected(
    monkeypatch,
    invalid_count,
):
    mock_inventory(
        monkeypatch,
        make_inventory(
            user_documents=invalid_count,
        ),
    )

    with pytest.raises(
        verification.DeletionVerificationError
    ):
        verification.verify_account_data_removed(
            UID,
            Mock(),
        )


def test_verification_is_read_only(
    monkeypatch,
):
    mock_inventory(
        monkeypatch,
        make_inventory(),
    )

    db = Mock()

    result = verification.verify_account_data_removed(
        UID,
        db,
    )

    assert result.verified is True

    db.add.assert_not_called()
    db.delete.assert_not_called()
    db.execute.assert_not_called()
    db.commit.assert_not_called()
    db.rollback.assert_not_called()
