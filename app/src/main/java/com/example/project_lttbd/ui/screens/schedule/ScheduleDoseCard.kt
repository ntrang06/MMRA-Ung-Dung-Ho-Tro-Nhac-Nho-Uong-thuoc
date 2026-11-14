package com.example.project_lttbd.ui.screens.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.data.remote.dto.DoseResponseDto
import com.example.project_lttbd.data.remote.dto.DoseStatusDto
import com.example.project_lttbd.data.remote.dto.DoseUnitDto
import com.example.project_lttbd.notifications.sync.MedicationReminderSync

enum class ScheduleDoseStatus(
    val label: String,
    val symbol: String,
    val foreground: Color,
    val background: Color
) {
    TAKEN(
        "ĐÃ UỐNG",
        "✓",
        Color(0xFF287A4D),
        Color(0xFFE3F4E9)
    ),
    PENDING(
        "CHỜ UỐNG",
        "◷",
        Color(0xFFAA5B00),
        Color(0xFFFFF1D6)
    ),
    OVERDUE(
        "QUÁ GIỜ",
        "!",
        Color(0xFFBA3028),
        Color(0xFFFBE6E3)
    ),
    UNCONFIRMED(
        "CHỜ XÁC NHẬN",
        "◷",
        Color(0xFFAA5B00),
        Color(0xFFFFF1D6)
    ),
    SNOOZED(
        "NHẮC LẠI",
        "◷",
        Color(0xFFAA5B00),
        Color(0xFFFFF1D6)
    ),
    SKIPPED(
        "ĐÃ BỎ QUA",
        "×",
        Color(0xFFBA3028),
        Color(0xFFFBE6E3)
    ),
    MISSED(
        "BỎ LỠ",
        "!",
        Color(0xFFBA3028),
        Color(0xFFFBE6E3)
    ),
    CANCELLED(
        "ĐÃ HỦY",
        "−",
        Color(0xFF526565),
        Color(0xFFEEF5F3)
    )
}

data class ScheduleDoseUiModel(
    val doseId: String,
    val medicineId: String,
    val time: String,
    val medicineName: String,
    val quantityText: String,
    val instruction: String,
    val status: ScheduleDoseStatus
)

/**
 * Trạng thái đã uống, bỏ qua, bỏ lỡ và hủy lấy từ máy chủ.
 * Liều PENDING đã đến giờ chỉ đổi nhãn hiển thị thành chờ xác nhận.
 */
internal fun DoseResponseDto.toScheduleDoseUiModel(
    nowMillis: Long = System.currentTimeMillis()
): ScheduleDoseUiModel {
    val snapshot = medicineSnapshot

    val strengthText = listOf(
        snapshot.strength.trim(),
        snapshot.strengthUnit.trim()
    )
        .filter { it.isNotEmpty() }
        .joinToString(" ")
        .takeIf {
            snapshot.strength.isNotBlank()
        }
        .orEmpty()

    val displayName = listOf(
        snapshot.medicineName.trim(),
        strengthText
    )
        .filter { it.isNotEmpty() }
        .joinToString(" ")
        .ifBlank { "Thuốc" }

    val unitText = when (doseUnit) {
        DoseUnitDto.TABLET -> "viên"
        DoseUnitDto.SACHET -> "gói"
        DoseUnitDto.ML -> "ml"
        DoseUnitDto.DROP -> "giọt"
        DoseUnitDto.DOSE -> "liều"
    }

    val quantity = doseQuantity.trim()

    val displayQuantity = quantity
        .replace(',', '.')
        .toBigDecimalOrNull()
        ?.stripTrailingZeros()
        ?.toPlainString()
        ?.replace('.', ',')
        ?: quantity

    val displayTime = if (minuteOfDay in 0..1439) {
        val hour = (minuteOfDay / 60)
            .toString()
            .padStart(2, '0')

        val minute = (minuteOfDay % 60)
            .toString()
            .padStart(2, '0')

        "$hour:$minute"
    } else {
        "—"
    }

    val displayStatus = when (status) {
        DoseStatusDto.TAKEN ->
            ScheduleDoseStatus.TAKEN

        DoseStatusDto.SKIPPED ->
            ScheduleDoseStatus.SKIPPED

        DoseStatusDto.MISSED ->
            ScheduleDoseStatus.MISSED

        DoseStatusDto.CANCELLED ->
            ScheduleDoseStatus.CANCELLED

        DoseStatusDto.PENDING -> {
            val scheduledMillis =
                MedicationReminderSync.parseIsoToMillis(scheduledAt)

            when {
                !nextReminderAt.isNullOrBlank() ->
                    ScheduleDoseStatus.SNOOZED

                scheduledMillis > 0L && scheduledMillis <= nowMillis ->
                    ScheduleDoseStatus.UNCONFIRMED

                else ->
                    ScheduleDoseStatus.PENDING
            }
        }
    }

    return ScheduleDoseUiModel(
        doseId = doseId,
        medicineId = medicineId,
        time = displayTime,
        medicineName = displayName,
        quantityText = "$displayQuantity $unitText",
        instruction = mealInstruction.trim(),
        status = displayStatus
    )
}

