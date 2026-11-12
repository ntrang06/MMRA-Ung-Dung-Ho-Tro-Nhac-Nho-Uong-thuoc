package com.example.project_lttbd.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.project_lttbd.MainActivity
import com.example.project_lttbd.R
import com.example.project_lttbd.ui.navigation.MMRADestinations

object NotificationHelper {
    const val CHANNEL_ID_REMINDERS = "mmra_medication_reminders"
    const val CHANNEL_NAME_REMINDERS = "Nhắc nhở uống thuốc MMRA"
    const val CHANNEL_DESC_REMINDERS = "Thông báo giờ uống thuốc và cảnh báo sức khỏe"

    const val NOTIFICATION_TAG_REMINDER = "MMRA_DOSE_REMINDER"

    const val EXTRA_DESTINATION = "extra_destination"
    const val EXTRA_DOSE_ID = "extra_dose_id"
    const val EXTRA_UID = "extra_uid"

    fun resolveDestination(data: Map<String, String>): String {
        val doseId = extractDoseId(data)
        if (!doseId.isNullOrBlank()) {
            return "reminder/$doseId?readOnly=false"
        }
        return data["destination"]?.ifBlank { null } ?: MMRADestinations.HOME
    }

    fun extractDoseId(data: Map<String, String>): String? {
        return data["doseId"]?.ifBlank { null }
    }

    fun notificationIdForDose(doseId: String): Int {
        val hash = doseId.hashCode()
        return if (hash == Int.MIN_VALUE) 0 else Math.abs(hash)
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val existing = notificationManager.getNotificationChannel(CHANNEL_ID_REMINDERS)
            if (existing == null) {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID_REMINDERS,
                    CHANNEL_NAME_REMINDERS,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC_REMINDERS
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 400, 200, 400)
                    setSound(soundUri, audioAttributes)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun showMedicationReminderNotification(
        context: Context,
        uid: String,
        doseId: String,
        title: String,
        message: String
    ) {
        ensureNotificationChannel(context)

        val destination = "reminder/$doseId?readOnly=false"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, destination)
            putExtra(EXTRA_DOSE_ID, doseId)
            putExtra(EXTRA_UID, uid)
        }

        val notifId = notificationIdForDose(doseId)

        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .build()

        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_TAG_REMINDER, notifId, notification)
            Log.d("NotificationHelper", "Đã gửi thông báo thành công: tag=$NOTIFICATION_TAG_REMINDER, id=$notifId, title=$title")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Lỗi khi gửi thông báo", e)
        }
    }

    fun cancelNotification(context: Context, doseId: String) {
        if (doseId.isBlank()) return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_TAG_REMINDER, notificationIdForDose(doseId))
    }

    fun cancelAllNotifications(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
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
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = (System.currentTimeMillis() % 100000).toInt()
            notificationManager.notify(notificationId, notification)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Lỗi khi gửi thông báo chung", e)
        }
    }
}
