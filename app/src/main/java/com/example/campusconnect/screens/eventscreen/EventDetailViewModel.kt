
package com.example.campusconnect.screens.eventscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.model.CampusEvent
import com.example.campusconnect.data.repository.CollegeRepository
import com.example.campusconnect.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    /** The event was deleted (or never existed). */
    val notFound: Boolean = false,
    val event: CampusEvent? = null,
    val departmentName: String = "",
    val collegeName: String = ""
)

class EventDetailViewModel : ViewModel() {

    private val eventRepository = EventRepository()
    private val collegeRepository = CollegeRepository()

    private var loadedId: String? = null

    private val _uiState = MutableStateFlow(EventDetailUiState())
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    fun load(eventId: String, force: Boolean = false) {
        if (!force && loadedId == eventId) return
        loadedId = eventId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, notFound = false) }

            val event = eventRepository.getEvent(eventId).getOrElse {
                loadedId = null
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Couldn't load this event. Check your connection and try again."
                    )
                }
                return@launch
            }
            if (event == null) {
                _uiState.update { it.copy(isLoading = false, notFound = true) }
                return@launch
            }

            val departmentName = if (event.departmentId.isBlank()) {
                ""
            } else {
                collegeRepository.getDepartment(event.departmentId).getOrNull()?.name.orEmpty()
            }
            val collegeName = collegeRepository.getCollege(event.collegeId).getOrNull()?.name.orEmpty()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    event = event,
                    departmentName = departmentName,
                    collegeName = collegeName
                )
            }
        }
    }
}
