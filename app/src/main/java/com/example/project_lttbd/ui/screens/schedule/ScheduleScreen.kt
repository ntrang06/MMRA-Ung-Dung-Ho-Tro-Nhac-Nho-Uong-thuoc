package com.example.project_lttbd.ui.screens.schedule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.project_lttbd.ui.theme.MMRATheme

@Composable
fun ScheduleScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScheduleHeader(
            initials = "NL",
            onCalendarClick = {
                // Bổ sung bộ chọn ngày ở bước tiếp theo.
            },
            onAccountClick = {
                // Chờ ghép phần tài khoản của nhóm.
            }
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