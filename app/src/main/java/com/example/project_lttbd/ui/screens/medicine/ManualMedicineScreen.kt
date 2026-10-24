package com.example.project_lttbd.ui.screens.medicine

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.ui.components.MMRAButton
import com.example.project_lttbd.ui.components.MMRATextField
import com.example.project_lttbd.ui.theme.MMRATheme

@Composable
fun ManualMedicineScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onContinue: (ManualMedicineDraft) -> Unit = {},
    initialDraft: ManualMedicineDraft? = null
) {
    var medicineName by rememberSaveable {
        mutableStateOf(initialDraft?.medicineName.orEmpty())
    }
    var strength by rememberSaveable {
        mutableStateOf(initialDraft?.strength.orEmpty())
    }
    var strengthUnit by rememberSaveable {
        mutableStateOf(initialDraft?.strengthUnit ?: "mg")
    }
    var dosageForm by rememberSaveable {
        mutableStateOf(initialDraft?.dosageForm.orEmpty())
    }
    var note by rememberSaveable {
        mutableStateOf(initialDraft?.note.orEmpty())
    }
    var imageUri by rememberSaveable {
        mutableStateOf<String?>(initialDraft?.imageUri)
    }
    var attemptedContinue by rememberSaveable {
        mutableStateOf(false)
    }

    val focusManager = LocalFocusManager.current
    val isPreview = LocalInspectionMode.current

    val imagePicker = if (!isPreview) {
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) imageUri = uri.toString()
        }
    } else null

    val trimmedStrength = strength.trim()

    val validStrength = trimmedStrength.isEmpty() || (
            trimmedStrength.matches(Regex("\\d+([.,]\\d+)?")) &&
                    trimmedStrength.replace(',', '.')
                        .toBigDecimalOrNull()?.signum() == 1
            )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAF9))
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBackClick,
                modifier = Modifier.size(44.dp).semantics {
                    contentDescription = "Quay lại"
                },
                shape = CircleShape,
                color = Color(0xFFEEF5F3)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    ManualMedicineIcon(
                        ManualIconKind.BACK,
                        Modifier.size(20.dp),
                        Color(0xFF102A2A)
                    )
                }
            }

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (initialDraft == null) {
                        "Nhập thông tin thuốc"
                    } else {
                        "Sửa thông tin thuốc"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        lineHeight = 27.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF102A2A)
                )

                Text(
                    text = if (initialDraft == null) {
                        "Bước 1/3 • Nhập thủ công"
                    } else {
                        "Kiểm tra thông tin và lịch uống"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 19.5.sp,
                        letterSpacing = 0.sp
                    ),
                    color = Color(0xFF526565)
                )
            }
        }

        Text(
            "Nhập thông tin theo vỏ hộp hoặc đơn thuốc.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                lineHeight = 19.5.sp,
                letterSpacing = 0.sp
            ),
            color = Color(0xFF526565)
        )

        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFD7E2DF))
        ) {
            Row(
                Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    Modifier.size(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFD9EEEA)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ManualMedicineIcon(
                            ManualIconKind.CAMERA,
                            Modifier.size(30.dp)
                        )
                    }
                }

                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        "Ảnh thuốc",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF102A2A)
                    )
                    Text(
                        if (imageUri == null) "Không bắt buộc" else "Đã chọn ảnh",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF526565)
                    )
                }

                OutlinedButton(
                    onClick = {
                        focusManager.clearFocus()
                        imagePicker?.launch("image/*")
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF126B62))
                ) {
                    Text(if (imageUri == null) "Thêm ảnh" else "Đổi ảnh")
                }
            }
        }

        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFD7E2DF))
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MMRATextField(
                    value = medicineName,
                    onValueChange = { medicineName = it },
                    label = "Tên thuốc *",
                    placeholder = "Ví dụ: Metformin",
                    modifier = Modifier.fillMaxWidth(),
                    labelOutside = true,
                    errorMessage =
                        if (attemptedContinue && medicineName.isBlank()) {
                            "Vui lòng nhập tên thuốc."
                        } else null,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    )
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    MMRATextField(
                        value = strength,
                        onValueChange = { strength = it },
                        label = "Hàm lượng",
                        placeholder = "Ví dụ: 500",
                        modifier = Modifier.weight(1f),
                        labelOutside = true,
                        errorMessage =
                            if (attemptedContinue && !validStrength) {
                                "Nhập hàm lượng là số lớn hơn 0."
                            } else null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        )
                    )

                    ManualMedicineDropdown(
                        label = "Đơn vị",
                        value = strengthUnit,
                        placeholder = "",
                        options = listOf("mg", "g", "mcg", "IU", "mg/ml", "%"),
                        onSelect = { strengthUnit = it },
                        modifier = Modifier.weight(0.55f)
                    )
                }

                ManualMedicineDropdown(
                    label = "Dạng thuốc",
                    value = dosageForm,
                    placeholder = "Chọn dạng thuốc",
                    options = listOf(
                        "Viên nén", "Viên nang", "Dung dịch",
                        "Siro", "Thuốc nhỏ", "Khác"
                    ),
                    onSelect = { dosageForm = it }
                )

                MMRATextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Ghi chú",
                    placeholder = "Thông tin bổ sung (nếu có)",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 3,
                    labelOutside = true
                )
            }
        }

        Text(
            "* Thông tin bắt buộc",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF526565)
        )

        Box(
            Modifier.fillMaxWidth().border(
                1.dp,
                Color(0xFFAA5B00),
                RoundedCornerShape(12.dp)
            )
        ) {
            MedicineNotice()
        }

        MMRAButton(
            text = "Tiếp tục",
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                attemptedContinue = true

                if (medicineName.isNotBlank() && validStrength) {
                    focusManager.clearFocus()
                    onContinue(
                        ManualMedicineDraft(
                            medicineName.trim(),
                            trimmedStrength,
                            strengthUnit,
                            dosageForm,
                            note.trim(),
                            imageUri
                        )
                    )
                }
            },
            trailingIcon = {
                ManualMedicineIcon(
                    ManualIconKind.FORWARD,
                    Modifier.size(20.dp),
                    Color.White
                )
            }
        )
    }
}

