package com.example.project_lttbd.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.project_lttbd.MainActivity
import com.example.project_lttbd.R
import com.example.project_lttbd.ui.navigation.MMRADestinations

object NotificationHelper {
    const val CHANNEL_ID_REMINDERS = "mmra_medication_reminders"
    const val CHANNEL_NAME_REMINDERS = "Nhắc nhở uống thuốc MMRA"
    const val CHANNEL_DESC_REMINDERS = "Thông báo giờ uống thuốc và cảnh báo sức khỏe"

    const val EXTRA_DESTINATION = "extra_destination"
    const val EXTRA_DOSE_ID = "extra_dose_id"

    fun resolveDestination(data: Map<String, String>): String {
        return data["destination"]?.ifBlank { null } ?: MMRADestinations.HOME
    }

    fun extractDoseId(data: Map<String, String>): String? {
        return data["doseId"]?.ifBlank { null }
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                CHANNEL_NAME_REMINDERS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_REMINDERS
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ) {
        ensureNotificationChannel(context)

        val destination = resolveDestination(data)
        val doseId = extractDoseId(data)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, destination)
            doseId?.let { putExtra(EXTRA_DOSE_ID, it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.chuong)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        notificationManager.notify(notificationId, notification)
    }
}
