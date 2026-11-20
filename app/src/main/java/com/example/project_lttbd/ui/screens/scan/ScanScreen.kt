
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
    CAMERA,
    PREVIEW,
    PROCESSING,
    RESULT,
    LOOKUP
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
            listOf(
                it.left,
                it.top,
                it.right,
                it.bottom
            )
        },
        restore = {
            ScanCropRegion(
                it[0],
                it[1],
                it[2],
                it[3]
            ).normalized()
        }
    )

/**
 * Giữ thông tin dạng thuốc đặc biệt để người dùng
 * không nhầm với dạng thuốc thông thường.
 */
private fun buildMedicineNote(
    existingNote: String,
    catalogForm: String
): String {
    val specialReleaseKeywords = listOf(
        "extended-release",
        "modified-release",
        "sustained-release",
        "delayed-release",
        "prolonged-release",
        "controlled-release",
        "extended release",
        "modified release",
        "sustained release",
        "delayed release",
        "prolonged release",
        "controlled release",
        "giai phong keo dai",
        "giai phong cham"
    )

    val isSpecialRelease = specialReleaseKeywords.any {
        catalogForm.contains(
            it,
            ignoreCase = true
        )
    }

    if (!isSpecialRelease) {
        return existingNote
    }

    return buildString {
        if (existingNote.isNotBlank()) {
            appendLine(existingNote.trim())
            appendLine()
        }

        appendLine(
            "Dạng bào chế theo nguồn: $catalogForm"
        )

        append(
            "Lưu ý: Thuốc có đặc tính giải phóng " +
                    "đặc biệt. Cần kiểm tra bao bì hoặc " +
                    "tờ hướng dẫn sử dụng trước khi lưu."
        )
    }
}

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
                medicineName = "",
                strength = "",
                strengthUnit = "mg",
                dosageForm = "",
                note = "",
                imageUri = null
            )
        )
    }

    fun setImage(uri: Uri, gallery: Boolean) {
        imageUri = uri.toString()
        fromGallery = gallery
        rawText = ""
        error = null

        draft = ManualMedicineDraft(
            medicineName = "",
            strength = "",
            strengthUnit = "mg",
            dosageForm = "",
            note = "",
            imageUri = null
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
            ScanFlowState.CAMERA -> {
                onClose()
            }

            ScanFlowState.PREVIEW -> {
                state = ScanFlowState.CAMERA
            }

            ScanFlowState.PROCESSING,
            ScanFlowState.RESULT -> {
                state = ScanFlowState.PREVIEW
            }

            ScanFlowState.LOOKUP -> {
                state = ScanFlowState.RESULT
            }
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

                    val photo =
                        MedicinePhotoCodec.prepare(
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

            // OCR chỉ gợi ý tên và hàm lượng.
            // Không dùng OCR để tự xác định dạng thuốc.
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
        modifier = modifier.fillMaxSize()
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
                        style =
                            MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = "Thông tin nhận diện chỉ là " +
                                "gợi ý. Bạn có thể kiểm tra " +
                                "và sửa tên thuốc ở bước tiếp theo."
                    )

                    Text(
                        text = "Tên thuốc gợi ý: " +
                                draft.medicineName.ifBlank {
                                    "Chưa xác định"
                                }
                    )

                    Text(
                        text = "Hàm lượng gợi ý: " +
                                if (draft.strength.isNotBlank()) {
                                    "${draft.strength} ${draft.strengthUnit}"
                                } else {
                                    "Chưa xác định"
                                }
                    )

                    Text(
                        text = "Dạng thuốc sẽ được lấy " +
                                "từ dữ liệu sản phẩm tìm được."
                    )

                    Text(
                        text = "Nội dung đọc từ ảnh:"
                    )

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
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                    ) {
                        Text(
                            "Kiểm tra và tìm thuốc"
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            goBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                    ) {
                        Text(
                            "Quét lại / chọn lại vùng"
                        )
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

                        // Hàm lượng từ sản phẩm đã chọn.
                        val strength = parseCatalogStrength(
                            item.strength
                        )

                        // Dạng thuốc từ dữ liệu danh mục.
                        val dosageForm = mapCatalogDosageForm(
                            item.dosage_form
                        )

                        // Giữ đặc tính giải phóng đặc biệt
                        // trong ghi chú nếu nguồn có thông tin.
                        val medicineNote = buildMedicineNote(
                            existingNote = draft.note,
                            catalogForm = item.dosage_form
                        )

                        onMedicineDraftReady(
                            draft.copy(
                                medicineName = item.name,
                                strength =
                                    strength?.amount.orEmpty(),
                                strengthUnit =
                                    strength?.unit ?: "mg",
                                dosageForm = dosageForm,
                                note = medicineNote
                            )
                        )
                    },

                    onSkip = {
                        onMedicineDraftReady(
                            draft.copy(
                                medicineName = "",
                                strength = "",
                                strengthUnit = "mg",
                                dosageForm = "",
                                note = ""
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
                            "Ảnh chỉ được lưu khi bạn xác nhận " +
                            "lưu thông tin thuốc."
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
