package com.example.campusconnect.screens.profilescreen

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val profile: UserProfile = UserProfile(),
    val photo: Bitmap? = null,
    val draft: UserProfile = UserProfile(),
    val draftPhoto: Bitmap? = null,
    val nameError: String? = null,
    val phoneError: String? = null,
    val message: String? = null
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository(application)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val (profile, photo) = withContext(Dispatchers.IO) {
                repository.load() to repository.loadPhoto()
            }
            _uiState.update {
                it.copy(isLoading = false, profile = profile, photo = photo)
            }
        }
    }

    fun startEditing() {
        _uiState.update {
            it.copy(
                isEditing = true,
                draft = it.profile,
                draftPhoto = null,
                nameError = null,
                phoneError = null
            )
        }
    }

    fun cancelEditing() {
        _uiState.update {
            it.copy(
                isEditing = false,
                draft = it.profile,
                draftPhoto = null,
                nameError = null,
                phoneError = null
            )
        }
    }

    fun updateDraft(transform: (UserProfile) -> UserProfile) {
        _uiState.update { state ->
            val updated = transform(state.draft)
            state.copy(
                draft = updated,
                nameError = if (updated.name != state.draft.name) null else state.nameError,
                phoneError = if (updated.phone != state.draft.phone) null else state.phoneError
            )
        }
    }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) { repository.decodePickedImage(uri) }
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
            studentId = current.draft.studentId.trim(),
            department = current.draft.department.trim(),
            phone = current.draft.phone.trim()
        )

        val nameError = if (draft.name.isEmpty()) "Name is required" else null
        val phoneError = if (draft.phone.isNotEmpty() && !PHONE_REGEX.matches(draft.phone)) {
            "Enter a valid phone number (10–13 digits)"
        } else {
            null
        }
        if (nameError != null || phoneError != null) {
            _uiState.update { it.copy(nameError = nameError, phoneError = phoneError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val saved = withContext(Dispatchers.IO) {
                runCatching { repository.save(draft, current.draftPhoto) }.isSuccess
            }
            _uiState.update {
                if (saved) {
                    it.copy(
                        isSaving = false,
                        isEditing = false,
                        profile = draft,
                        photo = current.draftPhoto ?: it.photo,
                        draft = draft,
                        draftPhoto = null,
                        message = "Profile saved"
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

    private companion object {
        val PHONE_REGEX = Regex("^\\+?[0-9]{10,13}$")
    }
}
