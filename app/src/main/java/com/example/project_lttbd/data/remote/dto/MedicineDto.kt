package com.example.project_lttbd.data.remote.dto

import com.google.firebase.Timestamp

data class MedicineDto(
    val medicineName: String = "",
    val strength: String = "",
    val strengthUnit: String = "",
    val dosageForm: String = "",
    val note: String = "",
    val imageUrl: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)