package com.example.project_lttbd.ui.screens.medicine

import com.example.project_lttbd.data.remote.dto.DoseUnitDto
import com.example.project_lttbd.data.remote.dto.ScheduleResponseDto
import com.example.project_lttbd.data.repository.MedicineRecord
import java.util.Locale

internal fun MedicineRecord.toMedicineUiModel(
    schedules: List<ScheduleResponseDto>,
    schedulesLoading: Boolean,
    schedulesFailed: Boolean
): MedicineUiModel {
    val linked = schedules.filter {
        it.medicineId == id
    }

    val single = linked.singleOrNull()

    val status = when {
        schedulesLoading -> "Đang tải lịch uống…"
        schedulesFailed -> "Chưa tải được lịch uống"
        linked.isEmpty() -> "Chưa có lịch uống"
        single != null && !single.enabled -> "Đã tạm dừng lịch uống"
        single != null -> "Đã có lịch uống"
        else -> "Đã có ${linked.size} lịch uống"
    }

    val details = when {
        schedulesLoading || schedulesFailed -> ""

        single != null -> listOf(
            single.times.sorted().joinToString(" • ") {
                String.format(
                    Locale.ROOT,
                    "%02d:%02d",
                    it / 60,
                    it % 60
                )
            },

            if (single.weekdays.toSet() == (1..7).toSet()) {
                "Mỗi ngày"
            } else {
                single.weekdays
                    .distinct()
                    .sorted()
                    .joinToString(", ") {
                        if (it == 7) "CN" else "T${it + 1}"
                    }
            },

            single.mealInstruction
        )
            .filter { it.isNotBlank() }
            .joinToString(" · ")

        linked.isNotEmpty() -> {
            val active = linked.count { it.enabled }
            "$active đang bật · ${linked.size - active} tạm dừng"
        }

        else -> ""
    }

    return MedicineUiModel(
        medicineId = id,

        medicineName = listOf(
            data.medicineName,
            data.strength,
            data.strengthUnit
        )
            .filter { it.isNotBlank() }
            .joinToString(" "),

        medicineCode = "",

        quantityText = if (
            single != null &&
            !schedulesLoading &&
            !schedulesFailed
        ) {
            "${single.doseQuantity} ${single.doseUnit.displayUnit()}"
        } else {
            ""
        },

        scheduleText = details,
        hasSchedule = linked.isNotEmpty(),
        scheduleStatusText = status,
        accent = MedicineAccent.GREEN
    )
}

private fun DoseUnitDto.displayUnit(): String = when (this) {
    DoseUnitDto.TABLET -> "viên"
    DoseUnitDto.SACHET -> "gói"
    DoseUnitDto.ML -> "ml"
    DoseUnitDto.DROP -> "giọt"
    DoseUnitDto.DOSE -> "liều"
}