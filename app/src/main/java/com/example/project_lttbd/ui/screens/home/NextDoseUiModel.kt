package com.example.project_lttbd.ui.screens.home

data class NextDoseUiModel(
    val doseId: String,
    val medicineId: String,
    val time: String,
    val medicineName: String,
    val instruction: String,
    val medicineCode: String,
    val countdownText: String
)