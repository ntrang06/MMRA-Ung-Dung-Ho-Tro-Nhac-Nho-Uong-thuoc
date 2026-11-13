package com.example.project_lttbd.notifications.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import org.json.JSONObject

object MedicationAlarmScheduler {

    private const val TAG = "MedicationAlarmSched"
    private const val PREFS_NAME = "mmra_scheduled_doses_prefs"
    private const val PAYLOAD_PREFIX = "alarm_payload_"

    const val ACTION_MEDICATION_ALARM =
        "com.example.project_lttbd.ACTION_MEDICATION_ALARM"

    const val EXTRA_UID = "extra_alarm_uid"
    const val EXTRA_DOSE_ID = "extra_alarm_dose_id"
    const val EXTRA_TITLE = "extra_alarm_title"
    const val EXTRA_MESSAGE = "extra_alarm_message"
    const val EXTRA_MEDICINE_NAME = "extra_alarm_medicine_name"

    private val apiDosePattern = Regex("^[0-9a-f]{64}$")

    private val demoDosePattern = Regex(
        """^medicine_0[123]\|\d{4}-\d{2}-\d{2}\|\d{1,4}$"""
    )

    private data class SavedAlarm(
        val uid: String,
        val doseId: String,
        val triggerAtMillis: Long,
        val title: String,
        val message: String,
        val medicineName: String
    )

    fun requestCodeForDose(
        doseId: String
    ): Int {
        val hash = doseId.hashCode()
        return if (hash == Int.MIN_VALUE) {
            0
        } else {
            Math.abs(hash)
        }
    }

    fun isValidScheduleTime(
        triggerAtMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        return triggerAtMillis > nowMillis
    }

