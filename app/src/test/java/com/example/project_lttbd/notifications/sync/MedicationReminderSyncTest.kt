package com.example.project_lttbd.notifications.sync

import com.example.project_lttbd.data.remote.dto.DoseActionDto
import com.example.project_lttbd.data.remote.dto.DoseResponseDto
import com.example.project_lttbd.data.remote.dto.DoseStatusDto
import com.example.project_lttbd.data.remote.dto.DoseUnitDto
import com.example.project_lttbd.data.remote.dto.MedicineSnapshotDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationReminderSyncTest {

    private fun createDose(
        doseId: String,
        status: DoseStatusDto,
        scheduledAt: String = "2026-11-12T15:00:00+07:00",
        nextReminderAt: String? = null
    ): DoseResponseDto {
        return DoseResponseDto(
            doseId = doseId,
            scheduleId = "sched_1",
            scheduleVersion = 1,
            medicineId = "med_1",
            localDate = "2026-11-12",
            minuteOfDay = 900,
            timeZone = "Asia/Ho_Chi_Minh",
            scheduledAt = scheduledAt,
            doseQuantity = "1",
            doseUnit = DoseUnitDto.TABLET,
            mealInstruction = "Sau ăn",
            medicineSnapshot = MedicineSnapshotDto(
                medicineName = "Panadol",
                strength = "500",
                strengthUnit = "mg",
                dosageForm = "Viên nén"
            ),
            status = status,
            actionAt = null,
            nextReminderAt = nextReminderAt,
            createdAt = "2026-11-12T00:00:00Z",
            updatedAt = "2026-11-12T00:00:00Z"
        )
    }

    @Test
    fun testClassifyDosesForAlarmScheduling() {
        val pendingDose = createDose("dose_pending", DoseStatusDto.PENDING)
        val takenDose = createDose("dose_taken", DoseStatusDto.TAKEN)
        val skippedDose = createDose("dose_skipped", DoseStatusDto.SKIPPED)
        val cancelledDose = createDose("dose_cancelled", DoseStatusDto.CANCELLED)

        assertTrue(MedicationReminderSync.shouldScheduleDose(pendingDose))
        assertFalse(MedicationReminderSync.shouldScheduleDose(takenDose))
        assertFalse(MedicationReminderSync.shouldScheduleDose(skippedDose))
        assertFalse(MedicationReminderSync.shouldScheduleDose(cancelledDose))

        assertTrue(MedicationReminderSync.shouldCancelDose(takenDose))
        assertTrue(MedicationReminderSync.shouldCancelDose(skippedDose))
        assertTrue(MedicationReminderSync.shouldCancelDose(cancelledDose))
        assertFalse(MedicationReminderSync.shouldCancelDose(pendingDose))
    }

    @Test
    fun testParseIsoTriggerTime_handlesOffsetAndZ() {
        val isoWithOffset = "2026-11-12T15:00:00+07:00"
        val millis = MedicationReminderSync.parseIsoToMillis(isoWithOffset)
        assertTrue(millis > 0)

        val isoZ = "2026-11-12T08:00:00Z"
        val millisZ = MedicationReminderSync.parseIsoToMillis(isoZ)
        assertEquals(millis, millisZ)
    }
}
