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
    val dosageForm: String = "",
    val candidateNames: List<String> = emptyList(),
    val detectedStrengths: List<String> = emptyList(),
    val isAmbiguousStrength: Boolean = false,
    val isSpecialRelease: Boolean = false
)

object MedicineOcrParser {
    private val qualityMarkPattern = Regex(
        """^(?:(?:who[\s-]*)?(?:gmp|gmr|gimp|gsp|glp)(?:\b|[\s-])|iso[\s-]*\d{3,5}(?:\b|:))"""
    )

    private val registrationOrBatchPattern = Regex(
        """^(?:s[dđ]k|so dang ky|vn|vd|vs|gc|ql[a-z]*)\s*[-:/]?\s*\d[\d\-/.]*$"""
    )

    private val packagingCountPattern = Regex(
        """^\d+\s*(?:vi|hop|vien|goi|chai|ong|lo|tuyp)\b.*"""
    )

    // Bắt cụm số bị OCR nhầm (O->0, l/I/|->1) và đơn vị bị nhầm (rng/mq/m9->mg, rncg->mcg, rnl->ml, 1U/lU->IU)
    // Chỉ áp dụng trên cụm có chứa ít nhất 1 chữ số thật đi liền đơn vị đo để không làm biến dạng tên thuốc.
    private val ocrNumberUnitPattern = Regex(
        """(?<![\p{L}\d.,])([0-9OoIl|]+(?:[.,][0-9OoIl|]+)?)\s*(mg\s*/\s*ml|rncg|mc9|mcg|rng|mq|m9|mg|µg|μg|rnl|ml|g|1u|lu|\|u|iu|%)(?![\p{L}\d])""",
        RegexOption.IGNORE_CASE
    )

    // Sửa đúng một chữ g lặp ở cuối đơn vị: 500mgg -> 500 mg.
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

    private val specialReleasePattern = Regex(
        """(?:\b(extended|modified|sustained|delayed|prolonged|controlled)[\s-]+release\b|\b(phong thich|giai phong|tac dung)\s+(keo dai|cham|co kiem soat)\b|\bbao tan trong ruot\b|\b(pr|xr|sr|mr|cr|er|retard)\b)""",
        RegexOption.IGNORE_CASE
    )

    private val ignoredPrefixes = listOf(
        "thanh phan", "ham luong", "hoat chat", "moi ",
        "lieu ", "cach dung", "chi dinh", "chong chi dinh",
        "bao quan", "han dung", "hsd", "nsx", "ngay san xuat",
        "san xuat", "nha san xuat", "hang san xuat", "phan phoi",
        "cong ty", "cty", "dia chi", "xuat xu", "tieu chuan",
        "so lo", "so dang ky", "sdk", "dnnk", "hop ", "vi ",
        "vien ", "thuoc ", "dung dich", "siro", "thuoc nho",
        "quy cach", "doc ky", "de xa", "dung cho",
        "composition", "ingredients", "strength", "each ",
        "dosage", "storage", "keep ", "printing",
        "manufactur", "exp", "mfg", "batch", "lot", "box ",
        "tablets", "capsules", "film-coated", "film coated"
    )

