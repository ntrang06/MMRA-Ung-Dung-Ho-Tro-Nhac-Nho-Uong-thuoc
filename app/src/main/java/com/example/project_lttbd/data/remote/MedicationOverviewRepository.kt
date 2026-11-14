package com.example.project_lttbd.data.remote

import com.example.project_lttbd.data.remote.dto.DoseResponseDto
import com.example.project_lttbd.data.remote.dto.DoseStatusDto
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException

data class HomeResponseDto(
    val localDate: String,
    val timeZone: String,
    val serverTime: String,
    val doses: List<DoseResponseDto>,
    val nextDose: DoseResponseDto?,
    val totalDoses: Int,
    val takenDoses: Int,
    val skippedDoses: Int,
    val missedDoses: Int,
    val pendingDoses: Int,
    val progressPercent: Int
)

data class MedicineDeletionDto(
    val medicineId: String,
    val doseIdsToCancel: List<String>
)

interface MedicationOverviewApi {
    @GET("api/v1/home")
    suspend fun getHome(
        @Header("Authorization") authorization: String,
        @Query("date") date: String
    ): Response<HomeResponseDto>

    @DELETE("api/v1/medicines/{medicineId}")
    suspend fun deleteMedicine(
        @Header("Authorization") authorization: String,
        @Path("medicineId") medicineId: String
    ): Response<MedicineDeletionDto>
}

class MedicationOverviewRepository(
    private val api: MedicationOverviewApi =
        ScheduleApiClient.overviewApi
) {
    suspend fun getHome(date: String): HomeResponseDto {
        require(
            Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(date)
        ) {
            "Ngày cần có định dạng yyyy-MM-dd."
        }

        val result = execute {
            api.getHome(it, date)
        }

        try {
            validateHomeResponse(result, date)
        } catch (_: IllegalArgumentException) {
            throw invalidResponse(false)
        } catch (_: NullPointerException) {
            throw invalidResponse(false)
        }

        return result
    }

    suspend fun deleteMedicine(
        medicineId: String
    ): MedicineDeletionDto {
        require(
            medicineId.isNotBlank() && '/' !in medicineId
        ) {
            "Mã thuốc không hợp lệ."
        }

        val result = execute(isWrite = true) {
            api.deleteMedicine(it, medicineId)
        }

        if (result.medicineId != medicineId) {
            throw invalidResponse(true)
        }

        return result
    }

    private suspend fun <T> execute(
        isWrite: Boolean = false,
        call: suspend (String) -> Response<T>
    ): T {
        val uid = FirebaseProvider.auth.currentUser?.uid
            ?: throw DoseException(
                "Vui lòng đăng nhập lại.",
                "unauthenticated"
            )

        fun ensureOwner() {
            if (FirebaseProvider.auth.currentUser?.uid != uid) {
                throw CancellationException(
                    "Tài khoản đã thay đổi."
                )
            }
        }

        val authorization = try {
            BackendSession.authorizationHeader()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            ensureOwner()

            throw DoseException(
                "Chưa xác thực được phiên đăng nhập. Vui lòng thử lại.",
                "auth_unavailable"
            )
        }

        ensureOwner()

        try {
            val response = call(authorization)

            try {
                ensureOwner()
            } catch (cancelled: CancellationException) {
                response.errorBody()?.close()
                throw cancelled
            }

            if (!response.isSuccessful) {
                response.errorBody()?.close()

                val message = when (response.code()) {
                    401 ->
                        "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."

                    403 ->
                        "Bạn không có quyền thực hiện thao tác này."

                    404 ->
                        "Không tìm thấy dữ liệu. Vui lòng kéo xuống để cập nhật."

                    409 ->
                        "Dữ liệu đã thay đổi. Vui lòng tải lại trước khi thử tiếp."

                    422 ->
                        "Thông tin gửi lên chưa hợp lệ."

                    429 ->
                        "Bạn thao tác quá nhanh. Vui lòng thử lại sau."

                    else -> {
                        if (isWrite) {
                            uncertainWrite()
                        } else {
                            "Chưa tải được Trang chủ. Vui lòng thử lại."
                        }
                    }
                }

                throw DoseException(
                    message,
                    "request_failed",
                    response.code()
                )
            }

            return response.body()
                ?: throw invalidResponse(isWrite)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            ensureOwner()

            throw DoseException(
                if (isWrite) {
                    uncertainWrite()
                } else {
                    "Không kết nối được máy chủ. " +
                            "Vui lòng kiểm tra kết nối và thử lại."
                },
                "network_error"
            )
        } catch (_: JsonParseException) {
            ensureOwner()
            throw invalidResponse(isWrite)
        }
    }

    private fun uncertainWrite(): String {
        return "Chưa xác nhận được kết quả xóa. " +
                "Vui lòng tải lại danh sách trước khi thử tiếp."
    }

    private fun invalidResponse(
        isWrite: Boolean
    ): DoseException {
        return DoseException(
            if (isWrite) {
                uncertainWrite()
            } else {
                "Dữ liệu Trang chủ chưa hợp lệ. " +
                        "Vui lòng kéo xuống để thử lại."
            },
            "invalid_response"
        )
    }
}

internal fun validateHomeResponse(
    result: HomeResponseDto,
    date: String
) {
    require(
        result.localDate == date &&
                result.timeZone == "Asia/Ho_Chi_Minh"
    )

    require(
        result.doses.all {
            it.localDate == date
        }
    )

    require(
        result.doses.map {
            it.doseId
        }.distinct().size == result.doses.size
    )

    val active = result.doses.filter {
        it.status != DoseStatusDto.CANCELLED
    }

    val pending = active.filter {
        it.status == DoseStatusDto.PENDING
    }

    require(result.totalDoses == active.size)

    require(
        result.takenDoses == active.count {
            it.status == DoseStatusDto.TAKEN
        }
    )

    require(
        result.skippedDoses == active.count {
            it.status == DoseStatusDto.SKIPPED
        }
    )

    require(
        result.missedDoses == active.count {
            it.status == DoseStatusDto.MISSED
        }
    )

    require(result.pendingDoses == pending.size)

    require(
        (result.nextDose == null) == pending.isEmpty()
    )

    require(
        result.nextDose == null ||
                result.nextDose in pending
    )

    val expectedProgress = if (active.isEmpty()) {
        0
    } else {
        result.takenDoses * 100 / active.size
    }

    require(result.progressPercent == expectedProgress)
}