package com.example.project_lttbd.ui.screens.medicine

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.project_lttbd.data.remote.DoseException
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.data.remote.MedicationOverviewRepository
import com.example.project_lttbd.notifications.NotificationHelper
import com.example.project_lttbd.notifications.alarm.MedicationAlarmScheduler
import com.example.project_lttbd.ui.screens.schedule.rememberScheduleOwnerUid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MedicineDeleteAction(
    medicineId: String,
    medicineName: String,
    onDeleted: () -> Unit
) {
    val uid = rememberScheduleOwnerUid()
    val context = LocalContext.current.applicationContext
    val latestOnDeleted by rememberUpdatedState(onDeleted)

    key(uid, medicineId) {
        val repository = remember {
            MedicationOverviewRepository()
        }

        val scope = rememberCoroutineScope()

        var confirming by remember {
            mutableStateOf(false)
        }

        var deleting by remember {
            mutableStateOf(false)
        }

        var errorMessage by remember {
            mutableStateOf<String?>(null)
        }

        fun ensureOwner() {
            if (
                uid == null ||
                FirebaseProvider.auth.currentUser?.uid != uid
            ) {
                throw CancellationException(
                    "Tài khoản đã thay đổi."
                )
            }
        }

        TextButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = uid != null && !deleting,
            onClick = {
                errorMessage = null
                confirming = true
            }
        ) {
            Text(
                text = "Xóa thuốc",
                color = Color(0xFFB3261E)
            )
        }

        if (confirming) {
            AlertDialog(
                onDismissRequest = {
                    if (!deleting) {
                        confirming = false
                    }
                },
                title = {
                    Text(
                        if (deleting) {
                            "Đang xóa thuốc…"
                        } else {
                            "Xóa thuốc?"
                        }
                    )
                },
                text = {
                    Text(
                        if (deleting) {
                            "Vui lòng giữ kết nối Internet."
                        } else {
                            "Xóa $medicineName và dừng các lịch uống liên quan? " +
                                    "Lịch sử đã ghi nhận vẫn được giữ lại."
                        }
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = !deleting,
                        onClick = {
                            if (!deleting) {
                                deleting = true

                                scope.launch {
                                    try {
                                        ensureOwner()

                                        val result =
                                            repository.deleteMedicine(
                                                medicineId
                                            )

                                        ensureOwner()

                                        withContext(Dispatchers.IO) {
                                            result.doseIdsToCancel.forEach { doseId ->
                                                ensureOwner()

                                                MedicationAlarmScheduler.cancel(
                                                    context,
                                                    doseId
                                                )

                                                NotificationHelper.cancelNotification(
                                                    context,
                                                    doseId
                                                )
                                            }
                                        }

                                        ensureOwner()
                                        confirming = false

                                        Toast.makeText(
                                            context,
                                            "Đã xóa thuốc",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        latestOnDeleted()
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (error: Exception) {
                                        ensureOwner()
                                        confirming = false

                                        errorMessage =
                                            (error as? DoseException)?.message
                                                ?: "Chưa hoàn tất thao tác. " +
                                                        "Vui lòng tải lại danh sách để kiểm tra."
                                    } finally {
                                        deleting = false
                                    }
                                }
                            }
                        }
                    ) {
                        Text(
                            if (deleting) "Đang xóa…" else "Xóa"
                        )
                    }
                },
                dismissButton = {
                    if (!deleting) {
                        TextButton(
                            onClick = {
                                confirming = false
                            }
                        ) {
                            Text("Hủy")
                        }
                    }
                }
            )
        }

        errorMessage?.let { message ->
            AlertDialog(
                onDismissRequest = {
                    errorMessage = null
                },
                title = {
                    Text("Cần kiểm tra lại danh sách thuốc")
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
}