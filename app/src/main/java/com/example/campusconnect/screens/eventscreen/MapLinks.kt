package com.example.campusconnect.screens.eventscreen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.campusconnect.data.model.CampusEvent

/**
 * Opens an event's location in Google Maps. No API key is needed: these are ordinary
 * Maps links, which open in the Maps app when it's installed and in the browser otherwise.
 */
object MapLinks {

    // Only Google Maps links are accepted, so an event can't point people at some other site.
    private val allowedPrefixes = listOf(
        "https://maps.app.goo.gl/",
        "https://www.google.com/maps",
        "https://google.com/maps",
        "https://maps.google.com/",
        "https://goo.gl/maps/"
    )

    const val MAX_URL_LENGTH = 300

    /** An empty link is fine (it's optional); anything else must be a Google Maps link. */
    fun isValidMapUrl(url: String): Boolean =
        url.isEmpty() ||
            (url.length <= MAX_URL_LENGTH && allowedPrefixes.any { url.startsWith(it) })

    fun hasMap(event: CampusEvent): Boolean =
        event.location.isNotBlank() || event.mapUrl.isNotBlank()

    /**
     * Uses the pasted Maps link when there is one (exact spot); otherwise searches for the
     * location text together with the college name so short names like "Seminar Hall" resolve nearby.
     * Returns false when there's nothing to open or no app can open it.
     */
    fun open(context: Context, event: CampusEvent, collegeName: String): Boolean {
        val url = when {
            event.mapUrl.isNotBlank() && isValidMapUrl(event.mapUrl) -> event.mapUrl
            event.location.isNotBlank() -> searchUrl(
                listOf(event.location, collegeName).filter { it.isNotBlank() }.joinToString(", ")
            )
            else -> return false
        }
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    private fun searchUrl(query: String): String =
        "https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}"
}
