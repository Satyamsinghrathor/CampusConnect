package com.example.campusconnect.screens.profilescreen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.example.campusconnect.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Stores the profile on the device, separately for each signed-in account.
 * Text fields go in SharedPreferences and the picture in the app's private files dir,
 * so no extra dependencies are needed. Call from a background thread.
 */
class ProfileRepository(context: Context) {

    private val appContext = context.applicationContext
    private val auth = FirebaseAuth.getInstance()

    private val uid: String get() = auth.currentUser?.uid ?: "guest"

    private fun prefs() = appContext.getSharedPreferences(
        "campusconnect_profile_$uid",
        Context.MODE_PRIVATE
    )

    private fun photoFile() = File(appContext.filesDir, "profile_$uid.jpg")

    fun load(): UserProfile {
        val user = auth.currentUser
        val email = user?.email.orEmpty()
        val p = prefs()
        val defaultName = user?.displayName?.takeIf { it.isNotBlank() }
            ?: email.substringBefore('@')
        return UserProfile(
            name = p.getString(KEY_NAME, null) ?: defaultName,
            email = email,
            studentId = p.getString(KEY_STUDENT_ID, "").orEmpty(),
            department = p.getString(KEY_DEPARTMENT, "").orEmpty(),
            year = p.getInt(KEY_YEAR, 0),
            semester = p.getInt(KEY_SEMESTER, 0),
            phone = p.getString(KEY_PHONE, "").orEmpty()
        )
    }

    fun loadPhoto(): Bitmap? {
        val file = photoFile()
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    /** Throws [IOException] if anything could not be written. */
    fun save(profile: UserProfile, photo: Bitmap?) {
        if (photo != null) {
            FileOutputStream(photoFile()).use { out ->
                if (!photo.compress(Bitmap.CompressFormat.JPEG, 90, out)) {
                    throw IOException("Could not write profile picture")
                }
            }
        }
        val saved = prefs().edit()
            .putString(KEY_NAME, profile.name)
            .putString(KEY_STUDENT_ID, profile.studentId)
            .putString(KEY_DEPARTMENT, profile.department)
            .putInt(KEY_YEAR, profile.year)
            .putInt(KEY_SEMESTER, profile.semester)
            .putString(KEY_PHONE, profile.phone)
            .commit()
        if (!saved) throw IOException("Could not save profile")
    }

    /**
     * Decodes a picked image, fixes its rotation, center-crops it to a square
     * and downsizes it so the stored picture stays small.
     */
    fun decodePickedImage(uri: Uri, targetSize: Int = 512): Bitmap? {
        val resolver = appContext.contentResolver
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= targetSize &&
                bounds.outHeight / (sample * 2) >= targetSize
            ) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            val rotation = try {
                resolver.openInputStream(uri)?.use { stream ->
                    when (
                        ExifInterface(stream).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                    ) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                } ?: 0f
            } catch (e: Exception) {
                0f
            }

            val upright = if (rotation != 0f) {
                val matrix = Matrix().apply { postRotate(rotation) }
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            } else {
                decoded
            }

            val side = minOf(upright.width, upright.height)
            val square = Bitmap.createBitmap(
                upright,
                (upright.width - side) / 2,
                (upright.height - side) / 2,
                side,
                side
            )
            if (side > targetSize) {
                Bitmap.createScaledBitmap(square, targetSize, targetSize, true)
            } else {
                square
            }
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val KEY_NAME = "name"
        const val KEY_STUDENT_ID = "student_id"
        const val KEY_DEPARTMENT = "department"
        const val KEY_YEAR = "year"
        const val KEY_SEMESTER = "semester"
        const val KEY_PHONE = "phone"
    }
}
