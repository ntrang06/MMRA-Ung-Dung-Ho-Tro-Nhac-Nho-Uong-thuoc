package com.example.project_lttbd.data.mock

enum class AlertSeverity {
    HIGH,
    MEDIUM,
    LOW
}

data class CaregiverAlert(
    val id: String,
    val memberId: String,
    val memberName: String,
    val relationship: String,
    val medicineName: String,
    val scheduledTime: String,
    val minutesLate: Int,
    val severity: AlertSeverity = AlertSeverity.HIGH,
    val memberPhone: String = "",
    val isRead: Boolean = false,
    val timestamp: String = "Hôm nay"
)

object MockAlertData {
    val mockAlerts = listOf(
        CaregiverAlert(
            id = "alert_01",
            memberId = "mem_01",
            memberName = "Nguyễn Văn Hùng",
            relationship = "Bố",
            medicineName = "Atorvastatin 10 mg",
            scheduledTime = "20:00",
            minutesLate = 45,
            severity = AlertSeverity.HIGH,
            memberPhone = "0912 345 678",
            isRead = false,
            timestamp = "Hôm nay, 20:45"
        ),
        CaregiverAlert(
            id = "alert_02",
            memberId = "mem_01",
            memberName = "Nguyễn Văn Hùng",
            relationship = "Bố",
            medicineName = "Amlodipine 5 mg",
            scheduledTime = "07:30",
            minutesLate = 30,
            severity = AlertSeverity.MEDIUM,
            memberPhone = "0912 345 678",
            isRead = true,
            timestamp = "Hôm qua, 08:00"
        )
    )
}
