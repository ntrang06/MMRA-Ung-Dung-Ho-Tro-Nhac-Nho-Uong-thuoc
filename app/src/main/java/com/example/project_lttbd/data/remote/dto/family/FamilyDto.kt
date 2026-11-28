package com.example.project_lttbd.data.remote.dto.family

import com.example.project_lttbd.data.remote.dto.ScheduleResponseDto
import com.google.gson.annotations.SerializedName

data class InvitationResponseDto(
    @SerializedName("invitationId") val invitationId: String,
    @SerializedName("code") val code: String,
    @SerializedName("expiresAt") val expiresAt: String
)

data class AcceptInvitationRequestDto(
    @SerializedName("code") val code: String
)

data class AcceptInvitationResponseDto(
    @SerializedName("linkId") val linkId: String,
    @SerializedName("status") val status: String
)

data class InvitationActionResponseDto(
    @SerializedName("invitationId") val invitationId: String,
    @SerializedName("status") val status: String
)

data class FamilyPermissionsDto(
    @SerializedName("viewSchedule") val viewSchedule: Boolean = false,
    @SerializedName("viewDoseStatus") val viewDoseStatus: Boolean = false,
    @SerializedName("receiveMissedDoseAlerts") val receiveMissedDoseAlerts: Boolean = false
)

data class FamilyMemberResponseDto(
    @SerializedName("linkId") val linkId: String,
    @SerializedName("role") val role: String,
    @SerializedName("memberUid") val memberUid: String,
    @SerializedName("status") val status: String,
    @SerializedName("permissions") val permissions: FamilyPermissionsDto
)

data class FamilyMembersResponseDto(
    @SerializedName("items") val items: List<FamilyMemberResponseDto>
)

data class FamilyLinkResponseDto(
    @SerializedName("linkId") val linkId: String,
    @SerializedName("status") val status: String
)

data class FamilyPermissionsResponseDto(
    @SerializedName("linkId") val linkId: String,
    @SerializedName("status") val status: String,
    @SerializedName("permissions") val permissions: FamilyPermissionsDto
)

data class SharedSchedulesResponseDto(
    @SerializedName("items") val items: List<ScheduleResponseDto>
)

data class SharedDoseDto(
    @SerializedName("doseId") val doseId: String,
    @SerializedName("localDate") val localDate: String,
    @SerializedName("scheduledAt") val scheduledAt: String,
    @SerializedName("status") val status: String,
    @SerializedName("actionAt") val actionAt: String? = null,
    @SerializedName("nextReminderAt") val nextReminderAt: String? = null
)

data class SharedDosesResponseDto(
    @SerializedName("items") val items: List<SharedDoseDto>
)

data class FamilyAlertDto(
    @SerializedName("alertId") val alertId: String,
    @SerializedName("linkId") val linkId: String,
    @SerializedName("ownerUid") val ownerUid: String,
    @SerializedName("doseId") val doseId: String,
    @SerializedName("localDate") val localDate: String,
    @SerializedName("scheduledAt") val scheduledAt: String,
    @SerializedName("status") val status: String,
    @SerializedName("actionAt") val actionAt: String? = null
)

data class FamilyAlertsResponseDto(
    @SerializedName("items") val items: List<FamilyAlertDto>
)
