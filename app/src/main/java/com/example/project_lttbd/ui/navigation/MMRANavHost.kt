package com.example.project_lttbd.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.project_lttbd.data.mock.MockDoseAction
import com.example.project_lttbd.data.mock.MockDoseActions
import com.example.project_lttbd.data.mock.MockReminderDoses
import com.example.project_lttbd.ui.screens.account.AccountScreen
import com.example.project_lttbd.ui.screens.auth.LoginScreen
import com.example.project_lttbd.ui.screens.auth.RegisterScreen
import com.example.project_lttbd.ui.screens.family.FamilyScreen
import com.example.project_lttbd.ui.screens.history.HistoryScreen
import com.example.project_lttbd.ui.screens.home.HomeScreen
import com.example.project_lttbd.ui.screens.medicine.ManualMedicineScreen
import com.example.project_lttbd.ui.screens.medicine.MedicineScreen
import com.example.project_lttbd.ui.screens.reminder.DoseReminderScreen
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
                    navController.navigate(
                        MMRADestinations.SCHEDULE
                    ) {
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
            MedicineScreen(
                onAddMedicineClick = {
                    navController.navigate(
                        MMRADestinations.MANUAL_MEDICINE
                    ) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(MMRADestinations.MANUAL_MEDICINE) {
            ManualMedicineScreen(
                onBackClick = {
                    navController.popBackStack()
                }
                // Nối onContinue với màn Thiết lập lịch uống
                // sau khi hoàn thành màn đó.
            )
        }

        composable(MMRADestinations.SCHEDULE) {
            ScheduleScreen(
                onDoseClick = { doseId ->
                    navController.navigate(
                        "reminder/${Uri.encode(doseId)}"
                    ) {
                        launchSingleTop = true
                    }
                }
            )
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
            val doseId = backStackEntry.arguments
                ?.getString("doseId")

            val dose = MockReminderDoses.findById(doseId)

            var dialogAction by rememberSaveable(doseId) {
                mutableStateOf<String?>(null)
            }

            if (dose != null) {
                val record = MockDoseActions.get(dose.doseId)
                val finished = MockDoseActions.isFinished(
                    dose.doseId
                )

                DoseReminderScreen(
                    dose = dose,
                    actionsEnabled = !finished,
                    onClose = {
                        navController.popBackStack()
                    },
                    onTaken = {
                        dialogAction = "CONFIRM_TAKEN"
                    },
                    onSnooze = { selectedDoseId ->
                        MockDoseActions.record(
                            doseId = selectedDoseId,
                            action = MockDoseAction.SNOOZED
                        )

                        dialogAction = "SNOOZED"
                    },
                    onSkip = {
                        dialogAction = "CONFIRM_SKIP"
                    },
                    onPlayInstructions = {
                        // Chưa nối âm thanh.
                    }
                )

                val currentDialog = dialogAction

                if (currentDialog != null) {
                    val needsConfirmation =
                        currentDialog == "CONFIRM_TAKEN" ||
                                currentDialog == "CONFIRM_SKIP"

                    val title = when (currentDialog) {
                        "CONFIRM_TAKEN" -> "Xác nhận đã uống?"
                        "CONFIRM_SKIP" -> "Bỏ qua liều này?"
                        "TAKEN" -> "Đã ghi nhận uống thuốc"
                        "SKIPPED" -> "Đã ghi nhận bỏ qua"
                        else -> "Đã chọn nhắc lại"
                    }

                    val message = when (currentDialog) {
                        "CONFIRM_TAKEN" ->
                            "Bạn đã uống ${dose.quantityText} " +
                                    "${dose.medicineName} cho liều " +
                                    "${dose.timeLabel}?"

                        "CONFIRM_SKIP" ->
                            "Liều ${dose.medicineName} " +
                                    "sẽ được đánh dấu bỏ qua."

                        "TAKEN" ->
                            "Liều này đã được đánh dấu đã uống."

                        "SKIPPED" ->
                            "Liều này đã được đánh dấu bỏ qua."

                        else ->
                            "Bạn đã chọn nhắc lại sau 10 phút. " +
                                    "Liều này vẫn chưa được " +
                                    "đánh dấu đã uống."
                    }

                    AlertDialog(
                        onDismissRequest = {
                            dialogAction = null
                        },
                        title = {
                            Text(title)
                        },
                        text = {
                            Text(message)
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    when (currentDialog) {
                                        "CONFIRM_TAKEN" -> {
                                            MockDoseActions.record(
                                                doseId = dose.doseId,
                                                action = MockDoseAction.TAKEN
                                            )

                                            dialogAction = "TAKEN"
                                        }

                                        "CONFIRM_SKIP" -> {
                                            MockDoseActions.record(
                                                doseId = dose.doseId,
                                                action = MockDoseAction.SKIPPED
                                            )

                                            dialogAction = "SKIPPED"
                                        }

                                        else -> {
                                            dialogAction = null
                                            navController.popBackStack()
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    if (needsConfirmation) {
                                        "Xác nhận"
                                    } else {
                                        "Quay lại"
                                    }
                                )
                            }
                        },
                        dismissButton = {
                            if (needsConfirmation) {
                                TextButton(
                                    onClick = {
                                        dialogAction = null
                                    }
                                ) {
                                    Text("Hủy")
                                }
                            }
                        }
                    )
                } else if (finished && record != null) {
                    // Khóa thao tác khi mở lại liều đã xử lý.
                    AlertDialog(
                        onDismissRequest = {
                            navController.popBackStack()
                        },
                        title = {
                            Text(
                                if (
                                    record.action ==
                                    MockDoseAction.TAKEN
                                ) {
                                    "Liều này đã được ghi nhận uống"
                                } else {
                                    "Liều này đã được bỏ qua"
                                }
                            )
                        },
                        text = {
                            Text(
                                "Bạn có thể xem lại trong lịch sử " +
                                        "khi hoàn thiện màn đó."
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    navController.popBackStack()
                                }
                            ) {
                                Text("Quay lại")
                            }
                        }
                    )
                }
            } else {
                Column {
                    Text("Không tìm thấy liều thuốc.")

                    TextButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Text("Quay lại")
                    }
                }
            }
        }
    }
}