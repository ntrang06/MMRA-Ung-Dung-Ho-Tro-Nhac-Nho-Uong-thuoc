package com.example.project_lttbd.ui.screens.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ScheduleWeekPicker(
    selectedDateMillis: Long,
    todayMillis: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDate = Calendar.getInstance().apply {
        timeInMillis = selectedDateMillis
    }

    // Tìm thứ Hai của tuần đang chọn.
    val monday = Calendar.getInstance().apply {
        timeInMillis = selectedDateMillis
        val daysFromMonday = (get(Calendar.DAY_OF_WEEK) + 5) % 7
        add(Calendar.DAY_OF_MONTH, -daysFromMonday)
    }

    val weekdayNames = listOf(
        "Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư",
        "Thứ Năm", "Thứ Sáu", "Thứ Bảy"
    )

    val weekday = weekdayNames[
        selectedDate.get(Calendar.DAY_OF_WEEK) - 1
    ]

    val dateText = SimpleDateFormat(
        "dd 'tháng' MM",
        Locale.forLanguageTag("vi-VN")
    ).format(selectedDate.time)

    val isToday = sameScheduleDay(selectedDateMillis, todayMillis)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFD7E2DF))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WeekArrow(
                    label = "Tuần trước",
                    symbol = "‹",
                    onClick = {
                        onDateSelected(
                            shiftScheduleDate(selectedDateMillis, -7)
                        )
                    }
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$weekday, $dateText",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            lineHeight = 22.sp,
                            letterSpacing = 0.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF102A2A),
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        onClick = { onDateSelected(todayMillis) },
                        color = Color.Transparent
                    ) {
                        Text(
                            text = if (isToday) "Hôm nay" else "Về hôm nay",
                            modifier = Modifier.padding(
                                horizontal = 4.dp,
                                vertical = 2.dp
                            ),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                letterSpacing = 0.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color(0xFF126B62)
                        )
                    }
                }

                WeekArrow(
                    label = "Tuần sau",
                    symbol = "›",
                    onClick = {
                        onDateSelected(
                            shiftScheduleDate(selectedDateMillis, 7)
                        )
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val dayLabels = listOf(
                    "T2", "T3", "T4", "T5", "T6", "T7", "CN"
                )

                repeat(7) { index ->
                    val dayMillis = shiftScheduleDate(
                        monday.timeInMillis,
                        index
                    )

                    val day = Calendar.getInstance().apply {
                        timeInMillis = dayMillis
                    }

                    val selected = sameScheduleDay(
                        dayMillis,
                        selectedDateMillis
                    )

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onDateSelected(dayMillis) }
                            )
                            .semantics {
                                contentDescription =
                                    "${dayLabels[index]}, ngày " +
                                            "${day.get(Calendar.DAY_OF_MONTH)}, " +
                                            "tháng ${day.get(Calendar.MONTH) + 1}, " +
                                            "năm ${day.get(Calendar.YEAR)}"
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) {
                            Color(0xFF126B62)
                        } else {
                            Color.Transparent
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .heightIn(min = 58.dp)
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(
                                3.dp,
                                Alignment.CenterVertically
                            )
                        ) {
                            Text(
                                text = dayLabels[index],
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    letterSpacing = 0.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (selected) {
                                    Color.White
                                } else {
                                    Color(0xFF526565)
                                }
                            )

                            Text(
                                text = day.get(Calendar.DAY_OF_MONTH)
                                    .toString()
                                    .padStart(2, '0'),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 17.sp,
                                    lineHeight = 21.sp,
                                    letterSpacing = 0.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (selected) {
                                    Color.White
                                } else {
                                    Color(0xFF102A2A)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekArrow(
    label: String,
    symbol: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .semantics { contentDescription = label },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFEEF5F3)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                color = Color(0xFF126B62)
            )
        }
    }
}

internal fun shiftScheduleDate(
    dateMillis: Long,
    days: Int
): Long {
    return Calendar.getInstance().apply {
        timeInMillis = dateMillis
        add(Calendar.DAY_OF_MONTH, days)
    }.timeInMillis
}

private fun sameScheduleDay(
    firstMillis: Long,
    secondMillis: Long
): Boolean {
    val first = Calendar.getInstance().apply {
        timeInMillis = firstMillis
    }

    val second = Calendar.getInstance().apply {
        timeInMillis = secondMillis
    }

    return first.get(Calendar.ERA) == second.get(Calendar.ERA) &&
            first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
            first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}