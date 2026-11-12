
from fastapi import FastAPI

from app.api.v1.router import api_router
from app.core.config import get_settings


settings = get_settings()

app = FastAPI(
    title=settings.app_name,
    description="Backend API for Medication Management and Reminder Assistant",
    version="0.1.0",
)

app.include_router(
    api_router,
    prefix=settings.api_v1_prefix,
)


@app.get("/", include_in_schema=False)
def root():
    return {
        "message": "Welcome to MMRA Backend",
        "status": "running",
    }
