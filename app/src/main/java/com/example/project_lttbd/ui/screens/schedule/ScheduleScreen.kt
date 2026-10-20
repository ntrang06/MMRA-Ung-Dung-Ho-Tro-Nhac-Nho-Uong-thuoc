package com.example.project_lttbd.ui.screens.schedule

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.ui.theme.MMRATheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ScheduleScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val todayMillis = remember {
        Calendar.getInstance().timeInMillis
    }

    var selectedDateMillis by rememberSaveable {
        mutableStateOf(todayMillis)
    }

    // Dữ liệu mẫu chỉ được gắn với ngày hôm nay.
    val sampleDoses = remember {
        listOf(
            ScheduleDoseUiModel(
                doseId = "schedule_dose_01",
                medicineId = "medicine_01",
                time = "07:30",
                medicineName = "Amlodipine 5 mg",
                quantityText = "1 viên",
                instruction = "Sau ăn sáng",
                status = ScheduleDoseStatus.TAKEN
            ),
            ScheduleDoseUiModel(
                doseId = "schedule_dose_02",
                medicineId = "medicine_02",
                time = "12:00",
                medicineName = "Metformin 500 mg",
                quantityText = "1 viên",
                instruction = "Cùng bữa trưa",
                status = ScheduleDoseStatus.PENDING
            ),
            ScheduleDoseUiModel(
                doseId = "schedule_dose_03",
                medicineId = "medicine_04",
                time = "16:00",
                medicineName = "Vitamin D3 1000 IU",
                quantityText = "1 viên",
                instruction = "Sau ăn",
                status = ScheduleDoseStatus.OVERDUE
            ),
            ScheduleDoseUiModel(
                doseId = "schedule_dose_04",
                medicineId = "medicine_03",
                time = "20:30",
                medicineName = "Atorvastatin 20 mg",
                quantityText = "1 viên",
                instruction = "Sau ăn tối",
                status = ScheduleDoseStatus.UNCONFIRMED
            )
        )
    }

    val dateFormat = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
    }

    val doses = if (
        dateFormat.format(selectedDateMillis) ==
        dateFormat.format(todayMillis)
    ) {
        sampleDoses
    } else {
        emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScheduleHeader(
            initials = "NL",
            onCalendarClick = {
                val selectedDate = Calendar.getInstance().apply {
                    timeInMillis = selectedDateMillis
                }

                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        selectedDateMillis = Calendar.getInstance().apply {
                            clear()
                            set(year, month, day, 12, 0, 0)
                        }.timeInMillis
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            onAccountClick = {
                // Chờ ghép phần tài khoản của nhóm.
            }
        )

        Column(
            modifier = Modifier.padding(
                start = 20.dp,
                top = 6.dp,
                end = 20.dp,
                bottom = 14.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ScheduleWeekPicker(
                selectedDateMillis = selectedDateMillis,
                todayMillis = todayMillis,
                onDateSelected = { selectedDateMillis = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${doses.size} liều thuốc",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF102A2A)
                )

                if (doses.isNotEmpty()) {
                    Text(
                        text = "Chạm để xem chi tiết",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            letterSpacing = 0.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color(0xFF126B62)
                    )
                }
            }

            if (doses.isEmpty()) {
                Text(
                    text = "Chưa có lịch uống thuốc cho ngày này.",
                    modifier = Modifier.padding(vertical = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF526565)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    doses.forEach { dose ->
                        ScheduleDoseCard(
                            dose = dose,
                            onClick = {
                                // Nối màn chi tiết liều thuốc sau.
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun ScheduleScreenPreview() {
    MMRATheme {
        ScheduleScreen()
    }
}