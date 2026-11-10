from datetime import datetime
from uuid import UUID as PythonUUID, uuid4

from sqlalchemy import (
    CheckConstraint,
    DateTime,
    Index,
    Integer,
    String,
    func,
    text,
)
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class PasswordResetChallenge(Base):
    __tablename__ = "password_reset_challenges"

    __table_args__ = (
        CheckConstraint(
            "status IN ('pending', 'verified', 'consumed', 'locked')",
            name="valid_status",
        ),
        CheckConstraint(
            "attempt_count >= 0",
            name="non_negative_attempt_count",
        ),
        Index(
            "ix_password_reset_challenges_email_created_at",
            "email",
            "created_at",
        ),
        Index(
            "ix_password_reset_challenges_firebase_uid",
            "firebase_uid",
        ),
    )

    id: Mapped[PythonUUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid4,
    )

    firebase_uid: Mapped[str] = mapped_column(
        String(128),
        nullable=False,
    )

    email: Mapped[str] = mapped_column(
        String(320),
        nullable=False,
    )

    # HMAC-SHA256 hash, never store plain OTP.
    otp_hash: Mapped[str] = mapped_column(
        String(64),
        nullable=False,
    )

    otp_expires_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
    )

    attempt_count: Mapped[int] = mapped_column(
        Integer,
        nullable=False,
        default=0,
        server_default=text("0"),
    )

    status: Mapped[str] = mapped_column(
        String(20),
        nullable=False,
        default="pending",
        server_default=text("'pending'"),
    )

    verified_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True),
        nullable=True,
    )

    # Only populated after successful OTP verification.
    reset_token_hash: Mapped[str | None] = mapped_column(
        String(64),
        nullable=True,
    )

    reset_token_expires_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True),
        nullable=True,
    )

    consumed_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True),
        nullable=True,
    )

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
    )

    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
        onupdate=func.now(),
    )
