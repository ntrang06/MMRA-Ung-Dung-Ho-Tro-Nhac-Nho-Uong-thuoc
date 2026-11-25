
from functools import lru_cache

from pydantic import Field
from pydantic_settings import (
    BaseSettings,
    SettingsConfigDict,
)
from sqlalchemy.engine import URL


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        case_sensitive=False,
    )

    # Application
    app_name: str = "MMRA Backend"
    app_env: str = "development"
    api_v1_prefix: str = "/api/v1"

    # Local PostgreSQL
    database_url: str | None = None

    # Google Cloud SQL
    cloud_sql_connection_name: str | None = None
    db_user: str | None = None
    db_password: str | None = None
    db_name: str | None = None

    # Firebase
    firebase_project_id: str
    firebase_credentials_path: str | None = None

    # Security
    otp_pepper: str = Field(min_length=32)
    reset_token_pepper: str = Field(min_length=32)

    # OTP settings
    otp_ttl_seconds: int = Field(
        default=300,
        ge=60,
    )
    otp_resend_cooldown_seconds: int = Field(
        default=60,
        ge=30,
    )
    otp_max_attempts: int = Field(
        default=5,
        ge=1,
        le=10,
    )
    reset_token_ttl_seconds: int = Field(
        default=600,
        ge=60,
    )

    # SMTP
    smtp_host: str = "127.0.0.1"
    smtp_port: int = 1025
    smtp_username: str | None = None
    smtp_password: str | None = None
    smtp_from_email: str = "mmra@example.test"
    smtp_starttls: bool = False
    smtp_ssl: bool = False

    # Medicine lookup
    dlpl_api_key: str = ""

    def get_database_url(self) -> str | URL:
        """
        Local:
            Use DATABASE_URL.

        Cloud Run:
            Connect to Cloud SQL through
            the Unix socket mounted by Cloud Run.
        """
        connection_name = (
            self.cloud_sql_connection_name or ""
        ).strip()

        if connection_name:
            parts = connection_name.split(":")

            if (
                len(parts) != 3
                or any(not part for part in parts)
            ):
                raise ValueError(
                    "CLOUD_SQL_CONNECTION_NAME "
                    "không hợp lệ."
                )

            username = (self.db_user or "").strip()
            password = self.db_password or ""
            database = (self.db_name or "").strip()

            if not username or not password or not database:
                raise ValueError(
                    "Cloud SQL cần DB_USER, "
                    "DB_PASSWORD và DB_NAME."
                )

            socket_path = (
                f"/cloudsql/{connection_name}"
            )

            return URL.create(
                drivername="postgresql+psycopg",
                username=username,
                password=password,
                database=database,
                query={
                    "host": socket_path,
                },
            )

        local_url = (self.database_url or "").strip()

        if not local_url:
            raise ValueError(
                "Thiếu DATABASE_URL hoặc "
                "CLOUD_SQL_CONNECTION_NAME."
            )

        return local_url


@lru_cache
def get_settings() -> Settings:
    return Settings()
