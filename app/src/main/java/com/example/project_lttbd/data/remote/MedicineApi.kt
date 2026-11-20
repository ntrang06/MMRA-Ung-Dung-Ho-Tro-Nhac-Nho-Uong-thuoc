
package com.example.project_lttbd.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

data class MedicineApiDto(
    @SerializedName("medicineId")
    val medicineId: String = "",

    @SerializedName("medicineName")
    val medicineName: String = "",

    @SerializedName("strength")
    val strength: String = "",

    @SerializedName("strengthUnit")
    val strengthUnit: String = "",

    @SerializedName("dosageForm")
    val dosageForm: String = "",

    @SerializedName("note")
    val note: String = "",

    @SerializedName("imageUrl")
    val imageUrl: String? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

interface MedicineApi {

    @GET("api/v1/medicines")
    suspend fun getMedicines(
        @Header("Authorization")
        authorization: String
    ): List<MedicineApiDto>

    @GET("api/v1/medicines/{medicineId}")
    suspend fun getMedicine(
        @Header("Authorization")
        authorization: String,
        @Path("medicineId")
        medicineId: String
    ): MedicineApiDto
}

object MedicineApiClient {

    // Chỉ sử dụng cho môi trường debug qua adb reverse.
    // Bản phát hành cần chuyển sang HTTPS.
    private const val BASE_URL =
        "http://127.0.0.1:8000/"

    val api: MedicineApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(MedicineApi::class.java)
    }
}
