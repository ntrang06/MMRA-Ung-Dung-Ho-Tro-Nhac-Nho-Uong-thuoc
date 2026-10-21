package com.example.project_lttbd.ui.screens.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
        Color(0xFF126B62),
        Color(0xFFD9EEEA)
    ),
    OVERDUE(
        "QUÁ GIỜ",
        "!",
        Color(0xFFBA3028),
        Color(0xFFFBE6E3)
    ),
    UNCONFIRMED(
        "CHƯA XÁC NHẬN",
        "?",
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

@Composable
fun ScheduleDoseCard(
    dose: ScheduleDoseUiModel,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val status = dose.status

    Surface(
        onClick = { onClick(dose.doseId) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(
            width = 1.dp,
            color = if (status == ScheduleDoseStatus.OVERDUE) {
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
                    Text(
                        text = status.symbol,
                        fontSize = 18.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = status.foreground
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

            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "›",
                    fontSize = 24.sp,
                    color = Color(0xFF526565)
                )
            }
        }
    }
}