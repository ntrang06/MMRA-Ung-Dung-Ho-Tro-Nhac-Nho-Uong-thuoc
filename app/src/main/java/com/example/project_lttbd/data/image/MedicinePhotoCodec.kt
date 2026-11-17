package com.example.project_lttbd.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.math.max

object MedicinePhotoCodec {
    const val PREFIX = "data:image/jpeg;base64,"

    private const val MAX_JPEG_BYTES = 100_000
    private const val MAX_INPUT_BYTES = 8 * 1024 * 1024
    private const val MAX_ENCODED_LENGTH =
        ((MAX_JPEG_BYTES + 2) / 3) * 4

    fun isStoredSource(source: String?): Boolean {
        if (source == null) return true

        if (source.startsWith("https://")) {
            return !Uri.parse(source).host.isNullOrBlank()
        }

        if (!source.startsWith(PREFIX)) return false

        val encoded = source.removePrefix(PREFIX)

        if (
            encoded.isEmpty() ||
            encoded.length > MAX_ENCODED_LENGTH
        ) {
            return false
        }

        return runCatching {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)

            bytes.size in 3..MAX_JPEG_BYTES &&
                    bytes[0] == 0xFF.toByte() &&
                    bytes[1] == 0xD8.toByte()
        }.getOrDefault(false)
    }

    suspend fun prepare(
        context: Context,
        uri: Uri
    ): String = withContext(Dispatchers.IO) {
        require(uri.scheme == "content") {
            "Vui lòng chọn ảnh từ thư viện."
        }

        var bitmap = decode(readContent(context, uri), 1024)

        while (true) {
            for (quality in listOf(85, 70, 55, 40)) {
                val output = ByteArrayOutputStream()

                check(
                    bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        quality,
                        output
                    )
                ) {
                    "Chưa xử lý được ảnh. Vui lòng chọn ảnh khác."
                }

                val bytes = output.toByteArray()

                if (bytes.size <= MAX_JPEG_BYTES) {
                    return@withContext PREFIX +
                            Base64.encodeToString(
                                bytes,
                                Base64.NO_WRAP
                            )
                }
            }

            val nextSide =
                max(bitmap.width, bitmap.height) * 3 / 4

            check(nextSide >= 128) {
                "Ảnh quá lớn. Vui lòng chọn ảnh khác."
            }

            bitmap = resize(bitmap, nextSide)
        }

        @Suppress("UNREACHABLE_CODE")
        error("Chưa xử lý được ảnh.")
    }

    suspend fun load(
        context: Context,
        source: String,
        maxSide: Int
    ): Bitmap = withContext(Dispatchers.IO) {
        val bytes = when {
            source.startsWith(PREFIX) -> {
                require(isStoredSource(source)) {
                    "Ảnh đã lưu không hợp lệ."
                }

                Base64.decode(
                    source.removePrefix(PREFIX),
                    Base64.NO_WRAP
                )
            }

            source.startsWith("https://") -> {
                val connection =
                    URL(source).openConnection() as HttpsURLConnection

                connection.connectTimeout = 10_000
                connection.readTimeout = 15_000
                connection.instanceFollowRedirects = false

                try {
                    require(connection.responseCode in 200..299) {
                        "Chưa tải được ảnh."
                    }

                    connection.inputStream.use {
                        readBounded(it)
                    }
                } finally {
                    connection.disconnect()
                }
            }

            else -> readContent(context, Uri.parse(source))
        }

        decode(bytes, maxSide)
    }

    private fun readContent(
        context: Context,
        uri: Uri
    ): ByteArray {
        require(uri.scheme == "content") {
            "Đường dẫn ảnh không hợp lệ."
        }

        return context.contentResolver
            .openInputStream(uri)
            ?.use { readBounded(it) }
            ?: error("Không mở được ảnh. Vui lòng chọn lại.")
    }

    private fun readBounded(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)

        while (true) {
            val read = input.read(buffer)
            if (read < 0) break

            require(output.size() + read <= MAX_INPUT_BYTES) {
                "Ảnh gốc vượt quá 8 MB. Vui lòng chọn ảnh nhỏ hơn."
            }

            output.write(buffer, 0, read)
        }

        return output.toByteArray()
    }

    private fun decode(
        bytes: ByteArray,
        maxSide: Int
    ): Bitmap {
        require(maxSide > 0)

        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            bounds
        )

        require(bounds.outWidth > 0 && bounds.outHeight > 0) {
            "Không đọc được ảnh. Vui lòng chọn JPG, PNG hoặc WebP."
        }

        var sample = 1

        while (
            max(bounds.outWidth, bounds.outHeight) / sample >
            maxSide * 2
        ) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
        }

        var bitmap = BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            options
        ) ?: error("Không đọc được ảnh. Vui lòng chọn lại.")

        val orientation = runCatching {
            ByteArrayInputStream(bytes).use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    1
                )
            }
        }.getOrDefault(1)

        val matrix = Matrix()

        when (orientation) {
            2 -> matrix.setScale(-1f, 1f)
            3 -> matrix.setRotate(180f)
            4 -> matrix.setScale(1f, -1f)

            5 -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }

            6 -> matrix.setRotate(90f)

            7 -> {
                matrix.setRotate(270f)
                matrix.postScale(-1f, 1f)
            }

            8 -> matrix.setRotate(270f)
        }

        if (orientation in 2..8) {
            bitmap = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
        }

        bitmap = resize(bitmap, maxSide)

        if (bitmap.hasAlpha()) {
            val opaque = Bitmap.createBitmap(
                bitmap.width,
                bitmap.height,
                Bitmap.Config.ARGB_8888
            )

            Canvas(opaque).apply {
                drawColor(Color.WHITE)
                drawBitmap(
                    bitmap,
                    0f,
                    0f,
                    Paint(Paint.ANTI_ALIAS_FLAG)
                )
            }

            bitmap = opaque
        }

        return bitmap
    }

    private fun resize(
        bitmap: Bitmap,
        maxSide: Int
    ): Bitmap {
        val side = max(bitmap.width, bitmap.height)

        if (side <= maxSide) return bitmap

        val ratio = maxSide.toFloat() / side

        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true
        )
    }
}