package com.example.project_lttbd.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onViewAllClick: () -> Unit = {}
) {
    // Dữ liệu mẫu FE.
    val nextDose = NextDoseUiModel(
        medicineId = "medicine_02",
        time = "13:00",
        medicineName = "Metformin 500 mg",
        instruction = "1 viên • Cùng bữa trưa",
        medicineCode = "M500",
        countdownText = "Còn 1 giờ 18 phút"
    )

    val todayDoses = listOf(
        HomeDoseUiModel(
            doseId = "dose_01",
            time = "08:00",
            medicineName = "Amlodipine 5 mg",
            instruction = "1 viên • Sau ăn sáng",
            status = HomeDoseStatus.TAKEN
        ),
        HomeDoseUiModel(
            doseId = "dose_02",
            time = "13:00",
            medicineName = "Metformin 500 mg",
            instruction = "1 viên • Cùng bữa trưa",
            status = HomeDoseStatus.NEXT
        ),
        HomeDoseUiModel(
            doseId = "dose_03",
            time = "20:00",
            medicineName = "Atorvastatin 20 mg",
            instruction = "1 viên • Buổi tối",
            status = HomeDoseStatus.UPCOMING
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        HomeHeader(
            greeting = "Chào buổi sáng, cô Lan",
            dateText = "Thứ Sáu, 02 tháng 10",
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
            NextDoseCard(
                dose = nextDose,
                onViewDoseClick = {
                    // Nối màn chi tiết liều thuốc sau.
                }
            )

            TodayProgressCard(
                takenDoses = 1,
                totalDoses = 3,
                onTimeDoses = 1
            )

            TodayScheduleSection(
                doses = todayDoses,
                onViewAllClick = onViewAllClick,
                onDoseClick = { doseId ->
                    // Nối màn chi tiết theo doseId sau.
                }
            )
        }
    }
}