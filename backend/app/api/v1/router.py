
from fastapi import APIRouter

from app.api.v1.endpoints import (
    account_deletion,
    admin,
    doses,
    family,
    family_shared,
    health,
    history,
    home,
    medicine_lookup,
    medicines,
    password_reset,
    schedules,
    session,
)

api_router = APIRouter()

api_router.include_router(health.router)
api_router.include_router(password_reset.router)
api_router.include_router(session.router)

# Account deletion requests
api_router.include_router(account_deletion.router)

api_router.include_router(schedules.router)
api_router.include_router(doses.router)
api_router.include_router(history.router)
api_router.include_router(home.router)
api_router.include_router(medicines.router)
api_router.include_router(medicine_lookup.router)

# Family
api_router.include_router(family.router)
api_router.include_router(family_shared.router)

# Admin
api_router.include_router(admin.router)
