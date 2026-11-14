package com.example.project_lttbd.ui.screens.history

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.project_lttbd.data.model.UserProfileManager
import com.example.project_lttbd.data.remote.DoseException
import com.example.project_lttbd.data.remote.DoseRepository
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.data.remote.dto.DoseResponseDto
import com.example.project_lttbd.data.remote.dto.DoseStatusDto
import com.example.project_lttbd.notifications.sync.MedicationReminderSync
import com.example.project_lttbd.ui.components.MedicineListSkeleton
import com.example.project_lttbd.ui.components.MMRATextField
import com.example.project_lttbd.ui.components.rememberUiNow
import com.example.project_lttbd.ui.screens.schedule.rememberScheduleOwnerUid
import com.example.project_lttbd.ui.screens.schedule.toScheduleDoseUiModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

private val historyGreen = Color(0xFF126B62)
private val historyMuted = Color(0xFF526565)

internal enum class HistoryStatus(
    val label: String,
    val color: Color,
    val background: Color
) {
    ON_TIME("Đúng giờ", Color(0xFF287A4D), Color(0xFFE3F4E9)),
    EARLY("Đã uống sớm", Color(0xFF287A4D), Color(0xFFE3F4E9)),
    LATE("Đã uống muộn", Color(0xFFAA5B00), Color(0xFFFFF1D6)),
    TAKEN("Đã uống", Color(0xFF287A4D), Color(0xFFE3F4E9)),
    MISSED("Bỏ lỡ", Color(0xFFBA3028), Color(0xFFFBE6E3)),
    SKIPPED("Đã bỏ qua", Color(0xFFBA3028), Color(0xFFFBE6E3)),
    PENDING("Chưa đến giờ", historyMuted, Color(0xFFEEF5F3)),
    UNCONFIRMED("Chờ xác nhận", Color(0xFFAA5B00), Color(0xFFFFF1D6)),
    SNOOZED("Nhắc lại", Color(0xFFAA5B00), Color(0xFFFFF1D6))
}

internal fun historyStatus(
    dose: DoseResponseDto,
    now: Long
): HistoryStatus {
    return when (dose.status) {
        DoseStatusDto.TAKEN -> {
            val actual = dose.actionAt?.let {
                MedicationReminderSync.parseIsoToMillis(it)
            } ?: 0L

            val scheduled = MedicationReminderSync.parseIsoToMillis(
                dose.scheduledAt
            )

            if (actual <= 0L || scheduled <= 0L) {
                HistoryStatus.TAKEN
            } else {
                val difference = actual - scheduled

                when {
                    abs(difference) <= 600_000L -> HistoryStatus.ON_TIME
                    difference < 0L -> HistoryStatus.EARLY
                    else -> HistoryStatus.LATE
                }
            }
        }

        DoseStatusDto.SKIPPED -> HistoryStatus.SKIPPED
        DoseStatusDto.MISSED -> HistoryStatus.MISSED
        DoseStatusDto.CANCELLED -> HistoryStatus.PENDING

        DoseStatusDto.PENDING -> when {
            !dose.nextReminderAt.isNullOrBlank() ->
                HistoryStatus.SNOOZED

            MedicationReminderSync.parseIsoToMillis(
                dose.scheduledAt
            ) <= now ->
                HistoryStatus.UNCONFIRMED

            else -> HistoryStatus.PENDING
        }
    }
}

private data class HistoryLoad(
    val loading: Boolean = true,
    val hasLoaded: Boolean = false,
    val doses: List<DoseResponseDto> = emptyList(),
    val error: String? = null
)

internal data class HistorySummary(
    val taken: Int,
    val due: Int,
    val onTime: Int,
    val untimedTaken: Int,
    val percent: Int?
)

