package com.example.project_lttbd.notifications

import android.util.Log
import com.example.project_lttbd.data.device.DeviceManager
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MMRAFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d(
            TAG,
            "Đã nhận token FCM mới."
        )

        val uid = FirebaseProvider.auth.currentUser?.uid

        if (!uid.isNullOrBlank()) {
            // Đồng bộ đầy đủ để tạo document nếu chưa có.
            DeviceManager.syncCurrentDevice(
                applicationContext,
                uid
            )
        }
    }

    override fun onMessageReceived(
        remoteMessage: RemoteMessage
    ) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Nhắc nhở từ MMRA"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "Bạn có thông báo mới về lịch dùng thuốc."

        NotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            body = body,
            data = remoteMessage.data
        )
    }

    companion object {
        private const val TAG = "MMRAFirebaseMsgService"
    }
}