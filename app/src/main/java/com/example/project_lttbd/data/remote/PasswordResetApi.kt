package com.example.project_lttbd.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// Dữ liệu yêu cầu gửi OTP.
data class PasswordResetRequestBody(
    val email: String
)

// Kết quả tiếp nhận yêu cầu gửi OTP.
data class PasswordResetRequestResponse(
    val message: String,

    @SerializedName("expires_in_seconds")
    val expiresInSeconds: Int,

    @SerializedName("resend_after_seconds")
    val resendAfterSeconds: Int
)

// Dữ liệu xác minh mã OTP.
data class PasswordResetVerifyBody(
    val email: String,

    @SerializedName("otp_code")
    val otpCode: String
)

// Token cho phép đặt mật khẩu mới sau khi OTP hợp lệ.
data class PasswordResetVerifyResponse(
    @SerializedName("reset_token")
    val resetToken: String,

    @SerializedName("expires_in_seconds")
    val expiresInSeconds: Int
)

// Dữ liệu đặt mật khẩu mới.
data class PasswordResetCompleteBody(
    @SerializedName("reset_token")
    val resetToken: String,

    @SerializedName("new_password")
    val newPassword: String
)

// Kết quả đặt mật khẩu mới.
data class PasswordResetCompleteResponse(
    val message: String
)

interface PasswordResetApi {

    @POST("api/v1/auth/password-reset/request")
    suspend fun requestOtp(
        @Body body: PasswordResetRequestBody
    ): Response<PasswordResetRequestResponse>

    @POST("api/v1/auth/password-reset/verify")
    suspend fun verifyOtp(
        @Body body: PasswordResetVerifyBody
    ): Response<PasswordResetVerifyResponse>

    @POST("api/v1/auth/password-reset/complete")
    suspend fun completeReset(
        @Body body: PasswordResetCompleteBody
    ): Response<PasswordResetCompleteResponse>
}