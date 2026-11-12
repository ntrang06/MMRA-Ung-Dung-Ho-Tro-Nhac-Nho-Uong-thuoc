package com.example.project_lttbd.data.remote

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object BackendSessionCheck {
    private const val TAG = "MMRA_API_TEST"

    private const val URL =
        "http://127.0.0.1:8000/api/v1/auth/me"

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private var started = false

    fun run(context: Context) {
        val isDebug = (
                context.applicationInfo.flags and
                        ApplicationInfo.FLAG_DEBUGGABLE
                ) != 0

        if (!isDebug || started) return
        started = true

        FirebaseProvider.auth.addAuthStateListener(
            object : FirebaseAuth.AuthStateListener {
                override fun onAuthStateChanged(
                    auth: FirebaseAuth
                ) {
                    val user = auth.currentUser ?: return

                    auth.removeAuthStateListener(this)

                    scope.launch {
                        try {
                            val token = user
                                .getIdToken(true)
                                .await()
                                .token
                                ?.takeIf { it.isNotBlank() }
                                ?: error(
                                    "Chưa lấy được Firebase ID token."
                                )

                            if (
                                auth.currentUser?.uid != user.uid
                            ) {
                                Log.d(
                                    TAG,
                                    "Tài khoản đã thay đổi; dừng kiểm tra."
                                )
                                return@launch
                            }

                            val client = OkHttpClient.Builder()
                                .connectTimeout(
                                    10,
                                    TimeUnit.SECONDS
                                )
                                .readTimeout(
                                    20,
                                    TimeUnit.SECONDS
                                )
                                .callTimeout(
                                    30,
                                    TimeUnit.SECONDS
                                )
                                .retryOnConnectionFailure(false)
                                .followRedirects(false)
                                .followSslRedirects(false)
                                .build()

                            fun checkRequest(
                                label: String,
                                authorization: String?
                            ) {
                                val request = Request.Builder()
                                    .url(
                                        "$URL?uid=ignored-client-uid"
                                    )
                                    .apply {
                                        if (
                                            authorization != null
                                        ) {
                                            header(
                                                "Authorization",
                                                authorization
                                            )
                                        }
                                    }
                                    .build()

                                client.newCall(request)
                                    .execute()
                                    .use { response ->
                                        Log.d(
                                            TAG,
                                            "$label: ${response.code}"
                                        )

                                        if (
                                            label == "Token thật" &&
                                            response.isSuccessful
                                        ) {
                                            val body = response.body
                                                ?.string()
                                                .orEmpty()

                                            val correctUid =
                                                JSONObject(body)
                                                    .optString("uid") ==
                                                        user.uid

                                            Log.d(
                                                TAG,
                                                "UID đúng tài khoản: $correctUid"
                                            )
                                        }
                                    }
                            }

                            checkRequest(
                                "Không token",
                                null
                            )

                            checkRequest(
                                "Token sai",
                                "Bearer invalid-test-token"
                            )

                            checkRequest(
                                "Token thật",
                                "Bearer $token"
                            )
                        } catch (
                            cancelled: CancellationException
                        ) {
                            throw cancelled
                        } catch (error: Exception) {
                            Log.e(
                                TAG,
                                "Kiểm tra thất bại: " +
                                        error.javaClass.simpleName
                            )
                        }
                    }
                }
            }
        )
    }
}