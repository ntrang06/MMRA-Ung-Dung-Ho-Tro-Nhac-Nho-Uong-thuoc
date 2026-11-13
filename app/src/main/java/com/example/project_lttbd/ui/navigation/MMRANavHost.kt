package com.example.project_lttbd.ui.navigation

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.project_lttbd.data.device.DeviceManager
import com.example.project_lttbd.data.mock.MockCalendar
import com.example.project_lttbd.data.mock.MockDoseAction
import com.example.project_lttbd.data.mock.MockDoseActions
import com.example.project_lttbd.data.mock.MockFamilyData
import com.example.project_lttbd.data.mock.MockReminderDoses
import com.example.project_lttbd.data.model.InitialSetupPreferences
import com.example.project_lttbd.data.model.UserProfileManager
import com.example.project_lttbd.data.remote.AuthRepository
import com.example.project_lttbd.data.remote.DoseException
import com.example.project_lttbd.data.remote.DoseRepository
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.data.remote.UserProfileRepository
import com.example.project_lttbd.data.remote.dto.DoseActionDto
import com.example.project_lttbd.data.remote.dto.DoseResponseDto
import com.example.project_lttbd.data.remote.dto.DoseStatusDto
import com.example.project_lttbd.data.remote.dto.MedicineDto
import com.example.project_lttbd.data.repository.MedicineRepository
import com.example.project_lttbd.notifications.alarm.MedicationAlarmScheduler
import com.example.project_lttbd.notifications.storage.LocalNotificationStore
import com.example.project_lttbd.notifications.sync.MedicationReminderSync
import com.example.project_lttbd.ui.components.MedicineFormSkeleton
import com.example.project_lttbd.ui.components.ScheduleFormSkeleton
import com.example.project_lttbd.ui.components.rememberUiNow
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
import com.example.project_lttbd.ui.screens.family.CaregiverMemberDetailScreen
import com.example.project_lttbd.ui.screens.family.FamilyAlertsScreen
import com.example.project_lttbd.ui.screens.family.FamilyInviteDetailScreen
import com.example.project_lttbd.ui.screens.family.FamilyPermissionsScreen
import com.example.project_lttbd.ui.screens.family.FamilyScreen
import com.example.project_lttbd.ui.screens.family.InviteFamilyScreen
import com.example.project_lttbd.ui.screens.history.HistoryScreen
import com.example.project_lttbd.ui.screens.home.HomeScreen
import com.example.project_lttbd.ui.screens.medicine.ManualMedicineDraft
import com.example.project_lttbd.ui.screens.medicine.ManualMedicineScreen
import com.example.project_lttbd.ui.screens.medicine.MedicineDeleteAction
import com.example.project_lttbd.ui.screens.medicine.MedicineEditFlowScreen
import com.example.project_lttbd.ui.screens.medicine.MedicineScreen
import com.example.project_lttbd.ui.screens.notifications.NotificationHistoryScreen
import com.example.project_lttbd.ui.screens.reminder.DoseReminderScreen
import com.example.project_lttbd.ui.screens.reminder.ReminderDoseUiModel
import com.example.project_lttbd.ui.screens.reminder.ReminderNoticeUiModel
import com.example.project_lttbd.ui.screens.reminder.rememberReminderSpeech
import com.example.project_lttbd.ui.screens.scan.ScanScreen
import com.example.project_lttbd.ui.screens.schedule.MedicineSchedulesScreen
import com.example.project_lttbd.ui.screens.schedule.ScheduleCreateScreen
import com.example.project_lttbd.ui.screens.schedule.ScheduleEditScreen
import com.example.project_lttbd.ui.screens.schedule.ScheduleScreen
import com.example.project_lttbd.ui.screens.schedule.ScheduleSetupEditor
import com.example.project_lttbd.ui.screens.schedule.rememberScheduleOwnerUid
import com.example.project_lttbd.ui.screens.schedule.toScheduleDoseUiModel
import com.example.project_lttbd.ui.screens.setup.SetupDisplayPreferenceScreen
import com.example.project_lttbd.ui.screens.setup.SetupTargetUserScreen
import com.example.project_lttbd.ui.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

private const val MANUAL_MEDICINE_DRAFT_KEY = "manual_medicine_draft"

