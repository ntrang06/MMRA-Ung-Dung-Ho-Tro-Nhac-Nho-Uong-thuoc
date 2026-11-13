package com.example.project_lttbd.ui.screens.schedule

import android.app.DatePickerDialog
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.data.model.UserProfileManager
import com.example.project_lttbd.data.remote.DoseException
import com.example.project_lttbd.data.remote.DoseRepository
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.data.remote.dto.DoseActionDto
import com.example.project_lttbd.data.remote.dto.DoseResponseDto
import com.example.project_lttbd.data.remote.dto.DoseStatusDto
import com.example.project_lttbd.notifications.sync.MedicationReminderSync
import com.example.project_lttbd.ui.components.rememberUiNow
import com.example.project_lttbd.ui.screens.reminder.DoseReminderScreen
import com.example.project_lttbd.ui.screens.reminder.ReminderDoseUiModel
import com.example.project_lttbd.ui.screens.reminder.ReminderNoticeUiModel
import com.example.project_lttbd.ui.screens.reminder.rememberReminderSpeech
import com.example.project_lttbd.ui.theme.MMRATheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

private data class DailyDoseLoad(
    val loading: Boolean = true,
    val doses: List<DoseResponseDto> = emptyList(),
    val error: String? = null
)

private enum class DailyDoseAction {
    TAKEN,
    SKIPPED,
    SNOOZE
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun ScheduleScreen(
    modifier: Modifier = Modifier,
    onDoseClick: (String) -> Unit = {},
    onAccountClick: () -> Unit = {}
) {
    if (LocalInspectionMode.current) {
        ScheduleListContent(
            modifier = modifier,
            selectedDateMillis = scheduleDayMillis(
                System.currentTimeMillis()
            ),
            todayMillis = scheduleDayMillis(
                System.currentTimeMillis()
            ),
            doses = listOf(
                ScheduleDoseUiModel(
                    doseId = "preview",
                    medicineId = "preview_medicine",
                    time = "08:00",
                    medicineName = "Thuốc mẫu",
                    quantityText = "1 viên",
                    instruction = "Sau bữa ăn",
                    status = ScheduleDoseStatus.PENDING
                )
            ),
            loading = false,
            signedIn = true,
            error = null,
            onDateSelected = {},
            onRefresh = {},
            onDoseClick = {},
            onAccountClick = onAccountClick
        )
        return
    }

    val ownerUid = rememberScheduleOwnerUid()

    // Đổi tài khoản sẽ tạo lại toàn bộ trạng thái của màn này.
    key(ownerUid) {
        DailyScheduleContent(
            ownerUid = ownerUid,
            modifier = modifier,
            onAccountClick = onAccountClick
        )
    }
}

@Composable
private fun DailyScheduleContent(
    ownerUid: String?,
    modifier: Modifier,
    onAccountClick: () -> Unit
) {
    val context = LocalContext.current
    val now = rememberUiNow()

    val todayMillis = scheduleDayMillis(now)

    var selectedDateMillis by rememberSaveable {
        mutableStateOf(todayMillis)
    }

    val dateKey = scheduleDateKey(selectedDateMillis)

    val repository = remember {
        DoseRepository()
    }

    var revision by remember {
        mutableStateOf(0)
    }

    var loaded by remember(ownerUid, dateKey) {
        mutableStateOf(DailyDoseLoad())
    }

    var selectedDoseId by rememberSaveable(ownerUid, dateKey) {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(ownerUid, dateKey, revision) {
        loaded = DailyDoseLoad()

        if (ownerUid == null) {
            loaded = DailyDoseLoad(loading = false)
            return@LaunchedEffect
        }

        try {
            ensureDailyDoseOwner(ownerUid)

            val result = repository.listDoses(dateKey)

            currentCoroutineContext().ensureActive()
            ensureDailyDoseOwner(ownerUid)

            loaded = DailyDoseLoad(
                loading = false,
                doses = result
                    .filter {
                        it.localDate == dateKey &&
                                it.status != DoseStatusDto.CANCELLED
                    }
                    .sortedWith(
                        compareBy<DoseResponseDto> {
                            it.minuteOfDay
                        }.thenBy {
                            it.doseId
                        }
                    )
            )

            // Đồng bộ báo thức bằng kết quả thật từ máy chủ.
            // Lỗi báo thức cục bộ không biến thành lỗi tải API.
            runCatching {
                MedicationReminderSync.syncDoses(
                    context = context,
                    uid = ownerUid,
                    doses = result
                )
            }.onFailure {
                Log.w(
                    "MMRA_API_ERROR",
                    "Đã tải liều nhưng chưa đồng bộ được báo thức cục bộ.",
                    it
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            currentCoroutineContext().ensureActive()
            ensureDailyDoseOwner(ownerUid)

            loaded = DailyDoseLoad(
                loading = false,
                error = (error as? DoseException)?.message
                    ?: "Chưa tải được lịch uống. Vui lòng thử lại."
            )
        }
    }

    val selectedDose = loaded.doses.firstOrNull {
        it.doseId == selectedDoseId
    }

    if (selectedDose != null && ownerUid != null) {
        DailyDoseDetail(
            ownerUid = ownerUid,
            dose = selectedDose,
            repository = repository,
            modifier = modifier,
            onUpdated = { updated ->
                loaded = loaded.copy(
                    doses = loaded.doses.map {
                        if (it.doseId == updated.doseId) {
                            updated
                        } else {
                            it
                        }
                    }
                )
            },
            onClose = {
                selectedDoseId = null
                revision++
            }
        )
        return
    }

    ScheduleListContent(
        modifier = modifier,
        selectedDateMillis = selectedDateMillis,
        todayMillis = todayMillis,
        doses = loaded.doses.map {
            it.toScheduleDoseUiModel()
        },
        loading = loaded.loading,
        signedIn = ownerUid != null,
        error = loaded.error,
        onDateSelected = {
            selectedDoseId = null
            selectedDateMillis = it
        },
        onRefresh = {
            selectedDoseId = null
            revision++
        },
        onDoseClick = {
            selectedDoseId = it
        },
        onAccountClick = onAccountClick
    )
}

@Composable
private fun ScheduleListContent(
    selectedDateMillis: Long,
    todayMillis: Long,
    doses: List<ScheduleDoseUiModel>,
    loading: Boolean,
    signedIn: Boolean,
    error: String?,
    onDateSelected: (Long) -> Unit,
    onRefresh: () -> Unit,
    onDoseClick: (String) -> Unit,
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAF9))
            .verticalScroll(rememberScrollState())
    ) {
        ScheduleHeader(
            initials = UserProfileManager.getInitials(),
            onCalendarClick = {
                val selected = Calendar.getInstance().apply {
                    timeInMillis = selectedDateMillis
                }

                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val date = Calendar.getInstance().apply {
                            clear()
                            set(year, month, day, 12, 0, 0)
                        }

                        onDateSelected(date.timeInMillis)
                    },
                    selected.get(Calendar.YEAR),
                    selected.get(Calendar.MONTH),
                    selected.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            onAccountClick = onAccountClick
        )

        Column(
            modifier = Modifier.padding(
                start = 20.dp,
                top = 6.dp,
                end = 20.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ScheduleWeekPicker(
                selectedDateMillis = selectedDateMillis,
                todayMillis = todayMillis,
                onDateSelected = onDateSelected
            )

            when {
                !signedIn -> {
                    Text(
                        text = "Vui lòng đăng nhập để xem lịch uống.",
                        color = Color(0xFF526565)
                    )

                    TextButton(onClick = onAccountClick) {
                        Text(
                            text = "Mở tài khoản",
                            color = Color(0xFF126B62)
                        )
                    }
                }

                loading -> {
                    DailyDoseListSkeleton()
                }

                error != null -> {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )

                    TextButton(onClick = onRefresh) {
                        Text(
                            text = "Thử lại",
                            color = Color(0xFF126B62)
                        )
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${doses.size} liều thuốc",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                lineHeight = 22.sp,
                                letterSpacing = 0.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF102A2A)
                        )

                        TextButton(onClick = onRefresh) {
                            Text(
                                text = "Tải lại",
                                color = Color(0xFF126B62)
                            )
                        }
                    }

                    if (doses.isEmpty()) {
                        Text(
                            text = "Chưa có liều thuốc cho ngày này.",
                            modifier = Modifier.padding(vertical = 16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF526565)
                        )
                    } else {
                        Text(
                            text = "Chạm vào liều thuốc để xem chi tiết",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF526565)
                        )

                        doses.forEach { dose ->
                            key(dose.doseId) {
                                ScheduleDoseCard(
                                    dose = dose,
                                    onClick = onDoseClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyDoseDetail(
    ownerUid: String,
    dose: DoseResponseDto,
    repository: DoseRepository,
    onUpdated: (DoseResponseDto) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val now = rememberUiNow()

    val ui = dose.toScheduleDoseUiModel()

    var busy by remember(dose.doseId) {
        mutableStateOf(false)
    }

    var mustReload by remember(dose.doseId) {
        mutableStateOf(false)
    }

    var confirmation by remember(dose.doseId) {
        mutableStateOf<DailyDoseAction?>(null)
    }

    var errorMessage by remember(dose.doseId) {
        mutableStateOf<String?>(null)
    }

    var successMessage by remember(dose.doseId) {
        mutableStateOf<String?>(null)
    }

    val isToday = dose.localDate == scheduleDateKey(
        millis = now,
        timeZone = TimeZone.getTimeZone(dose.timeZone)
    )

    val canAct = isToday &&
            dose.status == DoseStatusDto.PENDING &&
            !busy &&
            !mustReload &&
            successMessage == null

    val reminder = ReminderDoseUiModel(
        doseId = dose.doseId,
        medicineId = dose.medicineId,
        timeLabel = "${ui.time} • ${dose.localDate}",
        medicineName = ui.medicineName,
        quantityText = ui.quantityText,
        mealInstruction = ui.instruction,
        scheduledAtMillis = MedicationReminderSync.parseIsoToMillis(
            dose.scheduledAt
        )
    )

    val playInstructions = rememberReminderSpeech(
        "${ui.medicineName}. ${ui.quantityText}. ${ui.instruction}."
    )

    fun submit(action: DailyDoseAction) {
        if (busy || mustReload || !canAct) {
            return
        }

        busy = true

        scope.launch {
            try {
                ensureDailyDoseOwner(ownerUid)

                val updated = when (action) {
                    DailyDoseAction.TAKEN ->
                        repository.updateDoseAction(
                            doseId = dose.doseId,
                            action = DoseActionDto.TAKEN
                        )

                    DailyDoseAction.SKIPPED ->
                        repository.updateDoseAction(
                            doseId = dose.doseId,
                            action = DoseActionDto.SKIPPED
                        )

                    DailyDoseAction.SNOOZE ->
                        repository.snoozeDose(
                            doseId = dose.doseId,
                            minutes = 10
                        )
                }

                currentCoroutineContext().ensureActive()
                ensureDailyDoseOwner(ownerUid)

                if (updated.doseId != dose.doseId) {
                    throw DoseException(
                        "Chưa nhận được kết quả hợp lệ. " +
                                "Vui lòng tải lại lịch uống.",
                        "invalid_response"
                    )
                }

                onUpdated(updated)

                successMessage = when (action) {
                    DailyDoseAction.TAKEN ->
                        "Đã ghi nhận bạn uống liều thuốc này."

                    DailyDoseAction.SKIPPED ->
                        "Đã ghi nhận bỏ qua liều thuốc này."

                    DailyDoseAction.SNOOZE ->
                        "Đã lưu yêu cầu nhắc lại sau 10 phút."
                }

                runCatching {
                    MedicationReminderSync.syncDoses(
                        context = context,
                        uid = ownerUid,
                        doses = listOf(updated)
                    )
                }.onFailure {
                    Log.w(
                        "MMRA_API_ERROR",
                        "Đã lưu thao tác nhưng chưa đồng bộ được báo thức cục bộ.",
                        it
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                ensureDailyDoseOwner(ownerUid)

                // Không gửi lại thao tác khi chưa biết kết quả lần trước.
                mustReload = true

                errorMessage = (error as? DoseException)?.message
                    ?: "Chưa xác nhận được kết quả thao tác. " +
                            "Vui lòng tải lại lịch uống trước khi thử lại."
            } finally {
                busy = false
            }
        }
    }

    BackHandler {
        if (!busy) {
            onClose()
        }
    }

    val notice = when {
        busy -> ReminderNoticeUiModel(
            title = "Đang lưu",
            message = "Vui lòng chờ máy chủ xác nhận."
        )

        mustReload -> ReminderNoticeUiModel(
            title = "Cần tải lại lịch uống",
            message = "Đóng màn này để kiểm tra kết quả trên máy chủ " +
                    "trước khi thao tác tiếp."
        )

        dose.status != DoseStatusDto.PENDING -> ReminderNoticeUiModel(
            title = ui.status.label,
            message = "Đây là trạng thái đã được ghi nhận trên máy chủ."
        )

        !isToday -> ReminderNoticeUiModel(
            title = "Xem chi tiết",
            message = "Bạn đang xem liều của ngày đã chọn. " +
                    "Thao tác xác nhận được thực hiện với liều hôm nay."
        )

        !dose.nextReminderAt.isNullOrBlank() -> ReminderNoticeUiModel(
            title = "Đã chọn nhắc lại",
            message = "Liều này vẫn đang chờ xác nhận đã uống."
        )

        else -> null
    }

    DoseReminderScreen(
        dose = reminder,
        modifier = modifier,
        notice = notice,
        actionsEnabled = canAct,
        onClose = {
            if (!busy) {
                onClose()
            }
        },
        onTaken = {
            if (canAct) {
                confirmation = DailyDoseAction.TAKEN
            }
        },
        onSkip = {
            if (canAct) {
                confirmation = DailyDoseAction.SKIPPED
            }
        },
        onSnooze = {
            submit(DailyDoseAction.SNOOZE)
        },
        onPlayInstructions = playInstructions
    )

    confirmation?.let { action ->
        val taken = action == DailyDoseAction.TAKEN

        AlertDialog(
            onDismissRequest = {
                confirmation = null
            },
            title = {
                Text(
                    if (taken) {
                        "Xác nhận đã uống?"
                    } else {
                        "Bỏ qua liều này?"
                    }
                )
            },
            text = {
                Text(
                    if (taken) {
                        "Bạn đã uống ${ui.quantityText} " +
                                "${ui.medicineName} cho liều ${ui.time}?"
                    } else {
                        "Liều ${ui.medicineName} lúc ${ui.time} " +
                                "sẽ được đánh dấu bỏ qua."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmation = null
                        submit(action)
                    }
                ) {
                    Text("Xác nhận")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmation = null
                    }
                ) {
                    Text("Hủy")
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
                Text("Kiểm tra kết quả")
            },
            text = {
                Text(message)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        errorMessage = null
                        onClose()
                    }
                ) {
                    Text("Đóng và tải lại")
                }
            }
        )
    }

    successMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {
                onClose()
            },
            title = {
                Text("Đã lưu")
            },
            text = {
                Text(message)
            },
            confirmButton = {
                TextButton(onClick = onClose) {
                    Text("Quay lại lịch uống")
                }
            }
        )
    }
}

@Composable
private fun DailyDoseListSkeleton() {
    val transition = rememberInfiniteTransition(
        label = "dailyDoseLoading"
    )

    val offset by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "dailyDoseHighlight"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFE1E7E5),
            Color(0xFFF2F5F4),
            Color(0xFFE1E7E5)
        ),
        start = Offset(offset, 0f),
        end = Offset(offset + 480f, 240f)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = "Đang tải các liều thuốc"
                progressBarRangeInfo =
                    ProgressBarRangeInfo.Indeterminate
            },
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(22.dp)
                .background(brush, RoundedCornerShape(8.dp))
        )

        repeat(3) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(
                    1.dp,
                    Color(0xFFD7E2DF)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                brush,
                                RoundedCornerShape(12.dp)
                            )
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(18.dp)
                                .background(
                                    brush,
                                    RoundedCornerShape(6.dp)
                                )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(14.dp)
                                .background(
                                    brush,
                                    RoundedCornerShape(6.dp)
                                )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(12.dp)
                                .background(
                                    brush,
                                    RoundedCornerShape(6.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

private fun ensureDailyDoseOwner(ownerUid: String) {
    if (FirebaseProvider.auth.currentUser?.uid != ownerUid) {
        throw CancellationException("Tài khoản đã thay đổi.")
    }
}

private fun scheduleDayMillis(millis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun scheduleDateKey(
    millis: Long,
    timeZone: TimeZone = TimeZone.getDefault()
): String {
    return SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.ROOT
    ).apply {
        this.timeZone = timeZone
    }.format(Date(millis))
}

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun ScheduleScreenPreview() {
    MMRATheme {
        ScheduleScreen()
    }
}