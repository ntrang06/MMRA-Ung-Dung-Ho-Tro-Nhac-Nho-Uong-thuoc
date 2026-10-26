package com.example.project_lttbd.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mmra_medicationmanager.R

enum class HomeDoseStatus {
    TAKEN,
    NEXT,
    UPCOMING,
    SNOOZED,
    SKIPPED
}

data class HomeDoseUiModel(
    val doseId: String,
    val time: String,
    val medicineName: String,
    val instruction: String,
    val status: HomeDoseStatus
)

@Composable
fun TodayScheduleSection(
    doses: List<HomeDoseUiModel>,
    onViewAllClick: () -> Unit,
    onDoseClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lịch uống hôm nay",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF102A2A)
            )

            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onViewAllClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Xem tất cả",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color(0xFF126B62)
                )
            }
        }

        if (doses.isEmpty()) {
            Text(
                text = "Bạn chưa có lịch uống thuốc hôm nay.",
                modifier = Modifier.padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF526565)
            )
        } else {
            doses.forEach { dose ->
                TodayDoseRow(
                    dose = dose,
                    onClick = { onDoseClick(dose.doseId) }
                )
            }
        }
    }
}

@Composable
private fun TodayDoseRow(
    dose: HomeDoseUiModel,
    onClick: () -> Unit
) {
    val statusColor = when (dose.status) {
        HomeDoseStatus.TAKEN -> Color(0xFF287A4D)
        HomeDoseStatus.NEXT -> Color(0xFF126B62)
        HomeDoseStatus.UPCOMING -> Color(0xFF526565)
        HomeDoseStatus.SNOOZED -> Color(0xFFAA5B00)
        HomeDoseStatus.SKIPPED -> Color(0xFF526565)
    }

    val statusText = when (dose.status) {
        HomeDoseStatus.TAKEN -> "Đã uống"
        HomeDoseStatus.NEXT -> "Tiếp theo"
        HomeDoseStatus.UPCOMING -> "Sắp tới"
        HomeDoseStatus.SNOOZED -> "Nhắc lại"
        HomeDoseStatus.SKIPPED -> "Đã bỏ qua"
    }

    val iconBackground = when (dose.status) {
        HomeDoseStatus.TAKEN -> Color(0xFFE3F4E9)
        HomeDoseStatus.NEXT -> Color(0xFF126B62)
        HomeDoseStatus.UPCOMING -> Color(0xFFEEF5F3)
        HomeDoseStatus.SNOOZED -> Color(0xFFFFF1D6)
        HomeDoseStatus.SKIPPED -> Color(0xFFEEF5F3)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dose.time,
            modifier = Modifier.width(46.dp),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 15.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.sp,
                fontWeight = FontWeight.Bold
            ),
            color = if (dose.status == HomeDoseStatus.NEXT) {
                Color(0xFF126B62)
            } else {
                Color(0xFF526565)
            }
        )

        Box(
            modifier = Modifier
                .size(26.dp)
                .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (dose.status == HomeDoseStatus.TAKEN) {
                TakenCheckIcon(
                    modifier = Modifier.size(14.dp),
                    color = statusColor
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.thuoc),
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = if (dose.status == HomeDoseStatus.NEXT) {
                        Color.White
                    } else {
                        statusColor
                    }
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = dose.medicineName,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF102A2A)
            )

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

        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.sp,
                fontWeight = FontWeight.Medium
            ),
            color = statusColor
        )
    }
}

@Composable
private fun TakenCheckIcon(
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        drawLine(
            color = color,
            start = Offset(size.width * 0.2f, size.height * 0.5f),
            end = Offset(size.width * 0.42f, size.height * 0.72f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawLine(
            color = color,
            start = Offset(size.width * 0.42f, size.height * 0.72f),
            end = Offset(size.width * 0.8f, size.height * 0.28f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}