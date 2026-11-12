from fastapi import APIRouter, Response
from pydantic import BaseModel

from app.api.deps import AuthenticatedUser


router = APIRouter(
    prefix="/auth",
    tags=["Session"],
)


class SessionResponse(BaseModel):
    uid: str


@router.get(
    "/me",
    response_model=SessionResponse,
)
def get_session(
    user: AuthenticatedUser,
    response: Response,
) -> SessionResponse:
    response.headers["Cache-Control"] = "no-store"

    return SessionResponse(
        uid=user.uid,
    )