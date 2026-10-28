package com.example.project_lttbd.ui.screens.schedule

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.R

@Composable
fun ScheduleHeader(
    initials: String,
    onCalendarClick: () -> Unit,
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Lịch uống thuốc",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    lineHeight = 27.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF102A2A)
            )

            Text(
                text = "Tất cả liều trong ngày",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color(0xFF526565)
            )
        }

        Surface(
            onClick = onCalendarClick,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = Color(0xFFEEF5F3)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.lich),
                    contentDescription = "Chọn ngày",
                    modifier = Modifier.size(22.dp),
                    tint = Color(0xFF102A2A)
                )
            }
        }

        Surface(
            onClick = onAccountClick,
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .width(52.dp)
                    .heightIn(min = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    3.dp,
                    Alignment.CenterVertically
                )
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = Color(0xFFD9EEEA)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                lineHeight = 22.sp,
                                letterSpacing = 0.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF126B62)
                        )
                    }
                }

                Text(
                    text = "Tài khoản",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color(0xFF126B62)
                )
            }
        }
    }
}