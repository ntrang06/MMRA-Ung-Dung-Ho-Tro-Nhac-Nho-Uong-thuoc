package com.example.project_lttbd.ui.screens.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mmra_medicationmanager.R
import com.example.project_lttbd.ui.components.MMRAButton
import com.example.project_lttbd.ui.screens.medicine.MedicineNotice
import com.example.project_lttbd.ui.theme.MMRATheme

@Composable
fun ScheduleSetupScreen(
    state: ScheduleSetupUiModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onFieldClick: (ScheduleSetupField) -> Unit = {},
    onSaveClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAF9))
            .imePadding()
    ) {
        // Vùng cuộn chỉ chiếm phần còn lại sau khi chừa chỗ cho nút Lưu.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ScheduleSetupHeader(
                onBackClick = onBackClick
            )

            ScheduleMedicineSummary(
                state = state
            )

            ScheduleSetupGroup(
                title = "Liều dùng và thời gian"
            ) {
                ScheduleSetupRow(
                    field = ScheduleSetupField.QUANTITY,
                    label = "Liều mỗi lần",
                    value = state.quantityText,
                    onClick = {
                        onFieldClick(ScheduleSetupField.QUANTITY)
                    }
                )

                ScheduleSetupDivider()

                ScheduleSetupRow(
                    field = ScheduleSetupField.FREQUENCY,
                    label = "Tần suất",
                    value = state.frequencyText,
                    onClick = {
                        onFieldClick(ScheduleSetupField.FREQUENCY)
                    }
                )

                ScheduleSetupDivider()

                ScheduleSetupRow(
                    field = ScheduleSetupField.TIMES,
                    label = "Giờ uống",
                    value = state.timesText,
                    supportingText = state.reminderText,
                    onClick = {
                        onFieldClick(ScheduleSetupField.TIMES)
                    }
                )

                ScheduleSetupDivider()

                ScheduleSetupRow(
                    field = ScheduleSetupField.MEAL,
                    label = "Cùng thức ăn",
                    value = state.mealInstruction,
                    onClick = {
                        onFieldClick(ScheduleSetupField.MEAL)
                    }
                )
            }

            ScheduleSetupGroup(
                title = "Đợt thuốc và theo dõi"
            ) {
                ScheduleSetupRow(
                    field = ScheduleSetupField.PERIOD,
                    label = "Thời gian dùng",
                    value = state.periodText,
                    onClick = {
                        onFieldClick(ScheduleSetupField.PERIOD)
                    }
                )

                ScheduleSetupDivider()

                ScheduleSetupRow(
                    field = ScheduleSetupField.STOCK,
                    label = "Số lượng hiện có",
                    value = state.stockText,
                    supportingText = state.stockReminderText,
                    onClick = {
                        onFieldClick(ScheduleSetupField.STOCK)
                    }
                )

                ScheduleSetupDivider()

                ScheduleSetupRow(
                    field = ScheduleSetupField.SHARING,
                    label = "Người thân có thể xem",
                    value = state.sharingText,
                    supportingText = state.sharingDescription,
                    onClick = {
                        onFieldClick(ScheduleSetupField.SHARING)
                    }
                )
            }

            MedicineNotice()
        }

        // Nút Lưu là phần riêng, không nằm trong nội dung cuộn.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFF7FAF9)
        ) {
            Box(
                modifier = Modifier.padding(
                    start = 20.dp,
                    top = 10.dp,
                    end = 20.dp,
                    bottom = 14.dp
                )
            ) {
                MMRAButton(
                    text = "Lưu lịch uống",
                    onClick = onSaveClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ScheduleSetupHeader(
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onBackClick,
            modifier = Modifier
                .size(44.dp)
                .semantics {
                    contentDescription = "Quay lại"
                },
            shape = CircleShape,
            color = Color(0xFFEEF5F3)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                ScheduleSetupChevron(
                    pointsLeft = true,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Thiết lập lịch uống",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    lineHeight = 27.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF102A2A)
            )

            Text(
                text = "Bước 3/3 • Kiểm tra và lưu",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 19.5.sp,
                    letterSpacing = 0.sp
                ),
                color = Color(0xFF526565)
            )
        }
    }
}

