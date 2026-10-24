package com.example.project_lttbd.data.mock

import androidx.compose.runtime.mutableStateListOf
import com.example.project_lttbd.ui.screens.medicine.ManualMedicineDraft
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

object MockCalendar {
    fun day(value: Long): Long = Calendar.getInstance().apply {
        timeInMillis = value
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun today(): Long = day(System.currentTimeMillis())

    fun plusDays(value: Long, count: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = day(value)
            add(Calendar.DAY_OF_MONTH, count)
        }.timeInMillis

    fun key(value: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(value))

    fun date(value: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(Date(value))

    fun at(value: Long, minute: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = day(value)
            set(Calendar.HOUR_OF_DAY, minute / 60)
            set(Calendar.MINUTE, minute % 60)
        }.timeInMillis

    fun time(minute: Int): String =
        String.format(Locale.ROOT, "%02d:%02d", minute / 60, minute % 60)

    fun doseId(medicineId: String, date: Long, minute: Int): String =
        "$medicineId|${key(date)}|$minute"
}

data class MockMedicineSchedule(
    val quantity: String,
    val unit: String = "viên",
    val times: List<Int>,
    val weekdays: Set<Int> = (1..7).toSet(),
    val meal: String = "",
    val reminderMinutes: Int = 10,
    val startDay: Long = MockCalendar.today(),
    val endDay: Long? = null,
    val stock: Int? = null,
    val lowStock: Int? = null,
    val sharingId: String? = null,
    val sharingName: String = ""
) : java.io.Serializable {
    val quantityText: String
        get() = "$quantity $unit"

    fun contains(date: Long): Boolean {
        val selectedDay = MockCalendar.day(date)
        val weekday = Calendar.getInstance().apply {
            timeInMillis = selectedDay
        }.get(Calendar.DAY_OF_WEEK)

        return selectedDay >= MockCalendar.day(startDay) &&
                (endDay == null || selectedDay <= MockCalendar.day(endDay)) &&
                weekday in weekdays
    }
}

data class MockMedicine(
    val medicineId: String,
    val medicineName: String,
    val medicineCode: String,
    val quantityText: String,
    val baseName: String = medicineName,
    val strength: String = "",
    val strengthUnit: String = "mg",
    val dosageForm: String = "",
    val note: String = "",
    val imageUri: String? = null,
    val schedule: MockMedicineSchedule? = null
) {
    fun draft() = ManualMedicineDraft(
        medicineName = baseName,
        strength = strength,
        strengthUnit = strengthUnit,
        dosageForm = dosageForm,
        note = note,
        imageUri = imageUri
    )
}

private data class MockMedicineRevision(
    val previous: MockMedicine,
    val effectiveFromDay: Long
)

object MockMedicines {
    private val revisions = mutableStateListOf<MockMedicineRevision>()
    private val start = MockCalendar.plusDays(MockCalendar.today(), -6)
    private val end = MockCalendar.plusDays(MockCalendar.today(), 23)

    val medicines = mutableStateListOf(
        MockMedicine(
            medicineId = "medicine_01",
            medicineName = "Amlodipine 5 mg",
            medicineCode = "A5",
            quantityText = "1 viên",
            baseName = "Amlodipine",
            strength = "5",
            dosageForm = "Viên nén",
            schedule = MockMedicineSchedule(
                quantity = "1",
                times = listOf(480),
                meal = "Sau ăn sáng",
                startDay = start,
                endDay = end
            )
        ),
        MockMedicine(
            medicineId = "medicine_02",
            medicineName = "Metformin 500 mg",
            medicineCode = "M500",
            quantityText = "1 viên",
            baseName = "Metformin",
            strength = "500",
            dosageForm = "Viên nén",
            schedule = MockMedicineSchedule(
                quantity = "1",
                times = listOf(780),
                meal = "Cùng bữa trưa",
                startDay = start,
                endDay = end
            )
        ),
        MockMedicine(
            medicineId = "medicine_03",
            medicineName = "Atorvastatin 20 mg",
            medicineCode = "A20",
            quantityText = "1 viên",
            baseName = "Atorvastatin",
            strength = "20",
            dosageForm = "Viên nén",
            schedule = MockMedicineSchedule(
                quantity = "1",
                times = listOf(1200),
                meal = "Buổi tối",
                startDay = start,
                endDay = end
            )
        )
    )

    fun findById(id: String?): MockMedicine? =
        medicines.firstOrNull { it.medicineId == id }

    fun forDate(date: Long): List<MockMedicine> {
        val selectedDay = MockCalendar.day(date)

        return medicines.map { current ->
            revisions.firstOrNull {
                it.previous.medicineId == current.medicineId &&
                        selectedDay < it.effectiveFromDay
            }?.previous ?: current
        }
    }

    fun save(
        draft: ManualMedicineDraft,
        schedule: MockMedicineSchedule,
        id: String? = null
    ): String {
        require(draft.medicineName.isNotBlank())
        require(isPositiveDoseQuantity(schedule.quantity))
        require(schedule.unit.isNotBlank())
        require(
            schedule.times.isNotEmpty() &&
                    schedule.times.all { it in 0..1439 }
        )
        require(schedule.times.distinct().size == schedule.times.size)
        require(
            schedule.weekdays.isNotEmpty() &&
                    schedule.weekdays.all { it in 1..7 }
        )
        require(
            schedule.endDay == null ||
                    schedule.endDay >= schedule.startDay
        )
        require(schedule.stock == null || schedule.stock >= 0)
        require(schedule.lowStock == null || schedule.lowStock >= 0)
        require(schedule.lowStock == null || schedule.stock != null)

        val previous = findById(id)
        require(id == null || previous != null)

        val newId = previous?.medicineId ?: "medicine_${UUID.randomUUID()}"

        val fullName = listOf(
            draft.medicineName.trim(),
            if (draft.strength.isBlank()) {
                ""
            } else {
                "${draft.strength.trim()} ${draft.strengthUnit}"
            }
        ).filter { it.isNotBlank() }.joinToString(" ")

        val medicine = MockMedicine(
            medicineId = newId,
            medicineName = fullName,
            medicineCode = previous?.medicineCode.orEmpty(),
            quantityText = schedule.quantityText,
            baseName = draft.medicineName.trim(),
            strength = draft.strength.trim(),
            strengthUnit = draft.strengthUnit,
            dosageForm = draft.dosageForm,
            note = draft.note.trim(),
            imageUri = draft.imageUri,
            schedule = schedule.copy(times = schedule.times.sorted())
        )

        val index = medicines.indexOfFirst {
            it.medicineId == newId
        }

        if (index < 0) {
            medicines.add(medicine)
        } else {
            // Bản sửa có hiệu lực từ hôm nay.
            revisions.add(
                MockMedicineRevision(
                    previous = medicines[index],
                    effectiveFromDay = MockCalendar.today()
                )
            )
            medicines[index] = medicine
        }

        return newId
    }
}

fun isPositiveDoseQuantity(value: String): Boolean =
    value.matches(Regex("\\d+([.,]\\d+)?")) &&
            value.replace(',', '.').toBigDecimalOrNull()?.signum() == 1