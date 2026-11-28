package com.example.project_lttbd.data.remote

import android.util.Log
import com.example.project_lttbd.data.remote.dto.ScheduleResponseDto
import com.example.project_lttbd.data.remote.dto.family.AcceptInvitationRequestDto
import com.example.project_lttbd.data.remote.dto.family.AcceptInvitationResponseDto
import com.example.project_lttbd.data.remote.dto.family.FamilyAlertDto
import com.example.project_lttbd.data.remote.dto.family.FamilyLinkResponseDto
import com.example.project_lttbd.data.remote.dto.family.FamilyMemberResponseDto
import com.example.project_lttbd.data.remote.dto.family.FamilyPermissionsDto
import com.example.project_lttbd.data.remote.dto.family.FamilyPermissionsResponseDto
import com.example.project_lttbd.data.remote.dto.family.InvitationActionResponseDto
import com.example.project_lttbd.data.remote.dto.family.InvitationResponseDto
import com.example.project_lttbd.data.remote.dto.family.SharedDoseDto
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException

private const val FAMILY_REPOSITORY_LOG_TAG = "MMRA_FAMILY_API"

class FamilyException(
    message: String,
    val errorCode: String? = null,
    val httpCode: Int? = null
) : Exception(message)

class FamilyRepository(
    private val api: FamilyApi = ScheduleApiClient.familyApi
) {
    suspend fun createInvitation(): InvitationResponseDto {
        return requireBody(
            execute(
                operation = "POST /api/v1/family/invitations",
                isWrite = true
            ) { authorization ->
                api.createInvitation(authorization)
            }
        )
    }

    suspend fun acceptInvitation(code: String): AcceptInvitationResponseDto {
        return requireBody(
            execute(
                operation = "POST /api/v1/family/invitations/accept",
                isWrite = true
            ) { authorization ->
                api.acceptInvitation(
                    authorization,
                    AcceptInvitationRequestDto(code = code.trim())
                )
            }
        )
    }

    suspend fun declineInvitation(code: String): InvitationActionResponseDto {
        return requireBody(
            execute(
                operation = "POST /api/v1/family/invitations/decline",
                isWrite = true
            ) { authorization ->
                api.declineInvitation(
                    authorization,
                    AcceptInvitationRequestDto(code = code.trim())
                )
            }
        )
    }

    suspend fun cancelInvitation(invitationId: String): InvitationActionResponseDto {
        return requireBody(
            execute(
                operation = "DELETE /api/v1/family/invitations/{invitationId}",
                isWrite = true
            ) { authorization ->
                api.cancelInvitation(authorization, invitationId)
            }
        )
    }

    suspend fun listMembers(): List<FamilyMemberResponseDto> {
        val response = requireBody(
            execute(
                operation = "GET /api/v1/family/members"
            ) { authorization ->
                api.listMembers(authorization)
            }
        )
        return response.items
    }

    suspend fun updatePermissions(
        linkId: String,
        permissions: FamilyPermissionsDto
    ): FamilyPermissionsResponseDto {
        return requireBody(
            execute(
                operation = "PUT /api/v1/family/members/{linkId}/permissions",
                isWrite = true
            ) { authorization ->
                api.updatePermissions(authorization, linkId, permissions)
            }
        )
    }

    suspend fun revokeLink(linkId: String): FamilyLinkResponseDto {
        return requireBody(
            execute(
                operation = "DELETE /api/v1/family/members/{linkId}",
                isWrite = true
            ) { authorization ->
                api.revokeLink(authorization, linkId)
            }
        )
    }

    suspend fun getSharedSchedules(linkId: String): List<ScheduleResponseDto> {
        val response = requireBody(
            execute(
                operation = "GET /api/v1/family/members/{linkId}/schedules"
            ) { authorization ->
                api.getSharedSchedules(authorization, linkId)
            }
        )
        return response.items
    }

    suspend fun getSharedDoses(
        linkId: String,
        date: String
    ): List<SharedDoseDto> {
        val response = requireBody(
            execute(
                operation = "GET /api/v1/family/members/{linkId}/doses"
            ) { authorization ->
                api.getSharedDoses(authorization, linkId, date)
            }
        )
        return response.items
    }

    suspend fun getFamilyAlerts(
        date: String? = null,
        linkId: String? = null
    ): List<FamilyAlertDto> {
        val response = requireBody(
            execute(
                operation = "GET /api/v1/family/alerts"
            ) { authorization ->
                api.getFamilyAlerts(authorization, date, linkId)
            }
        )
        return response.items
    }

    private suspend fun <T> execute(
        operation: String,
        isWrite: Boolean = false,
        call: suspend (String) -> Response<T>
    ): Response<T> {
        Log.i(FAMILY_REPOSITORY_LOG_TAG, "BEGIN $operation")

        val uid = FirebaseProvider.auth.currentUser?.uid
        if (uid == null) {
            Log.w(FAMILY_REPOSITORY_LOG_TAG, "$operation: chưa đăng nhập")
            throw FamilyException(
                "Vui lòng đăng nhập lại.",
                "unauthenticated"
            )
        }

        val authorization = try {
            BackendSession.authorizationHeader()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            ensureSameUser(uid)
            Log.e(FAMILY_REPOSITORY_LOG_TAG, "$operation: lỗi lấy phiên xác thực", error)
            throw FamilyException(
                "Chưa xác thực được phiên đăng nhập. Vui lòng kiểm tra kết nối và thử lại.",
                "auth_unavailable"
            )
        }

        ensureSameUser(uid)

        try {
            val response = call(authorization)
            ensureSameUser(uid, response)

            if (!response.isSuccessful) {
                Log.e(FAMILY_REPOSITORY_LOG_TAG, "$operation: HTTP ${response.code()}")
                throw apiError(response, isWrite)
            }

            Log.i(FAMILY_REPOSITORY_LOG_TAG, "DONE $operation: HTTP ${response.code()}")
            return response
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: FamilyException) {
            throw error
        } catch (error: IOException) {
            ensureSameUser(uid)
            Log.e(FAMILY_REPOSITORY_LOG_TAG, "$operation: ${error.javaClass.simpleName}: ${error.message}", error)
            throw FamilyException(
                message = if (isWrite) {
                    "Chưa xác nhận được kết quả thao tác. Vui lòng thử lại."
                } else {
                    "Không kết nối được máy chủ. Vui lòng kiểm tra kết nối và thử lại."
                },
                errorCode = "network_error"
            )
        } catch (error: JsonParseException) {
            ensureSameUser(uid)
            Log.e(FAMILY_REPOSITORY_LOG_TAG, "$operation: lỗi đọc dữ liệu JSON", error)
            throw FamilyException(
                "Máy chủ trả về dữ liệu chưa hợp lệ. Vui lòng thử lại.",
                "invalid_response"
            )
        } catch (error: Exception) {
            ensureSameUser(uid)
            Log.e(FAMILY_REPOSITORY_LOG_TAG, "$operation: lỗi chưa phân loại", error)
            throw error
        }
    }

    private fun ensureSameUser(
        uid: String,
        response: Response<*>? = null
    ) {
        if (FirebaseProvider.auth.currentUser?.uid != uid) {
            response?.errorBody()?.close()
            throw CancellationException("Tài khoản đã thay đổi.")
        }
    }

    private fun <T> requireBody(response: Response<T>): T {
        val body = response.body()
        if (body == null) {
            Log.e(FAMILY_REPOSITORY_LOG_TAG, "HTTP ${response.code()}: không có dữ liệu phản hồi")
            throw FamilyException(
                "Chưa nhận được dữ liệu hợp lệ. Vui lòng thử lại.",
                "invalid_response"
            )
        }
        return body
    }

    private fun apiError(
        response: Response<*>,
        isWrite: Boolean
    ): FamilyException {
        var detailCode: String? = null
        var detailMessage: String? = null

        runCatching {
            val text = response.errorBody()?.use { it.string() } ?: return@runCatching
            val json = JsonParser.parseString(text)
            if (json.isJsonObject) {
                val detailObj = json.asJsonObject.get("detail")
                if (detailObj != null && detailObj.isJsonObject) {
                    detailCode = detailObj.asJsonObject.get("code")?.asString
                    detailMessage = detailObj.asJsonObject.get("message")?.asString
                } else if (detailObj != null && detailObj.isJsonPrimitive) {
                    detailMessage = detailObj.asString
                }
            }
        }

        Log.e(
            FAMILY_REPOSITORY_LOG_TAG,
            "API_ERROR HTTP ${response.code()}, code=$detailCode, msg=$detailMessage"
        )

        val message = detailMessage ?: when (response.code()) {
            400 -> when (detailCode) {
                "invitation_invalid" -> "Mã mời không hợp lệ. Vui lòng kiểm tra lại."
                "invitation_expired" -> "Mã mời đã hết hạn."
                "self_invitation" -> "Không thể tự liên kết với chính mình."
                else -> "Mã mời không hợp lệ hoặc đã hết hạn."
            }
            401 -> "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
            403 -> "Bạn không có quyền thực hiện thao tác này."
            404 -> "Không tìm thấy dữ liệu kết nối gia đình."
            409 -> when (detailCode) {
                "invitation_unavailable" -> "Mã mời đã được sử dụng hoặc thu hồi."
                "family_already_linked" -> "Hai tài khoản đã được kết nối từ trước."
                else -> "Dữ liệu đã thay đổi hoặc đã được liên kết."
            }
            429 -> "Bạn thao tác quá nhanh. Vui lòng đợi một lúc rồi thử lại."
            in 500..599 -> "Dịch vụ Family tạm thời không khả dụng. Vui lòng thử lại sau."
            else -> "Thao tác không thành công (${response.code()}). Vui lòng thử lại."
        }

        return FamilyException(
            message = message,
            errorCode = detailCode,
            httpCode = response.code()
        )
    }
}
