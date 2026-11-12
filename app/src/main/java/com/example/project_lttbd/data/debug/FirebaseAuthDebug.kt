package com.example.project_lttbd.data.debug

import android.util.Log
import com.example.project_lttbd.data.remote.FirebaseProvider

object FirebaseAuthDebug {

    private const val TAG = "MMRA_AUTH"

    fun printIdTokenForLocalTesting() {
        val user = FirebaseProvider.auth.currentUser

        if (user == null) {
            Log.e(TAG, "Chưa có người dùng đăng nhập")
            return
        }

        user.getIdToken(false)
            .addOnSuccessListener { result ->
                val token = result.token

                if (token.isNullOrBlank()) {
                    Log.e(TAG, "ID Token rỗng")
                } else {
                    Log.d(TAG, "Đã lấy ID Token thành công")
                    Log.d(TAG, "UID: ${user.uid}")
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Không lấy được ID Token", error)
            }
    }
}
