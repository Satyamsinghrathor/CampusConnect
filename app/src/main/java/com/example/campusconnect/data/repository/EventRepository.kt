package com.example.campusconnect.data.repository

import com.example.campusconnect.data.model.CampusEvent
import com.example.campusconnect.data.model.EventAudience
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

/** Reads and writes `events/{eventId}` in Firestore. */
class EventRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val events get() = db.collection(COLLECTION)

    /** All events of one college, soonest first. Department filtering is done by the caller. */
    suspend fun getEvents(collegeId: String): Result<List<CampusEvent>> = attempt {
        events.whereEqualTo("collegeId", collegeId).get().await()
            .documents
            .map { it.toEvent() }
            .sortedBy { it.startAtMillis }
    }

    suspend fun createEvent(event: CampusEvent): Result<Unit> = attempt {
        val data = hashMapOf<String, Any>(
            "title" to event.title,
            "description" to event.description,
            "location" to event.location,
            "mapUrl" to event.mapUrl,
            "startAt" to Timestamp(Date(event.startAtMillis)),
            "collegeId" to event.collegeId,
            "departmentId" to event.departmentId,
            "audienceRoles" to event.audienceRoles,
            "createdBy" to event.createdBy,
            "createdByName" to event.createdByName,
            "createdByRole" to event.createdByRole,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        events.add(data).await()
    }

    /** Only the details an author may change; owner, college and creation time stay put. */
    suspend fun updateEvent(event: CampusEvent): Result<Unit> = attempt {
        val changes = hashMapOf<String, Any>(
            "title" to event.title,
            "description" to event.description,
            "location" to event.location,
            "mapUrl" to event.mapUrl,
            "startAt" to Timestamp(Date(event.startAtMillis)),
            "departmentId" to event.departmentId,
            "audienceRoles" to event.audienceRoles,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        events.document(event.id).update(changes).await()
    }

    suspend fun deleteEvent(id: String): Result<Unit> = attempt {
        events.document(id).delete().await()
    }

    private fun DocumentSnapshot.toEvent() = CampusEvent(
        id = id,
        title = getString("title").orEmpty(),
        description = getString("description").orEmpty(),
        location = getString("location").orEmpty(),
        mapUrl = getString("mapUrl").orEmpty(),
        startAtMillis = getTimestamp("startAt")?.toDate()?.time ?: 0L,
        collegeId = getString("collegeId").orEmpty(),
        departmentId = getString("departmentId").orEmpty(),
        // Events created before audiences existed have none stored and are for everyone.
        audienceRoles = (get("audienceRoles") as? List<*>)
            ?.filterIsInstance<String>()
            ?.takeIf { it.isNotEmpty() }
            ?: EventAudience.allRoleValues,
        createdBy = getString("createdBy").orEmpty(),
        createdByName = getString("createdByName").orEmpty(),
        createdByRole = getString("createdByRole").orEmpty()
    )

    private companion object {
        const val COLLECTION = "events"
    }
}
