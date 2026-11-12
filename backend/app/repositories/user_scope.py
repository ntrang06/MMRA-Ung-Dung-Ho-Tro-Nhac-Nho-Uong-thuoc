from typing import Literal

from firebase_admin import firestore

from app.api.deps import CurrentUser
from app.core.firebase import get_firebase_app


UserCollection = Literal[
    "medicines",
    "schedules",
    "doses",
]


def user_collection(
    user: CurrentUser,
    name: UserCollection,
):
    if name not in {
        "medicines",
        "schedules",
        "doses",
    }:
        raise ValueError(
            "Collection không được hỗ trợ."
        )

    if not user.uid or "/" in user.uid:
        raise ValueError(
            "UID không hợp lệ."
        )

    client = firestore.client(
        app=get_firebase_app()
    )

    return (
        client.collection("users")
        .document(user.uid)
        .collection(name)
    )


def user_document(
    user: CurrentUser,
    name: UserCollection,
    document_id: str,
):
    if not document_id or "/" in document_id:
        raise ValueError(
            "Document ID không hợp lệ."
        )

    return user_collection(
        user,
        name,
    ).document(document_id)