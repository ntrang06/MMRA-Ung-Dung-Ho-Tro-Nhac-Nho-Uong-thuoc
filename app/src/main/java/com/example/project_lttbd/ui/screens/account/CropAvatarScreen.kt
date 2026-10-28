package com.example.project_lttbd.ui.screens.account

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.R
import com.example.project_lttbd.data.model.UserProfileManager
import kotlin.math.min

private const val AVATAR_CROP_FRACTION = 0.84f

@Composable
fun CropAvatarScreen(
    modifier: Modifier = Modifier,
    inputBitmap: Bitmap? = UserProfileManager.tempCapturedBitmap,
    onBackClick: () -> Unit = {},
    onConfirmCrop: (Bitmap) -> Unit = {}
) {
    val primaryColor = Color(0xFF0F5B51)
    val lightBgColor = Color(0xFFE0F0EC)
    val darkTextColor = Color(0xFF0A3C35)
    val subTextColor = Color(0xFF5A6E6A)
    val borderColor = Color(0xFFE2E8E6)

    var rotation by remember(inputBitmap) {
        mutableIntStateOf(0)
    }
    var zoomScale by remember(inputBitmap) {
        mutableFloatStateOf(1f)
    }
    var cropOffset by remember(inputBitmap) {
        mutableStateOf(Offset.Zero)
    }
    var editorSide by remember {
        mutableFloatStateOf(0f)
    }
    var isSaving by remember(inputBitmap) {
        mutableStateOf(false)
    }
    var errorMessage by remember(inputBitmap) {
        mutableStateOf<String?>(null)
    }

    val rotatedBitmap = remember(inputBitmap, rotation) {
        inputBitmap?.let { source ->
            if (rotation == 0) {
                source
            } else {
                Bitmap.createBitmap(
                    source,
                    0,
                    0,
                    source.width,
                    source.height,
                    Matrix().apply {
                        postRotate(rotation.toFloat())
                    },
                    true
                )
            }
        }
    }

    fun boundedOffset(
        proposed: Offset,
        zoom: Float
    ): Offset {
        val bitmap = rotatedBitmap ?: return Offset.Zero
        val shortest = min(bitmap.width, bitmap.height).toFloat()

        val maxX = (
                bitmap.width / shortest * zoom - 1f
                ).coerceAtLeast(0f) / 2f

        val maxY = (
                bitmap.height / shortest * zoom - 1f
                ).coerceAtLeast(0f) / 2f

        return Offset(
            x = proposed.x.coerceIn(-maxX, maxX),
            y = proposed.y.coerceIn(-maxY, maxY)
        )
    }

    fun changeZoom(newZoom: Float) {
        zoomScale = newZoom.coerceIn(1f, 3f)
        cropOffset = boundedOffset(cropOffset, zoomScale)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFFAFDFB)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
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
                        text = "Cắt ảnh đại diện",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor
                    )
                    Text(
                        text = "Căn chỉnh trước khi lưu",
                        fontSize = 13.sp,
                        color = subTextColor
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Kéo để căn ảnh, dùng hai ngón tay " +
                        "để phóng to hoặc dùng thanh bên dưới.",
                fontSize = 14.sp,
                color = subTextColor,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF38524B),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                val bitmap = rotatedBitmap

                if (bitmap != null) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .onSizeChanged {
                                editorSide = it.width.toFloat()
                            }
                            .pointerInput(bitmap, editorSide) {
                                detectTransformGestures {
                                        _, pan, zoom, _ ->
                                    val newZoom =
                                        (zoomScale * zoom)
                                            .coerceIn(1f, 3f)

                                    val relativePan =
                                        if (editorSide > 0f) {
                                            Offset(
                                                pan.x / editorSide,
                                                pan.y / editorSide
                                            )
                                        } else {
                                            Offset.Zero
                                        }

                                    zoomScale = newZoom
                                    cropOffset = boundedOffset(
                                        cropOffset + relativePan,
                                        newZoom
                                    )
                                }
                            }
                    ) {
                        val side = size.minDimension

                        drawIntoCanvas { canvas ->
                            drawAvatarSource(
                                canvas = canvas.nativeCanvas,
                                source = bitmap,
                                viewportSide = side,
                                zoom = zoomScale,
                                offset = cropOffset
                            )
                        }

                        val radius =
                            side * AVATAR_CROP_FRACTION / 2f

                        val center = Offset(side / 2f, side / 2f)

                        val overlay = Path().apply {
                            fillType = PathFillType.EvenOdd

                            addRect(
                                Rect(0f, 0f, side, side)
                            )

                            addOval(
                                Rect(
                                    center.x - radius,
                                    center.y - radius,
                                    center.x + radius,
                                    center.y + radius
                                )
                            )
                        }

                        drawPath(
                            path = overlay,
                            color = Color.Black.copy(alpha = 0.45f)
                        )

                        drawCircle(
                            color = Color.White,
                            radius = radius,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Chưa có ảnh để cắt. " +
                                    "Hãy quay lại chụp hoặc chọn ảnh.",
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        changeZoom(zoomScale - 0.2f)
                    },
                    enabled = rotatedBitmap != null && !isSaving
                ) {
                    Text(
                        text = "−",
                        fontSize = 24.sp,
                        color = primaryColor
                    )
                }

                Slider(
                    value = zoomScale,
                    onValueChange = { changeZoom(it) },
                    valueRange = 1f..3f,
                    enabled = rotatedBitmap != null && !isSaving,
                    colors = SliderDefaults.colors(
                        thumbColor = primaryColor,
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = Color(0xFFE2E8E6)
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        changeZoom(zoomScale + 0.2f)
                    },
                    enabled = rotatedBitmap != null && !isSaving
                ) {
                    Text(
                        text = "+",
                        fontSize = 24.sp,
                        color = primaryColor
                    )
                }

                Surface(
                    onClick = {
                        rotation = (rotation + 90) % 360
                        zoomScale = 1f
                        cropOffset = Offset.Zero
                    },
                    enabled = rotatedBitmap != null && !isSaving,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF7F4)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 10.dp
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_rotate),
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Xoay",
                            color = primaryColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            errorMessage?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    color = Color(0xFFD32F2F),
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    val bitmap = rotatedBitmap

                    if (bitmap != null) {
                        Canvas(Modifier.size(52.dp)) {
                            drawIntoCanvas { canvas ->
                                drawAvatarCircle(
                                    canvas = canvas.nativeCanvas,
                                    source = bitmap,
                                    outputSide = size.minDimension,
                                    zoom = zoomScale,
                                    offset = cropOffset
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = lightBgColor,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = UserProfileManager.getInitials(),
                                    color = primaryColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Xem trước ảnh đại diện",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkTextColor
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = UserProfileManager.name,
                            fontSize = 13.sp,
                            color = subTextColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val bitmap = rotatedBitmap

                    if (bitmap != null && !isSaving) {
                        isSaving = true
                        errorMessage = null

                        try {
                            val output = Bitmap.createBitmap(
                                512,
                                512,
                                Bitmap.Config.ARGB_8888
                            )

                            drawAvatarCircle(
                                canvas = AndroidCanvas(output),
                                source = bitmap,
                                outputSide = 512f,
                                zoom = zoomScale,
                                offset = cropOffset
                            )

                            // MMRANavHost đang chịu trách nhiệm lưu ảnh.
                            onConfirmCrop(output)
                        } catch (_: Exception) {
                            isSaving = false
                            errorMessage =
                                "Không xử lý được ảnh. Hãy thử lại."
                        }
                    }
                },
                enabled = rotatedBitmap != null && !isSaving,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(
                            R.drawable.ic_check_circle
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = if (isSaving) {
                        "Đang xử lý…"
                    } else {
                        "Sử dụng ảnh"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Ảnh đại diện sẽ lấy phần bên trong khung tròn.",
                fontSize = 12.sp,
                color = subTextColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Vẽ ảnh vào khung vuông.
 * Offset được lưu theo tỷ lệ cạnh khung để dùng chung cho
 * màn chỉnh sửa, ảnh xem trước và ảnh xuất ra.
 */
private fun drawAvatarSource(
    canvas: AndroidCanvas,
    source: Bitmap,
    viewportSide: Float,
    zoom: Float,
    offset: Offset
) {
    val scale = viewportSide /
            min(source.width, source.height).toFloat() * zoom

    val left = (viewportSide - source.width * scale) / 2f +
            offset.x * viewportSide

    val top = (viewportSide - source.height * scale) / 2f +
            offset.y * viewportSide

    val matrix = Matrix().apply {
        setScale(scale, scale)
        postTranslate(left, top)
    }

    canvas.drawBitmap(
        source,
        matrix,
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    )
}

/**
 * Xuất đúng vùng tròn ở giữa khung chỉnh sửa.
 */
private fun drawAvatarCircle(
    canvas: AndroidCanvas,
    source: Bitmap,
    outputSide: Float,
    zoom: Float,
    offset: Offset
) {
    val circle = android.graphics.Path().apply {
        addCircle(
            outputSide / 2f,
            outputSide / 2f,
            outputSide / 2f,
            android.graphics.Path.Direction.CW
        )
    }

    val viewportSide = outputSide / AVATAR_CROP_FRACTION
    val translation = (outputSide - viewportSide) / 2f

    canvas.save()
    canvas.clipPath(circle)
    canvas.translate(translation, translation)

    drawAvatarSource(
        canvas = canvas,
        source = source,
        viewportSide = viewportSide,
        zoom = zoom,
        offset = offset
    )

    canvas.restore()
}

@Preview(showBackground = true)
@Composable
fun CropAvatarScreenPreview() {
    CropAvatarScreen()
}