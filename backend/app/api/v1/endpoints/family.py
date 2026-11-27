
import hashlib
import logging
import secrets
from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, HTTPException, Response, status
from firebase_admin import firestore
from google.api_core.exceptions import GoogleAPICallError
from google.cloud import firestore as google_firestore
from google.cloud.firestore_v1.base_query import FieldFilter
from pydantic import BaseModel, ConfigDict, Field

from app.api.deps import AuthenticatedUser
from app.core.firebase import get_firebase_app


logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/family",
    tags=["Family"],
)

INVITE_TTL_HOURS = 24


# ============================================================
# SCHEMAS
# ============================================================

class AcceptInvitationRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    code: str = Field(
        min_length=32,
        max_length=128,
        pattern=r"^[0-9a-fA-F]+$",
    )


class InvitationResponse(BaseModel):
    invitationId: str
    code: str
    expiresAt: datetime


class AcceptInvitationResponse(BaseModel):
    linkId: str
    status: str



class FamilyPermissions(BaseModel):
    viewSchedule: bool = False
    viewDoseStatus: bool = False
    receiveMissedDoseAlerts: bool = False



class FamilyMemberResponse(BaseModel):
    linkId: str
    role: str
    memberUid: str
    status: str
    permissions: FamilyPermissions


class FamilyMembersResponse(BaseModel):
    items: list[FamilyMemberResponse]


# ============================================================
# HELPERS
# ============================================================

def get_firestore_client():
    return firestore.client(
        app=get_firebase_app()
    )


def now_utc() -> datetime:
    return datetime.now(timezone.utc)


def invite_hash(code: str) -> str:
    return hashlib.sha256(
        code.upper().encode("ascii")
    ).hexdigest()


def generate_invite_code() -> str:
    # 128-bit random code, không sử dụng mã PIN ngắn.
    return secrets.token_hex(16).upper()


def family_link_id(
    owner_uid: str,
    caregiver_uid: str,
) -> str:
    # Định danh ổn định để tránh trùng liên kết.
    payload = (
        f"{owner_uid}:{caregiver_uid}"
    ).encode("utf-8")

    return hashlib.sha256(payload).hexdigest()


def family_error(
    code: str,
    message: str,
    http_status: int,
) -> HTTPException:
    return HTTPException(
        status_code=http_status,
        detail={
            "code": code,
            "message": message,
        },
    )


def unavailable() -> HTTPException:
    logger.exception(
        "Family Firestore operation failed."
    )

    return family_error(
        "family_unavailable",
        "Dịch vụ Family tạm thời không khả dụng.",
        503,
    )


def is_invitation_expired(
    expires_at: datetime,
    now: datetime,
) -> bool:
    if not isinstance(expires_at, datetime):
        return True

    if expires_at.tzinfo is None:
        return True

    return expires_at <= now


def snapshot_to_member(
    snapshot,
    current_uid: str,
) -> FamilyMemberResponse | None:
    if not snapshot.exists:
        return None

    data = snapshot.to_dict() or {}

    if data.get("status") != "active":
        return None

    owner_uid = data.get("ownerUid")
    caregiver_uid = data.get("caregiverUid")

    if current_uid == owner_uid:
        role = "owner"
        member_uid = caregiver_uid
    elif current_uid == caregiver_uid:
        role = "caregiver"
        member_uid = owner_uid
    else:
        return None

    if not isinstance(member_uid, str):
        return None

    permissions = FamilyPermissions.model_validate(
        data.get("permissions") or {}
    )

    return FamilyMemberResponse(
        linkId=snapshot.id,
        role=role,
        memberUid=member_uid,
        status="active",
        permissions=permissions,
    )


# ============================================================
# POST - TẠO LỜI MỜI
# ============================================================

