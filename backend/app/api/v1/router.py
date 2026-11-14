from fastapi import APIRouter

from app.api.v1.endpoints import (
    doses,
    health,
    history,
    password_reset,
    schedules,
    session,
)


api_router = APIRouter()

api_router.include_router(health.router)
api_router.include_router(password_reset.router)
api_router.include_router(session.router)
api_router.include_router(schedules.router)
api_router.include_router(doses.router)
api_router.include_router(history.router)