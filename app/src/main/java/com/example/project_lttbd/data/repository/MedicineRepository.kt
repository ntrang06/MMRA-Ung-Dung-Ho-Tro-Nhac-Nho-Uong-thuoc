
package com.example.project_lttbd.data.repository

import com.example.project_lttbd.data.image.MedicinePhotoCodec
import com.example.project_lttbd.data.remote.BackendSession
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.data.remote.MedicationOverviewRepository
import com.example.project_lttbd.data.remote.MedicineApiClient
import com.example.project_lttbd.data.remote.MedicineApiDto
import com.example.project_lttbd.data.remote.MedicineRequestDto
import com.example.project_lttbd.data.remote.MedicineUpdateRequestDto
import com.example.project_lttbd.data.remote.dto.MedicineDto
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.gson.Gson
import java.io.IOException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import retrofit2.Response

data class MedicineRecord(
    val id: String,
    val data: MedicineDto
)

class MedicineVersionConflictException(
    message: String
) : IllegalStateException(message)

class MedicineRepository {

    private val api = MedicineApiClient.api

    private val overviewRepository =
        MedicationOverviewRepository()

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private val gson = Gson()

    // GET - DANH SÁCH THUỐC
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

    // GET - CHI TIẾT THUỐC
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

    // POST - THÊM THUỐC KHÔNG TẠO TRÙNG
    fun addMedicine(
        medicine: MedicineDto
    ): Task<String> {
        return runAsTask {
            val payload = medicine.toCreateRequest()

            val uid = currentUid()
            val authorization =
                BackendSession.authorizationHeader()

            ensureCurrentUser(uid)

            // Lưu khóa trước khi gửi request.
            // Khi mạng mất, lần retry của cùng nội dung
            // sẽ sử dụng lại chính khóa này.
            val request = pendingCreateRequest(
                uid = uid,
                payload = payload
            )

            val response = try {
                api.addMedicine(
                    authorization = authorization,
                    idempotencyKey = request.key,
                    medicine = payload
                )
            } catch (error: IOException) {
                throw IllegalStateException(
                    "Chưa xác nhận được thuốc đã lưu hay chưa. " +
                            "Vui lòng kiểm tra danh sách rồi thử lại.",
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

            check(result.version >= 1) {
                "Phiên bản thuốc trả về không hợp lệ."
            }

            // Chỉ xóa khóa sau khi Backend xác nhận
            // yêu cầu đã tạo hoặc được phát lại thành công.
            clearPendingCreateRequest(request)

            result.medicineId
        }
    }

    // PUT - SỬA THUỐC CÓ KIỂM TRA VERSION
    fun updateMedicine(
        medicineId: String,
        medicine: MedicineDto
    ): Task<Void> {
        return runAsVoidTask {
            validateMedicineId(medicineId)

            require(medicine.version >= 1) {
                "Chưa có phiên bản thuốc hợp lệ. " +
                        "Vui lòng mở lại màn sửa thuốc."
            }

            val payload = medicine.toUpdateRequest()

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

            check(result.version > medicine.version) {
                "Phiên bản thuốc không được cập nhật. " +
                        "Vui lòng tải lại."
            }
        }
    }

    // DELETE - GIỮ NGUYÊN LUỒNG XÓA
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

    // =====================================================
    // B3.3 - LƯU KHÓA YÊU CẦU TRÊN THIẾT BỊ
    // =====================================================

    private data class PendingCreateRequest(
        val preferenceKey: String,
        val key: String
    )

    private fun pendingCreateRequest(
        uid: String,
        payload: MedicineRequestDto
    ): PendingCreateRequest {
        val preferenceKey = createPreferenceKey(
            uid = uid,
            payload = payload
        )

        val preferences = FirebaseApp.getInstance()
            .applicationContext
            .getSharedPreferences(
                "mmra_medicine_pending_requests",
                android.content.Context.MODE_PRIVATE
            )

        synchronized(createRequestLock) {
            val previous = preferences.getString(
                preferenceKey,
                null
            )

            if (!previous.isNullOrBlank()) {
                return PendingCreateRequest(
                    preferenceKey = preferenceKey,
                    key = previous
                )
            }

            val newKey = UUID.randomUUID().toString()

            val persisted = preferences.edit()
                .putString(
                    preferenceKey,
                    newKey
                )
                .commit()

            check(persisted) {
                "Không lưu được mã yêu cầu trên máy. " +
                        "Vui lòng kiểm tra bộ nhớ và thử lại."
            }

            return PendingCreateRequest(
                preferenceKey = preferenceKey,
                key = newKey
            )
        }
    }

    private fun clearPendingCreateRequest(
        request: PendingCreateRequest
    ) {
        val preferences = FirebaseApp.getInstance()
            .applicationContext
            .getSharedPreferences(
                "mmra_medicine_pending_requests",
                android.content.Context.MODE_PRIVATE
            )

        synchronized(createRequestLock) {
            // Chỉ xóa nếu đây vẫn là khóa
            // đã sử dụng cho request thành công.
            val current = preferences.getString(
                request.preferenceKey,
                null
            )

            if (current == request.key) {
                preferences.edit()
                    .remove(request.preferenceKey)
                    .apply()
            }
        }
    }

    private fun createPreferenceKey(
        uid: String,
        payload: MedicineRequestDto
    ): String {
        // Băm UID cùng nội dung yêu cầu.
        // Không lưu trực tiếp tên thuốc hoặc ảnh
        // trong tên khóa SharedPreferences.
        val content = "$uid\u0000${gson.toJson(payload)}"

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(content.toByteArray(Charsets.UTF_8))

        val hex = digest.joinToString("") {
            "%02x".format(it.toInt() and 0xFF)
        }

        return "pending_$hex"
    }

    // =====================================================
    // DỮ LIỆU CHUNG DÙNG CHO POST VÀ PUT
    // =====================================================

    private fun MedicineDto.toCreateRequest():
            MedicineRequestDto {

        require(medicineName.isNotBlank()) {
            "Vui lòng nhập tên thuốc."
        }

        require(
            MedicinePhotoCodec.isStoredSource(
                imageUrl
            )
        ) {
            "Ảnh thuốc chưa hợp lệ. " +
                    "Vui lòng chọn lại ảnh."
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

    private fun MedicineDto.toUpdateRequest():
            MedicineUpdateRequestDto {

        val fields = toCreateRequest()

        require(version >= 1) {
            "Phiên bản thuốc không hợp lệ."
        }

        return MedicineUpdateRequestDto(
            medicineName = fields.medicineName,
            strength = fields.strength,
            strengthUnit = fields.strengthUnit,
            dosageForm = fields.dosageForm,
            note = fields.note,
            imageUrl = fields.imageUrl,
            expectedVersion = version
        )
    }

    // =====================================================
    // XỬ LÝ HTTP RESPONSE
    // =====================================================

    private fun requireWriteResponse(
        response: Response<MedicineApiDto>
    ): MedicineApiDto {

        if (!response.isSuccessful) {
            response.errorBody()?.close()

            val message = when (response.code()) {
                400, 422 ->
                    "Thông tin gửi lên chưa hợp lệ. " +
                            "Vui lòng kiểm tra và thử lại."

                401 ->
                    "Phiên đăng nhập hết hạn. " +
                            "Vui lòng đăng nhập lại."

                403 ->
                    "Bạn không có quyền sửa thuốc."

                404 ->
                    "Không tìm thấy thuốc cần sửa."

                409 ->
                    "Yêu cầu bị xung đột. " +
                            "Vui lòng tải lại dữ liệu thuốc."

                else ->
                    "Chưa xác nhận được kết quả lưu thuốc. " +
                            "Vui lòng tải lại danh sách để kiểm tra."
            }

            if (response.code() == 409) {
                throw MedicineVersionConflictException(
                    message
                )
            }

            throw IllegalStateException(message)
        }

        return response.body()
            ?: throw IllegalStateException(
                "Máy chủ không trả về thông tin thuốc. " +
                        "Vui lòng tải lại danh sách."
            )
    }

    // =====================================================
    // CHUYỂN API DTO SANG MODEL GIAO DIỆN
    // =====================================================

    private fun MedicineApiDto.toMedicineRecord():
            MedicineRecord {

        require(
            medicineId.isNotBlank() &&
                    '/' !in medicineId
        ) {
            "Mã thuốc trả về không hợp lệ."
        }

        require(version >= 1) {
            "Phiên bản thuốc trả về không hợp lệ. " +
                    "Vui lòng kiểm tra Backend."
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
                updatedAt = parseTimestamp(updatedAt),
                version = version
            )
        )
    }

    private fun parseTimestamp(
        value: String?
    ): Timestamp? {
        if (value.isNullOrBlank()) {
            return null
        }

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

    // =====================================================
    // GOOGLE TASK - GIỮ TƯƠNG THÍCH GIAO DIỆN
    // =====================================================

    private fun <T> runAsTask(
        operation: suspend () -> T
    ): Task<T> {
        val source = TaskCompletionSource<T>()

        scope.launch {
            try {
                source.trySetResult(
                    operation()
                )
            } catch (
                error: CancellationException
            ) {
                source.trySetException(error)
            } catch (
                error: Exception
            ) {
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
            } catch (
                error: CancellationException
            ) {
                source.trySetException(error)
            } catch (
                error: Exception
            ) {
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

    private fun ensureCurrentUser(
        uid: String
    ) {
        check(
            FirebaseProvider.auth.currentUser?.uid == uid
        ) {
            "Tài khoản đã thay đổi. " +
                    "Vui lòng tải lại."
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

    companion object {
        private val createRequestLock = Any()
    }
}
