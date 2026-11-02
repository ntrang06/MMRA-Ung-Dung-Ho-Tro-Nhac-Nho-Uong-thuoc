package com.example.project_lttbd.data.remote

import com.example.project_lttbd.data.remote.dto.UserProfileDto
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

object UserProfileRepository {

    suspend fun createProfile(uid: String, displayName: String, email: String): UserProfileDto {
        val now = Timestamp.now()
        val profile = UserProfileDto(
            displayName = displayName,
            email = email,
            usageMode = null,
            avatarUrl = null,
            createdAt = now,
            updatedAt = now
        )
        FirebaseProvider.userDocument(uid).set(profile).await()
        return profile
    }

    suspend fun getProfile(uid: String): UserProfileDto? {
        val snapshot = FirebaseProvider.userDocument(uid).get().await()
        return snapshot.toObject(UserProfileDto::class.java)
    }

    suspend fun updateProfile(uid: String, updates: Map<String, Any?>) {
        val mutableUpdates = updates.toMutableMap()
        mutableUpdates["updatedAt"] = FieldValue.serverTimestamp()
        FirebaseProvider.userDocument(uid).update(mutableUpdates).await()
    }
}
