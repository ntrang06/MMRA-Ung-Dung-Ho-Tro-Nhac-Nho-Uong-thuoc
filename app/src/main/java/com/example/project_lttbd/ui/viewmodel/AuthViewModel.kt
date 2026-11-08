package com.example.project_lttbd.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_lttbd.data.device.DeviceManager
import com.example.project_lttbd.data.model.UserProfileManager
import com.example.project_lttbd.data.remote.AuthRepository
import com.example.project_lttbd.data.remote.UserProfileRepository
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isSuccess by mutableStateOf(false)
        private set

    fun clearError() {
        errorMessage = null
    }

    fun signIn(
        email: String,
        pass: String,
        context: Context,
        onComplete: (Boolean) -> Unit
    ) {
        if (isLoading) return

        if (email.isBlank() || pass.isBlank()) {
            errorMessage = "Vui lòng nhập đầy đủ email và mật khẩu."
            onComplete(false)
            return
        }

        isLoading = true
        isSuccess = false
        errorMessage = null

        viewModelScope.launch {
            try {
                val result = AuthRepository.signIn(email, pass)
                val uid = result.user?.uid

                if (uid != null) {
                    val profile = UserProfileRepository.getProfile(uid)

                    if (profile != null) {
                        UserProfileManager.name =
                            profile.displayName.ifBlank { "Người dùng" }

                        UserProfileManager.email =
                            profile.email.ifBlank { email }

                        UserProfileManager.setSetupCompleted(
                            context,
                            !profile.usageMode.isNullOrBlank()
                        )
                    } else {
                        UserProfileRepository.createProfile(
                            uid,
                            email.substringBefore('@'),
                            email
                        )

                        UserProfileManager.setSetupCompleted(
                            context,
                            false
                        )
                    }

                    UserProfileManager.setLoggedIn(context, true)

                    DeviceManager.syncCurrentDevice(context, uid)

                    isSuccess = true
                    onComplete(true)
                } else {
                    errorMessage =
                        "Không thể xác định tài khoản đăng nhập."
                    onComplete(false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                errorMessage = translateFirebaseError(error)
                onComplete(false)
            } finally {
                isLoading = false
            }
        }
    }

    fun signUp(
        name: String,
        email: String,
        pass: String,
        context: Context,
        onComplete: (Boolean) -> Unit
    ) {
        if (isLoading) return

        if (
            name.isBlank() ||
            email.isBlank() ||
            pass.isBlank()
        ) {
            errorMessage =
                "Vui lòng nhập đầy đủ thông tin đăng ký."
            onComplete(false)
            return
        }

        isLoading = true
        isSuccess = false
        errorMessage = null

        viewModelScope.launch {
            try {
                val result = AuthRepository.signUp(email, pass)
                val uid = result.user?.uid

                if (uid != null) {
                    try {
                        UserProfileRepository.createProfile(
                            uid,
                            name,
                            email
                        )
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        errorMessage =
                            "Đã tạo tài khoản nhưng chưa lưu được hồ sơ. " +
                                    "Vui lòng kiểm tra kết nối và thử lại."

                        onComplete(false)
                        return@launch
                    }

                    UserProfileManager.name = name
                    UserProfileManager.email = email

                    UserProfileManager.setLoggedIn(context, true)
                    UserProfileManager.setSetupCompleted(
                        context,
                        false
                    )

                    DeviceManager.syncCurrentDevice(context, uid)

                    isSuccess = true
                    onComplete(true)
                } else {
                    errorMessage = "Không thể tạo tài khoản."
                    onComplete(false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                errorMessage = translateFirebaseError(error)
                onComplete(false)
            } finally {
                isLoading = false
            }
        }
    }

    fun sendPasswordReset(
        email: String,
        onComplete: (Boolean) -> Unit
    ) {
        if (isLoading) return

        if (email.isBlank()) {
            errorMessage = "Vui lòng nhập email."
            onComplete(false)
            return
        }

        isLoading = true
        isSuccess = false
        errorMessage = null

        viewModelScope.launch {
            try {
                AuthRepository.sendPasswordResetEmail(email)

                isSuccess = true
                onComplete(true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                errorMessage = translateFirebaseError(error)
                onComplete(false)
            } finally {
                isLoading = false
            }
        }
    }

    fun signOut(
        context: Context,
        onComplete: () -> Unit
    ) {
        if (isLoading) return

        isLoading = true
        isSuccess = false
        errorMessage = null

        viewModelScope.launch {
            try {
                val uid = AuthRepository.currentUid

                if (uid != null) {
                    DeviceManager.removeDevice(context, uid)
                }

                AuthRepository.signOut()
                UserProfileManager.setLoggedIn(context, false)
            } finally {
                isLoading = false
            }

            onComplete()
        }
    }

    @Suppress("DiscouragedApi")
    fun signInWithGoogle(
        context: Context,
        onComplete: (Boolean) -> Unit
    ) {
        if (isLoading) return

        if (AuthRepository.currentUser != null) {
            errorMessage =
                "Hãy đăng xuất tài khoản hiện tại trước khi đăng nhập Google."

            onComplete(false)
            return
        }

        isLoading = true
        isSuccess = false
        errorMessage = null

        viewModelScope.launch {
            var firebaseAuthenticated = false
            var completed = false

            try {
                // Lấy Web Client ID từ google-services.json.
                val clientIdRes = context.resources.getIdentifier(
                    "default_web_client_id",
                    "string",
                    context.packageName
                )

                check(clientIdRes != 0) {
                    "Chưa có cấu hình đăng nhập Google."
                }

                val googleOption = GetSignInWithGoogleOption.Builder(
                    context.getString(clientIdRes)
                ).build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleOption)
                    .build()

                // Mở giao diện chọn tài khoản Google.
                val credential = CredentialManager.create(context)
                    .getCredential(context, request)
                    .credential

                check(
                    credential is CustomCredential &&
                            credential.type ==
                            GoogleIdTokenCredential
                                .TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    "Không nhận được thông tin đăng nhập Google hợp lệ."
                }

                val idToken = GoogleIdTokenCredential
                    .createFrom(credential.data)
                    .idToken

                // Firebase xác thực token và tạo phiên đăng nhập.
                val user = withContext(NonCancellable) {
                    AuthRepository.signInWithGoogle(idToken)
                        .user
                        .also {
                            firebaseAuthenticated = it != null
                        }
                } ?: error(
                    "Chưa xác nhận được tài khoản Google."
                )

                val userEmail = user.email.orEmpty()

                val userName = user.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?: userEmail.substringBefore('@')
                        .ifBlank { "Người dùng" }

                // Ưu tiên hồ sơ đã lưu; chỉ tạo khi chưa có.
                val profile = UserProfileRepository
                    .getProfile(user.uid)
                    ?: UserProfileRepository.createProfile(
                        user.uid,
                        userName,
                        userEmail
                    )

                UserProfileManager.name =
                    profile.displayName.ifBlank { userName }

                UserProfileManager.email =
                    profile.email.ifBlank { userEmail }

                UserProfileManager.phone =
                    user.phoneNumber.orEmpty()

                UserProfileManager.saveProfileInfo(context)

                UserProfileManager.setLoggedIn(context, true)

                UserProfileManager.setSetupCompleted(
                    context,
                    !profile.usageMode.isNullOrBlank()
                )

                DeviceManager.syncCurrentDevice(
                    context.applicationContext,
                    user.uid
                )

                isSuccess = true
                completed = true
            } catch (_: GetCredentialCancellationException) {
                // Hủy chọn tài khoản: ở lại màn đăng nhập.
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: NoCredentialException) {
                errorMessage =
                    "Chưa lấy được tài khoản Google. " +
                            "Vui lòng kiểm tra tài khoản trên điện thoại " +
                            "và thử lại."
            } catch (error: Exception) {
                errorMessage = translateFirebaseError(error)
            } finally {
                // Không giữ phiên đăng nhập chưa chuẩn bị được hồ sơ.
                if (firebaseAuthenticated && !completed) {
                    AuthRepository.signOut()

                    UserProfileManager.setLoggedIn(
                        context,
                        false
                    )

                    UserProfileManager.setSetupCompleted(
                        context,
                        false
                    )
                }

                isLoading = false
            }

            onComplete(completed)
        }
    }

    private fun translateFirebaseError(
        error: Throwable
    ): String {
        return when (error) {
            is FirebaseNetworkException ->
                "Không kết nối được máy chủ. " +
                        "Vui lòng kiểm tra Internet và thử lại."

            is FirebaseTooManyRequestsException ->
                "Bạn đã thử quá nhiều lần. " +
                        "Vui lòng đợi một lúc rồi thử lại."

            is FirebaseAuthException -> {
                when (error.errorCode) {
                    "ERROR_INVALID_EMAIL" ->
                        "Vui lòng nhập địa chỉ email hợp lệ."

                    "ERROR_WRONG_PASSWORD",
                    "ERROR_INVALID_CREDENTIAL",
                    "ERROR_INVALID_LOGIN_CREDENTIALS",
                    "ERROR_USER_NOT_FOUND" ->
                        "Email hoặc mật khẩu không đúng. " +
                                "Vui lòng kiểm tra lại."

                    "ERROR_EMAIL_ALREADY_IN_USE" ->
                        "Email này đã được đăng ký. " +
                                "Hãy đăng nhập hoặc chọn Quên mật khẩu."

                    "ERROR_WEAK_PASSWORD" ->
                        "Mật khẩu chưa đáp ứng yêu cầu. " +
                                "Hãy dùng ít nhất 8 ký tự, gồm chữ và số."

                    "ERROR_USER_DISABLED" ->
                        "Tài khoản đã bị vô hiệu hóa."

                    "ERROR_REQUIRES_RECENT_LOGIN",
                    "ERROR_USER_TOKEN_EXPIRED",
                    "ERROR_INVALID_USER_TOKEN" ->
                        "Phiên đăng nhập đã hết hạn. " +
                                "Vui lòng đăng nhập lại."

                    "ERROR_OPERATION_NOT_ALLOWED" ->
                        "Cách đăng nhập này chưa được bật. " +
                                "Vui lòng chọn cách khác."

                    else ->
                        "Chưa thực hiện được yêu cầu. " +
                                "Vui lòng thử lại."
                }
            }

            else ->
                "Chưa thực hiện được yêu cầu. " +
                        "Vui lòng kiểm tra kết nối và thử lại."
        }
    }
}