import asyncio
import logging
import re
import time
import unicodedata
from collections import OrderedDict
from typing import Any

import httpx
from fastapi import APIRouter, HTTPException, Query, Response
from pydantic import BaseModel

from app.api.deps import AuthenticatedUser

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/medicine-lookup",
    tags=["Medicine lookup"],
)

VIETNAM_DAV_URL = (
    "https://dichvucong.dav.gov.vn/api/services/app/soDangKy/GetAllPublicServerPaging"
)
FDA_URL = "https://api.fda.gov/drug/ndc.json"
CANADA_URL = "https://health-products.canada.ca/api/drug"

SOURCE_VIETNAM = "Việt Nam - Cục Quản lý Dược"
SOURCE_USA = "Mỹ - openFDA"
SOURCE_CANADA = "Canada - Health Canada DPD"

SOURCE_PRIORITY = {
    SOURCE_VIETNAM: 3,
    SOURCE_USA: 2,
    SOURCE_CANADA: 1,
}

MAX_RESULTS_PER_SOURCE = 20
MAX_CANADA_DETAIL_FETCHES = 10
SOURCE_TIMEOUT_SECONDS = 8.0
RETRYABLE_STATUS_CODES = {429, 500, 502, 503, 504}
LOOKUP_CACHE_TTL_SECONDS = 600.0
CANADA_DETAIL_CACHE_TTL_SECONDS = 1800.0

SYNONYM_MAP = {
    "paracetamol": "acetaminophen",
    "acetaminophen": "paracetamol",
    "salbutamol": "albuterol",
    "albuterol": "salbutamol",
    "hydrochlorid": "hydrochloride",
    "hydrochloride": "hydrochlorid",
    "clavulanic": "clavulanate",
    "clavulanate": "clavulanic",
    "amoxicilin": "amoxicillin",
    "omeprazol": "omeprazole",
}

TRAILING_STRENGTH_RE = re.compile(
    r"[\s\-/]+\d+(?:[.,]\d+)?(?:\s*/\s*\d+(?:[.,]\d+)?)?\s*"
    r"(?:mg/ml|mg|mcg|µg|μg|g|iu|ml|%)?\s*$",
    flags=re.IGNORECASE,
)

TRAILING_RELEASE_RE = re.compile(
    r"[\s\-]+(?:pr|xr|sr|mr|cr|er|la|xl|retard)\s*$",
    flags=re.IGNORECASE,
)

VN_REG_NUMBER_RE = re.compile(
    r"^(?:\d{10,}|(?:vn|vd|vs|gc|ql)[a-z0-9\-/]*\d+)$",
    flags=re.IGNORECASE,
)

STRENGTH_TOKEN_RE = re.compile(
    r"(?<![\d.,])(\d+(?:[.,]\d+)?)\s*(mg|mcg|[µμ]g|g|iu|%)"
    r"(?:\s*/\s*(?:(\d+(?:[.,]\d+)?)\s*)?(ml|l))?(?![a-zA-Z0-9])",
    flags=re.IGNORECASE,
)


class TTLCache:
    """In-memory cache có thời hạn (TTL) và giới hạn số mục LRU."""

    def __init__(self, ttl_seconds: float, max_size: int = 256):
        self.ttl_seconds = ttl_seconds
        self.max_size = max_size
        self._store: OrderedDict[str, tuple[float, Any]] = OrderedDict()

    def get(self, key: str, now: float | None = None) -> Any | None:
        current = time.monotonic() if now is None else now
        entry = self._store.get(key)
        if entry is None:
            return None

        expires_at, value = entry
        if current >= expires_at:
            self._store.pop(key, None)
            return None

        self._store.move_to_end(key)
        return value

    def set(self, key: str, value: Any, now: float | None = None) -> None:
        current = time.monotonic() if now is None else now
        expires_at = current + self.ttl_seconds
        if key in self._store:
            self._store.move_to_end(key)
        self._store[key] = (expires_at, value)
        while len(self._store) > self.max_size:
            self._store.popitem(last=False)

    def clear(self) -> None:
        self._store.clear()


