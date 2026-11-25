
"""Các hàm hỗ trợ chống tạo thuốc trùng khi gửi lại yêu cầu."""

import hashlib
import json
import re
from collections.abc import Mapping


_KEY_PATTERN = re.compile(
    r"[A-Za-z0-9][A-Za-z0-9._-]{15,127}\Z"
)


def validate_idempotency_key(key: str) -> str:
    """
    Kiểm tra khóa gửi từ Android.

    Cho phép UUID, chữ, số, dấu chấm,
    dấu gạch dưới và dấu gạch ngang.
    """
    if (
        not isinstance(key, str)
        or _KEY_PATTERN.fullmatch(key) is None
    ):
        raise ValueError(
            "Idempotency-Key phải dài 16–128 ký tự "
            "và chỉ chứa chữ, số, dấu chấm, "
            "gạch dưới hoặc gạch ngang."
        )

    return key


def medicine_request_document_id(
    uid: str,
    key: str,
) -> str:
    """
    Tạo mã cố định cho một yêu cầu thêm thuốc.

    Cùng UID và cùng key -> cùng mã.
    Khác UID hoặc khác key -> mã khác.
    """
    if not uid:
        raise ValueError("Thiếu UID tài khoản.")

    valid_key = validate_idempotency_key(key)

    content = f"{uid}\x00{valid_key}".encode("utf-8")

    return hashlib.sha256(
        content
    ).hexdigest()


def medicine_payload_digest(
    payload: Mapping[str, object],
) -> str:
    """
    Tạo dấu vân tay cho nội dung thuốc.

    Thứ tự trường khác nhau không làm
    thay đổi digest.

    Nội dung thuốc khác nhau sẽ tạo
    digest khác nhau.
    """
    normalized = json.dumps(
        dict(payload),
        sort_keys=True,
        ensure_ascii=False,
        separators=(",", ":"),
        allow_nan=False,
    )

    return hashlib.sha256(
        normalized.encode("utf-8")
    ).hexdigest()
