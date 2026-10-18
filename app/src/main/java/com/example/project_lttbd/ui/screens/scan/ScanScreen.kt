package com.example.project_lttbd.ui.screens.scan

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.project_lttbd.data.mock.MockScanMedicine
import kotlinx.coroutines.delay

enum class ScanFlowState {
    CAMERA,
    PREVIEW,
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
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isFromGallery by remember { mutableStateOf(false) }
    var currentScanResult by remember { mutableStateOf(MockScanMedicine.defaultResult) }

    // Launcher chọn ảnh từ thư viện
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            isFromGallery = true
            flowState = ScanFlowState.PREVIEW
        }
    }

    // Khi ở màn hình PROCESSING, mô phỏng AI xử lý nhận diện ảnh trong 1.5 giây rồi chuyển sang RESULT
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
                        isFromGallery = false
                        selectedImageUri = null
                        flowState = ScanFlowState.PREVIEW
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

            ScanFlowState.PREVIEW -> {
                ScanPreviewScreen(
                    imageUri = selectedImageUri,
                    isFromGallery = isFromGallery,
                    onRetakeClick = {
                        if (isFromGallery) {
                            photoPickerLauncher.launch("image/*")
                        } else {
                            flowState = ScanFlowState.CAMERA
                        }
                    },
                    onUsePhotoClick = {
                        flowState = ScanFlowState.PROCESSING
                    },
                    onBackClick = {
                        flowState = ScanFlowState.CAMERA
                    }
                )
            }

            ScanFlowState.PROCESSING -> {
                ScanProcessingScreen(
                    onBackClick = {
                        flowState = ScanFlowState.PREVIEW
                    }
                )
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
                        flowState = ScanFlowState.PREVIEW
                    }
                )
            }
        }
    }
}