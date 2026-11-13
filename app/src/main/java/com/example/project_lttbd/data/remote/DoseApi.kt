package com.example.project_lttbd.data.remote

import com.example.project_lttbd.data.remote.dto.DoseActionRequestDto
import com.example.project_lttbd.data.remote.dto.DoseResponseDto
import com.example.project_lttbd.data.remote.dto.SnoozeRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DoseApi {

    @GET("api/v1/doses")
    suspend fun listDoses(
        @Header("Authorization") authorization: String,
        @Query("date") date: String
    ): Response<List<DoseResponseDto>>

    @GET("api/v1/doses/{doseId}")
    suspend fun getDose(
        @Header("Authorization") authorization: String,
        @Path("doseId") doseId: String
    ): Response<DoseResponseDto>

    @PATCH("api/v1/doses/{doseId}/action")
    suspend fun updateDoseAction(
        @Header("Authorization") authorization: String,
        @Path("doseId") doseId: String,
        @Body body: DoseActionRequestDto
    ): Response<DoseResponseDto>

    @POST("api/v1/doses/{doseId}/snooze")
    suspend fun snoozeDose(
        @Header("Authorization") authorization: String,
        @Path("doseId") doseId: String,
        @Body body: SnoozeRequestDto
    ): Response<DoseResponseDto>
}