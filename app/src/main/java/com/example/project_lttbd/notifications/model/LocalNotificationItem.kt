package com.example.project_lttbd.notifications.model

/**
 * Bản ghi thông báo lưu trữ cục bộ trên máy,
 * được phân tách theo từng tài khoản (UID).
 */
data class LocalNotificationItem(
    val id: String,
    val doseId: String,
    val title: String,
    val body: String,
    val timestampMillis: Long,
    val isRead: Boolean = false,
    val destinationRoute: String = ""
)
