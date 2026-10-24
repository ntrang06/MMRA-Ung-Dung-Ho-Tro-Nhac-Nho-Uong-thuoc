package com.example.project_lttbd.ui.screens.schedule

data class ScheduleSetupUiModel(
    val medicineName: String,
    val dosageForm: String = "",
    val medicineCode: String = "",
    val confirmed: Boolean = false,

    val quantityText: String = "Chưa thiết lập",
    val frequencyText: String = "Chưa thiết lập",
    val timesText: String = "Chưa chọn giờ",
    val reminderText: String = "",

    val mealInstruction: String = "Chưa thiết lập",

    val periodText: String = "Chưa thiết lập",
    val stockText: String = "Chưa nhập",
    val stockReminderText: String = "",

    val sharingText: String = "Không chia sẻ",
    val sharingDescription: String = ""
)

enum class ScheduleSetupField {
    QUANTITY,
    FREQUENCY,
    TIMES,
    MEAL,
    PERIOD,
    STOCK,
    SHARING
}