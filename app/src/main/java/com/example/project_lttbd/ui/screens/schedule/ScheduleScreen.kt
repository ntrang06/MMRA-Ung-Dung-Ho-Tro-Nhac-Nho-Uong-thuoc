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
import com.example.project_lttbd.data.mock.MockDoseAction
import com.example.project_lttbd.data.mock.MockDoseActions
import com.example.project_lttbd.data.mock.MockReminderDoses
import com.example.project_lttbd.ui.theme.MMRATheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ScheduleScreen(
    modifier: Modifier = Modifier,
    onDoseClick: (String) -> Unit = {}
) {
    val context = LocalContext.current

    val todayMillis = remember {
        Calendar.getInstance().timeInMillis
    }

    var selectedDateMillis by rememberSaveable {
        mutableStateOf(todayMillis)
    }

    val dateFormat = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
    }

    val isToday =
        dateFormat.format(Date(selectedDateMillis)) ==
                dateFormat.format(Date(todayMillis))

    // Dữ liệu mẫu dùng chung với Trang chủ.
    // Đọc trạng thái trực tiếp để giao diện cập nhật khi có thao tác.
    val doses: List<ScheduleDoseUiModel> = if (isToday) {
        MockReminderDoses.doses.map { dose ->
            val status = when (
                MockDoseActions.get(dose.doseId)?.action
            ) {
                MockDoseAction.TAKEN -> ScheduleDoseStatus.TAKEN
                MockDoseAction.SNOOZED -> ScheduleDoseStatus.SNOOZED
                MockDoseAction.SKIPPED -> ScheduleDoseStatus.SKIPPED
                null -> ScheduleDoseStatus.PENDING
            }

            val instruction = dose.mealInstruction
                .lowercase(Locale.forLanguageTag("vi-VN"))
                .replaceFirstChar { it.titlecase() }

            ScheduleDoseUiModel(
                doseId = dose.doseId,
                medicineId = dose.medicineId,
                time = dose.timeLabel.substringBefore(" •"),
                medicineName = dose.medicineName,
                quantityText = dose.quantityText,
                instruction = instruction,
                status = status
            )
        }
    } else {
        // Hiện tại dữ liệu mẫu chỉ có lịch của hôm nay.
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
                            onClick = onDoseClick
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