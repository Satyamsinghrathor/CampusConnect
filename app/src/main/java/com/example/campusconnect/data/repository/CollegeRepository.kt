package com.example.campusconnect.data.repository

import com.example.campusconnect.data.model.College
import com.example.campusconnect.data.model.Department
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Reads `colleges/{collegeId}` and `departments/{departmentId}` (each department
 * has a `collegeId` field pointing at its college).
 *
 * When [seedSampleData] is true (debug builds only) and the colleges collection is
 * empty, a small sample set is written so the dropdowns have something to show.
 * Real data should be managed in the Firebase console.
 */
class CollegeRepository(
    private val seedSampleData: Boolean = false,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getColleges(): Result<List<College>> = attempt {
        var snapshot = db.collection(COLLEGES).get().await()
        if (snapshot.isEmpty && seedSampleData) {
            // Seeding can fail (e.g. locked-down rules); that just leaves the list empty.
            attempt { seed() }
            snapshot = db.collection(COLLEGES).get().await()
        }
        snapshot.documents.map { it.toCollege() }.sortedBy { it.name.lowercase() }
    }

    suspend fun getDepartments(collegeId: String): Result<List<Department>> = attempt {
        db.collection(DEPARTMENTS)
            .whereEqualTo("collegeId", collegeId)
            .get()
            .await()
            .documents
            .map { it.toDepartment() }
            .sortedBy { it.name.lowercase() }
    }

    suspend fun getCollege(id: String): Result<College?> = attempt {
        if (id.isBlank()) return@attempt null
        val snapshot = db.collection(COLLEGES).document(id).get().await()
        if (snapshot.exists()) snapshot.toCollege() else null
    }

    suspend fun getDepartment(id: String): Result<Department?> = attempt {
        if (id.isBlank()) return@attempt null
        val snapshot = db.collection(DEPARTMENTS).document(id).get().await()
        if (snapshot.exists()) snapshot.toDepartment() else null
    }

    suspend fun getAllDepartments(): Result<List<Department>> = attempt {
        db.collection(DEPARTMENTS).get().await()
            .documents
            .map { it.toDepartment() }
            .sortedBy { it.name.lowercase() }
    }

    // --- Super admin management. Firestore rules only allow these for the super admin. ---

    suspend fun addCollege(name: String, shortName: String): Result<Unit> = attempt {
        val id = uniqueId(COLLEGES, slugify(shortName.ifBlank { name }, "college"))
        db.collection(COLLEGES).document(id)
            .set(mapOf("name" to name, "shortName" to shortName))
            .await()
    }

    suspend fun addDepartment(collegeId: String, name: String): Result<Unit> = attempt {
        val id = uniqueId(DEPARTMENTS, "${collegeId}_${slugify(name, "dept")}")
        db.collection(DEPARTMENTS).document(id)
            .set(mapOf("collegeId" to collegeId, "name" to name))
            .await()
    }

    /** Refuses with [InUseException] while any user is registered under the department. */
    suspend fun deleteDepartment(id: String): Result<Unit> = attempt {
        if (isUsedByUsers("departmentId", id)) {
            throw InUseException("People are registered under this department, so it can't be removed.")
        }
        db.collection(DEPARTMENTS).document(id).delete().await()
    }

    /**
     * Removes a college and its departments. Refuses with [InUseException] while any user
     * is registered under it, so no profile is left pointing at a college that's gone.
     */
    suspend fun deleteCollege(id: String): Result<Unit> = attempt {
        if (isUsedByUsers("collegeId", id)) {
            throw InUseException("People are registered under this college, so it can't be removed.")
        }
        val departments = db.collection(DEPARTMENTS).whereEqualTo("collegeId", id).get().await()
        val batch = db.batch()
        departments.documents.forEach { batch.delete(it.reference) }
        batch.delete(db.collection(COLLEGES).document(id))
        batch.commit().await()
    }

    private suspend fun isUsedByUsers(field: String, id: String): Boolean =
        !db.collection("users").whereEqualTo(field, id).limit(1).get().await().isEmpty

    private suspend fun uniqueId(collection: String, base: String): String {
        var candidate = base
        var suffix = 2
        while (db.collection(collection).document(candidate).get().await().exists()) {
            candidate = "${base}_$suffix"
            suffix++
        }
        return candidate
    }

    private fun slugify(text: String, fallback: String): String {
        val slug = text.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        return slug.ifEmpty { fallback }.take(40)
    }

    private suspend fun seed() {
        val batch = db.batch()
        batch.set(
            db.collection(COLLEGES).document(SAMPLE_COLLEGE_ID),
            mapOf(
                "name" to "Puducherry Technological University",
                "shortName" to "PTU"
            )
        )
        SAMPLE_DEPARTMENTS.forEach { (slug, name) ->
            batch.set(
                db.collection(DEPARTMENTS).document("${SAMPLE_COLLEGE_ID}_$slug"),
                mapOf(
                    "collegeId" to SAMPLE_COLLEGE_ID,
                    "name" to name
                )
            )
        }
        batch.commit().await()
    }

    private fun DocumentSnapshot.toCollege() = College(
        id = id,
        name = getString("name").orEmpty(),
        shortName = getString("shortName").orEmpty()
    )

    private fun DocumentSnapshot.toDepartment() = Department(
        id = id,
        collegeId = getString("collegeId").orEmpty(),
        name = getString("name").orEmpty()
    )

    private companion object {
        const val COLLEGES = "colleges"
        const val DEPARTMENTS = "departments"
        const val SAMPLE_COLLEGE_ID = "ptu"
        val SAMPLE_DEPARTMENTS = listOf(
            "cse" to "Computer Science and Engineering",
            "it" to "Information Technology",
            "ece" to "Electronics and Communication Engineering",
            "eee" to "Electrical and Electronics Engineering",
            "mech" to "Mechanical Engineering",
            "civil" to "Civil Engineering"
        )
    }
}
