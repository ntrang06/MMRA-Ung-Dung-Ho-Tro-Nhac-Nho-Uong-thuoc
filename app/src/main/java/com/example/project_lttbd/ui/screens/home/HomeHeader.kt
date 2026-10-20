package com.example.project_lttbd.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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

@Composable
fun HomeHeader(
    greeting: String,
    dateText: String,
    initials: String,
    onNotificationsClick: () -> Unit,
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    lineHeight = 26.4.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF102A2A)
            )

            Text(
                text = dateText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 17.55.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color(0xFF526565)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onNotificationsClick,
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = Color(0xFFEEF5F3)
            ) {
                NotificationBell(
                    modifier = Modifier.padding(11.dp)
                )
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
                        androidx.compose.foundation.layout.Box(
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
}

@Composable
private fun NotificationBell(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val scaleX = size.width / 24f
        val scaleY = size.height / 24f
        val bellColor = Color(0xFF102A2A)

        val outline = Path().apply {
            moveTo(18 * scaleX, 8 * scaleY)
            cubicTo(
                18 * scaleX, 4.7f * scaleY,
                15.3f * scaleX, 2 * scaleY,
                12 * scaleX, 2 * scaleY
            )
            cubicTo(
                8.7f * scaleX, 2 * scaleY,
                6 * scaleX, 4.7f * scaleY,
                6 * scaleX, 8 * scaleY
            )
            cubicTo(
                6 * scaleX, 15 * scaleY,
                3 * scaleX, 15 * scaleY,
                3 * scaleX, 17 * scaleY
            )
            lineTo(21 * scaleX, 17 * scaleY)
            cubicTo(
                21 * scaleX, 15 * scaleY,
                18 * scaleX, 15 * scaleY,
                18 * scaleX, 8 * scaleY
            )
            close()
        }

        drawPath(
            path = outline,
            color = bellColor,
            style = Stroke(
                width = 1.8.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        drawLine(
            color = bellColor,
            start = Offset(10 * scaleX, 21 * scaleY),
            end = Offset(14 * scaleX, 21 * scaleY),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}