package com.example.project_lttbd.data.mock

import androidx.compose.runtime.mutableStateListOf

data class MockMedicine(
    val medicineId: String,
    val medicineName: String,
    val medicineCode: String,
    val quantityText: String
)

object MockMedicines {

    val medicines = mutableStateListOf(
        MockMedicine(
            medicineId = "medicine_01",
            medicineName = "Amlodipine 5 mg",
            medicineCode = "A5",
            quantityText = "1 viên"
        ),
        MockMedicine(
            medicineId = "medicine_02",
            medicineName = "Metformin 500 mg",
            medicineCode = "M500",
            quantityText = "1 viên"
        ),
        MockMedicine(
            medicineId = "medicine_03",
            medicineName = "Atorvastatin 20 mg",
            medicineCode = "A20",
            quantityText = "1 viên"
        )
    )
}