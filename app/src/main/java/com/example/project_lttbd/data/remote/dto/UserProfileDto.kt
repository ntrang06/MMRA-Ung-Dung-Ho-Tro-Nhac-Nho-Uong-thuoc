package com.example.project_lttbd.data.remote.dto

import com.google.firebase.Timestamp

data class UserProfileDto(
    val displayName: String = "",
    val email: String = "",
    val usageMode: String? = null,
    val avatarUrl: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)