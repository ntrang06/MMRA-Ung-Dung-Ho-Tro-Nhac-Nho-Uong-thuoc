package com.example.project_lttbd.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.R
import com.example.project_lttbd.ui.theme.MMRATheme

private val NextDoseBackground = Color(0xFF0A4D47)
private val NextDoseMuted = Color(0xFFD9EEEA)
private val NextDoseTagBackground = Color(0xFFE3F4E9)
private val NextDoseTagText = Color(0xFF287A4D)

@Composable
fun NextDoseCard(
    dose: NextDoseUiModel,
    onViewDoseClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = NextDoseBackground
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NextDoseTag()

                Text(
                    text = dose.countdownText,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = NextDoseMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MedicineVisual(code = dose.medicineCode)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = dose.time,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 38.sp,
                            lineHeight = 38.sp,
                            letterSpacing = 0.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )

                    Text(
                        text = dose.medicineName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            lineHeight = 21.sp,
                            letterSpacing = 0.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )

                    Text(
                        text = dose.instruction,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            letterSpacing = 0.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = NextDoseMuted
                    )
                }
            }

            Surface(
                onClick = { onViewDoseClick(dose.doseId) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                contentColor = MaterialTheme.colorScheme.primary,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    modifier = Modifier
                        .heightIn(min = 50.dp)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp,
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.thuoc),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = "Xem liều thuốc",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun NextDoseTag() {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = NextDoseTagBackground,
        contentColor = NextDoseTagText
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 26.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClockIcon(
                modifier = Modifier.size(14.dp),
                color = NextDoseTagText
            )

            Text(
                text = "LIỀU TIẾP THEO",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun MedicineVisual(code: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primary,
        border = BorderStroke(1.5.dp, Color.White)
    ) {
        Column(
            modifier = Modifier
                .size(62.dp)
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                3.dp,
                Alignment.CenterVertically
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.thuoc),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )

            Text(
                text = code,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }
    }
}

@Composable
private fun ClockIcon(
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.7.dp.toPx()

        drawCircle(
            color = color,
            radius = size.minDimension / 2 - strokeWidth / 2,
            style = Stroke(width = strokeWidth)
        )

        drawLine(
            color = color,
            start = center,
            end = Offset(center.x, size.height * 0.23f),
            strokeWidth = strokeWidth
        )

        drawLine(
            color = color,
            start = center,
            end = Offset(size.width * 0.72f, center.y),
            strokeWidth = strokeWidth
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NextDoseCardPreview() {
    MMRATheme {
        NextDoseCard(
            dose = NextDoseUiModel(
                doseId = "dose_02",
                medicineId = "medicine_02",
                time = "13:00",
                medicineName = "Metformin 500 mg",
                instruction = "1 viên • Cùng bữa trưa",
                medicineCode = "M500",
                countdownText = "Còn 1 giờ 18 phút"
            ),
            onViewDoseClick = {},
            modifier = Modifier.padding(20.dp)
        )
    }
}