@Composable
private fun ScheduleMedicineSummary(
    state: ScheduleSetupUiModel
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFEDF5FF)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.width(54.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFEDF5FF),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFF2E6FAD)
                )
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(min = 58.dp)
                        .padding(
                            horizontal = 4.dp,
                            vertical = 8.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        4.dp,
                        Alignment.CenterVertically
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.thuoc),
                        contentDescription = null,
                        modifier = Modifier.size(25.dp),
                        tint = Color(0xFF2E6FAD)
                    )

                    if (state.medicineCode.isNotBlank()) {
                        Text(
                            text = state.medicineCode,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                lineHeight = 12.sp,
                                letterSpacing = 0.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF2E6FAD)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = state.medicineName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        lineHeight = 21.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF102A2A)
                )

                if (state.dosageForm.isNotBlank()) {
                    Text(
                        text = state.dosageForm,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            letterSpacing = 0.sp
                        ),
                        color = Color(0xFF526565)
                    )
                }
            }

            if (state.confirmed) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFFE3F4E9)
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = 7.dp,
                            vertical = 5.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScheduleSetupCheck(
                            modifier = Modifier.size(11.dp)
                        )

                        Text(
                            text = "ĐÃ XÁC NHẬN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                lineHeight = 12.sp,
                                letterSpacing = 0.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF287A4D)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleSetupGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFD7E2DF)
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 8.dp
            )
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(
                    top = 2.dp,
                    bottom = 4.dp
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF102A2A)
            )

            content()
        }
    }
}

@Composable
private fun ScheduleSetupRow(
    field: ScheduleSetupField,
    label: String,
    value: String,
    onClick: () -> Unit,
    supportingText: String = ""
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 62.dp)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFEEF5F3)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    ScheduleSetupFieldIcon(
                        field = field,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        letterSpacing = 0.sp
                    ),
                    color = Color(0xFF526565)
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color(0xFF102A2A)
                )

                if (supportingText.isNotBlank()) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            letterSpacing = 0.sp
                        ),
                        color = Color(0xFF526565)
                    )
                }
            }

            ScheduleSetupChevron(
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ScheduleSetupDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFD7E2DF))
    )
}

