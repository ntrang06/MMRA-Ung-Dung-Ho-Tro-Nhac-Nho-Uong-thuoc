package com.example.project_lttbd.ui.screens.account

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_lttbd.R
import com.example.project_lttbd.data.remote.AuthRepository
import com.example.project_lttbd.ui.viewmodel.AccountPasswordViewModel

@Composable
fun ChangePasswordScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onCancelClick: () -> Unit = {}
) {
    val passwordViewModel: AccountPasswordViewModel = viewModel()
    val context = LocalContext.current

    val hasPassword = AuthRepository.hasPasswordProvider
    val signedIn = AuthRepository.currentUser != null
    val loading = passwordViewModel.isLoading

    val primary = Color(0xFF0F5B51)
    val light = Color(0xFFE0F0EC)
    val dark = Color(0xFF0A3C35)
    val secondary = Color(0xFF5A6E6A)
    val border = Color(0xFFE2E8E6)

    // Mật khẩu chỉ được giữ tạm trong trạng thái của màn hình.
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var currentVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    var validationError by remember {
        mutableStateOf<String?>(null)
    }

    BackHandler(enabled = loading) {
        // Đợi Firebase trả kết quả lưu.
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFFAFDFB)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    onClick = onBackClick,
                    enabled = !loading,
                    shape = CircleShape,
                    color = light,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Quay lại",
                            tint = primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        text = if (hasPassword) {
                            "Đổi mật khẩu"
                        } else {
                            "Đặt mật khẩu"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = dark
                    )

                    Text(
                        text = "Bảo vệ tài khoản của bạn",
                        fontSize = 13.sp,
                        color = secondary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = when {
                    !signedIn ->
                        "Vui lòng đăng nhập trước khi đặt mật khẩu."

                    hasPassword ->
                        "Nhập mật khẩu hiện tại và mật khẩu mới của bạn."

                    else ->
                        "Bạn đang đăng nhập bằng Google. Đặt mật khẩu " +
                                "để có thể đăng nhập thêm bằng email và mật khẩu."
                },
                fontSize = 14.sp,
                color = secondary,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Chỉ hiện ô mật khẩu cũ nếu tài khoản đã có mật khẩu.
                    if (hasPassword) {
                        AccountPasswordInput(
                            label = "Mật khẩu hiện tại",
                            value = currentPassword,
                            onValueChange = {
                                currentPassword = it
                                validationError = null
                                passwordViewModel.clearError()
                            },
                            visible = currentVisible,
                            onToggleVisibility = {
                                currentVisible = !currentVisible
                            },
                            enabled = !loading
                        )

                        Spacer(Modifier.height(16.dp))
                    }

                    AccountPasswordInput(
                        label = "Mật khẩu mới",
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            validationError = null
                            passwordViewModel.clearError()
                        },
                        visible = newVisible,
                        onToggleVisibility = {
                            newVisible = !newVisible
                        },
                        enabled = !loading
                    )

                    Spacer(Modifier.height(16.dp))

                    AccountPasswordInput(
                        label = "Nhập lại mật khẩu mới",
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            validationError = null
                            passwordViewModel.clearError()
                        },
                        visible = confirmVisible,
                        onToggleVisibility = {
                            confirmVisible = !confirmVisible
                        },
                        enabled = !loading
                    )

                    val message =
                        validationError ?: passwordViewModel.errorMessage

                    message?.let {
                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = it,
                            fontSize = 12.sp,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

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
                        painter = painterResource(R.drawable.ic_lock),
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(10.dp))

                    Text(
                        text = "Mật khẩu cần ít nhất 8 ký tự, gồm chữ và số.",
                        fontSize = 12.sp,
                        color = secondary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                enabled = signedIn && !loading,
                onClick = {
                    validationError = when {
                        hasPassword && currentPassword.isBlank() ->
                            "Vui lòng nhập mật khẩu hiện tại."

                        newPassword.length < 8 ||
                                !newPassword.any { it.isLetter() } ||
                                !newPassword.any { it.isDigit() } ->
                            "Mật khẩu mới cần ít nhất 8 ký tự, gồm chữ và số."

                        newPassword != confirmPassword ->
                            "Mật khẩu xác nhận không khớp."

                        else -> null
                    }

                    if (validationError == null) {
                        passwordViewModel.savePassword(
                            currentPassword,
                            newPassword
                        ) { success ->
                            if (success) {
                                currentPassword = ""
                                newPassword = ""
                                confirmPassword = ""

                                Toast.makeText(
                                    context,
                                    if (hasPassword) {
                                        "Đã đổi mật khẩu"
                                    } else {
                                        "Đã thêm đăng nhập bằng email và mật khẩu"
                                    },
                                    Toast.LENGTH_LONG
                                ).show()

                                onSaveSuccess()
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Lưu mật khẩu",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (hasPassword) {
                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Quên mật khẩu hiện tại?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = primary,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable(
                            enabled = !loading,
                            onClick = onForgotPasswordClick
                        )
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Hủy",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = secondary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable(
                        enabled = !loading,
                        onClick = onCancelClick
                    )
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AccountPasswordInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    enabled: Boolean
) {
    val primary = Color(0xFF0F5B51)

    Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF0A3C35)
    )

    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        placeholder = {
            Text(
                text = "Nhập ${label.lowercase()}",
                fontSize = 14.sp
            )
        },
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password
        ),
        trailingIcon = {
            IconButton(
                onClick = onToggleVisibility,
                enabled = enabled
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_eye),
                    contentDescription = if (visible) {
                        "Ẩn $label"
                    } else {
                        "Hiện $label"
                    },
                    tint = primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(0xFFE2E8E6),
            focusedBorderColor = primary
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
private fun AccountPasswordInputPreview() {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        AccountPasswordInput(
            label = "Mật khẩu mới",
            value = "",
            onValueChange = {},
            visible = false,
            onToggleVisibility = {},
            enabled = true
        )
    }
}