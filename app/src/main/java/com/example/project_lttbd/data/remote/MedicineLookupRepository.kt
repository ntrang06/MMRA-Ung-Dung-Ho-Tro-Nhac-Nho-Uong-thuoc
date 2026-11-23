package com.example.project_lttbd.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.io.IOException
import java.net.SocketTimeoutException
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val LOOKUP_BASE_URL = "http://127.0.0.1:8000/"
private const val CACHE_TTL_MS = 5 * 60 * 1000L
private const val CACHE_MAX_ENTRIES = 64

data class MedicineLookupCandidate(
    val id: String = "",
    val name: String = "",
    val strength: String = "",
    val dosage_form: String = "",
    val registration_number: String = "",
    val active_ingredient: String = "",
    val manufacturer: String = "",
    val source: String = ""
)

data class MedicineLookupResponse(
    val items: List<MedicineLookupCandidate> = emptyList()
)

enum class LookupErrorKind {
    TIMEOUT,
    UNAUTHORIZED,
    SERVICE_UNAVAILABLE,
    NETWORK
}

class MedicineLookupException(
    val kind: LookupErrorKind,
    override val message: String,
    cause: Throwable? = null
) : IOException(message, cause)

/**
 * Bộ nhớ đệm TTL ngắn hạn trên máy giúp tránh gọi trùng API khi người dùng
 * chuyển qua lại giữa bước xem kết quả và tìm kiếm thuốc.
 */
class LookupTtlCache(
    private val ttlMillis: Long = CACHE_TTL_MS,
    private val maxEntries: Int = CACHE_MAX_ENTRIES,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    private data class Entry(
        val expiresAtMillis: Long,
        val items: List<MedicineLookupCandidate>
    )

    private val entries = LinkedHashMap<String, Entry>(16, 0.75f, true)

    fun normalizeKey(query: String): String =
        query.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

    @Synchronized
    fun get(query: String): List<MedicineLookupCandidate>? {
        val key = normalizeKey(query)
        if (key.isBlank()) return null

        val entry = entries[key] ?: return null
        if (nowProvider() >= entry.expiresAtMillis) {
            entries.remove(key)
            return null
        }
        return entry.items
    }

    @Synchronized
    fun put(query: String, items: List<MedicineLookupCandidate>) {
        val key = normalizeKey(query)
        if (key.isBlank()) return

        entries[key] = Entry(
            expiresAtMillis = nowProvider() + ttlMillis,
            items = items
        )
        while (entries.size > maxEntries) {
            val eldestKey = entries.entries.firstOrNull()?.key ?: break
            entries.remove(eldestKey)
        }
    }

    @Synchronized
    fun clear() {
        entries.clear()
    }
}

private interface MedicineLookupApi {
    @GET("api/v1/medicine-lookup")
    suspend fun find(
        @Header("Authorization") token: String,
        @Query("q") query: String
    ): MedicineLookupResponse
}

class MedicineLookupRepository(
    private val cache: LookupTtlCache = sharedCache
) {
    companion object {
        private val sharedCache = LookupTtlCache()

        private val isLocalServer: Boolean
            get() = LOOKUP_BASE_URL.startsWith("http://127.0.0.1:") ||
                LOOKUP_BASE_URL.startsWith("http://localhost:")

        private val httpClient: OkHttpClient by lazy {
            val builder = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)

            if (isLocalServer) {
                builder
                    .connectionPool(ConnectionPool(0, 1, TimeUnit.SECONDS))
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header("Connection", "close")
                            .build()
                        chain.proceed(request)
                    }
            }

            builder.build()
        }

        private val sharedApi: MedicineLookupApi by lazy {
            Retrofit.Builder()
                .baseUrl(LOOKUP_BASE_URL)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MedicineLookupApi::class.java)
        }
    }

    suspend fun find(
        query: String,
        bypassCache: Boolean = false
    ): List<MedicineLookupCandidate> {
        val value = query.trim().replace(Regex("\\s+"), " ").take(100)
        if (value.length < 3) {
            return emptyList()
        }

        if (!bypassCache) {
            cache.get(value)?.let { cached ->
                return cached
            }
        }

        val authHeader = BackendSession.authorizationHeader()
        var lastError: Throwable? = null

        for (attempt in 0..1) {
            try {
                val items = sharedApi.find(authHeader, value).items.filter {
                    it.name.isNotBlank()
                }
                cache.put(value, items)
                return items
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (http: HttpException) {
                if (http.code() == 401 || http.code() == 403) {
                    throw MedicineLookupException(
                        kind = LookupErrorKind.UNAUTHORIZED,
                        message = "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.",
                        cause = http
                    )
                }
                lastError = http
                if (http.code() in listOf(429, 502, 503, 504) && attempt == 0) {
                    delay(400)
                    continue
                }
                if (http.code() >= 500) {
                    throw MedicineLookupException(
                        kind = LookupErrorKind.SERVICE_UNAVAILABLE,
                        message = "Các nguồn dữ liệu thuốc tạm thời bận. Vui lòng thử lại.",
                        cause = http
                    )
                }
                throw MedicineLookupException(
                    kind = LookupErrorKind.NETWORK,
                    message = "Không tải được dữ liệu thuốc (mã ${http.code()}).",
                    cause = http
                )
            } catch (timeout: SocketTimeoutException) {
                lastError = timeout
                if (attempt == 0) {
                    delay(400)
                    continue
                }
                throw MedicineLookupException(
                    kind = LookupErrorKind.TIMEOUT,
                    message = "Tra cứu quá thời gian chờ (timeout). Vui lòng thử lại.",
                    cause = timeout
                )
            } catch (io: IOException) {
                lastError = io
                if (attempt == 0) {
                    delay(400)
                    continue
                }
                throw MedicineLookupException(
                    kind = LookupErrorKind.NETWORK,
                    message = "Không kết nối được dữ liệu thuốc. Vui lòng kiểm tra Internet và thử lại.",
                    cause = io
                )
            }
        }

        throw MedicineLookupException(
            kind = LookupErrorKind.NETWORK,
            message = "Không kết nối được dữ liệu thuốc. Vui lòng thử lại.",
            cause = lastError
        )
    }
}

