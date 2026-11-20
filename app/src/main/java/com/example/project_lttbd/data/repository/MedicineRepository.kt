
package com.example.project_lttbd.data.repository

import com.example.project_lttbd.data.image.MedicinePhotoCodec
import com.example.project_lttbd.data.remote.BackendSession
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.data.remote.MedicineApiClient
import com.example.project_lttbd.data.remote.MedicineApiDto
import com.example.project_lttbd.data.remote.dto.MedicineDto
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class MedicineRecord(
    val id: String,
    val data: MedicineDto
)

class MedicineRepository {

    private val api = MedicineApiClient.api

    private val scope = CoroutineScope(Dispatchers.IO)

    // ======================================================
    // GET DANH SÁCH: FASTAPI
    // ======================================================

    fun getMedicines(): Task<List<MedicineRecord>> {
        return runAsTask {
            val uid = currentUid()
            val authorization =
                BackendSession.authorizationHeader()

            ensureCurrentUser(uid)

            val response = api.getMedicines(authorization)

            ensureCurrentUser(uid)

            response.map { medicine ->
                medicine.toMedicineRecord()
            }
        }
    }

    // ======================================================
    // GET CHI TIẾT: FASTAPI
    // ======================================================

    fun getMedicine(
        medicineId: String
    ): Task<MedicineRecord> {
        return runAsTask {
            validateMedicineId(medicineId)

            val uid = currentUid()
            val authorization =
                BackendSession.authorizationHeader()

            ensureCurrentUser(uid)

            val response = api.getMedicine(
                authorization,
                medicineId
            )

            ensureCurrentUser(uid)

            response.toMedicineRecord()
        }
    }

    // ======================================================
    // POST: FIRESTORE CŨ - SẼ CHUYỂN Ở B2.2
    // ======================================================

    fun addMedicine(
        medicine: MedicineDto
    ): Task<String> {
        return withCurrentUser { collection ->
            val fields = medicineFields(
                medicine
            ).toMutableMap()

            fields["createdAt"] =
                FieldValue.serverTimestamp()

            fields["updatedAt"] =
                FieldValue.serverTimestamp()

            val document = collection.document()

            document.set(fields).continueWith { task ->
                if (!task.isSuccessful) {
                    throw task.exception
                        ?: IllegalStateException(
                            "Không thể thêm thuốc."
                        )
                }

                document.id
            }
        }
    }

    // ======================================================
    // PUT: FIRESTORE CŨ - SẼ CHUYỂN Ở B2.2
    // ======================================================

    fun updateMedicine(
        medicineId: String,
        medicine: MedicineDto
    ): Task<Void> {
        return withCurrentUser { collection ->
            validateMedicineId(medicineId)

            val fields = medicineFields(
                medicine
            ).toMutableMap()

            fields["updatedAt"] =
                FieldValue.serverTimestamp()

            collection.document(medicineId)
                .update(fields)
        }
    }

    // ======================================================
    // DELETE: FIRESTORE CŨ - SẼ CHUYỂN Ở B2.2
    // ======================================================

    fun deleteMedicine(
        medicineId: String
    ): Task<Void> {
        return withCurrentUser { collection ->
            validateMedicineId(medicineId)

            collection.document(medicineId)
                .delete()
        }
    }

    // ======================================================
    // CHUYỂN JSON API SANG MODEL HIỆN TẠI
    // ======================================================

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
            // FastAPI trả thời gian ISO 8601.
            // Firestore Admin SDK lưu UTC.
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

            val date = requireNotNull(
                parser.parse(normalized)
            )

            Timestamp(date)
        }.getOrNull()
    }

    // ======================================================
    // GIỮ KIỂU TASK ĐỂ KHÔNG PHẢI SỬA MÀN HÌNH
    // ======================================================

    private fun <T> runAsTask(
        operation: suspend () -> T
    ): Task<T> {
        val source = TaskCompletionSource<T>()

        scope.launch {
            try {
                source.trySetResult(operation())
            } catch (error: Exception) {
                source.trySetException(error)
            }
        }

        return source.task
    }

    private fun currentUid(): String {
        return FirebaseProvider.auth.currentUser?.uid
            ?: throw IllegalStateException(
                "Vui lòng đăng nhập để xem thuốc."
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

    // ======================================================
    // GIỮ CÁC HELPER FIRESTORE CHO B2.2
    // ======================================================

    private fun medicineFields(
        medicine: MedicineDto
    ): Map<String, Any?> {
        require(medicine.medicineName.isNotBlank()) {
            "Vui lòng nhập tên thuốc."
        }

        val imageUrl = medicine.imageUrl

        require(
            MedicinePhotoCodec.isStoredSource(imageUrl)
        ) {
            "Ảnh thuốc chưa hợp lệ. Vui lòng chọn lại."
        }

        return mapOf(
            "medicineName" to medicine.medicineName.trim(),
            "strength" to medicine.strength.trim(),
            "strengthUnit" to medicine.strengthUnit.trim(),
            "dosageForm" to medicine.dosageForm.trim(),
            "note" to medicine.note.trim(),
            "imageUrl" to imageUrl
        )
    }

    private fun <T> withCurrentUser(
        action: (CollectionReference) -> Task<T>
    ): Task<T> {
        return try {
            val uid = currentUid()

            action(
                FirebaseProvider.medicinesCollection(uid)
            )
        } catch (error: Exception) {
            Tasks.forException(error)
        }
    }
}
