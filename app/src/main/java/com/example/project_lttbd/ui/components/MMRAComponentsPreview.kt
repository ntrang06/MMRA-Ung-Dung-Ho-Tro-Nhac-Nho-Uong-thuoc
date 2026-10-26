package com.example.project_lttbd.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.project_lttbd.ui.theme.MMRATheme

@Preview(showBackground = true, widthDp = 360)
@Composable
fun MMRAComponentsPreview() {
    MMRATheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            ComponentsDemo()
        }
    }
}

@Composable
private fun ComponentsDemo() {
    var medicineName by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Giao diện chung MMRA",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Kiểm tra màu sắc, kiểu chữ, nút và ô nhập liệu.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        MMRASearchField(
            value = medicineName,
            onValueChange = { medicineName = it }
        )
        MMRATextField(
            value = medicineName,
            onValueChange = {
                medicineName = it
                showError = false
            },
            label = "Tên thuốc",
            placeholder = "Nhập tên thuốc",
            errorMessage = if (showError) "Vui lòng nhập tên thuốc" else null,
            modifier = Modifier.fillMaxWidth()
        )

        MMRAButton(
            text = "Lưu",
            onClick = {
                showError = medicineName.isBlank()
            },
            modifier = Modifier.fillMaxWidth()
        )

        MMRAButton(
            text = "Nút đang bị vô hiệu hóa",
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}