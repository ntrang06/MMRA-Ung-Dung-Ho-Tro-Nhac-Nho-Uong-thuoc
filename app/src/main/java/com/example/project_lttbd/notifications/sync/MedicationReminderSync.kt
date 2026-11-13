package com.example.project_lttbd.notifications.sync

import android.content.Context
import android.util.Log
import com.example.project_lttbd.data.remote.dto.DoseActionDto
import com.example.project_lttbd.data.remote.dto.DoseResponseDto
import com.example.project_lttbd.data.remote.dto.DoseStatusDto
import com.example.project_lttbd.notifications.NotificationHelper
import com.example.project_lttbd.notifications.alarm.MedicationAlarmScheduler
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Đồng bộ giữa dữ liệu liều thuốc và hệ thống báo thức cục bộ:
 * - Tự động lập lịch nhắc cho các liều PENDING.
 * - Tự động hủy báo thức và xóa notification khi liều chuyển sang TAKEN, SKIPPED, CANCELLED, MISSED.
 */
object MedicationReminderSync {

    private const val TAG = "MedicationReminderSync"

    fun shouldScheduleDose(dose: DoseResponseDto): Boolean {
        return dose.status == DoseStatusDto.PENDING
    }

    fun shouldCancelDose(dose: DoseResponseDto): Boolean {
        return dose.status != DoseStatusDto.PENDING
    }

    fun parseIsoToMillis(isoString: String): Long {
        if (isoString.isBlank()) return 0L

        // Hỗ trợ chuẩn ISO 8601 (có múi giờ hoặc kết thúc bằng Z)
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss"
        )

        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                if (pattern.endsWith("'Z'")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(isoString)
                if (date != null) return date.time
            } catch (e: Exception) {
                // Thử pattern tiếp theo
            }
        }
        return 0L
    }

    fun syncDoses(
        context: Context,
        uid: String,
        doses: List<DoseResponseDto>
    ) {
        if (uid.isBlank()) return

        val now = System.currentTimeMillis()
        var scheduledCount = 0
        var cancelledCount = 0

        for (dose in doses) {
            if (shouldScheduleDose(dose)) {
                // Ưu tiên thời gian nhắc lại nếu có (nextReminderAt)
                val targetIso = dose.nextReminderAt ?: dose.scheduledAt
                val triggerAtMillis = parseIsoToMillis(targetIso)

                if (triggerAtMillis > now) {
                    val medicineName = dose.medicineSnapshot.medicineName
                    val title = "Đến giờ uống thuốc: $medicineName"
                    val message = "Uống ${dose.doseQuantity} ${dose.doseUnit.name.lowercase(Locale.ROOT)}. ${dose.mealInstruction}".trim()

                    MedicationAlarmScheduler.schedule(
                        context = context,
                        uid = uid,
                        doseId = dose.doseId,
                        triggerAtMillis = triggerAtMillis,
                        title = title,
                        message = message,
                        medicineName = medicineName
                    )
                    scheduledCount++
                }
            } else if (shouldCancelDose(dose)) {
                MedicationAlarmScheduler.cancel(context, dose.doseId)
                NotificationHelper.cancelNotification(context, dose.doseId)
                cancelledCount++
            }
        }

        Log.d(TAG, "Đã đồng bộ liều thuốc cho UID $uid: $scheduledCount đặt mới, $cancelledCount đã hủy.")
    }

    fun onDoseAction(
        context: Context,
        doseId: String,
        action: DoseActionDto? = null
    ) {
        if (doseId.isBlank()) return
        Log.d(TAG, "Xử lý hành động liều $doseId ($action): Hủy báo thức và notification ngay lập tức.")
        MedicationAlarmScheduler.cancel(context, doseId)
        NotificationHelper.cancelNotification(context, doseId)
    }

    fun scheduleRawDose(
        context: Context,
        uid: String,
        doseId: String,
        scheduledAtMillis: Long,
        medicineName: String,
        quantityText: String,
        mealInstruction: String
    ) {
        if (scheduledAtMillis <= System.currentTimeMillis() || uid.isBlank() || doseId.isBlank()) return
        val title = "Đến giờ uống thuốc: $medicineName"
        val message = "$quantityText. $mealInstruction".trim()

        MedicationAlarmScheduler.schedule(
            context = context,
            uid = uid,
            doseId = doseId,
            triggerAtMillis = scheduledAtMillis,
            title = title,
            message = message,
            medicineName = medicineName
        )
    }
}