@router.post(
    "/invitations",
    response_model=InvitationResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_invitation(
    user: AuthenticatedUser,
    response: Response,
):
    response.headers["Cache-Control"] = "no-store"

    now = now_utc()
    expires_at = now + timedelta(
        hours=INVITE_TTL_HOURS
    )

    code = generate_invite_code()
    invitation_id = invite_hash(code)

    try:
        db = get_firestore_client()

        reference = (
            db.collection("familyInvitations")
            .document(invitation_id)
        )

        reference.create({
            "ownerUid": user.uid,
            "inviteCodeHash": invitation_id,
            "status": "pending",
            "createdAt": now,
            "expiresAt": expires_at,
            "acceptedByUid": None,
        })

    except GoogleAPICallError:
        raise unavailable() from None

    return InvitationResponse(
        invitationId=invitation_id,
        code=code,
        expiresAt=expires_at,
    )


# ============================================================
# POST - CHẤP NHẬN LỜI MỜI
# ============================================================

@router.post(
    "/invitations/accept",
    response_model=AcceptInvitationResponse,
)
def accept_invitation(
    payload: AcceptInvitationRequest,
    user: AuthenticatedUser,
    response: Response,
):
    response.headers["Cache-Control"] = "no-store"

    invitation_id = invite_hash(payload.code)

    try:
        db = get_firestore_client()

        invitation_ref = (
            db.collection("familyInvitations")
            .document(invitation_id)
        )

        @google_firestore.transactional
        def accept(transaction):
            invitation_snapshot = invitation_ref.get(
                transaction=transaction
            )

            if not invitation_snapshot.exists:
                raise family_error(
                    "invitation_invalid",
                    "Mã mời không hợp lệ.",
                    400,
                )

            invitation = (
                invitation_snapshot.to_dict() or {}
            )

            if invitation.get("status") != "pending":
                raise family_error(
                    "invitation_unavailable",
                    "Mã mời đã được sử dụng hoặc thu hồi.",
                    409,
                )

            now = now_utc()

            if is_invitation_expired(
                invitation.get("expiresAt"),
                now,
            ):
                raise family_error(
                    "invitation_expired",
                    "Mã mời đã hết hạn.",
                    400,
                )

            owner_uid = invitation.get("ownerUid")

            if (
                not isinstance(owner_uid, str)
                or not owner_uid
                or "/" in owner_uid
            ):
                raise family_error(
                    "invitation_invalid",
                    "Lời mời không hợp lệ.",
                    400,
                )

            if owner_uid == user.uid:
                raise family_error(
                    "self_invitation",
                    "Không thể tự liên kết với chính mình.",
                    400,
                )

            link_id = family_link_id(
                owner_uid,
                user.uid,
            )

            link_ref = (
                db.collection("familyLinks")
                .document(link_id)
            )

            link_snapshot = link_ref.get(
                transaction=transaction
            )

            if (
                link_snapshot.exists
                and (link_snapshot.to_dict() or {}).get(
                    "status"
                ) == "active"
            ):
                raise family_error(
                    "family_already_linked",
                    "Hai tài khoản đã được liên kết.",
                    409,
                )

            permissions = FamilyPermissions()

            link_data = {
                "ownerUid": owner_uid,
                "caregiverUid": user.uid,
                "status": "active",
                "permissions": permissions.model_dump(),
                "createdAt": now,
                "updatedAt": now,
            }

            # Toàn bộ việc ghi được commit cùng transaction.
            # Không được đọc Firestore sau lệnh ghi đầu tiên.
            if link_snapshot.exists:
                transaction.set(
                    link_ref,
                    link_data,
                )
            else:
                transaction.create(
                    link_ref,
                    link_data,
                )

            transaction.update(
                invitation_ref,
                {
                    "status": "accepted",
                    "acceptedByUid": user.uid,
                    "acceptedAt": now,
                },
            )

            return link_id

        link_id = accept(
            db.transaction()
        )

    except HTTPException:
        raise

    except GoogleAPICallError:
        raise unavailable() from None

    return AcceptInvitationResponse(
        linkId=link_id,
        status="active",
    )


# ============================================================
# GET - DANH SÁCH FAMILY
# ============================================================

@router.get(
    "/members",
    response_model=FamilyMembersResponse,
)
def list_family_members(
    user: AuthenticatedUser,
    response: Response,
):
    response.headers["Cache-Control"] = "no-store"

    try:
        db = get_firestore_client()
        collection = db.collection("familyLinks")

        owned = collection.where(
            filter=FieldFilter(
                "ownerUid", "==", user.uid
            )
        ).stream()

        caring = collection.where(
            filter=FieldFilter(
                "caregiverUid", "==", user.uid
            )
        ).stream()

        members_by_id = {}

        for snapshot in list(owned) + list(caring):
            member = snapshot_to_member(
                snapshot,
                user.uid,
            )

            if member is not None:
                members_by_id[member.linkId] = member

        return FamilyMembersResponse(
            items=sorted(
                members_by_id.values(),
                key=lambda member: member.linkId,
            )
        )

    except GoogleAPICallError:
        raise unavailable() from None
