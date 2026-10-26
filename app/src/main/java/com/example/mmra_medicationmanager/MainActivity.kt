package com.example.mmra_medicationmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.mmra_medicationmanager.model.InitialSetupPreferences
import com.example.mmra_medicationmanager.model.UserProfileManager
import com.example.mmra_medicationmanager.ui.screens.auth.CompletionScreen
import com.example.mmra_medicationmanager.ui.screens.auth.CreateNewPasswordScreen
import com.example.mmra_medicationmanager.ui.screens.auth.EmailLoginScreen
import com.example.mmra_medicationmanager.ui.screens.auth.ForgotPasswordScreen
import com.example.mmra_medicationmanager.ui.screens.auth.LoginScreen
import com.example.mmra_medicationmanager.ui.screens.auth.PhoneLoginScreen
import com.example.mmra_medicationmanager.ui.screens.auth.RegisterScreen
import com.example.mmra_medicationmanager.ui.screens.auth.VerifyEmailScreen
import com.example.mmra_medicationmanager.ui.screens.auth.VerifyPhoneScreen
import com.example.mmra_medicationmanager.ui.screens.auth.WelcomeScreen
import com.example.mmra_medicationmanager.ui.screens.home.HomeScreen
import com.example.mmra_medicationmanager.ui.screens.profile.AccessibilitySettingsScreen
import com.example.mmra_medicationmanager.ui.screens.profile.AvatarDeletedScreen
import com.example.mmra_medicationmanager.ui.screens.profile.AvatarOptionsScreen
import com.example.mmra_medicationmanager.ui.screens.profile.AvatarUpdatedScreen
import com.example.mmra_medicationmanager.ui.screens.profile.CaptureAvatarScreen
import com.example.mmra_medicationmanager.ui.screens.profile.ChangePasswordScreen
import com.example.mmra_medicationmanager.ui.screens.profile.CropAvatarScreen
import com.example.mmra_medicationmanager.ui.screens.profile.EditEmailScreen
import com.example.mmra_medicationmanager.ui.screens.profile.EditNameScreen
import com.example.mmra_medicationmanager.ui.screens.profile.EditPhoneScreen
import com.example.mmra_medicationmanager.ui.screens.profile.GalleryPickerScreen
import com.example.mmra_medicationmanager.ui.screens.profile.LogoutConfirmationScreen
import com.example.mmra_medicationmanager.ui.screens.profile.ManageUsageModeScreen
import com.example.mmra_medicationmanager.ui.screens.profile.MyAccountScreen
import com.example.mmra_medicationmanager.ui.screens.profile.NotificationSettingsScreen
import com.example.mmra_medicationmanager.ui.screens.profile.ReviewCapturedPhotoScreen
import com.example.mmra_medicationmanager.ui.screens.profile.SwitchAccountScreen
import com.example.mmra_medicationmanager.ui.screens.profile.VoiceGuidanceSettingsScreen
import com.example.mmra_medicationmanager.ui.screens.setup.SetupDisplayPreferenceScreen
import com.example.mmra_medicationmanager.ui.screens.setup.SetupTargetUserScreen

