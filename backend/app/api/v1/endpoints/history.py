from calendar import monthrange
from datetime import date
from typing import Annotated

from fastapi import APIRouter, HTTPException, Query
from google.cloud.firestore_v1.base_query import FieldFilter

from app.api.deps import AuthenticatedUser
from app.repositories.user_scope import user_collection
from app.schemas.medication import DoseResponse, DoseStatus


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
    target_date: Annotated[
        date | None,
        Query(alias="date"),
    ] = None,
    status: Annotated[
        DoseStatus | None,
        Query(),
    ] = None,
):
    """
    Đọc lịch sử Dose trong tháng.

    Hỗ trợ:
    - Lọc theo tháng.
    - Lọc theo ngày cụ thể trong tháng.
    - Lọc theo trạng thái Dose.

    Không sinh hoặc sửa Dose.
    """
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

    # Ngày được lọc phải thuộc tháng đang truy vấn.
    if target_date is not None:
        if target_date < first or target_date > last:
            raise HTTPException(
                status_code=422,
                detail="Ngày lọc phải thuộc tháng được yêu cầu.",
            )

    query = user_collection(
        user,
        "doses",
    )

    if target_date is not None:
        query = query.where(
            filter=FieldFilter(
                "localDate",
                "==",
                target_date.isoformat(),
            )
        )
    else:
        query = (
            query.where(
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
        )

    if status is not None:
        query = query.where(
            filter=FieldFilter(
                "status",
                "==",
                status.value,
            )
        )

    snapshots = query.stream()

    result = []

    for snapshot in snapshots:
        if not snapshot.exists:
            continue

        data = snapshot.to_dict() or {}

        # Không hiển thị các Dose đã bị hủy trong lịch sử.
        if data.get("status") == DoseStatus.CANCELLED.value:
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
