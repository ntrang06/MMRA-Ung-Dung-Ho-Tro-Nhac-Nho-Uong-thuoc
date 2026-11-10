package com.example.project_lttbd.data.remote

import com.google.gson.JsonParser
import retrofit2.Response
import java.io.IOException

class PasswordResetException(
    message: String,
    val errorCode: String? = null
) : Exception(message)

class PasswordResetRepository(
    private val api: PasswordResetApi = PasswordResetApiClient.api
) {
    suspend fun requestOtp(email: String): PasswordResetRequestResponse {
        return execute(Action.REQUEST) {
            api.requestOtp(PasswordResetRequestBody(email.trim()))
        }
    }

    suspend fun verifyOtp(
        email: String,
        otpCode: String
    ): PasswordResetVerifyResponse {
        val result = execute(Action.VERIFY) {
            api.verifyOtp(
                PasswordResetVerifyBody(
                    email.trim(),
                    otpCode.trim()
                )
            )
        }

        val token = result.resetToken

        if (
            token.isNullOrBlank() ||
            token.length !in 32..256 ||
            token.all { it == '*' } ||
            result.expiresInSeconds <= 0
        ) {
            throw PasswordResetException(
                "Máy chủ trả về thông tin đặt lại mật khẩu không hợp lệ. " +
                        "Vui lòng gửi lại mã."
            )
        }

        return result
    }

    suspend fun completeReset(
        resetToken: String,
        newPassword: String
    ): PasswordResetCompleteResponse {
        return execute(Action.COMPLETE) {
            // Giữ nguyên mật khẩu, không tự xóa khoảng trắng.
            api.completeReset(
                PasswordResetCompleteBody(
                    resetToken,
                    newPassword
                )
            )
        }
    }

    private suspend fun <T> execute(
        action: Action,
        call: suspend () -> Response<T>
    ): T {
        try {
            val response = call()

            if (!response.isSuccessful) {
                throw apiError(response, action)
            }

            return response.body() ?: throw PasswordResetException(
                "Máy chủ chưa trả về kết quả. Vui lòng thử lại."
            )
        } catch (error: IOException) {
            val message = when (action) {
                Action.REQUEST ->
                    "Chưa nhận được phản hồi. Kiểm tra kết nối và hộp thư " +
                            "trước khi gửi lại mã."

                Action.VERIFY ->
                    "Không kết nối được máy chủ. " +
                            "Vui lòng kiểm tra kết nối và thử lại."

                Action.COMPLETE ->
                    "Chưa nhận được kết quả đặt lại mật khẩu. " +
                            "Hãy thử đăng nhập bằng mật khẩu mới " +
                            "trước khi gửi lại mã."
            }

            throw PasswordResetException(message, "network_error")
        }
        // Việc hủy coroutine vẫn được truyền tiếp bình thường.
    }

    private fun apiError(
        response: Response<*>,
        action: Action
    ): PasswordResetException {
        val code = readErrorCode(response)
        val status = response.code()

        if (status == 400 && action == Action.COMPLETE) {
            return PasswordResetException(
                "Phiên đặt lại mật khẩu không hợp lệ hoặc đã hết hạn. " +
                        "Vui lòng gửi lại mã.",
                "reset_session_invalid"
            )
        }

        if (status == 400 && action == Action.VERIFY) {
            return PasswordResetException(
                if (code == "otp_expired") {
                    "Mã OTP đã hết hạn. Vui lòng gửi lại mã."
                } else {
                    "Mã OTP không đúng hoặc không còn hiệu lực. " +
                            "Vui lòng kiểm tra lại."
                },
                code ?: "invalid_otp"
            )
        }

        val message = when (status) {
            422 -> when (action) {
                Action.REQUEST ->
                    "Vui lòng nhập địa chỉ email hợp lệ."

                Action.VERIFY ->
                    "Vui lòng kiểm tra email và nhập mã OTP gồm đúng 6 chữ số."

                Action.COMPLETE ->
                    "Mật khẩu chưa đáp ứng yêu cầu. Vui lòng kiểm tra lại."
            }

            429 ->
                "Bạn đã thử quá nhiều lần. Vui lòng đợi một lúc rồi thử lại."

            500, 502, 503, 504 ->
                "Máy chủ đang gặp sự cố. Vui lòng thử lại sau."

            else ->
                "Chưa thực hiện được yêu cầu. Vui lòng thử lại."
        }

        return PasswordResetException(message, code)
    }

    private fun readErrorCode(response: Response<*>): String? {
        // Chỉ đọc mã lỗi, không hiển thị nguyên lỗi kỹ thuật.
        return runCatching {
            val json = response.errorBody()?.string()
                ?: return@runCatching null

            val detail = JsonParser.parseString(json)
                .asJsonObject
                .get("detail")

            if (detail?.isJsonObject == true) {
                detail.asJsonObject.get("code")?.asString
            } else {
                null
            }
        }.getOrNull()
    }

    private enum class Action {
        REQUEST,
        VERIFY,
        COMPLETE
    }
}