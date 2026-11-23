package com.example.project_lttbd.data.scan

import com.example.project_lttbd.data.remote.LookupTtlCache
import com.example.project_lttbd.data.remote.MedicineLookupCandidate
import com.example.project_lttbd.data.remote.StrengthParts
import com.example.project_lttbd.data.remote.hasMultipleActiveIngredients
import com.example.project_lttbd.data.remote.isSafeSingleStrengthForAutoFill
import com.example.project_lttbd.data.remote.isSpecialReleaseMedicine
import com.example.project_lttbd.data.remote.mapCatalogDosageForm
import com.example.project_lttbd.data.remote.parseCatalogStrength
import com.example.project_lttbd.ui.screens.scan.buildMedicineNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicineOcrAndLookupTest {

    @Test
    fun parseOcr_fixesCommonStrengthAndUnitArtifacts_withoutCorruptingNames() {
        val result1 = MedicineOcrParser.parse(
            listOf(
                MedicineOcrLine("Glucophage XR", height = 42),
                MedicineOcrLine("Metformin hydrochloride 5OOmg", height = 24),
                MedicineOcrLine("Viên nén phóng thích kéo dài", height = 20)
            )
        )
        assertEquals("Glucophage XR", result1.medicineName)
        assertEquals("500", result1.strength)
        assertEquals("mg", result1.strengthUnit)
        assertEquals("Viên nén", result1.dosageForm)
        assertTrue(result1.isSpecialRelease)
        assertFalse(result1.isAmbiguousStrength)

        val result2 = MedicineOcrParser.parse(
            listOf(
                MedicineOcrLine("Amlor", height = 40),
                MedicineOcrLine("Amlodipine l0rng", height = 22)
            )
        )
        assertEquals("Amlor", result2.medicineName)
        assertEquals("10", result2.strength)
        assertEquals("mg", result2.strengthUnit)

        val result3 = MedicineOcrParser.parse(
            listOf(
                MedicineOcrLine("Vitamin D3", height = 38),
                MedicineOcrLine("Cholecalciferol 1000 1U", height = 22)
            )
        )
        assertEquals("Vitamin D3", result3.medicineName)
        assertEquals("1000", result3.strength)
        assertEquals("IU", result3.strengthUnit)

        val result4 = MedicineOcrParser.parse(
            listOf(
                MedicineOcrLine("Panadol", height = 40),
                MedicineOcrLine("Paracetamol 500mq", height = 20)
            )
        )
        assertEquals("500", result4.strength)
        assertEquals("mg", result4.strengthUnit)

        // Chữ 'O'/'l' trong tên thuốc không bị sửa nhầm thành số '0'/'1'
        assertEquals("APO-METFORMIN", MedicineOcrParser.normalizeOcrStrengthArtifacts("APO-METFORMIN"))
        assertEquals("500 mg", MedicineOcrParser.normalizeOcrStrengthArtifacts("5OOmg"))
        assertEquals("0.5 g", MedicineOcrParser.normalizeOcrStrengthArtifacts("O.5g"))
    }

    @Test
    fun parseOcr_keepsMedicineName_butRefusesToGuessStrength_forMultiIngredientOrRatio() {
        val combo = MedicineOcrParser.parse(
            listOf(
                MedicineOcrLine("WHO-GMP", height = 60),
                MedicineOcrLine("SĐK: VD-12345-19", height = 50),
                MedicineOcrLine("Panadol Extra", height = 45),
                MedicineOcrLine("Paracetamol 500mg + Caffeine 65mg", height = 22),
                MedicineOcrLine("Hộp 10 vỉ x 10 viên", height = 18)
            )
        )

        assertEquals("Panadol Extra", combo.medicineName)
        assertEquals("", combo.strength)
        assertTrue(combo.isAmbiguousStrength)
        assertTrue(combo.detectedStrengths.containsAll(listOf("500 mg", "65 mg")))
        assertFalse(combo.candidateNames.contains("WHO-GMP"))
        assertFalse(combo.candidateNames.any { it.contains("VD-12345") })
        assertFalse(combo.candidateNames.any { it.contains("Hộp 10") })

        val syrupRatio = MedicineOcrParser.parse(
            listOf(
                MedicineOcrLine("Augmentin 125mg/5ml", height = 40),
                MedicineOcrLine("Hỗn dịch uống", height = 20)
            )
        )
        assertEquals("Augmentin", syrupRatio.medicineName)
        assertEquals("", syrupRatio.strength)
        assertTrue(syrupRatio.isAmbiguousStrength)
    }

    @Test
    fun autoFillGuard_rejectsMultiIngredientAndComplexRatios_andDetectsSpecialRelease() {
        val vnSingle = MedicineLookupCandidate(
            id = "VN:890110197726",
            name = "Panfor PR-750",
            strength = "750 mg",
            dosage_form = "Viên nén phóng thích kéo dài",
            registration_number = "890110197726",
            active_ingredient = "Metformin Hydrochloride",
            manufacturer = "Lee Pharma Limited (India)",
            source = "Việt Nam - Cục Quản lý Dược"
        )
        assertEquals(StrengthParts("750", "mg"), isSafeSingleStrengthForAutoFill(vnSingle))
        assertTrue(isSpecialReleaseMedicine(vnSingle.name, vnSingle.dosage_form))
        assertEquals("Viên nén", mapCatalogDosageForm(vnSingle.dosage_form))

        val vnCombo = MedicineLookupCandidate(
            id = "VN:890110191226",
            name = "Vildagliptin/Metformin Hydrochloride Tablets 50/850",
            strength = "50 mg + 850 mg",
            dosage_form = "Viên nén bao phim",
            registration_number = "890110191226",
            active_ingredient = "Vildagliptin 50mg, Metformin hydrochlorid 850mg",
            manufacturer = "Lee Pharma Limited (India)",
            source = "Việt Nam - Cục Quản lý Dược"
        )
        assertTrue(hasMultipleActiveIngredients(vnCombo.active_ingredient))
        assertNull(isSafeSingleStrengthForAutoFill(vnCombo))
        assertFalse(isSpecialReleaseMedicine(vnCombo.name, vnCombo.dosage_form))

        val usRatio = MedicineLookupCandidate(
            id = "US:0093-1234",
            name = "Amoxicillin Suspension",
            strength = "125 mg/5 mL",
            dosage_form = "POWDER, FOR SUSPENSION",
            active_ingredient = "AMOXICILLIN",
            source = "Mỹ - openFDA"
        )
        assertNull(parseCatalogStrength(usRatio.strength))
        assertNull(isSafeSingleStrengthForAutoFill(usRatio))
        assertEquals("Dung dịch", mapCatalogDosageForm(usRatio.dosage_form))

        val note = buildMedicineNote("", vnCombo)
        assertTrue(note.contains("Vildagliptin 50mg"))
        assertTrue(note.contains("50 mg + 850 mg"))

        val prNote = buildMedicineNote("", vnSingle)
        assertTrue(prNote.contains("phóng thích kéo dài"))
        assertTrue(prNote.contains("giải phóng đặc biệt"))
    }

    @Test
    fun specialReleaseDetection_coversVietnameseAndEnglishKeywords() {
        assertTrue(isSpecialReleaseMedicine("Metformin", "Viên nén phóng thích kéo dài"))
        assertTrue(isSpecialReleaseMedicine("Diamicron MR", "Viên nén giải phóng chậm"))
        assertTrue(isSpecialReleaseMedicine("Glucophage XR", "Extended-release tablet"))
        assertFalse(isSpecialReleaseMedicine("Metformin Stella", "Viên nén bao phim"))
    }

    @Test
    fun lookupTtlCache_normalizesKeysAndExpiresAfterTtl() {
        var now = 1_000L
        val cache = LookupTtlCache(
            ttlMillis = 5_000L,
            maxEntries = 2,
            nowProvider = { now }
        )

        val sample = listOf(
            MedicineLookupCandidate(
                id = "VN:1",
                name = "Panfor PR-750",
                source = "Việt Nam - Cục Quản lý Dược"
            )
        )
        cache.put("  Metformin  ", sample)
        assertEquals(sample, cache.get("metformin"))

        now = 6_000L
        assertNull(cache.get("metformin"))
    }
}
