package com.example.project_lttbd.data.mock

data class ScanResult(
    val id: String,
    val medicineName: String,
    val dosage: String,
    val dosageForm: String,
    val matchScore: Int,
    val activeIngredient: String,
    val manufacturer: String,
    val packaging: String,
    val verifiedSource: String = "Cục Quản lý Dược • Cập nhật 09/2026",
    val pillCode: String = "M500",
    val color: String = "Màu trắng",
    val imageUrl: String? = null
)

object MockScanMedicine {
    val defaultResult = ScanResult(
        id = "med_001",
        medicineName = "Metformin",
        dosage = "500 mg",
        dosageForm = "Viên nén bao phim",
        matchScore = 92,
        activeIngredient = "Metformin hydrochloride",
        manufacturer = "Mekophar",
        packaging = "Hộp 3 vỉ × 10 viên",
        verifiedSource = "Cục Quản lý Dược • Cập nhật 09/2026",
        pillCode = "M500",
        color = "Màu trắng"
    )

    val mockResults = listOf(
        defaultResult,
        ScanResult(
            id = "med_002",
            medicineName = "Amlodipine",
            dosage = "5 mg",
            dosageForm = "Viên nén",
            matchScore = 95,
            activeIngredient = "Amlodipine besylate",
            manufacturer = "Dược Hậu Giang (DHG Pharma)",
            packaging = "Hộp 3 vỉ × 10 viên",
            verifiedSource = "Cục Quản lý Dược • Cập nhật 09/2026",
            pillCode = "A5",
            color = "Màu vàng nhạt"
        ),
        ScanResult(
            id = "med_003",
            medicineName = "Panadol Extra",
            dosage = "500 mg / 65 mg",
            dosageForm = "Viên nén bao phim",
            matchScore = 88,
            activeIngredient = "Paracetamol, Caffeine",
            manufacturer = "GlaxoSmithKline (GSK)",
            packaging = "Hộp 15 vỉ × 12 viên",
            verifiedSource = "Cục Quản lý Dược • Cập nhật 09/2026",
            pillCode = "P+",
            color = "Màu đỏ trắng"
        )
    )
}
