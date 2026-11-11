package com.example.project_lttbd.data.remote.dto

enum class DoseStatusDto {
    PENDING,
    TAKEN,
    SKIPPED,
    MISSED,
    CANCELLED
}

enum class DoseUnitDto {
    TABLET,
    SACHET,
    ML,
    DROP,
    DOSE
}

enum class DoseActionDto {
    TAKEN,
    SKIPPED
}

// Ngày: yyyy-MM-dd.
// times: số phút từ 00:00.
// weekdays: 1 = thứ Hai, ..., 7 = Chủ nhật.
data class ScheduleInputDto(
    val medicineId: String,
    val doseQuantity: String,
    val doseUnit: DoseUnitDto,
    val times: List<Int>,
    val weekdays: List<Int>,
    val startDate: String,
    val endDate: String? = null,
    val timeZone: String = "Asia/Ho_Chi_Minh",
    val mealInstruction: String = "",
    val reminderMinutes: Int = 0,
    val enabled: Boolean = true
)

data class ScheduleUpdateDto(
    val medicineId: String,
    val doseQuantity: String,
    val doseUnit: DoseUnitDto,
    val times: List<Int>,
    val weekdays: List<Int>,
    val startDate: String,
    val expectedVersion: Int,
    val endDate: String? = null,
    val timeZone: String = "Asia/Ho_Chi_Minh",
    val mealInstruction: String = "",
    val reminderMinutes: Int = 0,
    val enabled: Boolean = true
)

data class ScheduleResponseDto(
    val scheduleId: String,
    val version: Int,
    val medicineId: String,
    val doseQuantity: String,
    val doseUnit: DoseUnitDto,
    val times: List<Int>,
    val weekdays: List<Int>,
    val startDate: String,
    val endDate: String?,
    val timeZone: String,
    val mealInstruction: String,
    val reminderMinutes: Int,
    val enabled: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class MedicineSnapshotDto(
    val medicineName: String,
    val strength: String,
    val strengthUnit: String,
    val dosageForm: String
)

// Các thời điểm dùng ISO 8601 có múi giờ,
// ví dụ 2026-11-12T01:00:00Z.
data class DoseResponseDto(
    val doseId: String,
    val scheduleId: String,
    val scheduleVersion: Int,
    val medicineId: String,
    val localDate: String,
    val minuteOfDay: Int,
    val timeZone: String,
    val scheduledAt: String,
    val doseQuantity: String,
    val doseUnit: DoseUnitDto,
    val mealInstruction: String,
    val medicineSnapshot: MedicineSnapshotDto,
    val status: DoseStatusDto,
    val actionAt: String?,
    val nextReminderAt: String?,
    val createdAt: String,
    val updatedAt: String
)

data class DoseActionRequestDto(
    val action: DoseActionDto
)

data class SnoozeRequestDto(
    val minutes: Int
)