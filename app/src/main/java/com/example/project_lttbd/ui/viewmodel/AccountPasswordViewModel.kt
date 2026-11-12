package com.example.project_lttbd.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_lttbd.data.remote.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class AccountPasswordViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun clearError() {
        errorMessage = null
    }

    fun savePassword(
        currentPassword: String,
        newPassword: String,
        onComplete: (Boolean) -> Unit
    ) {
        if (isLoading) return

        isLoading = true
        errorMessage = null

        viewModelScope.launch {
            var success = false

            try {
                AuthRepository.setAccountPassword(
                    currentPassword,
                    newPassword
                )

                success = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: FirebaseNetworkException) {
                errorMessage =
                    "Không có kết nối mạng. Vui lòng thử lại."
            } catch (_: FirebaseTooManyRequestsException) {
                errorMessage =
                    "Bạn đã thử quá nhiều lần. Vui lòng đợi rồi thử lại."
            } catch (error: FirebaseAuthException) {
                errorMessage = when (error.errorCode) {
                    "ERROR_REQUIRES_RECENT_LOGIN" ->
                        "Phiên đăng nhập đã cũ. Hãy đăng xuất, " +
                                "đăng nhập lại rồi đặt mật khẩu."

                    "ERROR_WRONG_PASSWORD",
                    "ERROR_INVALID_CREDENTIAL",
                    "ERROR_INVALID_LOGIN_CREDENTIALS" ->
                        "Không xác thực được mật khẩu hiện tại. " +
                                "Vui lòng kiểm tra lại."

                    "ERROR_WEAK_PASSWORD" ->
                        "Mật khẩu mới chưa đáp ứng yêu cầu của Firebase."

                    "ERROR_EMAIL_ALREADY_IN_USE",
                    "ERROR_CREDENTIAL_ALREADY_IN_USE" ->
                        "Thông tin này đã liên kết với tài khoản khác. " +
                                "Chưa thể lưu mật khẩu."

                    "ERROR_PROVIDER_ALREADY_LINKED" ->
                        "Tài khoản đã có mật khẩu. " +
                                "Vui lòng mở lại màn hình để đổi mật khẩu."

                    else ->
                        "Chưa lưu được mật khẩu (${error.errorCode})."
                }
            } catch (error: IllegalArgumentException) {
                errorMessage = error.message
            } catch (error: IllegalStateException) {
                errorMessage = error.message
            } catch (_: Exception) {
                errorMessage =
                    "Chưa lưu được mật khẩu. Vui lòng thử lại."
            } finally {
                isLoading = false
            }

            onComplete(success)
        }
    }
}