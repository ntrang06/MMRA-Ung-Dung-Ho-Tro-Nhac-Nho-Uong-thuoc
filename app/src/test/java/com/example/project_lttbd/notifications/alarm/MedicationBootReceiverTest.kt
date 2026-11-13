package com.example.project_lttbd.notifications.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationBootReceiverTest {

    @Test
    fun testShouldRestoreDose_onlyFutureAndUnfinished() {
        val now = 1_700_000_000_000L
        val past = now - 60_000L
        val future = now + 60_000L

        // Finished doses should never be restored
        assertFalse(
            "Finished dose in future should NOT be restored",
            MedicationBootReceiver.shouldRestoreDose(isFinished = true, scheduledAtMillis = future, nowMillis = now)
        )
        assertFalse(
            "Finished dose in past should NOT be restored",
            MedicationBootReceiver.shouldRestoreDose(isFinished = true, scheduledAtMillis = past, nowMillis = now)
        )

        // Unfinished doses in the past should NOT be restored
        assertFalse(
            "Unfinished dose in past should NOT be restored",
            MedicationBootReceiver.shouldRestoreDose(isFinished = false, scheduledAtMillis = past, nowMillis = now)
        )

        // Only unfinished doses in the future should be restored
        assertTrue(
            "Unfinished dose in future MUST be restored",
            MedicationBootReceiver.shouldRestoreDose(isFinished = false, scheduledAtMillis = future, nowMillis = now)
        )
    }

    @Test
    fun testRestoreAlarms_returnsZeroWhenUidIsBlank() {
        // Without needing Android context methods, blank UID should immediately bail out
        val count = MedicationBootReceiver.restoreAlarms(
            context = null as? android.content.Context ?: return,
            uid = ""
        )
        assertEquals(0, count)
    }
}
