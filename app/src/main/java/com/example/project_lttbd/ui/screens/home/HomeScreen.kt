package com.example.project_lttbd.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.project_lttbd.data.mock.MockDoseAction
import com.example.project_lttbd.data.mock.MockDoseActions
import com.example.project_lttbd.data.mock.MockReminderDoses
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onViewAllClick: () -> Unit = {},
    onViewDoseClick: (String) -> Unit = {}
) {
    val doses = MockReminderDoses.doses

    // Đọc trạng thái từ nguồn dữ liệu mẫu dùng chung.
    fun actionFor(doseId: String): MockDoseAction? {
        return MockDoseActions.get(doseId)?.action
    }

    // Chọn liều chưa uống và chưa bỏ qua theo thứ tự dữ liệu mẫu.
    val nextDose = doses.firstOrNull { dose ->
        val action = actionFor(dose.doseId)

        action != MockDoseAction.TAKEN &&
                action != MockDoseAction.SKIPPED
    }

    val todayDoses = doses.map { dose ->
        val instruction = dose.mealInstruction
            .lowercase(Locale.forLanguageTag("vi-VN"))
            .replaceFirstChar { it.titlecase() }

        val status = when (actionFor(dose.doseId)) {
            MockDoseAction.TAKEN -> HomeDoseStatus.TAKEN
            MockDoseAction.SKIPPED -> HomeDoseStatus.SKIPPED
            MockDoseAction.SNOOZED -> HomeDoseStatus.SNOOZED

            null -> {
                if (dose.doseId == nextDose?.doseId) {
                    HomeDoseStatus.NEXT
                } else {
                    HomeDoseStatus.UPCOMING
                }
            }
        }

        HomeDoseUiModel(
            doseId = dose.doseId,
            time = dose.timeLabel.substringBefore(" •"),
            medicineName = dose.medicineName,
            instruction = listOf(
                dose.quantityText,
                instruction
            )
                .filter { it.isNotBlank() }
                .joinToString(" • "),
            status = status
        )
    }

    val takenCount = todayDoses.count {
        it.status == HomeDoseStatus.TAKEN
    }

    val skippedCount = todayDoses.count {
        it.status == HomeDoseStatus.SKIPPED
    }

    val todayText = SimpleDateFormat(
        "EEEE, dd 'tháng' MM",
        Locale.forLanguageTag("vi-VN")
    )
        .format(Calendar.getInstance().time)
        .replaceFirstChar { it.titlecase() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        HomeHeader(
            greeting = "Chào buổi sáng, cô Lan",
            dateText = todayText,
            initials = "NL",
            onNotificationsClick = {
                // Chờ ghép phần thông báo của nhóm.
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
            if (nextDose != null) {
                val row = todayDoses.first {
                    it.doseId == nextDose.doseId
                }

                val medicineCode = when (nextDose.medicineId) {
                    "medicine_01" -> "A5"
                    "medicine_02" -> "M500"
                    "medicine_03" -> "A20"
                    else -> ""
                }

                NextDoseCard(
                    dose = NextDoseUiModel(
                        doseId = nextDose.doseId,
                        medicineId = nextDose.medicineId,
                        time = row.time,
                        medicineName = nextDose.medicineName,
                        instruction = row.instruction,
                        medicineCode = medicineCode,
                        countdownText = if (
                            row.status == HomeDoseStatus.SNOOZED
                        ) {
                            "Đã chọn nhắc lại"
                        } else {
                            "Liều sắp tới"
                        }
                    ),
                    onViewDoseClick = onViewDoseClick
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(
                        1.dp,
                        Color(0xFFD7E2DF)
                    )
                ) {
                    Text(
                        text = "Không còn liều chờ xử lý hôm nay.",
                        modifier = Modifier.padding(18.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF102A2A)
                    )
                }
            }

            TodayProgressCard(
                takenDoses = takenCount,
                totalDoses = todayDoses.size,
                skippedDoses = skippedCount,
                // Số mẫu cũ, chưa nối thống kê đúng giờ.
                onTimeDoses = 1
            )

            TodayScheduleSection(
                doses = todayDoses,
                onViewAllClick = onViewAllClick,
                onDoseClick = onViewDoseClick
            )
        }
    }
}