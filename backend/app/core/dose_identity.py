from datetime import date
from hashlib import sha256


def make_dose_id(
    schedule_id: str,
    schedule_version: int,
    local_date: date,
    minute_of_day: int,
) -> str:
    """
    Cùng lịch, phiên bản, ngày và giờ
    luôn tạo ra cùng một ID liều.
    """

    if (
        not schedule_id
        or "/" in schedule_id
        or "|" in schedule_id
    ):
        raise ValueError(
            "scheduleId không hợp lệ."
        )

    if (
        type(schedule_version) is not int
        or schedule_version < 1
    ):
        raise ValueError(
            "scheduleVersion phải là số nguyên dương."
        )

    if (
        type(minute_of_day) is not int
        or not 0 <= minute_of_day <= 1439
    ):
        raise ValueError(
            "Giờ uống phải từ 0 đến 1439 phút."
        )

    source = (
        f"{schedule_id}|"
        f"{schedule_version}|"
        f"{local_date.isoformat()}|"
        f"{minute_of_day}"
    )

    return sha256(
        source.encode("utf-8")
    ).hexdigest()