package com.example.project_lttbd.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.project_lttbd.ui.screens.account.AccountScreen
import com.example.project_lttbd.ui.screens.auth.LoginScreen
import com.example.project_lttbd.ui.screens.auth.RegisterScreen
import com.example.project_lttbd.ui.screens.family.FamilyScreen
import com.example.project_lttbd.ui.screens.history.HistoryScreen
import com.example.project_lttbd.ui.screens.home.HomeScreen
import com.example.project_lttbd.ui.screens.medicine.MedicineScreen
import com.example.project_lttbd.ui.screens.reminder.DoseReminderScreen
import com.example.project_lttbd.ui.screens.reminder.ReminderDoseUiModel
import com.example.project_lttbd.ui.screens.scan.ScanScreen
import com.example.project_lttbd.ui.screens.schedule.ScheduleScreen

@Composable
fun MMRANavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = MMRADestinations.HOME,
        modifier = modifier
    ) {
        composable(MMRADestinations.LOGIN) {
            LoginScreen()
        }

        composable(MMRADestinations.REGISTER) {
            RegisterScreen()
        }

        composable(MMRADestinations.ACCOUNT) {
            AccountScreen()
        }

        composable(MMRADestinations.HOME) {
            HomeScreen(
                onViewAllClick = {
                    navController.navigate(MMRADestinations.SCHEDULE) {
                        popUpTo(MMRADestinations.HOME) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onViewDoseClick = { doseId ->
                    navController.navigate(
                        "reminder/${Uri.encode(doseId)}"
                    ) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(MMRADestinations.MEDICINE) {
            MedicineScreen()
        }

        composable(MMRADestinations.SCHEDULE) {
            ScheduleScreen()
        }

        composable(MMRADestinations.SCAN) {
            ScanScreen()
        }

        composable(MMRADestinations.HISTORY) {
            HistoryScreen()
        }

        composable(MMRADestinations.FAMILY) {
            FamilyScreen()
        }

        composable(
            route = MMRADestinations.REMINDER,
            arguments = listOf(
                navArgument("doseId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val doseId = backStackEntry.arguments?.getString("doseId")

            // Dữ liệu mẫu FE của liều tiếp theo ở Trang chủ.
            val dose = if (doseId == "dose_02") {
                ReminderDoseUiModel(
                    doseId = "dose_02",
                    medicineId = "medicine_02",
                    timeLabel = "13:00 • Hôm nay",
                    medicineName = "Metformin 500 mg",
                    quantityText = "1 viên",
                    mealInstruction = "CÙNG BỮA TRƯA",
                    audioDurationLabel = "0:08"
                )
            } else {
                null
            }

            if (dose != null) {
                DoseReminderScreen(
                    dose = dose,
                    onClose = {
                        navController.popBackStack()
                    },
                    onTaken = {
                        // Nối cập nhật trạng thái FE ở bước sau.
                    },
                    onSnooze = {
                        // Nối trạng thái nhắc lại ở bước sau.
                    },
                    onSkip = {
                        // Nối trạng thái bỏ qua ở bước sau.
                    },
                    onPlayInstructions = {
                        // Chưa nối âm thanh.
                    }
                )
            } else {
                Column {
                    Text("Không tìm thấy liều thuốc.")

                    TextButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Text("Quay lại")
                    }
                }
            }
        }
    }
}