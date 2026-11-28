package com.example.project_lttbd.data.remote

import com.example.project_lttbd.data.remote.dto.family.AcceptInvitationRequestDto
import com.example.project_lttbd.data.remote.dto.family.AcceptInvitationResponseDto
import com.example.project_lttbd.data.remote.dto.family.FamilyAlertsResponseDto
import com.example.project_lttbd.data.remote.dto.family.FamilyLinkResponseDto
import com.example.project_lttbd.data.remote.dto.family.FamilyMembersResponseDto
import com.example.project_lttbd.data.remote.dto.family.FamilyPermissionsDto
import com.example.project_lttbd.data.remote.dto.family.FamilyPermissionsResponseDto
import com.example.project_lttbd.data.remote.dto.family.InvitationActionResponseDto
import com.example.project_lttbd.data.remote.dto.family.InvitationResponseDto
import com.example.project_lttbd.data.remote.dto.family.SharedDosesResponseDto
import com.example.project_lttbd.data.remote.dto.family.SharedSchedulesResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface FamilyApi {

    @POST("api/v1/family/invitations")
    suspend fun createInvitation(
        @Header("Authorization") authorization: String
    ): Response<InvitationResponseDto>

    @POST("api/v1/family/invitations/accept")
    suspend fun acceptInvitation(
        @Header("Authorization") authorization: String,
        @Body body: AcceptInvitationRequestDto
    ): Response<AcceptInvitationResponseDto>

    @POST("api/v1/family/invitations/decline")
    suspend fun declineInvitation(
        @Header("Authorization") authorization: String,
        @Body body: AcceptInvitationRequestDto
    ): Response<InvitationActionResponseDto>

    @DELETE("api/v1/family/invitations/{invitationId}")
    suspend fun cancelInvitation(
        @Header("Authorization") authorization: String,
        @Path("invitationId") invitationId: String
    ): Response<InvitationActionResponseDto>

    @GET("api/v1/family/members")
    suspend fun listMembers(
        @Header("Authorization") authorization: String
    ): Response<FamilyMembersResponseDto>

    @PUT("api/v1/family/members/{linkId}/permissions")
    suspend fun updatePermissions(
        @Header("Authorization") authorization: String,
        @Path("linkId") linkId: String,
        @Body body: FamilyPermissionsDto
    ): Response<FamilyPermissionsResponseDto>

    @DELETE("api/v1/family/members/{linkId}")
    suspend fun revokeLink(
        @Header("Authorization") authorization: String,
        @Path("linkId") linkId: String
    ): Response<FamilyLinkResponseDto>

    @GET("api/v1/family/members/{linkId}/schedules")
    suspend fun getSharedSchedules(
        @Header("Authorization") authorization: String,
        @Path("linkId") linkId: String
    ): Response<SharedSchedulesResponseDto>

    @GET("api/v1/family/members/{linkId}/doses")
    suspend fun getSharedDoses(
        @Header("Authorization") authorization: String,
        @Path("linkId") linkId: String,
        @Query("date") date: String
    ): Response<SharedDosesResponseDto>

    @GET("api/v1/family/alerts")
    suspend fun getFamilyAlerts(
        @Header("Authorization") authorization: String,
        @Query("date") date: String? = null,
        @Query("linkId") linkId: String? = null
    ): Response<FamilyAlertsResponseDto>
}
