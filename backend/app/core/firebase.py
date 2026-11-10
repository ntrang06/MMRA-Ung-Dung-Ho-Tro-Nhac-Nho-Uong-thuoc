
from pathlib import Path

import firebase_admin
from firebase_admin import auth, credentials

from app.core.config import get_settings


def get_firebase_app() -> firebase_admin.App:
    settings = get_settings()

    try:
        return firebase_admin.get_app()
    except ValueError:
        pass

    credential_path = settings.firebase_credentials_path

    if credential_path:
        path = Path(credential_path)

        if not path.is_absolute():
            path = Path.cwd() / path

        if not path.is_file():
            raise FileNotFoundError(
                "Firebase service account file was not found."
            )

        credential = credentials.Certificate(str(path))
    else:
        # Use Application Default Credentials in deployed environments.
        credential = credentials.ApplicationDefault()

    return firebase_admin.initialize_app(
        credential,
        options={
            "projectId": settings.firebase_project_id,
        },
    )

def get_firebase_auth():
    app = get_firebase_app()
    return auth, app
