package com.example.campusconnect.screens.collegescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.model.ApprovalStatus
import com.example.campusconnect.data.model.College
import com.example.campusconnect.data.model.Department
import com.example.campusconnect.data.model.UserRole
import com.example.campusconnect.data.repository.CollegeRepository
import com.example.campusconnect.data.repository.InUseException
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
    val items: List<CollegeWithDepartments> = emptyList(),
    val isWorking: Boolean = false,
    val message: String? = null
)

/** Lets the super admin add and remove colleges and their departments. */
class ManageCollegesViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()
    private val collegeRepository = CollegeRepository()

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
            if (me == null || me.status != ApprovalStatus.ACTIVE || me.userRole != UserRole.SUPER_ADMIN) {
                _uiState.update { it.copy(isLoading = false, hasAccess = false) }
                return@launch
            }

            val items = fetchItems()
            if (items == null) {
                _uiState.update { it.copy(isLoading = false, error = LOAD_ERROR) }
            } else {
                _uiState.update { it.copy(isLoading = false, hasAccess = true, items = items) }
            }
        }
    }

    fun addCollege(name: String, shortName: String) =
        mutate("College added") { collegeRepository.addCollege(name.trim(), shortName.trim()) }

    fun addDepartment(college: College, name: String) =
        mutate("Department added") { collegeRepository.addDepartment(college.id, name.trim()) }

    fun deleteCollege(college: College) =
        mutate("${college.name} removed") { collegeRepository.deleteCollege(college.id) }

    fun deleteDepartment(department: Department) =
        mutate("${department.name} removed") { collegeRepository.deleteDepartment(department.id) }

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
                    if (e is InUseException) {
                        e.message ?: "That can't be removed right now."
                    } else {
                        "Something went wrong. Please try again."
                    }
                }
            )
            val refreshed = if (result.isSuccess) fetchItems() else null
            _uiState.update {
                it.copy(
                    isWorking = false,
                    message = message,
                    items = refreshed ?: it.items
                )
            }
        }
    }

    private suspend fun fetchItems(): List<CollegeWithDepartments>? {
        val colleges = collegeRepository.getColleges().getOrNull() ?: return null
        val departments = collegeRepository.getAllDepartments().getOrNull() ?: return null
        val byCollege = departments.groupBy { it.collegeId }
        return colleges.map { CollegeWithDepartments(it, byCollege[it.id].orEmpty()) }
    }

    private companion object {
        const val LOAD_ERROR = "Couldn't load colleges. Check your connection and try again."
    }
}