_lookup_cache = TTLCache(ttl_seconds=LOOKUP_CACHE_TTL_SECONDS, max_size=256)
_canada_detail_cache = TTLCache(
    ttl_seconds=CANADA_DETAIL_CACHE_TTL_SECONDS,
    max_size=512,
)


class MedicineCandidate(BaseModel):
    id: str
    name: str
    strength: str = ""
    dosage_form: str = ""
    registration_number: str = ""
    active_ingredient: str = ""
    manufacturer: str = ""
    source: str = ""


class MedicineLookupResponse(BaseModel):
    items: list[MedicineCandidate]


def text_value(value: Any) -> str:
    if value is None:
        return ""
    return str(value).strip()


def fold_text(value: str) -> str:
    """Chuẩn hóa chuỗi để so khớp và khử trùng lặp không phân biệt dấu/ký tự đặc biệt."""
    normalized = unicodedata.normalize("NFKD", text_value(value).lower())
    without_marks = "".join(
        ch for ch in normalized if not unicodedata.combining(ch)
    ).replace("đ", "d")
    alnum = re.sub(r"[^a-z0-9]+", " ", without_marks)
    return " ".join(alnum.split())


def canonical_unit(unit: str) -> str:
    lowered = text_value(unit).lower()
    if lowered == "iu":
        return "IU"
    if lowered in {"µg", "μg", "mcg"}:
        return "mcg"
    return lowered


def simple_strength(value: str) -> str:
    """Chỉ nhận hàm lượng đơn giản với 1 con số dương và 1 đơn vị chuẩn."""
    match = re.fullmatch(
        r"(\d+(?:[.,]\d+)?)\s*(mg|mcg|µg|μg|g|IU|%)",
        text_value(value),
        flags=re.IGNORECASE,
    )
    if not match:
        return ""

    amount = match.group(1).replace(",", ".")
    try:
        if float(amount) <= 0:
            return ""
    except ValueError:
        return ""

    return f"{amount} {canonical_unit(match.group(2))}"


def normalize_strength_display(value: str) -> str:
    """Chuẩn hóa chuỗi hàm lượng để hiển thị; không tự suy đoán giá trị thiếu."""
    cleaned = " ".join(text_value(value).split())
    if not cleaned:
        return ""

    simple = simple_strength(cleaned)
    if simple:
        return simple

    def _format_token(match: re.Match[str]) -> str:
        num = match.group(1).replace(",", ".")
        unit = canonical_unit(match.group(2))
        denom_num = match.group(3)
        denom_unit = match.group(4)
        if denom_unit:
            denom = f"{denom_num.replace(',', '.')} " if denom_num else ""
            return f"{num} {unit}/{denom}{denom_unit.lower()}"
        return f"{num} {unit}"

    return STRENGTH_TOKEN_RE.sub(_format_token, cleaned)


def list_or_empty(payload: Any) -> list[Any]:
    return payload if isinstance(payload, list) else []


def build_query_variants(query: str) -> list[str]:
    """Sinh các biến thể truy vấn hợp lệ (tên gốc, bỏ hàm lượng, bỏ gạch nối, đồng nghĩa hoạt chất)."""
    base = " ".join(text_value(query).split())
    if len(base) < 3:
        return []

    variants: list[str] = []

    def add(candidate: str) -> None:
        cleaned = " ".join(candidate.strip(" -/,").split())
        if len(cleaned) >= 3 and cleaned.casefold() not in {
            v.casefold() for v in variants
        }:
            variants.append(cleaned)

    add(base)

    no_hyphen = base.replace("-", " ")
    add(no_hyphen)

    stripped_strength = TRAILING_STRENGTH_RE.sub("", no_hyphen).strip()
    add(stripped_strength)

    if stripped_strength:
        stripped_release = TRAILING_RELEASE_RE.sub(
            "", stripped_strength
        ).strip()
        add(stripped_release)

    # Ưu tiên biến thể đồng nghĩa không kèm hàm lượng trước để khớp tốt với openFDA/Canada
    synonym_sources = [s for s in (stripped_strength, no_hyphen, base) if s]
    for variant in synonym_sources:
        tokens = variant.split()
        replaced = False
        new_tokens = []
        for token in tokens:
            syn = SYNONYM_MAP.get(fold_text(token))
            if syn:
                new_tokens.append(syn)
                replaced = True
            else:
                new_tokens.append(token)
        if replaced:
            add(" ".join(new_tokens))

    return variants[:3]


