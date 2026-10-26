package com.example.campusconnect.data.repository

import com.example.campusconnect.data.model.UserProfile
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

    /** Creates the profile document on first login. Role is always "student". */
    suspend fun createProfile(profile: UserProfile): Result<Unit> = attempt {
        val data = hashMapOf<String, Any>(
            "uid" to profile.uid,
            "name" to profile.name,
            "email" to profile.email,
            "photoUrl" to profile.photoUrl,
            "role" to "student",
            "collegeId" to profile.collegeId,
            "departmentId" to profile.departmentId,
            "rollNumber" to profile.rollNumber,
            "batch" to profile.batch,
            "year" to profile.year,
            "semester" to profile.semester,
            "section" to profile.section,
            "phone" to profile.phone,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        users.document(profile.uid).set(data).await()
    }

    /**
     * Updates the fields a student may change. Email, role, college and
     * department are intentionally left alone.
     */
    suspend fun updateProfile(profile: UserProfile): Result<Unit> = attempt {
        val changes = hashMapOf<String, Any>(
            "name" to profile.name,
            "rollNumber" to profile.rollNumber,
            "batch" to profile.batch,
            "year" to profile.year,
            "semester" to profile.semester,
            "section" to profile.section,
            "phone" to profile.phone,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        users.document(profile.uid).update(changes).await()
    }

    private fun DocumentSnapshot.toProfile() = UserProfile(
        uid = getString("uid") ?: id,
        name = getString("name").orEmpty(),
        email = getString("email").orEmpty(),
        photoUrl = getString("photoUrl").orEmpty(),
        role = getString("role") ?: "student",
        collegeId = getString("collegeId").orEmpty(),
        departmentId = getString("departmentId").orEmpty(),
        rollNumber = getString("rollNumber").orEmpty(),
        batch = getString("batch").orEmpty(),
        year = getLong("year")?.toInt() ?: 0,
        semester = getLong("semester")?.toInt() ?: 0,
        section = getString("section").orEmpty(),
        phone = getString("phone").orEmpty()
    )

    private companion object {
        const val COLLECTION = "users"
    }
}
