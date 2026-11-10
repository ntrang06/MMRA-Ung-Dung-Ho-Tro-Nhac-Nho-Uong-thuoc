package com.example.project_lttbd.ui.navigation

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_lttbd.ui.screens.account.SwitchAccountScreen
import com.example.project_lttbd.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun SwitchAccountDestination(
    onBack: () -> Unit,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current

    val authViewModel: AuthViewModel = viewModel(
        key = "mmra_switch_account"
    )

    val busy = authViewModel.isLoading ||
            authViewModel.isSuccess

    BackHandler(enabled = busy) {
        // Chờ thao tác kết thúc trước khi rời màn hình.
    }

    // Nhận kết quả từ ViewModel khi màn hình đang hoạt động.
    LaunchedEffect(authViewModel.isSuccess) {
        if (authViewModel.isSuccess) {
            try {
                withTimeoutOrNull(3_000L) {
                    CredentialManager.create(context)
                        .clearCredentialState(
                            ClearCredentialStateRequest()
                        )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(
                    "MMRA_SwitchAccount",
                    "Chưa dọn được lựa chọn đăng nhập Google.",
                    error
                )
            }

            onSignedOut()
        }
    }

    SwitchAccountScreen(
        onBackClick = {
            if (
                !authViewModel.isLoading &&
                !authViewModel.isSuccess
            ) {
                onBack()
            }
        },
        onCancelClick = {
            if (
                !authViewModel.isLoading &&
                !authViewModel.isSuccess
            ) {
                onBack()
            }
        },
        onAddAccountClick = {
            if (
                !authViewModel.isLoading &&
                !authViewModel.isSuccess
            ) {
                authViewModel.signOut(context) {}
            }
        }
    )

    if (busy) {
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = {
                Text("Đang chuyển tài khoản")
            },
            text = {
                Text(
                    if (authViewModel.isSuccess) {
                        "Đang chuẩn bị màn đăng nhập."
                    } else {
                        "Đang gỡ liên kết thiết bị. " +
                                "Vui lòng giữ kết nối Internet."
                    }
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
                Text("Chưa chuyển được tài khoản")
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