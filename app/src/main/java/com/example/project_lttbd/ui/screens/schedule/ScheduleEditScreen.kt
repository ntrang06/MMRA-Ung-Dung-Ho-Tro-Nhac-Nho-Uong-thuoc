package com.example.project_lttbd.ui.screens.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.project_lttbd.data.mock.MockMedicineSchedule
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.data.remote.ScheduleException
import com.example.project_lttbd.data.remote.ScheduleRepository
import com.example.project_lttbd.data.remote.dto.ScheduleResponseDto
import com.example.project_lttbd.data.repository.MedicineRepository
import com.example.project_lttbd.ui.screens.medicine.ManualMedicineDraft
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

@Composable
internal fun rememberScheduleOwnerUid(): String? {
    val auth = FirebaseProvider.auth

    var uid by remember(auth) {
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

    return uid
}

private data class LoadedSchedule(
    val schedule: ScheduleResponseDto,
    val medicine: ManualMedicineDraft,
    val form: MockMedicineSchedule
)

@Composable
fun ScheduleEditScreen(
    scheduleId: String,
    onBackClick: () -> Unit,
    onSaved: (String) -> Unit
) {
    val uid = rememberScheduleOwnerUid()

    if (uid == null) {
        Column(Modifier.padding(20.dp)) {
            Text("Vui lòng đăng nhập để sửa lịch uống.")

            TextButton(onClick = onBackClick) {
                Text("Quay lại")
            }
        }
        return
    }

    key(uid, scheduleId) {
        val repository = remember {
            ScheduleRepository()
        }

        val medicines = remember {
            MedicineRepository()
        }

        var loaded by remember {
            mutableStateOf<LoadedSchedule?>(null)
        }

        var loading by remember {
            mutableStateOf(true)
        }

        var error by remember {
            mutableStateOf<String?>(null)
        }

        var revision by remember {
            mutableStateOf(0)
        }

        var mustReload by remember {
            mutableStateOf(false)
        }

        var confirmReload by remember {
            mutableStateOf(false)
        }

        LaunchedEffect(revision) {
            loading = true
            loaded = null
            error = null

            try {
                val schedule = repository.getSchedule(scheduleId)

                if (FirebaseProvider.auth.currentUser?.uid != uid) {
                    throw CancellationException(
                        "Tài khoản đã thay đổi."
                    )
                }

                val medicine = medicines
                    .getMedicine(schedule.medicineId)
                    .await()
                    .data

                if (FirebaseProvider.auth.currentUser?.uid != uid) {
                    throw CancellationException(
                        "Tài khoản đã thay đổi."
                    )
                }

                loaded = LoadedSchedule(
                    schedule = schedule,

                    medicine = ManualMedicineDraft(
                        medicineName = medicine.medicineName,
                        strength = medicine.strength,
                        strengthUnit = medicine.strengthUnit,
                        dosageForm = medicine.dosageForm,
                        note = medicine.note,
                        imageUri = medicine.imageUrl
                    ),

                    form = schedule.toEditorSchedule()
                )

                mustReload = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                error = (exception as? ScheduleException)?.message
                    ?: "Chưa tải được thông tin lịch và thuốc. Vui lòng thử lại."
            } finally {
                loading = false
            }
        }

        val current = loaded

        when {
            loading -> {
                Column(Modifier.padding(20.dp)) {
                    CircularProgressIndicator()
                    Text("Đang tải lịch đã lưu…")

                    TextButton(onClick = onBackClick) {
                        Text("Quay lại")
                    }
                }
            }

            error != null || current == null -> {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        error ?: "Không tìm thấy lịch uống."
                    )

                    TextButton(
                        onClick = { revision++ }
                    ) {
                        Text("Thử lại")
                    }

                    TextButton(onClick = onBackClick) {
                        Text("Quay lại")
                    }
                }
            }

            else -> {
                Column(Modifier.fillMaxSize()) {
                    if (mustReload) {
                        TextButton(
                            onClick = {
                                confirmReload = true
                            }
                        ) {
                            Text("Tải lại lịch đã lưu")
                        }
                    }

                    key(current.schedule.version, revision) {
                        ScheduleSetupEditor(
                            draft = current.medicine,
                            medicineId = current.schedule.medicineId,
                            initialSchedule = current.form,
                            modifier = Modifier.weight(1f),
                            stepLabel = "Chỉnh sửa lịch uống",
                            onBackClick = onBackClick,
                            onSaved = onSaved,

                            saveAction = { form ->
                                if (mustReload) {
                                    throw ScheduleException(
                                        "Vui lòng bấm Tải lại lịch đã lưu trước khi lưu tiếp."
                                    )
                                }

                                if (
                                    FirebaseProvider.auth.currentUser?.uid != uid
                                ) {
                                    throw CancellationException(
                                        "Tài khoản đã thay đổi."
                                    )
                                }

                                try {
                                    repository.updateSchedule(
                                        scheduleId,
                                        form.toScheduleUpdate(
                                            current.schedule
                                        )
                                    ).scheduleId
                                } catch (exception: ScheduleException) {
                                    if (
                                        exception.requiresScheduleReload()
                                    ) {
                                        mustReload = true
                                    }

                                    throw exception
                                }
                            }
                        )
                    }
                }
            }
        }

        if (confirmReload) {
            AlertDialog(
                onDismissRequest = {
                    confirmReload = false
                },

                title = {
                    Text("Tải lại lịch uống?")
                },

                text = {
                    Text(
                        "Các thay đổi chưa lưu trên form sẽ được " +
                                "thay bằng dữ liệu máy chủ."
                    )
                },

                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmReload = false
                            revision++
                        }
                    ) {
                        Text("Tải lại")
                    }
                },

                dismissButton = {
                    TextButton(
                        onClick = {
                            confirmReload = false
                        }
                    ) {
                        Text("Để sau")
                    }
                }
            )
        }
    }
}

internal fun ScheduleException.requiresScheduleReload(): Boolean {
    return httpCode == 409 ||
            httpCode in 500..599 ||
            errorCode in setOf(
        "network_error",
        "invalid_response"
    )
}