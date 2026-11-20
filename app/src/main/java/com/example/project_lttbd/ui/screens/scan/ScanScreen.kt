
package com.example.project_lttbd.ui.screens.scan

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.project_lttbd.data.image.MedicinePhotoCodec
import com.example.project_lttbd.data.remote.mapCatalogDosageForm
import com.example.project_lttbd.data.remote.parseCatalogStrength
import com.example.project_lttbd.data.scan.MedicineOcrReader
import com.example.project_lttbd.data.scan.MedicineScanCrop
import com.example.project_lttbd.data.scan.ScanCropRegion
import com.example.project_lttbd.ui.screens.medicine.ManualMedicineDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

enum class ScanFlowState {
    CAMERA, PREVIEW, PROCESSING, RESULT, LOOKUP
}

private val OcrDraftSaver =
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
                it[0],
                it[1],
                it[2],
                it[3],
                it[4],
                it[5].ifBlank { null }
            )
        }
    )

private val ScanCropSaver =
    listSaver<ScanCropRegion, Float>(
        save = {
            listOf(it.left, it.top, it.right, it.bottom)
        },
        restore = {
            ScanCropRegion(
                it[0], it[1], it[2], it[3]
            ).normalized()
        }
    )

@Composable
fun ScanScreen(
    modifier: Modifier = Modifier,
    onMedicineDraftReady: (ManualMedicineDraft) -> Unit = {},
    onNavigateToManualInput: () -> Unit = {},
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current.applicationContext

    var state by rememberSaveable {
        mutableStateOf(ScanFlowState.CAMERA)
    }

    var imageUri by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var fromGallery by rememberSaveable {
        mutableStateOf(false)
    }

    var processingPhoto by remember(imageUri) {
        mutableStateOf<String?>(null)
    }

    var crop by rememberSaveable(
        imageUri,
        stateSaver = ScanCropSaver
    ) {
        mutableStateOf(ScanCropRegion.Full)
    }

    var rawText by rememberSaveable {
        mutableStateOf("")
    }

    var error by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var showGalleryInfo by rememberSaveable {
        mutableStateOf(false)
    }

    var galleryAccepted by rememberSaveable {
        mutableStateOf(false)
    }

    var draft by rememberSaveable(
        stateSaver = OcrDraftSaver
    ) {
        mutableStateOf(
            ManualMedicineDraft(
                "", "", "mg", "", "", null
            )
        )
    }

    fun setImage(uri: Uri, gallery: Boolean) {
        imageUri = uri.toString()
        fromGallery = gallery
        rawText = ""
        error = null

        draft = ManualMedicineDraft(
            "", "", "mg", "", "", null
        )

        state = ScanFlowState.PREVIEW
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            setImage(uri, true)
        }
    }

    fun openGallery() {
        if (galleryAccepted) {
            picker.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts
                        .PickVisualMedia.ImageOnly
                )
            )
        } else {
            showGalleryInfo = true
        }
    }

    fun goBack() {
        when (state) {
            ScanFlowState.CAMERA -> onClose()

            ScanFlowState.PREVIEW ->
                state = ScanFlowState.CAMERA

            ScanFlowState.PROCESSING,
            ScanFlowState.RESULT ->
                state = ScanFlowState.PREVIEW

            ScanFlowState.LOOKUP ->
                state = ScanFlowState.RESULT
        }
    }

    BackHandler(
        enabled = state != ScanFlowState.CAMERA
    ) {
        goBack()
    }

    LaunchedEffect(
        state,
        imageUri,
        crop
    ) {
        if (state != ScanFlowState.PROCESSING) {
            return@LaunchedEffect
        }

        try {
            val source = Uri.parse(
                requireNotNull(imageUri)
            )

            val pair = withTimeout(30_000) {
                MedicineScanCrop.useRegion(
                    context,
                    source,
                    crop
                ) { cropped ->
                    val photo = MedicinePhotoCodec.prepare(
                        context,
                        cropped
                    )

                    withContext(Dispatchers.Main) {
                        processingPhoto = photo
                    }

                    MedicineOcrReader.read(
                        context,
                        cropped
                    ) to photo
                }
            }

            val suggestion = pair.first.suggestion

            rawText = pair.first.rawText

            draft = ManualMedicineDraft(
                medicineName = suggestion.medicineName,
                strength = suggestion.strength,
                strengthUnit = suggestion.strengthUnit,
                dosageForm = "",
                note = "",
                imageUri = pair.second
            )

            state = ScanFlowState.RESULT

        } catch (_: TimeoutCancellationException) {
            error = "Đọc ảnh quá lâu. Hãy thử lại."
            state = ScanFlowState.PREVIEW

        } catch (cancelled: CancellationException) {
            throw cancelled

        } catch (exception: Exception) {
            error = exception.message
                ?: "Không đọc được ảnh thuốc."
            state = ScanFlowState.PREVIEW
        }
    }

    Box(
        modifier.fillMaxSize()
    ) {
        when (state) {

            ScanFlowState.CAMERA -> {
                ScanCameraScreen(
                    onCaptureClick = {
                        setImage(it, false)
                    },
                    onPickImageClick = {
                        openGallery()
                    },
                    onManualInputClick = onNavigateToManualInput,
                    onCloseClick = onClose
                )
            }

            ScanFlowState.PREVIEW -> {
                ScanPreviewScreen(
                    imageUri = imageUri?.let(Uri::parse),
                    isFromGallery = fromGallery,
                    cropRegion = crop,
                    onCropRegionChange = {
                        crop = it
                    },
                    onRetakeClick = {
                        if (fromGallery) {
                            openGallery()
                        } else {
                            state = ScanFlowState.CAMERA
                        }
                    },
                    onUsePhotoClick = {
                        error = null
                        processingPhoto = null
                        state = ScanFlowState.PROCESSING
                    },
                    onBackClick = {
                        goBack()
                    }
                )
            }

            ScanFlowState.PROCESSING -> {
                ScanProcessingScreen(
                    imageUri = processingPhoto,
                    onBackClick = {
                        goBack()
                    }
                )
            }

            ScanFlowState.RESULT -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Đọc chữ từ ảnh thuốc",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        "Thông tin OCR chỉ là gợi ý. " +
                                "Bước tiếp theo sẽ tìm sản phẩm thật."
                    )

                    Text(
                        "Tên gợi ý: " +
                                draft.medicineName.ifBlank {
                                    "Chưa xác định"
                                }
                    )

                    Text(
                        "Hàm lượng gợi ý: " +
                                draft.strength.ifBlank {
                                    "Chưa xác định"
                                }
                    )

                    Text(
                        "Dạng bào chế sẽ lấy từ sản phẩm " +
                                "tìm được, không đoán từ OCR."
                    )

                    Text("Nội dung đọc từ ảnh:")

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(
                                rememberScrollState()
                            )
                    ) {
                        Text(rawText)
                    }

                    Button(
                        onClick = {
                            state = ScanFlowState.LOOKUP
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tra cứu sản phẩm thuốc")
                    }

                    OutlinedButton(
                        onClick = {
                            goBack()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Quét lại / chọn lại vùng")
                    }
                }
            }

            ScanFlowState.LOOKUP -> {
                MedicineLookupChooser(
                    initialQuery = draft.medicineName,
                    scannedStrength = draft.strength,
                    scannedUnit = draft.strengthUnit,

                    onBack = {
                        goBack()
                    },

                    onChoose = { item ->
                        val strength =
                            parseCatalogStrength(
                                item.strength
                            )

                        onMedicineDraftReady(
                            draft.copy(
                                medicineName = item.name,
                                strength =
                                    strength?.amount.orEmpty(),
                                strengthUnit =
                                    strength?.unit ?: "mg",
                                dosageForm =
                                    mapCatalogDosageForm(
                                        item.dosage_form
                                    )
                            )
                        )
                    },

                    onSkip = {
                        onMedicineDraftReady(
                            draft.copy(
                                medicineName = "",
                                strength = "",
                                dosageForm = ""
                            )
                        )
                    }
                )
            }
        }
    }

    if (showGalleryInfo) {
        AlertDialog(
            onDismissRequest = {
                showGalleryInfo = false
            },
            title = {
                Text("Chọn ảnh thuốc")
            },
            text = {
                Text(
                    "MMRA đọc ảnh để gợi ý tìm thuốc. " +
                            "Ảnh chỉ được lưu khi bạn xác nhận lưu thuốc."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGalleryInfo = false
                        galleryAccepted = true

                        picker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts
                                    .PickVisualMedia.ImageOnly
                            )
                        )
                    }
                ) {
                    Text("Chọn ảnh")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showGalleryInfo = false
                    }
                ) {
                    Text("Để sau")
                }
            }
        )
    }

    error?.let { message ->
        AlertDialog(
            onDismissRequest = {
                error = null
            },
            title = {
                Text("Chưa đọc được ảnh")
            },
            text = {
                Text(message)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        error = null
                    }
                ) {
                    Text("Đóng")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        error = null
                        onNavigateToManualInput()
                    }
                ) {
                    Text("Nhập thủ công")
                }
            }
        )
    }
}