    fun parse(lines: List<MedicineOcrLine>): MedicineOcrSuggestion {
        val cleaned = lines.map { line ->
            val compact = line.text.replace(Regex("\\s+"), " ").trim()
            val fixedArtifacts = normalizeOcrStrengthArtifacts(compact)
            val fixedRepeated = repeatedUnitPattern.replace(fixedArtifacts) { match ->
                "${match.groupValues[1]} ${normalizeUnit(match.groupValues[2])}"
            }

            line.copy(
                text = joinedUnitPattern.replace(fixedRepeated) { match ->
                    "${match.groupValues[1]} ${normalizeUnit(match.groupValues[2])}"
                }
            )
        }.filter { it.text.isNotBlank() }

        val text = cleaned.joinToString("\n") { it.text }

        val strengths = strengthPattern.findAll(text)
            .map { match ->
                match.groupValues[1].replace(',', '.') to normalizeUnit(match.groupValues[2])
            }
            .filter { (value, _) ->
                value.toBigDecimalOrNull()?.signum() == 1
            }
            .distinct()
            .toList()

        val detectedStrengthLabels = strengths.map { (amount, unit) ->
            "$amount $unit"
        }

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
        val isSpecialRelease = specialReleasePattern.containsMatchIn(folded)

        // Vẫn trích xuất tên thuốc ứng viên ngay cả khi hàm lượng đa hoạt chất / mơ hồ
        // để người dùng có thể đem tên thuốc đi tra cứu thay vì mất trắng tên thuốc.
        val candidateNames = cleaned.mapNotNull { line ->
            val withoutRatios = ratioPattern.replace(line.text, "")
            val candidate = strengthPattern
                .replace(withoutRatios, "")
                .replace("®", "")
                .replace("™", "")
                .replace(Regex("\\s+"), " ")
                .trim(' ', ':', '-', '•', '/', ',', '.', '+')

            val candidateFolded = fold(candidate)

            if (isValidCandidateName(candidate, candidateFolded, line.angleDegrees)) {
                candidate to line.height
            } else {
                null
            }
        }
            .sortedByDescending { it.second }
            .map { it.first }
            .distinctBy { fold(it) }
            .take(5)

        val name = candidateNames.firstOrNull().orEmpty()

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
            dosageForm = form,
            candidateNames = candidateNames,
            detectedStrengths = detectedStrengthLabels,
            isAmbiguousStrength = ambiguous,
            isSpecialRelease = isSpecialRelease
        )
    }

    internal fun normalizeOcrStrengthArtifacts(line: String): String {
        return ocrNumberUnitPattern.replace(line) { match ->
            val rawNum = match.groupValues[1]
            val rawUnit = match.groupValues[2]

            // Chỉ sửa khi cụm số có ít nhất 1 chữ số thật [0-9] để tránh sửa nhầm từ chữ thường.
            if (rawNum.none { it.isDigit() }) {
                return@replace match.value
            }

            val fixedNum = rawNum
                .replace('O', '0')
                .replace('o', '0')
                .replace('l', '1')
                .replace('I', '1')
                .replace('|', '1')

            val fixedUnit = normalizeUnitArtifact(rawUnit)
            "$fixedNum $fixedUnit"
        }
    }

    private fun normalizeUnitArtifact(rawUnit: String): String {
        val compact = rawUnit.replace(Regex("\\s+"), "").lowercase(Locale.ROOT)
        return when (compact) {
            "rng", "mq", "m9", "mg" -> "mg"
            "rncg", "mc9", "mcg", "µg", "μg" -> "mcg"
            "rnl", "ml" -> "ml"
            "1u", "lu", "|u", "iu" -> "IU"
            else -> compact
        }
    }

    private fun isValidCandidateName(
        candidate: String,
        candidateFolded: String,
        angleDegrees: Float
    ): Boolean {
        if (candidate.length !in 3..70) return false
        if (!isNameOrientation(angleDegrees)) return false
        if (candidate.count { it.isLetter() } < 3) return false
        if (qualityMarkPattern.containsMatchIn(candidateFolded)) return false
        if (registrationOrBatchPattern.containsMatchIn(candidateFolded)) return false
        if (packagingCountPattern.containsMatchIn(candidateFolded)) return false
        if (ignoredPrefixes.any { candidateFolded.startsWith(it) }) return false
        if (candidateFolded.contains("www.") || candidateFolded.contains("http") || '@' in candidate) {
            return false
        }
        if (candidateFolded in setOf("rx", "otc", "mg", "mcg", "iu", "ml", "sdk")) {
            return false
        }
        return true
    }

    private fun normalizeUnit(value: String): String = when (
        value.replace(Regex("\\s+"), "")
            .lowercase(Locale.ROOT)
    ) {
        "iu", "1u", "lu", "|u" -> "IU"
        "µg", "μg", "rncg", "mc9" -> "mcg"
        "rng", "mq", "m9" -> "mg"
        "rnl" -> "ml"
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
