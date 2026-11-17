package com.example.project_lttbd.ui.screens.medicine

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.example.project_lttbd.data.image.MedicinePhotoCodec
import com.example.project_lttbd.ui.components.MedicinePhoto
import com.example.project_lttbd.ui.components.MedicinePhotoViewer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun MedicinePhotoField(
    source: String?,
    savedSource: String?,
    onChange: (String?) -> Unit,
    onBusyChange: (Boolean) -> Unit
) {
    val context = LocalContext.current.applicationContext
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var processing by remember { mutableStateOf(false) }
    var showViewer by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = if (!LocalInspectionMode.current) {
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null && !processing) {
                processing = true
                onBusyChange(true)
                error = null

                scope.launch {
                    try {
                        val encoded = MedicinePhotoCodec.prepare(
                            context,
                            uri
                        )

                        onChange(encoded)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (exception: Exception) {
                        error = exception.message
                            ?: "Chưa đọc được ảnh. Vui lòng chọn lại."
                    } finally {
                        processing = false
                        onBusyChange(false)
                    }
                }
            }
        }
    } else {
        null
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFD7E2DF))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(76.dp)
                        .clickable(
                            enabled = source != null && !processing
                        ) {
                            showViewer = true
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFD9EEEA)
                ) {
                    when {
                        processing -> {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }

                        source != null -> {
                            MedicinePhoto(
                                source = source,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        else -> {
                            Box(contentAlignment = Alignment.Center) {
                                ManualMedicineIcon(
                                    kind = ManualIconKind.CAMERA,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Ảnh thuốc",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = when {
                            processing -> "Đang xử lý ảnh…"
                            source != savedSource ->
                                "Thay đổi ảnh chưa lưu"
                            source != null -> "Ảnh hiện tại"
                            else -> "Không bắt buộc"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF526565)
                    )

                    OutlinedButton(
                        enabled = !processing,
                        onClick = {
                            focusManager.clearFocus()
                            picker?.launch("image/*")
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (source == null) "Thêm ảnh" else "Đổi ảnh"
                        )
                    }
                }
            }

            if (source != null && !processing) {
                Row {
                    TextButton(
                        onClick = { showViewer = true }
                    ) {
                        Text("Xem ảnh")
                    }

                    TextButton(
                        onClick = {
                            showViewer = false
                            onChange(null)
                        }
                    ) {
                        Text(
                            text = "Xóa ảnh",
                            color = Color(0xFFB3261E)
                        )
                    }
                }
            }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showViewer && source != null) {
        MedicinePhotoViewer(
            source = source,
            onClose = { showViewer = false }
        )
    }
}