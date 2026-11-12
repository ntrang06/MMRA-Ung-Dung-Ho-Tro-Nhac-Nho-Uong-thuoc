package com.example.project_lttbd.notifications

import com.example.project_lttbd.ui.navigation.MMRADestinations
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationHelperRouteTest {

    @Test
    fun testResolveDestination_withDoseId() {
        val data = mapOf("doseId" to "sample_dose_999")
        val destination = NotificationHelper.resolveDestination(data)
        assertEquals("reminder/sample_dose_999?readOnly=false", destination)
    }

    @Test
    fun testResolveDestination_withoutDoseId_defaultsToHomeOrCustom() {
        val dataWithCustom = mapOf("destination" to MMRADestinations.FAMILY)
        assertEquals(MMRADestinations.FAMILY, NotificationHelper.resolveDestination(dataWithCustom))

        val emptyData = emptyMap<String, String>()
        assertEquals(MMRADestinations.HOME, NotificationHelper.resolveDestination(emptyData))
    }

    @Test
    fun testNotificationIdForDose_isPositiveAndConsistent() {
        val doseId = "dose_test_unique_id"
        val id1 = NotificationHelper.notificationIdForDose(doseId)
        val id2 = NotificationHelper.notificationIdForDose(doseId)
        assertEquals(id1, id2)
        org.junit.Assert.assertTrue(id1 >= 0)
    }
}
