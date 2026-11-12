package com.example.project_lttbd.ui.viewmodel

import android.os.SystemClock
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_lttbd.data.remote.PasswordResetException
import com.example.project_lttbd.data.remote.PasswordResetRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PasswordResetStep {
    EMAIL,
    OTP,
    PASSWORD,
    SUCCESS
}

data class PasswordResetUiState(
    val step: PasswordResetStep = PasswordResetStep.EMAIL,
    val email: String = "",
    val otpCode: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val resendInSeconds: Int = 0,
    val otpExpiresInSeconds: Int = 0
)

class PasswordResetViewModel(
    private val repository: PasswordResetRepository = PasswordResetRepository()
) : ViewModel() {

    var uiState by mutableStateOf(PasswordResetUiState())
        private set

    // Token chỉ nằm trong bộ nhớ, không lưu xuống máy.
    private var resetToken: String? = null
    private var tokenDeadline = 0L
    private var otpDeadline = 0L
    private var resendDeadline = 0L
    private var timerJob: Job? = null

    fun updateEmail(value: String) {
        if (uiState.isLoading || uiState.step != PasswordResetStep.EMAIL) return

        uiState = uiState.copy(
            email = value,
            errorMessage = null
        )
    }

    fun updateOtp(value: String) {
        if (uiState.isLoading || uiState.step != PasswordResetStep.OTP) return

        uiState = uiState.copy(
            otpCode = value.filter { it in '0'..'9' }.take(6),
            errorMessage = null
        )
    }

    fun updateNewPassword(value: String) {
        if (uiState.isLoading || uiState.step != PasswordResetStep.PASSWORD) return

        uiState = uiState.copy(
            newPassword = value,
            errorMessage = null
        )
    }

    fun updateConfirmPassword(value: String) {
        if (uiState.isLoading || uiState.step != PasswordResetStep.PASSWORD) return

        uiState = uiState.copy(
            confirmPassword = value,
            errorMessage = null
        )
    }

    fun sendOtp() {
        if (
            uiState.isLoading ||
            uiState.step !in setOf(
                PasswordResetStep.EMAIL,
                PasswordResetStep.OTP
            )
        ) return

        refreshTime()

        if (uiState.resendInSeconds > 0) {
            showError(
                "Vui lòng đợi ${uiState.resendInSeconds} giây trước khi gửi lại mã."
            )
            return
        }

        val email = uiState.email.trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Vui lòng nhập địa chỉ email hợp lệ.")
            return
        }

        perform {
            val startedAt = SystemClock.elapsedRealtime()
            val result = repository.requestOtp(email)

            resetToken = null
            tokenDeadline = 0L

            otpDeadline = startedAt +
                    result.expiresInSeconds.coerceAtLeast(0).toLong() * 1000

            resendDeadline = startedAt +
                    result.resendAfterSeconds.coerceAtLeast(0).toLong() * 1000

            uiState = uiState.copy(
                step = PasswordResetStep.OTP,
                email = email,
                otpCode = "",
                newPassword = "",
                confirmPassword = "",
                infoMessage =
                    "Nếu email thuộc tài khoản hợp lệ, mã OTP sẽ được gửi " +
                            "đến hộp thư. Hãy kiểm tra cả thư rác."
            )

            startTimer()
        }
    }

    fun verifyOtp() {
        if (uiState.isLoading || uiState.step != PasswordResetStep.OTP) return

        refreshTime()

        if (uiState.otpExpiresInSeconds == 0) {
            showError("Mã OTP đã hết hạn. Vui lòng gửi lại mã.")
            return
        }

        if (!Regex("[0-9]{6}").matches(uiState.otpCode)) {
            showError("Vui lòng nhập mã OTP gồm đúng 6 chữ số.")
            return
        }

        perform {
            val startedAt = SystemClock.elapsedRealtime()

            val result = repository.verifyOtp(
                uiState.email,
                uiState.otpCode
            )

            resetToken = result.resetToken
            tokenDeadline = startedAt +
                    result.expiresInSeconds.toLong() * 1000

            uiState = uiState.copy(
                step = PasswordResetStep.PASSWORD,
                otpCode = "",
                infoMessage = null
            )

            startTimer()
        }
    }

    fun completeReset() {
        if (
            uiState.isLoading ||
            uiState.step != PasswordResetStep.PASSWORD
        ) return

        refreshTime()

        val token = resetToken

        if (
            uiState.step != PasswordResetStep.PASSWORD ||
            token == null
        ) return

        val password = uiState.newPassword

        if (
            password.length !in 8..128 ||
            password.none { it.isLetter() } ||
            password.none { it.isDigit() }
        ) {
            showError("Mật khẩu cần từ 8 đến 128 ký tự, gồm chữ và số.")
            return
        }

        if (password != uiState.confirmPassword) {
            showError("Mật khẩu nhập lại chưa khớp.")
            return
        }

        perform {
            repository.completeReset(token, password)

            resetToken = null
            tokenDeadline = 0L
            otpDeadline = 0L
            resendDeadline = 0L
            timerJob?.cancel()

            uiState = uiState.copy(
                step = PasswordResetStep.SUCCESS,
                otpCode = "",
                newPassword = "",
                confirmPassword = "",
                resendInSeconds = 0,
                otpExpiresInSeconds = 0,
                infoMessage =
                    "Mật khẩu đã được cập nhật thành công. " +
                            "Vui lòng đăng nhập lại."
            )
        }
    }

    fun changeEmail() {
        if (uiState.isLoading) return

        resetToken = null
        tokenDeadline = 0L
        otpDeadline = 0L

        // Giữ thời gian chờ để tránh bấm Back rồi gửi liên tục.
        uiState = uiState.copy(
            step = PasswordResetStep.EMAIL,
            otpCode = "",
            newPassword = "",
            confirmPassword = "",
            errorMessage = null,
            infoMessage = null,
            otpExpiresInSeconds = 0
        )
    }

    private fun perform(action: suspend () -> Unit) {
        uiState = uiState.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (
                    error is PasswordResetException &&
                    error.errorCode == "reset_session_invalid"
                ) {
                    returnToOtp()
                }

                showError(
                    (error as? PasswordResetException)?.message
                        ?: "Chưa thực hiện được yêu cầu. Vui lòng thử lại."
                )
            } finally {
                uiState = uiState.copy(isLoading = false)
                refreshTime()
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        refreshTime()

        timerJob = viewModelScope.launch {
            while (
                isActive &&
                uiState.step != PasswordResetStep.SUCCESS
            ) {
                refreshTime()

                if (
                    uiState.resendInSeconds == 0 &&
                    uiState.otpExpiresInSeconds == 0 &&
                    resetToken == null
                ) break

                delay(1000)
            }
        }
    }

    private fun refreshTime() {
        val now = SystemClock.elapsedRealtime()

        uiState = uiState.copy(
            resendInSeconds = remainingSeconds(resendDeadline, now),
            otpExpiresInSeconds = remainingSeconds(otpDeadline, now)
        )

        if (
            !uiState.isLoading &&
            uiState.step == PasswordResetStep.PASSWORD &&
            now >= tokenDeadline
        ) {
            returnToOtp()

            showError(
                "Phiên đặt lại mật khẩu đã hết hạn. Vui lòng gửi lại mã."
            )
        }
    }

    private fun returnToOtp() {
        resetToken = null
        tokenDeadline = 0L
        otpDeadline = 0L

        uiState = uiState.copy(
            step = PasswordResetStep.OTP,
            otpExpiresInSeconds = 0,
            otpCode = "",
            newPassword = "",
            confirmPassword = "",
            infoMessage = null
        )
    }

    private fun remainingSeconds(deadline: Long, now: Long): Int {
        return ((deadline - now).coerceAtLeast(0L) + 999L)
            .div(1000L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    private fun showError(message: String) {
        uiState = uiState.copy(errorMessage = message)
    }
}