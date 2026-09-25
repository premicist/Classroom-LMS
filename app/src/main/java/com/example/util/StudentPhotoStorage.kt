package com.example.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Persists student profile photos into app-private internal storage.
 *
 * Photo Picker / gallery URIs are not guaranteed to remain readable across
 * app restarts or device reboots, so we copy the bytes into our own
 * `filesDir/student_photos/` folder once and store that stable path in Room.
 */
object StudentPhotoStorage {

    private const val FOLDER_NAME = "student_photos"

    /**
     * Copies the image at [sourceUri] into internal storage and returns the
     * resulting file's absolute path, or null if the copy failed.
     */
    suspend fun savePhoto(context: Context, sourceUri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                val folder = File(context.filesDir, FOLDER_NAME).apply { mkdirs() }
                val destFile = File(folder, "${UUID.randomUUID()}.jpg")

                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext null

                destFile.absolutePath
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Deletes a previously saved photo file, if it exists. Safe to call with
     * null or a path that no longer exists.
     */
    suspend fun deletePhoto(path: String?) {
        if (path.isNullOrBlank()) return
        withContext(Dispatchers.IO) {
            try {
                File(path).let { if (it.exists()) it.delete() }
            } catch (e: Exception) {
                // Best-effort cleanup; ignore failures.
            }
        }
    }
}
