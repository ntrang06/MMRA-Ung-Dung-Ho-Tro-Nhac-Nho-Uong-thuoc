package com.example.project_lttbd.ui.screens.schedule

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.project_lttbd.data.mock.MockMedicineSchedule
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.data.remote.ScheduleException
import com.example.project_lttbd.data.remote.ScheduleRepository
import com.example.project_lttbd.data.remote.dto.*
import com.example.project_lttbd.data.repository.MedicineRecord
import com.example.project_lttbd.data.repository.MedicineRepository
import com.example.project_lttbd.ui.components.ScheduleFormSkeleton
import com.example.project_lttbd.ui.screens.medicine.ManualMedicineDraft
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ScheduleCreateScreen(
    medicineId: String,
    onBackClick: () -> Unit,
    onSaved: (String) -> Unit,
    draftOverride: ManualMedicineDraft? = null,
    initialForm: MockMedicineSchedule? = null,
    initialScheduleId: String? = null,
    onFormChange: (MockMedicineSchedule) -> Unit = {},
    onSelectionChange: (String?) -> Unit = {},
    allowDeleteMedicine: Boolean = false,
    onDeleted: () -> Unit = onBackClick
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
        Column(Modifier.padding(20.dp)) {
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
        val scope = rememberCoroutineScope()

        var record by remember {
            mutableStateOf<MedicineRecord?>(null)
        }

        var options by remember {
            mutableStateOf<List<ScheduleResponseDto>>(emptyList())
        }

        var selected by remember {
            mutableStateOf<ScheduleResponseDto?>(null)
        }

        var seed by remember {
            mutableStateOf<MockMedicineSchedule?>(null)
        }

        var loading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }
        var retry by remember { mutableStateOf(0) }
        var generation by remember { mutableStateOf(0) }
        var mustReload by remember { mutableStateOf(false) }
        var confirmReload by remember { mutableStateOf(false) }
        var confirmDelete by remember { mutableStateOf(false) }
        var deleting by remember { mutableStateOf(false) }
        var expanded by remember { mutableStateOf(false) }

        var deleteError by remember {
            mutableStateOf<String?>(null)
        }

        fun checkOwner() {
            if (auth.currentUser?.uid != ownerUid) {
                throw CancellationException("Tài khoản đã thay đổi.")
            }
        }

        LaunchedEffect(retry) {
            loading = true
            error = null

            try {
                val loadedMedicine = medicines
                    .getMedicine(medicineId)
                    .await()

                checkOwner()

                val loadedSchedules = schedules
                    .listSchedules()
                    .filter {
                        it.medicineId == medicineId
                    }

                checkOwner()

                val chosen = loadedSchedules.firstOrNull {
                    it.scheduleId ==
                            (selected?.scheduleId ?: initialScheduleId)
                } ?: loadedSchedules.singleOrNull()

                val restored = initialForm?.takeIf {
                    retry == 0 &&
                            initialScheduleId == chosen?.scheduleId
                }

                seed = restored
                    ?: chosen?.toEditForm()
                            ?: if (loadedSchedules.isEmpty()) {
                        MockMedicineSchedule(
                            quantity = "",
                            unit = "",
                            times = emptyList()
                        )
                    } else {
                        null
                    }

                record = loadedMedicine
                options = loadedSchedules
                selected = chosen

                onSelectionChange(chosen?.scheduleId)
                seed?.let(onFormChange)

                mustReload = false
                generation++
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: ScheduleException) {
                error = exception.message
            } catch (_: Exception) {
                error =
                    "Chưa tải được thuốc hoặc lịch uống. Vui lòng thử lại."
            } finally {
                loading = false
            }
        }

        BackHandler(enabled = deleting) {}

        when {
            loading -> {
                ScheduleFormSkeleton()
            }

            error != null -> {
                Column(Modifier.padding(20.dp)) {
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
                val originalMedicine = record!!.data

                val draft = draftOverride ?: ManualMedicineDraft(
                    medicineName = originalMedicine.medicineName,
                    strength = originalMedicine.strength,
                    strengthUnit = originalMedicine.strengthUnit,
                    dosageForm = originalMedicine.dosageForm,
                    note = originalMedicine.note,
                    imageUri = originalMedicine.imageUrl
                )

                Column(Modifier.fillMaxSize()) {
                    if (options.size > 1) {
                        Box(
                            Modifier.padding(horizontal = 20.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    expanded = true
                                }
                            ) {
                                Text(
                                    selected?.let {
                                        "Lịch: ${scheduleTimesLabel(it.times)}"
                                    } ?: "Chọn lịch uống cần sửa"
                                )
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = {
                                    expanded = false
                                }
                            ) {
                                options.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            val times =
                                                scheduleTimesLabel(option.times)

                                            val status = if (option.enabled) {
                                                ""
                                            } else {
                                                " • Đang tạm dừng"
                                            }

                                            Text("$times$status")
                                        },
                                        onClick = {
                                            try {
                                                val nextForm = option.toEditForm()

                                                selected = option
                                                seed = nextForm

                                                onSelectionChange(
                                                    option.scheduleId
                                                )

                                                onFormChange(nextForm)
                                                generation++
                                            } catch (_: Exception) {
                                                error =
                                                    "Dữ liệu lịch uống không hợp lệ. " +
                                                            "Vui lòng tải lại."
                                            }

                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    val startingForm = seed

                    if (startingForm == null) {
                        Text(
                            text = "Thuốc có nhiều lịch uống. " +
                                    "Hãy chọn lịch cần sửa.",
                            modifier = Modifier.padding(20.dp)
                        )

                        TextButton(onClick = onBackClick) {
                            Text("Quay lại")
                        }
                    } else {
                        key(generation) {
                            ScheduleSetupEditor(
                                draft = draft,
                                medicineId = medicineId,
                                modifier = Modifier.weight(1f),
                                initialSchedule = startingForm,
                                onDraftChange = onFormChange,

                                stepLabel = if (draftOverride == null) {
                                    "Kiểm tra và lưu"
                                } else {
                                    "Kiểm tra và lưu thay đổi"
                                },

                                saveButtonText = if (draftOverride == null) {
                                    "Lưu lịch uống"
                                } else {
                                    "Lưu thay đổi"
                                },

                                actionsEnabled = !deleting && !mustReload,
                                onBackClick = onBackClick,
                                onSaved = onSaved,

                                footerContent = {
                                    if (selected?.enabled == false) {
                                        Text(
                                            text = "Lịch đang tạm dừng; " +
                                                    "lưu thay đổi sẽ giữ trạng thái này.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    if (mustReload) {
                                        TextButton(
                                            onClick = {
                                                confirmReload = true
                                            }
                                        ) {
                                            Text("Tải lại lịch đã lưu")
                                        }
                                    }

                                    if (allowDeleteMedicine) {
                                        TextButton(
                                            onClick = {
                                                confirmDelete = true
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = !deleting
                                        ) {
                                            Text(
                                                text = "Xóa thuốc",
                                                color = Color(0xFFB3261E)
                                            )
                                        }
                                    }
                                },

                                saveAction = { form ->
                                    checkOwner()

                                    val baseline = selected

                                    val input = form
                                        .toScheduleInput(medicineId)
                                        .copy(
                                            timeZone = baseline?.timeZone
                                                ?: "Asia/Ho_Chi_Minh",
                                            enabled = baseline?.enabled ?: true
                                        )

                                    require(
                                        draft.imageUri == null ||
                                                draft.imageUri.startsWith("https://")
                                    ) {
                                        "Ảnh vừa chọn chưa được tải lên. " +
                                                "Hãy quay lại dùng ảnh đã lưu trước khi tiếp tục."
                                    }

                                    val result = try {
                                        val latest = schedules
                                            .listSchedules()
                                            .filter {
                                                it.medicineId == medicineId
                                            }

                                        checkOwner()

                                        val current = if (baseline == null) {
                                            latest.singleOrNull()
                                        } else {
                                            latest.firstOrNull {
                                                it.scheduleId == baseline.scheduleId
                                            }
                                        }

                                        when {
                                            baseline == null &&
                                                    latest.isEmpty() -> {
                                                schedules.createSchedule(input)
                                            }

                                            current != null &&
                                                    current.toInput() == input -> {
                                                current
                                            }

                                            baseline != null &&
                                                    current?.version == baseline.version -> {
                                                schedules.updateSchedule(
                                                    baseline.scheduleId,
                                                    input.asUpdate(
                                                        baseline.version
                                                    )
                                                )
                                            }

                                            else -> {
                                                throw ScheduleException(
                                                    "Lịch đã thay đổi. " +
                                                            "Hãy tải lại lịch đã lưu trước khi sửa tiếp.",
                                                    "version_conflict",
                                                    409
                                                )
                                            }
                                        }
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (exception: Exception) {
                                        mustReload = true
                                        throw exception
                                    }

                                    checkOwner()

                                    selected = result

                                    options = options.filterNot {
                                        it.scheduleId == result.scheduleId
                                    } + result

                                    onSelectionChange(result.scheduleId)

                                    if (draftOverride != null) {
                                        try {
                                            checkOwner()

                                            medicines.updateMedicine(
                                                medicineId,
                                                MedicineDto(
                                                    medicineName = draft.medicineName,
                                                    strength = draft.strength,
                                                    strengthUnit =
                                                        if (draft.strength.isBlank()) {
                                                            ""
                                                        } else {
                                                            draft.strengthUnit
                                                        },
                                                    dosageForm = draft.dosageForm,
                                                    note = draft.note,
                                                    imageUrl = draft.imageUri
                                                )
                                            ).await()

                                            checkOwner()
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (_: Exception) {
                                            throw ScheduleException(
                                                "Lịch uống đã lưu, nhưng chưa xác nhận " +
                                                        "được thông tin thuốc. Giữ nguyên nội dung " +
                                                        "và nhấn Lưu thay đổi để thử lại.",
                                                "medicine_save_failed"
                                            )
                                        }
                                    }

                                    result.scheduleId
                                }
                            )
                        }
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
                        "Các thay đổi liều và giờ chưa lưu sẽ được thay " +
                                "bằng dữ liệu trên máy chủ. " +
                                "Thông tin thuốc vừa nhập vẫn được giữ."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmReload = false
                            retry++
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
                        Text("Hủy")
                    }
                }
            )
        }

        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = {
                    confirmDelete = false
                },
                title = {
                    Text("Xóa thuốc?")
                },
                text = {
                    Text(
                        "Hủy các liều chờ từ thời điểm xóa của mọi lịch " +
                                "thuộc thuốc này, sau đó xóa thuốc. " +
                                "Lịch sử cũ được giữ lại."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmDelete = false
                            deleting = true
                            deleteError = null

                            scope.launch {
                                try {
                                    checkOwner()

                                    val existing = schedules
                                        .listSchedules()
                                        .filter {
                                            it.medicineId == medicineId
                                        }

                                    checkOwner()

                                    existing.forEach {
                                        checkOwner()

                                        schedules.deleteSchedule(
                                            it.scheduleId,
                                            it.version
                                        )
                                    }

                                    checkOwner()

                                    medicines
                                        .deleteMedicine(medicineId)
                                        .await()

                                    checkOwner()

                                    Toast.makeText(
                                        context,
                                        "Đã xóa thuốc",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    onDeleted()
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    mustReload = true

                                    deleteError =
                                        "Chưa xác nhận được toàn bộ thao tác xóa. " +
                                                "Một số lịch có thể đã hủy. " +
                                                "Hãy thử Xóa thuốc lại hoặc tải lại lịch đã lưu."
                                } finally {
                                    deleting = false
                                }
                            }
                        }
                    ) {
                        Text(
                            text = "Xóa thuốc",
                            color = Color(0xFFB3261E)
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            confirmDelete = false
                        }
                    ) {
                        Text("Hủy")
                    }
                }
            )
        }

        if (deleting) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text("Đang xóa thuốc")
                },
                text = {
                    CircularProgressIndicator()
                },
                confirmButton = {}
            )
        }

        deleteError?.let { message ->
            AlertDialog(
                onDismissRequest = {
                    deleteError = null
                },
                title = {
                    Text("Chưa xóa xong")
                },
                text = {
                    Text(message)
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            deleteError = null
                        }
                    ) {
                        Text("Đã hiểu")
                    }
                }
            )
        }
    }
}

