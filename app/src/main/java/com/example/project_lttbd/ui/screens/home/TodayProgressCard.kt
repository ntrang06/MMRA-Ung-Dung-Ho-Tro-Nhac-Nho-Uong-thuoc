package com.example.project_lttbd.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TodayProgressCard(
    takenDoses: Int,
    totalDoses: Int,
    onTimeDoses: Int,
    modifier: Modifier = Modifier,
    skippedDoses: Int = 0
) {
    val total = totalDoses.coerceAtLeast(0)
    val taken = takenDoses.coerceIn(0, total)
    val skipped = skippedDoses.coerceIn(0, total - taken)
    val onTime = onTimeDoses.coerceIn(0, taken)
    val remaining = total - taken - skipped

    val title = when {
        total == 0 -> "Hôm nay chưa có lịch uống"
        taken == total -> "Đã hoàn thành lịch hôm nay"
        remaining == 0 -> "Không còn liều chờ xử lý"
        else -> "Tiến độ uống thuốc hôm nay"
    }
    val description = when {
        total == 0 -> "Thêm lịch uống để theo dõi tại đây."

        skipped > 0 ->
            "Đã uống $taken liều, bỏ qua $skipped liều. " +
                    "Còn $remaining liều."

        taken == total -> "Đã uống đủ $total liều trong ngày."

        else -> "Đã uống $taken liều. Còn $remaining liều trong ngày."
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFD7E2DF))
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
                    )
                    .border(
                        width = 6.dp,
                        color = Color(0xFF287A4D),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
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