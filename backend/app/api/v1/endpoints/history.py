from calendar import monthrange
from datetime import date
from typing import Annotated

from fastapi import APIRouter, HTTPException, Query
from google.cloud.firestore_v1.base_query import FieldFilter

from app.api.deps import AuthenticatedUser
from app.repositories.user_scope import user_collection
from app.schemas.medication import DoseResponse


router = APIRouter(tags=["Medication History"])


@router.get(
    "/history/doses",
    response_model=list[DoseResponse],
)
def get_dose_history(
    user: AuthenticatedUser,
    month: Annotated[
        str,
        Query(
            pattern=r"^[0-9]{4}-(0[1-9]|1[0-2])$",
            min_length=7,
            max_length=7,
        ),
    ],
):
    """Đọc liều đã lưu trong tháng, không sinh hoặc sửa liều."""
    try:
        year, month_number = map(int, month.split("-"))

        first = date(year, month_number, 1)
        last = date(
            year,
            month_number,
            monthrange(year, month_number)[1],
        )
    except ValueError as exc:
        raise HTTPException(
            status_code=422,
            detail="Tháng cần có định dạng yyyy-MM hợp lệ.",
        ) from exc

    snapshots = (
        user_collection(user, "doses")
        .where(
            filter=FieldFilter(
                "localDate",
                ">=",
                first.isoformat(),
            )
        )
        .where(
            filter=FieldFilter(
                "localDate",
                "<=",
                last.isoformat(),
            )
        )
        .stream()
    )

    result = []

    for snapshot in snapshots:
        if not snapshot.exists:
            continue

        data = snapshot.to_dict() or {}

        if data.get("status") == "CANCELLED":
            continue

        data.pop("id", None)
        data["doseId"] = snapshot.id

        result.append(
            DoseResponse.model_validate(data)
        )

    return sorted(
        result,
        key=lambda item: (
            item.localDate,
            item.minuteOfDay,
            item.doseId,
        ),
    )