package com.example.campusconnect.data.model

enum class NoticeCategory(val value: String, val label: String) {
    GENERAL("general", "General"),
    EXAM("exam", "Exam"),
    ACADEMIC("academic", "Academic"),
    PLACEMENT("placement", "Placement"),
    HOLIDAY("holiday", "Holiday"),
    URGENT("urgent", "Urgent");

    companion object {
        fun fromValue(value: String): NoticeCategory =
            entries.firstOrNull { it.value == value } ?: GENERAL
    }
}

/**
 * A notice, stored in Firestore at `notices/{noticeId}`.
 *
 * Who sees it: an empty [departmentId] means the whole college, [audienceRoles] lists the
 * roles that should see it, and for students the optional [targetYear], [targetSemester] and
 * [targetSection] narrow it further (0 or empty means "any").
 */
data class Notice(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = NoticeCategory.GENERAL.value,
    /** Important notices are pinned to the top of the list. */
    val important: Boolean = false,
    /** Optional https link, e.g. a form or a document. */
    val linkUrl: String = "",
    /** 0 means the notice never expires. */
    val expiresAtMillis: Long = 0L,
    val collegeId: String = "",
    val departmentId: String = "",
    val audienceRoles: List<String> = EventAudience.allRoleValues,
    val targetYear: Int = 0,
    val targetSemester: Int = 0,
    val targetSection: String = "",
    val createdBy: String = "",
    val createdByName: String = "",
    val createdByRole: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    val isEdited: Boolean get() = createdAtMillis > 0L && updatedAtMillis > createdAtMillis
}
