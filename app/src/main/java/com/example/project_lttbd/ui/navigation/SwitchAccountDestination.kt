package com.example.project_lttbd.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.project_lttbd.data.device.DeviceManager
import com.example.project_lttbd.data.model.UserProfileManager
import com.example.project_lttbd.data.remote.AuthRepository
import com.example.project_lttbd.ui.screens.account.SwitchAccountScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
fun SwitchAccountDestination(
    onBack: () -> Unit,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var switching by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    BackHandler(enabled = switching) {
        // Chờ kết thúc phiên cũ trước khi rời màn hình.
    }

    SwitchAccountScreen(
        avatarBitmap = UserProfileManager.currentAvatarBitmap,

        onBackClick = {
            if (!switching) onBack()
        },

        onCancelClick = {
            if (!switching) onBack()
        },

        onAddAccountClick = {
            if (!switching) {
                switching = true
                errorMessage = null

                scope.launch {
                    var completed = false

                    try {
                        val uid = AuthRepository.currentUid

                        if (uid != null) {
                            val removed =
                                suspendCancellableCoroutine<Boolean> { continuation ->
                                    DeviceManager.removeDevice(
                                        context.applicationContext,
                                        uid
                                    ) { success ->
                                        if (continuation.isActive) {
                                            continuation.resume(success)
                                        }
                                    }
                                }

                            check(removed) {
                                "Chưa gỡ được thiết bị."
                            }

                            check(AuthRepository.currentUid == uid) {
                                "Tài khoản đã thay đổi."
                            }
                        }

                        AuthRepository.signOut()

                        UserProfileManager.setLoggedIn(
                            context,
                            false
                        )

                        UserProfileManager.setSetupCompleted(
                            context,
                            false
                        )

                        UserProfileManager.name = "Người dùng"
                        UserProfileManager.email = ""
                        UserProfileManager.phone = ""
                        UserProfileManager.saveProfileInfo(context)

                        try {
                            CredentialManager.create(context)
                                .clearCredentialState(
                                    ClearCredentialStateRequest()
                                )
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            // Firebase đã đăng xuất.
                            // Vẫn cho người dùng mở màn đăng nhập.
                        }

                        completed = true
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        errorMessage =
                            "Chưa chuyển được tài khoản. " +
                                    "Vui lòng kiểm tra mạng và thử lại."
                    } finally {
                        switching = false
                    }

                    if (completed) {
                        onSignedOut()
                    }
                }
            }
        }
    )

    if (switching) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text("Đang chuyển tài khoản")
            },
            text = {
                Text(
                    "Đang gỡ liên kết thiết bị và kết thúc " +
                            "phiên đăng nhập cũ. Vui lòng giữ kết nối Internet."
                )
            },
            confirmButton = {}
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {
                errorMessage = null
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
                        errorMessage = null
                    }
                ) {
                    Text("Đóng")
                }
            }
        )
    }
}