
import smtplib
import ssl
from email.message import EmailMessage

from app.core.config import get_settings


class EmailDeliveryError(Exception):
    """Raised when an email cannot be delivered."""


def send_password_reset_otp(
    to_email: str,
    otp_code: str,
    expires_in_minutes: int = 5,
) -> None:
    """
    Send a password reset OTP email.

    This implementation supports local Mailpit and SMTP providers.
    The OTP is never written to application logs.
    """
    settings = get_settings()

    if not to_email.strip():
        raise ValueError("Recipient email is required.")

    if (
        not isinstance(otp_code, str)
        or len(otp_code) != 6
        or not otp_code.isdigit()
    ):
        raise ValueError("OTP must contain exactly six digits.")

    if settings.smtp_ssl and settings.smtp_starttls:
        raise ValueError(
            "SMTP SSL and STARTTLS cannot be enabled together."
        )

    message = EmailMessage()

    message["Subject"] = "Mã xác minh đặt lại mật khẩu MMRA"
    message["From"] = settings.smtp_from_email
    message["To"] = to_email.strip()

    message.set_content(
        f"""
Xin chào,

Bạn vừa yêu cầu đặt lại mật khẩu tài khoản MMRA.

Mã xác minh của bạn là:

{otp_code}

Mã có hiệu lực trong {expires_in_minutes} phút.

Vui lòng không chia sẻ mã xác minh này với bất kỳ ai.

Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.

Trân trọng,
MMRA Team
        """.strip()
    )

    smtp_client = (
        smtplib.SMTP_SSL
        if settings.smtp_ssl
        else smtplib.SMTP
    )

    try:
        with smtp_client(
            settings.smtp_host,
            settings.smtp_port,
            timeout=10,
        ) as server:

            server.ehlo()

            if settings.smtp_starttls:
                server.starttls(
                    context=ssl.create_default_context()
                )
                server.ehlo()

            if settings.smtp_username or settings.smtp_password:
                if not (
                    settings.smtp_username
                    and settings.smtp_password
                ):
                    raise EmailDeliveryError(
                        "SMTP username and password must both be configured."
                    )

                server.login(
                    settings.smtp_username,
                    settings.smtp_password,
                )

            server.send_message(message)

    except (OSError, smtplib.SMTPException) as exc:
        raise EmailDeliveryError(
            "Unable to send password reset email."
        ) from exc
