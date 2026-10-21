package com.example.project_lttbd.data.mock

import com.example.project_lttbd.ui.screens.reminder.ReminderDoseUiModel

object MockReminderDoses {
    val doses = listOf(
        ReminderDoseUiModel(
            doseId = "dose_01",
            medicineId = "medicine_01",
            timeLabel = "08:00 • Hôm nay",
            medicineName = "Amlodipine 5 mg",
            quantityText = "1 viên",
            mealInstruction = "SAU ĂN SÁNG"
        ),
        ReminderDoseUiModel(
            doseId = "dose_02",
            medicineId = "medicine_02",
            timeLabel = "13:00 • Hôm nay",
            medicineName = "Metformin 500 mg",
            quantityText = "1 viên",
            mealInstruction = "CÙNG BỮA TRƯA",
            audioDurationLabel = "0:08"
        ),
        ReminderDoseUiModel(
            doseId = "dose_03",
            medicineId = "medicine_03",
            timeLabel = "20:00 • Hôm nay",
            medicineName = "Atorvastatin 20 mg",
            quantityText = "1 viên",
            mealInstruction = "BUỔI TỐI"
        )
    )

    fun findById(doseId: String?): ReminderDoseUiModel? {
        return doses.firstOrNull { it.doseId == doseId }
    }
}