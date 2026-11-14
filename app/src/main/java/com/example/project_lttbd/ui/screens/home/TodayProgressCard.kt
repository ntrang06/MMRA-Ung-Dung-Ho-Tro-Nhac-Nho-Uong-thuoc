package com.example.project_lttbd.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Suppress("UNUSED_PARAMETER")
@Composable
fun TodayProgressCard(
    takenDoses: Int,
    totalDoses: Int,
    onTimeDoses: Int = 0,
    modifier: Modifier = Modifier,
    skippedDoses: Int = 0,
    missedDoses: Int = 0
) {
    // Giữ onTimeDoses để tương thích với các chỗ đang gọi.
    // Thẻ này hiển thị tiến độ và trạng thái thực tế của các liều.
    val total = totalDoses.coerceAtLeast(0)

    val taken = takenDoses.coerceIn(
        minimumValue = 0,
        maximumValue = total
    )

    val skipped = skippedDoses.coerceIn(
        minimumValue = 0,
        maximumValue = total - taken
    )

    val missed = missedDoses.coerceIn(
        minimumValue = 0,
        maximumValue = total - taken - skipped
    )

    val remaining = total - taken - skipped - missed

    val progress = if (total > 0) {
        taken.toFloat() / total.toFloat()
    } else {
        0f
    }

    val title = when {
        total == 0 ->
            "Hôm nay chưa có lịch uống"

        taken == total ->
            "Đã hoàn thành lịch hôm nay"

        remaining == 0 ->
            "Không còn liều chờ xử lý"

        else ->
            "Tiến độ uống thuốc hôm nay"
    }

    val description = when {
        total == 0 ->
            "Thêm lịch uống để theo dõi tại đây."

        taken == total ->
            "Đã uống đủ $total liều trong ngày."

        else -> {
            val details = buildList {
                add("Đã uống $taken liều")

                if (skipped > 0) {
                    add("bỏ qua $skipped liều")
                }

                if (missed > 0) {
                    add("bỏ lỡ $missed liều")
                }
            }

            details.joinToString(", ") +
                    ". Còn $remaining liều."
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFD7E2DF)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .background(
                        color = Color(0xFFE3F4E9),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.size(86.dp)
                ) {
                    val strokeWidth = 6.dp.toPx()
                    val radius = (
                            size.minDimension - strokeWidth
                            ) / 2f

                    drawCircle(
                        color = Color(0xFFD7E2DF),
                        radius = radius,
                        center = center,
                        style = Stroke(
                            width = strokeWidth
                        )
                    )

                    if (progress > 0f) {
                        drawArc(
                            color = Color(0xFF287A4D),
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            topLeft = Offset(
                                x = strokeWidth / 2f,
                                y = strokeWidth / 2f
                            ),
                            size = Size(
                                width = size.width - strokeWidth,
                                height = size.height - strokeWidth
                            ),
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "$taken/$total",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            lineHeight = 26.sp,
                            letterSpacing = 0.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF287A4D)
                    )

                    Text(
                        text = "ĐÃ UỐNG",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            lineHeight = 12.sp,
                            letterSpacing = 0.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFF526565)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF102A2A)
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = Color(0xFF526565)
                )
            }
        }
    }
}