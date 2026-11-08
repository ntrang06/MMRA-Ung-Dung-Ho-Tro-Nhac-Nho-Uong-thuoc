import re

from pydantic import (
    BaseModel,
    EmailStr,
    Field,
    SecretStr,
    field_serializer,
    field_validator,
)


class PasswordResetRequest(BaseModel):
    """Dữ liệu yêu cầu gửi OTP."""

    email: EmailStr


class PasswordResetVerifyRequest(BaseModel):
    """Dữ liệu xác minh OTP gồm 6 chữ số."""

    email: EmailStr
    otp_code: SecretStr

    @field_validator("otp_code")
    @classmethod
    def validate_otp(cls, value: SecretStr) -> SecretStr:
        code = value.get_secret_value()

        if not re.fullmatch(r"[0-9]{6}", code):
            raise ValueError("Mã OTP phải gồm đúng 6 chữ số.")

        return value


class PasswordResetCompleteRequest(BaseModel):
    """Dữ liệu đặt mật khẩu mới sau khi xác minh OTP."""

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
            raise ValueError("Vui lòng nhập mật khẩu mới.")

        return value


class PasswordResetRequestResponse(BaseModel):
    """Kết quả xử lý yêu cầu gửi OTP."""

    message: str
    expires_in_seconds: int
    resend_after_seconds: int


class PasswordResetVerifyResponse(BaseModel):
    """Kết quả xác minh OTP thành công."""

    reset_token: SecretStr
    expires_in_seconds: int

    @field_serializer("reset_token", when_used="json")
    def serialize_reset_token(self, value: SecretStr) -> str:
        # App cần token thật để gọi API đặt mật khẩu mới.
        return value.get_secret_value()


class PasswordResetCompleteResponse(BaseModel):
    """Kết quả đặt mật khẩu mới."""

    message: str