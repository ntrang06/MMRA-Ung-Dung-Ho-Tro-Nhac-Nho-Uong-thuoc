package com.example.project_lttbd.data.remote.dto

import com.google.firebase.Timestamp

data class DeviceDto(
    val deviceId: String = "",
    val fcmToken: String = "",
    val platform: String = "android",
    val notificationsEnabled: Boolean = false,
    val updatedAt: Timestamp? = null
)