@Composable
private fun ManualMedicineDropdown(
    label: String,
    value: String,
    placeholder: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = Color(0xFF102A2A)
        )

        Box {
            Surface(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFD7E2DF))
            ) {
                Row(
                    Modifier.heightIn(min = 56.dp)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        value.ifEmpty { placeholder },
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            letterSpacing = 0.sp
                        ),
                        color = if (value.isEmpty()) {
                            Color(0xFF526565)
                        } else {
                            Color(0xFF102A2A)
                        }
                    )

                    ManualMedicineIcon(
                        ManualIconKind.DOWN,
                        Modifier.size(18.dp),
                        Color(0xFF526565)
                    )
                }
            }

            DropdownMenu(
                expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

internal enum class ManualIconKind {
    BACK, CAMERA, DOWN, FORWARD
}

@Composable
internal fun ManualMedicineIcon(
    kind: ManualIconKind,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF126B62)
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(
                color,
                Offset(w * x1, h * y1),
                Offset(w * x2, h * y2),
                stroke,
                StrokeCap.Round
            )
        }

        when (kind) {
            ManualIconKind.BACK -> {
                line(0.65f, 0.2f, 0.35f, 0.5f)
                line(0.35f, 0.5f, 0.65f, 0.8f)
            }
            ManualIconKind.DOWN -> {
                line(0.2f, 0.35f, 0.5f, 0.65f)
                line(0.5f, 0.65f, 0.8f, 0.35f)
            }
            ManualIconKind.FORWARD -> {
                line(0.15f, 0.5f, 0.85f, 0.5f)
                line(0.6f, 0.25f, 0.85f, 0.5f)
                line(0.85f, 0.5f, 0.6f, 0.75f)
            }
            ManualIconKind.CAMERA -> {
                drawRoundRect(
                    color,
                    Offset(w * 0.1f, h * 0.3f),
                    Size(w * 0.8f, h * 0.55f),
                    CornerRadius(w * 0.1f),
                    style = Stroke(stroke)
                )
                drawCircle(
                    color,
                    w * 0.16f,
                    Offset(w * 0.5f, h * 0.57f),
                    style = Stroke(stroke)
                )

                val top = Path().apply {
                    moveTo(w * 0.3f, h * 0.3f)
                    lineTo(w * 0.38f, h * 0.16f)
                    lineTo(w * 0.62f, h * 0.16f)
                    lineTo(w * 0.7f, h * 0.3f)
                }
                drawPath(top, color, style = Stroke(stroke))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ManualMedicineScreenPreview() {
    MMRATheme { ManualMedicineScreen() }
}