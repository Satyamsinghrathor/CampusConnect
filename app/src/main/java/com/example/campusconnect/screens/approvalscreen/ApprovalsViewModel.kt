package com.example.campusconnect.screens.approvalscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.model.ApprovalStatus
import com.example.campusconnect.data.model.UserProfile
import com.example.campusconnect.data.repository.CollegeRepository
import com.example.campusconnect.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PendingRequest(
    val profile: UserProfile,
    val collegeName: String,
    val departmentName: String
)

data class ApprovalsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val hasAccess: Boolean = true,
    val requests: List<PendingRequest> = emptyList(),
    val busyUids: Set<String> = emptySet(),
    val message: String? = null
)

class ApprovalsViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()
    private val collegeRepository = CollegeRepository()

    private val _uiState = MutableStateFlow(ApprovalsUiState())
    val uiState: StateFlow<ApprovalsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(isLoading = false, error = "You're not signed in.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val approver = userRepository.getProfile(uid).getOrElse {
                _uiState.update { it.copy(isLoading = false, error = LOAD_ERROR) }
                return@launch
            }
            if (approver == null ||
                approver.status != ApprovalStatus.ACTIVE ||
                !approver.userRole.canApprove
            ) {
                _uiState.update { it.copy(isLoading = false, hasAccess = false) }
                return@launch
            }

            val pending = userRepository.getPendingApprovals(approver).getOrElse {
                _uiState.update { it.copy(isLoading = false, error = LOAD_ERROR) }
                return@launch
            }

            val colleges = collegeRepository.getColleges().getOrDefault(emptyList()).associateBy { it.id }
            val departmentNames = pending
                .map { it.departmentId }
                .filter { it.isNotBlank() }
                .distinct()
                .associateWith { id -> collegeRepository.getDepartment(id).getOrNull()?.name.orEmpty() }

            val requests = pending.map {
                PendingRequest(
                    profile = it,
                    collegeName = colleges[it.collegeId]?.name.orEmpty(),
                    departmentName = departmentNames[it.departmentId].orEmpty()
                )
            }
            _uiState.update { it.copy(isLoading = false, hasAccess = true, requests = requests) }
        }
    }

    fun decide(target: UserProfile, approve: Boolean) {
        val approverUid = auth.currentUser?.uid ?: return
        if (target.uid in _uiState.value.busyUids) return

        viewModelScope.launch {
            _uiState.update { it.copy(busyUids = it.busyUids + target.uid) }
            val result = userRepository.setApproval(target.uid, approverUid, approve)
            _uiState.update { state ->
                if (result.isSuccess) {
                    state.copy(
                        requests = state.requests.filterNot { it.profile.uid == target.uid },
                        busyUids = state.busyUids - target.uid,
                        message = if (approve) "${target.name} approved" else "${target.name} rejected"
                    )
                } else {
                    state.copy(
                        busyUids = state.busyUids - target.uid,
                        message = "Couldn't update ${target.name}. Please try again."
                    )
                }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private companion object {
        const val LOAD_ERROR = "Couldn't load requests. Check your connection and try again."
    }
}
