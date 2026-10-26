package com.example.campusconnect.screens.profilescreen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Handles the profile picture on the device. Profile details live in Firestore
 * (see UserRepository); this class only deals with the picture:
 * a photo the student picked wins, otherwise their Google account photo is used.
 * Call from a background thread.
 */
class ProfileRepository(context: Context) {

    private val appContext = context.applicationContext

    private fun customPhotoFile(uid: String) = File(appContext.filesDir, "profile_$uid.jpg")

    private fun googlePhotoFile(uid: String) = File(appContext.cacheDir, "google_photo_$uid.jpg")

    fun loadCustomPhoto(uid: String): Bitmap? {
        val file = customPhotoFile(uid)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    /** Throws [IOException] if the picture could not be written. */
    fun saveCustomPhoto(uid: String, photo: Bitmap) {
        FileOutputStream(customPhotoFile(uid)).use { out ->
            if (!photo.compress(Bitmap.CompressFormat.JPEG, 90, out)) {
                throw IOException("Could not write profile picture")
            }
        }
    }

    /** Returns the cached Google photo, downloading it once if needed. Null on any failure. */
    fun loadGooglePhoto(uid: String, photoUrl: String): Bitmap? {
        if (photoUrl.isBlank()) return null
        val cache = googlePhotoFile(uid)
        if (cache.exists()) {
            BitmapFactory.decodeFile(cache.absolutePath)?.let { return it }
        }
        return try {
            val connection = URL(photoUrl.replace("=s96-c", "=s256-c")).openConnection() as HttpURLConnection
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            try {
                val bitmap = connection.inputStream.use { BitmapFactory.decodeStream(it) }
                if (bitmap != null) {
                    FileOutputStream(cache).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                }
                bitmap
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            null
        }
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
}
