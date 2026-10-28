package com.example.project_lttbd.ui.screens.scan

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.example.project_lttbd.data.mock.MockScanMedicine
import com.example.project_lttbd.data.mock.ScanResult
import com.example.project_lttbd.ui.screens.medicine.ManualMedicineDraft
import com.example.project_lttbd.ui.screens.medicine.ManualMedicineScreen
import kotlinx.coroutines.delay

enum class ScanFlowState {
    CAMERA,
    PREVIEW,
    PROCESSING,
    INPUT,
    RESULT
}

private val ScanMedicineDraftSaver =
    listSaver<ManualMedicineDraft, String>(
        save = {
            listOf(
                it.medicineName,
                it.strength,
                it.strengthUnit,
                it.dosageForm,
                it.note,
                it.imageUri.orEmpty()
            )
        },
        restore = {
            ManualMedicineDraft(
                medicineName = it[0],
                strength = it[1],
                strengthUnit = it[2],
                dosageForm = it[3],
                note = it[4],
                imageUri = it[5].ifBlank { null }
            )
        }
    )

private val ScanMedicineResultSaver =
    listSaver<ScanResult, Any>(
        save = {
            listOf(
                it.id,
                it.medicineName,
                it.dosage,
                it.dosageForm,
                it.matchScore,
                it.activeIngredient,
                it.manufacturer,
                it.packaging,
                it.verifiedSource,
                it.pillCode,
                it.color,
                it.imageUrl.orEmpty()
            )
        },
        restore = {
            ScanResult(
                id = it[0] as String,
                medicineName = it[1] as String,
                dosage = it[2] as String,
                dosageForm = it[3] as String,
                matchScore = it[4] as Int,
                activeIngredient = it[5] as String,
                manufacturer = it[6] as String,
                packaging = it[7] as String,
                verifiedSource = it[8] as String,
                pillCode = it[9] as String,
                color = it[10] as String,
                imageUrl = (it[11] as String).ifBlank { null }
            )
        }
    )

