package com.example.project_lttbd.data.mock

enum class DoseStatus {
    TAKEN,
    PENDING,
    LATE,
    SKIPPED
}

data class DoseScheduleItem(
    val id: String,
    val medicineName: String,
    val dosage: String,
    val scheduledTime: String,
    val mealRelation: String,
    val status: DoseStatus = DoseStatus.PENDING
)

data class FamilyMember(
    val id: String,
    val fullName: String,
    val relationship: String,
    val role: String,
    val phone: String,
    val age: Int,
    val avatarResId: Int? = null,
    val todayTakenCount: Int,
    val todayTotalCount: Int,
    val hasLateDose: Boolean,
    val doseSchedules: List<DoseScheduleItem> = emptyList(),
    val canViewSchedule: Boolean = true,
    val canReceiveAlerts: Boolean = true,
    val canViewHistory: Boolean = true
)

object MockFamilyData {
    val mockMembers = listOf(
        FamilyMember(
            id = "mem_01",
            fullName = "Nguyễn Văn Hùng",
            relationship = "Bố",
            role = "Người cần chăm sóc",
            phone = "0912 345 678",
            age = 68,
            todayTakenCount = 2,
            todayTotalCount = 3,
            hasLateDose = true,
            doseSchedules = listOf(
                DoseScheduleItem(
                    id = "dose_01",
                    medicineName = "Amlodipine",
                    dosage = "5 mg",
                    scheduledTime = "07:30",
                    mealRelation = "Sau khi ăn sáng",
                    status = DoseStatus.TAKEN
                ),
                DoseScheduleItem(
                    id = "dose_02",
                    medicineName = "Metformin",
                    dosage = "500 mg",
                    scheduledTime = "12:00",
                    mealRelation = "Ngay sau bữa trưa",
                    status = DoseStatus.TAKEN
                ),
                DoseScheduleItem(
                    id = "dose_03",
                    medicineName = "Atorvastatin",
                    dosage = "10 mg",
                    scheduledTime = "20:00",
                    mealRelation = "Trước khi đi ngủ",
                    status = DoseStatus.LATE
                )
            )
        ),
        FamilyMember(
            id = "mem_02",
            fullName = "Trần Thị Mai",
            relationship = "Mẹ",
            role = "Người cần chăm sóc",
            phone = "0987 654 321",
            age = 65,
            todayTakenCount = 2,
            todayTotalCount = 2,
            hasLateDose = false,
            doseSchedules = listOf(
                DoseScheduleItem(
                    id = "dose_04",
                    medicineName = "Canxi Nano",
                    dosage = "1 viên",
                    scheduledTime = "08:00",
                    mealRelation = "Sau ăn sáng 30 phút",
                    status = DoseStatus.TAKEN
                ),
                DoseScheduleItem(
                    id = "dose_05",
                    medicineName = "Glucosamine",
                    dosage = "500 mg",
                    scheduledTime = "12:30",
                    mealRelation = "Sau bữa trưa",
                    status = DoseStatus.TAKEN
                )
            )
        ),
        FamilyMember(
            id = "mem_03",
            fullName = "Nguyễn Minh Anh",
            relationship = "Con gái",
            role = "Người chăm sóc",
            phone = "0901 234 567",
            age = 28,
            todayTakenCount = 1,
            todayTotalCount = 1,
            hasLateDose = false,
            doseSchedules = listOf(
                DoseScheduleItem(
                    id = "dose_06",
                    medicineName = "Vitamin C",
                    dosage = "500 mg",
                    scheduledTime = "09:00",
                    mealRelation = "Sau ăn sáng",
                    status = DoseStatus.TAKEN
                )
            )
        )
    )
}
