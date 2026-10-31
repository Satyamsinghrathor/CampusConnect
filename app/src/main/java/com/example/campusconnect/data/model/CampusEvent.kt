package com.example.campusconnect.data.model

/** Who an event can be aimed at. College admins and the super admin always see every event. */
object EventAudience {
    val roles: List<UserRole> =
        listOf(UserRole.STUDENT, UserRole.TEACHER, UserRole.STAFF, UserRole.HOD)

    val allRoleValues: List<String> = roles.map { it.value }

    fun pluralLabel(role: UserRole): String = when (role) {
        UserRole.STUDENT -> "Students"
        UserRole.TEACHER -> "Teachers"
        UserRole.STAFF -> "Staff"
        UserRole.HOD -> "HODs"
        else -> role.label
    }
}

/**
 * A campus event, stored in Firestore at `events/{eventId}`.
 * An empty [departmentId] means the event is for the whole college, and
 * [audienceRoles] lists the roles that should see it (role values like "student").
 */
data class CampusEvent(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    /** Optional Google Maps link that points at the exact spot. */
    val mapUrl: String = "",
    val startAtMillis: Long = 0L,
    val collegeId: String = "",
    val departmentId: String = "",
    val audienceRoles: List<String> = EventAudience.allRoleValues,
    val createdBy: String = "",
    val createdByName: String = "",
    val createdByRole: String = ""
)
