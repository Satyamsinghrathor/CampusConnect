package com.example.campusconnect.data.model

/** Who a user is in CampusConnect. [value] is what's stored in Firestore. */
enum class UserRole(val value: String, val label: String) {
    STUDENT("student", "Student"),
    TEACHER("teacher", "Teacher"),
    STAFF("staff", "Staff"),
    HOD("hod", "Head of Department"),
    ADMIN("admin", "College Admin"),
    SUPER_ADMIN("superadmin", "Super Admin");

    /** Students, teachers and HODs must pick a department; staff may; college admins have none. */
    val usesDepartment: Boolean get() = this != ADMIN && this != SUPER_ADMIN
    val requiresDepartment: Boolean get() = this == STUDENT || this == TEACHER || this == HOD

    /** Can open the Pending Approvals screen. */
    val canApprove: Boolean get() = this == ADMIN || this == SUPER_ADMIN

    /** Who has to approve this role before the account becomes active. */
    val approverDescription: String
        get() = when (this) {
            TEACHER, STAFF -> "your college admin"
            HOD, ADMIN -> "the CampusConnect administrators"
            else -> ""
        }

    companion object {
        /** Roles a person can ask for when registering. Super Admin is only set in the console. */
        val selectable: List<UserRole> = listOf(STUDENT, TEACHER, STAFF, HOD, ADMIN)

        fun fromValue(value: String): UserRole = entries.firstOrNull { it.value == value } ?: STUDENT
    }
}

object ApprovalStatus {
    const val ACTIVE = "active"
    const val PENDING = "pending"
    const val REJECTED = "rejected"
}

/**
 * A user's profile, stored in Firestore at `users/{uid}`.
 * [year] and [semester] use 0 to mean "not set". Student-only fields are empty for
 * other roles, and [employeeId] / [designation] are empty for students.
 */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val role: String = UserRole.STUDENT.value,
    val status: String = ApprovalStatus.ACTIVE,
    val collegeId: String = "",
    val departmentId: String = "",
    val rollNumber: String = "",
    val batch: String = "",
    val year: Int = 0,
    val semester: Int = 0,
    val section: String = "",
    val employeeId: String = "",
    val designation: String = "",
    val phone: String = ""
) {
    val userRole: UserRole get() = UserRole.fromValue(role)
}

/** Choices and validation shared by the profile setup and profile screens. */
object ProfileOptions {
    val years: List<Int> = listOf(1, 2, 3, 4)
    val sections: List<String> = listOf("A", "B", "C", "D", "E", "F")

    private val batchRegex = Regex("^(\\d{4})-(\\d{4})$")
    private val phoneRegex = Regex("^\\+?[0-9]{10,13}$")

    /** Year N covers semesters 2N-1 and 2N; with no year chosen, all eight are allowed. */
    fun semestersFor(year: Int): List<Int> =
        if (year in years) listOf(year * 2 - 1, year * 2) else (1..8).toList()

    fun isValidBatch(batch: String): Boolean {
        val match = batchRegex.matchEntire(batch) ?: return false
        val start = match.groupValues[1].toInt()
        val end = match.groupValues[2].toInt()
        return end > start
    }

    fun isValidPhone(phone: String): Boolean = phoneRegex.matches(phone)
}

fun UserProfile.yearSemesterLabel(): String = when {
    year > 0 && semester > 0 -> "Year $year · Semester $semester"
    year > 0 -> "Year $year"
    semester > 0 -> "Semester $semester"
    else -> ""
}
