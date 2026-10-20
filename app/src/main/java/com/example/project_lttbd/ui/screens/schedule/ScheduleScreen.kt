package com.example.project_lttbd.ui.screens.schedule

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.project_lttbd.ui.theme.MMRATheme
import java.util.Calendar

@Composable
fun ScheduleScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val todayMillis = remember {
        Calendar.getInstance().timeInMillis
    }

    var selectedDateMillis by rememberSaveable {
        mutableStateOf(todayMillis)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScheduleHeader(
            initials = "NL",
            onCalendarClick = {
                val selectedDate = Calendar.getInstance().apply {
                    timeInMillis = selectedDateMillis
                }

                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        selectedDateMillis = Calendar.getInstance().apply {
                            clear()
                            set(year, month, day, 12, 0, 0)
                        }.timeInMillis
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            onAccountClick = {
                // Chờ ghép phần tài khoản của nhóm.
            }
        )

        ScheduleWeekPicker(
            selectedDateMillis = selectedDateMillis,
            todayMillis = todayMillis,
            onDateSelected = { selectedDateMillis = it },
            modifier = Modifier.padding(
                start = 20.dp,
                top = 6.dp,
                end = 20.dp
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun ScheduleScreenPreview() {
    MMRATheme {
        ScheduleScreen()
    }
}