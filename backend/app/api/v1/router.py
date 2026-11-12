from fastapi import APIRouter

from app.api.v1.endpoints import (
    health,
    password_reset,
    session,
    doses,
)


api_router = APIRouter()

# Kiểm tra backend hoạt động.
api_router.include_router(health.router)

# Đặt lại mật khẩu bằng OTP.
api_router.include_router(password_reset.router)

# Kiểm tra phiên Firebase.
api_router.include_router(session.router)
api_router.include_router(doses.router)