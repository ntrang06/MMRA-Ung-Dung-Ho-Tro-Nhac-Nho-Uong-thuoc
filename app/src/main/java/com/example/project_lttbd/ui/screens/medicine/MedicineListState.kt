package com.example.project_lttbd.ui.screens.medicine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.data.remote.ScheduleException
import com.example.project_lttbd.data.remote.ScheduleRepository
import com.example.project_lttbd.data.remote.dto.ScheduleResponseDto
import com.example.project_lttbd.data.repository.MedicineRecord
import com.example.project_lttbd.data.repository.MedicineRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MedicineListState(
    private val repository: MedicineRepository = MedicineRepository(),
    private val auth: FirebaseAuth = FirebaseProvider.auth,
    private val scheduleRepository: ScheduleRepository = ScheduleRepository()
) {
    var medicines by mutableStateOf<List<MedicineRecord>>(emptyList())
        private set

    var schedules by mutableStateOf<List<ScheduleResponseDto>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var isSchedulesLoading by mutableStateOf(false)
        private set

    var isSignedIn by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var scheduleErrorMessage by mutableStateOf<String?>(null)
        private set

    var ownerName by mutableStateOf("bạn")
        private set

    var hasLoadedMedicines by mutableStateOf(false)
        private set

    var hasLoadedSchedules by mutableStateOf(false)
        private set

    var hasFinishedInitialLoad by mutableStateOf(false)
        private set

    private var ownerUid: String? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var scope: CoroutineScope? = null
    private var requestJob: Job? = null
    private var requestVersion = 0

    fun start() {
        if (authListener != null) return

        scope = CoroutineScope(
            SupervisorJob() + Dispatchers.Main.immediate
        )

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

        scope?.cancel()
        scope = null
    }

    fun refresh() {
        if (authListener == null) return

        val user = auth.currentUser

        if (user == null) {
            clearUserData()
            return
        }

        val uid = user.uid

        if (ownerUid != uid) {
            clearUserData()
            ownerUid = uid
        }

        if (isLoading || isSchedulesLoading) return

        val version = ++requestVersion
        requestJob?.cancel()

        isSignedIn = true
        ownerName = user.displayName
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "bạn"

        errorMessage = null
        scheduleErrorMessage = null
        isLoading = true
        isSchedulesLoading = true

        requestJob = scope?.launch {
            launch {
                try {
                    val result = repository.getMedicines().await()

                    if (isCurrentRequest(version, uid)) {
                        medicines = result
                        hasLoadedMedicines = true
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    if (isCurrentRequest(version, uid)) {
                        errorMessage =
                            "Không thể tải danh sách thuốc. Vui lòng thử lại."
                    }
                } finally {
                    if (isCurrentRequest(version, uid)) {
                        isLoading = false
                        finishInitialLoadIfReady()
                    }
                }
            }

            launch {
                try {
                    val result = scheduleRepository.listSchedules()

                    if (isCurrentRequest(version, uid)) {
                        schedules = result
                        hasLoadedSchedules = true
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    if (isCurrentRequest(version, uid)) {
                        scheduleErrorMessage =
                            (error as? ScheduleException)?.message
                                ?: "Chưa tải được lịch uống. Vui lòng thử lại."
                    }
                } finally {
                    if (isCurrentRequest(version, uid)) {
                        isSchedulesLoading = false
                        finishInitialLoadIfReady()
                    }
                }
            }
        }
    }

    private fun finishInitialLoadIfReady() {
        if (!isLoading && !isSchedulesLoading) {
            hasFinishedInitialLoad = true
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

        requestJob?.cancel()
        requestJob = null
        ownerUid = null

        medicines = emptyList()
        schedules = emptyList()
        isLoading = false
        isSchedulesLoading = false
        isSignedIn = false
        errorMessage = null
        scheduleErrorMessage = null
        ownerName = "bạn"

        hasLoadedMedicines = false
        hasLoadedSchedules = false
        hasFinishedInitialLoad = false
    }
}