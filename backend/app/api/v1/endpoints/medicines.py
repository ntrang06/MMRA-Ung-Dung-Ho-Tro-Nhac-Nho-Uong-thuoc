
import base64
import binascii
import logging
from datetime import datetime, timezone
from urllib.parse import urlsplit

from fastapi import (
    APIRouter,
    Header,
    HTTPException,
    Response,
    status,
)
from firebase_admin import firestore
from google.api_core.exceptions import (
    GoogleAPICallError,
    NotFound,
)
from google.cloud.firestore_v1.base_query import FieldFilter
from pydantic import (
    AwareDatetime,
    BaseModel,
    ConfigDict,
    Field,
    field_validator,
)

from app.api.deps import AuthenticatedUser
from app.core.firebase import get_firebase_app
from app.repositories.user_scope import (
    user_collection,
    user_document,
)
from app.schemas.medication import DocumentId
from app.services.medicine_idempotency import (
    medicine_payload_digest,
    medicine_request_document_id,
    validate_idempotency_key,
)


logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/medicines",
    tags=["Medicines"],
)


# ============================================================
# 1. SCHEMAS
# ============================================================

class MedicineInput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    medicineName: str = Field(
        min_length=1,
        max_length=200,
    )
    strength: str = Field(
        default="",
        max_length=50,
    )
    strengthUnit: str = Field(
        default="",
        max_length=20,
    )
    dosageForm: str = Field(
        default="",
        max_length=100,
    )
    note: str = Field(
        default="",
        max_length=2000,
    )
    imageUrl: str | None = Field(
        default=None,
        max_length=140000,
    )

    @field_validator(
        "medicineName",
        "strength",
        "strengthUnit",
        "dosageForm",
        "note",
    )
    @classmethod
    def clean_text(
        cls,
        value: str,
    ) -> str:
        return value.strip()

    @field_validator("medicineName")
    @classmethod
    def validate_name(
        cls,
        value: str,
    ) -> str:
        if not value:
            raise ValueError(
                "Tên thuốc không được để trống."
            )

        return value

    @field_validator("imageUrl")
    @classmethod
    def validate_image(
        cls,
        value: str | None,
    ) -> str | None:
        if value is None:
            return None

        if value.startswith("https://"):
            parsed = urlsplit(value)

            if (
                parsed.scheme != "https"
                or not parsed.hostname
                or parsed.username is not None
                or parsed.password is not None
            ):
                raise ValueError(
                    "Đường dẫn ảnh HTTPS không hợp lệ."
                )

            return value

        prefix = "data:image/jpeg;base64,"

        if not value.startswith(prefix):
            raise ValueError(
                "Ảnh phải là JPEG Base64 hoặc HTTPS."
            )

        encoded = value[len(prefix):]

        if not encoded:
            raise ValueError(
                "Ảnh không hợp lệ."
            )

        try:
            image_bytes = base64.b64decode(
                encoded,
                validate=True,
            )
        except (ValueError, binascii.Error):
            raise ValueError(
                "Dữ liệu ảnh Base64 không hợp lệ."
            ) from None

        if (
            not 3 <= len(image_bytes) <= 100000
            or not image_bytes.startswith(b"\xff\xd8")
        ):
            raise ValueError(
                "Ảnh JPEG không hợp lệ hoặc quá lớn."
            )

        return value


class MedicineUpdateInput(MedicineInput):
    expectedVersion: int = Field(
        ge=1,
        strict=True,
    )


class MedicineResponse(MedicineInput):
    medicineId: DocumentId
    version: int = Field(
        ge=1,
        strict=True,
    )
    createdAt: AwareDatetime | None = None
    updatedAt: AwareDatetime | None = None


class MedicineDeletionResponse(BaseModel):
    medicineId: str
    doseIdsToCancel: list[str]


# ============================================================
# 2. HELPERS
# ============================================================

def medicine_from_snapshot(snapshot):
    if not snapshot.exists:
        return None

    data = snapshot.to_dict() or {}

    return MedicineResponse.model_validate({
        "medicineId": snapshot.id,
        "medicineName": data.get(
            "medicineName", ""
        ),
        "strength": data.get(
            "strength", ""
        ),
        "strengthUnit": data.get(
            "strengthUnit", ""
        ),
        "dosageForm": data.get(
            "dosageForm", ""
        ),
        "note": data.get(
            "note", ""
        ),
        "imageUrl": data.get(
            "imageUrl"
        ),
        "version": data.get("version", 1),
        "createdAt": data.get(
            "createdAt"
        ),
        "updatedAt": data.get(
            "updatedAt"
        ),
    })


