package com.example.project_lttbd.ui.navigation

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.project_lttbd.data.mock.MockCalendar
import com.example.project_lttbd.data.mock.MockDoseAction
import com.example.project_lttbd.data.mock.MockDoseActions
import com.example.project_lttbd.data.mock.MockMedicines
import com.example.project_lttbd.data.mock.MockReminderDoses
import com.example.project_lttbd.ui.components.rememberUiNow
import com.example.project_lttbd.ui.screens.account.AccountScreen
import com.example.project_lttbd.ui.screens.auth.LoginScreen
import com.example.project_lttbd.ui.screens.auth.RegisterScreen
import com.example.project_lttbd.ui.screens.family.FamilyScreen
import com.example.project_lttbd.ui.screens.history.HistoryScreen
import com.example.project_lttbd.ui.screens.home.HomeScreen
import com.example.project_lttbd.ui.screens.medicine.ManualMedicineDraft
import com.example.project_lttbd.ui.screens.medicine.ManualMedicineScreen
import com.example.project_lttbd.ui.screens.medicine.MedicineScreen
import com.example.project_lttbd.ui.screens.reminder.DoseReminderScreen
import com.example.project_lttbd.ui.screens.reminder.ReminderNoticeUiModel
import com.example.project_lttbd.ui.screens.reminder.rememberReminderSpeech
import com.example.project_lttbd.ui.screens.scan.ScanScreen
import com.example.project_lttbd.ui.screens.schedule.ScheduleScreen
import com.example.project_lttbd.ui.screens.schedule.ScheduleSetupEditor

private const val MANUAL_MEDICINE_DRAFT_KEY = "manual_medicine_draft"