    fun computeSnoozeTriggerMillis(
        nowMillis: Long,
        snoozeMinutes: Int
    ): Long {
        return nowMillis + snoozeMinutes * 60 * 1000L
    }

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
        schedule(
            context = context,
            uid = uid,
            doseId = doseId,
            triggerAtMillis = computeSnoozeTriggerMillis(
                nowMillis,
                snoozeMinutes
            ),
            title = title,
            message = message,
            medicineName = medicineName
        )
    }

    @Synchronized
    fun schedule(
        context: Context,
        uid: String,
        doseId: String,
        triggerAtMillis: Long,
        title: String,
        message: String,
        medicineName: String = ""
    ) {
        scheduleSavedAlarm(
            context,
            SavedAlarm(
                uid = uid,
                doseId = doseId,
                triggerAtMillis = triggerAtMillis,
                title = title,
                message = message,
                medicineName = medicineName
            )
        )
    }

    private fun scheduleSavedAlarm(
        context: Context,
        alarm: SavedAlarm
    ): Boolean {
        if (alarm.uid.isBlank() || alarm.doseId.isBlank()) {
            return false
        }

        if (!isValidScheduleTime(alarm.triggerAtMillis)) {
            return false
        }

        // Chặn các báo thức thuốc mẫu cũ.
        if (demoDosePattern.matches(alarm.doseId)) {
            cancel(context, alarm.doseId)
            return false
        }

        val manager = context.getSystemService(
            Context.ALARM_SERVICE
        ) as? AlarmManager ?: return false

        return try {
            val intent = Intent(
                context,
                MedicationAlarmReceiver::class.java
            ).apply {
                action = ACTION_MEDICATION_ALARM

                putExtra(EXTRA_UID, alarm.uid)
                putExtra(EXTRA_DOSE_ID, alarm.doseId)
                putExtra(EXTRA_TITLE, alarm.title)
                putExtra(EXTRA_MESSAGE, alarm.message)
                putExtra(
                    EXTRA_MEDICINE_NAME,
                    alarm.medicineName
                )
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCodeForDose(alarm.doseId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (manager.canScheduleExactAlarms()) {
                    manager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarm.triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    manager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarm.triggerAtMillis,
                        pendingIntent
                    )
                }
            } else if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            ) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarm.triggerAtMillis,
                    pendingIntent
                )
            } else {
                manager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    alarm.triggerAtMillis,
                    pendingIntent
                )
            }

            saveAlarm(context, alarm)

            Log.d(
                TAG,
                "Đã đặt và lưu báo thức liều thuốc."
            )

            true
        } catch (error: Exception) {
            Log.e(
                TAG,
                "Chưa đặt được báo thức liều thuốc.",
                error
            )

            false
        }
    }

    @Synchronized
    fun cancel(
        context: Context,
        doseId: String
    ) {
        if (doseId.isBlank()) return

        try {
            val manager = context.getSystemService(
                Context.ALARM_SERVICE
            ) as? AlarmManager

            val intent = Intent(
                context,
                MedicationAlarmReceiver::class.java
            ).apply {
                action = ACTION_MEDICATION_ALARM
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCodeForDose(doseId),
                intent,
                PendingIntent.FLAG_NO_CREATE or
                        PendingIntent.FLAG_IMMUTABLE
            )

            if (pendingIntent != null) {
                manager?.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (error: Exception) {
            Log.e(
                TAG,
                "Chưa hủy được báo thức trên hệ thống.",
                error
            )
        }

        // Xóa cả dữ liệu khôi phục để liều đã xử lý
        // không được đặt báo thức lại sau khi khởi động máy.
        val prefs = getPrefs(context)

        val editor = prefs.edit()
            .remove(PAYLOAD_PREFIX + doseId)

        prefs.all.keys
            .filter {
                it.startsWith("doses_")
            }
            .forEach { key ->
                val ids = prefs.getStringSet(
                    key,
                    emptySet()
                ).orEmpty()

                if (doseId in ids) {
                    editor.putStringSet(
                        key,
                        ids - doseId
                    )
                }
            }

        editor.apply()
    }

    @Synchronized
    fun cancelAll(
        context: Context,
        uid: String
    ) {
        if (uid.isBlank()) return

        getScheduledDosesForUid(
            context,
            uid
        ).forEach { doseId ->
            cancel(context, doseId)
        }

        getPrefs(context).edit()
            .remove("doses_$uid")
            .apply()
    }

    @Synchronized
    fun getScheduledDosesForUid(
        context: Context,
        uid: String
    ): Set<String> {
        if (uid.isBlank()) return emptySet()

        return getPrefs(context)
            .getStringSet(
                "doses_$uid",
                emptySet()
            )
            .orEmpty()
            .toSet()
    }

    @Synchronized
    fun restoreSavedAlarms(
        context: Context,
        uid: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        if (uid.isBlank()) return 0

        var restoredCount = 0

        getScheduledDosesForUid(
            context,
            uid
        ).forEach { doseId ->
            // Chỉ khôi phục ID liều đúng cấu trúc API.
            if (!apiDosePattern.matches(doseId)) {
                return@forEach
            }

            val alarm = readAlarm(
                context,
                uid,
                doseId
            ) ?: return@forEach

            if (
                !isValidScheduleTime(
                    alarm.triggerAtMillis,
                    nowMillis
                )
            ) {
                cancel(context, doseId)
                return@forEach
            }

            if (scheduleSavedAlarm(context, alarm)) {
                restoredCount++
            }
        }

        return restoredCount
    }

    private fun getPrefs(
        context: Context
    ) = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private fun saveAlarm(
        context: Context,
        alarm: SavedAlarm
    ) {
        val prefs = getPrefs(context)

        val ids = prefs.getStringSet(
            "doses_${alarm.uid}",
            emptySet()
        )
            .orEmpty()
            .toSet()

        val payload = JSONObject()
            .put("uid", alarm.uid)
            .put("doseId", alarm.doseId)
            .put("triggerAtMillis", alarm.triggerAtMillis)
            .put("title", alarm.title)
            .put("message", alarm.message)
            .put("medicineName", alarm.medicineName)

        prefs.edit()
            .putStringSet(
                "doses_${alarm.uid}",
                ids + alarm.doseId
            )
            .putString(
                PAYLOAD_PREFIX + alarm.doseId,
                payload.toString()
            )
            .apply()
    }

    private fun readAlarm(
        context: Context,
        uid: String,
        doseId: String
    ): SavedAlarm? {
        val text = getPrefs(context)
            .getString(
                PAYLOAD_PREFIX + doseId,
                null
            )
            ?: return null

        return runCatching {
            val payload = JSONObject(text)

            if (
                payload.optString("uid") != uid ||
                payload.optString("doseId") != doseId
            ) {
                return@runCatching null
            }

            SavedAlarm(
                uid = uid,
                doseId = doseId,
                triggerAtMillis = payload.optLong(
                    "triggerAtMillis",
                    0L
                ),
                title = payload.optString(
                    "title",
                    "Nhắc uống thuốc"
                ),
                message = payload.optString(
                    "message",
                    ""
                ),
                medicineName = payload.optString(
                    "medicineName",
                    ""
                )
            )
        }.getOrNull()
    }
}