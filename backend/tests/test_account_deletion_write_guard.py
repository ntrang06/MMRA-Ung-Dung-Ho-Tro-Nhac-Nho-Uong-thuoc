
from typing import get_type_hints
from unittest.mock import Mock

import pytest
from fastapi import HTTPException

from app.api.deps import CurrentUser
from app.api import write_guard


# ============================================================
# 1. MOCK HELPERS
# ============================================================

def make_snapshot(
    exists=True,
    status="pending",
    uid="user-001",
):
    snapshot = Mock()
    snapshot.exists = exists

    snapshot.to_dict.return_value = {
        "uid": uid,
        "status": status,
    }

    return snapshot


def setup_state(monkeypatch, snapshot):
    reference = Mock()
    reference.get.return_value = snapshot

    monkeypatch.setattr(
        write_guard,
        "get_deletion_status_reference",
        lambda uid: reference,
    )

    return reference


# ============================================================
# 2. NO DELETION REQUEST
# ============================================================

def test_no_request_allows_write(monkeypatch):
    setup_state(
        monkeypatch,
        make_snapshot(exists=False),
    )

    user = write_guard.get_writable_user(
        CurrentUser(uid="user-001")
    )

    assert user.uid == "user-001"


# ============================================================
# 3. PENDING - ALLOW WRITES
# ============================================================

def test_pending_allows_write(monkeypatch):
    setup_state(
        monkeypatch,
        make_snapshot(status="pending"),
    )

    user = write_guard.get_writable_user(
        CurrentUser(uid="user-001")
    )

    assert user.uid == "user-001"


# ============================================================
# 4. DELETION STATES - BLOCK WRITES
# ============================================================

@pytest.mark.parametrize(
    "state",
    [
        "confirmed",
        "processing",
        "failed",
        "completed",
    ],
)
def test_deleting_account_blocks_write(
    monkeypatch,
    state,
):
    setup_state(
        monkeypatch,
        make_snapshot(status=state),
    )

    with pytest.raises(HTTPException) as error:
        write_guard.get_writable_user(
            CurrentUser(uid="user-001")
        )

    assert error.value.status_code == 409

    assert (
        error.value.detail["code"]
        == "account_deletion_in_progress"
    )


# ============================================================
# 5. INVALID ACCOUNT DATA
# ============================================================

def test_wrong_uid_fails_closed(monkeypatch):
    setup_state(
        monkeypatch,
        make_snapshot(uid="another-user"),
    )

    with pytest.raises(HTTPException) as error:
        write_guard.get_writable_user(
            CurrentUser(uid="user-001")
        )

    assert error.value.status_code == 503

    assert (
        error.value.detail["code"]
        == "account_state_unavailable"
    )


def test_invalid_state_fails_closed(monkeypatch):
    setup_state(
        monkeypatch,
        make_snapshot(status="unknown"),
    )

    with pytest.raises(HTTPException) as error:
        write_guard.get_writable_user(
            CurrentUser(uid="user-001")
        )

    assert error.value.status_code == 503

    assert (
        error.value.detail["code"]
        == "account_state_unavailable"
    )


# ============================================================
# 6. FIRESTORE ERROR
# ============================================================

def test_firestore_failure_fails_closed(
    monkeypatch,
):
    def unavailable(uid):
        raise RuntimeError(
            "Firestore unavailable"
        )

    monkeypatch.setattr(
        write_guard,
        "get_deletion_status_reference",
        unavailable,
    )

    with pytest.raises(HTTPException) as error:
        write_guard.get_writable_user(
            CurrentUser(uid="user-001")
        )

    assert error.value.status_code == 503

    assert (
        error.value.detail["code"]
        == "account_state_unavailable"
    )


# ============================================================
# 7. ENDPOINT REGISTRATION
# ============================================================

def test_all_target_endpoints_use_write_guard():
    from app.api.v1.endpoints import (
        doses,
        medicines,
        schedules,
    )

    targets = {
        medicines: (
            "create_medicine",
            "update_medicine",
            "delete_medicine",
        ),
        schedules: (
            "create_schedule",
            "update_schedule",
            "delete_schedule",
        ),
        doses: (
            "update_dose_action",
            "snooze_dose",
        ),
    }

    for module, functions in targets.items():
        for name in functions:
            handler = getattr(
                module,
                name,
            )

            annotations = get_type_hints(
                handler,
                include_extras=True,
            )

            assert (
                annotations["user"]
                == write_guard.WritableUser
            ), (
                f"Missing write guard: "
                f"{module.__name__}.{name}"
            )
