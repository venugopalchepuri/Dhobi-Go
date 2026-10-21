package com.dhobigo.tracker.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ImageStorageManager(private val context: Context) {

    private val baseChitsDirectory: File
        get() {
            val dir = File(context.filesDir, "chits")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    private val tempDirectory: File
        get() {
            val dir = File(context.cacheDir, "images")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    private fun getUserChitsDirectory(userId: String): File {
        val safeUserId = userId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val dir = File(baseChitsDirectory, safeUserId)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Copies and normalizes an image from a content Uri (such as Photo Picker or Camera)
     * into durable app internal storage scoped to userId. Fixes EXIF rotation so image is right-side up.
     * Returns the relative path "chits/<userId>/<filename>.jpg".
     */
    suspend fun saveImageFromUri(sourceUri: Uri, userId: String): String = withContext(Dispatchers.IO) {
        val userDir = getUserChitsDirectory(userId)
        val fileName = "chit_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
        val destinationFile = File(userDir, fileName)

        // Read orientation from source stream if available
        var orientation = ExifInterface.ORIENTATION_NORMAL
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            try {
                val exif = ExifInterface(input)
                orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } catch (_: Exception) {}
        }

        // Decode and rotate
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            val bitmap = BitmapFactory.decodeStream(input)
                ?: throw IllegalArgumentException("Could not decode image from Uri: $sourceUri")

            val rotatedBitmap = rotateBitmapIfRequired(bitmap, orientation)

            FileOutputStream(destinationFile).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
            }
            rotatedBitmap.recycle()
        } ?: throw IllegalStateException("Cannot open input stream for Uri: $sourceUri")

        val safeUserId = userId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        "chits/$safeUserId/$fileName"
    }

    /**
     * Saves a direct Bitmap into internal storage.
     */
    suspend fun saveBitmap(bitmap: Bitmap, userId: String): String = withContext(Dispatchers.IO) {
        val userDir = getUserChitsDirectory(userId)
        val fileName = "chit_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
        val destinationFile = File(userDir, fileName)

        FileOutputStream(destinationFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        val safeUserId = userId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        "chits/$safeUserId/$fileName"
    }

    /**
     * Resolves a relative path (e.g. "chits/user123/abc.jpg" or legacy "chits/abc.jpg") to an existing local File.
     */
    fun getFile(relativePath: String?): File? {
        if (relativePath.isNullOrBlank()) return null
        val normalized = relativePath.removePrefix("/").removePrefix("chits/")
        // Check if inside user dir or top-level chits dir
        val fileInBase = File(baseChitsDirectory, normalized)
        if (fileInBase.exists()) return fileInBase

        // Direct file
        val directFile = File(context.filesDir, relativePath)
        return if (directFile.exists()) directFile else null
    }

    /**
     * Gets a content Uri for sharing or viewing via FileProvider.
     */
    fun getShareableUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Generates a temporary URI for the Camera capture intent to write into.
     */
    fun createTempCameraUri(): Uri {
        val tempFile = File.createTempFile("camera_", ".jpg", tempDirectory)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    /**
     * Safely deletes a stored chit image.
     */
    suspend fun deleteImage(relativePath: String?): Boolean = withContext(Dispatchers.IO) {
        val file = getFile(relativePath)
        file?.delete() ?: false
    }

    /**
     * Deletes all photos owned by a specific user.
     */
    suspend fun deleteAllImagesForUser(userId: String): Boolean = withContext(Dispatchers.IO) {
        val userDir = getUserChitsDirectory(userId)
        userDir.deleteRecursively()
    }

    private fun rotateBitmapIfRequired(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
