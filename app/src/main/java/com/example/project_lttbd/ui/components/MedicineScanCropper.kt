package com.example.project_lttbd.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.project_lttbd.data.image.MedicinePhotoCodec
import com.example.project_lttbd.data.scan.ScanCropRegion
import kotlinx.coroutines.CancellationException
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun MedicineScanCropper(
    source: String?,
    region: ScanCropRegion,
    onRegionChange: (ScanCropRegion) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext

    var failed by remember(source) {
        mutableStateOf(false)
    }

    val bitmap by produceState<Bitmap?>(null, source) {
        value = null
        failed = false

        value = try {
            requireNotNull(source)
            MedicinePhotoCodec.load(context, source, 1024)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failed = true
            null
        }
    }

    val currentRegion by rememberUpdatedState(region)
    val currentChange by rememberUpdatedState(onRegionChange)

    var canvasSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    val touchRadius = with(LocalDensity.current) {
        36.dp.toPx()
    }

    // Chừa viền để nút kéo không bị cắt bởi góc bo tròn.
    val imageInset = with(LocalDensity.current) {
        16.dp.toPx()
    }

    Box(
        modifier = modifier.background(Color(0xFF142523)),
        contentAlignment = Alignment.Center
    ) {
        val photo = bitmap

        if (photo == null) {
            if (failed) {
                Text(
                    text = "Không mở được ảnh. Vui lòng chọn lại.",
                    color = Color.White
                )
            } else {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            val image = remember(photo) {
                photo.asImageBitmap()
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        canvasSize = it
                    }
                    .pointerInput(
                        photo,
                        canvasSize,
                        touchRadius,
                        imageInset
                    ) {
                        var mode = -1
                        var dragging = currentRegion.normalized()

                        detectDragGestures(
                            onDragStart = { position ->
                                val bounds = scanCropImageBounds(
                                    size = canvasSize,
                                    width = photo.width,
                                    height = photo.height,
                                    inset = imageInset
                                )

                                dragging = currentRegion.normalized()

                                val crop = regionRect(bounds, dragging)

                                val corners = listOf(
                                    crop.topLeft,
                                    crop.topRight,
                                    crop.bottomRight,
                                    crop.bottomLeft
                                )

                                val nearest = corners.indices
                                    .minByOrNull {
                                        (corners[it] - position)
                                            .getDistance()
                                    } ?: 0

                                mode = if (
                                    (corners[nearest] - position)
                                        .getDistance() <= touchRadius
                                ) {
                                    nearest
                                } else if (crop.contains(position)) {
                                    4
                                } else {
                                    -1
                                }
                            },
                            onDragEnd = {
                                mode = -1
                            },
                            onDragCancel = {
                                mode = -1
                            },
                            onDrag = { change, amount ->
                                val bounds = scanCropImageBounds(
                                    size = canvasSize,
                                    width = photo.width,
                                    height = photo.height,
                                    inset = imageInset
                                )

                                if (
                                    mode >= 0 &&
                                    bounds.width > 0 &&
                                    bounds.height > 0
                                ) {
                                    change.consume()

                                    val dx = amount.x / bounds.width
                                    val dy = amount.y / bounds.height

                                    dragging = if (mode == 4) {
                                        dragging.moved(dx, dy)
                                    } else {
                                        dragging.resized(mode, dx, dy)
                                    }

                                    currentChange(dragging)
                                }
                            }
                        )
                    }
            ) {
                val bounds = scanCropImageBounds(
                    size = IntSize(
                        size.width.roundToInt(),
                        size.height.roundToInt()
                    ),
                    width = photo.width,
                    height = photo.height,
                    inset = imageInset
                )

                drawImage(
                    image = image,
                    dstOffset = IntOffset(
                        bounds.left.roundToInt(),
                        bounds.top.roundToInt()
                    ),
                    dstSize = IntSize(
                        bounds.width.roundToInt().coerceAtLeast(1),
                        bounds.height.roundToInt().coerceAtLeast(1)
                    )
                )

                val crop = regionRect(
                    bounds,
                    region.normalized()
                )

                val shade = Color.Black.copy(alpha = 0.6f)

                drawRect(
                    color = shade,
                    topLeft = Offset(bounds.left, bounds.top),
                    size = Size(
                        bounds.width,
                        (crop.top - bounds.top).coerceAtLeast(0f)
                    )
                )

                drawRect(
                    color = shade,
                    topLeft = Offset(bounds.left, crop.bottom),
                    size = Size(
                        bounds.width,
                        (bounds.bottom - crop.bottom).coerceAtLeast(0f)
                    )
                )

                drawRect(
                    color = shade,
                    topLeft = Offset(bounds.left, crop.top),
                    size = Size(
                        (crop.left - bounds.left).coerceAtLeast(0f),
                        crop.height
                    )
                )

                drawRect(
                    color = shade,
                    topLeft = Offset(crop.right, crop.top),
                    size = Size(
                        (bounds.right - crop.right).coerceAtLeast(0f),
                        crop.height
                    )
                )

                drawRect(
                    color = Color.White,
                    topLeft = crop.topLeft,
                    size = crop.size,
                    style = Stroke(2.dp.toPx())
                )

                val corners = listOf(
                    crop.topLeft,
                    crop.topRight,
                    crop.bottomRight,
                    crop.bottomLeft
                )

                for (corner in corners) {
                    drawCircle(
                        color = Color(0xFF126B62),
                        radius = 8.dp.toPx(),
                        center = corner
                    )

                    drawCircle(
                        color = Color.White,
                        radius = 8.dp.toPx(),
                        center = corner,
                        style = Stroke(2.dp.toPx())
                    )
                }
            }
        }
    }
}

internal fun scanCropImageBounds(
    size: IntSize,
    width: Int,
    height: Int,
    inset: Float
): Rect {
    if (size.width <= 0 || size.height <= 0) {
        return Rect.Zero
    }

    val padding = min(
        inset.coerceAtLeast(0f),
        min(size.width, size.height) / 4f
    )

    val scale = min(
        (size.width - padding * 2) / width,
        (size.height - padding * 2) / height
    )

    val w = width * scale
    val h = height * scale
    val x = (size.width - w) / 2f
    val y = (size.height - h) / 2f

    return Rect(x, y, x + w, y + h)
}

private fun regionRect(
    bounds: Rect,
    region: ScanCropRegion
): Rect {
    return Rect(
        left = bounds.left + region.left * bounds.width,
        top = bounds.top + region.top * bounds.height,
        right = bounds.left + region.right * bounds.width,
        bottom = bounds.top + region.bottom * bounds.height
    )
}