package com.example.project_lttbd.ui.screens.medicine

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.project_lttbd.data.mock.MockMedicineSchedule
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.data.repository.MedicineRepository
import com.example.project_lttbd.ui.components.MedicineFormSkeleton
import com.example.project_lttbd.ui.screens.schedule.ScheduleCreateScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

@Composable
fun MedicineEditFlowScreen(
    medicineId: String,
    onBackClick: () -> Unit,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseProvider.auth

    var uid by remember {
        mutableStateOf(auth.currentUser?.uid)
    }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener {
            uid = it.currentUser?.uid
        }

        auth.addAuthStateListener(listener)

        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    val ownerUid = uid

    if (ownerUid == null) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text("Vui lòng đăng nhập để sửa thuốc.")

            TextButton(
                onClick = onBackClick
            ) {
                Text("Quay lại")
            }
        }

        return
    }

    key(ownerUid, medicineId) {
        val repository = remember {
            MedicineRepository()
        }

        var draft by remember {
            mutableStateOf<ManualMedicineDraft?>(null)
        }

        var savedImageUrl by remember {
            mutableStateOf<String?>(null)
        }

        var pendingImageDraft by remember {
            mutableStateOf<ManualMedicineDraft?>(null)
        }

        var form by remember {
            mutableStateOf<MockMedicineSchedule?>(null)
        }

        var selectedId by remember {
            mutableStateOf<String?>(null)
        }

        var step by remember {
            mutableStateOf(1)
        }

        var loading by remember {
            mutableStateOf(true)
        }

        var error by remember {
            mutableStateOf<String?>(null)
        }

        var retry by remember {
            mutableStateOf(0)
        }

        LaunchedEffect(retry) {
            loading = true
            error = null

            try {
                val medicine = repository
                    .getMedicine(medicineId)
                    .await()
                    .data

                if (auth.currentUser?.uid != ownerUid) {
                    throw CancellationException(
                        "Tài khoản đã thay đổi."
                    )
                }

                draft = ManualMedicineDraft(
                    medicineName = medicine.medicineName,
                    strength = medicine.strength,
                    strengthUnit = medicine.strengthUnit,
                    dosageForm = medicine.dosageForm,
                    note = medicine.note,
                    imageUri = medicine.imageUrl
                )

                savedImageUrl = medicine.imageUrl
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = "Chưa mở được thuốc. Vui lòng thử lại."
            } finally {
                loading = false
            }
        }

        when {
            loading -> {
                MedicineFormSkeleton()
            }

            error != null -> {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(error.orEmpty())

                    TextButton(
                        onClick = {
                            retry++
                        }
                    ) {
                        Text("Thử lại")
                    }

                    TextButton(
                        onClick = onBackClick
                    ) {
                        Text("Quay lại")
                    }
                }
            }

            draft != null -> {
                val medicineDraft = draft!!

                if (step == 1) {
                    ManualMedicineScreen(
                        initialDraft = medicineDraft,
                        isEditing = true,
                        saveMedicineOnly = true,
                        primaryButtonText = "Tiếp tục",
                        deleteAction = null,

                        onBackClick = onBackClick,

                        onContinue = { updatedDraft ->
                            val image = updatedDraft.imageUri

                            if (
                                image != null &&
                                !image.startsWith("https://")
                            ) {
                                pendingImageDraft = updatedDraft
                            } else {
                                draft = updatedDraft
                                step = 2
                            }
                        }
                    )
                } else {
                    BackHandler {
                        step = 1
                    }

                    ScheduleCreateScreen(
                        medicineId = medicineId,
                        draftOverride = medicineDraft,
                        initialForm = form,
                        initialScheduleId = selectedId,

                        onFormChange = {
                            form = it
                        },

                        onSelectionChange = {
                            selectedId = it
                        },

                        allowDeleteMedicine = true,

                        onBackClick = {
                            step = 1
                        },

                        onSaved = {
                            Toast.makeText(
                                context,
                                "Đã lưu thay đổi",
                                Toast.LENGTH_SHORT
                            ).show()

                            onFinished()
                        },

                        onDeleted = onFinished
                    )
                }
            }
        }

        pendingImageDraft?.let { pending ->
            AlertDialog(
                onDismissRequest = {
                    pendingImageDraft = null
                },

                title = {
                    Text("Ảnh mới chưa được tải lên")
                },

                text = {
                    Text(
                        "Bạn có muốn tiếp tục sửa thuốc " +
                                "bằng ảnh đã lưu trước đó không?"
                    )
                },

                confirmButton = {
                    TextButton(
                        onClick = {
                            draft = pending.copy(
                                imageUri = savedImageUrl
                            )

                            pendingImageDraft = null
                            step = 2
                        }
                    ) {
                        Text("Tiếp tục")
                    }
                },

                dismissButton = {
                    TextButton(
                        onClick = {
                            pendingImageDraft = null
                        }
                    ) {
                        Text("Hủy")
                    }
                }
            )
        }
    }
}