@Composable
fun ScanScreen(
    modifier: Modifier = Modifier,
    onNavigateToSchedule: (ManualMedicineDraft) -> Unit = {},
    onNavigateToManualInput: () -> Unit = {},
    onClose: () -> Unit = {}
) {
    var flowState by rememberSaveable {
        mutableStateOf(ScanFlowState.CAMERA)
    }

    var selectedImageUriText by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var isFromGallery by rememberSaveable {
        mutableStateOf(false)
    }

    var currentScanResult by rememberSaveable(
        stateSaver = ScanMedicineResultSaver
    ) {
        mutableStateOf(MockScanMedicine.defaultResult)
    }

    var medicineDraft by rememberSaveable(
        stateSaver = ScanMedicineDraftSaver
    ) {
        mutableStateOf(
            ManualMedicineDraft(
                medicineName = "",
                strength = "",
                strengthUnit = "mg",
                dosageForm = "",
                note = "",
                imageUri = null
            )
        )
    }

    var inputSession by rememberSaveable {
        mutableIntStateOf(0)
    }

    val inputStateHolder = rememberSaveableStateHolder()

    val selectedImageUri = selectedImageUriText?.let {
        Uri.parse(it)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUriText = uri.toString()
            isFromGallery = true
            flowState = ScanFlowState.PREVIEW
        }
    }

    // Hiện vẫn mô phỏng nhận diện bằng dữ liệu mẫu.
    // Sau khi xử lý, mở INPUT trước RESULT.
    LaunchedEffect(flowState) {
        if (flowState == ScanFlowState.PROCESSING) {
            delay(1500)

            currentScanResult = MockScanMedicine.defaultResult

            medicineDraft = draftFromScanResult(
                result = currentScanResult,
                imageUri = selectedImageUriText
            )

            inputStateHolder.removeState(
                "scan_medicine_input_$inputSession"
            )
            inputSession += 1

            flowState = ScanFlowState.INPUT
        }
    }

    fun goBack() {
        when (flowState) {
            ScanFlowState.CAMERA -> onClose()

            ScanFlowState.PREVIEW -> {
                flowState = ScanFlowState.CAMERA
            }

            ScanFlowState.PROCESSING -> {
                flowState = ScanFlowState.PREVIEW
            }

            ScanFlowState.INPUT -> {
                flowState = ScanFlowState.PREVIEW
            }

            ScanFlowState.RESULT -> {
                flowState = ScanFlowState.INPUT
            }
        }
    }

    BackHandler(
        enabled = flowState != ScanFlowState.CAMERA
    ) {
        goBack()
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when (flowState) {
            ScanFlowState.CAMERA -> {
                ScanCameraScreen(
                    onCaptureClick = { capturedUri ->
                        selectedImageUriText = capturedUri.toString()
                        isFromGallery = false
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
                        goBack()
                    }
                )
            }

            ScanFlowState.PROCESSING -> {
                ScanProcessingScreen(
                    onBackClick = {
                        goBack()
                    }
                )
            }

            ScanFlowState.INPUT -> {
                inputStateHolder.SaveableStateProvider(
                    key = "scan_medicine_input_$inputSession"
                ) {
                    ManualMedicineScreen(
                        initialDraft = medicineDraft,
                        isEditing = false,
                        onBackClick = {
                            goBack()
                        },
                        onContinue = { enteredDraft ->
                            medicineDraft = enteredDraft

                            currentScanResult = currentScanResult.copy(
                                medicineName = enteredDraft.medicineName,
                                dosage = if (
                                    enteredDraft.strength.isBlank()
                                ) {
                                    ""
                                } else {
                                    "${enteredDraft.strength} " +
                                            enteredDraft.strengthUnit
                                },
                                dosageForm = enteredDraft.dosageForm,
                                imageUrl = enteredDraft.imageUri
                            )

                            flowState = ScanFlowState.RESULT
                        }
                    )
                }
            }

            ScanFlowState.RESULT -> {
                ScanResultScreen(
                    scanResult = currentScanResult,
                    onRescanClick = {
                        flowState = ScanFlowState.CAMERA
                    },
                    onConfirmClick = { confirmed ->
                        currentScanResult = confirmed

                        val dosage = parseScanDosage(
                            confirmed.dosage
                        )

                        val confirmedDraft = medicineDraft.copy(
                            medicineName = confirmed.medicineName.trim(),
                            strength = dosage?.first
                                ?: medicineDraft.strength,
                            strengthUnit = dosage?.second
                                ?: medicineDraft.strengthUnit,
                            dosageForm = confirmed.dosageForm
                        )

                        medicineDraft = confirmedDraft

                        // Đi thẳng tới bước thiết lập lịch.
                        // Không mở lại màn nhập thuốc.
                        onNavigateToSchedule(confirmedDraft)
                    },
                    onBackClick = {
                        goBack()
                    }
                )
            }
        }
    }
}

private fun draftFromScanResult(
    result: ScanResult,
    imageUri: String?
): ManualMedicineDraft {
    val dosage = parseScanDosage(result.dosage)

    return ManualMedicineDraft(
        medicineName = result.medicineName,
        strength = dosage?.first.orEmpty(),
        strengthUnit = dosage?.second ?: "mg",
        dosageForm = result.dosageForm,
        note = if (
            dosage == null && result.dosage.isNotBlank()
        ) {
            "Hàm lượng trên kết quả quét: ${result.dosage}"
        } else {
            ""
        },
        imageUri = imageUri
    )
}

private fun parseScanDosage(
    value: String
): Pair<String, String>? {
    val match = Regex(
        """^\s*(\d+(?:[.,]\d+)?)\s*(mg/ml|mcg|mg|g|IU|%)\s*$""",
        RegexOption.IGNORE_CASE
    ).matchEntire(value) ?: return null

    val supportedUnits = listOf(
        "mg", "g", "mcg", "IU", "mg/ml", "%"
    )

    val unit = supportedUnits.first {
        it.equals(
            match.groupValues[2],
            ignoreCase = true
        )
    }

    return match.groupValues[1] to unit
}