@Composable
fun MMRANavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
                onViewDoseClick = {
                    navController.openReminder(it)
                }
            )
        }

        composable(MMRADestinations.MEDICINE) {
            MedicineScreen(
                onMedicineClick = { id ->
                    navController.navigate(
                        "medicine/edit/${Uri.encode(id)}"
                    )
                },
                onAddMedicineClick = {
                    navController.navigate(
                        MMRADestinations.MANUAL_MEDICINE
                    ) {
                        launchSingleTop = true
                    }
                },
                onAccountClick = {
                    navController.navigate(MMRADestinations.ACCOUNT)
                }
            )
        }

        composable(MMRADestinations.MANUAL_MEDICINE) { entry ->
            MedicineEntryDestination(navController, entry)
        }

        composable(
            route = MMRADestinations.EDIT_MEDICINE,
            arguments = listOf(
                navArgument("medicineId") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            MedicineEntryDestination(
                navController,
                entry,
                entry.arguments?.getString("medicineId")
            )
        }

        composable(MMRADestinations.SCHEDULE_SETUP) { entry ->
            val saved = remember(entry) {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<Bundle>(MANUAL_MEDICINE_DRAFT_KEY)
            }

            val name = saved?.getString("medicineName").orEmpty()

            if (saved != null && name.isNotBlank()) {
                ScheduleSetupEditor(
                    medicineId = saved.getString("medicineId"),
                    draft = ManualMedicineDraft(
                        medicineName = name,
                        strength = saved.getString("strength").orEmpty(),
                        strengthUnit =
                            saved.getString("strengthUnit") ?: "mg",
                        dosageForm =
                            saved.getString("dosageForm").orEmpty(),
                        note = saved.getString("note").orEmpty(),
                        imageUri = saved.getString("imageUri")
                    ),
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onSaved = {
                        Toast.makeText(
                            context,
                            "Đã lưu thuốc và lịch uống",
                            Toast.LENGTH_SHORT
                        ).show()

                        if (
                            !navController.popBackStack(
                                MMRADestinations.MEDICINE,
                                false
                            )
                        ) {
                            navController.navigate(
                                MMRADestinations.MEDICINE
                            ) {
                                popUpTo(MMRADestinations.HOME)
                                launchSingleTop = true
                            }
                        }
                    }
                )
            } else {
                Column {
                    Text("Không tìm thấy thông tin thuốc vừa nhập.")

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

        composable(MMRADestinations.SCHEDULE) {
            ScheduleScreen(
                onDoseClick = {
                    navController.openReminder(it)
                },
                onAccountClick = {
                    navController.navigate(MMRADestinations.ACCOUNT)
                }
            )
        }

        composable(MMRADestinations.SCAN) {
            ScanScreen()
        }

        composable(MMRADestinations.HISTORY) {
            HistoryScreen(
                onDoseClick = {
                    navController.openReminder(
                        it,
                        readOnly = true
                    )
                },
                onAccountClick = {
                    navController.navigate(MMRADestinations.ACCOUNT)
                }
            )
        }

        composable(MMRADestinations.FAMILY) {
            FamilyScreen()
        }

        composable(
            route = MMRADestinations.REMINDER,
            arguments = listOf(
                navArgument("doseId") {
                    type = NavType.StringType
                },
                navArgument("readOnly") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { entry ->
            ReminderDestination(
                doseId = entry.arguments?.getString("doseId"),
                readOnly =
                    entry.arguments?.getBoolean("readOnly") ?: false,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}

private fun NavHostController.openReminder(
    doseId: String,
    readOnly: Boolean = false
) {
    navigate(
        "reminder/${Uri.encode(doseId)}?readOnly=$readOnly"
    ) {
        launchSingleTop = true
    }
}

@Composable
private fun MedicineEntryDestination(
    navController: NavHostController,
    entry: NavBackStackEntry,
    medicineId: String? = null
) {
    val previous = MockMedicines.findById(medicineId)

    if (medicineId != null && previous == null) {
        Column {
            Text("Không tìm thấy thuốc cần sửa.")

            TextButton(
                onClick = {
                    navController.popBackStack()
                }
            ) {
                Text("Quay lại")
            }
        }
        return
    }

    ManualMedicineScreen(
        initialDraft = previous?.draft(),
        onBackClick = {
            navController.popBackStack()
        },
        onContinue = { draft ->
            entry.savedStateHandle[MANUAL_MEDICINE_DRAFT_KEY] =
                Bundle().apply {
                    putString("medicineId", medicineId)
                    putString("medicineName", draft.medicineName)
                    putString("strength", draft.strength)
                    putString("strengthUnit", draft.strengthUnit)
                    putString("dosageForm", draft.dosageForm)
                    putString("note", draft.note)
                    putString("imageUri", draft.imageUri)
                }

            navController.navigate(
                MMRADestinations.SCHEDULE_SETUP
            ) {
                launchSingleTop = true
            }
        }
    )
}

@Composable
private fun ReminderDestination(
    doseId: String?,
    readOnly: Boolean,
    onBackClick: () -> Unit
) {
    val dose = MockReminderDoses.findById(doseId)

    var dialog by rememberSaveable(doseId) {
        mutableStateOf<String?>(null)
    }

    if (dose == null) {
        Column {
            Text("Không tìm thấy liều thuốc.")

            TextButton(onClick = onBackClick) {
                Text("Quay lại")
            }
        }
        return
    }

    val record = MockDoseActions.get(dose.doseId)
    val finished = MockDoseActions.isFinished(dose.doseId)
    val today = MockCalendar.day(dose.scheduledAtMillis) ==
            MockCalendar.day(rememberUiNow())

    val canAct = today && !readOnly && !finished

    val playInstructions = rememberReminderSpeech(
        "${dose.medicineName}. ${dose.quantityText}. ${dose.mealInstruction}."
    )

    val notice = when {
        readOnly || !today -> ReminderNoticeUiModel(
            "Chế độ xem chi tiết",
            "Bạn đang xem liều của ngày đã chọn. " +
                    "Các thao tác xác nhận được thực hiện tại lịch uống hôm nay."
        )

        finished -> ReminderNoticeUiModel(
            if (record?.action == MockDoseAction.TAKEN) {
                "Đã ghi nhận uống"
            } else {
                "Đã bỏ qua"
            },
            "Liều này đã được xử lý, không ghi nhận thêm lần nữa."
        )

        else -> null
    }

    DoseReminderScreen(
        dose = dose,
        actionsEnabled = canAct,
        notice = notice,
        onClose = onBackClick,
        onTaken = {
            if (canAct) dialog = "CONFIRM_TAKEN"
        },
        onSkip = {
            if (canAct) dialog = "CONFIRM_SKIP"
        },
        onSnooze = {
            if (canAct) {
                MockDoseActions.record(
                    it,
                    MockDoseAction.SNOOZED
                )
                dialog = "SNOOZED"
            }
        },
        onPlayInstructions = playInstructions
    )

    val current = dialog

    if (current != null) {
        val confirming =
            current == "CONFIRM_TAKEN" ||
                    current == "CONFIRM_SKIP"

        val title = when (current) {
            "CONFIRM_TAKEN" -> "Xác nhận đã uống?"
            "CONFIRM_SKIP" -> "Bỏ qua liều này?"
            "TAKEN" -> "Đã ghi nhận uống thuốc"
            "SKIPPED" -> "Đã ghi nhận bỏ qua"
            else -> "Đã chọn nhắc lại"
        }

        val message = when (current) {
            "CONFIRM_TAKEN" ->
                "Bạn đã uống ${dose.quantityText} ${dose.medicineName} cho liều ${dose.timeLabel}?"

            "CONFIRM_SKIP" ->
                "Liều ${dose.medicineName} sẽ được đánh dấu bỏ qua."

            "TAKEN" ->
                "Liều này đã được đánh dấu đã uống."

            "SKIPPED" ->
                "Liều này đã được đánh dấu bỏ qua."

            else ->
                "Đã chọn nhắc lại sau 10 phút. Liều này chưa được đánh dấu đã uống."
        }

        AlertDialog(
            onDismissRequest = {
                dialog = null
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
                        when (current) {
                            "CONFIRM_TAKEN" -> {
                                MockDoseActions.record(
                                    dose.doseId,
                                    MockDoseAction.TAKEN
                                )
                                dialog = "TAKEN"
                            }

                            "CONFIRM_SKIP" -> {
                                MockDoseActions.record(
                                    dose.doseId,
                                    MockDoseAction.SKIPPED
                                )
                                dialog = "SKIPPED"
                            }

                            else -> {
                                dialog = null
                                onBackClick()
                            }
                        }
                    }
                ) {
                    Text(
                        if (confirming) {
                            "Xác nhận"
                        } else {
                            "Quay lại"
                        }
                    )
                }
            },
            dismissButton = {
                if (confirming) {
                    TextButton(
                        onClick = {
                            dialog = null
                        }
                    ) {
                        Text("Hủy")
                    }
                }
            }
        )
    }
}