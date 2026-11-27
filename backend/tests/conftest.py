
from unittest.mock import Mock

import pytest

from app.api import write_guard


@pytest.fixture(autouse=True)
def mock_account_deletion_status(monkeypatch):
    """
    Gia lap trang thai tai khoan mac dinh
    cho cac unit test Backend.

    Mac dinh:
    - Chua co yeu cau xoa tai khoan
    - Duoc phep ghi du lieu

    Chi ap dung trong pytest.
    Khong thay doi FastAPI Production.

    Cac test Write Guard co the monkeypatch
    lai get_deletion_status_reference de kiem
    tra pending, processing, completed.
    """
    reference = Mock()

    snapshot = Mock()
    snapshot.exists = False
    snapshot.to_dict.return_value = {}

    reference.get.return_value = snapshot

    monkeypatch.setattr(
        write_guard,
        "get_deletion_status_reference",
        lambda uid: reference,
    )
