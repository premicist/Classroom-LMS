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
    private const val MAX_PHOTO_BYTES = 10 * 1024 * 1024L // 10MB limit

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
                        val buffer = ByteArray(8192)
                        var totalBytes = 0L
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            totalBytes += bytesRead
                            if (totalBytes > MAX_PHOTO_BYTES) {
                                destFile.delete()
                                return@withContext null
                            }
                            output.write(buffer, 0, bytesRead)
                        }
                    }
                } ?: return@withContext null

                destFile.canonicalPath
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
                val file = File(path)
                if (file.name.endsWith(".jpg") && file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                // Best-effort cleanup; ignore failures.
            }
        }
    }
}
