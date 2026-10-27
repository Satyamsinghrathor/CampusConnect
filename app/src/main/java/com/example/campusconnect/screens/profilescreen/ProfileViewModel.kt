package com.example.campusconnect.screens.profilescreen

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.model.ProfileOptions
import com.example.campusconnect.data.model.UserProfile
import com.example.campusconnect.data.model.UserRole
import com.example.campusconnect.data.repository.CollegeRepository
import com.example.campusconnect.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class EditField { Name, RollNumber, Batch, EmployeeId, Designation, Phone }

data class ProfileUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val profile: UserProfile = UserProfile(),
    val collegeName: String = "",
    val departmentName: String = "",
    val photo: Bitmap? = null,
    val draft: UserProfile = UserProfile(),
    val draftPhoto: Bitmap? = null,
    val errors: Map<EditField, String> = emptyMap(),
    val message: String? = null
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()
    private val collegeRepository = CollegeRepository()
    private val photoRepository = ProfileRepository(application)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(isLoading = false, loadError = "You're not signed in.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }

            val profile = userRepository.getProfile(uid).getOrElse {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadError = "Couldn't load your profile. Check your connection and try again."
                    )
                }
                return@launch
            }
            if (profile == null) {
                _uiState.update {
                    it.copy(isLoading = false, loadError = "Your profile hasn't been set up yet.")
                }
                return@launch
            }

            // Show the details right away; names and the picture fill in afterwards.
            _uiState.update { it.copy(isLoading = false, profile = profile, draft = profile) }

            val collegeName = collegeRepository.getCollege(profile.collegeId).getOrNull()?.name.orEmpty()
            val departmentName = collegeRepository.getDepartment(profile.departmentId).getOrNull()?.name.orEmpty()
            _uiState.update { it.copy(collegeName = collegeName, departmentName = departmentName) }

            val photo = withContext(Dispatchers.IO) {
                photoRepository.loadCustomPhoto(uid)
                    ?: photoRepository.loadGooglePhoto(uid, profile.photoUrl)
            }
            _uiState.update { it.copy(photo = photo) }
        }
    }

    fun startEditing() {
        _uiState.update {
            it.copy(isEditing = true, draft = it.profile, draftPhoto = null, errors = emptyMap())
        }
    }

    fun cancelEditing() {
        _uiState.update {
            it.copy(isEditing = false, draft = it.profile, draftPhoto = null, errors = emptyMap())
        }
    }

    fun updateDraft(transform: (UserProfile) -> UserProfile) {
        _uiState.update { state ->
            var updated = transform(state.draft)
            // Keep semester consistent with the chosen year.
            if (updated.year > 0 && updated.semester !in ProfileOptions.semestersFor(updated.year)) {
                updated = updated.copy(semester = 0)
            }
            var errors = state.errors
            if (updated.name != state.draft.name) errors = errors - EditField.Name
            if (updated.rollNumber != state.draft.rollNumber) errors = errors - EditField.RollNumber
            if (updated.batch != state.draft.batch) errors = errors - EditField.Batch
            if (updated.employeeId != state.draft.employeeId) errors = errors - EditField.EmployeeId
            if (updated.designation != state.draft.designation) errors = errors - EditField.Designation
            if (updated.phone != state.draft.phone) errors = errors - EditField.Phone
            state.copy(draft = updated, errors = errors)
        }
    }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) { photoRepository.decodePickedImage(uri) }
            if (bitmap != null) {
                _uiState.update { it.copy(draftPhoto = bitmap) }
            } else {
                _uiState.update { it.copy(message = "Couldn't use that image. Try another one.") }
            }
        }
    }

    fun saveProfile() {
        val current = _uiState.value
        if (current.isSaving || !current.isEditing) return

        val draft = current.draft.copy(
            name = current.draft.name.trim(),
            rollNumber = current.draft.rollNumber.trim(),
            batch = current.draft.batch.trim(),
            employeeId = current.draft.employeeId.trim(),
            designation = current.draft.designation.trim(),
            phone = current.draft.phone.trim()
        )
        val isStudent = draft.userRole == UserRole.STUDENT

        val errors = buildMap<EditField, String> {
            if (draft.name.isEmpty()) put(EditField.Name, "Name is required")
            if (isStudent) {
                if (draft.rollNumber.length < 3) put(EditField.RollNumber, "Enter your roll number")
                if (!ProfileOptions.isValidBatch(draft.batch)) put(EditField.Batch, "Use the format 2023-2027")
            } else {
                if (draft.employeeId.length < 2) put(EditField.EmployeeId, "Enter your employee ID")
                if (draft.designation.length < 2) put(EditField.Designation, "Enter your designation")
            }
            if (draft.phone.isNotEmpty() && !ProfileOptions.isValidPhone(draft.phone)) {
                put(EditField.Phone, "Enter a valid phone number (10–13 digits)")
            }
        }
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        if (isStudent && (draft.year == 0 || draft.semester == 0 || draft.section.isEmpty())) {
            _uiState.update { it.copy(message = "Choose your year, semester and section.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val photoSaved = current.draftPhoto?.let { photo ->
                withContext(Dispatchers.IO) {
                    runCatching { photoRepository.saveCustomPhoto(draft.uid, photo) }.isSuccess
                }
            } ?: true

            val saved = userRepository.updateProfile(draft).isSuccess

            _uiState.update {
                if (saved) {
                    it.copy(
                        isSaving = false,
                        isEditing = false,
                        profile = draft,
                        draft = draft,
                        photo = if (photoSaved) current.draftPhoto ?: it.photo else it.photo,
                        draftPhoto = null,
                        message = if (photoSaved) "Profile saved" else "Profile saved, but the picture couldn't be stored"
                    )
                } else {
                    it.copy(isSaving = false, message = "Couldn't save profile. Please try again.")
                }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
