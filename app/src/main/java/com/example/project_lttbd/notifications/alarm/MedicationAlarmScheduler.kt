package com.example.project_lttbd.notifications.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Đặt và quản lý báo thức chính xác bằng Android AlarmManager.
 * Hỗ trợ setExactAndAllowWhileIdle để đảm bảo nổ chuông ngay cả trong chế độ Doze / tiết kiệm pin.
 */
object MedicationAlarmScheduler {

    private const val TAG = "MedicationAlarmSched"
    const val ACTION_MEDICATION_ALARM = "com.example.project_lttbd.ACTION_MEDICATION_ALARM"

    const val EXTRA_UID = "extra_alarm_uid"
    const val EXTRA_DOSE_ID = "extra_alarm_dose_id"
    const val EXTRA_TITLE = "extra_alarm_title"
    const val EXTRA_MESSAGE = "extra_alarm_message"
    const val EXTRA_MEDICINE_NAME = "extra_alarm_medicine_name"

    private const val PREFS_NAME = "mmra_scheduled_doses_prefs"

    fun requestCodeForDose(doseId: String): Int {
        // Sử dụng mã hash dương ổn định
        val hash = doseId.hashCode()
        return if (hash == Int.MIN_VALUE) 0 else Math.abs(hash)
    }

    fun isValidScheduleTime(triggerAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean {
        return triggerAtMillis > nowMillis
    }

    fun computeSnoozeTriggerMillis(nowMillis: Long, snoozeMinutes: Int): Long {
        return nowMillis + (snoozeMinutes * 60 * 1000L)
    }

    /**
     * Lập lịch hoãn báo thức (Snooze) sau [snoozeMinutes] phút (hỗ trợ 5, 10, 15, 30 phút theo chuẩn BE2/BE3).
     */
    fun scheduleSnooze(
        context: Context,
        uid: String,
        doseId: String,
        snoozeMinutes: Int,
        title: String,
        message: String,
        medicineName: String = "",
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val triggerAtMillis = computeSnoozeTriggerMillis(nowMillis, snoozeMinutes)
        schedule(
            context = context,
            uid = uid,
            doseId = doseId,
            triggerAtMillis = triggerAtMillis,
            title = title,
            message = message,
            medicineName = medicineName
        )
    }

    fun schedule(
        context: Context,
        uid: String,
        doseId: String,
        triggerAtMillis: Long,
        title: String,
        message: String,
        medicineName: String = ""
    ) {
        if (!isValidScheduleTime(triggerAtMillis)) {
            Log.d(TAG, "Bỏ qua đặt báo thức vì thời gian đã qua: doseId=$doseId")
            return
        }
        if (uid.isBlank() || doseId.isBlank()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            action = ACTION_MEDICATION_ALARM
            putExtra(EXTRA_UID, uid)
            putExtra(EXTRA_DOSE_ID, doseId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeForDose(doseId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }

            trackScheduledDose(context, uid, doseId)
            Log.d(TAG, "Đã đặt báo thức cho liều $doseId lúc $triggerAtMillis (UID: $uid)")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi đặt báo thức cho liều $doseId", e)
        }
    }

    fun cancel(context: Context, doseId: String) {
        if (doseId.isBlank()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            action = ACTION_MEDICATION_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeForDose(doseId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Đã hủy báo thức cho liều $doseId")
        }

        untrackScheduledDose(context, doseId)
    }

    fun cancelAll(context: Context, uid: String) {
        if (uid.isBlank()) return
        val activeDoseIds = getScheduledDosesForUid(context, uid)
        for (doseId in activeDoseIds) {
            cancel(context, doseId)
        }
        clearTrackingForUid(context, uid)
        Log.d(TAG, "Đã hủy toàn bộ (${activeDoseIds.size}) báo thức của UID: $uid")
    }

    // --- Tracking helper methods ---

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun trackScheduledDose(context: Context, uid: String, doseId: String) {
        val prefs = getPrefs(context)
        val currentSet = prefs.getStringSet("doses_$uid", emptySet()) ?: emptySet()
        prefs.edit().putStringSet("doses_$uid", currentSet + doseId).apply()
    }

    private fun untrackScheduledDose(context: Context, doseId: String) {
        val prefs = getPrefs(context)
        val allKeys = prefs.all.keys.filter { it.startsWith("doses_") }
        val editor = prefs.edit()
        for (key in allKeys) {
            val currentSet = prefs.getStringSet(key, emptySet()) ?: emptySet()
            if (doseId in currentSet) {
                editor.putStringSet(key, currentSet - doseId)
            }
        }
        editor.apply()
    }

    fun getScheduledDosesForUid(context: Context, uid: String): Set<String> {
        return getPrefs(context).getStringSet("doses_$uid", emptySet()) ?: emptySet()
    }

    private fun clearTrackingForUid(context: Context, uid: String) {
        getPrefs(context).edit().remove("doses_$uid").apply()
    }
}
