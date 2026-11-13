package com.example.project_lttbd.ui.screens.schedule

import com.example.project_lttbd.data.mock.MockCalendar
import com.example.project_lttbd.data.mock.MockMedicineSchedule
import com.example.project_lttbd.data.remote.dto.DoseUnitDto
import com.example.project_lttbd.data.remote.dto.ScheduleInputDto
import com.example.project_lttbd.data.remote.dto.ScheduleResponseDto

internal fun MockMedicineSchedule.toScheduleInput(
    medicineId: String
): ScheduleInputDto {
    val quantity = quantity.trim().replace(',', '.')
    val pattern = Regex(
        "(?:0|[1-9][0-9]{0,5})(?:\\.[0-9]{1,3})?"
    )

    require(
        pattern.matches(quantity) &&
                quantity.toBigDecimal().signum() > 0
    ) {
        "Liều mỗi lần phải lớn hơn 0, tối đa 3 chữ số thập phân."
    }

    require(
        times.size in 1..24 &&
                times.all { it in 0..1439 }
    ) {
        "Chọn từ 1 đến 24 giờ uống hợp lệ."
    }

    require(times.distinct().size == times.size) {
        "Các giờ uống không được trùng nhau."
    }

    require(
        weekdays.isNotEmpty() &&
                weekdays.all { it in 1..7 }
    ) {
        "Chọn ít nhất một ngày trong tuần."
    }

    require(endDay == null || endDay >= startDay) {
        "Ngày kết thúc không được trước ngày bắt đầu."
    }

    require(
        stock == null &&
                lowStock == null &&
                sharingId == null
    ) {
        "Theo dõi số lượng và chia sẻ lịch chưa được hỗ trợ ở bước này."
    }

    val doseUnit = when (unit) {
        "viên" -> DoseUnitDto.TABLET
        "gói" -> DoseUnitDto.SACHET
        "ml" -> DoseUnitDto.ML
        "giọt" -> DoseUnitDto.DROP
        "liều" -> DoseUnitDto.DOSE

        else -> throw IllegalArgumentException(
            "Vui lòng chọn đơn vị liều dùng."
        )
    }

    return ScheduleInputDto(
        medicineId = medicineId,
        doseQuantity = quantity,
        doseUnit = doseUnit,
        times = times.sorted(),

        // Calendar: CN = 1; backend: thứ Hai = 1.
        weekdays = weekdays
            .map { (it + 5) % 7 + 1 }
            .sorted(),

        startDate = MockCalendar.key(startDay),
        endDate = endDay?.let { MockCalendar.key(it) },
        mealInstruction = meal.trim(),
        reminderMinutes = reminderMinutes
    )
}

internal fun ScheduleResponseDto.toInput(): ScheduleInputDto {
    return ScheduleInputDto(
        medicineId = medicineId,
        doseQuantity = doseQuantity,
        doseUnit = doseUnit,
        times = times.sorted(),
        weekdays = weekdays.sorted(),
        startDate = startDate,
        endDate = endDate,
        timeZone = timeZone,
        mealInstruction = mealInstruction,
        reminderMinutes = reminderMinutes,
        enabled = enabled
    )
}
internal fun ScheduleResponseDto.toEditorSchedule(): MockMedicineSchedule {
    return MockMedicineSchedule(
        quantity = doseQuantity,

        unit = when (doseUnit) {
            DoseUnitDto.TABLET -> "viên"
            DoseUnitDto.SACHET -> "gói"
            DoseUnitDto.ML -> "ml"
            DoseUnitDto.DROP -> "giọt"
            DoseUnitDto.DOSE -> "liều"
        },

        times = times.sorted(),

        // Backend: thứ Hai = 1; Calendar: Chủ nhật = 1.
        weekdays = weekdays.map {
            it % 7 + 1
        }.toSet(),

        meal = mealInstruction,
        reminderMinutes = reminderMinutes,
        startDay = parseScheduleDay(startDate),
        endDay = endDate?.let {
            parseScheduleDay(it)
        }
    )
}

internal fun MockMedicineSchedule.toScheduleUpdate(
    original: ScheduleResponseDto
): com.example.project_lttbd.data.remote.dto.ScheduleUpdateDto {
    val input = toScheduleInput(original.medicineId)

    return com.example.project_lttbd.data.remote.dto.ScheduleUpdateDto(
        medicineId = original.medicineId,
        expectedVersion = original.version,
        doseQuantity = input.doseQuantity,
        doseUnit = input.doseUnit,
        times = input.times,
        weekdays = input.weekdays,
        startDate = input.startDate,
        endDate = input.endDate,
        timeZone = original.timeZone,
        mealInstruction = input.mealInstruction,
        reminderMinutes = input.reminderMinutes,
        enabled = original.enabled
    )
}

private fun parseScheduleDay(value: String): Long {
    require(
        Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value)
    ) {
        "Ngày trong lịch chưa hợp lệ."
    }

    val format = java.text.SimpleDateFormat(
        "yyyy-MM-dd",
        java.util.Locale.ROOT
    )

    format.isLenient = false

    val date = requireNotNull(format.parse(value)) {
        "Ngày trong lịch chưa hợp lệ."
    }

    return MockCalendar.day(date.time)
}