package com.example.campusconnect.data.model

/** A college, stored in Firestore at `colleges/{collegeId}`. */
data class College(
    val id: String = "",
    val name: String = "",
    val shortName: String = ""
)

/** A department, stored in Firestore at `departments/{departmentId}` and linked by [collegeId]. */
data class Department(
    val id: String = "",
    val collegeId: String = "",
    val name: String = ""
)
