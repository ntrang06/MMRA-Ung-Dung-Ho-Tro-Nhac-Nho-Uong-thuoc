
package com.example.project_lttbd.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.Locale

private const val LOOKUP_BASE_URL =
    "http://127.0.0.1:8000/"

data class MedicineLookupCandidate(
    val id: String = "",
    val name: String = "",
    val strength: String = "",
    val dosage_form: String = "",
    val registration_number: String = "",
    val active_ingredient: String = "",
    val source: String = ""
)

data class MedicineLookupResponse(
    val items: List<MedicineLookupCandidate> = emptyList()
)

private interface MedicineLookupApi {

    @GET("api/v1/medicine-lookup")
    suspend fun find(
        @Header("Authorization") token: String,
        @Query("q") query: String
    ): MedicineLookupResponse
}

class MedicineLookupRepository {

    private val api: MedicineLookupApi by lazy {
        Retrofit.Builder()
            .baseUrl(LOOKUP_BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(MedicineLookupApi::class.java)
    }

    suspend fun find(
        query: String
    ): List<MedicineLookupCandidate> {

        val value = query.trim().take(100)

        if (value.length < 3) {
            return emptyList()
        }

        return api.find(
            BackendSession.authorizationHeader(),
            value
        ).items.filter {
            it.name.isNotBlank()
        }
    }
}

data class StrengthParts(
    val amount: String,
    val unit: String
)

fun parseCatalogStrength(
    text: String
): StrengthParts? {

    val normalized = text.trim().replace(',', '.')

    val match = Regex(
        """^(\d+(?:\.\d+)?)\s*(mg|mcg|µg|g|IU|%)$""",
        RegexOption.IGNORE_CASE
    ).matchEntire(normalized) ?: return null

    val unit = when (
        match.groupValues[2].lowercase(Locale.ROOT)
    ) {
        "iu" -> "IU"
        "µg" -> "mcg"
        else -> match.groupValues[2]
            .lowercase(Locale.ROOT)
    }

    return StrengthParts(
        amount = match.groupValues[1],
        unit = unit
    )
}

fun mapCatalogDosageForm(
    text: String
): String {

    val value = java.text.Normalizer.normalize(
        text.lowercase(Locale.ROOT),
        java.text.Normalizer.Form.NFD
    )
        .replace(Regex("\\p{M}+"), "")
        .replace('đ', 'd')

    return when {
        "vien nen" in value || "tablet" in value ->
            "Viên nén"

        "vien nang" in value || "capsule" in value ->
            "Viên nang"

        "siro" in value || "syrup" in value ->
            "Siro"

        "thuoc nho" in value || "drops" in value ->
            "Thuốc nhỏ"

        "dung dich" in value || "solution" in value ->
            "Dung dịch"

        text.isNotBlank() -> "Khác"

        else -> ""
    }
}
