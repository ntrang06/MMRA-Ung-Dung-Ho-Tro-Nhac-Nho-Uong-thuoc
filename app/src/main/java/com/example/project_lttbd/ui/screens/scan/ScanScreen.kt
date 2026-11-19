package com.example.project_lttbd.ui.screens.scan

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.project_lttbd.data.image.MedicinePhotoCodec
import com.example.project_lttbd.data.scan.MedicineOcrReader
import com.example.project_lttbd.data.scan.MedicineScanCrop
import com.example.project_lttbd.data.scan.ScanCropRegion
import com.example.project_lttbd.ui.components.MedicinePhoto
import com.example.project_lttbd.ui.screens.medicine.ManualMedicineDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

enum class ScanFlowState {
    CAMERA, PREVIEW, PROCESSING, RESULT
}

private val OcrDraftSaver = listSaver<ManualMedicineDraft, String>(
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

private val ScanCropSaver = listSaver<ScanCropRegion, Float>(
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

    var flowState by rememberSaveable {
        mutableStateOf(ScanFlowState.CAMERA)
    }

    var imageUri by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var isFromGallery by rememberSaveable {
        mutableStateOf(false)
    }

    var processingPhoto by remember(imageUri) {
        mutableStateOf<String?>(null)
    }

    var cropRegion by rememberSaveable(
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

    var showPhotoInfo by rememberSaveable {
        mutableStateOf(false)
    }

    var photoInfoAccepted by rememberSaveable {
        mutableStateOf(false)
    }

    var draft by rememberSaveable(
        stateSaver = OcrDraftSaver
    ) {
        mutableStateOf(
            ManualMedicineDraft("", "", "mg", "", "", null)
        )
    }

    fun selectImage(
        uri: Uri,
        gallery: Boolean
    ) {
        imageUri = uri.toString()
        isFromGallery = gallery
        rawText = ""
        error = null

        draft = ManualMedicineDraft(
            "", "", "mg", "", "", null
        )

        flowState = ScanFlowState.PREVIEW
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectImage(uri, true)
        }
    }

    fun openPhotoPicker() {
        picker.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    fun pickPhoto() {
        if (photoInfoAccepted) {
            openPhotoPicker()
        } else {
            showPhotoInfo = true
        }
    }

    fun goBack() {
        when (flowState) {
            ScanFlowState.CAMERA -> {
                onClose()
            }

            ScanFlowState.PREVIEW -> {
                flowState = ScanFlowState.CAMERA
            }

            ScanFlowState.PROCESSING,
            ScanFlowState.RESULT -> {
                flowState = ScanFlowState.PREVIEW
            }
        }
    }

    BackHandler(
        enabled = flowState != ScanFlowState.CAMERA
    ) {
        goBack()
    }

    LaunchedEffect(
        flowState,
        imageUri,
        cropRegion
    ) {
        if (flowState != ScanFlowState.PROCESSING) {
            return@LaunchedEffect
        }

        try {
            val uri = Uri.parse(
                requireNotNull(imageUri) {
                    "Vui lòng chọn lại ảnh."
                }
            )

            val prepared = withTimeout(30_000) {
                MedicineScanCrop.useRegion(
                    context = context,
                    uri = uri,
                    region = cropRegion
                ) { croppedUri ->
                    val photo = MedicinePhotoCodec.prepare(
                        context,
                        croppedUri
                    )

                    withContext(Dispatchers.Main) {
                        processingPhoto = photo
                    }

                    val result = MedicineOcrReader.read(
                        context,
                        croppedUri
                    )

                    result to photo
                }
            }

            currentCoroutineContext().ensureActive()

            val suggestion = prepared.first.suggestion

            rawText = prepared.first.rawText

            draft = ManualMedicineDraft(
                medicineName = suggestion.medicineName,
                strength = suggestion.strength,
                strengthUnit = suggestion.strengthUnit,
                dosageForm = suggestion.dosageForm,
                note = "",
                imageUri = prepared.second
            )

            flowState = ScanFlowState.RESULT
        } catch (_: TimeoutCancellationException) {
            error =
                "Đọc ảnh quá lâu. Vui lòng chọn ảnh khác hoặc thử lại."

            flowState = ScanFlowState.PREVIEW
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            error = if (
                exception is IllegalArgumentException ||
                exception is IllegalStateException
            ) {
                exception.message
                    ?: "Chưa xử lý được ảnh. Vui lòng chọn lại."
            } else {
                "Chưa đọc được ảnh. Hãy thử lại hoặc chọn ảnh khác."
            }

            flowState = ScanFlowState.PREVIEW
        }
    }

    Box(modifier.fillMaxSize()) {
        when (flowState) {
            ScanFlowState.CAMERA -> {
                ScanCameraScreen(
                    onCaptureClick = {
                        selectImage(it, false)
                    },
                    onPickImageClick = {
                        pickPhoto()
                    },
                    onManualInputClick = onNavigateToManualInput,
                    onCloseClick = onClose
                )
            }

            ScanFlowState.PREVIEW -> {
                ScanPreviewScreen(
                    imageUri = imageUri?.let(Uri::parse),
                    isFromGallery = isFromGallery,
                    cropRegion = cropRegion,
                    onCropRegionChange = {
                        cropRegion = it
                    },
                    onRetakeClick = {
                        if (isFromGallery) {
                            pickPhoto()
                        } else {
                            flowState = ScanFlowState.CAMERA
                        }
                    },
                    onUsePhotoClick = {
                        processingPhoto = null
                        error = null
                        flowState = ScanFlowState.PROCESSING
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
                OcrResultContent(
                    rawText = rawText,
                    draft = draft,
                    onBack = {
                        goBack()
                    },
                    onContinue = {
                        onMedicineDraftReady(draft)
                    },
                    onRescan = {
                        flowState = ScanFlowState.CAMERA
                    }
                )
            }
        }
    }

    if (showPhotoInfo) {
        AlertDialog(
            onDismissRequest = {
                showPhotoInfo = false
            },
            title = {
                Text("Chọn ảnh thuốc")
            },
            text = {
                Text(
                    "MMRA đọc ảnh bạn chọn để gợi ý thông tin thuốc. " +
                            "Ảnh chỉ được lưu cùng thuốc khi bạn bấm Lưu."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPhotoInfo = false
                        photoInfoAccepted = true
                        openPhotoPicker()
                    }
                ) {
                    Text("Chọn ảnh")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPhotoInfo = false
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

@Composable
private fun OcrResultContent(
    rawText: String,
    draft: ManualMedicineDraft,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onRescan: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF6FAF9))
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.padding(8.dp)
        ) {
            Text("‹  Quay lại")
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Kết quả đọc chữ",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = RoundedCornerShape(18.dp)
            ) {
                MedicinePhoto(
                    source = draft.imageUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    contentScale = ContentScale.Fit,
                    maxSide = 1024
                )
            }

            Surface(
                color = Color(0xFFFFF1D6),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Thông tin gợi ý từ chữ trên ảnh. " +
                            "Hãy đối chiếu với hộp/vỉ và kiểm tra lại " +
                            "trước khi lưu. Đây chưa phải xác minh thuốc.",
                    modifier = Modifier.padding(14.dp)
                )
            }

            Text(
                text = "Tên gợi ý: ${
                    draft.medicineName.ifBlank {
                        "Cần nhập ở bước sau"
                    }
                }"
            )

            Text(
                text = "Hàm lượng gợi ý: ${
                    if (draft.strength.isBlank()) {
                        "Cần kiểm tra ở bước sau"
                    } else {
                        "${draft.strength} ${draft.strengthUnit}"
                    }
                }"
            )

            Text(
                text = "Chữ trong vùng đã chọn",
                fontWeight = FontWeight.Bold
            )

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = rawText,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        Surface(
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF126B62)
                    )
                ) {
                    Text("Kiểm tra thông tin thuốc")
                }

                TextButton(
                    onClick = onRescan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Quét lại")
                }
            }
        }
    }
}