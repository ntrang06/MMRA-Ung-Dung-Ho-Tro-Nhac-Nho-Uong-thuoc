
from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        case_sensitive=False,
    )

    app_name: str = "MMRA Backend"
    app_env: str = "development"
    api_v1_prefix: str = "/api/v1"

    # Database
    database_url: str

    # Firebase
    firebase_project_id: str
    firebase_credentials_path: str | None = None

    # Security secrets
    otp_pepper: str = Field(min_length=32)
    reset_token_pepper: str = Field(min_length=32)

    # OTP policy
    otp_ttl_seconds: int = Field(default=300, ge=60)
    otp_resend_cooldown_seconds: int = Field(default=60, ge=30)
    otp_max_attempts: int = Field(default=5, ge=1, le=10)
    reset_token_ttl_seconds: int = Field(default=600, ge=60)

    # SMTP
    smtp_host: str = "127.0.0.1"
    smtp_port: int = 1025
    smtp_username: str | None = None
    smtp_password: str | None = None
    smtp_from_email: str = "mmra@example.test"
    smtp_starttls: bool = False
    smtp_ssl: bool = False


@lru_cache
def get_settings() -> Settings:
    return Settings()
