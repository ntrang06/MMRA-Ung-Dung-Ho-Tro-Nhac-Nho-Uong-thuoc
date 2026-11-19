package com.example.project_lttbd.data.scan

import android.content.Context
import android.net.Uri
import com.example.project_lttbd.data.image.MedicinePhotoCodec
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class MedicineOcrResult(
    val rawText: String,
    val suggestion: MedicineOcrSuggestion
)

object MedicineOcrReader {
    suspend fun read(
        context: Context,
        uri: Uri
    ): MedicineOcrResult = withContext(Dispatchers.IO) {
        // OCR dùng ảnh lớn hơn ảnh thu nhỏ lưu cùng thuốc.
        val bitmap = MedicinePhotoCodec.load(
            context,
            uri.toString(),
            2048
        )

        val recognizer = TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS
        )

        try {
            val result = recognizer.process(
                InputImage.fromBitmap(bitmap, 0)
            ).await()

            val rawText = result.text.trim().take(12_000)

            require(rawText.any { it.isLetterOrDigit() }) {
                "Chưa đọc được chữ. Hãy chụp gần tên và hàm lượng trên hộp/vỉ thuốc."
            }

            val lines = result.textBlocks
                .flatMap { it.lines }
                .take(200)
                .map { line ->
                    MedicineOcrLine(
                        text = line.text,
                        height = ocrCharacterHeight(
                            corners = line.cornerPoints
                                ?.map { point -> point.x to point.y }
                                .orEmpty(),
                            angleDegrees = line.angle,
                            fallbackWidth =
                                line.boundingBox?.width() ?: 0,
                            fallbackHeight =
                                line.boundingBox?.height() ?: 0
                        ),
                        angleDegrees = line.angle
                    )
                }

            MedicineOcrResult(
                rawText = rawText,
                suggestion = MedicineOcrParser.parse(lines)
            )
        } finally {
            recognizer.close()
        }
    }
}

// Đo chiều cao vuông góc với hướng chữ.
// Tránh coi một dòng chữ in dọc là dòng có cỡ chữ lớn nhất.
internal fun ocrCharacterHeight(
    corners: List<Pair<Int, Int>>,
    angleDegrees: Float,
    fallbackWidth: Int,
    fallbackHeight: Int
): Int {
    val angle = if (angleDegrees.isFinite()) {
        angleDegrees
    } else {
        0f
    }

    val radians = Math.toRadians(angle.toDouble())

    if (corners.size >= 4) {
        val projected = corners.map { (x, y) ->
            -sin(radians) * x + cos(radians) * y
        }

        return (projected.max() - projected.min())
            .roundToInt()
            .coerceAtLeast(0)
    }

    val height = if (
        abs(sin(radians)) > abs(cos(radians))
    ) {
        fallbackWidth
    } else {
        fallbackHeight
    }

    return height.coerceAtLeast(0)
}