async def request_json_with_retry(
    client: httpx.AsyncClient,
    method: str,
    url: str,
    *,
    params: dict[str, Any] | None = None,
    json_body: dict[str, Any] | None = None,
    max_retries: int = 2,
) -> Any:
    """Gọi HTTP kèm retry với exponential backoff cho lỗi mạng / 5xx / 429 tạm thời."""
    headers = {"Accept": "application/json"}
    if json_body is not None:
        headers["Content-Type"] = "application/json"

    for attempt in range(max_retries + 1):
        try:
            response = await client.request(
                method,
                url,
                params=params,
                json=json_body,
                headers=headers,
            )
            if (
                response.status_code in RETRYABLE_STATUS_CODES
                and attempt < max_retries
            ):
                await asyncio.sleep(0.25 * (2**attempt))
                continue

            response.raise_for_status()
            return response.json()

        except (
            httpx.TimeoutException,
            httpx.ConnectError,
            httpx.RemoteProtocolError,
            httpx.ReadError,
        ):
            if attempt >= max_retries:
                raise
            await asyncio.sleep(0.25 * (2**attempt))

    raise RuntimeError("Unreachable retry state")


async def get_json(client: httpx.AsyncClient, url: str, params: dict[str, Any]) -> Any:
    return await request_json_with_retry(
        client,
        "GET",
        url,
        params=params,
    )


async def post_json(
    client: httpx.AsyncClient,
    url: str,
    payload: dict[str, Any],
) -> Any:
    return await request_json_with_retry(
        client,
        "POST",
        url,
        json_body=payload,
    )


def _extract_dav_strength(ham_luong_raw: str, hoat_chat: str) -> str:
    if ham_luong_raw:
        return normalize_strength_display(ham_luong_raw)

    if not hoat_chat:
        return ""

    tokens = []
    for match in STRENGTH_TOKEN_RE.finditer(hoat_chat):
        formatted = normalize_strength_display(match.group(0))
        if formatted:
            tokens.append(formatted)

    if not tokens:
        return ""

    if len(tokens) == 1 and not any(
        sep in hoat_chat for sep in (",", ";", "+")
    ):
        return tokens[0]

    return " + ".join(tokens)


def _format_dav_manufacturer(product: dict[str, Any]) -> str:
    sx = (
        product.get("congTySanXuat")
        if isinstance(product.get("congTySanXuat"), dict)
        else {}
    )
    dk = (
        product.get("congTyDangKy")
        if isinstance(product.get("congTyDangKy"), dict)
        else {}
    )

    name = (
        text_value(sx.get("tenCongTySanXuat"))
        or text_value(product.get("tenCongTySanXuat"))
        or text_value(dk.get("tenCongTyDangKy"))
        or text_value(product.get("tenCongTyDangKy"))
    )
    country = (
        text_value(sx.get("nuocSanXuat"))
        or text_value(product.get("nuocSanXuat"))
        or text_value(dk.get("nuocDangKy"))
    )

    if name and country and country.casefold() not in name.casefold():
        return f"{name} ({country})"
    return name


