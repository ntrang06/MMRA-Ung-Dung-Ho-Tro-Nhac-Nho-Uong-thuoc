
package com.example.project_lttbd.data.repository

import com.example.project_lttbd.data.image.MedicinePhotoCodec
import com.example.project_lttbd.data.remote.BackendSession
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.data.remote.MedicationOverviewRepository
import com.example.project_lttbd.data.remote.MedicineApiClient
import com.example.project_lttbd.data.remote.MedicineApiDto
import com.example.project_lttbd.data.remote.MedicineRequestDto
import com.example.project_lttbd.data.remote.dto.MedicineDto
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

data class MedicineRecord(
    val id: String,
    val data: MedicineDto
)

class MedicineRepository {

    private val api = MedicineApiClient.api

    private val overviewRepository =
        MedicationOverviewRepository()

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    // GET - DANH SÁCH
    fun getMedicines(): Task<List<MedicineRecord>> {
        return runAsTask {
            val uid = currentUid()
            val authorization =
                BackendSession.authorizationHeader()

            ensureCurrentUser(uid)

            val result = api.getMedicines(
                authorization
            )

            ensureCurrentUser(uid)

            result.map {
                it.toMedicineRecord()
            }
        }
    }

    // GET - CHI TIẾT
    fun getMedicine(
        medicineId: String
    ): Task<MedicineRecord> {
        return runAsTask {
            validateMedicineId(medicineId)

            val uid = currentUid()
            val authorization =
                BackendSession.authorizationHeader()

            ensureCurrentUser(uid)

            val result = api.getMedicine(
                authorization,
                medicineId
            )

            ensureCurrentUser(uid)

            result.toMedicineRecord()
        }
    }

    // POST - THÊM THUỐC QUA FASTAPI
    fun addMedicine(
        medicine: MedicineDto
    ): Task<String> {
        return runAsTask {
            val payload = medicine.toRequest()

            val uid = currentUid()
            val authorization =
                BackendSession.authorizationHeader()

            ensureCurrentUser(uid)

            val response = try {
                api.addMedicine(
                    authorization,
                    payload
                )
            } catch (error: IOException) {
                throw IllegalStateException(
                    "Chưa xác nhận được thuốc đã lưu hay chưa. " +
                            "Vui lòng tải lại danh sách trước khi thử thêm.",
                    error
                )
            }

            ensureCurrentUser(uid)

            val result = requireWriteResponse(
                response
            )

            check(
                result.medicineId.isNotBlank() &&
                        '/' !in result.medicineId
            ) {
                "Phản hồi lưu thuốc chưa hợp lệ. " +
                        "Vui lòng tải lại danh sách."
            }

            result.medicineId
        }
    }

    // PUT - SỬA THUỐC QUA FASTAPI
    fun updateMedicine(
        medicineId: String,
        medicine: MedicineDto
    ): Task<Void> {
        return runAsVoidTask {
            validateMedicineId(medicineId)

            val payload = medicine.toRequest()
            val uid = currentUid()
            val authorization =
                BackendSession.authorizationHeader()

            ensureCurrentUser(uid)

            val response = try {
                api.updateMedicine(
                    authorization,
                    medicineId,
                    payload
                )
            } catch (error: IOException) {
                throw IllegalStateException(
                    "Chưa xác nhận được kết quả sửa thuốc. " +
                            "Vui lòng tải lại trước khi thử lại.",
                    error
                )
            }

            ensureCurrentUser(uid)

            val result = requireWriteResponse(
                response
            )

            check(result.medicineId == medicineId) {
                "Máy chủ trả về mã thuốc không đúng. " +
                        "Vui lòng tải lại thông tin thuốc."
            }
        }
    }

    // DELETE - DÙNG API XÓA ĐÃ CÓ TRONG BACKEND
    fun deleteMedicine(
        medicineId: String
    ): Task<Void> {
        return runAsVoidTask {
            validateMedicineId(medicineId)

            val uid = currentUid()

            overviewRepository.deleteMedicine(
                medicineId
            )

            ensureCurrentUser(uid)
        }
    }

