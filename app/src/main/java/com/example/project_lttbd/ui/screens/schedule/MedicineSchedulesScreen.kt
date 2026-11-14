package com.example.project_lttbd.ui.screens.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.data.remote.ScheduleException
import com.example.project_lttbd.data.remote.ScheduleRepository
import com.example.project_lttbd.data.remote.dto.DoseUnitDto
import com.example.project_lttbd.data.remote.dto.ScheduleResponseDto
import java.util.Locale
import kotlinx.coroutines.CancellationException

@Composable
fun MedicineSchedulesScreen(
    medicineId: String,
    onBackClick: () -> Unit,
    onCreateClick: () -> Unit,
    onEditClick: (String) -> Unit
) {
    val uid = rememberScheduleOwnerUid()

    if (uid == null) {
        Column(Modifier.padding(20.dp)) {
            Text("Vui lòng đăng nhập để xem lịch uống.")

            TextButton(onClick = onBackClick) {
                Text("Quay lại")
            }
        }
        return
    }

    key(uid, medicineId) {
        val repository = remember {
            ScheduleRepository()
        }

        var schedules by remember {
            mutableStateOf<List<ScheduleResponseDto>>(emptyList())
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
            schedules = emptyList()
            error = null

            try {
                val result = repository.listSchedules()

                if (FirebaseProvider.auth.currentUser?.uid == uid) {
                    schedules = result.filter {
                        it.medicineId == medicineId
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                error = (exception as? ScheduleException)?.message
                    ?: "Chưa tải được lịch uống. Vui lòng thử lại."
            } finally {
                loading = false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),

            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onBackClick) {
                Text("Quay lại")
            }

            Text(
                text = "Lịch uống đã lưu",
                style = MaterialTheme.typography.headlineSmall
            )

            when {
                loading -> {
                    CircularProgressIndicator()
                }

                error != null -> {
                    Text(
                        text = error.orEmpty(),
                        color = MaterialTheme.colorScheme.error
                    )

                    TextButton(
                        onClick = { retry++ }
                    ) {
                        Text("Tải lại lịch uống")
                    }
                }

                schedules.isEmpty() -> {
                    Text("Thuốc này chưa có lịch uống.")

                    Button(onClick = onCreateClick) {
                        Text("Thiết lập lịch uống")
                    }
                }

                else -> {
                    schedules.forEachIndexed { index, schedule ->
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Lịch ${index + 1} • " +
                                            if (schedule.enabled) {
                                                "Đang bật"
                                            } else {
                                                "Tạm dừng"
                                            }
                                )

                                Text(
                                    text = "${schedule.doseQuantity} " +
                                            schedule.doseUnit.unitLabel()
                                )

                                Text(
                                    text = schedule.times
                                        .sorted()
                                        .joinToString(" • ") {
                                            String.format(
                                                Locale.ROOT,
                                                "%02d:%02d",
                                                it / 60,
                                                it % 60
                                            )
                                        }
                                )

                                Text(
                                    text = "Từ ${schedule.startDate} đến " +
                                            (
                                                    schedule.endDate
                                                        ?: "khi ngừng lịch"
                                                    )
                                )

                                TextButton(
                                    onClick = {
                                        onEditClick(schedule.scheduleId)
                                    }
                                ) {
                                    Text("Sửa lịch uống")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DoseUnitDto.unitLabel(): String = when (this) {
    DoseUnitDto.TABLET -> "viên"
    DoseUnitDto.SACHET -> "gói"
    DoseUnitDto.ML -> "ml"
    DoseUnitDto.DROP -> "giọt"
    DoseUnitDto.DOSE -> "liều"
}