def _parse_dav_items(payload: Any) -> list[MedicineCandidate]:
    if not isinstance(payload, dict):
        return []

    result = payload.get("result")
    if not isinstance(result, dict):
        return []

    items: list[MedicineCandidate] = []
    for product in list_or_empty(result.get("items")):
        if not isinstance(product, dict):
            continue

        if product.get("isDeleted") is True:
            continue
        if product.get("isActive") is False:
            continue

        name = text_value(product.get("tenThuoc"))
        if not name:
            continue

        reg_number = text_value(product.get("soDangKy"))
        raw_id = text_value(product.get("id"))
        if not reg_number and not raw_id:
            continue

        co_ban = (
            product.get("thongTinThuocCoBan")
            if isinstance(product.get("thongTinThuocCoBan"), dict)
            else {}
        )

        hoat_chat = text_value(co_ban.get("hoatChatChinh")) or text_value(
            product.get("hoatChatChinh")
        )
        ham_luong_raw = text_value(co_ban.get("hamLuong")) or text_value(
            product.get("hamLuong")
        )
        dang_bao_che = text_value(co_ban.get("dangBaoChe")) or text_value(
            product.get("dangBaoChe")
        )

        items.append(
            MedicineCandidate(
                id=f"VN:{reg_number or raw_id}",
                name=name,
                strength=_extract_dav_strength(ham_luong_raw, hoat_chat),
                dosage_form=dang_bao_che,
                registration_number=reg_number,
                active_ingredient=hoat_chat,
                manufacturer=_format_dav_manufacturer(product),
                source=SOURCE_VIETNAM,
            )
        )

    return items


async def search_vietnam(
    client: httpx.AsyncClient,
    query: str,
) -> list[MedicineCandidate]:
    """Tra cứu dữ liệu thuốc chính thống từ Cục Quản lý Dược - Bộ Y tế Việt Nam."""
    variants = build_query_variants(query)
    if not variants:
        return []

    for variant in variants:
        payload = await post_json(
            client,
            VIETNAM_DAV_URL,
            {
                "filterText": variant,
                "skipCount": 0,
                "maxResultCount": MAX_RESULTS_PER_SOURCE,
                "KichHoat": True,
                "SoDangKyThuoc": {},
            },
        )
        items = _parse_dav_items(payload)
        if items:
            return items

    if VN_REG_NUMBER_RE.match(query.strip()):
        payload = await post_json(
            client,
            VIETNAM_DAV_URL,
            {
                "filterText": "",
                "skipCount": 0,
                "maxResultCount": MAX_RESULTS_PER_SOURCE,
                "KichHoat": True,
                "SoDangKyThuoc": {"SoDangKy": query.strip()},
            },
        )
        return _parse_dav_items(payload)

    return []


def _format_fda_ingredient_strength(raw: str, dosage_form: str) -> str:
    cleaned = text_value(raw)
    if not cleaned:
        return ""
    form = dosage_form.upper()
    if "TABLET" in form or "CAPSULE" in form:
        cleaned = re.sub(r"\s*/\s*1\s*$", "", cleaned)
    return normalize_strength_display(cleaned)