enum class AuthDestination {
    WELCOME,
    LOGIN,
    PHONE_LOGIN,
    EMAIL_LOGIN,
    REGISTER,
    VERIFY_PHONE,
    VERIFY_EMAIL,
    FORGOT_PASSWORD,
    CREATE_NEW_PASSWORD,
    COMPLETION,
    INITIAL_SETUP_TARGET,
    INITIAL_SETUP_DISPLAY,
    HOME,
    MY_ACCOUNT,
    MY_ACCOUNT_AVATAR_OPTIONS,
    CAPTURE_AVATAR,
    REVIEW_CAPTURED_PHOTO,
    GALLERY_PICKER,
    CROP_AVATAR,
    AVATAR_UPDATED_SUCCESS,
    AVATAR_DELETED_SUCCESS,
    EDIT_NAME,
    EDIT_EMAIL,
    EDIT_PHONE,
    CHANGE_PASSWORD,
    NOTIFICATION_SETTINGS,
    ACCESSIBILITY_SETTINGS,
    VOICE_GUIDANCE_SETTINGS,
    MANAGE_USAGE_MODE,
    SWITCH_ACCOUNT,
    LOGOUT_CONFIRMATION,
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        UserProfileManager.init(context)
    }

    var currentDestination by rememberSaveable {
        mutableStateOf(AuthDestination.WELCOME)
    }

    var isAuthenticated by rememberSaveable {
        mutableStateOf(false)
    }

    var isInitialSetupCompleted by rememberSaveable {
        mutableStateOf(false)
    }

    var setupPreferences by remember {
        mutableStateOf(InitialSetupPreferences())
    }

    fun onAuthenticationSuccess() {
        isAuthenticated = true

        currentDestination = if (isInitialSetupCompleted) {
            AuthDestination.HOME
        } else {
            AuthDestination.INITIAL_SETUP_TARGET
        }
    }

    when (currentDestination) {

        AuthDestination.WELCOME -> {
            WelcomeScreen(
                modifier = modifier,
                onLoginClick = {
                    currentDestination = AuthDestination.LOGIN
                },
                onRegisterClick = {
                    currentDestination = AuthDestination.REGISTER
                }
            )
        }

        AuthDestination.LOGIN -> {
            LoginScreen(
                modifier = modifier,
                onBackClick = {
                    currentDestination = AuthDestination.WELCOME
                },
                onGoogleSignInClick = {
                    onAuthenticationSuccess()
                },
                onPhoneSignInClick = {
                    currentDestination = AuthDestination.PHONE_LOGIN
                },
                onEmailSignInClick = {
                    currentDestination = AuthDestination.EMAIL_LOGIN
                },
                onRegisterClick = {
                    currentDestination = AuthDestination.REGISTER
                }
            )
        }

        AuthDestination.PHONE_LOGIN -> {
            PhoneLoginScreen(
                modifier = modifier,
                onBackClick = {
                    currentDestination = AuthDestination.LOGIN
                },
                onLoginClick = {
                    onAuthenticationSuccess()
                },
                onOtpLoginClick = {
                    onAuthenticationSuccess()
                },
                onForgotPasswordClick = {
                    currentDestination = AuthDestination.FORGOT_PASSWORD
                },
                onRegisterClick = {
                    currentDestination = AuthDestination.REGISTER
                }
            )
        }

        AuthDestination.EMAIL_LOGIN -> {
            EmailLoginScreen(
                modifier = modifier,
                onBackClick = {
                    currentDestination = AuthDestination.LOGIN
                },
                onLoginClick = {
                    onAuthenticationSuccess()
                },
                onOtpLoginClick = {
                    onAuthenticationSuccess()
                },
                onGoogleLoginClick = {
                    onAuthenticationSuccess()
                },
                onForgotPasswordClick = {
                    currentDestination = AuthDestination.FORGOT_PASSWORD
                },
                onRegisterClick = {
                    currentDestination = AuthDestination.REGISTER
                }
            )
        }

        AuthDestination.REGISTER -> {
            RegisterScreen(
                modifier = modifier,
                onBackClick = {
                    currentDestination = AuthDestination.LOGIN
                },
                onRegisterClick = {
                    currentDestination = AuthDestination.VERIFY_PHONE
                },
                onGoogleClick = {
                    onAuthenticationSuccess()
                },
                onLoginClick = {
                    currentDestination = AuthDestination.LOGIN
                }
            )
        }

        AuthDestination.VERIFY_PHONE -> {
            VerifyPhoneScreen(
                modifier = modifier,
                onBackClick = {
                    currentDestination = AuthDestination.REGISTER
                },
                onVerifyClick = {
                    currentDestination = AuthDestination.VERIFY_EMAIL
                },
                onChangePhoneClick = {
                    currentDestination = AuthDestination.REGISTER
                }
            )
        }

        AuthDestination.VERIFY_EMAIL -> {
            VerifyEmailScreen(
                modifier = modifier,
                onBackClick = {
                    currentDestination = AuthDestination.VERIFY_PHONE
                },
                onVerifyClick = {
                    currentDestination = AuthDestination.COMPLETION
                },
                onChangeEmailClick = {
                    currentDestination = AuthDestination.REGISTER
                }
            )
        }

        AuthDestination.FORGOT_PASSWORD -> {
            ForgotPasswordScreen(
                modifier = modifier,
                onBackClick = {
                    currentDestination = AuthDestination.LOGIN
                },
                onSendCodeClick = {
                    currentDestination = AuthDestination.CREATE_NEW_PASSWORD
                },
                onBackToLoginClick = {
                    currentDestination = AuthDestination.LOGIN
                }
            )
        }

        AuthDestination.CREATE_NEW_PASSWORD -> {
            CreateNewPasswordScreen(
                modifier = modifier,
                onBackClick = {
                    currentDestination = AuthDestination.FORGOT_PASSWORD
                },
                onSavePasswordClick = {
                    currentDestination = AuthDestination.COMPLETION
                }
            )
        }

        AuthDestination.COMPLETION -> {
            CompletionScreen(
                modifier = modifier,
                onBackClick = {
                    currentDestination = AuthDestination.LOGIN
                },
                onContinueClick = {
                    if (isAuthenticated) {
                        currentDestination =
                            if (isInitialSetupCompleted) {
                                AuthDestination.HOME
                            } else {
                                AuthDestination.INITIAL_SETUP_TARGET
                            }
                    } else {
                        onAuthenticationSuccess()
                    }
                }
            )
        }

        AuthDestination.INITIAL_SETUP_TARGET -> {
            SetupTargetUserScreen(
                modifier = modifier,
                initialSelectedRoles = setupPreferences.selectedRoles,
                onBackClick = {
                    // Không quay lại Login/Register sau khi đăng nhập.
                },
                onContinueClick = { roles ->
                    setupPreferences = setupPreferences.copy(
                        selectedRoles = roles
                    )

                    currentDestination =
                        AuthDestination.INITIAL_SETUP_DISPLAY
                }
            )
        }

        AuthDestination.INITIAL_SETUP_DISPLAY -> {
            SetupDisplayPreferenceScreen(
                modifier = modifier,
                initialLargeText =
                    setupPreferences.isLargeTextEnabled,
                initialVoiceGuidance =
                    setupPreferences.isVoiceGuidanceEnabled,
                onBackClick = {
                    currentDestination =
                        AuthDestination.INITIAL_SETUP_TARGET
                },
                onCompleteClick = { isLargeText, isVoiceGuidance ->
                    setupPreferences = setupPreferences.copy(
                        isLargeTextEnabled = isLargeText,
                        isVoiceGuidanceEnabled = isVoiceGuidance
                    )

                    isInitialSetupCompleted = true
                    currentDestination = AuthDestination.HOME
                }
            )
        }

        AuthDestination.HOME -> {
            HomeScreen(
                modifier = modifier,
                onProfileClick = {
                    currentDestination = AuthDestination.MY_ACCOUNT
                }
            )
        }

        AuthDestination.MY_ACCOUNT -> {
            MyAccountScreen(
                modifier = modifier,
                avatarBitmap = UserProfileManager.currentAvatarBitmap,
                onBackClick = {
                    currentDestination = AuthDestination.HOME
                },
                onAvatarOptionsClick = {
                    currentDestination = AuthDestination.MY_ACCOUNT_AVATAR_OPTIONS
                },
                onEditNameClick = {
                    currentDestination = AuthDestination.EDIT_NAME
                },
                onEditEmailClick = {
                    currentDestination = AuthDestination.EDIT_EMAIL
                },
                onEditPhoneClick = {
                    currentDestination = AuthDestination.EDIT_PHONE
                },
                onChangePasswordClick = {
                    currentDestination = AuthDestination.CHANGE_PASSWORD
                },
                onNotificationsClick = {
                    currentDestination = AuthDestination.NOTIFICATION_SETTINGS
                },
                onAccessibilityClick = {
                    currentDestination = AuthDestination.ACCESSIBILITY_SETTINGS
                },
                onVoiceGuidanceClick = {
                    currentDestination = AuthDestination.VOICE_GUIDANCE_SETTINGS
                },
                onUsageModeClick = {
                    currentDestination = AuthDestination.MANAGE_USAGE_MODE
                },
                onSwitchAccountClick = {
                    currentDestination = AuthDestination.SWITCH_ACCOUNT
                },
                onLogoutClick = {
                    currentDestination = AuthDestination.LOGOUT_CONFIRMATION
                }
            )
        }

        AuthDestination.MY_ACCOUNT_AVATAR_OPTIONS -> {
            AvatarOptionsScreen(
                modifier = modifier,
                avatarBitmap = UserProfileManager.currentAvatarBitmap,
                onTakePhotoClick = {
                    currentDestination = AuthDestination.CAPTURE_AVATAR
                },
                onPickGalleryClick = {
                    currentDestination = AuthDestination.GALLERY_PICKER
                },
                onDeletePhotoClick = {
                    UserProfileManager.deleteAvatar(context)
                    currentDestination = AuthDestination.AVATAR_DELETED_SUCCESS
                },
                onCancelClick = {
                    currentDestination = AuthDestination.MY_ACCOUNT
                }
            )
        }

        AuthDestination.CAPTURE_AVATAR -> {
            CaptureAvatarScreen(
                modifier = modifier,
                onBackClick = {
                    currentDestination = AuthDestination.MY_ACCOUNT_AVATAR_OPTIONS
                },
                onPhotoCaptured = { bitmap ->
                    UserProfileManager.tempCapturedBitmap = bitmap
                    currentDestination = AuthDestination.REVIEW_CAPTURED_PHOTO
                },
                onCancelClick = {
                    currentDestination = AuthDestination.MY_ACCOUNT_AVATAR_OPTIONS
                }
            )
        }

        AuthDestination.REVIEW_CAPTURED_PHOTO -> {
            ReviewCapturedPhotoScreen(
                modifier = modifier,
                capturedBitmap = UserProfileManager.tempCapturedBitmap,
                onBackClick = {
                    currentDestination = AuthDestination.CAPTURE_AVATAR
                },
                onContinueCropClick = {
                    currentDestination = AuthDestination.CROP_AVATAR
                },
                onRetakeClick = {
                    currentDestination = AuthDestination.CAPTURE_AVATAR
                }
            )
        }

        AuthDestination.GALLERY_PICKER -> {
            GalleryPickerScreen(
                modifier = modifier,
                onBackClick = {
                    currentDestination = AuthDestination.MY_ACCOUNT_AVATAR_OPTIONS
                },
                onPhotoSelected = { bitmap ->
                    UserProfileManager.tempCapturedBitmap = bitmap
                    currentDestination = AuthDestination.CROP_AVATAR
                },
                onCancelClick = {
                    currentDestination = AuthDestination.MY_ACCOUNT_AVATAR_OPTIONS
                }
            )
        }

        AuthDestination.CROP_AVATAR -> {
            CropAvatarScreen(
                modifier = modifier,
                inputBitmap = UserProfileManager.tempCapturedBitmap,
                onBackClick = {
                    currentDestination = AuthDestination.REVIEW_CAPTURED_PHOTO
                },
                onConfirmCrop = { croppedBitmap ->
                    currentDestination = AuthDestination.AVATAR_UPDATED_SUCCESS
                }
            )
        }

        AuthDestination.AVATAR_UPDATED_SUCCESS -> {
            AvatarUpdatedScreen(
                modifier = modifier,
                updatedBitmap = UserProfileManager.currentAvatarBitmap,
                onBackToAccountClick = {
                    currentDestination = AuthDestination.MY_ACCOUNT
                }
            )
        }

        AuthDestination.AVATAR_DELETED_SUCCESS -> {
            AvatarDeletedScreen(
                modifier = modifier,
                onBackToAccountClick = {
                    currentDestination = AuthDestination.MY_ACCOUNT
                }
            )
        }

        AuthDestination.EDIT_NAME -> {
            EditNameScreen(
                modifier = modifier,
                onBackClick = { currentDestination = AuthDestination.MY_ACCOUNT },
                onSaveSuccess = { currentDestination = AuthDestination.MY_ACCOUNT },
                onCancelClick = { currentDestination = AuthDestination.MY_ACCOUNT }
            )
        }

        AuthDestination.EDIT_EMAIL -> {
            EditEmailScreen(
                modifier = modifier,
                onBackClick = { currentDestination = AuthDestination.MY_ACCOUNT },
                onContinueVerify = { currentDestination = AuthDestination.MY_ACCOUNT },
                onCancelClick = { currentDestination = AuthDestination.MY_ACCOUNT }
            )
        }

        AuthDestination.EDIT_PHONE -> {
            EditPhoneScreen(
                modifier = modifier,
                onBackClick = { currentDestination = AuthDestination.MY_ACCOUNT },
                onContinueVerify = { currentDestination = AuthDestination.MY_ACCOUNT },
                onCancelClick = { currentDestination = AuthDestination.MY_ACCOUNT }
            )
        }

        AuthDestination.CHANGE_PASSWORD -> {
            ChangePasswordScreen(
                modifier = modifier,
                onBackClick = { currentDestination = AuthDestination.MY_ACCOUNT },
                onSaveSuccess = { currentDestination = AuthDestination.MY_ACCOUNT },
                onForgotPasswordClick = { currentDestination = AuthDestination.FORGOT_PASSWORD },
                onCancelClick = { currentDestination = AuthDestination.MY_ACCOUNT }
            )
        }

        AuthDestination.NOTIFICATION_SETTINGS -> {
            NotificationSettingsScreen(
                modifier = modifier,
                onBackClick = { currentDestination = AuthDestination.MY_ACCOUNT },
                onSaveSuccess = { currentDestination = AuthDestination.MY_ACCOUNT }
            )
        }

        AuthDestination.ACCESSIBILITY_SETTINGS -> {
            AccessibilitySettingsScreen(
                modifier = modifier,
                onBackClick = { currentDestination = AuthDestination.MY_ACCOUNT },
                onSaveSuccess = { currentDestination = AuthDestination.MY_ACCOUNT },
                onResetClick = { currentDestination = AuthDestination.INITIAL_SETUP_TARGET }
            )
        }

        AuthDestination.VOICE_GUIDANCE_SETTINGS -> {
            VoiceGuidanceSettingsScreen(
                modifier = modifier,
                onBackClick = { currentDestination = AuthDestination.MY_ACCOUNT },
                onSaveSuccess = { currentDestination = AuthDestination.MY_ACCOUNT }
            )
        }

        AuthDestination.MANAGE_USAGE_MODE -> {
            ManageUsageModeScreen(
                modifier = modifier,
                initialSelectedRoles = setupPreferences.selectedRoles,
                onBackClick = { currentDestination = AuthDestination.MY_ACCOUNT },
                onCancelClick = { currentDestination = AuthDestination.MY_ACCOUNT },
                onConfirmSave = { updatedRoles ->
                    setupPreferences = setupPreferences.copy(selectedRoles = updatedRoles)
                    currentDestination = AuthDestination.MY_ACCOUNT
                }
            )
        }

        AuthDestination.SWITCH_ACCOUNT -> {
            SwitchAccountScreen(
                modifier = modifier,
                avatarBitmap = UserProfileManager.currentAvatarBitmap,
                onBackClick = { currentDestination = AuthDestination.MY_ACCOUNT },
                onAddAccountClick = { currentDestination = AuthDestination.LOGIN },
                onCancelClick = { currentDestination = AuthDestination.MY_ACCOUNT }
            )
        }

        AuthDestination.LOGOUT_CONFIRMATION -> {
            LogoutConfirmationScreen(
                modifier = modifier,
                avatarBitmap = UserProfileManager.currentAvatarBitmap,
                onBackClick = { currentDestination = AuthDestination.MY_ACCOUNT },
                onCancelClick = { currentDestination = AuthDestination.MY_ACCOUNT },
                onConfirmLogout = {
                    isAuthenticated = false
                    currentDestination = AuthDestination.WELCOME
                }
            )
        }
    }
}
