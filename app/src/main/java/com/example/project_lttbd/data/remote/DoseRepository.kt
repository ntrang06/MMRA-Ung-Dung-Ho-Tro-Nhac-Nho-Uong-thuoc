package com.example.project_lttbd.data.remote

import android.util.Log
import com.example.project_lttbd.data.remote.dto.DoseActionDto
import com.example.project_lttbd.data.remote.dto.DoseActionRequestDto
import com.example.project_lttbd.data.remote.dto.DoseResponseDto
import com.example.project_lttbd.data.remote.dto.SnoozeRequestDto
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException

private const val DOSE_REPOSITORY_LOG_TAG = "MMRA_API_ERROR"

class DoseException(
    message: String,
    val errorCode: String? = null,
    val httpCode: Int? = null
) : Exception(message)

class DoseRepository(
    private val api: DoseApi = ScheduleApiClient.doseApi
) {
    suspend fun listDoses(
        date: String
    ): List<DoseResponseDto> {
        require(
            Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(date)
        ) {
            "Ngày cần có định dạng yyyy-MM-dd."
        }

        return requireBody(
            execute(
                operation = "GET /api/v1/doses"
            ) { authorization ->
                api.listDoses(
                    authorization = authorization,
                    date = date
                )
            }
        )
    }

    suspend fun getDose(
        doseId: String
    ): DoseResponseDto {
        require(doseId.isNotBlank()) {
            "Chưa xác định được liều thuốc."
        }

        return requireBody(
            execute(
                operation = "GET /api/v1/doses/{doseId}"
            ) { authorization ->
                api.getDose(
                    authorization = authorization,
                    doseId = doseId
                )
            }
        )
    }

    suspend fun updateDoseAction(
        doseId: String,
        action: DoseActionDto
    ): DoseResponseDto {
        require(doseId.isNotBlank()) {
            "Chưa xác định được liều thuốc."
        }

        return requireBody(
            execute(
                operation = "PATCH /api/v1/doses/{doseId}/action",
                isWrite = true
            ) { authorization ->
                api.updateDoseAction(
                    authorization = authorization,
                    doseId = doseId,
                    body = DoseActionRequestDto(
                        action = action
                    )
                )
            }
        )
    }

    suspend fun snoozeDose(
        doseId: String,
        minutes: Int = 10
    ): DoseResponseDto {
        require(doseId.isNotBlank()) {
            "Chưa xác định được liều thuốc."
        }

        require(minutes in setOf(5, 10, 15, 30)) {
            "Chọn nhắc lại sau 5, 10, 15 hoặc 30 phút."
        }

        return requireBody(
            execute(
                operation = "POST /api/v1/doses/{doseId}/snooze",
                isWrite = true
            ) { authorization ->
                api.snoozeDose(
                    authorization = authorization,
                    doseId = doseId,
                    body = SnoozeRequestDto(
                        minutes = minutes
                    )
                )
            }
        )
    }

    private suspend fun <T> execute(
        operation: String,
        isWrite: Boolean = false,
        call: suspend (String) -> Response<T>
    ): Response<T> {
        Log.i(
            DOSE_REPOSITORY_LOG_TAG,
            "BEGIN $operation"
        )

        val uid = FirebaseProvider.auth.currentUser?.uid
            ?: throw DoseException(
                message = "Vui lòng đăng nhập để xem lịch uống.",
                errorCode = "unauthenticated"
            )

        val authorization = try {
            BackendSession.authorizationHeader()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            ensureSameUser(uid)

            Log.e(
                DOSE_REPOSITORY_LOG_TAG,
                "$operation: lỗi xác thực",
                error
            )

            throw DoseException(
                message = "Chưa xác thực được phiên đăng nhập. " +
                        "Vui lòng kiểm tra kết nối và thử lại.",
                errorCode = "auth_unavailable"
            )
        }

        ensureSameUser(uid)

        try {
            val response = call(authorization)

            // Bỏ kết quả nếu đã đăng xuất hoặc đổi tài khoản.
            ensureSameUser(uid, response)

            if (!response.isSuccessful) {
                Log.e(
                    DOSE_REPOSITORY_LOG_TAG,
                    "$operation: HTTP ${response.code()}"
                )

                throw apiError(response, isWrite)
            }

            Log.i(
                DOSE_REPOSITORY_LOG_TAG,
                "DONE $operation: HTTP ${response.code()}"
            )

            return response
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: DoseException) {
            throw error
        } catch (error: IOException) {
            ensureSameUser(uid)

            Log.e(
                DOSE_REPOSITORY_LOG_TAG,
                "$operation: ${error.javaClass.simpleName}: ${error.message}",
                error
            )

            throw DoseException(
                message = if (isWrite) {
                    uncertainWriteMessage()
                } else {
                    "Không kết nối được máy chủ. " +
                            "Vui lòng kiểm tra kết nối và thử lại."
                },
                errorCode = "network_error"
            )
        } catch (error: JsonParseException) {
            ensureSameUser(uid)

            Log.e(
                DOSE_REPOSITORY_LOG_TAG,
                "$operation: dữ liệu JSON không hợp lệ",
                error
            )

            throw DoseException(
                message = if (isWrite) {
                    uncertainWriteMessage()
                } else {
                    "Máy chủ trả về dữ liệu liều thuốc chưa hợp lệ. " +
                            "Vui lòng thử lại."
                },
                errorCode = "invalid_response"
            )
        } catch (error: Exception) {
            ensureSameUser(uid)

            Log.e(
                DOSE_REPOSITORY_LOG_TAG,
                "$operation: lỗi chưa được phân loại",
                error
            )

            throw error
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

    private fun <T> requireBody(
        response: Response<T>
    ): T {
        return response.body() ?: throw DoseException(
            message = "Chưa nhận được dữ liệu liều thuốc hợp lệ. " +
                    "Vui lòng tải lại lịch uống trước khi thao tác tiếp.",
            errorCode = "invalid_response"
        )
    }

    private fun apiError(
        response: Response<*>,
        isWrite: Boolean
    ): DoseException {
        val errorCode = runCatching {
            val text = response.errorBody()?.use {
                it.string()
            } ?: return@runCatching null

            val detail = JsonParser.parseString(text)
                .asJsonObject
                .get("detail")

            if (detail?.isJsonObject == true) {
                detail.asJsonObject
                    .get("code")
                    ?.asString
            } else {
                null
            }
        }.getOrNull()

        val message = when (response.code()) {
            401 ->
                "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."

            403 ->
                "Bạn không có quyền truy cập liều thuốc này."

            404 ->
                "Không tìm thấy liều thuốc. Vui lòng tải lại lịch uống."

            409 ->
                "Liều thuốc đã thay đổi hoặc đã được xử lý. " +
                        "Vui lòng tải lại lịch uống trước khi thao tác tiếp."

            422 ->
                "Ngày hoặc thông tin xử lý liều chưa hợp lệ. " +
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

        return DoseException(
            message = message,
            errorCode = errorCode,
            httpCode = response.code()
        )
    }

    private fun uncertainWriteMessage(): String {
        return "Chưa xác nhận được kết quả thao tác. " +
                "Vui lòng tải lại lịch uống trước khi thử lại."
    }
}