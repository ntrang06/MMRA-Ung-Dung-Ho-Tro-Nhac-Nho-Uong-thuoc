
import asyncio
import logging
import re

import httpx

from fastapi import APIRouter, HTTPException, Query, Response
from pydantic import BaseModel
from app.api.deps import AuthenticatedUser

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/medicine-lookup",
    tags=["Medicine lookup"],
)

FDA_URL = "https://api.fda.gov/drug/ndc.json"
CANADA_URL = "https://health-products.canada.ca/api/drug"

MAX_RESULTS_PER_SOURCE = 20


class MedicineCandidate(BaseModel):
    id: str
    name: str
    strength: str = ""
    dosage_form: str = ""
    registration_number: str = ""
    active_ingredient: str = ""
    source: str = ""


class MedicineLookupResponse(BaseModel):
    items: list[MedicineCandidate]


def text_value(value) -> str:
    if value is None:
        return ""
    return str(value).strip()


def simple_strength(value: str) -> str:
    """Chỉ nhận hàm lượng đơn giản."""
    match = re.fullmatch(
        r"(\d+(?:[.,]\d+)?)\s*(mg|mcg|µg|g|IU|%)",
        value.strip(),
        flags=re.IGNORECASE,
    )
    if not match:
        return ""

    return f"{match.group(1)} {match.group(2)}"


def list_or_empty(payload) -> list:
    return payload if isinstance(payload, list) else []


async def get_json(client, url, params):
    response = await client.get(
        url,
        params=params,
        headers={"Accept": "application/json"},
    )
    response.raise_for_status()
    return response.json()


async def search_usa(client, query: str):
    escaped = (
        query.replace("\\", "\\\\")
        .replace('"', '\\"')
    )

    try:
        payload = await get_json(
            client,
            FDA_URL,
            {
                "search": f'brand_name:"{escaped}"',
                "limit": MAX_RESULTS_PER_SOURCE,
            },
        )
    except httpx.HTTPStatusError as error:
        if error.response.status_code == 404:
            return []
        raise

    results = payload.get("results", [])
    items = []

    for product in list_or_empty(results):
        if not isinstance(product, dict):
            continue

        name = text_value(product.get("brand_name"))
        ndc = text_value(product.get("product_ndc"))

        if not name or not ndc:
            continue

        ingredients = list_or_empty(
            product.get("active_ingredients")
        )

        ingredient_names = [
            text_value(x.get("name"))
            for x in ingredients
            if isinstance(x, dict)
            and text_value(x.get("name"))
        ]

        strength = ""

        if len(ingredients) == 1:
            ingredient = ingredients[0]
            if isinstance(ingredient, dict):
                raw = text_value(
                    ingredient.get("strength")
                )

                form = text_value(
                    product.get("dosage_form")
                ).upper()

                if (
                    "TABLET" in form
                    or "CAPSULE" in form
                ):
                    raw = re.sub(
                        r"\s*/\s*1\s*$",
                        "",
                        raw,
                    )

                strength = simple_strength(raw)

        items.append(
            MedicineCandidate(
                id=f"US:{ndc}",
                name=name,
                strength=strength,
                dosage_form=text_value(
                    product.get("dosage_form")
                ),
                registration_number=ndc,
                active_ingredient=", ".join(
                    ingredient_names
                ),
                source="Mỹ - openFDA",
            )
        )

    return items


