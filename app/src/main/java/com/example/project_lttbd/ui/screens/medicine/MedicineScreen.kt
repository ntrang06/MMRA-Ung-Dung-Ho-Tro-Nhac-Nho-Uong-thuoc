package com.example.project_lttbd.ui.screens.medicine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.ui.components.MMRASearchField
import com.example.project_lttbd.ui.theme.MMRATheme

@Composable
fun MedicineScreen(
    modifier: Modifier = Modifier,
    onMedicineClick: (String) -> Unit = {},
    onAddMedicineClick: () -> Unit = {},
    onAccountClick: () -> Unit = {}
) {
    if (LocalInspectionMode.current) {
        MedicineScreenContent(
            modifier = modifier,
            medicines = previewMedicines(),
            ownerName = "Lan",
            isLoading = false,
            isSignedIn = true,
            errorMessage = null,
            onRetry = {},
            onMedicineClick = onMedicineClick,
            onAddMedicineClick = onAddMedicineClick,
            onAccountClick = onAccountClick
        )
        return
    }

    val state = remember { MedicineListState() }

    DisposableEffect(state) {
        state.start()

        onDispose {
            state.stop()
        }
    }

    val medicines = state.medicines.map { record ->
        val medicine = record.data

        val strengthText = listOf(
            medicine.strength,
            medicine.strengthUnit
        )
            .filter { it.isNotBlank() }
            .joinToString(" ")

        val displayName = listOf(
            medicine.medicineName,
            strengthText
        )
            .filter { it.isNotBlank() }
            .joinToString(" ")

        MedicineUiModel(
            medicineId = record.id,
            medicineName = displayName,
            medicineCode = "",
            quantityText = "",
            scheduleText = "",
            hasSchedule = false,
            accent = MedicineAccent.GREEN
        )
    }

    MedicineScreenContent(
        modifier = modifier,
        medicines = medicines,
        ownerName = state.ownerName,
        isLoading = state.isLoading,
        isSignedIn = state.isSignedIn,
        errorMessage = state.errorMessage,
        onRetry = { state.refresh() },
        onMedicineClick = onMedicineClick,
        onAddMedicineClick = onAddMedicineClick,
        onAccountClick = onAccountClick
    )
}

@Composable
private fun MedicineScreenContent(
    medicines: List<MedicineUiModel>,
    ownerName: String,
    isLoading: Boolean,
    isSignedIn: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onMedicineClick: (String) -> Unit,
    onAddMedicineClick: () -> Unit,
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }

    val query = searchQuery.trim()
    val visibleMedicines = medicines.filter {
        it.medicineName.contains(query, ignoreCase = true)
    }

    val initials = ownerName
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .takeLast(2)
        .joinToString("") { it.first().toString() }
        .uppercase()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAF9))
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        MedicineHeader(
            ownerName = ownerName,
            initials = initials,
            onAccountClick = onAccountClick
        )

        Column(
            modifier = Modifier.padding(
                start = 20.dp,
                top = 6.dp,
                end = 20.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                isLoading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(
                            12.dp,
                            Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF126B62),
                            strokeWidth = 2.dp
                        )

                        Text(
                            text = "Đang tải danh sách thuốc…",
                            color = Color(0xFF526565)
                        )
                    }
                }

                !isSignedIn -> {
                    Text(
                        text = "Vui lòng đăng nhập để xem danh sách thuốc.",
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF526565)
                    )

                    TextButton(onClick = onAccountClick) {
                        Text(
                            text = "Mở tài khoản",
                            color = Color(0xFF126B62)
                        )
                    }
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )

                    TextButton(onClick = onRetry) {
                        Text(
                            text = "Thử lại",
                            color = Color(0xFF126B62)
                        )
                    }
                }

                else -> {
                    MMRASearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Tìm tên thuốc"
                    )

                    Text(
                        text = if (query.isEmpty()) {
                            "${medicines.size} thuốc"
                        } else {
                            "${visibleMedicines.size} kết quả"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            lineHeight = 22.sp,
                            letterSpacing = 0.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF102A2A)
                    )

                    if (visibleMedicines.isEmpty()) {
                        Text(
                            text = if (medicines.isEmpty()) {
                                "Bạn chưa thêm thuốc nào."
                            } else {
                                "Không tìm thấy thuốc phù hợp."
                            },
                            modifier = Modifier.padding(vertical = 16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF526565)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            visibleMedicines.forEach { medicine ->
                                MedicineCard(
                                    medicine = medicine,
                                    onClick = onMedicineClick
                                )
                            }
                        }
                    }
                }
            }

            if (isSignedIn && !isLoading) {
                AddMedicineButton(
                    onClick = onAddMedicineClick
                )
            }

            MedicineNotice()
        }
    }
}

@Composable
private fun AddMedicineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF126B62),
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(
            horizontal = 14.dp,
            vertical = 14.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier.size(18.dp)
            ) {
                val strokeWidth = 1.8.dp.toPx()

                drawLine(
                    color = Color.White,
                    start = Offset(
                        size.width * 0.2f,
                        size.height * 0.5f
                    ),
                    end = Offset(
                        size.width * 0.8f,
                        size.height * 0.5f
                    ),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = Color.White,
                    start = Offset(
                        size.width * 0.5f,
                        size.height * 0.2f
                    ),
                    end = Offset(
                        size.width * 0.5f,
                        size.height * 0.8f
                    ),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            Text(
                text = "Thêm thuốc",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 19.5.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
internal fun MedicineNotice(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFF1D6)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Canvas(
                modifier = Modifier.size(16.dp)
            ) {
                val color = Color(0xFF126B62)
                val strokeWidth = 1.5.dp.toPx()

                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.4f,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )

                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.045f,
                    center = Offset(
                        size.width * 0.5f,
                        size.height * 0.32f
                    )
                )

                drawLine(
                    color = color,
                    start = Offset(
                        size.width * 0.5f,
                        size.height * 0.48f
                    ),
                    end = Offset(
                        size.width * 0.5f,
                        size.height * 0.7f
                    ),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            Text(
                text = "MMRA chỉ hỗ trợ nhắc lịch. " +
                        "Hãy dùng thuốc theo đơn của bác sĩ.",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 19.5.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color(0xFF526565)
            )
        }
    }
}

private fun previewMedicines(): List<MedicineUiModel> {
    return listOf(
        MedicineUiModel(
            medicineId = "preview_medicine",
            medicineName = "Thuốc mẫu",
            medicineCode = "",
            quantityText = "",
            scheduleText = "",
            hasSchedule = false
        )
    )
}

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun MedicineScreenPreview() {
    MMRATheme {
        MedicineScreen()
    }
}