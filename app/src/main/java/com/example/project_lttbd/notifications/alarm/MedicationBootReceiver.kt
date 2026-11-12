package com.example.project_lttbd.notifications.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.project_lttbd.data.mock.MockCalendar
import com.example.project_lttbd.data.mock.MockDoseActions
import com.example.project_lttbd.data.mock.MockReminderDoses
import com.example.project_lttbd.data.remote.FirebaseProvider

/**
 * Receiver tự động khôi phục lịch báo thức nhắc thuốc khi thiết bị khởi động lại.
 * Android sẽ tự động xóa sạch các AlarmManager sau reboot, nên cần thiết lập lại.
 */
class MedicationBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MedicationBootReceiver"
        const val ACTION_TEST_BOOT = "com.example.project_lttbd.ACTION_TEST_BOOT"

        /**
         * Kiểm tra xem một liều thuốc có đủ điều kiện để khôi phục báo thức hay không:
         * - Chưa hoàn thành (chưa uống và chưa bỏ qua)
         * - Thời gian uống dự kiến còn ở tương lai so với thời điểm hiện tại
         */
        fun shouldRestoreDose(
            isFinished: Boolean,
            scheduledAtMillis: Long,
            nowMillis: Long = System.currentTimeMillis()
        ): Boolean {
            return !isFinished && scheduledAtMillis > nowMillis
        }

        /**
         * Khôi phục toàn bộ báo thức cho các liều thuốc PENDING trong ngày của [uid].
         * @return Số lượng báo thức được khôi phục thành công.
         */
        fun restoreAlarms(
            context: Context,
            uid: String,
            nowMillis: Long = System.currentTimeMillis()
        ): Int {
            if (uid.isBlank()) return 0

            var restoredCount = 0
            val today = MockCalendar.today()
            val doses = MockReminderDoses.forDate(today)

            for (dose in doses) {
                val isFinished = MockDoseActions.isFinished(dose.doseId)
                if (shouldRestoreDose(isFinished, dose.scheduledAtMillis, nowMillis)) {
                    MedicationAlarmScheduler.schedule(
                        context = context,
                        uid = uid,
                        doseId = dose.doseId,
                        triggerAtMillis = dose.scheduledAtMillis,
                        title = "Đến giờ uống thuốc: ${dose.medicineName}",
                        message = "${dose.quantityText}. ${dose.mealInstruction}".trim(),
                        medicineName = dose.medicineName
                    )
                    restoredCount++
                }
            }

            Log.d(TAG, "Đã khôi phục $restoredCount báo thức cho người dùng $uid sau khởi động máy.")
            return restoredCount
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != ACTION_TEST_BOOT
        ) {
            return
        }

        val extraUid = intent.getStringExtra("extra_uid")?.ifBlank { null }
        val uid = extraUid ?: FirebaseProvider.auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            Log.d(TAG, "Thiết bị khởi động xong nhưng chưa có người dùng đăng nhập. Bỏ qua.")
            return
        }

        Log.d(TAG, "Thiết bị khởi động xong (hoặc nhận lệnh test boot), tiến hành khôi phục lịch báo thức cho UID: $uid")
        restoreAlarms(context, uid)
    }
}
