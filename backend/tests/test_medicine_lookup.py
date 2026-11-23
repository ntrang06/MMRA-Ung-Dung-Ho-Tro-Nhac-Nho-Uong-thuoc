import unittest
from unittest.mock import AsyncMock, Mock, patch

import httpx
from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.api.deps import CurrentUser, get_current_user
from app.api.v1.endpoints import medicine_lookup


def make_client():
    app = FastAPI()
    app.include_router(
        medicine_lookup.router,
        prefix="/api/v1",
    )
    app.dependency_overrides[get_current_user] = (
        lambda: CurrentUser(uid="user-huy")
    )
    return TestClient(app)


class MedicineLookupTests(unittest.TestCase):
    def setUp(self):
        medicine_lookup._lookup_cache.clear()
        medicine_lookup._canada_detail_cache.clear()

    def test_vietnam_dav_normalization_single_multi_and_missing_fields(self):
        dav_payload = {
            "result": {
                "totalCount": 3,
                "items": [
                    {
                        "id": 65443,
                        "tenThuoc": "Panfor PR-750",
                        "soDangKy": "890110197726",
                        "isHetHan": False,
                        "isActive": True,
                        "isDeleted": False,
                        "thongTinThuocCoBan": {
                            "hoatChatChinh": "Metformin Hydrochloride",
                            "hamLuong": "750mg",
                            "dangBaoChe": "Viên nén phóng thích kéo dài",
                        },
                        "congTySanXuat": {
                            "tenCongTySanXuat": "Lee Pharma Limited",
                            "nuocSanXuat": "India",
                        },
                    },
                    {
                        "id": 65444,
                        "tenThuoc": "Vildagliptin/Metformin Hydrochloride Tablets 50/850",
                        "soDangKy": "890110191226",
                        "isActive": True,
                        "isDeleted": False,
                        "thongTinThuocCoBan": {
                            "hoatChatChinh": "Vildagliptin 50mg, Metformin hydrochlorid 850mg",
                            "hamLuong": "",
                            "dangBaoChe": "Viên nén bao phim",
                        },
                        "congTySanXuat": {
                            "tenCongTySanXuat": "Lee Pharma Limited",
                            "nuocSanXuat": "",
                        },
                    },
                    {
                        "id": 65445,
                        "tenThuoc": "Metformin Unknown Spec",
                        "soDangKy": "VD-99999-24",
                        "thongTinThuocCoBan": {
                            "hoatChatChinh": "Metformin",
                            "hamLuong": None,
                            "dangBaoChe": None,
                        },
                        "congTyDangKy": {
                            "tenCongTyDangKy": "Cong ty Duoc A",
                            "nuocDangKy": "Việt Nam",
                        },
                    },
                ],
            },
            "success": True,
        }

        async def fake_post(client, url, payload):
            return dav_payload

        with (
            patch.object(medicine_lookup, "post_json", side_effect=fake_post),
            patch.object(medicine_lookup, "search_usa", AsyncMock(return_value=[])),
            patch.object(medicine_lookup, "search_canada", AsyncMock(return_value=[])),
        ):
            with make_client() as client:
                response = client.get("/api/v1/medicine-lookup?q=Metformin")

        self.assertEqual(response.status_code, 200)
        items = response.json()["items"]
        self.assertEqual(len(items), 3)

        panfor = next(x for x in items if x["id"] == "VN:890110197726")
        self.assertEqual(panfor["name"], "Panfor PR-750")
        self.assertEqual(panfor["active_ingredient"], "Metformin Hydrochloride")
        self.assertEqual(panfor["strength"], "750 mg")
        self.assertEqual(panfor["dosage_form"], "Viên nén phóng thích kéo dài")
        self.assertEqual(panfor["registration_number"], "890110197726")
        self.assertEqual(panfor["manufacturer"], "Lee Pharma Limited (India)")
        self.assertEqual(panfor["source"], medicine_lookup.SOURCE_VIETNAM)

        combo = next(x for x in items if x["id"] == "VN:890110191226")
        self.assertEqual(combo["strength"], "50 mg + 850 mg")
        self.assertEqual(
            combo["active_ingredient"],
            "Vildagliptin 50mg, Metformin hydrochlorid 850mg",
        )

        incomplete = next(x for x in items if x["id"] == "VN:VD-99999-24")
        self.assertEqual(incomplete["strength"], "")
        self.assertEqual(incomplete["dosage_form"], "")
        self.assertEqual(incomplete["manufacturer"], "Cong ty Duoc A (Việt Nam)")

    def test_search_variants_cover_strength_hyphens_and_synonyms(self):
        variants_panfor = medicine_lookup.build_query_variants("Panfor PR-750")
        self.assertIn("Panfor PR-750", variants_panfor)
        self.assertIn("Panfor PR", variants_panfor)

        variants_paracetamol = medicine_lookup.build_query_variants("Paracetamol 500mg")
        self.assertIn("Paracetamol 500mg", variants_paracetamol)
        self.assertIn("Paracetamol", variants_paracetamol)
        self.assertIn("acetaminophen", [v.lower() for v in variants_paracetamol])

    def test_search_usa_tries_synonym_fallback_for_active_ingredient(self):
        seen_queries = []

        async def fake_get_json(client, url, params):
            seen_queries.append(params["search"])
            if "acetaminophen" in params["search"].lower():
                return {
                    "results": [
                        {
                            "brand_name": "Tylenol",
                            "product_ndc": "50580-449",
                            "dosage_form": "TABLET",
                            "labeler_name": "Johnson & Johnson",
                            "active_ingredients": [
                                {"name": "ACETAMINOPHEN", "strength": "500 mg/1"}
                            ],
                        }
                    ]
                }
            return {"results": []}

        with (
            patch.object(medicine_lookup, "search_vietnam", AsyncMock(return_value=[])),
            patch.object(medicine_lookup, "get_json", side_effect=fake_get_json),
            patch.object(medicine_lookup, "search_canada", AsyncMock(return_value=[])),
        ):
            with make_client() as client:
                response = client.get("/api/v1/medicine-lookup?q=Paracetamol%20500mg")

        self.assertEqual(response.status_code, 200)
        items = response.json()["items"]
        self.assertEqual(len(items), 1)
        self.assertEqual(items[0]["name"], "Tylenol")
        self.assertEqual(items[0]["strength"], "500 mg")
        self.assertEqual(items[0]["manufacturer"], "Johnson & Johnson")
        self.assertEqual(len(seen_queries), 3)

    def test_deduplicate_and_preserve_distinct_strengths_and_sources(self):
        fda_payload = {
            "results": [
                {
                    "brand_name": "Glucophage",
                    "product_ndc": "0087-6060",
                    "dosage_form": "TABLET, FILM COATED",
                    "labeler_name": "",
                    "active_ingredients": [
                        {"name": "METFORMIN HYDROCHLORIDE", "strength": "500 mg/1"}
                    ],
                },
                {
                    "brand_name": "Glucophage",
                    "product_ndc": "0087-6061",
                    "dosage_form": "TABLET, FILM COATED",
                    "labeler_name": "Bristol-Myers Squibb",
                    "active_ingredients": [
                        {"name": "METFORMIN HYDROCHLORIDE", "strength": "500 mg/1"}
                    ],
                },
                {
                    "brand_name": "Glucophage",
                    "product_ndc": "0087-6070",
                    "dosage_form": "TABLET, FILM COATED",
                    "labeler_name": "Bristol-Myers Squibb",
                    "active_ingredients": [
                        {"name": "METFORMIN HYDROCHLORIDE", "strength": "850 mg/1"}
                    ],
                },
            ]
        }

        ca_item_1 = medicine_lookup.MedicineCandidate(
            id="CA:1001",
            name="APO-METFORMIN",
            strength="500 mg",
            dosage_form="Tablet",
            registration_number="02167781",
            active_ingredient="METFORMIN HYDROCHLORIDE",
            manufacturer="Apotex Inc.",
            source=medicine_lookup.SOURCE_CANADA,
        )
        ca_item_2 = medicine_lookup.MedicineCandidate(
            id="CA:1002",
            name="APO-METFORMIN",
            strength="500 mg",
            dosage_form="Tablet",
            registration_number="",
            active_ingredient="METFORMIN HYDROCHLORIDE",
            manufacturer="",
            source=medicine_lookup.SOURCE_CANADA,
        )
        vn_item = medicine_lookup.MedicineCandidate(
            id="VN:12345",
            name="Glucophage",
            strength="500 mg",
            dosage_form="Viên nén bao phim",
            registration_number="VN-12345",
            active_ingredient="Metformin hydrochloride",
            manufacturer="Merck",
            source=medicine_lookup.SOURCE_VIETNAM,
        )

        with (
            patch.object(medicine_lookup, "search_vietnam", AsyncMock(return_value=[vn_item])),
            patch.object(medicine_lookup, "get_json", AsyncMock(return_value=fda_payload)),
            patch.object(
                medicine_lookup,
                "search_canada",
                AsyncMock(return_value=[ca_item_2, ca_item_1]),
            ),
        ):
            with make_client() as client:
                response = client.get("/api/v1/medicine-lookup?q=Glucophage")

        self.assertEqual(response.status_code, 200)
        items = response.json()["items"]

        # 1 VN Glucophage + 2 FDA Glucophage (500 mg deduped to fuller one, 850 mg kept) + 1 CA APO-METFORMIN
        self.assertEqual(len(items), 4)
        self.assertEqual(items[0]["source"], medicine_lookup.SOURCE_VIETNAM)
        us_500 = next(
            x
            for x in items
            if x["source"] == medicine_lookup.SOURCE_USA and x["strength"] == "500 mg"
        )
        self.assertEqual(us_500["manufacturer"], "Bristol-Myers Squibb")
        self.assertTrue(
            any(
                x["source"] == medicine_lookup.SOURCE_USA and x["strength"] == "850 mg"
                for x in items
            )
        )
        ca = next(x for x in items if x["source"] == medicine_lookup.SOURCE_CANADA)
        self.assertEqual(ca["manufacturer"], "Apotex Inc.")

    def test_ttl_cache_avoids_duplicate_requests_and_expires(self):
        vn_mock = AsyncMock(
            return_value=[
                medicine_lookup.MedicineCandidate(
                    id="VN:888",
                    name="Metformin Stella",
                    strength="500 mg",
                    dosage_form="Viên nén",
                    registration_number="VD-888",
                    active_ingredient="Metformin",
                    source=medicine_lookup.SOURCE_VIETNAM,
                )
            ]
        )
        usa_mock = AsyncMock(return_value=[])
        ca_mock = AsyncMock(return_value=[])

        with (
            patch.object(medicine_lookup, "search_vietnam", vn_mock),
            patch.object(medicine_lookup, "search_usa", usa_mock),
            patch.object(medicine_lookup, "search_canada", ca_mock),
        ):
            with make_client() as client:
                r1 = client.get("/api/v1/medicine-lookup?q=Metformin")
                r2 = client.get("/api/v1/medicine-lookup?q=  metformin  ")

        self.assertEqual(r1.status_code, 200)
        self.assertEqual(r2.status_code, 200)
        self.assertEqual(r1.json(), r2.json())
        self.assertEqual(vn_mock.await_count, 1)

        ttl = medicine_lookup.TTLCache(ttl_seconds=10.0, max_size=4)
        ttl.set("k1", ["val"], now=100.0)
        self.assertEqual(ttl.get("k1", now=105.0), ["val"])
        self.assertIsNone(ttl.get("k1", now=110.0))

    def test_retry_and_partial_source_timeout_fallback(self):
        # Test partial fallback: DAV succeeds while FDA times out and Canada returns 503
        vn_item = medicine_lookup.MedicineCandidate(
            id="VN:777",
            name="Panadol Extra",
            strength="500 mg + 65 mg",
            dosage_form="Viên nén bao phim",
            registration_number="VD-777",
            active_ingredient="Paracetamol 500mg, Caffeine 65mg",
            source=medicine_lookup.SOURCE_VIETNAM,
        )

        with (
            patch.object(medicine_lookup, "search_vietnam", AsyncMock(return_value=[vn_item])),
            patch.object(medicine_lookup, "search_usa", AsyncMock(side_effect=TimeoutError())),
            patch.object(medicine_lookup, "search_canada", AsyncMock(side_effect=RuntimeError("503"))),
        ):
            with make_client() as client:
                ok_resp = client.get("/api/v1/medicine-lookup?q=Panadol")

        self.assertEqual(ok_resp.status_code, 200)
        self.assertEqual(len(ok_resp.json()["items"]), 1)

        # Verify all-sources-fail returns 503 and does not pollute cache
        medicine_lookup._lookup_cache.clear()
        with (
            patch.object(medicine_lookup, "search_vietnam", AsyncMock(side_effect=TimeoutError())),
            patch.object(medicine_lookup, "search_usa", AsyncMock(side_effect=TimeoutError())),
            patch.object(medicine_lookup, "search_canada", AsyncMock(side_effect=TimeoutError())),
        ):
            with make_client() as client:
                err_resp = client.get("/api/v1/medicine-lookup?q=Panadol")

        self.assertEqual(err_resp.status_code, 503)

    def test_retry_helper_retries_transient_503_and_timeout(self):
        import asyncio

        request = httpx.Request("GET", "https://example.test")
        calls = 0

        class FakeClient:
            async def request(self, method, url, **kwargs):
                nonlocal calls
                calls += 1
                if calls == 1:
                    raise httpx.ReadTimeout("slow", request=request)
                if calls == 2:
                    return httpx.Response(503, request=request)
                return httpx.Response(200, json={"ok": True}, request=request)

        with patch.object(medicine_lookup.asyncio, "sleep", AsyncMock()):
            data = asyncio.run(
                medicine_lookup.request_json_with_retry(
                    FakeClient(),
                    "GET",
                    "https://example.test",
                )
            )
        self.assertEqual(calls, 3)
        self.assertEqual(data, {"ok": True})

    def test_empty_not_found_and_short_query(self):
        with (
            patch.object(medicine_lookup, "search_vietnam", AsyncMock(return_value=[])),
            patch.object(medicine_lookup, "search_usa", AsyncMock(return_value=[])),
            patch.object(medicine_lookup, "search_canada", AsyncMock(return_value=[])),
        ):
            with make_client() as client:
                empty_resp = client.get("/api/v1/medicine-lookup?q=KhongTonTaiThuoc")
                short_resp = client.get("/api/v1/medicine-lookup?q=ab")
                blank_resp = client.get("/api/v1/medicine-lookup?q=%20%20%20")

        self.assertEqual(empty_resp.status_code, 200)
        self.assertEqual(empty_resp.json(), {"items": []})
        self.assertEqual(short_resp.status_code, 422)
        self.assertEqual(blank_resp.status_code, 422)