internal fun historySummary(
    doses: List<DoseResponseDto>,
    now: Long
): HistorySummary {
    val active = doses
        .filter { it.status != DoseStatusDto.CANCELLED }
        .distinctBy { it.doseId }

    val due = active.filter {
        it.status != DoseStatusDto.PENDING ||
                MedicationReminderSync.parseIsoToMillis(
                    it.scheduledAt
                ) <= now
    }

    val onTime = due.count {
        historyStatus(it, now) == HistoryStatus.ON_TIME
    }

    val untimedTaken = due.count {
        historyStatus(it, now) == HistoryStatus.TAKEN
    }

    return HistorySummary(
        taken = active.count {
            it.status == DoseStatusDto.TAKEN
        },
        due = due.size,
        onTime = onTime,
        untimedTaken = untimedTaken,
        percent = if (due.isEmpty() || untimedTaken > 0) {
            null
        } else {
            (onTime * 100.0 / due.size).roundToInt()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    onDoseClick: (String) -> Unit = {},
    onAccountClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val preview = LocalInspectionMode.current
    val ownerUid = if (preview) "preview" else rememberScheduleOwnerUid()
    val lifecycleOwner = LocalLifecycleOwner.current
    val now = rememberUiNow()
    val today = historyDateKey(now)

    val repository = remember(preview) {
        if (preview) null else DoseRepository()
    }

    val preferences = remember(context) {
        context.getSharedPreferences(
            "mmra_history_notes",
            Context.MODE_PRIVATE
        )
    }

    var selected by rememberSaveable(ownerUid) {
        mutableStateOf(today)
    }
    var month by rememberSaveable(ownerUid) {
        mutableStateOf(selected.take(7))
    }
    var filter by rememberSaveable(ownerUid) {
        mutableStateOf("Tất cả")
    }
    var showFilters by remember(ownerUid) {
        mutableStateOf(false)
    }
    var showStatsInfo by remember(ownerUid) {
        mutableStateOf(false)
    }
    var showNote by remember(ownerUid, selected) {
        mutableStateOf(false)
    }
    var noteDraft by remember(ownerUid, selected) {
        mutableStateOf("")
    }
    var revision by remember(ownerUid) {
        mutableStateOf(0)
    }
    var loaded by remember(ownerUid, month) {
        mutableStateOf(
            HistoryLoad(
                loading = !preview && ownerUid != null,
                hasLoaded = preview
            )
        )
    }

    val noteKey = "${ownerUid?.length ?: 0}:$ownerUid:$selected"

    var note by remember(ownerUid, selected) {
        mutableStateOf(
            if (ownerUid == null || preview) {
                ""
            } else {
                preferences.getString(noteKey, "").orEmpty()
            }
        )
    }

    DisposableEffect(lifecycleOwner, ownerUid, preview) {
        var paused = false

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> paused = true

                Lifecycle.Event.ON_RESUME -> if (paused) {
                    paused = false
                    revision++
                }

                else -> Unit
            }
        }

        if (!preview) {
            lifecycleOwner.lifecycle.addObserver(observer)
        }

        onDispose {
            if (!preview) {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    LaunchedEffect(ownerUid, month, revision) {
        if (preview || ownerUid == null || repository == null) {
            return@LaunchedEffect
        }

        loaded = loaded.copy(
            loading = true,
            error = null
        )

        try {
            ensureHistoryOwner(ownerUid)

            val result = repository.listDoseHistory(month)

            currentCoroutineContext().ensureActive()
            ensureHistoryOwner(ownerUid)

            loaded = HistoryLoad(
                loading = false,
                hasLoaded = true,
                doses = result
                    .filter {
                        it.localDate.take(7) == month &&
                                it.status != DoseStatusDto.CANCELLED
                    }
                    .distinctBy { it.doseId }
                    .sortedWith(
                        compareBy<DoseResponseDto> {
                            it.localDate
                        }.thenBy {
                            it.minuteOfDay
                        }.thenBy {
                            it.doseId
                        }
                    )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            currentCoroutineContext().ensureActive()
            ensureHistoryOwner(ownerUid)

            loaded = loaded.copy(
                loading = false,
                error = (error as? DoseException)?.message
                    ?: "Chưa tải được lịch sử. Vui lòng thử lại."
            )
        }
    }

    val refresh: () -> Unit = {
        if (!preview && ownerUid != null && !loaded.loading) {
            loaded = loaded.copy(
                loading = true,
                error = null
            )
            revision++
        }
    }

    val grouped = remember(loaded.doses) {
        loaded.doses.groupBy { it.localDate }
    }
    val summary = remember(loaded.doses, now) {
        historySummary(loaded.doses, now)
    }
    val streak = remember(loaded.doses, month, now) {
        historyStreak(loaded.doses, month, now)
    }

    val selectedDoses = grouped[selected].orEmpty()

    val visible = selectedDoses.filter { dose ->
        val status = historyStatus(dose, now)

        when (filter) {
            "Tất cả" -> true
            "Đã uống" -> dose.status == DoseStatusDto.TAKEN
            else -> status.label == filter
        }
    }

    PullToRefreshBox(
        isRefreshing = loaded.loading && loaded.hasLoaded,
        onRefresh = refresh,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAF9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HistoryHeader(
                preview = preview,
                onFilterClick = { showFilters = true },
                onAccountClick = onAccountClick
            )

            when {
                ownerUid == null -> {
                    Text(
                        "Vui lòng đăng nhập để xem lịch sử.",
                        color = historyMuted
                    )
                    TextButton(onClick = onAccountClick) {
                        Text("Mở tài khoản")
                    }
                }

                loaded.loading && !loaded.hasLoaded -> {
                    MedicineListSkeleton()
                }

                loaded.error != null && !loaded.hasLoaded -> {
                    Text(
                        loaded.error.orEmpty(),
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = refresh) {
                        Text("Thử lại")
                    }
                }

                else -> {
                    loaded.error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Row(
                        Modifier.height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = { showStatsInfo = true },
                            modifier = Modifier
                                .weight(2f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF0A4D47)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    summary.percent?.let { "$it%" } ?: "—",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    if (month == today.take(7)) {
                                        "Đúng lịch tháng này"
                                    } else {
                                        "Đúng lịch tháng đã chọn"
                                    },
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    if (summary.untimedTaken > 0) {
                                        "Chưa đủ giờ xác nhận"
                                    } else {
                                        "${summary.onTime}/${summary.due} liều đã đến giờ"
                                    },
                                    fontSize = 11.sp,
                                    color = Color(0xFFD9EEEA)
                                )
                            }
                        }

                        Surface(
                            onClick = { showStatsInfo = true },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFFFFF1D6)
                        ) {
                            Column(
                                Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                HistoryFlameIcon(Modifier.size(18.dp))
                                Text(
                                    "$streak ngày",
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFAA5B00)
                                )
                                Text(
                                    "Chuỗi đúng lịch",
                                    fontSize = 12.sp,
                                    color = historyMuted
                                )
                            }
                        }
                    }

                    HistoryCalendar(
                        month = month,
                        selected = selected,
                        today = today,
                        doses = grouped,
                        now = now,
                        onMonthChange = { offset ->
                            month = historyShiftMonth(month, offset)
                            selected = if (month == today.take(7)) {
                                today
                            } else {
                                "$month-01"
                            }
                        },
                        onDateSelected = {
                            selected = it
                            month = it.take(7)
                        }
                    )

                    HistoryNoteCard(
                        note = note,
                        doses = selectedDoses,
                        onEdit = {
                            noteDraft = note
                            showNote = true
                        }
                    )

                    Text(
                        "${historyDisplayDate(selected)} • ${selectedDoses.size} liều",
                        fontWeight = FontWeight.Bold
                    )

                    if (filter != "Tất cả") {
                        Text(
                            "Bộ lọc: $filter",
                            color = historyMuted,
                            fontSize = 12.sp
                        )
                    }

                    if (visible.isEmpty()) {
                        Text(
                            if (selectedDoses.isEmpty()) {
                                "Chưa có liều được ghi nhận cho ngày này."
                            } else {
                                "Không có liều phù hợp bộ lọc."
                            },
                            color = historyMuted
                        )
                    }

                    visible.forEach { dose ->
                        key(dose.doseId) {
                            HistoryDoseCard(dose, now, onDoseClick)
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        AlertDialog(
            onDismissRequest = { showFilters = false },
            title = { Text("Lọc lịch sử") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState())
                ) {
                    val options = listOf(
                        "Tất cả",
                        "Đã uống"
                    ) + HistoryStatus.entries
                        .filter { it != HistoryStatus.TAKEN }
                        .map { it.label }

                    options.forEach { value ->
                        TextButton(
                            onClick = {
                                filter = value
                                showFilters = false
                            }
                        ) {
                            Text(
                                if (filter == value) "✓ $value" else value
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilters = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    if (showStatsInfo) {
        AlertDialog(
            onDismissRequest = { showStatsInfo = false },
            title = { Text("Cách tính thống kê") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Thống kê dựa trên các liều đã lưu trong tháng đang xem."
                    )
                    Text(
                        "Đúng giờ: giờ xác nhận đã uống chênh lệch tối đa " +
                                "10 phút so với lịch."
                    )
                    Text(
                        "Liều chưa đến giờ được loại khỏi tỷ lệ. " +
                                "Nếu thiếu giờ xác nhận, tỷ lệ hiển thị dấu —."
                    )
                    Text(
                        "Chuỗi đúng lịch tính các ngày liên tiếp có toàn bộ " +
                                "liều đã lưu được xác nhận đúng giờ, " +
                                "trong tháng đang xem."
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatsInfo = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    if (showNote && ownerUid != null) {
        AlertDialog(
            onDismissRequest = { showNote = false },
            title = {
                Text("Ghi chú ngày ${historyDisplayDate(selected)}")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MMRATextField(
                        value = noteDraft,
                        onValueChange = {
                            noteDraft = it.take(2000)
                        },
                        label = "Ghi chú của bạn",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        singleLine = false,
                        minLines = 3
                    )
                    Text(
                        "Ghi chú được lưu trên thiết bị này theo tài khoản.",
                        fontSize = 12.sp,
                        color = historyMuted
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (
                            !preview &&
                            FirebaseProvider.auth.currentUser?.uid == ownerUid
                        ) {
                            note = noteDraft.trim()

                            preferences.edit().apply {
                                if (note.isEmpty()) {
                                    remove(noteKey)
                                } else {
                                    putString(noteKey, note)
                                }
                            }.apply()
                        }

                        showNote = false
                    }
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNote = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun HistoryHeader(
    preview: Boolean,
    onFilterClick: () -> Unit,
    onAccountClick: () -> Unit
) {
    val avatar = if (preview) {
        null
    } else {
        UserProfileManager.currentAvatarBitmap
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Lịch sử dùng thuốc",
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF102A2A)
            )
            Text(
                "Theo dõi tiến độ của bạn",
                color = historyMuted,
                fontSize = 12.sp
            )
        }

        IconButton(
            onClick = onFilterClick,
            modifier = Modifier
                .size(44.dp)
                .semantics {
                    contentDescription = "Lọc lịch sử"
                }
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .background(
                        Color(0xFFEEF5F3),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(22.dp)) {
                    val stroke = 1.7.dp.toPx()

                    repeat(3) { index ->
                        val y = size.height *
                                (0.25f + index * 0.25f)
                        val knob = size.width *
                                if (index == 1) 0.68f else 0.32f

                        drawLine(
                            color = historyGreen,
                            start = Offset(size.width * 0.12f, y),
                            end = Offset(size.width * 0.88f, y),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )

                        drawLine(
                            color = historyGreen,
                            start = Offset(
                                knob,
                                y - size.height * 0.09f
                            ),
                            end = Offset(
                                knob,
                                y + size.height * 0.09f
                            ),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        Surface(
            onClick = onAccountClick,
            color = Color.Transparent,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                Modifier.width(52.dp).heightIn(min = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    3.dp,
                    Alignment.CenterVertically
                )
            ) {
                Surface(
                    Modifier.size(36.dp),
                    shape = CircleShape,
                    color = Color(0xFFD9EEEA)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatar != null) {
                            Image(
                                avatar.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                if (preview) {
                                    "AB"
                                } else {
                                    UserProfileManager.getInitials()
                                },
                                color = historyGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                Text(
                    "Tài khoản",
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    color = historyGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryCalendar(
    month: String,
    selected: String,
    today: String,
    doses: Map<String, List<DoseResponseDto>>,
    now: Long,
    onMonthChange: (Int) -> Unit,
    onDateSelected: (String) -> Unit
) {
    val calendar = historyMonthCalendar(month)

    val dates = remember(month) {
        historyMonthDates(month)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFD7E2DF))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onMonthChange(-1) },
                    modifier = Modifier
                        .size(36.dp)
                        .semantics {
                            contentDescription = "Tháng trước"
                        }
                ) {
                    Text(
                        "‹",
                        fontSize = 24.sp,
                        color = historyGreen
                    )
                }

                Text(
                    "Tháng ${calendar.get(Calendar.MONTH) + 1}, " +
                            "${calendar.get(Calendar.YEAR)}",
                    Modifier.weight(1f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { onMonthChange(1) },
                    modifier = Modifier
                        .size(36.dp)
                        .semantics {
                            contentDescription = "Tháng sau"
                        }
                ) {
                    Text(
                        "›",
                        fontSize = 24.sp,
                        color = historyGreen
                    )
                }
            }

            Row {
                listOf(
                    "T2", "T3", "T4", "T5", "T6", "T7", "CN"
                ).forEach {
                    Box(
                        Modifier.weight(1f).height(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            it,
                            fontSize = 10.sp,
                            color = historyMuted
                        )
                    }
                }
            }

            CompositionLocalProvider(
                LocalMinimumInteractiveComponentSize provides 0.dp
            ) {
                dates.chunked(7).forEach { week ->
                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(3.dp)
                    ) {
                        week.forEach { date ->
                            val inMonth = date.take(7) == month

                            val mood = if (inMonth) {
                                historyDayMood(
                                    doses[date].orEmpty(),
                                    now
                                )
                            } else {
                                null
                            }

                            Surface(
                                onClick = {
                                    onDateSelected(date)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .semantics {
                                        contentDescription =
                                            historyDisplayDate(date) +
                                                    (mood?.let {
                                                        ", ${it.label}"
                                                    } ?: "")
                                    },
                                shape = RoundedCornerShape(7.dp),
                                color = mood?.background
                                    ?: if (inMonth) {
                                        Color.White
                                    } else {
                                        Color(0xFFF4F7F6)
                                    },
                                border = when (date) {
                                    selected -> BorderStroke(
                                        1.5.dp,
                                        historyGreen
                                    )
                                    today -> BorderStroke(
                                        1.dp,
                                        Color(0xFFD7E2DF)
                                    )
                                    else -> null
                                }
                            ) {
                                Column(
                                    horizontalAlignment =
                                        Alignment.CenterHorizontally,
                                    verticalArrangement =
                                        Arrangement.Center
                                ) {
                                    Text(
                                        date.takeLast(2)
                                            .toInt()
                                            .toString(),
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp,
                                        color = if (inMonth) {
                                            Color(0xFF102A2A)
                                        } else {
                                            historyMuted
                                        }
                                    )

                                    Text(
                                        when (mood) {
                                            HistoryStatus.ON_TIME,
                                            HistoryStatus.EARLY,
                                            HistoryStatus.TAKEN -> "✓"

                                            HistoryStatus.MISSED,
                                            HistoryStatus.SKIPPED -> "×"

                                            HistoryStatus.LATE,
                                            HistoryStatus.SNOOZED,
                                            HistoryStatus.UNCONFIRMED -> "◷"

                                            else -> "–"
                                        },
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp,
                                        color = mood?.color ?: historyMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HistoryLegend("✓ Đã uống", HistoryStatus.ON_TIME)
                HistoryLegend("◷ Muộn / chờ", HistoryStatus.LATE)
                HistoryLegend("× Bỏ qua / lỡ", HistoryStatus.MISSED)
            }
        }
    }
}

@Composable
private fun HistoryLegend(
    label: String,
    status: HistoryStatus
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = status.background
    ) {
        Text(
            label,
            Modifier.padding(
                horizontal = 6.dp,
                vertical = 4.dp
            ),
            fontSize = 10.sp,
            lineHeight = 12.sp,
            color = status.color
        )
    }
}

@Composable
private fun HistoryNoteCard(
    note: String,
    doses: List<DoseResponseDto>,
    onEdit: () -> Unit
) {
    val missed = doses.count {
        it.status == DoseStatusDto.MISSED
    }
    val skipped = doses.count {
        it.status == DoseStatusDto.SKIPPED
    }

    val title = when {
        missed > 0 && skipped > 0 ->
            "$missed liều bỏ lỡ • $skipped liều bỏ qua"

        missed > 0 -> "$missed liều bị bỏ lỡ"
        skipped > 0 -> "$skipped liều đã bỏ qua"
        else -> "Ghi chú trong ngày"
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Vì sao có liều bị lỡ?",
                Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF102A2A)
            )
            TextButton(onClick = onEdit) {
                Text(
                    "Chỉnh sửa",
                    fontSize = 12.sp,
                    color = historyGreen
                )
            }
        }

        Surface(
            onClick = onEdit,
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFD7E2DF))
        ) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    Modifier.size(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFF1D6)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(Modifier.size(22.dp)) {
                            val moon = Path().apply {
                                moveTo(
                                    size.width * 0.65f,
                                    size.height * 0.18f
                                )
                                cubicTo(
                                    size.width * 0.29f,
                                    size.height * 0.13f,
                                    size.width * 0.14f,
                                    size.height * 0.5f,
                                    size.width * 0.34f,
                                    size.height * 0.76f
                                )
                                cubicTo(
                                    size.width * 0.5f,
                                    size.height * 0.98f,
                                    size.width * 0.87f,
                                    size.height * 0.82f,
                                    size.width * 0.89f,
                                    size.height * 0.55f
                                )
                                cubicTo(
                                    size.width * 0.7f,
                                    size.height * 0.72f,
                                    size.width * 0.41f,
                                    size.height * 0.49f,
                                    size.width * 0.65f,
                                    size.height * 0.18f
                                )
                                close()
                            }

                            drawPath(
                                moon,
                                Color(0xFFAA5B00),
                                style = Stroke(1.5.dp.toPx())
                            )
                        }
                    }
                }

                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF102A2A)
                    )
                    Text(
                        note.ifBlank {
                            "Chạm Chỉnh sửa để thêm ghi chú cho ngày này."
                        },
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = historyMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

internal fun historyMonthDates(
    month: String
): List<String> {
    val calendar = historyMonthCalendar(month)
    val offset = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7

    val count = (
            (offset + calendar.getActualMaximum(
                Calendar.DAY_OF_MONTH
            ) + 6) / 7
            ) * 7

    calendar.add(Calendar.DAY_OF_MONTH, -offset)

    return List(count) {
        historyDateKey(calendar.timeInMillis).also {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }
}

internal fun historyDayMood(
    doses: List<DoseResponseDto>,
    now: Long
): HistoryStatus? {
    val statuses = doses
        .filter { it.status != DoseStatusDto.CANCELLED }
        .map { historyStatus(it, now) }

    return listOf(
        HistoryStatus.MISSED,
        HistoryStatus.SKIPPED,
        HistoryStatus.UNCONFIRMED,
        HistoryStatus.LATE,
        HistoryStatus.SNOOZED,
        HistoryStatus.EARLY,
        HistoryStatus.TAKEN,
        HistoryStatus.ON_TIME,
        HistoryStatus.PENDING
    ).firstOrNull {
        it in statuses
    }
}

private data class HistoryCardColors(
    val background: Color,
    val border: Color,
    val accent: Color,
    val timeBackground: Color,
    val symbol: String
)

private fun historyCardColors(
    dose: DoseResponseDto,
    status: HistoryStatus
): HistoryCardColors = when {
    dose.status == DoseStatusDto.TAKEN -> HistoryCardColors(
        background = Color(0xFFEAF6EE),
        border = Color(0xFFAACDB7),
        accent = Color(0xFF287A4D),
        timeBackground = Color(0xFFD6EBDE),
        symbol = "✓"
    )

    dose.status == DoseStatusDto.MISSED ||
            dose.status == DoseStatusDto.SKIPPED ->
        HistoryCardColors(
            background = Color(0xFFFFF0ED),
            border = Color(0xFFE3B8B1),
            accent = Color(0xFFBA3028),
            timeBackground = Color(0xFFF8DBD6),
            symbol = "×"
        )

    status == HistoryStatus.UNCONFIRMED ||
            status == HistoryStatus.SNOOZED ->
        HistoryCardColors(
            background = Color(0xFFFFF7E2),
            border = Color(0xFFE6C981),
            accent = Color(0xFF936000),
            timeBackground = Color(0xFFFFE8B2),
            symbol = "◷"
        )

    else -> HistoryCardColors(
        background = Color.White,
        border = Color(0xFFD7E2DF),
        accent = historyMuted,
        timeBackground = Color(0xFFEEF4F2),
        symbol = "◷"
    )
}

@Composable
private fun HistoryDoseCard(
    dose: DoseResponseDto,
    now: Long,
    onClick: (String) -> Unit
) {
    val ui = dose.toScheduleDoseUiModel()
    val status = historyStatus(dose, now)
    val colors = historyCardColors(dose, status)

    Surface(
        onClick = { onClick(dose.doseId) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = colors.background,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.timeBackground
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 64.dp)
                        .padding(
                            horizontal = 10.dp,
                            vertical = 10.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Canvas(Modifier.size(18.dp)) {
                        val stroke = 1.8.dp.toPx()

                        when (colors.symbol) {
                            "✓" -> {
                                drawLine(
                                    color = colors.accent,
                                    start = Offset(size.width * 0.2f, size.height * 0.52f),
                                    end = Offset(size.width * 0.42f, size.height * 0.73f),
                                    strokeWidth = stroke,
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = colors.accent,
                                    start = Offset(size.width * 0.42f, size.height * 0.73f),
                                    end = Offset(size.width * 0.8f, size.height * 0.28f),
                                    strokeWidth = stroke,
                                    cap = StrokeCap.Round
                                )
                            }

                            "×" -> {
                                drawLine(
                                    color = colors.accent,
                                    start = Offset(size.width * 0.25f, size.height * 0.25f),
                                    end = Offset(size.width * 0.75f, size.height * 0.75f),
                                    strokeWidth = stroke,
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = colors.accent,
                                    start = Offset(size.width * 0.75f, size.height * 0.25f),
                                    end = Offset(size.width * 0.25f, size.height * 0.75f),
                                    strokeWidth = stroke,
                                    cap = StrokeCap.Round
                                )
                            }

                            else -> {
                                drawCircle(
                                    color = colors.accent,
                                    radius = size.minDimension * 0.34f,
                                    style = Stroke(width = stroke)
                                )
                                drawLine(
                                    color = colors.accent,
                                    start = center,
                                    end = Offset(size.width * 0.5f, size.height * 0.28f),
                                    strokeWidth = stroke,
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = colors.accent,
                                    start = center,
                                    end = Offset(size.width * 0.68f, size.height * 0.54f),
                                    strokeWidth = stroke,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                    Text(
                        ui.time,
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    ui.medicineName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF102A2A)
                )
                Text(
                    listOf(
                        ui.quantityText,
                        ui.instruction
                    ).filter {
                        it.isNotBlank()
                    }.joinToString(" • "),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = historyMuted
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = status.background
                ) {
                    Text(
                        status.label,
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        ),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = status.color
                    )
                }

                dose.actionAt?.takeIf {
                    dose.status == DoseStatusDto.TAKEN
                }?.let {
                    val timestamp =
                        MedicationReminderSync.parseIsoToMillis(it)

                    if (timestamp > 0L) {
                        val time = SimpleDateFormat(
                            "HH:mm",
                            Locale.ROOT
                        ).apply {
                            timeZone = TimeZone.getTimeZone(
                                dose.timeZone
                            )
                        }.format(Date(timestamp))

                        Text(
                            "Xác nhận lúc $time",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = historyMuted
                        )
                    }
                }
            }

            Canvas(Modifier.size(14.dp)) {
                val stroke = 1.7.dp.toPx()

                drawLine(
                    colors.accent,
                    Offset(
                        size.width * 0.35f,
                        size.height * 0.2f
                    ),
                    Offset(
                        size.width * 0.65f,
                        size.height * 0.5f
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    colors.accent,
                    Offset(
                        size.width * 0.65f,
                        size.height * 0.5f
                    ),
                    Offset(
                        size.width * 0.35f,
                        size.height * 0.8f
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

internal fun historyStreak(
    doses: List<DoseResponseDto>,
    month: String,
    now: Long
): Int {
    val today = historyDateKey(now)

    if (month > today.take(7)) return 0

    val grouped = doses
        .filter { it.status != DoseStatusDto.CANCELLED }
        .groupBy { it.localDate }

    fun completed(date: String): Boolean {
        val day = grouped[date].orEmpty()
        return day.isNotEmpty() && day.all {
            historyStatus(it, now) == HistoryStatus.ON_TIME
        }
    }

    val cursor = historyMonthCalendar(month).apply {
        set(
            Calendar.DAY_OF_MONTH,
            if (month == today.take(7)) {
                today.takeLast(2).toInt()
            } else {
                getActualMaximum(Calendar.DAY_OF_MONTH)
            }
        )
    }

    if (month == today.take(7) && !completed(today)) {
        cursor.add(Calendar.DAY_OF_MONTH, -1)
    }

    var streak = 0

    while (historyDateKey(cursor.timeInMillis).take(7) == month) {
        if (!completed(historyDateKey(cursor.timeInMillis))) {
            break
        }
        streak++
        cursor.add(Calendar.DAY_OF_MONTH, -1)
    }

    return streak
}

private fun ensureHistoryOwner(uid: String) {
    if (FirebaseProvider.auth.currentUser?.uid != uid) {
        throw CancellationException("Tài khoản đã thay đổi.")
    }
}

internal fun historyDateKey(
    millis: Long
): String = SimpleDateFormat(
    "yyyy-MM-dd",
    Locale.ROOT
).apply {
    timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
}.format(Date(millis))

private fun historyMonthCalendar(
    month: String
): Calendar = Calendar.getInstance(
    TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
).apply {
    clear()
    set(
        month.take(4).toInt(),
        month.takeLast(2).toInt() - 1,
        1,
        12,
        0,
        0
    )
}

internal fun historyShiftMonth(
    month: String,
    offset: Int
): String {
    val calendar = historyMonthCalendar(month).apply {
        add(Calendar.MONTH, offset)
    }

    return SimpleDateFormat(
        "yyyy-MM",
        Locale.ROOT
    ).apply {
        timeZone = calendar.timeZone
    }.format(calendar.time)
}

private fun historyDisplayDate(
    date: String
): String =
    "${date.takeLast(2)}/${date.substring(5, 7)}/${date.take(4)}"

@Composable
private fun HistoryFlameIcon(
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val flame = Path().apply {
            moveTo(
                size.width * 0.55f,
                size.height * 0.06f
            )
            cubicTo(
                size.width * 0.7f, size.height * 0.33f,
                size.width * 0.86f, size.height * 0.4f,
                size.width * 0.8f, size.height * 0.68f
            )
            cubicTo(
                size.width * 0.73f, size.height * 0.97f,
                size.width * 0.25f, size.height * 0.99f,
                size.width * 0.19f, size.height * 0.69f
            )
            cubicTo(
                size.width * 0.13f, size.height * 0.48f,
                size.width * 0.26f, size.height * 0.4f,
                size.width * 0.34f, size.height * 0.31f
            )
            cubicTo(
                size.width * 0.32f, size.height * 0.53f,
                size.width * 0.5f, size.height * 0.55f,
                size.width * 0.55f, size.height * 0.06f
            )
            close()
        }

        drawPath(
            flame,
            Color(0xFFAA5B00),
            style = Stroke(1.5.dp.toPx())
        )
    }
}