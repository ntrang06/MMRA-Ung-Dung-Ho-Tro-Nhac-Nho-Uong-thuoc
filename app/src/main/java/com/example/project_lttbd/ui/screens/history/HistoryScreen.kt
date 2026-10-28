package com.example.project_lttbd.ui.screens.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.data.model.UserProfileManager
import com.example.project_lttbd.data.mock.*
import com.example.project_lttbd.ui.components.MMRATextField
import com.example.project_lttbd.ui.components.rememberUiNow
import com.example.project_lttbd.ui.screens.reminder.ReminderDoseUiModel
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

private val historyGreen = Color(0xFF126B62)
private val historyMuted = Color(0xFF526565)

internal enum class HistoryStatus(
    val label: String,
    val color: Color,
    val background: Color
) {
    ON_TIME("Đúng giờ", Color(0xFF287A4D), Color(0xFFE3F4E9)),
    EARLY("Đã uống sớm", Color(0xFF2E6FAD), Color(0xFFEDF5FF)),
    LATE("Muộn", Color(0xFFAA5B00), Color(0xFFFFF1D6)),
    MISSED("Bỏ lỡ", Color(0xFFBA3028), Color(0xFFFBE6E3)),
    SKIPPED("Đã bỏ qua", Color(0xFFBA3028), Color(0xFFFBE6E3)),
    PENDING("Chưa đến giờ", historyMuted, Color(0xFFEEF5F3)),
    SNOOZED("Nhắc lại", Color(0xFFAA5B00), Color(0xFFFFF1D6))
}

