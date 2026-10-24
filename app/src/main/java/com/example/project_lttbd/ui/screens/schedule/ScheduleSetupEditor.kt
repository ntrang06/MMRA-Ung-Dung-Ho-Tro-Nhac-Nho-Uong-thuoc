package com.example.project_lttbd.ui.screens.schedule

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.project_lttbd.data.mock.*
import com.example.project_lttbd.ui.components.MMRATextField
import com.example.project_lttbd.ui.screens.medicine.ManualMedicineDraft
import java.util.Calendar
import kotlin.math.roundToInt

data class ScheduleSharingOption(
    val id: String,
    val name: String
)

internal val scheduleWeekdays = listOf(
    2 to "T2",
    3 to "T3",
    4 to "T4",
    5 to "T5",
    6 to "T6",
    7 to "T7",
    1 to "CN"
)

internal fun frequencyLabel(
    schedule: MockMedicineSchedule
): String {
    val days = if (schedule.weekdays.size == 7) {
        "mỗi ngày"
    } else {
        scheduleWeekdays
            .filter { it.first in schedule.weekdays }
            .joinToString(", ") { it.second }
    }

    return "${schedule.times.size} lần • $days"
}

internal fun scheduleError(
    schedule: MockMedicineSchedule
): String? = when {
    !isPositiveDoseQuantity(schedule.quantity) ->
        "Nhập liều mỗi lần là số lớn hơn 0."

    schedule.unit.isBlank() ->
        "Chọn đơn vị liều dùng."

    schedule.times.isEmpty() ->
        "Chọn ít nhất một giờ uống."

    schedule.times.any { it !in 0..1439 } ->
        "Giờ uống không hợp lệ."

    schedule.times.distinct().size != schedule.times.size ->
        "Các giờ uống không được trùng nhau."

    schedule.weekdays.isEmpty() ->
        "Chọn ít nhất một ngày trong tuần."

    schedule.endDay != null &&
            schedule.endDay < schedule.startDay ->
        "Ngày kết thúc phải từ ngày bắt đầu trở đi."

    else -> null
}

