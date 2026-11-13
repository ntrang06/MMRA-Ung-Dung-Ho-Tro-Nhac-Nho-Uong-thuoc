package com.example.project_lttbd.ui.screens.schedule

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.data.remote.ScheduleException
import com.example.project_lttbd.data.remote.ScheduleRepository
import com.example.project_lttbd.data.repository.MedicineRecord
import com.example.project_lttbd.data.repository.MedicineRepository
import com.example.project_lttbd.ui.screens.medicine.ManualMedicineDraft
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

@Composable
fun ScheduleCreateScreen(
    medicineId: String,
    onBackClick: () -> Unit,
    onSaved: (String) -> Unit
) {
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
        Column {
            Text("Vui lòng đăng nhập để thiết lập lịch uống.")

            TextButton(onClick = onBackClick) {
                Text("Quay lại")
            }
        }
        return
    }

    key(ownerUid, medicineId) {
        val medicines = remember { MedicineRepository() }
        val schedules = remember { ScheduleRepository() }

        var record by remember {
            mutableStateOf<MedicineRecord?>(null)
        }

        var loading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }
        var retry by remember { mutableStateOf(0) }

        LaunchedEffect(ownerUid, medicineId, retry) {
            loading = true
            error = null

            try {
                val result = medicines
                    .getMedicine(medicineId)
                    .await()

                if (auth.currentUser?.uid == ownerUid) {
                    record = result
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error =
                    "Chưa tải được thuốc. " +
                            "Vui lòng kiểm tra kết nối và thử lại."
            } finally {
                loading = false
            }
        }

        when {
            loading -> {
                Column {
                    CircularProgressIndicator()
                    Text("Đang tải thông tin thuốc…")
                }
            }

            error != null -> {
                Column {
                    Text(error.orEmpty())

                    TextButton(onClick = { retry++ }) {
                        Text("Thử lại")
                    }

                    TextButton(onClick = onBackClick) {
                        Text("Quay lại")
                    }
                }
            }

            record != null -> {
                val medicine = record!!.data

                ScheduleSetupEditor(
                    draft = ManualMedicineDraft(
                        medicineName = medicine.medicineName,
                        strength = medicine.strength,
                        strengthUnit = medicine.strengthUnit,
                        dosageForm = medicine.dosageForm,
                        note = medicine.note,
                        imageUri = medicine.imageUrl
                    ),
                    medicineId = medicineId,
                    stepLabel = "Kiểm tra và lưu",
                    onBackClick = onBackClick,
                    onSaved = onSaved,

                    saveAction = { form ->
                        val input = form.toScheduleInput(medicineId)

                        if (auth.currentUser?.uid != ownerUid) {
                            throw CancellationException(
                                "Tài khoản đã thay đổi."
                            )
                        }

                        // Kiểm tra lại trước khi gửi,
                        // kể cả sau lần bị timeout.
                        val existing = schedules
                            .listSchedules()
                            .filter {
                                it.medicineId == medicineId
                            }

                        if (auth.currentUser?.uid != ownerUid) {
                            throw CancellationException(
                                "Tài khoản đã thay đổi."
                            )
                        }

                        if (existing.isEmpty()) {
                            schedules.createSchedule(input).scheduleId
                        } else if (
                            existing.size == 1 &&
                            existing.single().toInput() == input
                        ) {
                            // Đúng nội dung đã lưu thì dùng kết quả cũ.
                            existing.single().scheduleId
                        } else {
                            throw ScheduleException(
                                "Thuốc này đã có lịch uống khác. " +
                                        "Vui lòng kiểm tra lịch đã lưu trước khi sửa.",
                                "schedule_exists"
                            )
                        }
                    }
                )
            }
        }
    }
}