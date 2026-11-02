
import hashlib
import hmac
import secrets


def generate_otp() -> str:
    """Generate a cryptographically secure 6-digit OTP."""
    return f"{secrets.randbelow(1_000_000):06d}"


def generate_reset_token() -> str:
    """Generate a high-entropy, single-use reset token."""
    return secrets.token_urlsafe(32)


def hash_secret(value: str, pepper: str) -> str:
    """Hash a secret using HMAC-SHA256 and a server-side pepper."""
    return hmac.new(
        pepper.encode("utf-8"),
        value.encode("utf-8"),
        hashlib.sha256,
    ).hexdigest()


def verify_secret(
    value: str,
    expected_hash: str,
    pepper: str,
) -> bool:
    """Compare secret hashes safely."""
    actual_hash = hash_secret(value, pepper)

    return hmac.compare_digest(
        actual_hash,
        expected_hash,
    )
