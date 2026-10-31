package com.example.campusconnect.screens.collegescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.model.ApprovalStatus
import com.example.campusconnect.data.model.College
import com.example.campusconnect.data.model.Department
import com.example.campusconnect.data.model.UserRole
import com.example.campusconnect.data.repository.CollegeRepository
import com.example.campusconnect.data.repository.InUseException
import com.example.campusconnect.data.repository.InvalidInputException
import com.example.campusconnect.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CollegeWithDepartments(
    val college: College,
    val departments: List<Department>
)

data class ManageCollegesUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val hasAccess: Boolean = true,
    /** The super admin manages every college; a college admin only sees their own. */
    val isSuperAdmin: Boolean = false,
    val items: List<CollegeWithDepartments> = emptyList(),
    val isWorking: Boolean = false,
    val message: String? = null
)

class ManageCollegesViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()
    private val collegeRepository = CollegeRepository()

    private var adminCollegeId: String = ""

    private val _uiState = MutableStateFlow(ManageCollegesUiState())
    val uiState: StateFlow<ManageCollegesUiState> = _uiState.asStateFlow()

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

            val me = userRepository.getProfile(uid).getOrElse {
                _uiState.update { it.copy(isLoading = false, error = LOAD_ERROR) }
                return@launch
            }
            val role = me?.userRole
            val allowed = me != null &&
                    me.status == ApprovalStatus.ACTIVE &&
                    (role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN)
            if (!allowed) {
                _uiState.update { it.copy(isLoading = false, hasAccess = false) }
                return@launch
            }

            adminCollegeId = me!!.collegeId
            val isSuper = role == UserRole.SUPER_ADMIN
            _uiState.update { it.copy(isSuperAdmin = isSuper) }

            val items = fetchItems(isSuper)
            if (items == null) {
                _uiState.update { it.copy(isLoading = false, error = LOAD_ERROR) }
            } else {
                _uiState.update { it.copy(isLoading = false, hasAccess = true, items = items) }
            }
        }
    }

    // Colleges: add and delete are super admin only; editing is allowed for both,
    // but only the super admin may change the code.

    fun addCollege(code: String, name: String, shortName: String) {
        if (!_uiState.value.isSuperAdmin) return
        mutate("College added") { collegeRepository.addCollege(code, name.trim(), shortName.trim()) }
    }

    fun updateCollege(college: College, code: String, name: String, shortName: String) {
        val newCode = if (_uiState.value.isSuperAdmin && code != college.code) code else null
        mutate("College updated") {
            collegeRepository.updateCollege(college.id, name.trim(), shortName.trim(), newCode)
        }
    }

    fun deleteCollege(college: College) {
        if (!_uiState.value.isSuperAdmin) return
        mutate("${college.name} removed") { collegeRepository.deleteCollege(college.id) }
    }

    fun addDepartment(college: College, code: String, name: String, shortName: String) {
        mutate("Department added") {
            collegeRepository.addDepartment(college.id, code, name.trim(), shortName.trim())
        }
    }

    fun updateDepartment(department: Department, code: String, name: String, shortName: String) {
        val newCode = if (_uiState.value.isSuperAdmin && code != department.code) code else null
        mutate("Department updated") {
            collegeRepository.updateDepartment(department, name.trim(), shortName.trim(), newCode)
        }
    }

    fun deleteDepartment(department: Department) {
        if (!_uiState.value.isSuperAdmin) return
        mutate("${department.name} removed") { collegeRepository.deleteDepartment(department.id) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun mutate(successMessage: String, block: suspend () -> Result<Unit>) {
        if (_uiState.value.isWorking) return
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true) }
            val result = block()
            val message = result.fold(
                onSuccess = { successMessage },
                onFailure = { e ->
                    when (e) {
                        is InUseException, is InvalidInputException ->
                            e.message ?: "That can't be done right now."
                        else -> "Something went wrong. Please try again."
                    }
                }
            )
            val refreshed = if (result.isSuccess) fetchItems(_uiState.value.isSuperAdmin) else null
            _uiState.update {
                it.copy(isWorking = false, message = message, items = refreshed ?: it.items)
            }
        }
    }

    private suspend fun fetchItems(isSuperAdmin: Boolean): List<CollegeWithDepartments>? {
        val allColleges = collegeRepository.getColleges().getOrNull() ?: return null
        val colleges = if (isSuperAdmin) allColleges else allColleges.filter { it.id == adminCollegeId }
        val departments = if (isSuperAdmin) {
            collegeRepository.getAllDepartments().getOrNull()
        } else {
            collegeRepository.getDepartments(adminCollegeId).getOrNull()
        } ?: return null
        val byCollege = departments.groupBy { it.collegeId }
        return colleges.map { CollegeWithDepartments(it, byCollege[it.id].orEmpty()) }
    }

    private companion object {
        const val LOAD_ERROR = "Couldn't load colleges. Check your connection and try again."
    }
}