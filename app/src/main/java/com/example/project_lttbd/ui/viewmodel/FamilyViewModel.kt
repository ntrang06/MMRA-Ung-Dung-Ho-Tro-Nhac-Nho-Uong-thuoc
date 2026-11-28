package com.example.project_lttbd.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_lttbd.data.remote.FamilyException
import com.example.project_lttbd.data.remote.FamilyRepository
import com.example.project_lttbd.data.remote.dto.ScheduleResponseDto
import com.example.project_lttbd.data.remote.dto.family.FamilyAlertDto
import com.example.project_lttbd.data.remote.dto.family.FamilyMemberResponseDto
import com.example.project_lttbd.data.remote.dto.family.FamilyPermissionsDto
import com.example.project_lttbd.data.remote.dto.family.InvitationResponseDto
import com.example.project_lttbd.data.remote.dto.family.SharedDoseDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class FamilyViewModel(
    private val repository: FamilyRepository = FamilyRepository()
) : ViewModel() {

    var members by mutableStateOf<List<FamilyMemberResponseDto>>(emptyList())
        private set
    var isLoadingMembers by mutableStateOf(false)
        private set
    var membersError by mutableStateOf<String?>(null)
        private set

    var isCreatingInvitation by mutableStateOf(false)
        private set
    var createdInvitation by mutableStateOf<InvitationResponseDto?>(null)
        private set
    var createInvitationError by mutableStateOf<String?>(null)
        private set

    var isAcceptingInvitation by mutableStateOf(false)
        private set
    var acceptedLinkId by mutableStateOf<String?>(null)
        private set
    var acceptInvitationError by mutableStateOf<String?>(null)
        private set

    var isUpdatingPermissions by mutableStateOf(false)
        private set
    var updatePermissionsError by mutableStateOf<String?>(null)
        private set

    var isRevokingLink by mutableStateOf(false)
        private set
    var revokeLinkError by mutableStateOf<String?>(null)
        private set

    var sharedSchedules by mutableStateOf<List<ScheduleResponseDto>>(emptyList())
        private set
    var sharedDoses by mutableStateOf<List<SharedDoseDto>>(emptyList())
        private set
    var isLoadingSharedData by mutableStateOf(false)
        private set
    var sharedDataError by mutableStateOf<String?>(null)
        private set

    var familyAlerts by mutableStateOf<List<FamilyAlertDto>>(emptyList())
        private set
    var isLoadingAlerts by mutableStateOf(false)
        private set
    var alertsError by mutableStateOf<String?>(null)
        private set

    var activeDoseDate by mutableStateOf<String?>(null)
        private set
    var activeScheduleLinkId by mutableStateOf<String?>(null)
        private set

    init {
        loadMembers()
    }

    fun loadMembers() {
        if (isLoadingMembers) return
        isLoadingMembers = true
        membersError = null

        viewModelScope.launch {
            try {
                members = repository.listMembers()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: FamilyException) {
                membersError = error.message
            } catch (error: Exception) {
                membersError = "Không thể tải danh sách người thân. Vui lòng thử lại."
            } finally {
                isLoadingMembers = false
            }
        }
    }

    fun createInvitation(onSuccess: ((InvitationResponseDto) -> Unit)? = null) {
        if (isCreatingInvitation) return
        isCreatingInvitation = true
        createInvitationError = null

        viewModelScope.launch {
            try {
                val result = repository.createInvitation()
                createdInvitation = result
                onSuccess?.invoke(result)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: FamilyException) {
                createInvitationError = error.message
            } catch (error: Exception) {
                createInvitationError = "Không thể tạo mã mời. Vui lòng thử lại."
            } finally {
                isCreatingInvitation = false
            }
        }
    }

    fun acceptInvitation(code: String, onSuccess: ((String) -> Unit)? = null) {
        if (isAcceptingInvitation) return
        val trimmedCode = code.trim()
        if (trimmedCode.length < 32) {
            acceptInvitationError = "Mã mời phải có đủ 32 ký tự hex."
            return
        }
        isAcceptingInvitation = true
        acceptInvitationError = null
        acceptedLinkId = null

        viewModelScope.launch {
            try {
                val result = repository.acceptInvitation(trimmedCode)
                acceptedLinkId = result.linkId
                loadMembers()
                onSuccess?.invoke(result.linkId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: FamilyException) {
                acceptInvitationError = error.message
            } catch (error: Exception) {
                acceptInvitationError = "Không thể chấp nhận lời mời. Vui lòng thử lại."
            } finally {
                isAcceptingInvitation = false
            }
        }
    }

    fun declineInvitation(code: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.declineInvitation(code)
                onSuccess()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
            }
        }
    }

    fun cancelInvitation(invitationId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.cancelInvitation(invitationId)
                onSuccess()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
            }
        }
    }

    fun updatePermissions(
        linkId: String,
        permissions: FamilyPermissionsDto,
        onSuccess: (() -> Unit)? = null
    ) {
        if (isUpdatingPermissions) return
        isUpdatingPermissions = true
        updatePermissionsError = null

        viewModelScope.launch {
            try {
                repository.updatePermissions(linkId, permissions)
                loadMembers()
                onSuccess?.invoke()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: FamilyException) {
                updatePermissionsError = error.message
            } catch (error: Exception) {
                updatePermissionsError = "Không thể cập nhật quyền chia sẻ. Vui lòng thử lại."
            } finally {
                isUpdatingPermissions = false
            }
        }
    }

    fun revokeLink(linkId: String, onSuccess: (() -> Unit)? = null) {
        if (isRevokingLink) return
        isRevokingLink = true
        revokeLinkError = null

        viewModelScope.launch {
            try {
                repository.revokeLink(linkId)
                loadMembers()
                onSuccess?.invoke()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: FamilyException) {
                revokeLinkError = error.message
            } catch (error: Exception) {
                revokeLinkError = "Không thể hủy liên kết. Vui lòng thử lại."
            } finally {
                isRevokingLink = false
            }
        }
    }

    fun loadSharedSchedules(linkId: String) {
        val targetLinkId = linkId.trim()
        activeScheduleLinkId = targetLinkId
        isLoadingSharedData = true
        sharedDataError = null
        sharedSchedules = emptyList()

        viewModelScope.launch {
            try {
                val result = repository.getSharedSchedules(targetLinkId)
                if (activeScheduleLinkId == targetLinkId) {
                    sharedSchedules = result
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: FamilyException) {
                if (activeScheduleLinkId == targetLinkId) {
                    sharedDataError = error.message
                }
            } catch (error: Exception) {
                if (activeScheduleLinkId == targetLinkId) {
                    sharedDataError = "Không thể tải lịch uống của người thân."
                }
            } finally {
                if (activeScheduleLinkId == targetLinkId) {
                    isLoadingSharedData = false
                }
            }
        }
    }

    fun loadSharedDoses(linkId: String, date: String) {
        val targetDate = date.trim()
        activeDoseDate = targetDate
        isLoadingSharedData = true
        sharedDataError = null
        sharedDoses = emptyList()

        viewModelScope.launch {
            try {
                val result = repository.getSharedDoses(linkId, targetDate)
                if (activeDoseDate == targetDate) {
                    sharedDoses = result
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: FamilyException) {
                if (activeDoseDate == targetDate) {
                    sharedDataError = error.message
                }
            } catch (error: Exception) {
                if (activeDoseDate == targetDate) {
                    sharedDataError = "Không thể tải trạng thái liều của người thân."
                }
            } finally {
                if (activeDoseDate == targetDate) {
                    isLoadingSharedData = false
                }
            }
        }
    }

    fun loadFamilyAlerts(date: String? = null, linkId: String? = null) {
        isLoadingAlerts = true
        alertsError = null
        viewModelScope.launch {
            try {
                familyAlerts = repository.getFamilyAlerts(date, linkId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: FamilyException) {
                alertsError = error.message
            } catch (error: Exception) {
                alertsError = "Không thể tải danh sách cảnh báo."
            } finally {
                isLoadingAlerts = false
            }
        }
    }

    fun clearErrors() {
        membersError = null
        createInvitationError = null
        acceptInvitationError = null
        updatePermissionsError = null
        revokeLinkError = null
        sharedDataError = null
        alertsError = null
    }

    fun clearAcceptState() {
        acceptedLinkId = null
        acceptInvitationError = null
    }
}
