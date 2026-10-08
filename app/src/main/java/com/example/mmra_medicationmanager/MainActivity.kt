package com.example.mmra_medicationmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.mmra_medicationmanager.model.InitialSetupPreferences
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
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
) {
    var currentDestination by rememberSaveable { mutableStateOf(AuthDestination.WELCOME) }
    var isAuthenticated by rememberSaveable { mutableStateOf(false) }
    var isInitialSetupCompleted by rememberSaveable { mutableStateOf(false) }
    var setupPreferences by rememberSaveable { mutableStateOf(InitialSetupPreferences()) }

    fun onAuthenticationSuccess() {
        isAuthenticated = true
        if (isInitialSetupCompleted) {
            currentDestination = AuthDestination.HOME
        } else {
            currentDestination = AuthDestination.INITIAL_SETUP_TARGET
        }
    }

    when (currentDestination) {
        AuthDestination.WELCOME -> {
            WelcomeScreen(
                modifier = modifier,
                onLoginClick = { currentDestination = AuthDestination.LOGIN },
                onRegisterClick = { currentDestination = AuthDestination.REGISTER },
            )
        }
        AuthDestination.LOGIN -> {
            LoginScreen(
                modifier = modifier,
                onBackClick = { currentDestination = AuthDestination.WELCOME },
                onGoogleSignInClick = { onAuthenticationSuccess() },
                onPhoneSignInClick = { currentDestination = AuthDestination.PHONE_LOGIN },
                onEmailSignInClick = { currentDestination = AuthDestination.EMAIL_LOGIN },
                onRegisterClick = { currentDestination = AuthDestination.REGISTER },
            )
        }

        AuthDestination.PHONE_LOGIN -> {
            PhoneLoginScreen(
                modifier = modifier,
                onBackClick = { currentDestination = AuthDestination.LOGIN },
                onLoginClick = { onAuthenticationSuccess() },
                onOtpLoginClick = { onAuthenticationSuccess() },
                onForgotPasswordClick = { currentDestination = AuthDestination.FORGOT_PASSWORD },
                onRegisterClick = { currentDestination = AuthDestination.REGISTER },
            )
        }

        AuthDestination.EMAIL_LOGIN -> {
            EmailLoginScreen(
                modifier = modifier,
                onBackClick = { currentDestination = AuthDestination.LOGIN },
                onLoginClick = { onAuthenticationSuccess() },
                onOtpLoginClick = { onAuthenticationSuccess() },
                onGoogleLoginClick = { onAuthenticationSuccess() },
                onForgotPasswordClick = { currentDestination = AuthDestination.FORGOT_PASSWORD },
                onRegisterClick = { currentDestination = AuthDestination.REGISTER },
            )
        }

        AuthDestination.REGISTER -> {
            RegisterScreen(
                modifier = modifier,
                onBackClick = { currentDestination = AuthDestination.LOGIN },
                onRegisterClick = { currentDestination = AuthDestination.VERIFY_PHONE },
                onGoogleClick = { onAuthenticationSuccess() },
                onLoginClick = { currentDestination = AuthDestination.LOGIN },
            )
        }

        AuthDestination.VERIFY_PHONE -> {
            VerifyPhoneScreen(
                modifier = modifier,
                onBackClick = { currentDestination = AuthDestination.REGISTER },
                onVerifyClick = { currentDestination = AuthDestination.VERIFY_EMAIL },
                onChangePhoneClick = { currentDestination = AuthDestination.REGISTER },
            )
        }

        AuthDestination.VERIFY_EMAIL -> {
            VerifyEmailScreen(
                modifier = modifier,
                onBackClick = { currentDestination = AuthDestination.VERIFY_PHONE },
                onVerifyClick = { currentDestination = AuthDestination.COMPLETION },
                onChangeEmailClick = { currentDestination = AuthDestination.REGISTER },
            )
        }

        AuthDestination.FORGOT_PASSWORD -> {
            ForgotPasswordScreen(
                modifier = modifier,
                onBackClick = { currentDestination = AuthDestination.LOGIN },
                onSendCodeClick = { currentDestination = AuthDestination.CREATE_NEW_PASSWORD },
                onBackToLoginClick = { currentDestination = AuthDestination.LOGIN },
            )
        }

        AuthDestination.CREATE_NEW_PASSWORD -> {
            CreateNewPasswordScreen(
                modifier = modifier,
                onBackClick = { currentDestination = AuthDestination.FORGOT_PASSWORD },
                onSavePasswordClick = { currentDestination = AuthDestination.COMPLETION },
            )
        }

        AuthDestination.COMPLETION -> {
            CompletionScreen(
                modifier = modifier,
                onBackClick = { currentDestination = AuthDestination.LOGIN },
                onContinueClick = {
                    if (isAuthenticated) {
                        if (isInitialSetupCompleted) {
                            currentDestination = AuthDestination.HOME
                        } else {
                            currentDestination = AuthDestination.INITIAL_SETUP_TARGET
                        }
                    } else {
                        onAuthenticationSuccess()
                    }
                },
            )
        }

        AuthDestination.INITIAL_SETUP_TARGET -> {
            SetupTargetUserScreen(
                modifier = modifier,
                initialTargetUser = setupPreferences.targetUser,
                onBackClick = {
                    // Do not allow returning to Login/Register screens once authenticated
                },
                onContinueClick = { targetUser ->
                    setupPreferences = setupPreferences.copy(targetUser = targetUser)
                    currentDestination = AuthDestination.INITIAL_SETUP_DISPLAY
                },
            )
        }

        AuthDestination.INITIAL_SETUP_DISPLAY -> {
            SetupDisplayPreferenceScreen(
                modifier = modifier,
                initialLargeText = setupPreferences.isLargeTextEnabled,
                initialVoiceGuidance = setupPreferences.isVoiceGuidanceEnabled,
                onBackClick = {
                    currentDestination = AuthDestination.INITIAL_SETUP_TARGET
                },
                onCompleteClick = { isLargeText, isVoiceGuidance ->
                    setupPreferences = setupPreferences.copy(
                        isLargeTextEnabled = isLargeText,
                        isVoiceGuidanceEnabled = isVoiceGuidance
                    )
                    isInitialSetupCompleted = true
                    currentDestination = AuthDestination.HOME
                },
            )
        }

        AuthDestination.HOME -> {
            HomeScreen(
                modifier = modifier,
            )
        }
    }
}