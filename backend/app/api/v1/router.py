from fastapi import APIRouter

from app.api.v1.endpoints import (
    health,
    password_reset,
)


api_router = APIRouter()

# Health check
api_router.include_router(health.router)

# Authentication - Password Reset
api_router.include_router(password_reset.router)
