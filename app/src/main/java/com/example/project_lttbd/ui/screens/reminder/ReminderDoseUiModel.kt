package com.example.project_lttbd.ui.screens.reminder

import androidx.annotation.DrawableRes

data class ReminderDoseUiModel(
    val doseId: String,
    val medicineId: String,
    val timeLabel: String,
    val medicineName: String,
    val quantityText: String,
    val mealInstruction: String,
    @DrawableRes val imageRes: Int? = null,
    val audioDurationLabel: String? = null,
    val scheduledAtMillis: Long = 0L
)