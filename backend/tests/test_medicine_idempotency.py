
import pytest

from app.services.medicine_idempotency import (
    medicine_payload_digest,
    medicine_request_document_id,
    validate_idempotency_key,
)


def test_valid_uuid_key():
    key = "d909fd35-40e9-4b64-bada-f32d6cfece87"

    assert validate_idempotency_key(key) == key


def test_request_id_is_stable_for_retry():
    key = "d909fd35-40e9-4b64-bada-f32d6cfece87"

    first = medicine_request_document_id(
        "user-A",
        key,
    )

    second = medicine_request_document_id(
        "user-A",
        key,
    )

    assert first == second


def test_request_id_is_different_for_other_user():
    key = "d909fd35-40e9-4b64-bada-f32d6cfece87"

    first = medicine_request_document_id(
        "user-A",
        key,
    )

    second = medicine_request_document_id(
        "user-B",
        key,
    )

    assert first != second


def test_request_id_is_different_for_other_key():
    first = medicine_request_document_id(
        "user-A",
        "aaaaaaaaaaaaaaaa",
    )

    second = medicine_request_document_id(
        "user-A",
        "bbbbbbbbbbbbbbbb",
    )

    assert first != second


@pytest.mark.parametrize(
    "key",
    [
        "",
        "short",
        "has whitespace 123",
        "../path-traversal-test",
        "x" * 129,
        "中文abcdef1234567890",
    ],
)
def test_invalid_key_is_rejected(key):
    with pytest.raises(ValueError):
        validate_idempotency_key(key)


def test_payload_digest_ignores_field_order():
    first = {
        "medicineName": "Metformin",
        "strength": "500",
    }

    second = {
        "strength": "500",
        "medicineName": "Metformin",
    }

    assert (
        medicine_payload_digest(first)
        == medicine_payload_digest(second)
    )


def test_payload_digest_changes_when_content_changes():
    first = {
        "medicineName": "Metformin",
        "strength": "500",
    }

    second = {
        "medicineName": "Metformin",
        "strength": "850",
    }

    assert (
        medicine_payload_digest(first)
        != medicine_payload_digest(second)
    )


def test_digest_supports_vietnamese():
    result = medicine_payload_digest(
        {
            "medicineName": "Thuốc đau đầu",
        }
    )

    assert len(result) == 64
