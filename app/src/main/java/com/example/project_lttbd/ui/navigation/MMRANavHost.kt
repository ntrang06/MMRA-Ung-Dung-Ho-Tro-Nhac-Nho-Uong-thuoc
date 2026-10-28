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
import com.example.project_lttbd.data.model.InitialSetupPreferences
import com.example.project_lttbd.data.model.UserProfileManager
import com.example.project_lttbd.ui.screens.auth.CompletionScreen
import com.example.project_lttbd.ui.screens.auth.CreateNewPasswordScreen
import com.example.project_lttbd.ui.screens.auth.EmailLoginScreen
import com.example.project_lttbd.ui.screens.auth.ForgotPasswordScreen
import com.example.project_lttbd.ui.screens.auth.LoginScreen
import com.example.project_lttbd.ui.screens.auth.PhoneLoginScreen
import com.example.project_lttbd.ui.screens.auth.RegisterScreen
import com.example.project_lttbd.ui.screens.auth.VerifyEmailScreen
import com.example.project_lttbd.ui.screens.auth.VerifyPhoneScreen
import com.example.project_lttbd.ui.screens.auth.WelcomeScreen
import com.example.project_lttbd.ui.screens.account.AccessibilitySettingsScreen
import com.example.project_lttbd.ui.screens.account.AvatarDeletedScreen
import com.example.project_lttbd.ui.screens.account.AvatarOptionsScreen
import com.example.project_lttbd.ui.screens.account.AvatarUpdatedScreen
import com.example.project_lttbd.ui.screens.account.CaptureAvatarScreen
import com.example.project_lttbd.ui.screens.account.ChangePasswordScreen
import com.example.project_lttbd.ui.screens.account.CropAvatarScreen
import com.example.project_lttbd.ui.screens.account.EditEmailScreen
import com.example.project_lttbd.ui.screens.account.EditNameScreen
import com.example.project_lttbd.ui.screens.account.EditPhoneScreen
import com.example.project_lttbd.ui.screens.account.GalleryPickerScreen
import com.example.project_lttbd.ui.screens.account.LogoutConfirmationScreen
import com.example.project_lttbd.ui.screens.account.ManageUsageModeScreen
import com.example.project_lttbd.ui.screens.account.MyAccountScreen
import com.example.project_lttbd.ui.screens.account.NotificationSettingsScreen
import com.example.project_lttbd.ui.screens.account.ReviewCapturedPhotoScreen
import com.example.project_lttbd.ui.screens.account.SwitchAccountScreen
import com.example.project_lttbd.ui.screens.account.VoiceGuidanceSettingsScreen
import com.example.project_lttbd.ui.screens.setup.SetupDisplayPreferenceScreen
import com.example.project_lttbd.ui.screens.setup.SetupTargetUserScreen
import com.example.project_lttbd.data.mock.MockCalendar
import com.example.project_lttbd.data.mock.MockDoseAction
import com.example.project_lttbd.data.mock.MockDoseActions
import com.example.project_lttbd.data.mock.MockFamilyData
import com.example.project_lttbd.data.mock.MockMedicines
import com.example.project_lttbd.data.mock.MockReminderDoses
import com.example.project_lttbd.ui.components.rememberUiNow
import com.example.project_lttbd.ui.screens.history.HistoryScreen
import com.example.project_lttbd.ui.screens.home.HomeScreen
import com.example.project_lttbd.ui.screens.family.CaregiverMemberDetailScreen
import com.example.project_lttbd.ui.screens.family.FamilyAlertsScreen
import com.example.project_lttbd.ui.screens.family.FamilyInviteDetailScreen
import com.example.project_lttbd.ui.screens.family.FamilyPermissionsScreen
import com.example.project_lttbd.ui.screens.family.FamilyScreen
import com.example.project_lttbd.ui.screens.family.InviteFamilyScreen
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

    UserProfileManager.init(context)
    val isLoggedIn = UserProfileManager.isLoggedIn(context)
    val isSetupCompleted = UserProfileManager.isSetupCompleted(context)

    val startDest = MMRADestinations.WELCOME

    var setupPreferences by remember {
        mutableStateOf(InitialSetupPreferences())
    }

    NavHost(
        navController = navController,
        startDestination = startDest,
        modifier = modifier
    ) {
        // --- Authentication & Setup ---
        composable(MMRADestinations.WELCOME) {
            WelcomeScreen(
                onLoginClick = {
                    navController.navigate(MMRADestinations.LOGIN)
                },
                onRegisterClick = {
                    navController.navigate(MMRADestinations.REGISTER)
                }
            )
        }

        composable(MMRADestinations.LOGIN) {
            LoginScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onGoogleSignInClick = {
                    authenticateAndProceed(navController, context)
                },
                onPhoneSignInClick = {
                    navController.navigate(MMRADestinations.PHONE_LOGIN)
                },
                onEmailSignInClick = {
                    navController.navigate(MMRADestinations.EMAIL_LOGIN)
                },
                onRegisterClick = {
                    navController.navigate(MMRADestinations.REGISTER)
                }
            )
        }

        composable(MMRADestinations.PHONE_LOGIN) {
            PhoneLoginScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onLoginClick = {
                    authenticateAndProceed(navController, context)
                },
                onOtpLoginClick = {
                    authenticateAndProceed(navController, context)
                },
                onForgotPasswordClick = {
                    navController.navigate(MMRADestinations.FORGOT_PASSWORD)
                },
                onRegisterClick = {
                    navController.navigate(MMRADestinations.REGISTER)
                }
            )
        }

        composable(MMRADestinations.EMAIL_LOGIN) {
            EmailLoginScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onLoginClick = {
                    authenticateAndProceed(navController, context)
                },
                onOtpLoginClick = {
                    authenticateAndProceed(navController, context)
                },
                onGoogleLoginClick = {
                    authenticateAndProceed(navController, context)
                },
                onForgotPasswordClick = {
                    navController.navigate(MMRADestinations.FORGOT_PASSWORD)
                },
                onRegisterClick = {
                    navController.navigate(MMRADestinations.REGISTER)
                }
            )
        }

        composable(MMRADestinations.REGISTER) {
            RegisterScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onRegisterClick = {
                    navController.navigate(MMRADestinations.VERIFY_PHONE)
                },
                onGoogleClick = {
                    authenticateAndProceed(navController, context)
                },
                onLoginClick = {
                    navController.navigate(MMRADestinations.LOGIN)
                }
            )
        }

        composable(MMRADestinations.VERIFY_PHONE) {
            VerifyPhoneScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onVerifyClick = {
                    navController.navigate(MMRADestinations.VERIFY_EMAIL)
                },
                onChangePhoneClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(MMRADestinations.VERIFY_EMAIL) {
            VerifyEmailScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onVerifyClick = {
                    navController.navigate(MMRADestinations.COMPLETION)
                },
                onChangeEmailClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(MMRADestinations.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSendCodeClick = {
                    navController.navigate(MMRADestinations.CREATE_NEW_PASSWORD)
                },
                onBackToLoginClick = {
                    navController.navigate(MMRADestinations.LOGIN) {
                        popUpTo(MMRADestinations.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(MMRADestinations.CREATE_NEW_PASSWORD) {
            CreateNewPasswordScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSavePasswordClick = {
                    navController.navigate(MMRADestinations.COMPLETION)
                }
            )
        }

        composable(MMRADestinations.COMPLETION) {
            CompletionScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onContinueClick = {
                    authenticateAndProceed(navController, context)
                }
            )
        }

        composable(MMRADestinations.INITIAL_SETUP_TARGET) {
            SetupTargetUserScreen(
                initialSelectedRoles = setupPreferences.selectedRoles,
                onBackClick = {},
                onContinueClick = { roles ->
                    setupPreferences = setupPreferences.copy(selectedRoles = roles)
                    navController.navigate(MMRADestinations.INITIAL_SETUP_DISPLAY) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(MMRADestinations.INITIAL_SETUP_DISPLAY) {
            SetupDisplayPreferenceScreen(
                initialLargeText = setupPreferences.isLargeTextEnabled,
                initialVoiceGuidance = setupPreferences.isVoiceGuidanceEnabled,
                onBackClick = {
                    navController.popBackStack()
                },
                onCompleteClick = { isLargeText, isVoiceGuidance ->
                    setupPreferences = setupPreferences.copy(
                        isLargeTextEnabled = isLargeText,
                        isVoiceGuidanceEnabled = isVoiceGuidance
                    )
                    UserProfileManager.setSetupCompleted(context, true)
                    UserProfileManager.setLoggedIn(context, true)
                    navController.navigate(MMRADestinations.HOME) {
                        popUpTo(MMRADestinations.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        // --- Profile & Account Management ---
        composable(MMRADestinations.MY_ACCOUNT) {
            MyAccountScreen(
                avatarBitmap = UserProfileManager.currentAvatarBitmap,
                onBackClick = {
                    navController.popBackStack()
                },
                onAvatarOptionsClick = {
                    navController.navigate(MMRADestinations.MY_ACCOUNT_AVATAR_OPTIONS)
                },
                onEditNameClick = {
                    navController.navigate(MMRADestinations.EDIT_NAME)
                },
                onEditEmailClick = {
                    navController.navigate(MMRADestinations.EDIT_EMAIL)
                },
                onEditPhoneClick = {
                    navController.navigate(MMRADestinations.EDIT_PHONE)
                },
                onChangePasswordClick = {
                    navController.navigate(MMRADestinations.CHANGE_PASSWORD)
                },
                onNotificationsClick = {
                    navController.navigate(MMRADestinations.NOTIFICATION_SETTINGS)
                },
                onAccessibilityClick = {
                    navController.navigate(MMRADestinations.ACCESSIBILITY_SETTINGS)
                },
                onVoiceGuidanceClick = {
                    navController.navigate(MMRADestinations.VOICE_GUIDANCE_SETTINGS)
                },
                onUsageModeClick = {
                    navController.navigate(MMRADestinations.MANAGE_USAGE_MODE)
                },
                onSwitchAccountClick = {
                    navController.navigate(MMRADestinations.SWITCH_ACCOUNT)
                },
                onLogoutClick = {
                    navController.navigate(MMRADestinations.LOGOUT_CONFIRMATION)
                }
            )
        }

        composable(MMRADestinations.MY_ACCOUNT_AVATAR_OPTIONS) {
            AvatarOptionsScreen(
                avatarBitmap = UserProfileManager.currentAvatarBitmap,
                onTakePhotoClick = {
                    navController.navigate(MMRADestinations.CAPTURE_AVATAR)
                },
                onPickGalleryClick = {
                    navController.navigate(MMRADestinations.GALLERY_PICKER)
                },
                onDeletePhotoClick = {
                    UserProfileManager.deleteAvatar(context)
                    navController.navigate(MMRADestinations.AVATAR_DELETED_SUCCESS)
                },
                onCancelClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(MMRADestinations.CAPTURE_AVATAR) {
            CaptureAvatarScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onPhotoCaptured = { bitmap ->
                    UserProfileManager.tempCapturedBitmap = bitmap
                    navController.navigate(MMRADestinations.REVIEW_CAPTURED_PHOTO)
                },
                onCancelClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(MMRADestinations.REVIEW_CAPTURED_PHOTO) {
            ReviewCapturedPhotoScreen(
                capturedBitmap = UserProfileManager.tempCapturedBitmap,
                onBackClick = {
                    navController.popBackStack()
                },
                onContinueCropClick = {
                    navController.navigate(MMRADestinations.CROP_AVATAR)
                },
                onRetakeClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(MMRADestinations.GALLERY_PICKER) {
            GalleryPickerScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onPhotoSelected = { bitmap ->
                    UserProfileManager.tempCapturedBitmap = bitmap
                    navController.navigate(MMRADestinations.CROP_AVATAR)
                },
                onCancelClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(MMRADestinations.CROP_AVATAR) {
            CropAvatarScreen(
                inputBitmap = UserProfileManager.tempCapturedBitmap,
                onBackClick = {
                    navController.popBackStack()
                },
                onConfirmCrop = { croppedBitmap ->
                    UserProfileManager.saveAvatarBitmap(context, croppedBitmap)
                    navController.navigate(MMRADestinations.AVATAR_UPDATED_SUCCESS)
                }
            )
        }

        composable(MMRADestinations.AVATAR_UPDATED_SUCCESS) {
            AvatarUpdatedScreen(
                updatedBitmap = UserProfileManager.currentAvatarBitmap,
                onBackToAccountClick = {
                    navController.popBackStack(MMRADestinations.MY_ACCOUNT, false)
                }
            )
        }

        composable(MMRADestinations.AVATAR_DELETED_SUCCESS) {
            AvatarDeletedScreen(
                onBackToAccountClick = {
                    navController.popBackStack(MMRADestinations.MY_ACCOUNT, false)
                }
            )
        }

        composable(MMRADestinations.EDIT_NAME) {
            EditNameScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() },
                onCancelClick = { navController.popBackStack() }
            )
        }

        composable(MMRADestinations.EDIT_EMAIL) {
            EditEmailScreen(
                onBackClick = { navController.popBackStack() },
                onContinueVerify = { navController.popBackStack() },
                onCancelClick = { navController.popBackStack() }
            )
        }

        composable(MMRADestinations.EDIT_PHONE) {
            EditPhoneScreen(
                onBackClick = { navController.popBackStack() },
                onContinueVerify = { navController.popBackStack() },
                onCancelClick = { navController.popBackStack() }
            )
        }

        composable(MMRADestinations.CHANGE_PASSWORD) {
            ChangePasswordScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() },
                onForgotPasswordClick = { navController.navigate(MMRADestinations.FORGOT_PASSWORD) },
                onCancelClick = { navController.popBackStack() }
            )
        }

        composable(MMRADestinations.NOTIFICATION_SETTINGS) {
            NotificationSettingsScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }

        composable(MMRADestinations.ACCESSIBILITY_SETTINGS) {
            AccessibilitySettingsScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() },
                onResetClick = { navController.navigate(MMRADestinations.INITIAL_SETUP_TARGET) }
            )
        }

        composable(MMRADestinations.VOICE_GUIDANCE_SETTINGS) {
            VoiceGuidanceSettingsScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }

        composable(MMRADestinations.MANAGE_USAGE_MODE) {
            ManageUsageModeScreen(
                initialSelectedRoles = setupPreferences.selectedRoles,
                onBackClick = { navController.popBackStack() },
                onCancelClick = { navController.popBackStack() },
                onConfirmSave = { updatedRoles ->
                    setupPreferences = setupPreferences.copy(selectedRoles = updatedRoles)
                    navController.popBackStack()
                }
            )
        }

        composable(MMRADestinations.SWITCH_ACCOUNT) {
            SwitchAccountScreen(
                avatarBitmap = UserProfileManager.currentAvatarBitmap,
                onBackClick = { navController.popBackStack() },
                onAddAccountClick = { navController.navigate(MMRADestinations.LOGIN) },
                onCancelClick = { navController.popBackStack() }
            )
        }

        composable(MMRADestinations.LOGOUT_CONFIRMATION) {
            LogoutConfirmationScreen(
                avatarBitmap = UserProfileManager.currentAvatarBitmap,
                onBackClick = { navController.popBackStack() },
                onCancelClick = { navController.popBackStack() },
                onConfirmLogout = {
                    UserProfileManager.setLoggedIn(context, false)
                    UserProfileManager.setSetupCompleted(context, false)
                    navController.navigate(MMRADestinations.WELCOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // --- Main Feature Destinations ---
        composable(MMRADestinations.ACCOUNT) {
            MyAccountScreen(
                avatarBitmap = UserProfileManager.currentAvatarBitmap,
                onBackClick = { navController.popBackStack() },
                onLogoutClick = {
                    UserProfileManager.setLoggedIn(context, false)
                    navController.navigate(MMRADestinations.WELCOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
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
                },
                onAccountClick = {
                    navController.navigate(MMRADestinations.MY_ACCOUNT) {
                        launchSingleTop = true
                    }
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
                    navController.navigate(MMRADestinations.MY_ACCOUNT)
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
                    navController.navigate(MMRADestinations.MY_ACCOUNT)
                }
            )
        }

        composable(MMRADestinations.SCAN) {
            ScanScreen(
                onNavigateToAddMedicine = { _, _ ->
                    navController.navigate(MMRADestinations.MANUAL_MEDICINE)
                },
                onNavigateToManualInput = {
                    navController.navigate(MMRADestinations.MANUAL_MEDICINE)
                },
                onClose = {
                    if (!navController.popBackStack()) {
                        navController.navigate(MMRADestinations.HOME)
                    }
                }
            )
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
                    navController.navigate(MMRADestinations.MY_ACCOUNT)
                }
            )
        }

        composable(MMRADestinations.FAMILY) {
            FamilyScreen(
                onMemberClick = { memberId ->
                    navController.navigate(MMRADestinations.familyMemberDetail(memberId))
                },
                onInviteClick = {
                    navController.navigate(MMRADestinations.FAMILY_INVITE)
                },
                onInviteDetailClick = {
                    navController.navigate(MMRADestinations.FAMILY_INVITE_DETAIL)
                },
                onAlertsClick = {
                    navController.navigate(MMRADestinations.FAMILY_ALERTS)
                },
                onAccountClick = {
                    navController.navigate(MMRADestinations.MY_ACCOUNT)
                }
            )
        }

        composable(MMRADestinations.FAMILY_INVITE) {
            InviteFamilyScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(MMRADestinations.FAMILY_INVITE_DETAIL) {
            FamilyInviteDetailScreen(
                onAcceptClick = {
                    navController.navigate(MMRADestinations.FAMILY_PERMISSIONS)
                },
                onDeclineClick = {
                    navController.popBackStack()
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
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

        composable(MMRADestinations.FAMILY_PERMISSIONS) {
            FamilyPermissionsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = MMRADestinations.FAMILY_PERMISSIONS_MEMBER,
            arguments = listOf(navArgument("memberId") { type = NavType.StringType })
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getString("memberId") ?: ""
            val member = MockFamilyData.mockMembers.firstOrNull { it.id == memberId }
            FamilyPermissionsScreen(
                onBackClick = { navController.popBackStack() },
                memberName = member?.fullName ?: "Nguyễn Minh Anh",
                memberRole = if (member != null) "${member.relationship} • Hồ sơ theo dõi" else "Con gái • Người chăm sóc"
            )
        }

        composable(
            route = MMRADestinations.FAMILY_MEMBER_DETAIL,
            arguments = listOf(navArgument("memberId") { type = NavType.StringType })
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getString("memberId") ?: ""
            CaregiverMemberDetailScreen(
                memberId = memberId,
                onBackClick = { navController.popBackStack() },
                onViewHistoryClick = {
                    navController.navigate(MMRADestinations.HISTORY)
                },
                onPermissionsClick = {
                    navController.navigate(MMRADestinations.familyPermissions(memberId))
                }
            )
        }

        composable(MMRADestinations.FAMILY_ALERTS) {
            FamilyAlertsScreen(
                onBackClick = { navController.popBackStack() },
                onAlertClick = { alert ->
                    navController.navigate(MMRADestinations.familyMemberDetail(alert.memberId))
                }
            )
        }
    }
}

private fun authenticateAndProceed(
    navController: NavHostController,
    context: android.content.Context
) {
    UserProfileManager.setLoggedIn(context, true)
    val setupDone = UserProfileManager.isSetupCompleted(context)
    if (setupDone) {
        navController.navigate(MMRADestinations.HOME) {
            popUpTo(MMRADestinations.WELCOME) { inclusive = true }
        }
    } else {
        navController.navigate(MMRADestinations.INITIAL_SETUP_TARGET) {
            popUpTo(MMRADestinations.WELCOME) { inclusive = true }
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
                    "Các thao tác xác nhận được thực hiện tại lịch uống hôm hôm nay."
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
