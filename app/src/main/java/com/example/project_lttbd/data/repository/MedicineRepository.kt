package com.example.project_lttbd.data.repository

import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.data.remote.dto.MedicineDto
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query

data class MedicineRecord(
    val id: String,
    val data: MedicineDto
)

class MedicineRepository {

    fun getMedicines(): Task<List<MedicineRecord>> {
        return withCurrentUser { collection ->
            collection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .continueWith { task ->
                    if (!task.isSuccessful) {
                        throw task.exception
                            ?: IllegalStateException(
                                "Không thể tải danh sách thuốc."
                            )
                    }

                    task.result.documents.map { document ->
                        val medicine = document.toObject(
                            MedicineDto::class.java
                        ) ?: throw IllegalStateException(
                            "Dữ liệu thuốc không hợp lệ."
                        )

                        MedicineRecord(
                            id = document.id,
                            data = medicine
                        )
                    }
                }
        }
    }

    fun addMedicine(
        medicine: MedicineDto
    ): Task<String> {
        return withCurrentUser { collection ->
            val fields = medicineFields(medicine).toMutableMap()

            fields["createdAt"] = FieldValue.serverTimestamp()
            fields["updatedAt"] = FieldValue.serverTimestamp()

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

    fun updateMedicine(
        medicineId: String,
        medicine: MedicineDto
    ): Task<Void> {
        return withCurrentUser { collection ->
            validateMedicineId(medicineId)

            val fields = medicineFields(medicine).toMutableMap()

            fields["updatedAt"] = FieldValue.serverTimestamp()

            collection
                .document(medicineId)
                .update(fields)
        }
    }

    fun deleteMedicine(
        medicineId: String
    ): Task<Void> {
        return withCurrentUser { collection ->
            validateMedicineId(medicineId)

            collection
                .document(medicineId)
                .delete()
        }
    }

    private fun medicineFields(
        medicine: MedicineDto
    ): Map<String, Any?> {
        require(medicine.medicineName.isNotBlank()) {
            "Vui lòng nhập tên thuốc."
        }

        val imageUrl = medicine.imageUrl

        require(
            imageUrl == null ||
                    imageUrl.startsWith("https://")
        ) {
            "Ảnh thuốc phải là URL HTTPS đã tải lên máy chủ."
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

    private fun <T> withCurrentUser(
        action: (CollectionReference) -> Task<T>
    ): Task<T> {
        return try {
            val uid = FirebaseProvider.auth.currentUser?.uid
                ?: throw IllegalStateException(
                    "Vui lòng đăng nhập để quản lý thuốc."
                )

            val collection =
                FirebaseProvider.medicinesCollection(uid)

            action(collection)
        } catch (exception: Exception) {
            Tasks.forException(exception)
        }
    }
}