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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.data.image.MedicinePhotoCodec
import com.example.project_lttbd.data.remote.MedicineLookupCandidate
import com.example.project_lttbd.data.remote.hasMultipleActiveIngredients
import com.example.project_lttbd.data.remote.isSafeSingleStrengthForAutoFill
import com.example.project_lttbd.data.remote.isSpecialReleaseMedicine
import com.example.project_lttbd.data.remote.mapCatalogDosageForm
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
 * Giữ thông tin đặc tính giải phóng đặc biệt hoặc thành phần đa hoạt chất / hàm lượng phức hợp
 * vào phần ghi chú để người dùng không bị mất dữ liệu và không nhầm với hàm lượng đơn.
 */
internal fun buildMedicineNote(
    existingNote: String,
    item: MedicineLookupCandidate
): String {
    val notes = mutableListOf<String>()
    if (existingNote.isNotBlank()) {
        notes.add(existingNote.trim())
    }

    val safeStrength = isSafeSingleStrengthForAutoFill(item)
    val isComplex = hasMultipleActiveIngredients(item.active_ingredient) ||
        (item.strength.isNotBlank() && safeStrength == null)

    if (isComplex) {
        val parts = buildList {
            if (item.active_ingredient.isNotBlank()) {
                add("Hoạt chất: ${item.active_ingredient}")
            }
            if (item.strength.isNotBlank()) {
                add("Hàm lượng: ${item.strength}")
            }
        }
        if (parts.isNotEmpty()) {
            notes.add("Thông tin theo nguồn (${item.source.ifBlank { "Danh mục" }}): ${parts.joinToString(" • ")}")
        }
    }

    if (isSpecialReleaseMedicine(item.name, item.dosage_form)) {
        val formLabel = item.dosage_form.ifBlank { item.name }
        notes.add(
            "Dạng bào chế theo nguồn: $formLabel\n" +
                "Lưu ý: Thuốc có đặc tính giải phóng đặc biệt. " +
                "Cần kiểm tra bao bì hoặc tờ hướng dẫn sử dụng trước khi lưu."
        )
    }

    return notes.joinToString("\n\n")
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

    var ocrCandidateNames by rememberSaveable {
        mutableStateOf("")
    }

    var ocrDetectedStrengths by rememberSaveable {
        mutableStateOf("")
    }

    var ocrAmbiguousStrength by rememberSaveable {
        mutableStateOf(false)
    }

    var ocrSpecialRelease by rememberSaveable {
        mutableStateOf(false)
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
        ocrCandidateNames = ""
        ocrDetectedStrengths = ""
        ocrAmbiguousStrength = false
        ocrSpecialRelease = false
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
            ocrCandidateNames = suggestion.candidateNames.joinToString("\n")
            ocrDetectedStrengths = suggestion.detectedStrengths.joinToString(", ")
            ocrAmbiguousStrength = suggestion.isAmbiguousStrength
            ocrSpecialRelease = suggestion.isSpecialRelease

            // OCR chỉ gợi ý tên và hàm lượng đơn rõ ràng.
            // Không dùng OCR để tự suy đoán dạng thuốc hoặc thuốc đa hoạt chất.
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
                val candidateList = remember(ocrCandidateNames) {
                    ocrCandidateNames
                        .split('\n')
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Đọc chữ từ ảnh thuốc",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Thông tin nhận diện chỉ là gợi ý. " +
                            "Bạn có thể chọn tên bên dưới hoặc sửa trực tiếp ở bước tra cứu.",
                        fontSize = 14.sp
                    )

                    Text(
                        text = "Tên thuốc gợi ý: " +
                            draft.medicineName.ifBlank { "Chưa xác định" },
                        fontWeight = FontWeight.SemiBold
                    )

                    if (candidateList.size > 1) {
                        Text(
                            text = "Chọn nhanh dòng chữ làm tên tra cứu:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            candidateList.take(3).forEach { candidate ->
                                FilterChip(
                                    selected = draft.medicineName == candidate,
                                    onClick = {
                                        draft = draft.copy(medicineName = candidate)
                                    },
                                    label = { Text(candidate) }
                                )
                            }
                        }
                    }

                    Text(
                        text = "Hàm lượng gợi ý: " +
                            when {
                                draft.strength.isNotBlank() ->
                                    "${draft.strength} ${draft.strengthUnit}"
                                ocrAmbiguousStrength && ocrDetectedStrengths.isNotBlank() ->
                                    "Nhiều hàm lượng / phức hợp ($ocrDetectedStrengths) — không tự điền"
                                else ->
                                    "Chưa xác định"
                            }
                    )

                    if (ocrSpecialRelease) {
                        Text(
                            text = "Lưu ý: Phát hiện dấu hiệu thuốc giải phóng kéo dài/đặc biệt trên bao bì.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Text(
                        text = "Dạng thuốc và hàm lượng chuẩn sẽ được đối chiếu từ danh mục thuốc.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Nội dung đọc từ ảnh:",
                        fontWeight = FontWeight.SemiBold
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
                        Text("Kiểm tra và tìm thuốc")
                    }

                    OutlinedButton(
                        onClick = {
                            goBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
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
                        // Chỉ tự điền hàm lượng khi nguồn có đúng 1 hoạt chất và 1 hàm lượng đơn chuẩn.
                        val safeStrength = isSafeSingleStrengthForAutoFill(item)

                        // Dạng thuốc ánh xạ từ dữ liệu danh mục; để trống nếu nguồn thiếu thông tin.
                        val dosageForm = mapCatalogDosageForm(item.dosage_form)

                        // Giữ đặc tính giải phóng đặc biệt và chi tiết phức hợp trong ghi chú.
                        val medicineNote = buildMedicineNote(
                            existingNote = draft.note,
                            item = item
                        )

                        onMedicineDraftReady(
                            draft.copy(
                                medicineName = item.name,
                                strength = safeStrength?.amount.orEmpty(),
                                strengthUnit = safeStrength?.unit ?: "mg",
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
