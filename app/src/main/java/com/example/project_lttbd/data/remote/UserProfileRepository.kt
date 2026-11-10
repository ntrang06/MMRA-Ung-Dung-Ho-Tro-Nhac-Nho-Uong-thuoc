package com.example.project_lttbd.data.remote

import com.example.project_lttbd.data.remote.dto.UserProfileDto
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

object UserProfileRepository {

    suspend fun createProfile(
        uid: String,
        displayName: String,
        email: String
    ): UserProfileDto {
        val document = FirebaseProvider.userDocument(uid)

        // Chỉ tạo khi chưa có hồ sơ.
        FirebaseProvider.firestore.runTransaction { transaction ->
            val snapshot = transaction.get(document)

            if (!snapshot.exists()) {
                val fields = mapOf<String, Any?>(
                    "displayName" to displayName.trim(),
                    "email" to email.trim(),
                    "usageMode" to null,
                    "avatarUrl" to null,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                transaction.set(document, fields)
            }

            Unit
        }.await()

        return getProfile(uid)
            ?: error(
                "Chưa đọc được hồ sơ tài khoản. Vui lòng thử lại."
            )
    }

    suspend fun getProfile(uid: String): UserProfileDto? {
        val snapshot = FirebaseProvider.userDocument(uid)
            .get()
            .await()

        return snapshot.toObject(UserProfileDto::class.java)
    }

    suspend fun updateProfile(
        uid: String,
        updates: Map<String, Any?>
    ) {
        val fields = updates.toMutableMap()

        // Giữ nguyên thời điểm tạo hồ sơ.
        fields.remove("createdAt")
        fields["updatedAt"] = FieldValue.serverTimestamp()

        FirebaseProvider.userDocument(uid)
            .update(fields)
            .await()
    }
}