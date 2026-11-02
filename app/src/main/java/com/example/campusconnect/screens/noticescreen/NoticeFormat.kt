package com.example.campusconnect.screens.noticescreen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.campusconnect.data.model.EventAudience
import com.example.campusconnect.data.model.Notice
import com.example.campusconnect.screens.eventscreen.formatDate
import java.util.Calendar

const val MAX_LINK_LENGTH = 300

/** The link is optional; if there is one it must be a plain https URL. */
fun isValidNoticeLink(url: String): Boolean =
    url.isEmpty() ||
        (url.length in 12..MAX_LINK_LENGTH && url.startsWith("https://") && url.none { it.isWhitespace() })

fun openLink(context: Context, url: String): Boolean =
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }

/** The last moment of the day [millis] falls on, in the device's time zone. */
fun endOfDay(millis: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/** "5 min ago", "3 hr ago", "2 days ago", or a date once it's older than a week. */
fun relativeTime(millis: Long): String {
    if (millis <= 0L) return "Just now"
    val minutes = (System.currentTimeMillis() - millis) / 60_000
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 24 * 60 -> "${minutes / 60} hr ago"
        minutes < 7 * 24 * 60 -> {
            val days = minutes / (24 * 60)
            if (days == 1L) "1 day ago" else "$days days ago"
        }
        else -> formatDate(millis)
    }
}

/** e.g. "Students · Computer Science · Year 3 · Section A", or "Everyone · Whole college". */
fun noticeAudienceSummary(notice: Notice, departmentName: String): String {
    val roles = EventAudience.roles.filter { it.value in notice.audienceRoles }
    val who = if (roles.size == EventAudience.roles.size) {
        "Everyone"
    } else {
        roles.joinToString(", ") { EventAudience.pluralLabel(it) }
    }
    val parts = mutableListOf(who, departmentName.ifBlank { "Whole college" })
    if ("student" in notice.audienceRoles) {
        if (notice.targetYear != 0) parts += "Year ${notice.targetYear}"
        if (notice.targetSemester != 0) parts += "Semester ${notice.targetSemester}"
        if (notice.targetSection.isNotBlank()) parts += "Section ${notice.targetSection}"
    }
    return parts.joinToString(" · ")
}
