package com.example.project_lttbd.ui.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_lttbd.R
import com.example.project_lttbd.ui.viewmodel.PasswordResetStep
import com.example.project_lttbd.ui.viewmodel.PasswordResetUiState
import com.example.project_lttbd.ui.viewmodel.PasswordResetViewModel

private val ResetPrimary = Color(0xFF0F5B51)
private val ResetLight = Color(0xFFE0F0EC)
private val ResetDark = Color(0xFF0A3C35)
private val ResetSubText = Color(0xFF5A6E6A)
private val ResetBorder = Color(0xFFE2E8E6)

@Composable
fun ForgotPasswordScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onBackToLoginClick: () -> Unit = {},
) {
    val resetViewModel: PasswordResetViewModel? =
        if (LocalInspectionMode.current) null
        else viewModel<PasswordResetViewModel>()

    val state = resetViewModel?.uiState ?: PasswordResetUiState()
    val keyboard = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    val loading = state.isLoading

    val goBack: () -> Unit = {
        if (!loading) {
            keyboard?.hide()

            when (state.step) {
                PasswordResetStep.EMAIL -> onBackClick()

                PasswordResetStep.OTP,
                PasswordResetStep.PASSWORD ->
                    resetViewModel?.changeEmail()

                PasswordResetStep.SUCCESS ->
                    onBackToLoginClick()
            }
        }
    }

    val submit: () -> Unit = {
        if (!loading) {
            keyboard?.hide()

            when (state.step) {
                PasswordResetStep.EMAIL ->
                    resetViewModel?.sendOtp()

                PasswordResetStep.OTP ->
                    resetViewModel?.verifyOtp()

                PasswordResetStep.PASSWORD ->
                    resetViewModel?.completeReset()

                PasswordResetStep.SUCCESS ->
                    onBackToLoginClick()
            }
        }
    }

    BackHandler { goBack() }

    LaunchedEffect(state.step) {
        keyboard?.hide()
        scrollState.scrollTo(0)
    }

    val title = when (state.step) {
        PasswordResetStep.EMAIL -> "Chúng tôi sẽ giúp bạn"
        PasswordResetStep.OTP -> "Nhập mã xác minh"
        PasswordResetStep.PASSWORD -> "Tạo mật khẩu mới"
        PasswordResetStep.SUCCESS -> "Đặt lại mật khẩu thành công"
    }

    val description = when (state.step) {
        PasswordResetStep.EMAIL ->
            "Nhập email đã đăng ký để nhận mã OTP đặt lại mật khẩu."

        PasswordResetStep.OTP ->
            "Nhập mã OTP gồm 6 chữ số được gửi đến ${state.email}."

        PasswordResetStep.PASSWORD ->
            "Đặt mật khẩu mới cho tài khoản ${state.email}."

        PasswordResetStep.SUCCESS ->
            "Bạn có thể đăng nhập bằng mật khẩu mới ngay bây giờ."
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFFAFDFB)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = ResetLight,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(enabled = !loading) { goBack() }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Quay lại",
                            tint = ResetPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Quên mật khẩu",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ResetDark
                    )

                    Text(
                        text = "Khôi phục tài khoản qua email",
                        fontSize = 13.sp,
                        color = ResetSubText
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ResetLight,
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock),
                        contentDescription = null,
                        tint = ResetPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ResetDark
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = description,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = ResetSubText
            )

            Spacer(Modifier.height(20.dp))

            if (state.step != PasswordResetStep.SUCCESS) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, ResetBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        when (state.step) {
                            PasswordResetStep.EMAIL -> {
                                ResetInput(
                                    label = "Email đã đăng ký",
                                    value = state.email,
                                    onValueChange = {
                                        resetViewModel?.updateEmail(it)
                                    },
                                    placeholder = "Ví dụ: ten@gmail.com",
                                    enabled = !loading,
                                    isError = state.errorMessage != null,
                                    keyboardType = KeyboardType.Email,
                                    onDone = submit
                                )
                            }

                            PasswordResetStep.OTP -> {
                                ResetInput(
                                    label = "Mã OTP",
                                    value = state.otpCode,
                                    onValueChange = {
                                        resetViewModel?.updateOtp(it)
                                    },
                                    placeholder = "Nhập 6 chữ số",
                                    enabled = !loading,
                                    isError = state.errorMessage != null,
                                    keyboardType = KeyboardType.NumberPassword,
                                    onDone = submit
                                )

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = if (state.otpExpiresInSeconds > 0) {
                                        "Mã còn hiệu lực " +
                                                "${state.otpExpiresInSeconds / 60}:" +
                                                (state.otpExpiresInSeconds % 60)
                                                    .toString()
                                                    .padStart(2, '0')
                                    } else {
                                        "Mã đã hết hạn. Vui lòng gửi lại mã."
                                    },
                                    fontSize = 12.sp,
                                    color = ResetSubText
                                )

                                TextButton(
                                    enabled = !loading &&
                                            state.resendInSeconds == 0,
                                    onClick = {
                                        keyboard?.hide()
                                        resetViewModel?.sendOtp()
                                    }
                                ) {
                                    Text(
                                        text = if (state.resendInSeconds > 0) {
                                            "Gửi lại mã sau ${state.resendInSeconds}s"
                                        } else {
                                            "Gửi lại mã"
                                        }
                                    )
                                }

                                TextButton(
                                    enabled = !loading,
                                    onClick = {
                                        resetViewModel?.changeEmail()
                                    }
                                ) {
                                    Text(
                                        text = "Đổi email",
                                        color = ResetPrimary
                                    )
                                }
                            }

                            PasswordResetStep.PASSWORD -> {
                                ResetInput(
                                    label = "Mật khẩu mới",
                                    value = state.newPassword,
                                    onValueChange = {
                                        resetViewModel?.updateNewPassword(it)
                                    },
                                    placeholder = "Từ 8 ký tự, gồm chữ và số",
                                    enabled = !loading,
                                    isError = state.errorMessage != null,
                                    isPassword = true,
                                    onDone = submit
                                )

                                Spacer(Modifier.height(16.dp))

                                ResetInput(
                                    label = "Nhập lại mật khẩu mới",
                                    value = state.confirmPassword,
                                    onValueChange = {
                                        resetViewModel?.updateConfirmPassword(it)
                                    },
                                    placeholder = "Nhập lại mật khẩu",
                                    enabled = !loading,
                                    isError = state.errorMessage != null,
                                    isPassword = true,
                                    onDone = submit
                                )
                            }

                            PasswordResetStep.SUCCESS -> Unit
                        }

                        state.errorMessage?.let {
                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = it,
                                color = Color(0xFFB3261E),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = submit,
                enabled = !loading && when (state.step) {
                    PasswordResetStep.EMAIL ->
                        state.resendInSeconds == 0

                    PasswordResetStep.OTP ->
                        state.otpExpiresInSeconds > 0

                    else -> true
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ResetPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_send),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = if (loading) {
                        "Đang xử lý…"
                    } else {
                        when (state.step) {
                            PasswordResetStep.EMAIL ->
                                if (state.resendInSeconds > 0) {
                                    "Gửi mã sau ${state.resendInSeconds}s"
                                } else {
                                    "Gửi mã OTP"
                                }

                            PasswordResetStep.OTP ->
                                "Xác minh mã OTP"

                            PasswordResetStep.PASSWORD ->
                                "Cập nhật mật khẩu"

                            PasswordResetStep.SUCCESS ->
                                "Đăng nhập ngay"
                        }
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEFF6F4),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_info_circle),
                        contentDescription = null,
                        tint = ResetPrimary,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(10.dp))

                    Text(
                        text = state.infoMessage ?: when (state.step) {
                            PasswordResetStep.EMAIL ->
                                "Mã OTP được gửi qua email và có hiệu lực trong 5 phút."

                            PasswordResetStep.OTP ->
                                "Kiểm tra cả thư rác và dùng mã trong email mới nhất."

                            PasswordResetStep.PASSWORD ->
                                "Mật khẩu cần từ 8 đến 128 ký tự, gồm chữ và số."

                            PasswordResetStep.SUCCESS ->
                                "Vui lòng đăng nhập lại bằng mật khẩu mới."
                        },
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = ResetSubText
                    )
                }
            }

            if (state.step != PasswordResetStep.SUCCESS) {
                Spacer(Modifier.height(28.dp))

                Text(
                    text = "Quay lại đăng nhập",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ResetPrimary,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable(enabled = !loading) {
                            keyboard?.hide()
                            onBackToLoginClick()
                        }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ResetInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    isError: Boolean,
    onDone: () -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
) {
    var visible by remember { mutableStateOf(false) }

    Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = ResetDark
    )

    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        isError = isError,
        placeholder = {
            Text(
                text = placeholder,
                color = Color.Gray
            )
        },
        singleLine = true,
        visualTransformation = if (isPassword && !visible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        trailingIcon = if (isPassword) {
            {
                TextButton(
                    enabled = enabled,
                    onClick = { visible = !visible }
                ) {
                    Text(
                        text = if (visible) "Ẩn" else "Hiện",
                        color = ResetPrimary
                    )
                }
            }
        } else null,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) {
                KeyboardType.Password
            } else {
                keyboardType
            },
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { onDone() }
        ),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = ResetBorder,
            focusedBorderColor = ResetPrimary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
fun ForgotPasswordScreenPreview() {
    ForgotPasswordScreen()
}