@Composable
private fun ScheduleSetupFieldIcon(
    field: ScheduleSetupField,
    modifier: Modifier = Modifier
) {
    val resource = when (field) {
        ScheduleSetupField.QUANTITY -> R.drawable.thuoc
        ScheduleSetupField.PERIOD -> R.drawable.lich
        ScheduleSetupField.SHARING -> R.drawable.family
        else -> null
    }

    if (resource != null) {
        Icon(
            painter = painterResource(resource),
            contentDescription = null,
            modifier = modifier,
            tint = Color(0xFF126B62)
        )
    } else {
        Canvas(modifier = modifier) {
            val color = Color(0xFF126B62)
            val strokeWidth = 1.6.dp.toPx()
            val w = size.width
            val h = size.height

            fun line(
                x1: Float,
                y1: Float,
                x2: Float,
                y2: Float
            ) {
                drawLine(
                    color = color,
                    start = Offset(w * x1, h * y1),
                    end = Offset(w * x2, h * y2),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            when (field) {
                ScheduleSetupField.TIMES -> {
                    drawCircle(
                        color = color,
                        radius = size.minDimension * 0.38f,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )

                    line(0.5f, 0.27f, 0.5f, 0.5f)
                    line(0.5f, 0.5f, 0.67f, 0.6f)
                }

                ScheduleSetupField.FREQUENCY -> {
                    line(0.2f, 0.3f, 0.75f, 0.3f)
                    line(0.75f, 0.3f, 0.75f, 0.52f)
                    line(0.2f, 0.3f, 0.34f, 0.16f)
                    line(0.2f, 0.3f, 0.34f, 0.44f)

                    line(0.8f, 0.7f, 0.25f, 0.7f)
                    line(0.25f, 0.7f, 0.25f, 0.48f)
                    line(0.8f, 0.7f, 0.66f, 0.56f)
                    line(0.8f, 0.7f, 0.66f, 0.84f)
                }

                ScheduleSetupField.MEAL -> {
                    line(0.16f, 0.15f, 0.16f, 0.4f)
                    line(0.3f, 0.15f, 0.3f, 0.85f)
                    line(0.44f, 0.15f, 0.44f, 0.4f)
                    line(0.16f, 0.4f, 0.44f, 0.4f)

                    line(0.75f, 0.15f, 0.75f, 0.85f)
                    line(0.6f, 0.15f, 0.6f, 0.5f)
                    line(0.6f, 0.5f, 0.75f, 0.5f)
                }

                ScheduleSetupField.STOCK -> {
                    line(0.15f, 0.3f, 0.5f, 0.12f)
                    line(0.5f, 0.12f, 0.85f, 0.3f)
                    line(0.85f, 0.3f, 0.5f, 0.48f)
                    line(0.5f, 0.48f, 0.15f, 0.3f)

                    line(0.15f, 0.3f, 0.15f, 0.72f)
                    line(0.15f, 0.72f, 0.5f, 0.9f)
                    line(0.5f, 0.9f, 0.85f, 0.72f)
                    line(0.85f, 0.72f, 0.85f, 0.3f)

                    line(0.5f, 0.48f, 0.5f, 0.9f)
                    line(0.32f, 0.21f, 0.67f, 0.39f)
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun ScheduleSetupChevron(
    modifier: Modifier = Modifier,
    pointsLeft: Boolean = false
) {
    Canvas(modifier = modifier) {
        val color = Color(0xFF526565)
        val strokeWidth = 1.8.dp.toPx()

        val outerX = if (pointsLeft) 0.65f else 0.35f
        val middleX = if (pointsLeft) 0.35f else 0.65f

        drawLine(
            color = color,
            start = Offset(
                size.width * outerX,
                size.height * 0.22f
            ),
            end = Offset(
                size.width * middleX,
                size.height * 0.5f
            ),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        drawLine(
            color = color,
            start = Offset(
                size.width * middleX,
                size.height * 0.5f
            ),
            end = Offset(
                size.width * outerX,
                size.height * 0.78f
            ),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun ScheduleSetupCheck(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val color = Color(0xFF287A4D)
        val strokeWidth = 1.6.dp.toPx()

        drawLine(
            color = color,
            start = Offset(
                size.width * 0.16f,
                size.height * 0.5f
            ),
            end = Offset(
                size.width * 0.4f,
                size.height * 0.73f
            ),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        drawLine(
            color = color,
            start = Offset(
                size.width * 0.4f,
                size.height * 0.73f
            ),
            end = Offset(
                size.width * 0.84f,
                size.height * 0.25f
            ),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun ScheduleSetupScreenPreview() {
    MMRATheme {
        ScheduleSetupScreen(
            state = ScheduleSetupUiModel(
                medicineName = "Metformin 500 mg",
                dosageForm = "Viên nén bao phim",
                medicineCode = "M500",
                confirmed = true,
                quantityText = "1 viên (500 mg)",
                frequencyText = "2 lần mỗi ngày",
                timesText = "08:00 • 20:00",
                reminderText = "Nhắc trước 10 phút",
                mealInstruction = "Uống cùng hoặc ngay sau bữa ăn",
                periodText = "30 ngày • 02/10–31/10",
                stockText = "60 viên",
                stockReminderText = "Nhắc mua thêm khi còn 6 viên",
                sharingText = "Minh (Con trai)",
                sharingDescription = "Chỉ trạng thái uống và cảnh báo bỏ lỡ"
            )
        )
    }
}
