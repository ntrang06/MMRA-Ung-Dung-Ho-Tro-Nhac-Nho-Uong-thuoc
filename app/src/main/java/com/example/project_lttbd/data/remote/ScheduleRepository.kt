package com.example.project_lttbd.data.remote

import com.example.project_lttbd.data.remote.dto.ScheduleInputDto
import com.example.project_lttbd.data.remote.dto.ScheduleResponseDto
import com.example.project_lttbd.data.remote.dto.ScheduleUpdateDto
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException

class ScheduleException(
    message: String,
    val errorCode: String? = null,
    val httpCode: Int? = null
) : Exception(message)

class ScheduleRepository(
    private val api: ScheduleApi = ScheduleApiClient.api
) {

    suspend fun listSchedules(): List<ScheduleResponseDto> {
        return requireBody(execute { authorization ->
            api.listSchedules(authorization)
        })
    }

    suspend fun getSchedule(scheduleId: String): ScheduleResponseDto {
        return requireBody(execute { authorization ->
            api.getSchedule(authorization, scheduleId)
        })
    }

    suspend fun createSchedule(
        body: ScheduleInputDto
    ): ScheduleResponseDto {
        return requireBody(execute(isWrite = true) { authorization ->
            api.createSchedule(authorization, body)
        })
    }

    suspend fun updateSchedule(
        scheduleId: String,
        body: ScheduleUpdateDto
    ): ScheduleResponseDto {
        return requireBody(execute(isWrite = true) { authorization ->
            api.updateSchedule(authorization, scheduleId, body)
        })
    }

    suspend fun deleteSchedule(
        scheduleId: String,
        expectedVersion: Int
    ) {
        execute(isWrite = true) { authorization ->
            api.deleteSchedule(
                authorization,
                scheduleId,
                expectedVersion
            )
        }

        // HTTP 204 thành công không có response body.
    }

    private suspend fun <T> execute(
        isWrite: Boolean = false,
        call: suspend (String) -> Response<T>
    ): Response<T> {
        val uid = FirebaseProvider.auth.currentUser?.uid
            ?: throw ScheduleException(
                "Vui lòng đăng nhập lại.",
                "unauthenticated"
            )

        val authorization = try {
            BackendSession.authorizationHeader()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            ensureSameUser(uid)

            throw ScheduleException(
                "Chưa xác thực được phiên đăng nhập. " +
                        "Vui lòng kiểm tra kết nối và thử lại.",
                "auth_unavailable"
            )
        }

        ensureSameUser(uid)

        try {
            val response = call(authorization)

            // Bỏ kết quả nếu đã đăng xuất hoặc đổi tài khoản.
            ensureSameUser(uid, response)

            if (!response.isSuccessful) {
                throw apiError(response, isWrite)
            }

            return response
        } catch (error: IOException) {
            ensureSameUser(uid)

            throw ScheduleException(
                if (isWrite) {
                    uncertainWriteMessage()
                } else {
                    "Không kết nối được máy chủ. " +
                            "Vui lòng kiểm tra kết nối và thử lại."
                },
                "network_error"
            )
        } catch (error: JsonParseException) {
            ensureSameUser(uid)

            throw ScheduleException(
                "Máy chủ trả về dữ liệu chưa hợp lệ. " +
                        "Vui lòng tải lại danh sách trước khi thử lại.",
                "invalid_response"
            )
        }
    }

    private fun ensureSameUser(
        uid: String,
        response: Response<*>? = null
    ) {
        if (FirebaseProvider.auth.currentUser?.uid != uid) {
            response?.errorBody()?.close()

            throw CancellationException(
                "Tài khoản đã thay đổi."
            )
        }
    }

    private fun <T> requireBody(response: Response<T>): T {
        return response.body() ?: throw ScheduleException(
            "Chưa nhận được dữ liệu hợp lệ. " +
                    "Vui lòng tải lại danh sách trước khi thử lại.",
            "invalid_response"
        )
    }

    private fun apiError(
        response: Response<*>,
        isWrite: Boolean
    ): ScheduleException {
        val code = runCatching {
            val text = response.errorBody()?.string()
                ?: return@runCatching null

            val detail = JsonParser.parseString(text)
                .asJsonObject
                .get("detail")

            if (detail?.isJsonObject == true) {
                detail.asJsonObject.get("code")?.asString
            } else {
                null
            }
        }.getOrNull()

        val message = when (response.code()) {
            401 ->
                "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."

            403 ->
                "Bạn không có quyền thực hiện thao tác này."

            404 -> {
                if (code == "medicine_not_found") {
                    "Thuốc không còn tồn tại. Vui lòng chọn lại thuốc."
                } else {
                    "Lịch uống không còn tồn tại. " +
                            "Vui lòng tải lại danh sách."
                }
            }

            409 ->
                "Lịch đã thay đổi. " +
                        "Vui lòng tải lại trước khi thao tác tiếp."

            422 ->
                "Thông tin lịch uống chưa hợp lệ. " +
                        "Vui lòng kiểm tra lại."

            429 ->
                "Bạn thao tác quá nhanh. " +
                        "Vui lòng đợi một lúc rồi thử lại."

            in 500..599 -> {
                if (isWrite) {
                    uncertainWriteMessage()
                } else {
                    "Máy chủ đang gặp sự cố. Vui lòng thử lại sau."
                }
            }

            else -> {
                if (isWrite) {
                    uncertainWriteMessage()
                } else {
                    "Chưa tải được lịch uống. Vui lòng thử lại."
                }
            }
        }

        return ScheduleException(
            message,
            code,
            response.code()
        )
    }

    private fun uncertainWriteMessage(): String {
        return "Chưa xác nhận được kết quả lưu hoặc xóa. " +
                "Vui lòng tải lại danh sách trước khi thử lại."
    }
}