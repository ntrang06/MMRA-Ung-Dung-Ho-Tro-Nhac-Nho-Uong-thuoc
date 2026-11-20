
package com.example.project_lttbd.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
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

data class MedicineRequestDto(
    val medicineName: String,
    val strength: String,
    val strengthUnit: String,
    val dosageForm: String,
    val note: String,
    val imageUrl: String?
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

    @POST("api/v1/medicines")
    suspend fun addMedicine(
        @Header("Authorization")
        authorization: String,
        @Body medicine: MedicineRequestDto
    ): Response<MedicineApiDto>

    @PUT("api/v1/medicines/{medicineId}")
    suspend fun updateMedicine(
        @Header("Authorization")
        authorization: String,
        @Path("medicineId")
        medicineId: String,
        @Body medicine: MedicineRequestDto
    ): Response<MedicineApiDto>
}

object MedicineApiClient {

    // Môi trường debug: kết nối qua adb reverse.
    // Bản phát hành phải sử dụng Backend HTTPS.
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