async def canada_details(client, product: dict):
    drug_code = product.get("drug_code")

    if drug_code is None:
        return None

    code = text_value(drug_code)

    async def fetch(path):
        try:
            return await get_json(
                client,
                f"{CANADA_URL}/{path}/",
                {
                    "id": code,
                    "lang": "en",
                    "type": "json",
                },
            )
        except (httpx.HTTPError, ValueError):
            logger.warning(
                "Health Canada detail unavailable: %s",
                path,
            )
            return []

    ingredients_raw, forms_raw = await asyncio.gather(
        fetch("activeingredient"),
        fetch("form"),
    )

    ingredients = [
        x for x in list_or_empty(ingredients_raw)
        if isinstance(x, dict)
    ]

    forms = [
        text_value(x.get("pharmaceutical_form_name"))
        for x in list_or_empty(forms_raw)
        if isinstance(x, dict)
        and text_value(x.get("pharmaceutical_form_name"))
    ]

    ingredient_names = [
        text_value(x.get("ingredient_name"))
        for x in ingredients
        if text_value(x.get("ingredient_name"))
    ]

    strength = ""

    # Chỉ tự điền khi có một hoạt chất duy nhất,
    # không có mẫu số nồng độ hoặc đơn vị liều phức tạp.
    if len(ingredients) == 1:
        ingredient = ingredients[0]

        dosage_value = text_value(
            ingredient.get("dosage_value")
        )
        dosage_unit = text_value(
            ingredient.get("dosage_unit")
        )

        if not dosage_value and not dosage_unit:
            strength = simple_strength(
                text_value(
                    ingredient.get("strength")
                ) + " " +
                text_value(
                    ingredient.get("strength_unit")
                )
            )

    name = text_value(product.get("brand_name"))

    if not name:
        return None

    din = text_value(
        product.get("drug_identification_number")
    )

    return MedicineCandidate(
        id=f"CA:{code}",
        name=name,
        strength=strength,
        dosage_form=" / ".join(
            dict.fromkeys(forms)
        ),
        registration_number=din,
        active_ingredient=", ".join(
            ingredient_names
        ),
        source="Canada - Health Canada DPD",
    )


async def search_canada(client, query: str):
    try:
        products = await get_json(
            client,
            f"{CANADA_URL}/drugproduct/",
            {
                "brandname": query,
                "lang": "en",
                "type": "json",
            },
        )
    except httpx.HTTPStatusError as error:
        if error.response.status_code == 404:
            return []
        raise

    matching = []

    for product in list_or_empty(products):
        if not isinstance(product, dict):
            continue

        name = text_value(
            product.get("brand_name")
        )

        if query.casefold() in name.casefold():
            matching.append(product)

        if len(matching) >= MAX_RESULTS_PER_SOURCE:
            break

    # Hạn chế số request đồng thời đến API Canada.
    semaphore = asyncio.Semaphore(4)

    async def controlled_detail(product):
        async with semaphore:
            return await canada_details(client, product)

    details = await asyncio.gather(
        *[
            controlled_detail(product)
            for product in matching
        ]
    )

    return [
        item for item in details
        if item is not None
    ]


@router.get(
    "",
    response_model=MedicineLookupResponse,
)
async def lookup_medicines(
    user: AuthenticatedUser,
    response: Response,
    q: str = Query(min_length=3, max_length=100),
):
    response.headers["Cache-Control"] = "no-store"

    query = " ".join(q.strip().split())

    if len(query) < 3:
        raise HTTPException(
            status_code=422,
            detail="Tên thuốc phải có ít nhất 3 ký tự.",
        )

    async with httpx.AsyncClient(
        timeout=20.0,
        follow_redirects=False,
    ) as client:

        results = await asyncio.gather(
            search_usa(client, query),
            search_canada(client, query),
            return_exceptions=True,
        )

    items = []
    failed_sources = 0

    for source_result in results:
        if isinstance(source_result, BaseException):
            failed_sources += 1

            logger.warning(
                "Medicine lookup source unavailable: %s",
                type(source_result).__name__,
            )
            continue

        items.extend(source_result)

    if failed_sources == len(results):
        raise HTTPException(
            status_code=503,
            detail=(
                "Các nguồn dữ liệu thuốc tạm thời "
                "không khả dụng. Vui lòng thử lại."
            ),
        )

    unique = []
    seen = set()

    for item in items:
        if item.id in seen:
            continue

        seen.add(item.id)
        unique.append(item)

    return MedicineLookupResponse(
        items=unique
    )
