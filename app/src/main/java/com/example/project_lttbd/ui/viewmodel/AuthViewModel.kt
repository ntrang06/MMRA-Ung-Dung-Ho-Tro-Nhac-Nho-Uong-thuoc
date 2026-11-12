package com.example.project_lttbd.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_lttbd.data.device.DeviceManager
import com.example.project_lttbd.data.model.UserProfileManager
import com.example.project_lttbd.data.remote.AuthRepository
import com.example.project_lttbd.data.remote.UserProfileRepository
import com.example.project_lttbd.notifications.NotificationHelper
import com.example.project_lttbd.notifications.alarm.MedicationAlarmScheduler
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class AuthViewModel : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isSuccess by mutableStateOf(false)
        private set

    private data class PendingRegistration(
        val uid: String,
        val email: String
    )

    private var pendingRegistration: PendingRegistration? = null

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

        val address = email.trim()

        if (address.isBlank() || pass.isBlank()) {
            errorMessage = "Vui lòng nhập đầy đủ email và mật khẩu."
            onComplete(false)
            return
        }

        isLoading = true
        isSuccess = false
        errorMessage = null

        viewModelScope.launch {
            var authenticatedUid: String? = null
            var completed = false

            try {
                val user = withContext(NonCancellable) {
                    AuthRepository.signIn(address, pass)
                        .user
                        .also {
                            authenticatedUid = it?.uid
                        }
                } ?: error(
                    "Không thể xác định tài khoản đăng nhập."
                )

                val profile = UserProfileRepository
                    .getProfile(user.uid)
                    ?: UserProfileRepository.createProfile(
                        user.uid,
                        user.displayName
                            ?.takeIf { it.isNotBlank() }
                            ?: address.substringBefore('@'),
                        address
                    )

                UserProfileManager.applyProfile(
                    context,
                    user.uid,
                    profile
                )

                UserProfileManager.setLoggedIn(
                    context,
                    true
                )

                DeviceManager.syncCurrentDevice(
                    context.applicationContext,
                    user.uid
                )

                isSuccess = true
                completed = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("AuthViewModel", "Lỗi đăng nhập Google", error)
                errorMessage = translateFirebaseError(error)
            } finally {
                // Chỉ dọn phiên do chính lần đăng nhập này tạo ra.
                if (
                    authenticatedUid != null &&
                    !completed &&
                    AuthRepository.currentUid == authenticatedUid
                ) {
                    AuthRepository.signOut()

                    UserProfileManager.setLoggedIn(
                        context,
                        false
                    )
                }

                isLoading = false
            }

            onComplete(completed)
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

        val address = email.trim()
        val displayName = name.trim()

        if (
            displayName.isBlank() ||
            address.isBlank() ||
            pass.isBlank()
        ) {
            errorMessage = "Vui lòng nhập đầy đủ thông tin đăng ký."
            onComplete(false)
            return
        }

        val pending = pendingRegistration
        val currentUid = AuthRepository.currentUid

        if (pending != null) {
            if (!address.equals(pending.email, ignoreCase = true)) {
                errorMessage =
                    "Tài khoản đã được tạo bằng email ${pending.email}. " +
                            "Vui lòng giữ nguyên email này để thử lưu lại hồ sơ."

                onComplete(false)
                return
            }

            if (
                currentUid != null &&
                currentUid != pending.uid
            ) {
                errorMessage =
                    "Tài khoản đang đăng nhập đã thay đổi. " +
                            "Vui lòng đăng xuất trước khi thử lại."

                onComplete(false)
                return
            }
        } else if (currentUid != null) {
            errorMessage =
                "Bạn đang đăng nhập một tài khoản. " +
                        "Vui lòng đăng xuất trước khi tạo tài khoản mới."

            onComplete(false)
            return
        }

        isLoading = true
        isSuccess = false
        errorMessage = null

        viewModelScope.launch {
            try {
                val uid = if (pending != null) {
                    // Nếu mất phiên thì đăng nhập lại tài khoản đã tạo.
                    if (AuthRepository.currentUid == null) {
                        AuthRepository.signIn(
                            address,
                            pass
                        )
                    }

                    check(
                        AuthRepository.currentUid == pending.uid
                    )

                    pending.uid
                } else {
                    // Ghi nhớ UID ngay khi Firebase tạo tài khoản thành công.
                    withContext(NonCancellable) {
                        val user = AuthRepository
                            .signUp(address, pass)
                            .user
                            ?: error(
                                "Không thể xác định tài khoản vừa tạo."
                            )

                        pendingRegistration = PendingRegistration(
                            uid = user.uid,
                            email = address
                        )

                        user.uid
                    }
                }

                if (AuthRepository.currentUid != uid) {
                    errorMessage =
                        "Phiên đăng nhập đã thay đổi. Vui lòng thử lại."

                    onComplete(false)
                    return@launch
                }

                val profile = try {
                    // Repository chỉ tạo hồ sơ nếu chưa tồn tại.
                    UserProfileRepository.createProfile(
                        uid,
                        displayName,
                        address
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    errorMessage =
                        "Tài khoản đã được tạo nhưng chưa lưu được hồ sơ. " +
                                "Vui lòng kiểm tra kết nối, giữ nguyên email " +
                                "và bấm Đăng ký để thử lại."

                    onComplete(false)
                    return@launch
                }

                if (AuthRepository.currentUid != uid) {
                    errorMessage =
                        "Phiên đăng nhập đã thay đổi. Vui lòng thử lại."

                    onComplete(false)
                    return@launch
                }

                UserProfileManager.applyProfile(
                    context,
                    uid,
                    profile
                )

                UserProfileManager.setLoggedIn(
                    context,
                    true
                )

                DeviceManager.syncCurrentDevice(
                    context.applicationContext,
                    uid
                )

                pendingRegistration = null
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
                AuthRepository.sendPasswordResetEmail(
                    email
                )

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

        val appContext = context.applicationContext

        viewModelScope.launch {
            var completed = false

            try {
                val uid = AuthRepository.currentUid

                if (uid != null) {
                    val removed = withTimeoutOrNull(15_000L) {
                        suspendCancellableCoroutine<Boolean> { continuation ->
                            DeviceManager.removeDevice(
                                appContext,
                                uid
                            ) { success ->
                                if (continuation.isActive) {
                                    continuation.resume(success)
                                }
                            }
                        }
                    }

                    if (removed != true) {
                        errorMessage =
                            "Chưa gỡ được liên kết thiết bị. " +
                                    "Bạn vẫn đang đăng nhập. " +
                                    "Vui lòng kiểm tra Internet và thử lại."

                        return@launch
                    }
                }

                if (AuthRepository.currentUid != uid) {
                    errorMessage =
                        "Tài khoản đã thay đổi. " +
                                "Vui lòng quay lại và mở lại màn Đăng xuất."

                    return@launch
                }

                // Chỉ kết thúc phiên sau khi đã gỡ thiết bị.
                AuthRepository.signOut()

                UserProfileManager.setLoggedIn(
                    appContext,
                    false
                )

                UserProfileManager.setSetupCompleted(
                    appContext,
                    false
                )

                pendingRegistration = null

                if (!uid.isNullOrBlank()) {
                    MedicationAlarmScheduler.cancelAll(appContext, uid)
                }
                NotificationHelper.cancelAllNotifications(appContext)

                isSuccess = true
                completed = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                errorMessage =
                    "Chưa đăng xuất được. " +
                            "Vui lòng kiểm tra kết nối và thử lại."
            } finally {
                isLoading = false
            }

            if (completed) {
                onComplete()
            }
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
            var authenticatedUid: String? = null
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
                            authenticatedUid = it?.uid
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

                UserProfileManager.applyProfile(
                    context,
                    user.uid,
                    profile
                )

                UserProfileManager.setLoggedIn(
                    context,
                    true
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
                Log.e("AuthViewModel", "Lỗi đăng nhập Google", error)
                errorMessage = translateFirebaseError(error)
            } finally {
                // Chỉ dọn phiên do lần đăng nhập Google này tạo ra.
                if (
                    authenticatedUid != null &&
                    !completed &&
                    AuthRepository.currentUid == authenticatedUid
                ) {
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

            is GetCredentialException -> {
                Log.e("AuthViewModel", "GetCredentialException: ${error.message}", error)
                "Chưa đăng nhập được Google do mã SHA-1 của máy tính này chưa được đăng ký trong Firebase Console. " +
                        "Vui lòng đăng nhập bằng Email/SĐT hoặc thêm SHA-1 vào dự án."
            }

            else -> {
                Log.e("AuthViewModel", "Unhandled Auth Error: ${error.javaClass.simpleName} - ${error.message}", error)
                val detail = error.localizedMessage?.takeIf { it.isNotBlank() }
                if (detail != null && !detail.contains("Exception") && detail.length < 100) {
                    "Chưa thực hiện được yêu cầu: $detail"
                } else {
                    "Chưa thực hiện được yêu cầu. " +
                            "Vui lòng kiểm tra kết nối và thử lại."
                }
            }
        }
    }
}