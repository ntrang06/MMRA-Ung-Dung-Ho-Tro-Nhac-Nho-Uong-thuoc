package com.example.project_lttbd.notifications.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.notifications.sync.MedicationReminderSync

class MedicationBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MedicationBootReceiver"

        const val ACTION_TEST_BOOT =
            "com.example.project_lttbd.ACTION_TEST_BOOT"

        fun shouldRestoreDose(
            isFinished: Boolean,
            scheduledAtMillis: Long,
            nowMillis: Long = System.currentTimeMillis()
        ): Boolean {
            return !isFinished && scheduledAtMillis > nowMillis
        }

        fun restoreAlarms(
            context: Context,
            uid: String,
            nowMillis: Long = System.currentTimeMillis()
        ): Int {
            if (uid.isBlank()) return 0

            val currentUid =
                FirebaseProvider.auth.currentUser?.uid

            // Chỉ khôi phục báo thức của tài khoản đang đăng nhập.
            if (currentUid != uid) return 0

            MedicationReminderSync.clearDemoAlarms(
                context = context,
                uid = uid
            )

            // Đọc báo thức thật đã lưu trên máy.
            // Không cần gọi API trong lúc khởi động điện thoại.
            return MedicationAlarmScheduler.restoreSavedAlarms(
                context = context,
                uid = uid,
                nowMillis = nowMillis
            )
        }
    }

    override fun onReceive(
        context: Context,
        intent: Intent?
    ) {
        val action = intent?.action ?: return

        val supported = action == Intent.ACTION_BOOT_COMPLETED ||
                action == "android.intent.action.QUICKBOOT_POWERON" ||
                action == ACTION_TEST_BOOT

        if (!supported) return

        val requestedUid = intent
            .getStringExtra("extra_uid")
            ?.takeIf {
                it.isNotBlank()
            }

        val appContext = context.applicationContext ?: context
        val pendingResult = goAsync()

        Thread(
            {
                try {
                    val currentUid =
                        FirebaseProvider.auth.currentUser?.uid

                    if (currentUid.isNullOrBlank()) {
                        Log.d(
                            TAG,
                            "Chưa đăng nhập, bỏ qua khôi phục báo thức."
                        )
                    } else if (
                        requestedUid != null &&
                        requestedUid != currentUid
                    ) {
                        Log.d(
                            TAG,
                            "Tài khoản yêu cầu không khớp phiên hiện tại."
                        )
                    } else {
                        val count = restoreAlarms(
                            context = appContext,
                            uid = currentUid
                        )

                        Log.d(
                            TAG,
                            "Đã khôi phục $count báo thức đã lưu."
                        )
                    }
                } catch (error: Exception) {
                    Log.e(
                        TAG,
                        "Chưa khôi phục được báo thức.",
                        error
                    )
                } finally {
                    pendingResult.finish()
                }
            },
            "mmra-restore-alarms"
        ).start()
    }
}