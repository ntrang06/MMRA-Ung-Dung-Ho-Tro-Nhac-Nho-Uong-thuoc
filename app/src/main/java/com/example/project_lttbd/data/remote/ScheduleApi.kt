package com.example.project_lttbd.data.remote

import com.example.project_lttbd.data.remote.dto.ScheduleInputDto
import com.example.project_lttbd.data.remote.dto.ScheduleResponseDto
import com.example.project_lttbd.data.remote.dto.ScheduleUpdateDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ScheduleApi {

    @GET("api/v1/schedules")
    suspend fun listSchedules(
        @Header("Authorization") authorization: String
    ): Response<List<ScheduleResponseDto>>

    @GET("api/v1/schedules/{scheduleId}")
    suspend fun getSchedule(
        @Header("Authorization") authorization: String,
        @Path("scheduleId") scheduleId: String
    ): Response<ScheduleResponseDto>

    @POST("api/v1/schedules")
    suspend fun createSchedule(
        @Header("Authorization") authorization: String,
        @Body body: ScheduleInputDto
    ): Response<ScheduleResponseDto>

    @PUT("api/v1/schedules/{scheduleId}")
    suspend fun updateSchedule(
        @Header("Authorization") authorization: String,
        @Path("scheduleId") scheduleId: String,
        @Body body: ScheduleUpdateDto
    ): Response<ScheduleResponseDto>

    @DELETE("api/v1/schedules/{scheduleId}")
    suspend fun deleteSchedule(
        @Header("Authorization") authorization: String,
        @Path("scheduleId") scheduleId: String,
        @Query("expectedVersion") expectedVersion: Int
    ): Response<Unit>
}