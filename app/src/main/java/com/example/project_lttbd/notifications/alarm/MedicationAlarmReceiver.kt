package com.example.project_lttbd.notifications.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.notifications.NotificationHelper
import com.example.project_lttbd.notifications.model.LocalNotificationItem
import com.example.project_lttbd.notifications.storage.LocalNotificationStore
import java.util.UUID

/**
 * BroadcastReceiver nhận sự kiện báo thức từ AlarmManager khi đến giờ uống thuốc.
 * Tạo thông báo hệ thống và lưu trữ vào hộp thư thông báo cục bộ của UID tương ứng.
 */
class MedicationAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MedAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.action != MedicationAlarmScheduler.ACTION_MEDICATION_ALARM) {
            return
        }

        val uid = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_UID).orEmpty()
        val doseId = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_DOSE_ID).orEmpty()
        val title = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_TITLE)
            ?.ifBlank { "Đến giờ uống thuốc" } ?: "Đến giờ uống thuốc"
        val message = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_MESSAGE)
            ?.ifBlank { "Vui lòng kiểm tra và uống thuốc đúng giờ." } ?: "Vui lòng kiểm tra và uống thuốc đúng giờ."
        val medicineName = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_MEDICINE_NAME).orEmpty()

        if (uid.isBlank() || doseId.isBlank()) {
            Log.w(TAG, "Bỏ qua báo thức vì thiếu UID hoặc DoseID")
            return
        }

        // Kiểm tra phiên đăng nhập hiện tại: Chỉ báo chuông nếu người dùng hiện tại đúng là UID này
        val currentUid = FirebaseProvider.auth.currentUser?.uid
        if (currentUid != null && currentUid != uid) {
            Log.w(TAG, "Bỏ qua báo thức của UID $uid vì tài khoản hiện tại là $currentUid")
            return
        }

        Log.d(TAG, "Đang phát thông báo nhắc thuốc: doseId=$doseId, medicine=$medicineName")

        // 1. Hiển thị thông báo trên thanh trạng thái
        NotificationHelper.showMedicationReminderNotification(
            context = context,
            uid = uid,
            doseId = doseId,
            title = title,
            message = message
        )

        // 2. Lưu vào lịch sử thông báo cục bộ theo UID
        val notifItem = LocalNotificationItem(
            id = UUID.randomUUID().toString(),
            doseId = doseId,
            title = title,
            body = message,
            timestampMillis = System.currentTimeMillis(),
            isRead = false,
            destinationRoute = "reminder/$doseId?readOnly=false"
        )
        LocalNotificationStore.addNotification(context, uid, notifItem)
    }
}
