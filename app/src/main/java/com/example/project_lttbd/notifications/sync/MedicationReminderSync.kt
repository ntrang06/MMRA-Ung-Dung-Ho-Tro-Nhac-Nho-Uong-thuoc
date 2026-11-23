package com.example.project_lttbd.notifications.sync

import android.content.Context
import android.util.Log
import com.example.project_lttbd.data.remote.DoseRepository
import com.example.project_lttbd.data.remote.dto.DoseActionDto
import com.example.project_lttbd.data.remote.dto.DoseResponseDto
import com.example.project_lttbd.data.remote.dto.DoseStatusDto
import com.example.project_lttbd.data.remote.dto.DoseUnitDto
import com.example.project_lttbd.notifications.NotificationHelper
import com.example.project_lttbd.notifications.alarm.MedicationAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object MedicationReminderSync {

    private const val TAG = "MedicationReminderSync"

    private val demoDosePattern = Regex(
        """^medicine_0[123]\|\d{4}-\d{2}-\d{2}\|\d{1,4}$"""
    )

    private val isoPattern = Regex(
        """^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d{1,9}))?(Z|[+-]\d{2}:\d{2})?$"""
    )

    fun shouldScheduleDose(
        dose: DoseResponseDto
    ): Boolean {
        return dose.status == DoseStatusDto.PENDING
    }

    fun shouldCancelDose(
        dose: DoseResponseDto
    ): Boolean {
        return dose.status != DoseStatusDto.PENDING
    }

    fun isDemoDoseId(
        doseId: String
    ): Boolean {
        return demoDosePattern.matches(doseId)
    }

    fun parseIsoToMillis(
        isoString: String
    ): Long {
        val match = isoPattern.matchEntire(
            isoString.trim()
        ) ?: return 0L

        val dateTime = match.groupValues[1]

        val milliseconds = match.groupValues[2]
            .take(3)
            .padEnd(3, '0')

        val offset = match.groupValues[3]

        val normalized = "$dateTime.$milliseconds$offset"

        val format = SimpleDateFormat(
            if (offset.isEmpty()) {
                "yyyy-MM-dd'T'HH:mm:ss.SSS"
            } else {
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
            },
            Locale.ROOT
        ).apply {
            isLenient = false
            timeZone = TimeZone.getDefault()
        }

        val position = ParsePosition(0)
        val parsed = format.parse(normalized, position)

        return if (
            parsed != null &&
            position.index == normalized.length
        ) {
            parsed.time
        } else {
            0L
        }
    }

    fun clearDemoAlarms(
        context: Context,
        uid: String
    ): Int {
        if (uid.isBlank()) return 0

        val demoIds = MedicationAlarmScheduler
            .getScheduledDosesForUid(context, uid)
            .filter {
                isDemoDoseId(it)
            }

        demoIds.forEach { doseId ->
            MedicationAlarmScheduler.cancel(
                context,
                doseId
            )

            NotificationHelper.cancelNotification(
                context,
                doseId
            )
        }

        if (demoIds.isNotEmpty()) {
            Log.d(
                TAG,
                "Đã dọn ${demoIds.size} báo thức thuốc mẫu."
            )
        }

        return demoIds.size
    }

    fun syncDoses(
        context: Context,
        uid: String,
        doses: List<DoseResponseDto>,
        queryStartMillis: Long? = null,
        queryEndMillis: Long? = null
    ) {
        if (uid.isBlank()) return

        clearDemoAlarms(context, uid)

        val now = System.currentTimeMillis()

        var scheduledCount = 0
        var cancelledCount = 0

        val activePendingDoseIds = mutableSetOf<String>()

        for (dose in doses) {
            if (isDemoDoseId(dose.doseId)) {
                continue
            }

            if (shouldCancelDose(dose)) {
                MedicationAlarmScheduler.cancel(
                    context,
                    dose.doseId
                )

                NotificationHelper.cancelNotification(
                    context,
                    dose.doseId
                )

                cancelledCount++
                continue
            }

            if (!shouldScheduleDose(dose)) {
                continue
            }

            val targetIso = dose.nextReminderAt
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: dose.scheduledAt

            val triggerAtMillis = parseIsoToMillis(
                targetIso
            )

            if (triggerAtMillis <= now) {
                continue
            }

            activePendingDoseIds.add(dose.doseId)

            val medicineName = medicineLabel(dose)

            val message = listOf(
                "Uống ${quantityLabel(dose)}",
                dose.mealInstruction.trim()
            )
                .filter {
                    it.isNotBlank()
                }
                .joinToString(". ")

            MedicationAlarmScheduler.schedule(
                context = context,
                uid = uid,
                doseId = dose.doseId,
                triggerAtMillis = triggerAtMillis,
                title = "Đến giờ uống thuốc: $medicineName",
                message = message,
                medicineName = medicineName
            )

            scheduledCount++
        }

        // Xác định khoảng thời gian đối soát chính xác
        val minMillis = queryStartMillis ?: doses.minOfOrNull {
            parseIsoToMillis(it.nextReminderAt?.takeIf { s -> s.isNotBlank() } ?: it.scheduledAt)
        }
        val maxMillis = queryEndMillis ?: doses.maxOfOrNull {
            parseIsoToMillis(it.nextReminderAt?.takeIf { s -> s.isNotBlank() } ?: it.scheduledAt)
        }

        if (minMillis != null && maxMillis != null) {
            val localScheduledIds = MedicationAlarmScheduler.getScheduledDosesForUid(context, uid)
            val fetchedDoseIds = doses.map { it.doseId }.toSet()

            localScheduledIds.forEach { localId ->
                if (localId in fetchedDoseIds && localId !in activePendingDoseIds) {
                    MedicationAlarmScheduler.cancel(context, localId)
                    NotificationHelper.cancelNotification(context, localId)
                    cancelledCount++
                } else if (localId !in fetchedDoseIds) {
                    val savedTrigger = MedicationAlarmScheduler.getSavedAlarmTriggerMillis(context, uid, localId)
                    if (savedTrigger in (minMillis - 60_000L)..(maxMillis + 60_000L)) {
                        MedicationAlarmScheduler.cancel(context, localId)
                        NotificationHelper.cancelNotification(context, localId)
                        cancelledCount++
                    }
                }
            }
        }

        Log.d(
            TAG,
            "Đã đồng bộ liều thuốc: $scheduledCount đặt nhắc, " +
                    "$cancelledCount hủy nhắc."
        )
    }

    fun triggerBackgroundResync(
        context: Context,
        uid: String
    ) {
        if (uid.isBlank()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = DoseRepository()
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

                val cal = Calendar.getInstance()
                val todayStr = format.format(cal.time)

                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val rangeStartMillis = cal.timeInMillis

                cal.add(Calendar.DAY_OF_YEAR, 2)
                val rangeEndMillis = cal.timeInMillis - 1L

                cal.add(Calendar.DAY_OF_YEAR, -1) // back to tomorrow
                val tomorrowStr = format.format(cal.time)

                val todayDoses = runCatching { repository.listDoses(todayStr) }.getOrNull().orEmpty()
                val tomorrowDoses = runCatching { repository.listDoses(tomorrowStr) }.getOrNull().orEmpty()

                val allDoses = todayDoses + tomorrowDoses
                syncDoses(
                    context = context,
                    uid = uid,
                    doses = allDoses,
                    queryStartMillis = rangeStartMillis,
                    queryEndMillis = rangeEndMillis
                )
                Log.d(TAG, "Đã tự động đồng bộ lại ${allDoses.size} liều thuốc từ Backend trong khoảng thời gian hôm nay & ngày mai.")
            } catch (e: Exception) {
                Log.w(TAG, "Tự động đồng bộ báo thức thất bại (Mạng hoặc API lỗi)", e)
            }
        }
    }

    fun onDoseAction(
        context: Context,
        doseId: String,
        action: DoseActionDto? = null
    ) {
        if (doseId.isBlank()) return

        MedicationAlarmScheduler.cancel(
            context,
            doseId
        )

        NotificationHelper.cancelNotification(
            context,
            doseId
        )

        Log.d(
            TAG,
            "Đã hủy nhắc sau thao tác liều thuốc: $action."
        )
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
        if (
            uid.isBlank() ||
            doseId.isBlank() ||
            isDemoDoseId(doseId) ||
            scheduledAtMillis <= System.currentTimeMillis()
        ) {
            return
        }

        val message = listOf(
            quantityText.trim(),
            mealInstruction.trim()
        )
            .filter {
                it.isNotBlank()
            }
            .joinToString(". ")

        MedicationAlarmScheduler.schedule(
            context = context,
            uid = uid,
            doseId = doseId,
            triggerAtMillis = scheduledAtMillis,
            title = "Đến giờ uống thuốc: $medicineName",
            message = message,
            medicineName = medicineName
        )
    }

    private fun quantityLabel(
        dose: DoseResponseDto
    ): String {
        val quantity = dose.doseQuantity.trim()
            .replace(',', '.')
            .toBigDecimalOrNull()
            ?.stripTrailingZeros()
            ?.toPlainString()
            ?.replace('.', ',')
            ?: dose.doseQuantity.trim()

        val unit = when (dose.doseUnit) {
            DoseUnitDto.TABLET -> "viên"
            DoseUnitDto.SACHET -> "gói"
            DoseUnitDto.ML -> "ml"
            DoseUnitDto.DROP -> "giọt"
            DoseUnitDto.DOSE -> "liều"
        }

        return "$quantity $unit"
    }

    private fun medicineLabel(
        dose: DoseResponseDto
    ): String {
        val snapshot = dose.medicineSnapshot

        val strength = if (snapshot.strength.isBlank()) {
            ""
        } else {
            listOf(
                snapshot.strength.trim(),
                snapshot.strengthUnit.trim()
            )
                .filter {
                    it.isNotBlank()
                }
                .joinToString(" ")
        }

        return listOf(
            snapshot.medicineName.trim(),
            strength
        )
            .filter {
                it.isNotBlank()
            }
            .joinToString(" ")
            .ifBlank {
                "Thuốc"
            }
    }
}
