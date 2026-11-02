package com.example.campusconnect.screens.noticescreen

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.model.ApprovalStatus
import com.example.campusconnect.data.model.EventAudience
import com.example.campusconnect.data.model.Notice
import com.example.campusconnect.data.model.NoticeCategory
import com.example.campusconnect.data.model.UserProfile
import com.example.campusconnect.data.model.UserRole
import com.example.campusconnect.data.repository.CollegeRepository
import com.example.campusconnect.data.repository.NoticeRepository
import com.example.campusconnect.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NoticeTab { Active, Expired }

/** What the author fills in; the rest of the notice comes from their profile. */
data class NoticeDraft(
    val title: String,
    val body: String,
    val category: NoticeCategory,
    val important: Boolean,
    val linkUrl: String,
    val expiresAtMillis: Long,
    val wholeCollege: Boolean,
    val audienceRoles: Set<UserRole>,
    val targetYear: Int,
    val targetSemester: Int,
    val targetSection: String
)

data class NoticeItem(
    val notice: Notice,
    /** Empty when the notice is for the whole college. */
    val departmentName: String,
    /** Posted by someone else since the user last opened the Notices tab. */
    val isNew: Boolean
)

data class NoticesUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val myUid: String = "",
    /** HODs, teachers, staff, college admins and the super admin can post. */
    val canPost: Boolean = false,
    /** College admins and the super admin can delete any notice. */
    val canModerate: Boolean = false,
    val myDepartmentId: String = "",
    val myDepartmentName: String = "",
    val tab: NoticeTab = NoticeTab.Active,
    val category: NoticeCategory? = null,
    val active: List<NoticeItem> = emptyList(),
    val expired: List<NoticeItem> = emptyList(),
    val isSaving: Boolean = false,
    val noticeSaved: Boolean = false,
    val message: String? = null
)

class NoticesViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()
    private val collegeRepository = CollegeRepository()
    private val noticeRepository = NoticeRepository()
    private val prefs = application.getSharedPreferences("notices", Context.MODE_PRIVATE)

    private var me: UserProfile? = null
    private val departmentNames = mutableMapOf<String, String>()

    /** Notices posted after this moment count as "new". Nothing is new until the profile loads. */
    private var seenBaseline = Long.MAX_VALUE

    private val _uiState = MutableStateFlow(NoticesUiState())
    val uiState: StateFlow<NoticesUiState> = _uiState.asStateFlow()

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
            seenBaseline = readBaseline(uid)

            val myDepartmentName = if (profile.departmentId.isBlank()) {
                ""
            } else {
                collegeRepository.getDepartment(profile.departmentId).getOrNull()?.name.orEmpty()
            }
            if (profile.departmentId.isNotBlank()) departmentNames[profile.departmentId] = myDepartmentName

            val role = profile.userRole
            _uiState.update {
                it.copy(
                    myUid = uid,
                    canPost = role in POSTING_ROLES,
                    canModerate = role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN,
                    myDepartmentId = profile.departmentId,
                    myDepartmentName = myDepartmentName
                )
            }

            if (!refreshNotices()) {
                _uiState.update { it.copy(isLoading = false, error = LOAD_ERROR) }
            }
        }
    }

    /** The Notices tab was opened: pick up the "new" baseline and reload. */
    fun onScreenEntered() {
        val profile = me ?: return
        seenBaseline = readBaseline(profile.uid)
        refresh()
    }

    /** The Notices tab was left: everything posted so far counts as seen next time. */
    fun markSeen() {
        val uid = me?.uid ?: return
        prefs.edit().putLong(seenKey(uid), System.currentTimeMillis()).apply()
    }

    /** Re-fetches without a loading spinner. The bottom bar keeps this ViewModel alive between tabs. */
    fun refresh() {
        if (me == null || _uiState.value.isLoading) return
        viewModelScope.launch { refreshNotices() }
    }

    fun setTab(tab: NoticeTab) {
        _uiState.update { it.copy(tab = tab) }
    }

    fun setCategory(category: NoticeCategory?) {
        _uiState.update { it.copy(category = category) }
    }

    fun saveNotice(existing: Notice?, draft: NoticeDraft) {
        val profile = me ?: return
        if (!_uiState.value.canPost || _uiState.value.isSaving) return

        val audienceRoles = EventAudience.roles.filter { it in draft.audienceRoles }.map { it.value }
        if (audienceRoles.isEmpty()) return
        val studentsTargeted = UserRole.STUDENT in draft.audienceRoles

        val departmentId = if (draft.wholeCollege || profile.departmentId.isBlank()) "" else profile.departmentId
        // Year, semester and section only apply to students.
        val targetYear = if (studentsTargeted) draft.targetYear else 0
        val targetSemester = if (studentsTargeted) draft.targetSemester else 0
        val targetSection = if (studentsTargeted) draft.targetSection else ""

        val base = (existing ?: Notice()).copy(
            title = draft.title.trim(),
            body = draft.body.trim(),
            category = draft.category.value,
            important = draft.important,
            linkUrl = draft.linkUrl.trim(),
            expiresAtMillis = draft.expiresAtMillis,
            departmentId = departmentId,
            audienceRoles = audienceRoles,
            targetYear = targetYear,
            targetSemester = targetSemester,
            targetSection = targetSection
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = if (existing == null) {
                noticeRepository.createNotice(
                    base.copy(
                        collegeId = profile.collegeId,
                        createdBy = profile.uid,
                        createdByName = profile.name,
                        createdByRole = profile.role
                    )
                )
            } else {
                noticeRepository.updateNotice(base)
            }

            if (result.isSuccess) {
                refreshNotices()
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        noticeSaved = true,
                        message = if (existing == null) "Notice posted" else "Notice updated"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isSaving = false, message = "Couldn't save the notice. Please try again.")
                }
            }
        }
    }

    fun deleteNotice(notice: Notice) {
        viewModelScope.launch {
            val result = noticeRepository.deleteNotice(notice.id)
            if (result.isSuccess) {
                refreshNotices()
                _uiState.update { it.copy(message = "Notice deleted") }
            } else {
                _uiState.update { it.copy(message = "Couldn't delete the notice. Please try again.") }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun consumeSaved() {
        _uiState.update { it.copy(noticeSaved = false) }
    }

    private suspend fun refreshNotices(): Boolean {
        val profile = me ?: return false
        val all = noticeRepository.getNotices(profile.collegeId).getOrElse { return false }
        val visible = all.filter { canSee(profile, it) }

        visible.map { it.departmentId }
            .filter { it.isNotBlank() && it !in departmentNames }
            .distinct()
            .forEach { id ->
                departmentNames[id] = collegeRepository.getDepartment(id).getOrNull()?.name.orEmpty()
            }

        val now = System.currentTimeMillis()
        val items = visible.map {
            NoticeItem(
                notice = it,
                departmentName = departmentNames[it.departmentId].orEmpty(),
                isNew = it.createdBy != profile.uid && it.createdAtMillis > seenBaseline
            )
        }
        val (expired, active) = items.partition {
            it.notice.expiresAtMillis > 0L && it.notice.expiresAtMillis < now
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                error = null,
                // Important notices first, then newest first (a just-posted notice has no timestamp yet).
                active = active.sortedWith(
                    compareByDescending<NoticeItem> { item -> item.notice.important }
                        .thenByDescending { item -> sortKey(item.notice) }
                ),
                expired = expired.sortedByDescending { item -> item.notice.expiresAtMillis }
            )
        }
        return true
    }

    private fun sortKey(notice: Notice): Long =
        if (notice.createdAtMillis == 0L) Long.MAX_VALUE else notice.createdAtMillis

    private fun readBaseline(uid: String): Long {
        val saved = prefs.getLong(seenKey(uid), -1L)
        if (saved >= 0L) return saved
        // First time on this device: don't flag the whole backlog as new.
        val now = System.currentTimeMillis()
        prefs.edit().putLong(seenKey(uid), now).apply()
        return now
    }

    private fun seenKey(uid: String) = "last_seen_$uid"

    /**
     * Who sees a notice: the author, college admins and the super admin always do. Everyone else
     * needs to be in the notice's department (if it has one) and audience roles, and students also
     * have to match the year, semester and section it targets. This only controls what the app shows.
     */
    private fun canSee(profile: UserProfile, notice: Notice): Boolean {
        val role = profile.userRole
        if (role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN) return true
        if (notice.createdBy == profile.uid) return true
        if (notice.departmentId.isNotBlank() && notice.departmentId != profile.departmentId) return false
        if (profile.role !in notice.audienceRoles) return false
        if (role == UserRole.STUDENT) {
            if (notice.targetYear != 0 && notice.targetYear != profile.year) return false
            if (notice.targetSemester != 0 && notice.targetSemester != profile.semester) return false
            if (notice.targetSection.isNotBlank() && notice.targetSection != profile.section) return false
        }
        return true
    }

    private companion object {
        const val LOAD_ERROR = "Couldn't load notices. Check your connection and try again."
        val POSTING_ROLES = setOf(
            UserRole.HOD, UserRole.TEACHER, UserRole.STAFF, UserRole.ADMIN, UserRole.SUPER_ADMIN
        )
    }
}
