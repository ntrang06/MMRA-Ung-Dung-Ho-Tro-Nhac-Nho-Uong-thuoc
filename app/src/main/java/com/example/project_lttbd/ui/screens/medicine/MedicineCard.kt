package com.example.project_lttbd.ui.screens.medicine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.R
import com.example.project_lttbd.ui.theme.MMRATheme

enum class MedicineAccent(
    val foreground: Color,
    val background: Color
) {
    GREEN(
        foreground = Color(0xFF126B62),
        background = Color(0xFFD9EEEA)
    ),
    BLUE(
        foreground = Color(0xFF2E6FAD),
        background = Color(0xFFEDF5FF)
    ),
    AMBER(
        foreground = Color(0xFFAA5B00),
        background = Color(0xFFFFF1D6)
    )
}

data class MedicineUiModel(
    val medicineId: String,
    val medicineName: String,
    val medicineCode: String,
    val quantityText: String,
    val scheduleText: String,
    val hasSchedule: Boolean,
    val accent: MedicineAccent = MedicineAccent.GREEN
)

@Composable
fun MedicineCard(
    medicine: MedicineUiModel,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = medicine.accent

    Surface(
        onClick = { onClick(medicine.medicineId) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFD7E2DF)
        ),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 96.dp)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.width(54.dp),
                shape = RoundedCornerShape(18.dp),
                color = accent.background,
                border = BorderStroke(
                    width = 1.dp,
                    color = accent.foreground
                )
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(min = 62.dp)
                        .padding(
                            horizontal = 4.dp,
                            vertical = 8.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        4.dp,
                        Alignment.CenterVertically
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.thuoc),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = accent.foreground
                    )

                    if (medicine.medicineCode.isNotBlank()) {
                        Text(
                            text = medicine.medicineCode,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                letterSpacing = 0.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = accent.foreground
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = medicine.medicineName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF102A2A)
                )

                val scheduleStatus = if (medicine.hasSchedule) {
                    "Đã có lịch uống"
                } else {
                    "Chưa có lịch uống"
                }

                Text(
                    text = listOf(
                        medicine.quantityText,
                        scheduleStatus
                    )
                        .filter { it.isNotBlank() }
                        .joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 19.5.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = Color(0xFF526565)
                )

                if (medicine.scheduleText.isNotBlank()) {
                    Text(
                        text = medicine.scheduleText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            lineHeight = 19.5.sp,
                            letterSpacing = 0.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = Color(0xFF526565)
                    )
                }
            }

            MedicineChevron(
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MedicineChevron(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val color = Color(0xFF526565)
        val strokeWidth = 1.8.dp.toPx()

        drawLine(
            color = color,
            start = Offset(
                size.width * 0.38f,
                size.height * 0.25f
            ),
            end = Offset(
                size.width * 0.65f,
                size.height * 0.5f
            ),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        drawLine(
            color = color,
            start = Offset(
                size.width * 0.65f,
                size.height * 0.5f
            ),
            end = Offset(
                size.width * 0.38f,
                size.height * 0.75f
            ),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun MedicineCardPreview() {
    val medicines = listOf(
        MedicineUiModel(
            medicineId = "medicine_01",
            medicineName = "Amlodipine 5 mg",
            medicineCode = "A5",
            quantityText = "1 viên",
            scheduleText = "08:00 • Sau ăn sáng",
            hasSchedule = true,
            accent = MedicineAccent.GREEN
        ),
        MedicineUiModel(
            medicineId = "medicine_02",
            medicineName = "Metformin 500 mg",
            medicineCode = "M500",
            quantityText = "1 viên",
            scheduleText = "13:00 • Cùng bữa trưa",
            hasSchedule = true,
            accent = MedicineAccent.BLUE
        ),
        MedicineUiModel(
            medicineId = "medicine_03",
            medicineName = "Atorvastatin 20 mg",
            medicineCode = "A20",
            quantityText = "1 viên",
            scheduleText = "20:00 • Buổi tối",
            hasSchedule = true,
            accent = MedicineAccent.AMBER
        )
    )

    MMRATheme {
        Surface(
            color = Color(0xFFF5FAF9)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                medicines.forEach { medicine ->
                    MedicineCard(
                        medicine = medicine,
                        onClick = {}
                    )
                }
            }
        }
    }
}