private fun scheduleTimesLabel(
    times: List<Int>
): String {
    return times
        .sorted()
        .joinToString(" • ") { minute ->
            String.format(
                Locale.ROOT,
                "%02d:%02d",
                minute / 60,
                minute % 60
            )
        }
}

private fun ScheduleInputDto.asUpdate(
    version: Int
): ScheduleUpdateDto {
    return ScheduleUpdateDto(
        medicineId = medicineId,
        doseQuantity = doseQuantity,
        doseUnit = doseUnit,
        times = times,
        weekdays = weekdays,
        startDate = startDate,
        endDate = endDate,
        timeZone = timeZone,
        mealInstruction = mealInstruction,
        reminderMinutes = reminderMinutes,
        enabled = enabled,
        expectedVersion = version
    )
}

private fun ScheduleResponseDto.toEditForm(): MockMedicineSchedule {
    fun day(value: String): Long {
        require(
            Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value)
        )

        val format = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.ROOT
        ).apply {
            isLenient = false
        }

        return requireNotNull(format.parse(value)).time
    }

    return MockMedicineSchedule(
        quantity = doseQuantity,

        unit = when (doseUnit) {
            DoseUnitDto.TABLET -> "viên"
            DoseUnitDto.SACHET -> "gói"
            DoseUnitDto.ML -> "ml"
            DoseUnitDto.DROP -> "giọt"
            DoseUnitDto.DOSE -> "liều"
        },

        times = times.sorted(),

        weekdays = weekdays.map {
            it % 7 + 1
        }.toSet(),

        meal = mealInstruction,
        reminderMinutes = reminderMinutes,
        startDay = day(startDate),

        endDay = endDate?.let {
            day(it)
        }
    )
}