
from collections import Counter

from fastapi.routing import (
    APIRoute,
    iter_route_contexts,
)

from app.main import app
from app.api.v1.endpoints import doses


def get_api_operations():
    operations = []

    # Hỗ trợ router lồng nhau của FastAPI >= 0.137.2
    for context in iter_route_contexts(app.routes):
        if not isinstance(context.route, APIRoute):
            continue

        path = context.path

        if not path.startswith("/api/v1/"):
            continue

        for method in context.methods:
            if method not in {"HEAD", "OPTIONS"}:
                operations.append((method, path))

    return operations


def test_no_duplicate_schedule_and_dose_routes():
    operations = get_api_operations()

    assert operations, (
        "Không tìm thấy API nào trong FastAPI."
    )

    counts = Counter(operations)

    duplicates = [
        operation
        for operation, count in counts.items()
        if count > 1
    ]

    assert not duplicates, (
        f"Phát hiện API trùng: {duplicates}"
    )


def test_android_api_contract_is_preserved():
    operations = set(get_api_operations())

    required = {
        ("GET", "/api/v1/schedules"),
        ("POST", "/api/v1/schedules"),
        ("GET", "/api/v1/schedules/{schedule_id}"),
        ("PUT", "/api/v1/schedules/{schedule_id}"),
        ("DELETE", "/api/v1/schedules/{schedule_id}"),
        ("GET", "/api/v1/doses"),
        ("GET", "/api/v1/doses/{dose_id}"),
        ("PATCH", "/api/v1/doses/{dose_id}/action"),
        ("POST", "/api/v1/doses/{dose_id}/snooze"),
    }

    missing = required - operations

    assert not missing, (
        f"Thiếu API Android đang sử dụng: {missing}"
    )


def test_doses_router_has_no_schedule_routes():
    operations = [
        context.path
        for context in iter_route_contexts(
            doses.router.routes
        )
        if isinstance(context.route, APIRoute)
    ]

    assert operations, (
        "Dose router đang rỗng."
    )

    schedule_routes = [
        path
        for path in operations
        if path.startswith("/schedules")
    ]

    assert not schedule_routes, (
        f"Dose router chứa API lịch uống: {schedule_routes}"
    )
