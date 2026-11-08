package com.example.project_lttbd.ui.screens.medicine

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.data.repository.MedicineRepository

@Composable
fun MedicineDeleteAction(
    medicineId: String,
    medicineName: String,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val auth = FirebaseProvider.auth
    val uid = auth.currentUser?.uid
    val repository = remember { MedicineRepository() }

    var confirming by remember(uid, medicineId) {
        mutableStateOf(false)
    }
    var deleting by remember(uid, medicineId) {
        mutableStateOf(false)
    }
    var errorMessage by remember(uid, medicineId) {
        mutableStateOf<String?>(null)
    }

    val latestOnDeleted by rememberUpdatedState(onDeleted)

    // Bỏ qua kết quả cũ nếu đã rời màn hình hoặc đổi tài khoản.
    val active = remember(uid, medicineId) {
        booleanArrayOf(true)
    }

    DisposableEffect(active) {
        active[0] = true
        onDispose { active[0] = false }
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

    if (confirming || deleting) {
        AlertDialog(
            onDismissRequest = {
                if (!deleting) confirming = false
            },
            title = {
                Text(
                    if (deleting) "Đang xóa thuốc…"
                    else "Xóa thuốc?"
                )
            },
            text = {
                Text(
                    if (deleting) {
                        "Đang chờ xác nhận xóa. Vui lòng giữ kết nối Internet."
                    } else {
                        "Bạn muốn xóa $medicineName khỏi danh sách thuốc?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = {
                        if (
                            uid == null ||
                            auth.currentUser?.uid != uid
                        ) {
                            confirming = false
                            errorMessage =
                                "Tài khoản đã thay đổi. " +
                                        "Vui lòng mở lại danh sách thuốc."
                        } else if (!deleting) {
                            deleting = true

                            repository.deleteMedicine(medicineId)
                                .addOnSuccessListener {
                                    if (
                                        active[0] &&
                                        auth.currentUser?.uid == uid
                                    ) {
                                        deleting = false
                                        confirming = false

                                        Toast.makeText(
                                            context,
                                            "Đã xóa thuốc",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        latestOnDeleted()
                                    }
                                }
                                .addOnFailureListener {
                                    if (
                                        active[0] &&
                                        auth.currentUser?.uid == uid
                                    ) {
                                        deleting = false
                                        confirming = false
                                        errorMessage =
                                            "Chưa xóa được thuốc. " +
                                                    "Vui lòng kiểm tra kết nối và thử lại."
                                    }
                                }
                        }
                    }
                ) {
                    Text(if (deleting) "Đang xóa…" else "Xóa")
                }
            },
            dismissButton = {
                if (!deleting) {
                    TextButton(
                        onClick = { confirming = false }
                    ) {
                        Text("Hủy")
                    }
                }
            }
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Chưa xóa được thuốc") },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = { errorMessage = null }
                ) {
                    Text("Đóng")
                }
            }
        )
    }
}