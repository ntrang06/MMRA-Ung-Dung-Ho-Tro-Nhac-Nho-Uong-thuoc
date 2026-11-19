package com.example.project_lttbd.data.scan

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

data class MedicineOcrLine(
    val text: String,
    val height: Int = 0,
    val angleDegrees: Float = 0f
)

data class MedicineOcrSuggestion(
    val medicineName: String = "",
    val strength: String = "",
    val strengthUnit: String = "mg",
    val dosageForm: String = ""
)

object MedicineOcrParser {
    private val qualityMarkPattern = Regex(
        """^(?:(?:who[\s-]*)?(?:gmp|gmr|gimp|gsp|glp)(?:\b|[\s-])|iso[\s-]*\d{3,5}(?:\b|:))"""
    )

    // Sửa đúng một chữ g lặp ở cuối đơn vị: 500mgg -> 500 mg.
    // Giữ nguyên mẫu số để tiếp tục kiểm tra nồng độ.
    private val repeatedUnitPattern = Regex(
        """(?<![\d.,])(\d+(?:[.,]\d+)?)\s*(mcg|mg)g(?![\p{L}\d])""",
        RegexOption.IGNORE_CASE
    )

    // OCR đôi khi nối ký tự thừa vào đơn vị, ví dụ "500mgia".
    private val joinedUnitPattern = Regex(
        """(?<![\d.,])(\d+(?:[.,]\d+)?)\s*(mcg|mg)[il|][\p{L}]{0,2}(?![\p{L}\d])""",
        RegexOption.IGNORE_CASE
    )

    private val strengthPattern = Regex(
        """(?<![\d.,])(\d+(?:[.,]\d+)?)\s*(mg\s*/\s*ml|mcg|[µμ]g|mg|g|IU|%)(?![\p{L}\d])""",
        RegexOption.IGNORE_CASE
    )

    private val ratioPattern = Regex(
        """(\d+(?:[.,]\d+)?)\s*(mg|mcg|[µμ]g|g|IU)\s*/\s*(\d+(?:[.,]\d+)?)?\s*(ml|l|kg|mg|mcg|g|IU)(?![\p{L}\d])""",
        RegexOption.IGNORE_CASE
    )

    private val ignoredPrefixes = listOf(
        "thanh phan", "ham luong", "hoat chat", "moi ",
        "lieu ", "cach dung", "chi dinh", "chong chi dinh",
        "bao quan", "han dung", "ngay san xuat",
        "san xuat", "nha san xuat", "cong ty", "cty", "dia chi",
        "so lo", "so dang ky", "sdk", "dnnk", "hop ", "vi ",
        "vien ", "thuoc ", "dung dich", "siro", "thuoc nho",
        "composition", "ingredients", "strength", "each ",
        "dosage", "storage", "keep ", "printing",
        "manufactur", "exp", "mfg", "batch", "lot", "box ",
        "tablets", "capsules", "film-coated", "film coated"
    )

    fun parse(lines: List<MedicineOcrLine>): MedicineOcrSuggestion {
        val cleaned = lines.map { line ->
            val cleanedText = repeatedUnitPattern.replace(
                line.text.replace(Regex("\\s+"), " ").trim()
            ) { match ->
                "${match.groupValues[1]} ${match.groupValues[2]}"
            }

            line.copy(
                text = joinedUnitPattern.replace(cleanedText) { match ->
                    "${match.groupValues[1]} ${match.groupValues[2]}"
                }
            )
        }.filter { it.text.isNotBlank() }

        val text = cleaned.joinToString("\n") { it.text }

        val strengths = strengthPattern.findAll(text)
            .map { match ->
                match.groupValues[1] to normalizeUnit(match.groupValues[2])
            }
            .filter { (value, _) ->
                value.replace(',', '.')
                    .toBigDecimalOrNull()
                    ?.signum() == 1
            }
            .distinct()
            .toList()

        // Không bỏ mất mẫu số của nồng độ như 125 mg/5 ml.
        // Không ép thuốc phối hợp thành một hàm lượng đơn.
        val unsupportedRatio = ratioPattern.findAll(text).any {
            normalizeUnit(it.groupValues[2]) != "mg" ||
                    it.groupValues[3].isNotEmpty() ||
                    it.groupValues[4].lowercase(Locale.ROOT) != "ml"
        }

        val explicitCombination = cleaned.any {
            ('+' in it.text || '/' in it.text) &&
                    strengthPattern.findAll(it.text).count() > 1
        }

        val ambiguous =
            strengths.size > 1 || unsupportedRatio || explicitCombination

        val strength = strengths.singleOrNull()
            .takeUnless { ambiguous }

        val folded = fold(text)

        val medicineContext = strengths.isNotEmpty() || Regex(
            """\b(thuoc|vien|tablet|tablets|capsule|capsules)\b"""
        ).containsMatchIn(folded)

        val name = if (!ambiguous && medicineContext) {
            cleaned.mapNotNull { line ->
                val candidate = strengthPattern
                    .replace(line.text, "")
                    .replace("®", "")
                    .replace("™", "")
                    .trim(' ', ':', '-', '•', '/', ',', '.')

                val candidateFolded = fold(candidate)

                if (
                    candidate.length !in 3..70 ||
                    !isNameOrientation(line.angleDegrees) ||
                    candidate.count { it.isLetter() } < 3 ||
                    qualityMarkPattern.containsMatchIn(candidateFolded) ||
                    ignoredPrefixes.any {
                        candidateFolded.startsWith(it)
                    } ||
                    candidateFolded in setOf(
                        "rx", "otc", "mg", "mcg", "iu"
                    )
                ) {
                    null
                } else {
                    candidate to line.height
                }
            }.maxByOrNull { it.second }
                ?.first
                .orEmpty()
        } else {
            ""
        }

        val form = when {
            "vien nang" in folded || "capsul" in folded ->
                "Viên nang"

            "vien nen" in folded || "tablet" in folded ->
                "Viên nén"

            "siro" in folded || "syrup" in folded ->
                "Siro"

            "thuoc nho" in folded || "eye drop" in folded ->
                "Thuốc nhỏ"

            "dung dich" in folded || "solution" in folded ->
                "Dung dịch"

            else -> ""
        }

        return MedicineOcrSuggestion(
            medicineName = name,
            strength = strength?.first.orEmpty(),
            strengthUnit = strength?.second ?: "mg",
            dosageForm = form
        )
    }

    private fun normalizeUnit(value: String): String = when (
        value.replace(Regex("\\s+"), "")
            .lowercase(Locale.ROOT)
    ) {
        "iu" -> "IU"
        "µg", "μg" -> "mcg"
        else -> value.replace(Regex("\\s+"), "")
            .lowercase(Locale.ROOT)
    }

    private fun isNameOrientation(angleDegrees: Float): Boolean {
        if (!angleDegrees.isFinite()) return false

        val angle = abs(angleDegrees % 180f)
        return min(angle, 180f - angle) <= 45f
    }

    private fun fold(value: String): String {
        return Normalizer.normalize(
            value.lowercase(Locale.ROOT),
            Normalizer.Form.NFD
        )
            .replace(Regex("\\p{M}+"), "")
            .replace('đ', 'd')
    }
}