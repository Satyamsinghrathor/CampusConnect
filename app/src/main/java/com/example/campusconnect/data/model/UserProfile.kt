package com.example.campusconnect.data.model

/**
 * Editable profile details for the signed-in student.
 * [email] comes from the auth account and is never edited in the app.
 * [year] and [semester] use 0 to mean "not set".
 */
data class UserProfile(
    val name: String = "",
    val email: String = "",
    val studentId: String = "",
    val department: String = "",
    val year: Int = 0,
    val semester: Int = 0,
    val phone: String = ""
)

fun UserProfile.yearSemesterLabel(): String = when {
    year > 0 && semester > 0 -> "Year $year · Semester $semester"
    year > 0 -> "Year $year"
    semester > 0 -> "Semester $semester"
    else -> ""
}