async def search_usa(
    client: httpx.AsyncClient,
    query: str,
) -> list[MedicineCandidate]:
    """Tra cứu openFDA theo tên thương mại, tên chung và hoạt chất."""
    variants = build_query_variants(query)
    if not variants:
        return []

    payload: dict[str, Any] = {}
    for variant in variants:
        escaped = variant.replace("\\", "\\\\").replace('"', '\\"')
        search_expr = (
            f'(brand_name:"{escaped}" OR generic_name:"{escaped}" '
            f'OR active_ingredients.name:"{escaped}")'
        )
        try:
            candidate_payload = await get_json(
                client,
                FDA_URL,
                {
                    "search": search_expr,
                    "limit": MAX_RESULTS_PER_SOURCE,
                },
            )
            if isinstance(candidate_payload, dict) and list_or_empty(
                candidate_payload.get("results")
            ):
                payload = candidate_payload
                break
        except httpx.HTTPStatusError as error:
            if error.response.status_code in {400, 404}:
                continue
            raise

    results = payload.get("results", [])
    items: list[MedicineCandidate] = []

    for product in list_or_empty(results):
        if not isinstance(product, dict):
            continue

        name = text_value(product.get("brand_name")) or text_value(
            product.get("generic_name")
        )
        ndc = text_value(product.get("product_ndc"))

        if not name or not ndc:
            continue

        dosage_form = text_value(product.get("dosage_form"))
        ingredients = list_or_empty(product.get("active_ingredients"))

        ingredient_names = [
            text_value(x.get("name"))
            for x in ingredients
            if isinstance(x, dict) and text_value(x.get("name"))
        ]

        strength = ""
        if len(ingredients) == 1 and isinstance(ingredients[0], dict):
            strength = _format_fda_ingredient_strength(
                text_value(ingredients[0].get("strength")),
                dosage_form,
            )
        elif len(ingredients) > 1:
            multi_parts = [
                _format_fda_ingredient_strength(
                    text_value(ing.get("strength")),
                    dosage_form,
                )
                for ing in ingredients
                if isinstance(ing, dict)
            ]
            multi_parts = [part for part in multi_parts if part]
            if multi_parts:
                strength = " / ".join(multi_parts)

        items.append(
            MedicineCandidate(
                id=f"US:{ndc}",
                name=name,
                strength=strength,
                dosage_form=dosage_form,
                registration_number=ndc,
                active_ingredient=", ".join(ingredient_names),
                manufacturer=text_value(product.get("labeler_name")),
                source=SOURCE_USA,
            )
        )

    return items


async def canada_details(
    client: httpx.AsyncClient,
    product: dict[str, Any],
) -> MedicineCandidate | None:
    drug_code = product.get("drug_code")
    if drug_code is None:
        return None

    code = text_value(drug_code)
    if not code:
        return None

    cached = _canada_detail_cache.get(code)
    if isinstance(cached, MedicineCandidate):
        return cached

    async def fetch(path: str) -> list[Any]:
        try:
            data = await get_json(
                client,
                f"{CANADA_URL}/{path}/",
                {
                    "id": code,
                    "lang": "en",
                    "type": "json",
                },
            )
            return list_or_empty(data)
        except (httpx.HTTPError, ValueError):
            logger.warning("Health Canada detail unavailable: %s", path)
            return []

    ingredients_raw, forms_raw = await asyncio.gather(
        fetch("activeingredient"),
        fetch("form"),
    )

    ingredients = [x for x in ingredients_raw if isinstance(x, dict)]
    forms = [
        text_value(x.get("pharmaceutical_form_name"))
        for x in forms_raw
        if isinstance(x, dict)
        and text_value(x.get("pharmaceutical_form_name"))
    ]

    ingredient_names = [
        text_value(x.get("ingredient_name"))
        for x in ingredients
        if text_value(x.get("ingredient_name"))
    ]

    strength = ""
    if len(ingredients) == 1:
        ing = ingredients[0]
        raw_amount = text_value(ing.get("strength"))
        raw_unit = text_value(ing.get("strength_unit"))
        dosage_value = text_value(ing.get("dosage_value"))
        dosage_unit = text_value(ing.get("dosage_unit"))
        base_strength = f"{raw_amount} {raw_unit}".strip()

        if not dosage_value and not dosage_unit:
            strength = simple_strength(base_strength)
        elif base_strength and dosage_unit:
            denom = f"{dosage_value} {dosage_unit}".strip()
            strength = normalize_strength_display(f"{base_strength}/{denom}")
    elif len(ingredients) > 1:
        parts = []
        for ing in ingredients:
            raw_amount = text_value(ing.get("strength"))
            raw_unit = text_value(ing.get("strength_unit"))
            part = simple_strength(f"{raw_amount} {raw_unit}".strip())
            if part:
                parts.append(part)
        if parts:
            strength = " / ".join(parts)

    name = text_value(product.get("brand_name"))
    if not name:
        return None

    din = text_value(product.get("drug_identification_number"))
    candidate = MedicineCandidate(
        id=f"CA:{code}",
        name=name,
        strength=strength,
        dosage_form=" / ".join(dict.fromkeys(forms)),
        registration_number=din,
        active_ingredient=", ".join(ingredient_names),
        manufacturer=text_value(product.get("company_name")),
        source=SOURCE_CANADA,
    )
    _canada_detail_cache.set(code, candidate)
    return candidate