@Composable
fun ScheduleSetupEditor(
    draft: ManualMedicineDraft,
    medicineId: String? = null,
    modifier: Modifier = Modifier,
    linkedPeople: List<ScheduleSharingOption> = emptyList(),
    onBackClick: () -> Unit = {},
    onSaved: (String) -> Unit = {}
) {
    val previous = MockMedicines.findById(medicineId)

    var schedule by rememberSaveable(medicineId) {
        mutableStateOf(
            previous?.schedule ?: MockMedicineSchedule(
                quantity = "",
                unit = "",
                times = emptyList()
            )
        )
    }

    var fieldName by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var error by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var saving by remember {
        mutableStateOf(false)
    }

    val fullName = listOf(
        draft.medicineName,
        if (draft.strength.isBlank()) {
            ""
        } else {
            "${draft.strength} ${draft.strengthUnit}"
        }
    )
        .filter { it.isNotBlank() }
        .joinToString(" ")

    val period = schedule.endDay?.let { end ->
        val days =
            ((end - schedule.startDay) / 86_400_000.0)
                .roundToInt() + 1

        "$days ngày • ${MockCalendar.date(schedule.startDay)}–${MockCalendar.date(end)}"
    } ?: "Từ ${MockCalendar.date(schedule.startDay)} • Chưa có ngày kết thúc"

    ScheduleSetupScreen(
        state = ScheduleSetupUiModel(
            medicineName = fullName,
            dosageForm = draft.dosageForm,
            medicineCode = previous?.medicineCode.orEmpty(),
            confirmed = false,
            quantityText = if (schedule.quantity.isBlank()) {
                "Chưa thiết lập"
            } else {
                schedule.quantityText
            },
            frequencyText = if (schedule.times.isEmpty()) {
                "Chưa thiết lập"
            } else {
                frequencyLabel(schedule)
            },
            timesText = if (schedule.times.isEmpty()) {
                "Chưa chọn giờ"
            } else {
                schedule.times
                    .sorted()
                    .joinToString(" • ") {
                        MockCalendar.time(it)
                    }
            },
            reminderText = if (schedule.reminderMinutes == 0) {
                "Nhắc đúng giờ"
            } else {
                "Nhắc trước ${schedule.reminderMinutes} phút"
            },
            mealInstruction = schedule.meal.ifBlank {
                "Không có ghi chú về thức ăn"
            },
            periodText = period,
            stockText = schedule.stock?.let {
                "$it ${schedule.unit}"
            } ?: "Chưa nhập",
            stockReminderText = schedule.lowStock?.let {
                "Nhắc mua thêm khi còn $it ${schedule.unit}"
            }.orEmpty(),
            sharingText = schedule.sharingName.ifBlank {
                "Không chia sẻ"
            },
            sharingDescription = if (schedule.sharingId == null) {
                ""
            } else {
                "Theo quyền chia sẻ của kết nối hiện có"
            }
        ),
        modifier = modifier,
        onBackClick = onBackClick,
        onFieldClick = {
            fieldName = it.name
        },
        onSaveClick = {
            if (!saving) {
                error = scheduleError(schedule)

                if (error == null) {
                    saving = true

                    try {
                        val id = MockMedicines.save(
                            draft = draft,
                            schedule = schedule,
                            id = medicineId
                        )
                        onSaved(id)
                    } catch (_: IllegalArgumentException) {
                        saving = false
                        error =
                            "Chưa lưu được. Hãy kiểm tra thông tin thuốc và lịch uống."
                    }
                }
            }
        }
    )

    fieldName?.let { value ->
        ScheduleFieldDialog(
            field = ScheduleSetupField.valueOf(value),
            current = schedule,
            linkedPeople = linkedPeople,
            onDismiss = {
                fieldName = null
            },
            onConfirm = {
                schedule = it
                fieldName = null
            }
        )
    }

    error?.let { message ->
        AlertDialog(
            onDismissRequest = {
                error = null
            },
            title = {
                Text("Kiểm tra thông tin")
            },
            text = {
                Text(message)
            },
            confirmButton = {
                TextButton(onClick = { error = null }) {
                    Text("Đã hiểu")
                }
            }
        )
    }
}

