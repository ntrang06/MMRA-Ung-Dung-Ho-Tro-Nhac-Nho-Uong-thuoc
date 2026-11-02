import re

from pydantic import (
    BaseModel,
    EmailStr,
    Field,
    SecretStr,
    field_validator,
)


class PasswordResetRequest(BaseModel):
    """Request an OTP for password reset."""

    email: EmailStr


class PasswordResetVerifyRequest(BaseModel):
    """Verify a six-digit OTP."""

    email: EmailStr
    otp_code: SecretStr

    @field_validator("otp_code")
    @classmethod
    def validate_otp(cls, value: SecretStr) -> SecretStr:
        code = value.get_secret_value()

        if not re.fullmatch(r"\d{6}", code):
            raise ValueError("OTP must contain exactly six digits.")

        return value


class PasswordResetCompleteRequest(BaseModel):
    """Complete password reset using a verified reset token."""

    reset_token: SecretStr = Field(
        min_length=32,
        max_length=256,
    )

    new_password: SecretStr = Field(
        min_length=8,
        max_length=128,
    )

    @field_validator("new_password")
    @classmethod
    def validate_new_password(
        cls,
        value: SecretStr,
    ) -> SecretStr:
        password = value.get_secret_value()

        if not password.strip():
            raise ValueError("Password cannot be empty.")

        return value


class PasswordResetRequestResponse(BaseModel):
    """Generic response for password reset requests."""

    message: str
    expires_in_seconds: int
    resend_after_seconds: int


class PasswordResetVerifyResponse(BaseModel):
    """Response after successful OTP verification."""

    reset_token: SecretStr
    expires_in_seconds: int


class PasswordResetCompleteResponse(BaseModel):
    """Response after successful password update."""

    message: str
