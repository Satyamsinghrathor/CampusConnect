package com.example.campusconnect.screens.eventscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.model.ApprovalStatus
import com.example.campusconnect.data.model.CampusEvent
import com.example.campusconnect.data.model.EventAudience
import com.example.campusconnect.data.model.UserProfile
import com.example.campusconnect.data.model.UserRole
import com.example.campusconnect.data.repository.CollegeRepository
import com.example.campusconnect.data.repository.EventRepository
import com.example.campusconnect.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class EventFilter { Upcoming, Past }

/** What the author fills in; the rest of the event is taken from their profile. */
data class EventDraft(
    val title: String,
    val description: String,
    val location: String,
    val mapUrl: String,
    val startAtMillis: Long,
    val wholeCollege: Boolean,
    val audienceRoles: Set<UserRole>
)

data class EventItem(
    val event: CampusEvent,
    /** Empty when the event is for the whole college. */
    val departmentName: String
)

data class EventsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val myUid: String = "",
    val myName: String = "",
    val collegeName: String = "",
    /** HODs, teachers and staff can add events. */
    val canPost: Boolean = false,
    val myDepartmentId: String = "",
    val myDepartmentName: String = "",
    val filter: EventFilter = EventFilter.Upcoming,
    val upcoming: List<EventItem> = emptyList(),
    val past: List<EventItem> = emptyList(),
    val isSaving: Boolean = false,
    val eventSaved: Boolean = false,
    val message: String? = null
)

class EventsViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()
    private val collegeRepository = CollegeRepository()
    private val eventRepository = EventRepository()

    private var me: UserProfile? = null
    private val departmentNames = mutableMapOf<String, String>()

    private val _uiState = MutableStateFlow(EventsUiState())
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

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

            val profile = userRepository.getProfile(uid).getOrElse {
                _uiState.update { it.copy(isLoading = false, error = LOAD_ERROR) }
                return@launch
            }
            if (profile == null || profile.status != ApprovalStatus.ACTIVE) {
                _uiState.update { it.copy(isLoading = false, error = "Your profile isn't ready yet.") }
                return@launch
            }
            me = profile

            val myDepartmentName = if (profile.departmentId.isBlank()) {
                ""
            } else {
                collegeRepository.getDepartment(profile.departmentId).getOrNull()?.name.orEmpty()
            }
            if (profile.departmentId.isNotBlank()) departmentNames[profile.departmentId] = myDepartmentName

            val collegeName = collegeRepository.getCollege(profile.collegeId).getOrNull()?.name.orEmpty()

            _uiState.update {
                it.copy(
                    collegeName = collegeName,
                    myUid = uid,
                    myName = profile.name,
                    canPost = profile.status == ApprovalStatus.ACTIVE && profile.userRole in POSTING_ROLES,
                    myDepartmentId = profile.departmentId,
                    myDepartmentName = myDepartmentName
                )
            }

            if (!refreshEvents()) {
                _uiState.update { it.copy(isLoading = false, error = LOAD_ERROR) }
            }
        }
    }

    /**
     * Re-fetches events without showing the loading spinner. The bottom bar keeps this
     * ViewModel alive between tabs, so screens call this each time they come back into view.
     */
    fun refresh() {
        if (me == null || _uiState.value.isLoading) return
        viewModelScope.launch { refreshEvents() }
    }

    fun setFilter(filter: EventFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun saveEvent(existing: CampusEvent?, draft: EventDraft) {
        val profile = me ?: return
        if (!_uiState.value.canPost || _uiState.value.isSaving) return

        val departmentId = if (draft.wholeCollege || profile.departmentId.isBlank()) "" else profile.departmentId
        val title = draft.title.trim()
        val description = draft.description.trim()
        val location = draft.location.trim()
        val mapUrl = draft.mapUrl.trim()
        // Keep a stable order and only roles that can actually be targeted.
        val audienceRoles = EventAudience.roles.filter { it in draft.audienceRoles }.map { it.value }
        if (audienceRoles.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = if (existing == null) {
                eventRepository.createEvent(
                    CampusEvent(
                        title = title,
                        description = description,
                        location = location,
                        mapUrl = mapUrl,
                        startAtMillis = draft.startAtMillis,
                        collegeId = profile.collegeId,
                        departmentId = departmentId,
                        audienceRoles = audienceRoles,
                        createdBy = profile.uid,
                        createdByName = profile.name,
                        createdByRole = profile.role
                    )
                )
            } else {
                eventRepository.updateEvent(
                    existing.copy(
                        title = title,
                        description = description,
                        location = location,
                        mapUrl = mapUrl,
                        startAtMillis = draft.startAtMillis,
                        departmentId = departmentId,
                        audienceRoles = audienceRoles
                    )
                )
            }

            if (result.isSuccess) {
                refreshEvents()
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        eventSaved = true,
                        message = if (existing == null) "Event added" else "Event updated"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isSaving = false, message = "Couldn't save the event. Please try again.")
                }
            }
        }
    }

    fun deleteEvent(event: CampusEvent) {
        if (!_uiState.value.canPost) return
        viewModelScope.launch {
            val result = eventRepository.deleteEvent(event.id)
            if (result.isSuccess) {
                refreshEvents()
                _uiState.update { it.copy(message = "Event deleted") }
            } else {
                _uiState.update { it.copy(message = "Couldn't delete the event. Please try again.") }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun consumeSaved() {
        _uiState.update { it.copy(eventSaved = false) }
    }

    /** Fetches the college's events and splits them into upcoming and past. False on failure. */
    private suspend fun refreshEvents(): Boolean {
        val profile = me ?: return false
        val events = eventRepository.getEvents(profile.collegeId).getOrElse { return false }
        val visible = events.filter { canSee(profile, it) }

        visible.map { it.departmentId }
            .filter { it.isNotBlank() && it !in departmentNames }
            .distinct()
            .forEach { id ->
                departmentNames[id] = collegeRepository.getDepartment(id).getOrNull()?.name.orEmpty()
            }

        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val items = visible.map { EventItem(it, departmentNames[it.departmentId].orEmpty()) }
        _uiState.update {
            it.copy(
                isLoading = false,
                error = null,
                upcoming = items
                    .filter { item -> item.event.startAtMillis >= startOfToday }
                    .sortedBy { item -> item.event.startAtMillis },
                past = items
                    .filter { item -> item.event.startAtMillis < startOfToday }
                    .sortedByDescending { item -> item.event.startAtMillis }
            )
        }
        return true
    }

    /**
     * Who sees an event: the author, college admins and the super admin always do.
     * Everyone else needs to be in the event's department (if it has one) and in one of its
     * audience roles. This only controls what the app shows; events aren't private records.
     */
    private fun canSee(profile: UserProfile, event: CampusEvent): Boolean {
        if (profile.userRole == UserRole.ADMIN || profile.userRole == UserRole.SUPER_ADMIN) return true
        if (event.createdBy == profile.uid) return true
        if (event.departmentId.isNotBlank() && event.departmentId != profile.departmentId) return false
        return profile.role in event.audienceRoles
    }

    private companion object {
        const val LOAD_ERROR = "Couldn't load events. Check your connection and try again."
        val POSTING_ROLES = setOf(UserRole.HOD, UserRole.TEACHER, UserRole.STAFF)
    }
}
