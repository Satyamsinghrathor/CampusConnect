package com.example.campusconnect.screens.profilesetupscreen

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.model.College
import com.example.campusconnect.data.model.Department
import com.example.campusconnect.data.model.ProfileOptions
import com.example.campusconnect.data.model.UserProfile
import com.example.campusconnect.data.repository.CollegeRepository
import com.example.campusconnect.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SetupField { Name, College, Department, RollNumber, Batch, Year, Semester, Section }

data class ProfileSetupUiState(
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    /** True for Google sign-in, where the name already comes from the Google account. */
    val isNameLocked: Boolean = false,
    val colleges: List<College> = emptyList(),
    val departments: List<Department> = emptyList(),
    val isLoadingColleges: Boolean = true,
    val isLoadingDepartments: Boolean = false,
    val collegeId: String = "",
    val departmentId: String = "",
    val rollNumber: String = "",
    val batch: String = "",
    val year: Int = 0,
    val semester: Int = 0,
    val section: String = "",
    val errors: Map<SetupField, String> = emptyMap(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val loadError: String? = null,
    val saveError: String? = null
)

class ProfileSetupViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()
    private val collegeRepository = CollegeRepository(
        seedSampleData = (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    )

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    init {
        loadColleges()
    }

    private fun initialState(): ProfileSetupUiState {
        val user = auth.currentUser
        val displayName = user?.displayName.orEmpty()
        val isGoogle = user?.providerData?.any {
            it.providerId == GoogleAuthProvider.PROVIDER_ID
        } == true
        return ProfileSetupUiState(
            name = displayName,
            email = user?.email.orEmpty().lowercase(),
            photoUrl = user?.photoUrl?.toString().orEmpty(),
            isNameLocked = isGoogle && displayName.isNotBlank()
        )
    }

    fun loadColleges() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingColleges = true, loadError = null) }
            collegeRepository.getColleges()
                .onSuccess { list ->
                    _uiState.update { it.copy(colleges = list, isLoadingColleges = false) }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoadingColleges = false,
                            loadError = "Couldn't load colleges. Check your connection and try again."
                        )
                    }
                }
        }
    }

    fun onNameChange(value: String) {
        if (_uiState.value.isNameLocked) return
        _uiState.update { it.copy(name = value.take(60), errors = it.errors - SetupField.Name) }
    }

    fun onCollegeSelected(college: College) {
        _uiState.update {
            it.copy(
                collegeId = college.id,
                departmentId = "",
                departments = emptyList(),
                isLoadingDepartments = true,
                loadError = null,
                errors = it.errors - SetupField.College - SetupField.Department
            )
        }
        viewModelScope.launch {
            collegeRepository.getDepartments(college.id)
                .onSuccess { list ->
                    _uiState.update {
                        if (it.collegeId == college.id) {
                            it.copy(departments = list, isLoadingDepartments = false)
                        } else {
                            it
                        }
                    }
                }
                .onFailure {
                    _uiState.update {
                        if (it.collegeId == college.id) {
                            it.copy(
                                isLoadingDepartments = false,
                                loadError = "Couldn't load departments. Pick the college again to retry."
                            )
                        } else {
                            it
                        }
                    }
                }
        }
    }

    fun onDepartmentSelected(department: Department) {
        _uiState.update {
            it.copy(departmentId = department.id, errors = it.errors - SetupField.Department)
        }
    }

    fun onRollNumberChange(value: String) {
        val cleaned = value.uppercase().filter { it.isLetterOrDigit() || it == '-' || it == '/' }.take(20)
        _uiState.update { it.copy(rollNumber = cleaned, errors = it.errors - SetupField.RollNumber) }
    }

    fun onBatchChange(value: String) {
        val cleaned = value.filter { it.isDigit() || it == '-' }.take(9)
        _uiState.update { it.copy(batch = cleaned, errors = it.errors - SetupField.Batch) }
    }

    fun onYearSelected(year: Int) {
        _uiState.update {
            val semester = if (it.semester in ProfileOptions.semestersFor(year)) it.semester else 0
            it.copy(
                year = year,
                semester = semester,
                errors = it.errors - SetupField.Year - SetupField.Semester
            )
        }
    }

    fun onSemesterSelected(semester: Int) {
        _uiState.update { it.copy(semester = semester, errors = it.errors - SetupField.Semester) }
    }

    fun onSectionSelected(section: String) {
        _uiState.update { it.copy(section = section, errors = it.errors - SetupField.Section) }
    }

    fun saveProfile() {
        val state = _uiState.value
        if (state.isSaving) return

        val user = auth.currentUser
        if (user == null) {
            _uiState.update { it.copy(saveError = "Your session has expired. Please log in again.") }
            return
        }

        val name = state.name.trim()
        val errors = buildMap<SetupField, String> {
            if (name.isEmpty()) put(SetupField.Name, "Name is required")
            if (state.collegeId.isEmpty()) put(SetupField.College, "Select your college")
            if (state.departmentId.isEmpty()) put(SetupField.Department, "Select your department")
            if (state.rollNumber.length < 3) put(SetupField.RollNumber, "Enter your roll number")
            if (!ProfileOptions.isValidBatch(state.batch)) {
                put(SetupField.Batch, "Use the format 2023-2027")
            }
            if (state.year == 0) put(SetupField.Year, "Select your year")
            if (state.semester == 0) put(SetupField.Semester, "Select your semester")
            if (state.section.isEmpty()) put(SetupField.Section, "Select your section")
        }
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors, saveError = null) }
            return
        }

        val profile = UserProfile(
            uid = user.uid,
            name = name,
            email = user.email.orEmpty().lowercase(),
            photoUrl = user.photoUrl?.toString().orEmpty(),
            collegeId = state.collegeId,
            departmentId = state.departmentId,
            rollNumber = state.rollNumber,
            batch = state.batch,
            year = state.year,
            semester = state.semester,
            section = state.section
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null, errors = emptyMap()) }
            userRepository.createProfile(profile)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveError = "Couldn't save your profile. Check your connection and try again."
                        )
                    }
                }
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
