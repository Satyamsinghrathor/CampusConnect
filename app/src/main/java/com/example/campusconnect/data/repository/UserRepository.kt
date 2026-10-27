package com.example.campusconnect.data.repository

import com.example.campusconnect.data.model.ApprovalStatus
import com.example.campusconnect.data.model.UserProfile
import com.example.campusconnect.data.model.UserRole
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Reads and writes `users/{uid}` in Firestore. */
class UserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val users get() = db.collection(COLLECTION)

    /** Returns null (not a failure) when the user has no profile document yet. */
    suspend fun getProfile(uid: String): Result<UserProfile?> = attempt {
        val snapshot = users.document(uid).get().await()
        if (snapshot.exists()) snapshot.toProfile() else null
    }

    /**
     * Creates the profile document on first login. Students are active immediately;
     * every other role starts as "pending" until an approver activates it.
     */
    suspend fun createProfile(profile: UserProfile): Result<Unit> = attempt {
        val role = profile.userRole
        require(role in UserRole.selectable) { "Role not allowed at registration" }
        val status = if (role == UserRole.STUDENT) ApprovalStatus.ACTIVE else ApprovalStatus.PENDING

        val data = hashMapOf<String, Any>(
            "uid" to profile.uid,
            "name" to profile.name,
            "email" to profile.email,
            "photoUrl" to profile.photoUrl,
            "role" to role.value,
            "status" to status,
            "collegeId" to profile.collegeId,
            "departmentId" to profile.departmentId,
            "rollNumber" to profile.rollNumber,
            "batch" to profile.batch,
            "year" to profile.year,
            "semester" to profile.semester,
            "section" to profile.section,
            "employeeId" to profile.employeeId,
            "designation" to profile.designation,
            "phone" to profile.phone,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        users.document(profile.uid).set(data).await()
    }

    /**
     * Updates the details a user may change themselves. Email, role, status,
     * college and department are intentionally left alone.
     */
    suspend fun updateProfile(profile: UserProfile): Result<Unit> = attempt {
        val changes = hashMapOf<String, Any>(
            "name" to profile.name,
            "rollNumber" to profile.rollNumber,
            "batch" to profile.batch,
            "year" to profile.year,
            "semester" to profile.semester,
            "section" to profile.section,
            "employeeId" to profile.employeeId,
            "designation" to profile.designation,
            "phone" to profile.phone,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        users.document(profile.uid).update(changes).await()
    }

    /**
     * Pending requests this approver is allowed to decide:
     * a college admin sees teachers and staff of their own college,
     * the super admin sees HOD and admin requests from every college.
     */
    suspend fun getPendingApprovals(approver: UserProfile): Result<List<UserProfile>> =
        attempt<List<UserProfile>> {
            val query = when (approver.userRole) {
                UserRole.SUPER_ADMIN -> users
                    .whereEqualTo("status", ApprovalStatus.PENDING)
                    .whereIn("role", listOf(UserRole.HOD.value, UserRole.ADMIN.value))

                UserRole.ADMIN -> users
                    .whereEqualTo("collegeId", approver.collegeId)
                    .whereEqualTo("status", ApprovalStatus.PENDING)
                    .whereIn("role", listOf(UserRole.TEACHER.value, UserRole.STAFF.value))

                else -> return@attempt emptyList()
            }
            query.get().await().documents
                .map { it.toProfile() }
                .sortedBy { it.name.lowercase() }
        }

    suspend fun setApproval(targetUid: String, approverUid: String, approve: Boolean): Result<Unit> = attempt {
        val changes = hashMapOf<String, Any>(
            "status" to if (approve) ApprovalStatus.ACTIVE else ApprovalStatus.REJECTED,
            "approvedBy" to approverUid,
            "approvedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        users.document(targetUid).update(changes).await()
    }

    private fun DocumentSnapshot.toProfile() = UserProfile(
        uid = getString("uid") ?: id,
        name = getString("name").orEmpty(),
        email = getString("email").orEmpty(),
        photoUrl = getString("photoUrl").orEmpty(),
        role = getString("role") ?: UserRole.STUDENT.value,
        // Profiles created before roles existed have no status and count as active.
        status = getString("status") ?: ApprovalStatus.ACTIVE,
        collegeId = getString("collegeId").orEmpty(),
        departmentId = getString("departmentId").orEmpty(),
        rollNumber = getString("rollNumber").orEmpty(),
        batch = getString("batch").orEmpty(),
        year = getLong("year")?.toInt() ?: 0,
        semester = getLong("semester")?.toInt() ?: 0,
        section = getString("section").orEmpty(),
        employeeId = getString("employeeId").orEmpty(),
        designation = getString("designation").orEmpty(),
        phone = getString("phone").orEmpty()
    )

    private companion object {
        const val COLLECTION = "users"
    }
}
