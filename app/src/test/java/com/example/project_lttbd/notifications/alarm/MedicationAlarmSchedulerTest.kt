package com.example.project_lttbd.notifications.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationAlarmSchedulerTest {

    @Test
    fun testRequestCodeGeneration_isStableAndDistinct() {
        val doseId1 = "dose_abc_123"
        val doseId2 = "dose_xyz_789"

        val code1 = MedicationAlarmScheduler.requestCodeForDose(doseId1)
        val code1Repeat = MedicationAlarmScheduler.requestCodeForDose(doseId1)
        val code2 = MedicationAlarmScheduler.requestCodeForDose(doseId2)

        assertEquals("Same doseId must produce identical requestCode", code1, code1Repeat)
        assertNotEquals("Distinct doseIds must produce distinct requestCodes", code1, code2)
    }

    @Test
    fun testShouldSchedule_ignoresPastTimes() {
        val now = System.currentTimeMillis()
        val past = now - 60000L
        val future = now + 60000L

        val shouldPast = MedicationAlarmScheduler.isValidScheduleTime(past, now)
        val shouldFuture = MedicationAlarmScheduler.isValidScheduleTime(future, now)

        assertEquals(false, shouldPast)
        assertEquals(true, shouldFuture)
    }
}
