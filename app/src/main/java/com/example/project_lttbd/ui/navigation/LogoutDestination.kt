package com.example.project_lttbd.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_lttbd.ui.screens.account.LogoutConfirmationScreen
import com.example.project_lttbd.ui.viewmodel.AuthViewModel

@Composable
fun LogoutDestination(
    onBack: () -> Unit,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current

    val authViewModel: AuthViewModel = viewModel(
        key = "mmra_logout"
    )

    val loading = authViewModel.isLoading

    BackHandler(enabled = loading) {
        // Chờ thao tác kết thúc trước khi rời màn hình.
    }

    LogoutConfirmationScreen(
        onBackClick = {
            if (!authViewModel.isLoading) {
                onBack()
            }
        },
        onCancelClick = {
            if (!authViewModel.isLoading) {
                onBack()
            }
        },
        onConfirmLogout = {
            authViewModel.signOut(context, onSignedOut)
        }
    )

    if (loading) {
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = {
                Text("Đang đăng xuất")
            },
            text = {
                Text(
                    "Đang gỡ liên kết thiết bị. " +
                            "Vui lòng giữ kết nối Internet."
                )
            },
            confirmButton = {}
        )
    }

    authViewModel.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {
                authViewModel.clearError()
            },
            title = {
                Text("Chưa đăng xuất được")
            },
            text = {
                Text(message)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        authViewModel.clearError()
                    }
                ) {
                    Text("Đóng")
                }
            }
        )
    }
}