@Composable
fun ScheduleDoseCard(
    dose: ScheduleDoseUiModel,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val status = dose.status

    val highlighted = status == ScheduleDoseStatus.OVERDUE ||
            status == ScheduleDoseStatus.MISSED

    Surface(
        onClick = {
            onClick(dose.doseId)
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(
            width = 1.dp,
            color = if (highlighted) {
                status.foreground
            } else {
                Color(0xFFD7E2DF)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 96.dp)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = status.background
            ) {
                Column(
                    modifier = Modifier
                        .size(60.dp)
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        2.dp,
                        Alignment.CenterVertically
                    )
                ) {
                    ScheduleStatusIcon(
                        status = status,
                        modifier = Modifier.size(18.dp)
                    )

                    Text(
                        text = dose.time,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 15.sp,
                            lineHeight = 19.sp,
                            letterSpacing = 0.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = status.foreground
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = dose.medicineName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF102A2A)
                )

                Text(
                    text = dose.quantityText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF526565)
                )

                if (dose.instruction.isNotBlank()) {
                    Text(
                        text = dose.instruction,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            letterSpacing = 0.sp
                        ),
                        color = Color(0xFF526565)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = status.background
            ) {
                Text(
                    text = status.label,
                    modifier = Modifier.padding(
                        horizontal = 7.dp,
                        vertical = 5.dp
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = status.foreground
                )
            }

            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "›",
                    fontSize = 24.sp,
                    color = Color(0xFF526565)
                )
            }
        }
    }
}

@Composable
private fun ScheduleStatusIcon(
    status: ScheduleDoseStatus,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val color = status.foreground
        val stroke = 1.8.dp.toPx()

        fun line(
            x1: Float,
            y1: Float,
            x2: Float,
            y2: Float
        ) {
            drawLine(
                color = color,
                start = Offset(
                    size.width * x1,
                    size.height * y1
                ),
                end = Offset(
                    size.width * x2,
                    size.height * y2
                ),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }

        when (status) {
            ScheduleDoseStatus.TAKEN -> {
                line(0.2f, 0.52f, 0.42f, 0.73f)
                line(0.42f, 0.73f, 0.8f, 0.28f)
            }

            ScheduleDoseStatus.SKIPPED -> {
                line(0.25f, 0.25f, 0.75f, 0.75f)
                line(0.75f, 0.25f, 0.25f, 0.75f)
            }

            ScheduleDoseStatus.OVERDUE,
            ScheduleDoseStatus.MISSED -> {
                val triangle = Path().apply {
                    moveTo(
                        size.width * 0.5f,
                        size.height * 0.12f
                    )
                    lineTo(
                        size.width * 0.88f,
                        size.height * 0.82f
                    )
                    lineTo(
                        size.width * 0.12f,
                        size.height * 0.82f
                    )
                    close()
                }

                drawPath(
                    triangle,
                    color,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                line(0.5f, 0.38f, 0.5f, 0.54f)

                drawCircle(
                    color,
                    radius = 0.9.dp.toPx(),
                    center = Offset(
                        size.width * 0.5f,
                        size.height * 0.67f
                    )
                )
            }

            ScheduleDoseStatus.CANCELLED -> {
                line(0.25f, 0.5f, 0.75f, 0.5f)
            }

            ScheduleDoseStatus.PENDING,
            ScheduleDoseStatus.UNCONFIRMED,
            ScheduleDoseStatus.SNOOZED -> {
                drawCircle(
                    color,
                    radius = size.minDimension * 0.34f,
                    style = Stroke(width = stroke)
                )

                line(0.5f, 0.5f, 0.5f, 0.28f)
                line(0.5f, 0.5f, 0.68f, 0.54f)
            }
        }
    }
}