data class StrengthParts(
    val amount: String,
    val unit: String
)

private val saltModifiers = setOf(
    "hcl",
    "hydrochloride",
    "hydrochlorid",
    "dihydrate",
    "trihydrate",
    "monohydrate",
    "hemihydrate",
    "anhydrous"
)

private val specialReleaseFormKeywords = listOf(
    "extended-release",
    "modified-release",
    "sustained-release",
    "delayed-release",
    "prolonged-release",
    "controlled-release",
    "extended release",
    "modified release",
    "sustained release",
    "delayed release",
    "prolonged release",
    "controlled release",
    "enteric-coated",
    "enteric coated",
    "gastro-resistant",
    "phong thich keo dai",
    "giai phong keo dai",
    "phong thich cham",
    "giai phong cham",
    "phong thich co kiem soat",
    "giai phong co kiem soat",
    "tac dung keo dai",
    "bao tan trong ruot"
)

private val specialReleaseTokenRegex = Regex(
    """\b(?:xr|sr|mr|pr|cr|er|la|xl|retard)\b"""
)

internal fun foldLookupText(text: String): String {
    return Normalizer.normalize(
        text.lowercase(Locale.ROOT),
        Normalizer.Form.NFD
    )
        .replace(Regex("\\p{M}+"), "")
        .replace('đ', 'd')
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun hasMultipleActiveIngredients(activeIngredient: String): Boolean {
    val trimmed = activeIngredient.trim()
    if (trimmed.isBlank()) return false

    val parts = trimmed.split(Regex("""[,;+/]|(?i:\band\b|\bvà\b)"""))
        .map { it.trim() }
        .filter { part ->
            val folded = foldLookupText(part)
            folded.count { it.isLetter() } >= 2 && folded !in saltModifiers
        }
        .distinctBy { foldLookupText(it) }

    return parts.size > 1
}

fun parseCatalogStrength(
    text: String
): StrengthParts? {
    val normalized = text.trim().replace(',', '.')

    val match = Regex(
        """^(\d+(?:\.\d+)?)\s*(mg/ml|mg|mcg|µg|μg|g|IU|%)$""",
        RegexOption.IGNORE_CASE
    ).matchEntire(normalized) ?: return null

    val amount = match.groupValues[1]
    val numeric = amount.toBigDecimalOrNull() ?: return null
    if (numeric.signum() <= 0) return null

    val unit = when (match.groupValues[2].lowercase(Locale.ROOT)) {
        "iu" -> "IU"
        "µg", "μg" -> "mcg"
        else -> match.groupValues[2].lowercase(Locale.ROOT)
    }

    return StrengthParts(
        amount = amount,
        unit = unit
    )
}

/**
 * Chỉ cho phép tự điền vào ô hàm lượng đơn khi nguồn có đúng 1 hoạt chất
 * và 1 hàm lượng dương chuẩn duy nhất (tránh tự suy đoán trên thuốc phối hợp hoặc nồng độ).
 */
fun isSafeSingleStrengthForAutoFill(item: MedicineLookupCandidate): StrengthParts? {
    if (hasMultipleActiveIngredients(item.active_ingredient)) {
        return null
    }
    return parseCatalogStrength(item.strength)
}

fun isSpecialReleaseMedicine(
    name: String,
    dosageForm: String
): Boolean {
    val foldedForm = foldLookupText(dosageForm)
    val foldedName = foldLookupText(name)
        .replace('-', ' ')
        .replace('/', ' ')

    if (specialReleaseFormKeywords.any { it in foldedForm || it in foldedName }) {
        return true
    }

    return specialReleaseTokenRegex.containsMatchIn(foldedName) ||
        specialReleaseTokenRegex.containsMatchIn(foldedForm)
}

fun mapCatalogDosageForm(
    text: String
): String {
    val value = foldLookupText(text)

    return when {
        "vien nen" in value || "tablet" in value || "caplet" in value ->
            "Viên nén"

        "vien nang" in value || "capsule" in value || "softgel" in value ->
            "Viên nang"

        "siro" in value || "syrup" in value ->
            "Siro"

        "thuoc nho" in value || "drop" in value ->
            "Thuốc nhỏ"

        "dung dich" in value || "hon dich" in value ||
            "solution" in value || "suspension" in value || "elixir" in value ->
            "Dung dịch"

        text.isNotBlank() -> "Khác"

        else -> ""
    }
}
