package com.example.project_lttbd.data.mock

import com.example.project_lttbd.ui.screens.reminder.ReminderDoseUiModel
import java.text.SimpleDateFormat
import java.util.Locale

object MockReminderDoses {
    val doses: List<ReminderDoseUiModel>
        get() = forDate(MockCalendar.today())

    fun forDate(date: Long): List<ReminderDoseUiModel> {
        val generated = MockMedicines.forDate(date).flatMap { medicine ->
            val schedule = medicine.schedule

            if (schedule == null || !schedule.contains(date)) {
                emptyList()
            } else {
                schedule.times.map { time ->
                    val dateLabel =
                        if (MockCalendar.day(date) == MockCalendar.today()) {
                            "Hôm nay"
                        } else {
                            MockCalendar.date(date)
                        }

                    ReminderDoseUiModel(
                        doseId = MockCalendar.doseId(
                            medicine.medicineId,
                            date,
                            time
                        ),
                        medicineId = medicine.medicineId,
                        timeLabel = "${MockCalendar.time(time)} • $dateLabel",
                        medicineName = medicine.medicineName,
                        quantityText = schedule.quantityText,
                        mealInstruction = schedule.meal,
                        scheduledAtMillis = MockCalendar.at(date, time)
                    )
                }
            }
        }

        // Giữ thông tin các liều đã uống hoặc đã bỏ qua.
        val recorded = MockDoseActions.snapshots().filter {
            MockDoseActions.isFinished(it.doseId) &&
                    MockCalendar.day(it.scheduledAtMillis) ==
                    MockCalendar.day(date)
        }

        return (generated + recorded)
            .associateBy { it.doseId }
            .values
            .sortedBy { it.scheduledAtMillis }
    }

    fun findById(doseId: String?): ReminderDoseUiModel? {
        if (doseId == null) return null

        if (MockDoseActions.isFinished(doseId)) {
            MockDoseActions.snapshot(doseId)?.let {
                return it
            }
        }

        val dateKey = doseId.split('|').getOrNull(1) ?: return null

        val date = runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
                isLenient = false
            }.parse(dateKey)?.time
        }.getOrNull() ?: return null

        return forDate(date).firstOrNull {
            it.doseId == doseId
        }
    }
}