def medicine_service_unavailable():
    logger.exception(
        "Không truy cập được dữ liệu thuốc trên Firestore."
    )

    return HTTPException(
        status_code=503,
        detail={
            "code": "medicines_unavailable",
            "message": (
                "Chưa xác nhận được kết quả thao tác. "
                "Vui lòng tải lại danh sách trước khi thử lại."
            ),
        },
    )


# ============================================================
# 3. GET - DANH SÁCH THUỐC
# ============================================================

@router.get(
    "",
    response_model=list[MedicineResponse],
)
def list_medicines(
    user: AuthenticatedUser,
    response: Response,
):
    response.headers["Cache-Control"] = "no-store"

    try:
        collection = user_collection(
            user,
            "medicines",
        )

        snapshots = (
            collection
            .order_by(
                "createdAt",
                direction="DESCENDING",
            )
            .stream()
        )

        result = []

        for snapshot in snapshots:
            medicine = medicine_from_snapshot(
                snapshot
            )

            if medicine is not None:
                result.append(medicine)

        return result

    except GoogleAPICallError:
        raise medicine_service_unavailable() from None


# ============================================================
# 4. GET - CHI TIẾT THUỐC
# ============================================================

@router.get(
    "/{medicine_id}",
    response_model=MedicineResponse,
)
def get_medicine(
    medicine_id: DocumentId,
    user: AuthenticatedUser,
    response: Response,
):
    response.headers["Cache-Control"] = "no-store"

    try:
        snapshot = user_document(
            user,
            "medicines",
            medicine_id,
        ).get()

        medicine = medicine_from_snapshot(
            snapshot
        )

        if medicine is None:
            raise HTTPException(
                status_code=404,
                detail={
                    "code": "medicine_not_found",
                    "message": "Không tìm thấy thuốc.",
                },
            )

        return medicine

    except GoogleAPICallError:
        raise medicine_service_unavailable() from None


# ============================================================
# 5. POST - THÊM THUỐC KHÔNG TẠO TRÙNG
# ============================================================

