package com.example.project_lttbd.ui.screens.medicine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.data.repository.MedicineRecord
import com.example.project_lttbd.data.repository.MedicineRepository
import com.google.firebase.auth.FirebaseAuth

class MedicineListState(
    private val repository: MedicineRepository = MedicineRepository(),
    private val auth: FirebaseAuth = FirebaseProvider.auth
) {

    var medicines by mutableStateOf<List<MedicineRecord>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var isSignedIn by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var ownerName by mutableStateOf("bạn")
        private set

    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var requestVersion = 0

    fun start() {
        if (authListener != null) return

        val listener = FirebaseAuth.AuthStateListener {
            refresh()
        }

        authListener = listener
        auth.addAuthStateListener(listener)
    }

    fun stop() {
        authListener?.let {
            auth.removeAuthStateListener(it)
        }

        authListener = null
        clearUserData()
    }

    fun refresh() {
        if (authListener == null) return

        val user = auth.currentUser

        if (user == null) {
            clearUserData()
            return
        }

        val uid = user.uid
        val version = ++requestVersion

        isSignedIn = true
        ownerName = user.displayName
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "bạn"

        medicines = emptyList()
        errorMessage = null
        isLoading = true

        repository.getMedicines()
            .addOnSuccessListener { result ->
                if (isCurrentRequest(version, uid)) {
                    medicines = result
                    isLoading = false
                }
            }
            .addOnFailureListener {
                if (isCurrentRequest(version, uid)) {
                    medicines = emptyList()
                    isLoading = false
                    errorMessage =
                        "Không thể tải danh sách thuốc. Vui lòng thử lại."
                }
            }
    }

    private fun isCurrentRequest(
        version: Int,
        uid: String
    ): Boolean {
        return authListener != null &&
                version == requestVersion &&
                auth.currentUser?.uid == uid
    }

    private fun clearUserData() {
        requestVersion++

        medicines = emptyList()
        isLoading = false
        isSignedIn = false
        errorMessage = null
        ownerName = "bạn"
    }
}