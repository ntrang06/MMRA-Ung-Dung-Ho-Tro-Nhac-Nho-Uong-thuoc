package com.example.project_lttbd.notifications

import com.example.project_lttbd.ui.navigation.MMRADestinations
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPayloadParserTest {

    @Test
    fun testParseDestinationRoute() {
        val dataWithRoute = mapOf("destination" to MMRADestinations.SCHEDULE)
        val route1 = NotificationHelper.resolveDestination(dataWithRoute)
        assertEquals(MMRADestinations.SCHEDULE, route1)

        val dataEmpty = emptyMap<String, String>()
        val routeDefault = NotificationHelper.resolveDestination(dataEmpty)
        assertEquals(MMRADestinations.HOME, routeDefault)
    }

    @Test
    fun testExtractDoseId() {
        val data = mapOf("doseId" to "dose_morning_123")
        assertEquals("dose_morning_123", NotificationHelper.extractDoseId(data))
    }
}
