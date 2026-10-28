package com.example.project_lttbd.ui.screens.medicine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.data.model.UserProfileManager
import com.example.project_lttbd.data.mock.MockCalendar
import com.example.project_lttbd.data.mock.MockMedicines
import com.example.project_lttbd.ui.components.MMRASearchField
import com.example.project_lttbd.ui.theme.MMRATheme

@Composable
fun MedicineScreen(
    modifier: Modifier = Modifier,
    onMedicineClick: (String) -> Unit = {},
    onAddMedicineClick: () -> Unit = {},
    onAccountClick: () -> Unit = {}
) {
    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }

    val medicines = MockMedicines.medicines.map { medicine ->
        val schedule = medicine.schedule

        val scheduleText = schedule
            ?.times
            ?.sorted()
            ?.joinToString("\n") { minute ->
                listOf(
                    MockCalendar.time(minute),
                    schedule.meal
                )
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
            }
            .orEmpty()

        MedicineUiModel(
            medicineId = medicine.medicineId,
            medicineName = medicine.medicineName,
            medicineCode = medicine.medicineCode,
            quantityText =
                schedule?.quantityText ?: medicine.quantityText,
            scheduleText = scheduleText,
            hasSchedule =
                schedule != null && schedule.times.isNotEmpty(),
            accent = when (medicine.medicineId) {
                "medicine_02" -> MedicineAccent.BLUE
                "medicine_03" -> MedicineAccent.AMBER
                else -> MedicineAccent.GREEN
            }
        )
    }

    val query = searchQuery.trim()

    val visibleMedicines = medicines.filter {
        it.medicineName.contains(
            other = query,
            ignoreCase = true
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAF9))
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        MedicineHeader(
            ownerName = "cô Lan",
            initials = UserProfileManager.getInitials(),
            onAccountClick = onAccountClick
        )

        Column(
            modifier = Modifier.padding(
                start = 20.dp,
                top = 6.dp,
                end = 20.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MMRASearchField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                },
                placeholder = "Tìm tên thuốc"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (query.isEmpty()) {
                        "${medicines.size} thuốc đang dùng"
                    } else {
                        "${visibleMedicines.size} kết quả"
                    },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF102A2A)
                )

                if (visibleMedicines.isNotEmpty()) {
                    Text(
                        text = "Chạm để xem chi tiết",
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

            if (visibleMedicines.isEmpty()) {
                Text(
                    text = if (medicines.isEmpty()) {
                        "Bạn chưa thêm thuốc nào."
                    } else {
                        "Không tìm thấy thuốc phù hợp."
                    },
                    modifier = Modifier.padding(vertical = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF526565)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    visibleMedicines.forEach { medicine ->
                        MedicineCard(
                            medicine = medicine,
                            onClick = onMedicineClick
                        )
                    }
                }
            }

            AddMedicineButton(
                onClick = onAddMedicineClick
            )

            MedicineNotice()
        }
    }
}

@Composable
private fun AddMedicineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF126B62),
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(
            horizontal = 14.dp,
            vertical = 14.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier.size(18.dp)
            ) {
                val strokeWidth = 1.8.dp.toPx()

                drawLine(
                    color = Color.White,
                    start = Offset(
                        size.width * 0.2f,
                        size.height * 0.5f
                    ),
                    end = Offset(
                        size.width * 0.8f,
                        size.height * 0.5f
                    ),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = Color.White,
                    start = Offset(
                        size.width * 0.5f,
                        size.height * 0.2f
                    ),
                    end = Offset(
                        size.width * 0.5f,
                        size.height * 0.8f
                    ),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            Text(
                text = "Thêm thuốc",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 19.5.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
internal fun MedicineNotice(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFF1D6)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Canvas(
                modifier = Modifier.size(16.dp)
            ) {
                val color = Color(0xFF126B62)
                val strokeWidth = 1.5.dp.toPx()

                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.4f,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )

                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.045f,
                    center = Offset(
                        size.width * 0.5f,
                        size.height * 0.32f
                    )
                )

                drawLine(
                    color = color,
                    start = Offset(
                        size.width * 0.5f,
                        size.height * 0.48f
                    ),
                    end = Offset(
                        size.width * 0.5f,
                        size.height * 0.7f
                    ),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            Text(
                text = "MMRA chỉ hỗ trợ nhắc lịch. " +
                        "Hãy dùng thuốc theo đơn của bác sĩ.",
                modifier = Modifier.weight(1f),
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
}

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun MedicineScreenPreview() {
    MMRATheme {
        MedicineScreen()
    }
}