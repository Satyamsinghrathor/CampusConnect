package com.example.campusconnect.data.repository

import com.example.campusconnect.data.model.EventAudience
import com.example.campusconnect.data.model.Notice
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Reads and writes `notices/{noticeId}` in Firestore. */
class NoticeRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val notices get() = db.collection(COLLECTION)

    /** All notices of one college. Audience filtering and sorting are done by the caller. */
    suspend fun getNotices(collegeId: String): Result<List<Notice>> = attempt {
        notices.whereEqualTo("collegeId", collegeId).get().await()
            .documents
            .map { it.toNotice() }
    }

    /** Returns null (not a failure) when the notice doesn't exist any more. */
    suspend fun getNotice(id: String): Result<Notice?> = attempt {
        val snapshot = notices.document(id).get().await()
        if (snapshot.exists()) snapshot.toNotice() else null
    }

    suspend fun createNotice(notice: Notice): Result<Unit> = attempt {
        val data = hashMapOf<String, Any>(
            "title" to notice.title,
            "body" to notice.body,
            "category" to notice.category,
            "important" to notice.important,
            "linkUrl" to notice.linkUrl,
            "expiresAtMillis" to notice.expiresAtMillis,
            "collegeId" to notice.collegeId,
            "departmentId" to notice.departmentId,
            "audienceRoles" to notice.audienceRoles,
            "targetYear" to notice.targetYear,
            "targetSemester" to notice.targetSemester,
            "targetSection" to notice.targetSection,
            "createdBy" to notice.createdBy,
            "createdByName" to notice.createdByName,
            "createdByRole" to notice.createdByRole,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        notices.add(data).await()
    }

    /** Only what an author may change; owner, college and creation time stay put. */
    suspend fun updateNotice(notice: Notice): Result<Unit> = attempt {
        val changes = hashMapOf<String, Any>(
            "title" to notice.title,
            "body" to notice.body,
            "category" to notice.category,
            "important" to notice.important,
            "linkUrl" to notice.linkUrl,
            "expiresAtMillis" to notice.expiresAtMillis,
            "departmentId" to notice.departmentId,
            "audienceRoles" to notice.audienceRoles,
            "targetYear" to notice.targetYear,
            "targetSemester" to notice.targetSemester,
            "targetSection" to notice.targetSection,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        notices.document(notice.id).update(changes).await()
    }

    suspend fun deleteNotice(id: String): Result<Unit> = attempt {
        notices.document(id).delete().await()
    }

    private fun DocumentSnapshot.toNotice() = Notice(
        id = id,
        title = getString("title").orEmpty(),
        body = getString("body").orEmpty(),
        category = getString("category") ?: "general",
        important = getBoolean("important") ?: false,
        linkUrl = getString("linkUrl").orEmpty(),
        expiresAtMillis = getLong("expiresAtMillis") ?: 0L,
        collegeId = getString("collegeId").orEmpty(),
        departmentId = getString("departmentId").orEmpty(),
        audienceRoles = (get("audienceRoles") as? List<*>)
            ?.filterIsInstance<String>()
            ?.takeIf { it.isNotEmpty() }
            ?: EventAudience.allRoleValues,
        targetYear = getLong("targetYear")?.toInt() ?: 0,
        targetSemester = getLong("targetSemester")?.toInt() ?: 0,
        targetSection = getString("targetSection").orEmpty(),
        createdBy = getString("createdBy").orEmpty(),
        createdByName = getString("createdByName").orEmpty(),
        createdByRole = getString("createdByRole").orEmpty(),
        createdAtMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L,
        updatedAtMillis = getTimestamp("updatedAt")?.toDate()?.time ?: 0L
    )

    private companion object {
        const val COLLECTION = "notices"
    }
}
