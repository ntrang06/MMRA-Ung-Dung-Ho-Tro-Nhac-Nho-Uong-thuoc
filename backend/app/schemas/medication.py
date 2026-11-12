import re
from datetime import date
from enum import Enum
from typing import Annotated, Literal

from pydantic import (
    AwareDatetime,
    BaseModel,
    ConfigDict,
    Field,
    field_validator,
    model_validator,
)


Minute = Annotated[
    int,
    Field(strict=True, ge=0, le=1439),
]

Weekday = Annotated[
    int,
    Field(strict=True, ge=1, le=7),
]

DocumentId = Annotated[
    str,
    Field(
        min_length=1,
        max_length=128,
        pattern=r"^[^/|]+$",
    ),
]


class ContractModel(BaseModel):
    # Chặn các trường ngoài cấu trúc,
    # ví dụ client tự gửi uid hoặc createdAt.
    model_config = ConfigDict(extra="forbid")


class DoseStatus(str, Enum):
    PENDING = "PENDING"
    TAKEN = "TAKEN"
    SKIPPED = "SKIPPED"
    MISSED = "MISSED"
    CANCELLED = "CANCELLED"


class DoseUnit(str, Enum):
    TABLET = "TABLET"
    SACHET = "SACHET"
    ML = "ML"
    DROP = "DROP"
    DOSE = "DOSE"


class ScheduleInput(ContractModel):
    medicineId: DocumentId

    # Số lượng mỗi lần uống, ví dụ "1" hoặc "0.5".
    doseQuantity: str
    doseUnit: DoseUnit

    times: list[Minute] = Field(
        min_length=1,
        max_length=24,
    )

    weekdays: list[Weekday] = Field(
        min_length=1,
        max_length=7,
    )

    startDate: date
    endDate: date | None = None

    timeZone: Literal["Asia/Ho_Chi_Minh"] = (
        "Asia/Ho_Chi_Minh"
    )

    mealInstruction: str = Field(
        default="",
        max_length=500,
    )

    # Số phút nhắc trước giờ uống; 0 là đúng giờ.
    reminderMinutes: int = Field(
        default=0,
        strict=True,
        ge=0,
        le=1440,
    )

    enabled: bool = Field(
        default=True,
        strict=True,
    )

    @field_validator("doseQuantity")
    @classmethod
    def validate_quantity(cls, value: str) -> str:
        pattern = (
            r"(?:0|[1-9][0-9]{0,5})"
            r"(?:\.[0-9]{1,3})?"
        )

        if not re.fullmatch(pattern, value):
            raise ValueError(
                "Liều dùng phải là chuỗi số thập phân, "
                "tối đa 3 chữ số lẻ."
            )

        if not any(
            character in "123456789"
            for character in value
        ):
            raise ValueError(
                "Liều dùng phải lớn hơn 0."
            )

        return value

    @field_validator("times", "weekdays")
    @classmethod
    def validate_unique(
        cls,
        value: list[int],
    ) -> list[int]:
        if len(value) != len(set(value)):
            raise ValueError(
                "Danh sách không được có giá trị trùng."
            )

        return sorted(value)

    @model_validator(mode="after")
    def validate_period(self):
        if (
            self.endDate is not None
            and self.endDate < self.startDate
        ):
            raise ValueError(
                "Ngày kết thúc phải từ ngày bắt đầu trở đi."
            )

        return self


class ScheduleUpdate(ScheduleInput):
    # Phiên bản app đã tải trước khi sửa.
    expectedVersion: int = Field(
        strict=True,
        ge=1,
    )


class ScheduleResponse(ScheduleInput):
    scheduleId: DocumentId

    version: int = Field(
        strict=True,
        ge=1,
    )

    createdAt: AwareDatetime
    updatedAt: AwareDatetime


class MedicineSnapshot(ContractModel):
    # Giữ thông tin thuốc tại lúc sinh liều
    # để phục vụ lịch sử.
    medicineName: str
    strength: str
    strengthUnit: str
    dosageForm: str


class DoseResponse(ContractModel):
    doseId: str = Field(
        pattern=r"^[0-9a-f]{64}$"
    )

    scheduleId: DocumentId

    scheduleVersion: int = Field(
        strict=True,
        ge=1,
    )

    medicineId: DocumentId

    localDate: date
    minuteOfDay: Minute

    timeZone: Literal["Asia/Ho_Chi_Minh"] = (
        "Asia/Ho_Chi_Minh"
    )

    scheduledAt: AwareDatetime

    doseQuantity: str
    doseUnit: DoseUnit
    mealInstruction: str

    medicineSnapshot: MedicineSnapshot

    status: DoseStatus

    actionAt: AwareDatetime | None = None
    nextReminderAt: AwareDatetime | None = None

    createdAt: AwareDatetime
    updatedAt: AwareDatetime


class DoseActionRequest(ContractModel):
    action: Literal["TAKEN", "SKIPPED"]


class SnoozeRequest(ContractModel):
    minutes: Literal[5, 10, 15, 30]

    @field_validator("minutes", mode="before")
    @classmethod
    def validate_minutes(cls, value):
        if type(value) is not int:
            raise ValueError(
                "minutes phải là số nguyên."
            )

        return value