package com.example.campusconnect.data.model

/**
 * A student's profile, stored in Firestore at `users/{uid}`.
 * [year] and [semester] use 0 to mean "not set".
 */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val role: String = "student",
    val collegeId: String = "",
    val departmentId: String = "",
    val rollNumber: String = "",
    val batch: String = "",
    val year: Int = 0,
    val semester: Int = 0,
    val section: String = "",
    val phone: String = ""
)

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