    // CHUYỂN MEDICINE DTO SANG REQUEST BODY
    private fun MedicineDto.toRequest():
            MedicineRequestDto {

        require(medicineName.isNotBlank()) {
            "Vui lòng nhập tên thuốc."
        }

        require(
            MedicinePhotoCodec.isStoredSource(
                imageUrl
            )
        ) {
            "Ảnh thuốc chưa hợp lệ. Vui lòng chọn lại ảnh."
        }

        return MedicineRequestDto(
            medicineName = medicineName.trim(),
            strength = strength.trim(),
            strengthUnit = strengthUnit.trim(),
            dosageForm = dosageForm.trim(),
            note = note.trim(),
            imageUrl = imageUrl
        )
    }

    // XỬ LÝ HTTP RESPONSE CỦA POST VÀ PUT
    private fun requireWriteResponse(
        response: Response<MedicineApiDto>
    ): MedicineApiDto {

        if (!response.isSuccessful) {
            response.errorBody()?.close()

            val message = when (response.code()) {
                400, 422 ->
                    "Thông tin thuốc không hợp lệ. " +
                            "Vui lòng kiểm tra lại."

                401 ->
                    "Phiên đăng nhập hết hạn. " +
                            "Vui lòng đăng nhập lại."

                403 ->
                    "Bạn không có quyền sửa thuốc."

                404 ->
                    "Không tìm thấy thuốc cần sửa."

                409 ->
                    "Dữ liệu đã thay đổi. " +
                            "Vui lòng tải lại."

                else ->
                    "Chưa xác nhận được kết quả lưu thuốc. " +
                            "Vui lòng tải lại danh sách để kiểm tra."
            }

            throw IllegalStateException(message)
        }

        return response.body()
            ?: throw IllegalStateException(
                "Máy chủ không trả về thông tin thuốc. " +
                        "Vui lòng tải lại danh sách."
            )
    }

    // CHUYỂN RESPONSE THÀNH MODEL GIAO DIỆN
    private fun MedicineApiDto.toMedicineRecord():
            MedicineRecord {

        require(
            medicineId.isNotBlank() &&
                    '/' !in medicineId
        ) {
            "Mã thuốc trả về không hợp lệ."
        }

        return MedicineRecord(
            id = medicineId,
            data = MedicineDto(
                medicineName = medicineName,
                strength = strength,
                strengthUnit = strengthUnit,
                dosageForm = dosageForm,
                note = note,
                imageUrl = imageUrl,
                createdAt = parseTimestamp(createdAt),
                updatedAt = parseTimestamp(updatedAt)
            )
        )
    }

    private fun parseTimestamp(
        value: String?
    ): Timestamp? {
        if (value.isNullOrBlank()) return null

        return runCatching {
            val normalized = value
                .substringBefore('.')
                .substringBefore('+')
                .removeSuffix("Z")

            val parser = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.US
            ).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }

            Timestamp(
                requireNotNull(
                    parser.parse(normalized)
                )
            )
        }.getOrNull()
    }

    // GIỮ TASK ĐỂ TƯƠNG THÍCH GIAO DIỆN CŨ
    private fun <T> runAsTask(
        operation: suspend () -> T
    ): Task<T> {
        val source = TaskCompletionSource<T>()

        scope.launch {
            try {
                source.trySetResult(operation())
            } catch (error: CancellationException) {
                source.trySetException(error)
            } catch (error: Exception) {
                source.trySetException(error)
            }
        }

        return source.task
    }

    private fun runAsVoidTask(
        operation: suspend () -> Unit
    ): Task<Void> {
        val source = TaskCompletionSource<Void>()

        scope.launch {
            try {
                operation()
                source.trySetResult(null)
            } catch (error: CancellationException) {
                source.trySetException(error)
            } catch (error: Exception) {
                source.trySetException(error)
            }
        }

        return source.task
    }

    private fun currentUid(): String {
        return FirebaseProvider.auth.currentUser?.uid
            ?: throw IllegalStateException(
                "Vui lòng đăng nhập để quản lý thuốc."
            )
    }

    private fun ensureCurrentUser(uid: String) {
        check(
            FirebaseProvider.auth.currentUser?.uid == uid
        ) {
            "Tài khoản đã thay đổi. Vui lòng tải lại."
        }
    }

    private fun validateMedicineId(
        medicineId: String
    ) {
        require(
            medicineId.isNotBlank() &&
                    '/' !in medicineId
        ) {
            "Mã thuốc không hợp lệ."
        }
    }
}
