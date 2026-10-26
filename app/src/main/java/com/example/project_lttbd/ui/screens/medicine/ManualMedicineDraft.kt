package com.example.project_lttbd.ui.screens.medicine

data class ManualMedicineDraft(
    val medicineName: String,
    val strength: String,
    val strengthUnit: String,
    val dosageForm: String,
    val note: String,
    val imageUri: String? = null
)