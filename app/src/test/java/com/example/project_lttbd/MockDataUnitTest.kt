package com.example.project_lttbd

import com.example.project_lttbd.data.mock.MockAlertData
import com.example.project_lttbd.data.mock.MockFamilyData
import com.example.project_lttbd.data.mock.MockScanMedicine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockDataUnitTest {

    @Test
    fun testMockScanResults_areValid() {
        val results = MockScanMedicine.mockResults
        assertFalse("Mock scan results should not be empty", results.isEmpty())

        val defaultResult = MockScanMedicine.defaultResult
        assertNotNull(defaultResult)
        assertEquals("Metformin", defaultResult.medicineName)
        assertEquals("500 mg", defaultResult.dosage)
        assertEquals(92, defaultResult.matchScore)
        assertEquals("Metformin hydrochloride", defaultResult.activeIngredient)
        assertEquals("Mekophar", defaultResult.manufacturer)
        assertEquals("Hộp 3 vỉ × 10 viên", defaultResult.packaging)
    }

    @Test
    fun testMockFamilyMembers_areValid() {
        val members = MockFamilyData.mockMembers
        assertFalse("Mock family members should not be empty", members.isEmpty())

        val father = members.firstOrNull { it.relationship == "Bố" }
        assertNotNull("Father member should exist", father)
        assertTrue(father!!.doseSchedules.isNotEmpty())
        assertTrue(father.todayTotalCount >= father.todayTakenCount)
    }

    @Test
    fun testMockCaregiverAlerts_areValid() {
        val alerts = MockAlertData.mockAlerts
        assertFalse("Mock caregiver alerts should not be empty", alerts.isEmpty())

        val firstAlert = alerts.first()
        assertTrue(firstAlert.memberName.isNotBlank())
        assertTrue(firstAlert.medicineName.isNotBlank())
        assertTrue(firstAlert.minutesLate > 0)
    }
}
