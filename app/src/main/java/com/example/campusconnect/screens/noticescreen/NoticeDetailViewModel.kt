package com.example.campusconnect.screens.noticescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.model.Notice
import com.example.campusconnect.data.repository.CollegeRepository
import com.example.campusconnect.data.repository.NoticeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoticeDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    /** The notice was deleted (or never existed). */
    val notFound: Boolean = false,
    val notice: Notice? = null,
    val departmentName: String = ""
)

class NoticeDetailViewModel : ViewModel() {

    private val noticeRepository = NoticeRepository()
    private val collegeRepository = CollegeRepository()

    private var loadedId: String? = null

    private val _uiState = MutableStateFlow(NoticeDetailUiState())
    val uiState: StateFlow<NoticeDetailUiState> = _uiState.asStateFlow()

    fun load(noticeId: String, force: Boolean = false) {
        if (!force && loadedId == noticeId) return
        loadedId = noticeId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, notFound = false) }

            val notice = noticeRepository.getNotice(noticeId).getOrElse {
                loadedId = null
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Couldn't load this notice. Check your connection and try again."
                    )
                }
                return@launch
            }
            if (notice == null) {
                _uiState.update { it.copy(isLoading = false, notFound = true) }
                return@launch
            }

            val departmentName = if (notice.departmentId.isBlank()) {
                ""
            } else {
                collegeRepository.getDepartment(notice.departmentId).getOrNull()?.name.orEmpty()
            }
            _uiState.update {
                it.copy(isLoading = false, notice = notice, departmentName = departmentName)
            }
        }
    }
}
