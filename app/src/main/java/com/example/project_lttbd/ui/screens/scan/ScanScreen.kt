package com.example.project_lttbd.ui.screens.scan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.data.mock.MockScanMedicine
import com.example.project_lttbd.data.mock.ScanResult
import kotlinx.coroutines.delay

enum class ScanFlowState {
    CAMERA,
    PROCESSING,
    RESULT
}

@Composable
fun ScanScreen(
    modifier: Modifier = Modifier,
    onNavigateToAddMedicine: (medicineName: String, dosage: String) -> Unit = { _, _ -> },
    onNavigateToManualInput: () -> Unit = {},
    onClose: () -> Unit = {}
) {
    var flowState by remember { mutableStateOf(ScanFlowState.CAMERA) }
    var currentScanResult by remember { mutableStateOf(MockScanMedicine.defaultResult) }

    // Launcher chọn ảnh từ thư viện
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            flowState = ScanFlowState.PROCESSING
        }
    }

    // Mô phỏng AI xử lý nhận diện ảnh trong 1.5 giây
    if (flowState == ScanFlowState.PROCESSING) {
        LaunchedEffect(Unit) {
            delay(1500)
            currentScanResult = MockScanMedicine.defaultResult
            flowState = ScanFlowState.RESULT
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (flowState) {
            ScanFlowState.CAMERA -> {
                ScanCameraScreen(
                    onCaptureClick = {
                        flowState = ScanFlowState.PROCESSING
                    },
                    onPickImageClick = {
                        photoPickerLauncher.launch("image/*")
                    },
                    onManualInputClick = {
                        onNavigateToManualInput()
                    },
                    onCloseClick = {
                        onClose()
                    }
                )
            }

            ScanFlowState.PROCESSING -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F201C)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(Color(0x26126B62)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(54.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Đang nhận diện thuốc...",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Hệ thống AI đang phân tích vỏ hộp và OCR",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF9FBDB6),
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }

            ScanFlowState.RESULT -> {
                ScanResultScreen(
                    scanResult = currentScanResult,
                    onRescanClick = {
                        flowState = ScanFlowState.CAMERA
                    },
                    onConfirmClick = { confirmed ->
                        onNavigateToAddMedicine(confirmed.medicineName, confirmed.dosage)
                    },
                    onBackClick = {
                        flowState = ScanFlowState.CAMERA
                    }
                )
            }
        }
    }
}