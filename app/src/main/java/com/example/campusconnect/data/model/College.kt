package com.example.campusconnect.data.model

/**
 * A college, stored in Firestore at `colleges/{collegeId}`.
 * The document ID is the stable [id] that profiles point at; [code] is the
 * human-readable code (e.g. "PTU") and only the super admin can change it.
 */
data class College(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val shortName: String = ""
)

/**
 * A department, stored in Firestore at `departments/{departmentId}` and linked by [collegeId].
 * Like colleges, [code] is only changeable by the super admin.
 */
data class Department(
    val id: String = "",
    val collegeId: String = "",
    val code: String = "",
    val name: String = "",
    val shortName: String = ""
)