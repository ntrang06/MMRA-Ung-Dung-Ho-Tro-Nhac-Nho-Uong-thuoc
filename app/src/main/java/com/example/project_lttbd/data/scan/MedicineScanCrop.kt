package com.example.project_lttbd.data.scan

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.project_lttbd.data.image.MedicinePhotoCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.ceil
import kotlin.math.floor

data class ScanPixelRegion(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
)

data class ScanCropRegion(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    fun normalized(): ScanCropRegion {
        if (
            listOf(left, top, right, bottom)
                .any { !it.isFinite() }
        ) {
            return Full
        }

        val l = left.coerceIn(0f, 1f - MIN_SIZE)
        val t = top.coerceIn(0f, 1f - MIN_SIZE)

        return ScanCropRegion(
            left = l,
            top = t,
            right = right.coerceIn(l + MIN_SIZE, 1f),
            bottom = bottom.coerceIn(t + MIN_SIZE, 1f)
        )
    }

    fun moved(
        dx: Float,
        dy: Float
    ): ScanCropRegion {
        val r = normalized()
        val w = r.right - r.left
        val h = r.bottom - r.top

        val l = (r.left + dx).coerceIn(
            0f,
            (1f - w).coerceAtLeast(0f)
        )

        val t = (r.top + dy).coerceIn(
            0f,
            (1f - h).coerceAtLeast(0f)
        )

        return ScanCropRegion(
            l, t, l + w, t + h
        ).normalized()
    }

    // Góc: trên trái, trên phải, dưới phải, dưới trái.
    fun resized(
        corner: Int,
        dx: Float,
        dy: Float
    ): ScanCropRegion {
        val r = normalized()

        return when (corner) {
            0 -> r.copy(
                left = (r.left + dx)
                    .coerceIn(0f, r.right - MIN_SIZE),
                top = (r.top + dy)
                    .coerceIn(0f, r.bottom - MIN_SIZE)
            )

            1 -> r.copy(
                right = (r.right + dx)
                    .coerceIn(r.left + MIN_SIZE, 1f),
                top = (r.top + dy)
                    .coerceIn(0f, r.bottom - MIN_SIZE)
            )

            2 -> r.copy(
                right = (r.right + dx)
                    .coerceIn(r.left + MIN_SIZE, 1f),
                bottom = (r.bottom + dy)
                    .coerceIn(r.top + MIN_SIZE, 1f)
            )

            3 -> r.copy(
                left = (r.left + dx)
                    .coerceIn(0f, r.right - MIN_SIZE),
                bottom = (r.bottom + dy)
                    .coerceIn(r.top + MIN_SIZE, 1f)
            )

            else -> r
        }.normalized()
    }

    fun pixels(
        width: Int,
        height: Int
    ): ScanPixelRegion {
        require(width > 0 && height > 0)

        val r = normalized()

        val l = floor(r.left * width)
            .toInt()
            .coerceIn(0, width - 1)

        val t = floor(r.top * height)
            .toInt()
            .coerceIn(0, height - 1)

        val endX = ceil(r.right * width)
            .toInt()
            .coerceIn(l + 1, width)

        val endY = ceil(r.bottom * height)
            .toInt()
            .coerceIn(t + 1, height)

        return ScanPixelRegion(
            left = l,
            top = t,
            width = endX - l,
            height = endY - t
        )
    }

    companion object {
        val Full = ScanCropRegion()
        private const val MIN_SIZE = 0.08f
    }
}

object MedicineScanCrop {
    suspend fun <T> useRegion(
        context: Context,
        uri: Uri,
        region: ScanCropRegion,
        block: suspend (Uri) -> T
    ): T = withContext(Dispatchers.IO) {
        val file = File.createTempFile(
            "medicine_scan_crop_",
            ".jpg",
            context.cacheDir
        )

        try {
            val source = MedicinePhotoCodec.load(
                context,
                uri.toString(),
                2048
            )

            var cropped: Bitmap? = null

            try {
                currentCoroutineContext().ensureActive()

                val r = region.pixels(
                    source.width,
                    source.height
                )

                val image = Bitmap.createBitmap(
                    source,
                    r.left,
                    r.top,
                    r.width,
                    r.height
                )

                cropped = image

                file.outputStream().use {
                    check(
                        image.compress(
                            Bitmap.CompressFormat.JPEG,
                            95,
                            it
                        )
                    ) {
                        "Chưa cắt được ảnh. Vui lòng chọn lại."
                    }
                }
            } finally {
                cropped
                    ?.takeIf { it !== source }
                    ?.recycle()

                source.recycle()
            }

            currentCoroutineContext().ensureActive()

            block(Uri.fromFile(file))
        } finally {
            file.delete()
        }
    }
}