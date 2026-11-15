
package com.example.project_lttbd.data.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.project_lttbd.data.remote.FirebaseProvider

object FirebaseAuthDebug {

    private const val TAG = "MMRA_AUTH"

    fun copyIdTokenForLocalTesting(context: Context) {
        val user = FirebaseProvider.auth.currentUser

        if (user == null) {
            Log.e(TAG, "Chưa có người dùng đăng nhập")
            return
        }

        user.getIdToken(true)
            .addOnSuccessListener { result ->
                val token = result.token

                if (token.isNullOrBlank()) {
                    Log.e(TAG, "ID Token rỗng")
                    return@addOnSuccessListener
                }

                val clipboard = context.getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager

                clipboard.setPrimaryClip(
                    ClipData.newPlainText(
                        "Firebase ID Token",
                        token
                    )
                )

                Log.d(TAG, "Đã sao chép Firebase ID Token")
                Toast.makeText(
                    context,
                    "Đã sao chép Firebase ID Token",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Không lấy được ID Token", error)
            }
    }
}
