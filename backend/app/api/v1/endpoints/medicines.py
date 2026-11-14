import logging
from datetime import datetime, timezone

from fastapi import APIRouter, HTTPException, Response
from firebase_admin import firestore
from google.api_core.exceptions import GoogleAPICallError
from google.cloud.firestore_v1.base_query import FieldFilter
from pydantic import BaseModel

from app.api.deps import AuthenticatedUser
from app.core.firebase import get_firebase_app
from app.repositories.user_scope import (
    user_collection,
    user_document,
)
from app.schemas.medication import DocumentId


logger = logging.getLogger(__name__)
router = APIRouter(
    prefix="/medicines",
    tags=["Medicines"],
)


class MedicineDeletionResponse(BaseModel):
    medicineId: str
    doseIdsToCancel: list[str]


@router.delete(
    "/{medicine_id}",
    response_model=MedicineDeletionResponse,
)
def delete_medicine(
    medicine_id: DocumentId,
    user: AuthenticatedUser,
    response: Response,
):
    response.headers["Cache-Control"] = "no-store"

    medicine_ref = user_document(
        user,
        "medicines",
        medicine_id,
    )

    query_filter = FieldFilter(
        "medicineId",
        "==",
        medicine_id,
    )

    schedules_query = user_collection(
        user,
        "schedules",
    ).where(filter=query_filter)

    doses_query = user_collection(
        user,
        "doses",
    ).where(filter=query_filter)

    @firestore.transactional
    def remove(transaction):
        medicine = medicine_ref.get(
            transaction=transaction
        )

        if not medicine.exists:
            raise HTTPException(
                status_code=404,
                detail={
                    "code": "medicine_not_found",
                },
            )

        # Đọc toàn bộ dữ liệu trước khi ghi.
        schedules = list(
            schedules_query.stream(
                transaction=transaction
            )
        )

        doses = list(
            doses_query.stream(
                transaction=transaction
            )
        )

        now = datetime.now(timezone.utc)
        writes = []

        for snapshot in schedules:
            data = snapshot.to_dict() or {}

            if data.get("deletedAt") is not None:
                continue

            version = data.get("version")

            if type(version) is not int or version < 1:
                raise ValueError(
                    "Lịch uống có phiên bản không hợp lệ."
                )

            writes.append(
                (
                    snapshot.reference,
                    {
                        "enabled": False,
                        "deletedAt": now,
                        "updatedAt": now,
                        "version": version + 1,
                    },
                )
            )

        for snapshot in doses:
            data = snapshot.to_dict() or {}

            if data.get("status") != "PENDING":
                continue

            scheduled = data.get("scheduledAt")

            if (
                not isinstance(scheduled, datetime)
                or scheduled.utcoffset() is None
            ):
                raise ValueError(
                    "Liều thuốc thiếu thời gian hợp lệ."
                )

            if scheduled >= now:
                writes.append(
                    (
                        snapshot.reference,
                        {
                            "status": "CANCELLED",
                            "nextReminderAt": None,
                            "updatedAt": now,
                        },
                    )
                )

            elif data.get("nextReminderAt") is not None:
                # Dừng nhắc lại, giữ trạng thái và giờ gốc
                # của liều quá khứ.
                writes.append(
                    (
                        snapshot.reference,
                        {
                            "nextReminderAt": None,
                            "updatedAt": now,
                        },
                    )
                )

        for reference, values in writes:
            transaction.update(
                reference,
                values,
            )

        transaction.delete(medicine_ref)

        return MedicineDeletionResponse(
            medicineId=medicine_id,
            doseIdsToCancel=[
                snapshot.id
                for snapshot in doses
            ],
        )

    try:
        client = firestore.client(
            app=get_firebase_app()
        )

        return remove(client.transaction())

    except (GoogleAPICallError, ValueError):
        logger.exception(
            "Không xóa được thuốc và lịch liên quan"
        )

        raise HTTPException(
            status_code=503,
            detail={
                "code": "medicine_delete_unavailable",
                "message": (
                    "Chưa xác nhận được kết quả xóa. "
                    "Vui lòng tải lại danh sách."
                ),
            },
        ) from None