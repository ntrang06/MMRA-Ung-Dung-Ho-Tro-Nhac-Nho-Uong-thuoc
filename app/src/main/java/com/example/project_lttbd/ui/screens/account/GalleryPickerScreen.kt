package com.example.project_lttbd.ui.screens.account

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

@Composable
fun GalleryPickerScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onPhotoSelected: (Bitmap) -> Unit = {},
    onCancelClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    val primaryColor = Color(0xFF0F5B51)
    val lightBgColor = Color(0xFFE0F0EC)
    val darkTextColor = Color(0xFF0A3C35)
    val subTextColor = Color(0xFF5A6E6A)
    val borderColor = Color(0xFFE2E8E6)

    var selectedUri by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    var pickerOpened by rememberSaveable {
        mutableStateOf(false)
    }

    var selectedBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }
    var isLoading by remember {
        mutableStateOf(false)
    }
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri.toString()
        }
    }

    val openPicker: () -> Unit = {
        try {
            errorMessage = null
            photoPicker.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        } catch (_: Exception) {
            errorMessage =
                "Không mở được trình chọn ảnh trên thiết bị."
        }
    }

    LaunchedEffect(selectedUri) {
        selectedBitmap = null
        errorMessage = null

        val uriText = selectedUri

        if (uriText != null) {
            isLoading = true

            try {
                selectedBitmap = withContext(Dispatchers.IO) {
                    loadAvatarGalleryBitmap(
                        context.applicationContext,
                        Uri.parse(uriText)
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                errorMessage =
                    "Không đọc được ảnh này. Hãy chọn ảnh khác."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!isPreview && !pickerOpened) {
            pickerOpened = true
            openPicker()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFFAFDFB)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    onClick = onBackClick,
                    shape = CircleShape,
                    color = lightBgColor,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Quay lại",
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Chọn ảnh đại diện",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor
                    )
                    Text(
                        text = "Bước 1 / 2 • Chọn từ thư viện",
                        fontSize = 13.sp,
                        color = subTextColor
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Surface(
                onClick = openPicker,
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 14.dp
                    )
                ) {
                    Text(
                        text = if (selectedBitmap == null) {
                            "Mở thư viện ảnh"
                        } else {
                            "Chọn ảnh khác"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor
                    )
                    Icon(
                        painter = painterResource(
                            R.drawable.ic_chevron_right
                        ),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Chọn một ảnh có khuôn mặt rõ nét.",
                fontSize = 14.sp,
                color = subTextColor
            )

            Spacer(Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFEFF6F4),
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    val bitmap = selectedBitmap

                    when {
                        isLoading -> {
                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = primaryColor
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Đang đọc ảnh…",
                                    color = subTextColor
                                )
                            }
                        }

                        bitmap != null -> {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Ảnh đã chọn",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        else -> {
                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        R.drawable.ic_user
                                    ),
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(64.dp)
                                )

                                Spacer(Modifier.height(12.dp))

                                Text(
                                    text = "Chưa chọn ảnh",
                                    fontWeight = FontWeight.Bold,
                                    color = darkTextColor
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = "Bấm “Mở thư viện ảnh” " +
                                            "để chọn ảnh trên điện thoại.",
                                    color = subTextColor,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            errorMessage?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = message,
                    color = Color(0xFFD32F2F),
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            val bitmap = selectedBitmap

            if (bitmap != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = lightBgColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        )

                        Spacer(Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Đã chọn 1 ảnh",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = darkTextColor
                            )
                            Text(
                                text = "Tiếp tục để căn chỉnh ảnh đại diện.",
                                fontSize = 12.sp,
                                color = subTextColor
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            Button(
                onClick = {
                    selectedBitmap?.let(onPhotoSelected)
                },
                enabled = selectedBitmap != null && !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_crop),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Tiếp tục cắt ảnh",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                onClick = onCancelClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Hủy",
                    color = subTextColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun loadAvatarGalleryBitmap(
    context: Context,
    uri: Uri
): Bitmap {
    val resolver = context.contentResolver

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(resolver, uri)

        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE

            val ratio = min(
                1f,
                1600f / max(
                    info.size.width,
                    info.size.height
                ).toFloat()
            )

            decoder.setTargetSize(
                (info.size.width * ratio).toInt().coerceAtLeast(1),
                (info.size.height * ratio).toInt().coerceAtLeast(1)
            )
        }
    }

    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
    } ?: error("Không mở được ảnh")

    require(bounds.outWidth > 0 && bounds.outHeight > 0) {
        "Ảnh không hợp lệ"
    }

    var sampleSize = 1

    while (
        max(bounds.outWidth, bounds.outHeight) / sampleSize > 1600
    ) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }

    val bitmap = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    } ?: error("Không đọc được ảnh")

    val orientation = runCatching {
        resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    val matrix = Matrix()

    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL ->
            matrix.setScale(-1f, 1f)

        ExifInterface.ORIENTATION_ROTATE_180 ->
            matrix.setRotate(180f)

        ExifInterface.ORIENTATION_FLIP_VERTICAL ->
            matrix.setScale(1f, -1f)

        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.setRotate(90f)
            matrix.postScale(-1f, 1f)
        }

        ExifInterface.ORIENTATION_ROTATE_90 ->
            matrix.setRotate(90f)

        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.setRotate(-90f)
            matrix.postScale(-1f, 1f)
        }

        ExifInterface.ORIENTATION_ROTATE_270 ->
            matrix.setRotate(270f)

        else -> return bitmap
    }

    return Bitmap.createBitmap(
        bitmap,
        0,
        0,
        bitmap.width,
        bitmap.height,
        matrix,
        true
    )
}

@Preview(showBackground = true)
@Composable
fun GalleryPickerScreenPreview() {
    GalleryPickerScreen()
}