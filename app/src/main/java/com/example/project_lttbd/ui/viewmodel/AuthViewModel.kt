package com.example.project_lttbd.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_lttbd.data.device.DeviceManager
import com.example.project_lttbd.data.model.UserProfileManager
import com.example.project_lttbd.data.remote.AuthRepository
import com.example.project_lttbd.data.remote.UserProfileRepository
import kotlinx.coroutines.launch

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

    fun signIn(email: String, pass: String, context: Context, onComplete: (Boolean) -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            errorMessage = "Vui lòng nhập đầy đủ email và mật khẩu."
            onComplete(false)
            return
        }

        isLoading = true
        errorMessage = null

        viewModelScope.launch {
            try {
                val result = AuthRepository.signIn(email, pass)
                val uid = result.user?.uid
                if (uid != null) {
                    // Load profile from Firestore
                    val profile = UserProfileRepository.getProfile(uid)
                    if (profile != null) {
                        UserProfileManager.name = profile.displayName.ifBlank { "Người dùng" }
                        UserProfileManager.email = profile.email.ifBlank { email }
                        val setupDone = !profile.usageMode.isNullOrBlank()
                        UserProfileManager.setSetupCompleted(context, setupDone)
                    } else {
                        // If profile doesn't exist yet, create one
                        UserProfileRepository.createProfile(uid, email.substringBefore('@'), email)
                        UserProfileManager.setSetupCompleted(context, false)
                    }
                    UserProfileManager.setLoggedIn(context, true)

                    // Sync device for FCM
                    DeviceManager.syncCurrentDevice(context, uid)

                    isSuccess = true
                    onComplete(true)
                } else {
                    errorMessage = "Không thể xác định tài khoản đăng nhập."
                    onComplete(false)
                }
            } catch (e: Exception) {
                errorMessage = translateFirebaseError(e.message)
                onComplete(false)
            } finally {
                isLoading = false
            }
        }
    }

    fun signUp(name: String, email: String, pass: String, context: Context, onComplete: (Boolean) -> Unit) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            errorMessage = "Vui lòng nhập đầy đủ thông tin đăng ký."
            onComplete(false)
            return
        }

        isLoading = true
        errorMessage = null

        viewModelScope.launch {
            try {
                val result = AuthRepository.signUp(email, pass)
                val uid = result.user?.uid
                if (uid != null) {
                    try {
                        UserProfileRepository.createProfile(uid, name, email)
                    } catch (firestoreEx: Exception) {
                        // Auth succeeded but Firestore failed - keep auth session and allow retry or handle
                        errorMessage = "Tạo tài khoản thành công nhưng lưu hồ sơ thất bại: ${firestoreEx.localizedMessage}. Vui lòng thử lại."
                        onComplete(false)
                        return@launch
                    }

                    UserProfileManager.name = name
                    UserProfileManager.email = email
                    UserProfileManager.setLoggedIn(context, true)
                    UserProfileManager.setSetupCompleted(context, false) // goes to initial setup

                    DeviceManager.syncCurrentDevice(context, uid)

                    isSuccess = true
                    onComplete(true)
                } else {
                    errorMessage = "Không thể tạo tài khoản."
                    onComplete(false)
                }
            } catch (e: Exception) {
                errorMessage = translateFirebaseError(e.message)
                onComplete(false)
            } finally {
                isLoading = false
            }
        }
    }

    fun sendPasswordReset(email: String, onComplete: (Boolean) -> Unit) {
        if (email.isBlank()) {
            errorMessage = "Vui lòng nhập email."
            onComplete(false)
            return
        }

        isLoading = true
        errorMessage = null

        viewModelScope.launch {
            try {
                AuthRepository.sendPasswordResetEmail(email)
                isSuccess = true
                onComplete(true)
            } catch (e: Exception) {
                errorMessage = translateFirebaseError(e.message)
                onComplete(false)
            } finally {
                isLoading = false
            }
        }
    }

    fun signOut(context: Context, onComplete: () -> Unit) {
        isLoading = true
        viewModelScope.launch {
            val uid = AuthRepository.currentUid
            if (uid != null) {
                DeviceManager.removeDevice(context, uid)
            }
            AuthRepository.signOut()
            UserProfileManager.setLoggedIn(context, false)
            isLoading = false
            onComplete()
        }
    }

    private fun translateFirebaseError(msg: String?): String {
        if (msg == null) return "Đã xảy ra lỗi không xác định."
        return when {
            msg.contains("no user record", true) || msg.contains("user-not-found", true) -> "Không tìm thấy tài khoản với email này."
            msg.contains("password", true) && (msg.contains("invalid", true) || msg.contains("wrong", true)) -> "Mật khẩu không chính xác."
            msg.contains("email-already-in-use", true) -> "Email này đã được sử dụng bởi tài khoản khác."
            msg.contains("invalid-email", true) -> "Định dạng email không hợp lệ."
            msg.contains("weak-password", true) -> "Mật khẩu quá yếu (cần ít nhất 6 ký tự)."
            msg.contains("network-request-failed", true) -> "Lỗi kết nối mạng. Vui lòng kiểm tra lại internet."
            else -> "Lỗi: $msg"
        }
    }
}