async def search_canada(
    client: httpx.AsyncClient,
    query: str,
) -> list[MedicineCandidate]:
    """Tra cứu Health Canada DPD theo tên thương mại và hoạt chất, có khử trùng trước khi lấy chi tiết."""
    variants = build_query_variants(query)
    if not variants:
        return []

    matching: list[dict[str, Any]] = []
    seen_codes: set[str] = set()
    seen_name_company: set[tuple[str, str]] = set()

    def add_product(product: dict[str, Any]) -> None:
        code = text_value(product.get("drug_code"))
        name = text_value(product.get("brand_name"))
        company = text_value(product.get("company_name"))
        if not code or not name or code in seen_codes:
            return
        dedupe_key = (fold_text(name), fold_text(company))
        if dedupe_key in seen_name_company:
            return
        seen_codes.add(code)
        seen_name_company.add(dedupe_key)
        matching.append(product)

    for variant in variants:
        try:
            products = await get_json(
                client,
                f"{CANADA_URL}/drugproduct/",
                {
                    "brandname": variant,
                    "lang": "en",
                    "type": "json",
                },
            )
        except httpx.HTTPStatusError as error:
            if error.response.status_code == 404:
                products = []
            else:
                raise

        v_fold = fold_text(variant)
        for product in list_or_empty(products):
            if not isinstance(product, dict):
                continue
            brand = text_value(product.get("brand_name"))
            if v_fold in fold_text(brand):
                add_product(product)
            if len(matching) >= MAX_CANADA_DETAIL_FETCHES:
                break
        if matching:
            break

    # Fallback tìm theo hoạt chất nếu tìm theo brandname ra quá ít kết quả
    if len(matching) < 3:
        ingredient_query = variants[-1]
        try:
            ing_rows = await get_json(
                client,
                f"{CANADA_URL}/activeingredient/",
                {
                    "ingredientname": ingredient_query,
                    "lang": "en",
                    "type": "json",
                },
            )
        except httpx.HTTPStatusError as error:
            if error.response.status_code == 404:
                ing_rows = []
            else:
                raise

        ing_codes: list[str] = []
        for row in list_or_empty(ing_rows):
            if not isinstance(row, dict):
                continue
            code = text_value(row.get("drug_code"))
            if code and code not in seen_codes and code not in ing_codes:
                ing_codes.append(code)
            if len(matching) + len(ing_codes) >= MAX_CANADA_DETAIL_FETCHES:
                break

        if ing_codes:
            semaphore = asyncio.Semaphore(4)

            async def fetch_product(code: str) -> Any:
                async with semaphore:
                    try:
                        return await get_json(
                            client,
                            f"{CANADA_URL}/drugproduct/",
                            {"id": code, "lang": "en", "type": "json"},
                        )
                    except (httpx.HTTPError, ValueError):
                        return None

            fetched = await asyncio.gather(
                *[fetch_product(code) for code in ing_codes]
            )
            for res in fetched:
                if isinstance(res, dict):
                    add_product(res)
                elif isinstance(res, list):
                    for item in res:
                        if isinstance(item, dict):
                            add_product(item)

    target_products = matching[:MAX_CANADA_DETAIL_FETCHES]
    if not target_products:
        return []

    semaphore = asyncio.Semaphore(4)

    async def controlled_detail(product: dict[str, Any]) -> MedicineCandidate | None:
        async with semaphore:
            return await canada_details(client, product)

    details = await asyncio.gather(
        *[controlled_detail(product) for product in target_products]
    )
    return [item for item in details if item is not None]


