package com.example.project_lttbd.ui.screens.account

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.R

@Composable
fun ChangePasswordScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onCancelClick: () -> Unit = {}
) {
    val primaryColor = Color(0xFF0F5B51)
    val lightBgColor = Color(0xFFE0F0EC)
    val darkTextColor = Color(0xFF0A3C35)
    val subTextColor = Color(0xFF5A6E6A)
    val borderColor = Color(0xFFE2E8E6)
    val infoBgColor = Color(0xFFEFF6F4)

    // Giá trị thật bắt đầu trống, không điền sẵn ký tự dấu chấm.
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Mỗi ô có trạng thái ẩn/hiện riêng.
    var currentVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
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
                    shape = CircleShape,
                    color = lightBgColor,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(
                                R.string.cd_back_button
                            ),
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        text = stringResource(
                            R.string.account_change_password
                        ),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor
                    )
                    Text(
                        text = stringResource(
                            R.string.account_change_password_sub
                        ),
                        fontSize = 13.sp,
                        color = subTextColor
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Nhập mật khẩu hiện tại để bảo vệ tài khoản của cô Lan.",
                fontSize = 14.sp,
                color = subTextColor,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    ChangePasswordInput(
                        label = "Mật khẩu hiện tại",
                        placeholder = "Nhập mật khẩu hiện tại",
                        value = currentPassword,
                        onValueChange = {
                            currentPassword = it
                            errorMessage = null
                        },
                        visible = currentVisible,
                        onToggleVisibility = {
                            currentVisible = !currentVisible
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    ChangePasswordInput(
                        label = stringResource(
                            R.string.label_new_password
                        ),
                        placeholder = "Nhập mật khẩu mới",
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            errorMessage = null
                        },
                        visible = newVisible,
                        onToggleVisibility = {
                            newVisible = !newVisible
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    ChangePasswordInput(
                        label = stringResource(
                            R.string.label_confirm_password
                        ),
                        placeholder = "Nhập lại mật khẩu mới",
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = null
                        },
                        visible = confirmVisible,
                        onToggleVisibility = {
                            confirmVisible = !confirmVisible
                        }
                    )

                    errorMessage?.let { message ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = message,
                            fontSize = 12.sp,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = infoBgColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(
                            R.string.register_password_note
                        ),
                        fontSize = 12.sp,
                        color = subTextColor,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    when {
                        currentPassword.isBlank() -> {
                            errorMessage =
                                "Vui lòng nhập mật khẩu hiện tại."
                        }

                        newPassword.length < 8 -> {
                            errorMessage =
                                "Mật khẩu mới phải có ít nhất 8 ký tự."
                        }

                        newPassword != confirmPassword -> {
                            errorMessage =
                                "Mật khẩu xác nhận không khớp."
                        }

                        else -> {
                            errorMessage = null
                            onSaveSuccess()
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Lưu mật khẩu",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Quên mật khẩu hiện tại?",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onForgotPasswordClick() }
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.avatar_action_cancel),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = subTextColor,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onCancelClick() }
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ChangePasswordInput(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisibility: () -> Unit
) {
    val primaryColor = Color(0xFF0F5B51)
    val darkTextColor = Color(0xFF0A3C35)
    val subTextColor = Color(0xFF5A6E6A)
    val borderColor = Color(0xFFE2E8E6)

    Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = darkTextColor
    )

    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = subTextColor,
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
                onClick = onToggleVisibility
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_eye),
                    contentDescription = if (visible) {
                        "Ẩn $label"
                    } else {
                        "Hiện $label"
                    },
                    tint = if (visible) primaryColor else subTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = borderColor,
            focusedBorderColor = primaryColor
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
fun ChangePasswordScreenPreview() {
    ChangePasswordScreen()
}