@router.post(
    "",
    response_model=MedicineResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_medicine(
    body: MedicineInput,
    user: AuthenticatedUser,
    response: Response,
    idempotency_key: str = Header(
        ...,
        alias="Idempotency-Key",
    ),
):
    response.headers["Cache-Control"] = "no-store"

    try:
        key = validate_idempotency_key(
            idempotency_key
        )
    except ValueError as error:
        raise HTTPException(
            status_code=422,
            detail={
                "code": "invalid_idempotency_key",
                "message": str(error),
            },
        ) from None

    # Khóa được phân vùng theo tài khoản Firebase.
    request_id = medicine_request_document_id(
        user.uid,
        key,
    )

    # Dấu vân tay để phát hiện cùng khóa nhưng khác nội dung.
    fields = body.model_dump()
    payload_digest = medicine_payload_digest(
        fields
    )

    request_ref = user_document(
        user,
        "medicineCreateRequests",
        request_id,
    )

    medicine_collection = user_collection(
        user,
        "medicines",
    )

    @firestore.transactional
    def create_once(transaction):
        # Firestore yêu cầu đọc trước khi ghi.
        request_snapshot = request_ref.get(
            transaction=transaction
        )

        if request_snapshot.exists:
            previous = (
                request_snapshot.to_dict()
                or {}
            )

            if (
                previous.get("payloadDigest")
                != payload_digest
            ):
                raise HTTPException(
                    status_code=409,
                    detail={
                        "code": "idempotency_key_reused",
                        "message": (
                            "Khóa yêu cầu đã được dùng "
                            "cho nội dung thuốc khác."
                        ),
                    },
                )

            previous_medicine_id = previous.get(
                "medicineId"
            )

            if not previous_medicine_id:
                raise HTTPException(
                    status_code=503,
                    detail={
                        "code": "invalid_create_request",
                        "message": (
                            "Dữ liệu yêu cầu thêm thuốc "
                            "không hợp lệ."
                        ),
                    },
                )

            # Không tạo thuốc lần nữa.
            # Trả kết quả đã lưu của yêu cầu đầu tiên.
            previous_response = previous.get(
                "response"
            )

            if not isinstance(
                previous_response,
                dict,
            ):
                raise HTTPException(
                    status_code=503,
                    detail={
                        "code": "invalid_create_request",
                        "message": (
                            "Chưa xác nhận được kết quả "
                            "thêm thuốc trước đó."
                        ),
                    },
                )

            saved_response = (
                MedicineResponse.model_validate(
                    previous_response
                )
            )

            return saved_response, True

        now = datetime.now(timezone.utc)
        medicine_ref = medicine_collection.document()

        saved_fields = {
            **fields,
            "createdAt": now,
            "updatedAt": now,
            "version": 1,
        }

        created_response = MedicineResponse(
            medicineId=medicine_ref.id,
            **saved_fields,
        )

        # Hai ghi này nằm trong cùng một transaction.
        # Hoặc cùng thành công, hoặc cùng thất bại.
        transaction.create(
            medicine_ref,
            saved_fields,
        )

        transaction.create(
            request_ref,
            {
                "medicineId": medicine_ref.id,
                "payloadDigest": payload_digest,
                "response": (
                    created_response.model_dump(
                        mode="python"
                    )
                ),
                "createdAt": now,
            },
        )

        return created_response, False

    try:
        client = firestore.client(
            app=get_firebase_app()
        )

        result, replayed = create_once(
            client.transaction()
        )

        if replayed:
            response.status_code = 200
            response.headers[
                "Idempotency-Replayed"
            ] = "true"
        else:
            response.status_code = 201
            response.headers[
                "Idempotency-Replayed"
            ] = "false"

        return result

    except GoogleAPICallError:
        raise medicine_service_unavailable() from None


# ============================================================
# 6. PUT - SỬA THUỐC CÓ KIỂM TRA VERSION
# ============================================================

@router.put(
    "/{medicine_id}",
    response_model=MedicineResponse,
)
def update_medicine(
    medicine_id: DocumentId,
    body: MedicineUpdateInput,
    user: AuthenticatedUser,
    response: Response,
):
    response.headers["Cache-Control"] = "no-store"

    reference = user_document(
        user,
        "medicines",
        medicine_id,
    )

    @firestore.transactional
    def save(transaction):
        snapshot = reference.get(
            transaction=transaction
        )

        if not snapshot.exists:
            raise HTTPException(
                status_code=404,
                detail={
                    "code": "medicine_not_found",
                    "message": (
                        "Không tìm thấy thuốc cần sửa."
                    ),
                },
            )

        previous = snapshot.to_dict() or {}

        current_version = previous.get(
            "version", 1
        )

        if (
            type(current_version) is not int
            or current_version < 1
        ):
            raise HTTPException(
                status_code=503,
                detail={
                    "code": "medicine_invalid_version",
                    "message": (
                        "Phiên bản dữ liệu thuốc không hợp lệ."
                    ),
                },
            )

        if body.expectedVersion != current_version:
            raise HTTPException(
                status_code=409,
                detail={
                    "code": "medicine_version_conflict",
                    "message": (
                        "Thông tin thuốc đã thay đổi. "
                        "Vui lòng tải lại trước khi sửa."
                    ),
                    "currentVersion": current_version,
                },
            )

        now = datetime.now(timezone.utc)
        new_version = current_version + 1

        fields = body.model_dump(
            exclude={"expectedVersion"}
        )

        fields.update({
            "updatedAt": now,
            "version": new_version,
        })

        transaction.update(
            reference,
            fields,
        )

        return MedicineResponse(
            medicineId=medicine_id,
            **body.model_dump(
                exclude={"expectedVersion"}
            ),
            createdAt=previous.get("createdAt"),
            updatedAt=now,
            version=new_version,
        )

    try:
        client = firestore.client(
            app=get_firebase_app()
        )

        return save(client.transaction())

    except NotFound:
        raise HTTPException(
            status_code=404,
            detail={
                "code": "medicine_not_found",
                "message": "Thuốc không còn tồn tại.",
            },
        ) from None

    except GoogleAPICallError:
        raise medicine_service_unavailable() from None


# ============================================================
# 7. DELETE - XÓA THUỐC VÀ LỊCH LIÊN QUAN
# Giữ nguyên nghiệp vụ xóa đã có.
# ============================================================

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

            if (
                type(version) is not int
                or version < 1
            ):
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
                not isinstance(
                    scheduled,
                    datetime,
                )
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

            elif (
                data.get("nextReminderAt")
                is not None
            ):
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

        transaction.delete(
            medicine_ref
        )

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

        return remove(
            client.transaction()
        )

    except (
        GoogleAPICallError,
        ValueError,
    ):
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
