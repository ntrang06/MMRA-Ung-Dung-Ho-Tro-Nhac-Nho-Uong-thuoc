package com.example.project_lttbd.data.mock

import androidx.compose.runtime.mutableStateMapOf
import com.example.project_lttbd.ui.screens.reminder.ReminderDoseUiModel

enum class MockDoseAction {
    TAKEN,
    SNOOZED,
    SKIPPED
}

data class MockDoseActionRecord(
    val action: MockDoseAction,
    val recordedAtMillis: Long,
    val dose: ReminderDoseUiModel? = null
)

object MockDoseActions {
    private val records =
        mutableStateMapOf<String, MockDoseActionRecord>()

    val historyNotes = mutableStateMapOf<String, String>()

    init {
        // Dữ liệu mẫu để thử giao diện.
        for (offset in -6..0) {
            val day = MockCalendar.plusDays(
                MockCalendar.today(),
                offset
            )

            for (index in 0..2) {
                if (offset == 0 && index != 0) continue
                if (offset == -2 && index == 2) continue

                val minute = listOf(480, 780, 1200)[index]
                val medicineId = "medicine_0${index + 1}"
                val due = MockCalendar.at(day, minute)

                val dose = ReminderDoseUiModel(
                    doseId = MockCalendar.doseId(
                        medicineId,
                        day,
                        minute
                    ),
                    medicineId = medicineId,
                    timeLabel =
                        "${MockCalendar.time(minute)} • ${MockCalendar.date(day)}",
                    medicineName = listOf(
                        "Amlodipine 5 mg",
                        "Metformin 500 mg",
                        "Atorvastatin 20 mg"
                    )[index],
                    quantityText = "1 viên",
                    mealInstruction = listOf(
                        "Sau ăn sáng",
                        "Cùng bữa trưa",
                        "Buổi tối"
                    )[index],
                    scheduledAtMillis = due
                )

                val action =
                    if (offset == -3 && index == 1) {
                        MockDoseAction.SKIPPED
                    } else {
                        MockDoseAction.TAKEN
                    }

                val delay =
                    if (offset == -1 && index == 1) 25 else 3

                val recordedAt = due + delay * 60_000L

                if (recordedAt > System.currentTimeMillis()) continue

                records[dose.doseId] = MockDoseActionRecord(
                    action = action,
                    recordedAtMillis = recordedAt,
                    dose = dose
                )
            }
        }
    }

    fun get(doseId: String): MockDoseActionRecord? =
        records[doseId]

    fun snapshot(doseId: String): ReminderDoseUiModel? =
        records[doseId]?.dose

    fun snapshots(): List<ReminderDoseUiModel> =
        records.values.mapNotNull { it.dose }

    fun isFinished(doseId: String): Boolean =
        get(doseId)?.action.let {
            it == MockDoseAction.TAKEN ||
                    it == MockDoseAction.SKIPPED
        }

    fun record(doseId: String, action: MockDoseAction) {
        if (isFinished(doseId)) return

        val dose = MockReminderDoses.findById(doseId) ?: return

        records[doseId] = MockDoseActionRecord(
            action = action,
            recordedAtMillis = System.currentTimeMillis(),
            dose = dose
        )
    }
}