@Composable
private fun ScheduleFieldDialog(
    field: ScheduleSetupField,
    current: MockMedicineSchedule,
    linkedPeople: List<ScheduleSharingOption>,
    onDismiss: () -> Unit,
    onConfirm: (MockMedicineSchedule) -> Unit
) {
    val context = LocalContext.current

    var working by rememberSaveable(field.name) {
        mutableStateOf(current)
    }

    var stock by rememberSaveable(field.name) {
        mutableStateOf(current.stock?.toString().orEmpty())
    }

    var lowStock by rememberSaveable(field.name) {
        mutableStateOf(current.lowStock?.toString().orEmpty())
    }

    var error by rememberSaveable(field.name) {
        mutableStateOf<String?>(null)
    }

    val title = when (field) {
        ScheduleSetupField.QUANTITY -> "Liều mỗi lần"
        ScheduleSetupField.FREQUENCY -> "Tần suất"
        ScheduleSetupField.TIMES -> "Giờ uống"
        ScheduleSetupField.MEAL -> "Cùng thức ăn"
        ScheduleSetupField.PERIOD -> "Thời gian dùng"
        ScheduleSetupField.STOCK -> "Số lượng hiện có"
        ScheduleSetupField.SHARING -> "Người thân có thể xem"
    }

    fun pickDate(
        value: Long,
        callback: (Long) -> Unit
    ) {
        val selected = Calendar.getInstance().apply {
            timeInMillis = value
        }

        DatePickerDialog(
            context,
            { _, year, month, day ->
                val date = Calendar.getInstance().apply {
                    clear()
                    set(year, month, day)
                }.timeInMillis

                callback(date)
            },
            selected.get(Calendar.YEAR),
            selected.get(Calendar.MONTH),
            selected.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun pickTime(index: Int?) {
        val minute = index?.let {
            working.times[it]
        } ?: Calendar.getInstance().let {
            it.get(Calendar.HOUR_OF_DAY) * 60 +
                    it.get(Calendar.MINUTE)
        }

        TimePickerDialog(
            context,
            { _, hour, min ->
                val times = working.times.toMutableList()
                val selectedMinute = hour * 60 + min

                if (index == null) {
                    times.add(selectedMinute)
                } else {
                    times[index] = selectedMinute
                }

                working = working.copy(times = times)
            },
            minute / 60,
            minute % 60,
            true
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (field) {
                    ScheduleSetupField.QUANTITY -> {
                        MMRATextField(
                            value = working.quantity,
                            onValueChange = {
                                working = working.copy(quantity = it)
                            },
                            label = "Số lượng mỗi lần",
                            labelOutside = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            )
                        )

                        Text("Đơn vị")

                        listOf(
                            "viên",
                            "gói",
                            "ml",
                            "giọt",
                            "liều"
                        ).forEach { unit ->
                            ScheduleChoice(
                                text = unit,
                                selected = working.unit == unit
                            ) {
                                working = working.copy(unit = unit)
                            }
                        }
                    }

                    ScheduleSetupField.FREQUENCY -> {
                        Text(
                            "Chọn các ngày uống. Số lần trong ngày được tính theo các giờ đã chọn."
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = working.weekdays.size == 7,
                                onCheckedChange = { checked ->
                                    working = working.copy(
                                        weekdays = if (checked) {
                                            (1..7).toSet()
                                        } else {
                                            emptySet()
                                        }
                                    )
                                }
                            )
                            Text("Hằng ngày")
                        }

                        scheduleWeekdays.forEach { (day, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = day in working.weekdays,
                                    onCheckedChange = { checked ->
                                        working = working.copy(
                                            weekdays = if (checked) {
                                                working.weekdays + day
                                            } else {
                                                working.weekdays - day
                                            }
                                        )
                                    }
                                )
                                Text(label)
                            }
                        }
                    }

                    ScheduleSetupField.TIMES -> {
                        working.times.forEachIndexed { index, minute ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { pickTime(index) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(MockCalendar.time(minute))
                                }

                                TextButton(
                                    onClick = {
                                        working = working.copy(
                                            times = working.times
                                                .filterIndexed { i, _ ->
                                                    i != index
                                                }
                                        )
                                    }
                                ) {
                                    Text("Xóa")
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { pickTime(null) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("+ Thêm giờ uống")
                        }

                        Text("Thời điểm nhắc")

                        listOf(0, 5, 10, 15, 30).forEach { minutes ->
                            ScheduleChoice(
                                text = if (minutes == 0) {
                                    "Đúng giờ"
                                } else {
                                    "Trước $minutes phút"
                                },
                                selected =
                                    working.reminderMinutes == minutes
                            ) {
                                working = working.copy(
                                    reminderMinutes = minutes
                                )
                            }
                        }
                    }

                    ScheduleSetupField.MEAL -> {
                        listOf(
                            "",
                            "Trước bữa ăn",
                            "Sau bữa ăn",
                            "Cùng bữa ăn"
                        ).forEach { meal ->
                            ScheduleChoice(
                                text = meal.ifBlank {
                                    "Không có ghi chú"
                                },
                                selected = working.meal == meal
                            ) {
                                working = working.copy(meal = meal)
                            }
                        }

                        MMRATextField(
                            value = working.meal,
                            onValueChange = {
                                working = working.copy(meal = it)
                            },
                            label = "Hướng dẫn trên đơn thuốc",
                            labelOutside = true,
                            singleLine = false,
                            minLines = 2
                        )
                    }

                    ScheduleSetupField.PERIOD -> {
                        OutlinedButton(
                            onClick = {
                                pickDate(working.startDay) {
                                    working = working.copy(startDay = it)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Bắt đầu: ${MockCalendar.date(working.startDay)}"
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = working.endDay == null,
                                onCheckedChange = { checked ->
                                    working = working.copy(
                                        endDay = if (checked) {
                                            null
                                        } else {
                                            working.startDay
                                        }
                                    )
                                }
                            )
                            Text("Chưa có ngày kết thúc")
                        }

                        if (working.endDay != null) {
                            OutlinedButton(
                                onClick = {
                                    pickDate(
                                        working.endDay ?: working.startDay
                                    ) {
                                        working = working.copy(endDay = it)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Kết thúc: ${
                                        MockCalendar.date(
                                            working.endDay ?: working.startDay
                                        )
                                    }"
                                )
                            }
                        }
                    }

                    ScheduleSetupField.STOCK -> {
                        val unitLabel = working.unit.ifBlank {
                            "đơn vị thuốc"
                        }

                        MMRATextField(
                            value = stock,
                            onValueChange = { stock = it },
                            label = "Số lượng hiện có ($unitLabel)",
                            labelOutside = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            )
                        )

                        MMRATextField(
                            value = lowStock,
                            onValueChange = { lowStock = it },
                            label = "Nhắc mua thêm khi còn",
                            labelOutside = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            )
                        )

                        Text(
                            "Có thể để trống nếu chưa theo dõi số lượng."
                        )
                    }

                    ScheduleSetupField.SHARING -> {
                        ScheduleChoice(
                            text = "Không chia sẻ",
                            selected = working.sharingId == null
                        ) {
                            working = working.copy(
                                sharingId = null,
                                sharingName = ""
                            )
                        }

                        if (linkedPeople.isEmpty()) {
                            Text(
                                "Bạn chưa có người thân được kết nối."
                            )
                        }

                        linkedPeople.forEach { person ->
                            ScheduleChoice(
                                text = person.name,
                                selected =
                                    working.sharingId == person.id
                            ) {
                                working = working.copy(
                                    sharingId = person.id,
                                    sharingName = person.name
                                )
                            }
                        }
                    }
                }

                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    error = when (field) {
                        ScheduleSetupField.QUANTITY -> when {
                            !isPositiveDoseQuantity(working.quantity) ->
                                "Nhập số lượng lớn hơn 0."

                            working.unit.isBlank() ->
                                "Chọn đơn vị liều dùng."

                            else -> null
                        }

                        ScheduleSetupField.TIMES -> when {
                            working.times.isEmpty() ->
                                "Chọn ít nhất một giờ uống."

                            working.times.distinct().size !=
                                    working.times.size ->
                                "Các giờ uống đang bị trùng."

                            else -> null
                        }

                        ScheduleSetupField.FREQUENCY ->
                            if (working.weekdays.isEmpty()) {
                                "Chọn ít nhất một ngày."
                            } else {
                                null
                            }

                        ScheduleSetupField.PERIOD ->
                            if (
                                working.endDay != null &&
                                (working.endDay ?: 0) < working.startDay
                            ) {
                                "Ngày kết thúc không được trước ngày bắt đầu."
                            } else {
                                null
                            }

                        ScheduleSetupField.STOCK -> when {
                            stock.isNotBlank() &&
                                    stock.toIntOrNull()?.let {
                                        it >= 0
                                    } != true ->
                                "Số lượng phải là số nguyên từ 0 trở lên."

                            lowStock.isNotBlank() &&
                                    lowStock.toIntOrNull()?.let {
                                        it >= 0
                                    } != true ->
                                "Ngưỡng nhắc phải là số nguyên từ 0 trở lên."

                            lowStock.isNotBlank() && stock.isBlank() ->
                                "Nhập số lượng hiện có trước khi đặt ngưỡng nhắc."

                            else -> null
                        }

                        else -> null
                    }

                    if (error == null) {
                        val updated =
                            if (field == ScheduleSetupField.STOCK) {
                                working.copy(
                                    stock = stock.toIntOrNull(),
                                    lowStock = lowStock.toIntOrNull()
                                )
                            } else {
                                working
                            }

                        onConfirm(updated)
                    }
                }
            ) {
                Text("Xác nhận")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
private fun ScheduleChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = null
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = text,
                modifier = Modifier.weight(1f)
            )
        }
    }
}