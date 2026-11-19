
"""
MMRA - Tra cứu thông tin thuốc qua openFDA.

Nguồn: US FDA National Drug Code Directory
API: https://api.fda.gov/drug/ndc.json

Không yêu cầu DLPL_API_KEY.
Không tự suy đoán dạng thuốc, hàm lượng hoặc liều dùng.
"""

import logging
import re

import httpx

from fastapi import (
    APIRouter,
    HTTPException,
    Query,
    Response,
)
from pydantic import BaseModel

from app.api.deps import AuthenticatedUser


logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/medicine-lookup",
    tags=["Medicine lookup"],
)

OPENFDA_URL = "https://api.fda.gov/drug/ndc.json"


class MedicineCandidate(BaseModel):
    id: str
    name: str
    strength: str = ""
    dosage_form: str = ""
    registration_number: str = ""
    active_ingredient: str = ""
    source: str = "openFDA - US FDA NDC Directory"


class MedicineLookupResponse(BaseModel):
    items: list[MedicineCandidate]


def safe_strength(product: dict) -> str:
    """
    Chỉ trích xuất hàm lượng khi có duy nhất một
    hoạt chất và định dạng có thể xác định an toàn.

    Với thuốc phối hợp hoặc nồng độ phức tạp,
    để trống để người dùng kiểm tra.
    """

    ingredients = product.get(
        "active_ingredients"
    ) or []

    if not isinstance(ingredients, list):
        return ""

    if len(ingredients) != 1:
        return ""

    ingredient = ingredients[0]

    if not isinstance(ingredient, dict):
        return ""

    raw = str(
        ingredient.get("strength") or ""
    ).strip()

    dosage_form = str(
        product.get("dosage_form") or ""
    ).upper()

    # Chỉ bỏ mẫu số /1 ở dạng thuốc rắn.
    if (
        "TABLET" in dosage_form
        or "CAPSULE" in dosage_form
    ):
        raw = re.sub(
            r"\s*/\s*1\s*$",
            "",
            raw,
        )

    match = re.fullmatch(
        r"(\d+(?:[.,]\d+)?)\s*(mg|mcg|µg|g|IU|%)",
        raw,
        flags=re.IGNORECASE,
    )

    if not match:
        return ""

    amount = match.group(1)
    unit = match.group(2)

    return f"{amount} {unit}"


def extract_ingredients(product: dict) -> str:
    ingredients = product.get(
        "active_ingredients"
    ) or []

    if not isinstance(ingredients, list):
        return ""

    names = []

    for ingredient in ingredients:
        if not isinstance(ingredient, dict):
            continue

        name = str(
            ingredient.get("name") or ""
        ).strip()

        if name:
            names.append(name)

    return ", ".join(names)


@router.get(
    "",
    response_model=MedicineLookupResponse,
)
async def lookup_medicines(
    user: AuthenticatedUser,
    response: Response,
    q: str = Query(
        min_length=3,
        max_length=100,
    ),
):
    # Không lưu dữ liệu tra cứu cá nhân tại proxy.
    response.headers["Cache-Control"] = "no-store"

    keyword = " ".join(
        q.strip().split()
    )

    if len(keyword) < 3:
        raise HTTPException(
            status_code=422,
            detail="Tên thuốc phải có ít nhất 3 ký tự.",
        )

    # Escape ký tự đặc biệt trong truy vấn openFDA.
    escaped = (
        keyword
        .replace("\\", "\\\\")
        .replace('"', '\\"')
    )

    search_expression = (
        f'brand_name:"{escaped}"'
    )

    try:
        async with httpx.AsyncClient(
            timeout=15.0,
            follow_redirects=False,
        ) as client:

            upstream = await client.get(
                OPENFDA_URL,
                params={
                    "search": search_expression,
                    "limit": 20,
                },
                headers={
                    "Accept": "application/json",
                },
            )

        # openFDA có thể trả 404 nếu không có kết quả.
        if upstream.status_code == 404:
            return MedicineLookupResponse(
                items=[]
            )

        if upstream.status_code == 429:
            raise HTTPException(
                status_code=503,
                detail=(
                    "Đã vượt giới hạn truy vấn openFDA. "
                    "Vui lòng thử lại sau."
                ),
            )

        if upstream.status_code != 200:
            logger.warning(
                "openFDA HTTP status: %s",
                upstream.status_code,
            )

            raise HTTPException(
                status_code=502,
                detail=(
                    "Nguồn dữ liệu thuốc tạm thời "
                    "không khả dụng."
                ),
            )

        payload = upstream.json()

    except httpx.RequestError as error:
        logger.warning(
            "openFDA connection error: %s",
            type(error).__name__,
        )

        raise HTTPException(
            status_code=503,
            detail=(
                "Không kết nối được nguồn dữ liệu thuốc."
            ),
        ) from None

    except ValueError:
        raise HTTPException(
            status_code=502,
            detail=(
                "Nguồn dữ liệu thuốc trả về "
                "dữ liệu không hợp lệ."
            ),
        ) from None

    raw_results = (
        payload.get("results", [])
        if isinstance(payload, dict)
        else []
    )

    if not isinstance(raw_results, list):
        raise HTTPException(
            status_code=502,
            detail="Cấu trúc dữ liệu thuốc không hợp lệ.",
        )

    items = []
    seen = set()

    for product in raw_results:

        if not isinstance(product, dict):
            continue

        name = str(
            product.get("brand_name") or ""
        ).strip()

        product_ndc = str(
            product.get("product_ndc") or ""
        ).strip()

        if not name or not product_ndc:
            continue

        if product_ndc in seen:
            continue

        seen.add(product_ndc)

        dosage_form = str(
            product.get("dosage_form") or ""
        ).strip()

        strength = safe_strength(product)

        active_ingredient = extract_ingredients(
            product
        )

        items.append(
            MedicineCandidate(
                id=product_ndc,
                name=name,
                strength=strength,
                dosage_form=dosage_form,
                registration_number=product_ndc,
                active_ingredient=active_ingredient,
                source=(
                    "openFDA - US FDA NDC Directory"
                ),
            )
        )

    return MedicineLookupResponse(
        items=items
    )
