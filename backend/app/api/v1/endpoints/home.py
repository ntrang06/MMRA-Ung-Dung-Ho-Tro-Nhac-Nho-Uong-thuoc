import logging
from datetime import date, datetime, timezone
from typing import Annotated
from zoneinfo import ZoneInfo

from fastapi import APIRouter, HTTPException, Query, Response
from google.api_core.exceptions import GoogleAPICallError
from pydantic import AwareDatetime, BaseModel, ConfigDict

from app.api.deps import AuthenticatedUser
from app.schemas.medication import DoseResponse, DoseStatus
from app.services.dose_service import DoseService


logger = logging.getLogger(__name__)
router = APIRouter(prefix="/home", tags=["Home"])
HOME_TIME_ZONE = "Asia/Ho_Chi_Minh"


class HomeResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    localDate: date
    timeZone: str = HOME_TIME_ZONE
    serverTime: AwareDatetime
    doses: list[DoseResponse]
    nextDose: DoseResponse | None
    totalDoses: int
    takenDoses: int
    skippedDoses: int
    missedDoses: int
    pendingDoses: int
    progressPercent: int


def build_home_summary(
    rows: list[dict],
    target_date: date,
    now: datetime,
) -> HomeResponse:
    doses = [
        DoseResponse.model_validate(row)
        for row in rows
    ]

    doses = sorted(
        [
            dose
            for dose in doses
            if dose.localDate == target_date
        ],
        key=lambda dose: (
            dose.minuteOfDay,
            dose.doseId,
        ),
    )

    active = [
        dose
        for dose in doses
        if dose.status != DoseStatus.CANCELLED
    ]

    pending = [
        dose
        for dose in active
        if dose.status == DoseStatus.PENDING
    ]

    taken = sum(
        dose.status == DoseStatus.TAKEN
        for dose in active
    )

    total = len(active)

    next_dose = min(
        pending,
        key=lambda dose: (
            dose.nextReminderAt or dose.scheduledAt,
            dose.scheduledAt,
            dose.doseId,
        ),
        default=None,
    )

    return HomeResponse(
        localDate=target_date,
        serverTime=now,

        # Giữ liều CANCELLED để app hủy báo thức cũ.
        doses=doses,

        nextDose=next_dose,
        totalDoses=total,
        takenDoses=taken,
        skippedDoses=sum(
            dose.status == DoseStatus.SKIPPED
            for dose in active
        ),
        missedDoses=sum(
            dose.status == DoseStatus.MISSED
            for dose in active
        ),
        pendingDoses=len(pending),
        progressPercent=(
            taken * 100 // total
            if total
            else 0
        ),
    )


@router.get("", response_model=HomeResponse)
def get_home(
    user: AuthenticatedUser,
    response: Response,
    target_date: Annotated[
        date | None,
        Query(alias="date"),
    ] = None,
):
    response.headers["Cache-Control"] = "no-store"

    selected_date = target_date or datetime.now(
        ZoneInfo(HOME_TIME_ZONE)
    ).date()

    try:
        # Dùng chung cơ chế sinh liều với màn Lịch uống.
        rows = DoseService.get_doses_for_date(
            user,
            selected_date,
        )

        return build_home_summary(
            rows,
            selected_date,
            datetime.now(timezone.utc),
        )

    except (GoogleAPICallError, ValueError):
        logger.exception(
            "Không tổng hợp được dữ liệu Trang chủ"
        )

        raise HTTPException(
            status_code=503,
            detail={
                "code": "home_unavailable",
                "message": (
                    "Chưa tải được Trang chủ. "
                    "Vui lòng thử lại."
                ),
            },
        ) from None