@Composable
fun MMRANavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val googleAuthViewModel: AuthViewModel = viewModel(
        key = "mmra_google_auth"
    )

    val signInGoogle: () -> Unit = {
        googleAuthViewModel.signInWithGoogle(context) { success ->
            if (success) {
                authenticateAndProceed(navController, context)
            }
        }
    }

    if (googleAuthViewModel.isLoading) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Đang đăng nhập Google") },
            text = {
                Column {
                    CircularProgressIndicator()
                    Text("Đang xác thực và tải hồ sơ của bạn…")
                }
            },
            confirmButton = {}
        )
    }

    googleAuthViewModel.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {
                googleAuthViewModel.clearError()
            },
            title = { Text("Chưa đăng nhập được") },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        googleAuthViewModel.clearError()
                    }
                ) {
                    Text("Đóng")
                }
            }
        )
    }

    UserProfileManager.init(context)

    val currentUser = AuthRepository.currentUser
    val isSetupCompleted = UserProfileManager.isSetupCompleted(context)

    val startDest = remember {
        when {
            currentUser == null ->
                MMRADestinations.WELCOME

            !isSetupCompleted ->
                MMRADestinations.INITIAL_SETUP_TARGET

            else ->
                MMRADestinations.HOME
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            DeviceManager.syncCurrentDevice(
                context,
                currentUser.uid
            )
        }
    }

    var setupPreferences by remember(currentUser?.uid) {
        mutableStateOf(InitialSetupPreferences())
    }

    NavHost(
        navController = navController,
        startDestination = startDest,
        modifier = modifier
    ) {
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
                onGoogleSignInClick = signInGoogle,
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
                    navController.navigate(
                        MMRADestinations.FORGOT_PASSWORD
                    )
                },
                onRegisterClick = {
                    navController.navigate(MMRADestinations.REGISTER)
                }
            )
        }

        composable(MMRADestinations.EMAIL_LOGIN) {
            val authViewModel: AuthViewModel = remember {
                AuthViewModel()
            }

            EmailLoginScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onLoginClick = { email, pass ->
                    authViewModel.signIn(
                        email,
                        pass,
                        context
                    ) { success ->
                        if (success) {
                            authenticateAndProceed(
                                navController,
                                context
                            )
                        }
                    }
                },
                onOtpLoginClick = {
                    authenticateAndProceed(navController, context)
                },
                onGoogleLoginClick = signInGoogle,
                onForgotPasswordClick = {
                    navController.navigate(
                        MMRADestinations.FORGOT_PASSWORD
                    )
                },
                onRegisterClick = {
                    navController.navigate(MMRADestinations.REGISTER)
                },
                isLoading = authViewModel.isLoading,
                externalError = authViewModel.errorMessage
            )
        }

        composable(MMRADestinations.REGISTER) {
            val authViewModel: AuthViewModel = remember {
                AuthViewModel()
            }

            RegisterScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onRegisterClick = { name, email, pass ->
                    authViewModel.signUp(
                        name,
                        email,
                        pass,
                        context
                    ) { success ->
                        if (success) {
                            navController.navigate(
                                MMRADestinations.INITIAL_SETUP_TARGET
                            ) {
                                popUpTo(MMRADestinations.WELCOME) {
                                    inclusive = true
                                }
                            }
                        }
                    }
                },
                onGoogleClick = signInGoogle,
                onLoginClick = {
                    navController.navigate(MMRADestinations.LOGIN)
                },
                isLoading = authViewModel.isLoading,
                externalError = authViewModel.errorMessage
            )
        }

        composable(MMRADestinations.VERIFY_PHONE) {
            VerifyPhoneScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onVerifyClick = {
                    navController.navigate(
                        MMRADestinations.VERIFY_EMAIL
                    )
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
                    navController.navigate(
                        MMRADestinations.COMPLETION
                    )
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
                onBackToLoginClick = {
                    navController.navigate(MMRADestinations.LOGIN) {
                        popUpTo(MMRADestinations.LOGIN) {
                            inclusive = true
                        }
                        launchSingleTop = true
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
                    navController.navigate(
                        MMRADestinations.COMPLETION
                    )
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
                    setupPreferences = setupPreferences.copy(
                        selectedRoles = roles
                    )

                    navController.navigate(
                        MMRADestinations.INITIAL_SETUP_DISPLAY
                    ) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(MMRADestinations.INITIAL_SETUP_DISPLAY) {
            SetupDisplayPreferenceScreen(
                initialLargeText = setupPreferences.isLargeTextEnabled,
                initialVoiceGuidance =
                    setupPreferences.isVoiceGuidanceEnabled,
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

                    val uid = AuthRepository.currentUid

                    if (uid != null) {
                        val roleStr = setupPreferences.selectedRoles
                            .joinToString(",") {
                                it.name
                            }

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                UserProfileRepository.updateProfile(
                                    uid,
                                    mapOf("usageMode" to roleStr)
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    navController.navigate(MMRADestinations.HOME) {
                        popUpTo(MMRADestinations.WELCOME) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(MMRADestinations.MY_ACCOUNT) {
            MyAccountScreen(
                avatarBitmap = UserProfileManager.currentAvatarBitmap,
                onBackClick = {
                    navController.popBackStack()
                },
                onAvatarOptionsClick = {
                    navController.navigate(
                        MMRADestinations.MY_ACCOUNT_AVATAR_OPTIONS
                    )
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
                    navController.navigate(
                        MMRADestinations.CHANGE_PASSWORD
                    )
                },
                onNotificationsClick = {
                    navController.navigate(
                        MMRADestinations.NOTIFICATION_SETTINGS
                    )
                },
                onAccessibilityClick = {
                    navController.navigate(
                        MMRADestinations.ACCESSIBILITY_SETTINGS
                    )
                },
                onVoiceGuidanceClick = {
                    navController.navigate(
                        MMRADestinations.VOICE_GUIDANCE_SETTINGS
                    )
                },
                onUsageModeClick = {
                    navController.navigate(
                        MMRADestinations.MANAGE_USAGE_MODE
                    )
                },
                onSwitchAccountClick = {
                    navController.navigate(
                        MMRADestinations.SWITCH_ACCOUNT
                    )
                },
                onLogoutClick = {
                    navController.navigate(
                        MMRADestinations.LOGOUT_CONFIRMATION
                    )
                }
            )
        }

        composable(MMRADestinations.MY_ACCOUNT_AVATAR_OPTIONS) {
            AvatarOptionsScreen(
                avatarBitmap = UserProfileManager.currentAvatarBitmap,
                onTakePhotoClick = {
                    navController.navigate(
                        MMRADestinations.CAPTURE_AVATAR
                    )
                },
                onPickGalleryClick = {
                    navController.navigate(
                        MMRADestinations.GALLERY_PICKER
                    )
                },
                onDeletePhotoClick = {
                    UserProfileManager.deleteAvatar(context)
                    navController.navigate(
                        MMRADestinations.AVATAR_DELETED_SUCCESS
                    )
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
                    navController.navigate(
                        MMRADestinations.REVIEW_CAPTURED_PHOTO
                    )
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
                    navController.navigate(
                        MMRADestinations.CROP_AVATAR
                    )
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
                    navController.navigate(
                        MMRADestinations.CROP_AVATAR
                    )
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
                    UserProfileManager.saveAvatarBitmap(
                        context,
                        croppedBitmap
                    )
                    navController.navigate(
                        MMRADestinations.AVATAR_UPDATED_SUCCESS
                    )
                }
            )
        }

        composable(MMRADestinations.AVATAR_UPDATED_SUCCESS) {
            AvatarUpdatedScreen(
                updatedBitmap = UserProfileManager.currentAvatarBitmap,
                onBackToAccountClick = {
                    navController.popBackStack(
                        MMRADestinations.MY_ACCOUNT,
                        false
                    )
                }
            )
        }

        composable(MMRADestinations.AVATAR_DELETED_SUCCESS) {
            AvatarDeletedScreen(
                onBackToAccountClick = {
                    navController.popBackStack(
                        MMRADestinations.MY_ACCOUNT,
                        false
                    )
                }
            )
        }

        composable(MMRADestinations.EDIT_NAME) {
            EditNameScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    navController.popBackStack()
                },
                onCancelClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(MMRADestinations.EDIT_EMAIL) {
            EditEmailScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onContinueVerify = {
                    navController.popBackStack()
                },
                onCancelClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(MMRADestinations.EDIT_PHONE) {
            EditPhoneScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onContinueVerify = {
                    navController.popBackStack()
                },
                onCancelClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(MMRADestinations.CHANGE_PASSWORD) {
            ChangePasswordScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    navController.popBackStack()
                },
                onForgotPasswordClick = {
                    navController.navigate(
                        MMRADestinations.FORGOT_PASSWORD
                    )
                },
                onCancelClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(MMRADestinations.NOTIFICATION_SETTINGS) {
            NotificationSettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    navController.popBackStack()
                }
            )
        }

        composable(MMRADestinations.ACCESSIBILITY_SETTINGS) {
            AccessibilitySettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    navController.popBackStack()
                },
                onResetClick = {
                    navController.navigate(
                        MMRADestinations.INITIAL_SETUP_TARGET
                    )
                }
            )
        }

        composable(MMRADestinations.VOICE_GUIDANCE_SETTINGS) {
            VoiceGuidanceSettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    navController.popBackStack()
                }
            )
        }

        composable(MMRADestinations.MANAGE_USAGE_MODE) {
            ManageUsageModeScreen(
                initialSelectedRoles = setupPreferences.selectedRoles,
                onBackClick = {
                    navController.popBackStack()
                },
                onCancelClick = {
                    navController.popBackStack()
                },
                onConfirmSave = { updatedRoles ->
                    setupPreferences = setupPreferences.copy(
                        selectedRoles = updatedRoles
                    )
                    navController.popBackStack()
                }
            )
        }

        composable(MMRADestinations.SWITCH_ACCOUNT) {
            SwitchAccountDestination(
                onBack = {
                    navController.popBackStack()
                },
                onSignedOut = {
                    navController.navigate(MMRADestinations.LOGIN) {
                        popUpTo(0) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(MMRADestinations.LOGOUT_CONFIRMATION) {
            LogoutDestination(
                onBack = {
                    navController.popBackStack()
                },
                onSignedOut = {
                    navController.navigate(
                        MMRADestinations.WELCOME
                    ) {
                        popUpTo(0) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(MMRADestinations.ACCOUNT) {
            MyAccountScreen(
                avatarBitmap = UserProfileManager.currentAvatarBitmap,
                onBackClick = {
                    navController.popBackStack()
                },
                onLogoutClick = {
                    navController.navigate(
                        MMRADestinations.LOGOUT_CONFIRMATION
                    ) {
                        launchSingleTop = true
                    }
                }
            )
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
                onViewDoseClick = {
                    navController.openReminder(it)
                },
                onAccountClick = {
                    navController.navigate(
                        MMRADestinations.MY_ACCOUNT
                    ) {
                        launchSingleTop = true
                    }
                },
                onNotificationsClick = {
                    navController.navigate(
                        MMRADestinations.NOTIFICATIONS
                    ) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(MMRADestinations.NOTIFICATIONS) {
            val notificationContext = LocalContext.current
            val currentUid =
                FirebaseProvider.auth.currentUser?.uid.orEmpty()

            var notifications by remember {
                mutableStateOf(
                    LocalNotificationStore.getNotifications(
                        notificationContext,
                        currentUid
                    )
                )
            }

            NotificationHistoryScreen(
                notifications = notifications,
                onNotificationClick = { item ->
                    LocalNotificationStore.markAsRead(
                        notificationContext,
                        currentUid,
                        item.id
                    )

                    notifications =
                        LocalNotificationStore.getNotifications(
                            notificationContext,
                            currentUid
                        )

                    if (item.destinationRoute.isNotBlank()) {
                        navController.navigate(
                            item.destinationRoute
                        ) {
                            launchSingleTop = true
                        }
                    } else if (item.doseId.isNotBlank()) {
                        navController.openReminder(item.doseId)
                    }
                },
                onMarkAllAsRead = {
                    LocalNotificationStore.markAllAsRead(
                        notificationContext,
                        currentUid
                    )
                    notifications =
                        LocalNotificationStore.getNotifications(
                            notificationContext,
                            currentUid
                        )
                },
                onClearAll = {
                    LocalNotificationStore.clearAll(
                        notificationContext,
                        currentUid
                    )
                    notifications = emptyList()
                },
                onBackClick = {
                    navController.popBackStack()
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
                    navController.navigate(
                        MMRADestinations.MY_ACCOUNT
                    )
                },
                onScheduleClick = { id ->
                    navController.navigate(
                        "medicine/schedules/${Uri.encode(id)}"
                    )
                }
            )
        }

        composable(
            route = "medicine/schedules/{medicineId}",
            arguments = listOf(
                navArgument("medicineId") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val medicineId = entry.arguments
                ?.getString("medicineId")
                .orEmpty()

            MedicineSchedulesScreen(
                medicineId = medicineId,
                onBackClick = {
                    navController.popBackStack()
                },
                onCreateClick = {
                    navController.navigate(
                        "${MMRADestinations.SCHEDULE_SETUP}/${Uri.encode(medicineId)}"
                    )
                },
                onEditClick = { scheduleId ->
                    navController.navigate(
                        "schedule/edit/${Uri.encode(scheduleId)}"
                    )
                }
            )
        }

        composable(
            route = "schedule/edit/{scheduleId}",
            arguments = listOf(
                navArgument("scheduleId") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            ScheduleEditScreen(
                scheduleId = entry.arguments
                    ?.getString("scheduleId")
                    .orEmpty(),
                onBackClick = {
                    navController.popBackStack()
                },
                onSaved = {
                    Toast.makeText(
                        context,
                        "Đã cập nhật lịch uống",
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
        }

        composable(MMRADestinations.MANUAL_MEDICINE) { entry ->
            MedicineEntryDestination(
                navController = navController,
                entry = entry
            )
        }

        composable(
            route = MMRADestinations.EDIT_MEDICINE,
            arguments = listOf(
                navArgument("medicineId") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            MedicineEditFlowScreen(
                medicineId = entry.arguments
                    ?.getString("medicineId")
                    .orEmpty(),
                onBackClick = {
                    navController.popBackStack()
                },
                onFinished = {
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
        }

        composable(
            route = "${MMRADestinations.SCHEDULE_SETUP}/{medicineId}",
            arguments = listOf(
                navArgument("medicineId") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            ScheduleCreateScreen(
                medicineId = entry.arguments
                    ?.getString("medicineId")
                    .orEmpty(),
                onBackClick = {
                    navController.popBackStack()
                },
                onSaved = {
                    Toast.makeText(
                        context,
                        "Đã lưu lịch uống",
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
                    navController.navigate(
                        MMRADestinations.MY_ACCOUNT
                    )
                }
            )
        }

        composable(MMRADestinations.SCAN) { scanEntry ->
            ScanScreen(
                onNavigateToSchedule = { draft ->
                    scanEntry.savedStateHandle[
                        MANUAL_MEDICINE_DRAFT_KEY
                    ] = Bundle().apply {
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
                },
                onNavigateToManualInput = {
                    navController.navigate(
                        MMRADestinations.MANUAL_MEDICINE
                    )
                },
                onClose = {
                    if (!navController.popBackStack()) {
                        navController.navigate(
                            MMRADestinations.HOME
                        ) {
                            launchSingleTop = true
                        }
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
                    navController.navigate(
                        MMRADestinations.MY_ACCOUNT
                    )
                }
            )
        }

        composable(MMRADestinations.FAMILY) {
            FamilyScreen(
                onMemberClick = { memberId ->
                    navController.navigate(
                        MMRADestinations.familyMemberDetail(memberId)
                    )
                },
                onInviteClick = {
                    navController.navigate(
                        MMRADestinations.FAMILY_INVITE
                    )
                },
                onInviteDetailClick = {
                    navController.navigate(
                        MMRADestinations.FAMILY_INVITE_DETAIL
                    )
                },
                onAlertsClick = {
                    navController.navigate(
                        MMRADestinations.FAMILY_ALERTS
                    )
                },
                onAccountClick = {
                    navController.navigate(
                        MMRADestinations.MY_ACCOUNT
                    )
                }
            )
        }

        composable(MMRADestinations.FAMILY_INVITE) {
            InviteFamilyScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(MMRADestinations.FAMILY_INVITE_DETAIL) {
            FamilyInviteDetailScreen(
                onAcceptClick = {
                    navController.navigate(
                        MMRADestinations.FAMILY_PERMISSIONS
                    )
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
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = MMRADestinations.FAMILY_PERMISSIONS_MEMBER,
            arguments = listOf(
                navArgument("memberId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments
                ?.getString("memberId")
                ?: ""

            val member = MockFamilyData.mockMembers.firstOrNull {
                it.id == memberId
            }

            FamilyPermissionsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                memberName = member?.fullName ?: "Nguyễn Minh Anh",
                memberRole = if (member != null) {
                    "${member.relationship} • Hồ sơ theo dõi"
                } else {
                    "Con gái • Người chăm sóc"
                }
            )
        }

        composable(
            route = MMRADestinations.FAMILY_MEMBER_DETAIL,
            arguments = listOf(
                navArgument("memberId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments
                ?.getString("memberId")
                ?: ""

            CaregiverMemberDetailScreen(
                memberId = memberId,
                onBackClick = {
                    navController.popBackStack()
                },
                onViewHistoryClick = {
                    navController.navigate(
                        MMRADestinations.HISTORY
                    )
                },
                onPermissionsClick = {
                    navController.navigate(
                        MMRADestinations.familyPermissions(memberId)
                    )
                }
            )
        }

        composable(MMRADestinations.FAMILY_ALERTS) {
            FamilyAlertsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onAlertClick = { alert ->
                    navController.navigate(
                        MMRADestinations.familyMemberDetail(
                            alert.memberId
                        )
                    )
                }
            )
        }
    }
}

private fun authenticateAndProceed(
    navController: NavHostController,
    context: android.content.Context
) {
    if (AuthRepository.currentUser == null) {
        Toast.makeText(
            context,
            "Vui lòng đăng nhập bằng email hoặc Google trước khi tiếp tục.",
            Toast.LENGTH_LONG
        ).show()
        return
    }

    UserProfileManager.setLoggedIn(context, true)

    val setupDone = UserProfileManager.isSetupCompleted(context)

    if (setupDone) {
        navController.navigate(MMRADestinations.HOME) {
            popUpTo(MMRADestinations.WELCOME) {
                inclusive = true
            }
        }
    } else {
        navController.navigate(
            MMRADestinations.INITIAL_SETUP_TARGET
        ) {
            popUpTo(MMRADestinations.WELCOME) {
                inclusive = true
            }
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
    val context = LocalContext.current
    val repository = remember { MedicineRepository() }
    val auth = FirebaseProvider.auth

    var userId by remember(entry) {
        mutableStateOf<String?>(null)
    }
    var initialDraft by remember(entry) {
        mutableStateOf<ManualMedicineDraft?>(null)
    }
    var savedImageUrl by remember(entry) {
        mutableStateOf<String?>(null)
    }
    var loading by remember(entry) {
        mutableStateOf(true)
    }
    var signedIn by remember(entry) {
        mutableStateOf(false)
    }
    var loadError by remember(entry) {
        mutableStateOf<String?>(null)
    }
    var saveError by remember(entry) {
        mutableStateOf<String?>(null)
    }
    var saving by remember(entry) {
        mutableStateOf(false)
    }
    var pendingDraft by remember(entry) {
        mutableStateOf<ManualMedicineDraft?>(null)
    }
    var retryVersion by remember(entry) {
        mutableStateOf(0)
    }
    var requestVersion by remember(entry) {
        mutableStateOf(0)
    }
    var listening by remember(entry) {
        mutableStateOf(false)
    }

    DisposableEffect(entry, medicineId, retryVersion) {
        listening = true

        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val version = ++requestVersion
            val uid = firebaseAuth.currentUser?.uid

            userId = uid
            signedIn = uid != null
            initialDraft = null
            savedImageUrl = null
            loadError = null
            saveError = null
            pendingDraft = null
            saving = false
            loading = uid != null && medicineId != null

            if (uid != null && medicineId != null) {
                repository.getMedicine(medicineId)
                    .addOnSuccessListener { record ->
                        if (
                            listening &&
                            version == requestVersion &&
                            auth.currentUser?.uid == uid
                        ) {
                            val medicine = record.data

                            initialDraft = ManualMedicineDraft(
                                medicineName = medicine.medicineName,
                                strength = medicine.strength,
                                strengthUnit = medicine.strengthUnit,
                                dosageForm = medicine.dosageForm,
                                note = medicine.note,
                                imageUri = medicine.imageUrl
                            )

                            savedImageUrl = medicine.imageUrl
                            loading = false
                        }
                    }
                    .addOnFailureListener {
                        if (
                            listening &&
                            version == requestVersion &&
                            auth.currentUser?.uid == uid
                        ) {
                            loading = false
                            loadError =
                                "Không thể mở thuốc. Thuốc có thể đã bị xóa " +
                                        "hoặc kết nối đang gặp lỗi."
                        }
                    }
            }
        }

        auth.addAuthStateListener(listener)

        onDispose {
            listening = false
            requestVersion++
            auth.removeAuthStateListener(listener)
        }
    }

    BackHandler(enabled = saving) {
        // Chờ kết quả lưu trước khi rời màn hình.
    }

    when {
        loading -> {
            MedicineFormSkeleton()
        }

        !signedIn -> {
            Column {
                Text("Vui lòng đăng nhập để quản lý thuốc.")
                TextButton(
                    onClick = {
                        navController.navigate(
                            MMRADestinations.LOGIN
                        ) {
                            launchSingleTop = true
                        }
                    }
                ) {
                    Text("Đăng nhập")
                }
                TextButton(
                    onClick = {
                        navController.popBackStack()
                    }
                ) {
                    Text("Quay lại")
                }
            }
        }

        loadError != null -> {
            Column {
                Text(loadError.orEmpty())
                TextButton(
                    onClick = {
                        retryVersion++
                    }
                ) {
                    Text("Thử lại")
                }
                TextButton(
                    onClick = {
                        navController.popBackStack()
                    }
                ) {
                    Text("Quay lại")
                }
            }
        }

        else -> {
            key(userId, medicineId) {
                ManualMedicineScreen(
                    initialDraft = initialDraft,
                    isEditing = medicineId != null,
                    saveMedicineOnly = true,
                    deleteAction = if (medicineId != null) {
                        {
                            MedicineDeleteAction(
                                medicineId = medicineId,
                                medicineName =
                                    initialDraft?.medicineName.orEmpty(),
                                onDeleted = {
                                    val returnedToList =
                                        navController.popBackStack(
                                            MMRADestinations.MEDICINE,
                                            false
                                        )

                                    if (!returnedToList) {
                                        navController.navigate(
                                            MMRADestinations.MEDICINE
                                        ) {
                                            popUpTo(
                                                MMRADestinations.HOME
                                            )
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                        }
                    } else {
                        null
                    },
                    onBackClick = {
                        if (!saving) {
                            navController.popBackStack()
                        }
                    },
                    onContinue = { draft ->
                        if (!saving) {
                            pendingDraft = draft
                        }
                    }
                )
            }
        }
    }

    pendingDraft?.let { draft ->
        val hasLocalImage = draft.imageUri?.let {
            !it.startsWith("https://")
        } == true

        AlertDialog(
            onDismissRequest = {
                pendingDraft = null
            },
            title = {
                Text(
                    if (medicineId == null) {
                        "Lưu thuốc?"
                    } else {
                        "Lưu thay đổi?"
                    }
                )
            },
            text = {
                Column {
                    Text(
                        if (medicineId == null) {
                            "Lưu ${draft.medicineName} rồi tiếp tục thiết lập lịch uống."
                        } else {
                            "Lưu thay đổi thông tin ${draft.medicineName}."
                        }
                    )

                    if (hasLocalImage) {
                        Text(
                            "Ảnh vừa chọn chưa được tải lên nên " +
                                    "sẽ chưa được lưu cùng thuốc."
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uid = auth.currentUser?.uid

                        if (
                            uid == null ||
                            uid != userId ||
                            !listening
                        ) {
                            pendingDraft = null
                            saveError =
                                "Tài khoản đã thay đổi. " +
                                        "Vui lòng mở lại màn hình thuốc."
                        } else if (!saving) {
                            pendingDraft = null
                            saving = true
                            saveError = null

                            val version = ++requestVersion

                            val medicine = MedicineDto(
                                medicineName = draft.medicineName,
                                strength = draft.strength,
                                strengthUnit =
                                    if (draft.strength.isBlank()) {
                                        ""
                                    } else {
                                        draft.strengthUnit
                                    },
                                dosageForm = draft.dosageForm,
                                note = draft.note,
                                imageUrl = draft.imageUri
                                    ?.takeIf {
                                        it.startsWith("https://")
                                    }
                                    ?: savedImageUrl
                            )

                            val task: com.google.android.gms.tasks.Task<*> =
                                if (medicineId == null) {
                                    repository.addMedicine(medicine)
                                } else {
                                    repository.updateMedicine(
                                        medicineId,
                                        medicine
                                    )
                                }

                            task.addOnSuccessListener {
                                if (
                                    listening &&
                                    version == requestVersion &&
                                    auth.currentUser?.uid == uid
                                ) {
                                    Toast.makeText(
                                        context,
                                        "Đã lưu thông tin thuốc",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    saving = false

                                    if (medicineId == null) {
                                        val newId = task.result as? String

                                        if (newId.isNullOrBlank()) {
                                            saveError =
                                                "Chưa nhận được mã thuốc vừa lưu."
                                        } else {
                                            navController.navigate(
                                                "${MMRADestinations.SCHEDULE_SETUP}/${Uri.encode(newId)}"
                                            ) {
                                                popUpTo(
                                                    MMRADestinations.MANUAL_MEDICINE
                                                ) {
                                                    inclusive = true
                                                }
                                                launchSingleTop = true
                                            }
                                        }
                                    } else if (
                                        !navController.popBackStack(
                                            MMRADestinations.MEDICINE,
                                            false
                                        )
                                    ) {
                                        navController.navigate(
                                            MMRADestinations.MEDICINE
                                        ) {
                                            popUpTo(
                                                MMRADestinations.HOME
                                            )
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            }.addOnFailureListener {
                                if (
                                    listening &&
                                    version == requestVersion &&
                                    auth.currentUser?.uid == uid
                                ) {
                                    saving = false
                                    saveError =
                                        "Chưa lưu được thuốc. " +
                                                "Vui lòng kiểm tra kết nối và thử lại."
                                }
                            }
                        }
                    }
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDraft = null
                    }
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    if (saving) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Đang lưu thuốc") },
            text = {
                Column {
                    CircularProgressIndicator()
                    Text("Vui lòng chờ…")
                }
            },
            confirmButton = {}
        )
    }

    saveError?.let { message ->
        AlertDialog(
            onDismissRequest = {
                saveError = null
            },
            title = { Text("Chưa lưu được") },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        saveError = null
                    }
                ) {
                    Text("Đã hiểu")
                }
            }
        )
    }
}

@Composable
private fun ReminderDestination(
    doseId: String?,
    readOnly: Boolean,
    onBackClick: () -> Unit
) {
    val ownerUid = rememberScheduleOwnerUid()

    if (ownerUid == null) {
        Column(Modifier.padding(20.dp)) {
            Text("Vui lòng đăng nhập để xem liều thuốc.")
            TextButton(onClick = onBackClick) {
                Text("Quay lại")
            }
        }
        return
    }

    if (
        !doseId.isNullOrBlank() &&
        Regex("[0-9a-f]{64}").matches(doseId)
    ) {
        key(ownerUid, doseId) {
            ApiReminderDestination(
                ownerUid = ownerUid,
                doseId = doseId,
                readOnly = readOnly,
                onBackClick = onBackClick
            )
        }
    } else {
        // Trang chủ và Lịch sử sẽ được chuyển sang API ở bước sau.
        LegacyReminderDestination(
            doseId = doseId,
            readOnly = readOnly,
            onBackClick = onBackClick
        )
    }
}

private enum class ReminderApiAction {
    TAKEN,
    SKIPPED,
    SNOOZE
}

@Composable
private fun ApiReminderDestination(
    ownerUid: String,
    doseId: String,
    readOnly: Boolean,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { DoseRepository() }
    val scope = rememberCoroutineScope()

    var dose by remember {
        mutableStateOf<DoseResponseDto?>(null)
    }
    var loading by remember {
        mutableStateOf(true)
    }
    var loadError by remember {
        mutableStateOf<String?>(null)
    }
    var retry by remember {
        mutableStateOf(0)
    }
    var busy by remember {
        mutableStateOf(false)
    }
    var mustReload by remember {
        mutableStateOf(false)
    }
    var confirmation by remember {
        mutableStateOf<ReminderApiAction?>(null)
    }
    var actionError by remember {
        mutableStateOf<String?>(null)
    }
    var outcome by remember {
        mutableStateOf<String?>(null)
    }

    fun ensureOwner() {
        if (FirebaseProvider.auth.currentUser?.uid != ownerUid) {
            throw CancellationException(
                "Tài khoản đã thay đổi."
            )
        }
    }

    LaunchedEffect(retry) {
        loading = true
        loadError = null
        dose = null

        try {
            ensureOwner()

            val result = repository.getDose(doseId)

            currentCoroutineContext().ensureActive()
            ensureOwner()

            if (result.doseId != doseId) {
                throw DoseException(
                    "Máy chủ trả về liều thuốc chưa khớp. Vui lòng thử lại.",
                    "invalid_response"
                )
            }

            dose = result
            mustReload = false
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            currentCoroutineContext().ensureActive()
            ensureOwner()

            loadError = (error as? DoseException)?.message
                ?: "Chưa mở được liều thuốc. Vui lòng thử lại."
        } finally {
            loading = false
        }
    }

    BackHandler {
        if (!busy) {
            onBackClick()
        }
    }

    if (loading) {
        ScheduleFormSkeleton()
        return
    }

    val current = dose

    if (loadError != null || current == null) {
        Column(Modifier.padding(20.dp)) {
            Text("Chưa mở được liều thuốc")
            Text(loadError ?: "Không tìm thấy liều thuốc.")

            TextButton(
                onClick = {
                    retry++
                }
            ) {
                Text("Thử lại")
            }

            TextButton(onClick = onBackClick) {
                Text("Quay lại")
            }
        }
        return
    }

    val ui = current.toScheduleDoseUiModel()
    val now = rememberUiNow()

    val today = java.text.SimpleDateFormat(
        "yyyy-MM-dd",
        java.util.Locale.ROOT
    ).apply {
        timeZone = java.util.TimeZone.getTimeZone(
            current.timeZone
        )
    }.format(java.util.Date(now))

    val canAct = !readOnly &&
            current.localDate == today &&
            current.status == DoseStatusDto.PENDING &&
            !busy &&
            !mustReload &&
            outcome == null

    val reminder = ReminderDoseUiModel(
        doseId = current.doseId,
        medicineId = current.medicineId,
        timeLabel = "${ui.time} • ${current.localDate}",
        medicineName = ui.medicineName,
        quantityText = ui.quantityText,
        mealInstruction = ui.instruction,
        scheduledAtMillis = MedicationReminderSync.parseIsoToMillis(
            current.scheduledAt
        )
    )

    val playInstructions = rememberReminderSpeech(
        "${ui.medicineName}. ${ui.quantityText}. ${ui.instruction}."
    )

    fun submit(action: ReminderApiAction) {
        if (busy || mustReload || !canAct) {
            return
        }

        busy = true

        scope.launch {
            try {
                ensureOwner()

                val updated = when (action) {
                    ReminderApiAction.TAKEN ->
                        repository.updateDoseAction(
                            doseId,
                            DoseActionDto.TAKEN
                        )

                    ReminderApiAction.SKIPPED ->
                        repository.updateDoseAction(
                            doseId,
                            DoseActionDto.SKIPPED
                        )

                    ReminderApiAction.SNOOZE ->
                        repository.snoozeDose(
                            doseId,
                            minutes = 10
                        )
                }

                currentCoroutineContext().ensureActive()
                ensureOwner()

                if (updated.doseId != doseId) {
                    throw DoseException(
                        "Chưa nhận được kết quả hợp lệ. " +
                                "Vui lòng tải lại liều thuốc.",
                        "invalid_response"
                    )
                }

                dose = updated

                outcome = when (action) {
                    ReminderApiAction.TAKEN ->
                        "Đã ghi nhận bạn uống liều thuốc này."

                    ReminderApiAction.SKIPPED ->
                        "Đã ghi nhận bỏ qua liều thuốc này."

                    ReminderApiAction.SNOOZE ->
                        "Đã lưu yêu cầu nhắc lại sau 10 phút."
                }

                runCatching {
                    MedicationReminderSync.syncDoses(
                        context = context,
                        uid = ownerUid,
                        doses = listOf(updated)
                    )
                }.onFailure {
                    android.util.Log.w(
                        "MMRA_API_ERROR",
                        "Đã lưu thao tác nhưng chưa đồng bộ được báo thức cục bộ.",
                        it
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                ensureOwner()

                mustReload = true

                actionError = (error as? DoseException)?.message
                    ?: "Chưa xác nhận được kết quả thao tác. " +
                            "Vui lòng tải lại liều thuốc trước khi thử lại."
            } finally {
                busy = false
            }
        }
    }

    val notice = when {
        busy -> ReminderNoticeUiModel(
            "Đang lưu",
            "Vui lòng chờ máy chủ xác nhận."
        )

        mustReload -> ReminderNoticeUiModel(
            "Cần tải lại liều thuốc",
            "Vui lòng kiểm tra kết quả đã lưu trước khi thao tác tiếp."
        )

        current.status != DoseStatusDto.PENDING ->
            ReminderNoticeUiModel(
                ui.status.label,
                "Đây là trạng thái đã được ghi nhận trên máy chủ."
            )

        readOnly || current.localDate != today ->
            ReminderNoticeUiModel(
                "Xem chi tiết",
                "Thao tác xác nhận được thực hiện với liều hôm nay."
            )

        !current.nextReminderAt.isNullOrBlank() ->
            ReminderNoticeUiModel(
                "Đã chọn nhắc lại",
                "Liều này vẫn đang chờ xác nhận đã uống."
            )

        else -> null
    }

    DoseReminderScreen(
        dose = reminder,
        actionsEnabled = canAct,
        notice = notice,
        onClose = {
            if (!busy) {
                onBackClick()
            }
        },
        onTaken = {
            if (canAct) {
                confirmation = ReminderApiAction.TAKEN
            }
        },
        onSkip = {
            if (canAct) {
                confirmation = ReminderApiAction.SKIPPED
            }
        },
        onSnooze = {
            submit(ReminderApiAction.SNOOZE)
        },
        onPlayInstructions = playInstructions
    )

    confirmation?.let { action ->
        val taken = action == ReminderApiAction.TAKEN

        AlertDialog(
            onDismissRequest = {
                confirmation = null
            },
            title = {
                Text(
                    if (taken) {
                        "Xác nhận đã uống?"
                    } else {
                        "Bỏ qua liều này?"
                    }
                )
            },
            text = {
                Text(
                    if (taken) {
                        "Bạn đã uống ${ui.quantityText} ${ui.medicineName} " +
                                "cho liều ${ui.time}?"
                    } else {
                        "Liều ${ui.medicineName} lúc ${ui.time} " +
                                "sẽ được đánh dấu bỏ qua."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmation = null
                        submit(action)
                    }
                ) {
                    Text("Xác nhận")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmation = null
                    }
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    actionError?.let { message ->
        AlertDialog(
            onDismissRequest = {
                actionError = null
            },
            title = {
                Text("Kiểm tra kết quả")
            },
            text = {
                Text(message)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        actionError = null
                        retry++
                    }
                ) {
                    Text("Tải lại liều thuốc")
                }
            },
            dismissButton = {
                TextButton(onClick = onBackClick) {
                    Text("Quay lại")
                }
            }
        )
    }

    outcome?.let { message ->
        AlertDialog(
            onDismissRequest = onBackClick,
            title = {
                Text("Đã lưu")
            },
            text = {
                Text(message)
            },
            confirmButton = {
                TextButton(onClick = onBackClick) {
                    Text("Quay lại")
                }
            }
        )
    }
}

@Composable
private fun LegacyReminderDestination(
    doseId: String?,
    readOnly: Boolean,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
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
            if (canAct) {
                dialog = "CONFIRM_TAKEN"
            }
        },
        onSkip = {
            if (canAct) {
                dialog = "CONFIRM_SKIP"
            }
        },
        onSnooze = {
            if (canAct) {
                MockDoseActions.record(
                    it,
                    MockDoseAction.SNOOZED
                )

                val currentUid =
                    FirebaseProvider.auth.currentUser?.uid.orEmpty()

                MedicationAlarmScheduler.scheduleSnooze(
                    context = context,
                    uid = currentUid,
                    doseId = dose.doseId,
                    snoozeMinutes = 10,
                    title = "Nhắc lại: ${dose.medicineName}",
                    message =
                        "${dose.quantityText}. ${dose.mealInstruction}".trim(),
                    medicineName = dose.medicineName
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
                "Bạn đã uống ${dose.quantityText} ${dose.medicineName} " +
                        "cho liều ${dose.timeLabel}?"

            "CONFIRM_SKIP" ->
                "Liều ${dose.medicineName} sẽ được đánh dấu bỏ qua."

            "TAKEN" ->
                "Liều này đã được đánh dấu đã uống."

            "SKIPPED" ->
                "Liều này đã được đánh dấu bỏ qua."

            else ->
                "Đã chọn nhắc lại sau 10 phút. " +
                        "Liều này chưa được đánh dấu đã uống."
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
                                MedicationReminderSync.onDoseAction(
                                    context,
                                    dose.doseId
                                )
                                dialog = "TAKEN"
                            }

                            "CONFIRM_SKIP" -> {
                                MockDoseActions.record(
                                    dose.doseId,
                                    MockDoseAction.SKIPPED
                                )
                                MedicationReminderSync.onDoseAction(
                                    context,
                                    dose.doseId
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