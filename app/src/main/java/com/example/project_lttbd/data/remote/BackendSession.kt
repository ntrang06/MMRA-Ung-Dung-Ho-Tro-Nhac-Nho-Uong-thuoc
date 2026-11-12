package com.example.project_lttbd.data.remote

import kotlinx.coroutines.tasks.await

object BackendSession {

    // Gọi từ coroutine trước mỗi request có xác thực.
    suspend fun authorizationHeader(): String {
        val user = FirebaseProvider.auth.currentUser
            ?: error("Vui lòng đăng nhập lại.")

        val token = user
            .getIdToken(false)
            .await()
            .token
            ?.takeIf { it.isNotBlank() }
            ?: error("Chưa lấy được phiên đăng nhập.")

        check(
            FirebaseProvider.auth.currentUser?.uid == user.uid
        ) {
            "Tài khoản đã thay đổi. Vui lòng thử lại."
        }

        return "Bearer $token"
    }
}