internal fun historyStatus(
    dose: ReminderDoseUiModel,
    now: Long
): HistoryStatus {
    val record = MockDoseActions.get(dose.doseId)

    return when (record?.action) {
        MockDoseAction.SKIPPED -> HistoryStatus.SKIPPED

        MockDoseAction.SNOOZED ->
            if (record.recordedAtMillis + 600_000L > now) {
                HistoryStatus.SNOOZED
            } else {
                HistoryStatus.MISSED
            }

        MockDoseAction.TAKEN -> {
            // Quy ước thử FE: trong khoảng ±10 phút là đúng giờ.
            val difference =
                record.recordedAtMillis - dose.scheduledAtMillis

            when {
                abs(difference) <= 600_000L -> HistoryStatus.ON_TIME
                difference < 0 -> HistoryStatus.EARLY
                else -> HistoryStatus.LATE
            }
        }

        null ->
            if (dose.scheduledAtMillis + 600_000L < now) {
                HistoryStatus.MISSED
            } else {
                HistoryStatus.PENDING
            }
    }
}

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    onDoseClick: (String) -> Unit = {},
    onAccountClick: () -> Unit = {}
) {
    var selected by rememberSaveable {
        mutableStateOf(MockCalendar.today())
    }
    var month by rememberSaveable {
        mutableStateOf(
            Calendar.getInstance().apply {
                timeInMillis = selected
                set(Calendar.DAY_OF_MONTH, 1)
            }.timeInMillis
        )
    }
    var filter by rememberSaveable { mutableStateOf("Tất cả") }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showNote by rememberSaveable { mutableStateOf(false) }
    var note by rememberSaveable { mutableStateOf("") }

    val now = rememberUiNow()
    val cal = Calendar.getInstance().apply {
        timeInMillis = month
    }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val first = MockCalendar.day(month)
    val dayOffset = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7

    val monthDoses = (0 until daysInMonth).flatMap {
        MockReminderDoses.forDate(MockCalendar.plusDays(first, it))
    }
    val due = monthDoses.filter { it.scheduledAtMillis <= now }
    val onTime = due.count {
        historyStatus(it, now) == HistoryStatus.ON_TIME
    }
    val percent = if (due.isEmpty()) {
        null
    } else {
        (onTime * 100.0 / due.size).roundToInt()
    }

    var streak = 0
    var cursor = MockCalendar.today()

    for (iteration in 0 until 366) {
        val dayDoses = MockReminderDoses.forDate(cursor)

        if (
            dayDoses.isNotEmpty() &&
            dayDoses.all {
                historyStatus(it, now) == HistoryStatus.ON_TIME
            }
        ) {
            streak++
            cursor = MockCalendar.plusDays(cursor, -1)
        } else if (cursor == MockCalendar.today() && streak == 0) {
            cursor = MockCalendar.plusDays(cursor, -1)
        } else {
            break
        }
    }

    val selectedDoses = MockReminderDoses.forDate(selected)

    val visible = selectedDoses.filter { dose ->
        when (filter) {
            "Đúng giờ" ->
                historyStatus(dose, now) == HistoryStatus.ON_TIME
            "Muộn" ->
                historyStatus(dose, now) == HistoryStatus.LATE
            "Bỏ lỡ" ->
                historyStatus(dose, now) in setOf(
                    HistoryStatus.MISSED,
                    HistoryStatus.SKIPPED
                )
            else -> true
        }
    }

    Column(
        modifier.fillMaxSize()
            .background(Color(0xFFF7FAF9))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Lịch sử dùng thuốc",
                    fontSize = 22.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Theo dõi tiến độ của bạn",
                    color = historyMuted,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = { showFilters = true },
                modifier = Modifier.semantics {
                    contentDescription = "Lọc lịch sử"
                }
            ) {
                Canvas(Modifier.size(20.dp)) {
                    for (i in 0..2) {
                        val y = size.height * (0.2f + i * 0.3f)
                        drawLine(
                            historyMuted,
                            Offset(0f, y),
                            Offset(size.width, y),
                            1.4.dp.toPx(),
                            StrokeCap.Round
                        )
                        val x = size.width *
                                if (i == 1) 0.65f else 0.3f
                        drawLine(
                            historyMuted,
                            Offset(x, y - 3.dp.toPx()),
                            Offset(x, y + 3.dp.toPx()),
                            2.dp.toPx(),
                            StrokeCap.Round
                        )
                    }
                }
            }

            Surface(
                onClick = onAccountClick,
                color = Color.Transparent,
                modifier = Modifier.semantics {
                    contentDescription = "Mở tài khoản"
                }
            ) {
                Column(
                    Modifier.widthIn(min = 48.dp).padding(3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFD9EEEA)
                    ) {
                        Box(
                            Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                UserProfileManager.getInitials(),
                                color = historyGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Text(
                        "Tài khoản",
                        fontSize = 8.sp,
                        color = historyMuted
                    )
                }
            }
        }

        Row(
            Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                Modifier.weight(1.5f).fillMaxHeight(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF0A4D47)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        percent?.let { "$it%" } ?: "—",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Đúng lịch tháng này",
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Text(
                        "$onTime/${due.size} liều đã đến giờ",
                        fontSize = 11.sp,
                        color = Color(0xFFD9EEEA)
                    )
                }
            }

            Surface(
                Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFFFF1D6)
            ) {
                Column(Modifier.padding(16.dp)) {
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

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFD7E2DF))
        ) {
            Column(
                Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            month = Calendar.getInstance().apply {
                                timeInMillis = month
                                add(Calendar.MONTH, -1)
                            }.timeInMillis
                        }
                    ) {
                        Text("‹", fontSize = 24.sp)
                    }

                    Text(
                        "Tháng ${cal.get(Calendar.MONTH) + 1}, ${cal.get(Calendar.YEAR)}",
                        Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = {
                            month = Calendar.getInstance().apply {
                                timeInMillis = month
                                add(Calendar.MONTH, 1)
                            }.timeInMillis
                        }
                    ) {
                        Text("›", fontSize = 24.sp)
                    }
                }

                Row {
                    listOf(
                        "T2", "T3", "T4", "T5", "T6", "T7", "CN"
                    ).forEach { label ->
                        Box(
                            Modifier.weight(1f).height(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 11.sp,
                                color = historyMuted
                            )
                        }
                    }
                }

                val rows = (dayOffset + daysInMonth + 6) / 7

                repeat(rows) { row ->
                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(7) { column ->
                            val number =
                                row * 7 + column - dayOffset + 1

                            if (number !in 1..daysInMonth) {
                                Spacer(
                                    Modifier.weight(1f).height(48.dp)
                                )
                            } else {
                                val date = MockCalendar.plusDays(
                                    first,
                                    number - 1
                                )
                                val dayDoses =
                                    MockReminderDoses.forDate(date)

                                val statuses = dayDoses.filter {
                                    it.scheduledAtMillis <= now ||
                                            MockDoseActions.isFinished(it.doseId)
                                }.map {
                                    historyStatus(it, now)
                                }

                                val mood = when {
                                    statuses.any {
                                        it == HistoryStatus.MISSED ||
                                                it == HistoryStatus.SKIPPED
                                    } -> HistoryStatus.MISSED

                                    statuses.any {
                                        it == HistoryStatus.LATE ||
                                                it == HistoryStatus.SNOOZED
                                    } -> HistoryStatus.LATE

                                    statuses.isNotEmpty() &&
                                            statuses.all {
                                                it == HistoryStatus.ON_TIME
                                            } -> HistoryStatus.ON_TIME

                                    statuses.any {
                                        it == HistoryStatus.EARLY
                                    } -> HistoryStatus.EARLY

                                    else -> HistoryStatus.PENDING
                                }

                                Surface(
                                    onClick = { selected = date },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (statuses.isEmpty()) {
                                        Color.White
                                    } else {
                                        mood.background
                                    },
                                    border = if (date == selected) {
                                        BorderStroke(
                                            1.5.dp,
                                            historyGreen
                                        )
                                    } else null
                                ) {
                                    Column(
                                        horizontalAlignment =
                                            Alignment.CenterHorizontally,
                                        verticalArrangement =
                                            Arrangement.Center
                                    ) {
                                        Text("$number", fontSize = 12.sp)
                                        Text(
                                            if (statuses.isEmpty()) {
                                                "·"
                                            } else {
                                                when (mood) {
                                                    HistoryStatus.ON_TIME,
                                                    HistoryStatus.EARLY -> "✓"
                                                    HistoryStatus.MISSED -> "×"
                                                    HistoryStatus.LATE -> "◷"
                                                    else -> "·"
                                                }
                                            },
                                            fontSize = 11.sp,
                                            color = mood.color
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        HistoryStatus.ON_TIME to "✓ Đúng giờ",
                        HistoryStatus.LATE to "◷ Muộn",
                        HistoryStatus.MISSED to "× Bỏ lỡ"
                    ).forEach { (status, label) ->
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
                                fontSize = 11.sp,
                                color = status.color
                            )
                        }
                    }
                }

                if (
                    monthDoses.any {
                        historyStatus(it, now) == HistoryStatus.EARLY
                    }
                ) {
                    Text(
                        "✓ Màu xanh dương: đã uống sớm",
                        fontSize = 11.sp,
                        color = HistoryStatus.EARLY.color
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Vì sao có liều bị lỡ?",
                Modifier.weight(1f),
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = {
                    note = MockDoseActions.historyNotes[
                        MockCalendar.key(selected)
                    ].orEmpty()
                    showNote = true
                }
            ) {
                Text("Chỉnh sửa")
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFFF1D6)
        ) {
            Text(
                MockDoseActions.historyNotes[
                    MockCalendar.key(selected)
                ]?.takeIf {
                    it.isNotBlank()
                } ?: "Chưa có ghi chú cho ngày này.",
                Modifier.fillMaxWidth().padding(14.dp),
                fontSize = 13.sp
            )
        }

        Text(
            "${MockCalendar.date(selected)} • ${selectedDoses.size} liều",
            fontWeight = FontWeight.Bold
        )

        if (visible.isEmpty()) {
            Text(
                if (selectedDoses.isEmpty()) {
                    "Chưa có lịch uống cho ngày này."
                } else {
                    "Không có liều phù hợp bộ lọc."
                },
                color = historyMuted
            )
        }

        visible.forEach { dose ->
            val status = historyStatus(dose, now)

            Surface(
                onClick = { onDoseClick(dose.doseId) },
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(
                    1.dp,
                    if (status == HistoryStatus.MISSED) {
                        status.color
                    } else {
                        Color(0xFFD7E2DF)
                    }
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = status.background
                    ) {
                        Text(
                            dose.timeLabel.substringBefore(" •"),
                            Modifier.padding(10.dp),
                            color = status.color,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            dose.medicineName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            dose.quantityText,
                            fontSize = 12.sp,
                            color = historyMuted
                        )
                        Text(
                            status.label,
                            fontSize = 12.sp,
                            color = status.color
                        )
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
                Column {
                    listOf(
                        "Tất cả", "Đúng giờ", "Muộn", "Bỏ lỡ"
                    ).forEach { value ->
                        TextButton(
                            onClick = {
                                filter = value
                                showFilters = false
                            }
                        ) {
                            Text(
                                if (filter == value) {
                                    "✓ $value"
                                } else value
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

    if (showNote) {
        AlertDialog(
            onDismissRequest = { showNote = false },
            title = {
                Text("Ghi chú ngày ${MockCalendar.date(selected)}")
            },
            text = {
                MMRATextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Ghi chú của bạn",
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(max = 180.dp),
                    singleLine = false,
                    minLines = 3
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val key = MockCalendar.key(selected)

                        if (note.isBlank()) {
                            MockDoseActions.historyNotes.remove(key)
                        } else {
                            MockDoseActions.historyNotes[key] = note.trim()
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
private fun HistoryFlameIcon(
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val flame = Path().apply {
            moveTo(size.width * 0.55f, size.height * 0.06f)
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