def _completeness_score(item: MedicineCandidate) -> int:
    return sum(
        1
        for field in (
            item.strength,
            item.dosage_form,
            item.active_ingredient,
            item.registration_number,
            item.manufacturer,
        )
        if text_value(field)
    )


def deduplicate_candidates(
    items: list[MedicineCandidate],
) -> list[MedicineCandidate]:
    """Khử trùng lặp bản ghi cùng nguồn nhưng khác mã đóng gói NDC/DIN, giữ hàm lượng khác nhau."""
    by_id: dict[str, MedicineCandidate] = {}
    for item in items:
        if item.id not in by_id or _completeness_score(item) > _completeness_score(
            by_id[item.id]
        ):
            by_id[item.id] = item

    best_by_signature: dict[tuple[str, str, str, str, str], MedicineCandidate] = {}
    order: list[tuple[str, str, str, str, str]] = []

    for item in by_id.values():
        signature = (
            item.source,
            fold_text(item.name),
            fold_text(item.active_ingredient),
            fold_text(item.strength),
            fold_text(item.dosage_form),
        )
        existing = best_by_signature.get(signature)
        if existing is None:
            best_by_signature[signature] = item
            order.append(signature)
        elif _completeness_score(item) > _completeness_score(existing):
            best_by_signature[signature] = item

    return [best_by_signature[sig] for sig in order]


def rank_candidates(
    items: list[MedicineCandidate],
    query: str,
) -> list[MedicineCandidate]:
    """Xếp hạng ưu tiên: độ khớp tên/hoạt chất/SĐK -> nguồn Việt Nam/Mỹ/Canada -> mức đầy đủ."""
    q_fold = fold_text(query)
    q_stripped = fold_text(TRAILING_STRENGTH_RE.sub("", query))

    def score(item: MedicineCandidate) -> tuple[int, int, int]:
        name_fold = fold_text(item.name)
        ingredient_fold = fold_text(item.active_ingredient)
        reg_fold = fold_text(item.registration_number)

        if q_fold and name_fold == q_fold:
            match_score = 6
        elif q_stripped and name_fold == q_stripped:
            match_score = 5
        elif q_fold and name_fold.startswith(q_fold):
            match_score = 4
        elif q_fold and q_fold in name_fold:
            match_score = 3
        elif q_stripped and q_stripped in name_fold:
            match_score = 3
        elif q_fold and reg_fold == q_fold:
            match_score = 4
        elif q_fold and q_fold in ingredient_fold:
            match_score = 2
        elif q_stripped and q_stripped in ingredient_fold:
            match_score = 2
        else:
            match_score = 1

        source_score = SOURCE_PRIORITY.get(item.source, 0)
        return (match_score, source_score, _completeness_score(item))

    return sorted(items, key=score, reverse=True)


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

    cache_key = fold_text(query) or query.casefold()
    cached_items = _lookup_cache.get(cache_key)
    if isinstance(cached_items, list):
        return MedicineLookupResponse(items=cached_items)

    async def run_source(coro):
        return await asyncio.wait_for(coro, timeout=SOURCE_TIMEOUT_SECONDS)

    async with httpx.AsyncClient(
        timeout=httpx.Timeout(SOURCE_TIMEOUT_SECONDS, connect=4.0),
        follow_redirects=False,
    ) as client:
        results = await asyncio.gather(
            run_source(search_vietnam(client, query)),
            run_source(search_usa(client, query)),
            run_source(search_canada(client, query)),
            return_exceptions=True,
        )

    items: list[MedicineCandidate] = []
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

    unique = rank_candidates(deduplicate_candidates(items), query)

    # Chỉ lưu cache khi toàn bộ nguồn ổn định hoặc đã tìm thấy dữ liệu hợp lệ
    if failed_sources == 0 or unique:
        _lookup_cache.set(cache_key, unique)

    return MedicineLookupResponse(items=unique)
