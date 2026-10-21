package com.dhobigo.tracker.data.backup

import android.content.Context
import com.dhobigo.tracker.data.local.dao.ClothingCategoryDao
import com.dhobigo.tracker.data.local.dao.LaundryItemDao
import com.dhobigo.tracker.data.local.dao.LaundryRecordDao
import com.dhobigo.tracker.data.local.entity.ClothingCategoryEntity
import com.dhobigo.tracker.data.local.entity.LaundryItemEntity
import com.dhobigo.tracker.data.local.entity.LaundryRecordEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupMetadata(
    val version: Int = 2,
    val appVersion: String = "1.0.0",
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String,
    val recordCount: Int = 0,
    val imageCount: Int = 0
)

data class BackupRecordDto(
    val record: LaundryRecordEntity,
    val items: List<LaundryItemEntity>
)

sealed class BackupResult {
    data class Success(val recordCount: Int, val imageCount: Int) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

sealed class RestoreResult {
    data class Success(val recordsRestored: Int, val imagesRestored: Int) : RestoreResult()
    data class Error(val message: String) : RestoreResult()
}

class BackupRestoreManager(
    private val context: Context,
    private val recordDao: LaundryRecordDao,
    private val itemDao: LaundryItemDao,
    private val categoryDao: ClothingCategoryDao
) {
    private val gson = Gson()
    private val baseChitsDirectory: File
        get() = File(context.filesDir, "chits").apply { if (!exists()) mkdirs() }

    private fun getUserChitsDirectory(userId: String): File {
        val safeUserId = userId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(baseChitsDirectory, safeUserId).apply { if (!exists()) mkdirs() }
    }

    suspend fun createBackup(outputStream: OutputStream, userId: String): BackupResult = withContext(Dispatchers.IO) {
        try {
            // Strict user isolation: Only export current user's records
            val recordsWithItems = recordDao.getAllRecordsSnapshot(userId)
            val categories = categoryDao.getAllCategories()

            val backupRecords = recordsWithItems.map {
                BackupRecordDto(it.record, it.items)
            }

            var imageCount = 0
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                // 1. Write Records JSON
                zipOut.putNextEntry(ZipEntry("records.json"))
                val writer = OutputStreamWriter(zipOut, Charsets.UTF_8)
                gson.toJson(backupRecords, writer)
                writer.flush()
                zipOut.closeEntry()

                // 2. Write Categories JSON
                zipOut.putNextEntry(ZipEntry("categories.json"))
                gson.toJson(categories, writer)
                writer.flush()
                zipOut.closeEntry()

                // 3. Write Images for this user
                val writtenImages = mutableSetOf<String>()
                val userChitsDir = getUserChitsDirectory(userId)

                for (item in recordsWithItems) {
                    val relPath = item.record.chitImagePath
                    if (!relPath.isNullOrBlank()) {
                        val fileName = File(relPath).name
                        if (!writtenImages.contains(fileName)) {
                            var imgFile = File(userChitsDir, fileName)
                            if (!imgFile.exists()) {
                                // Fallback to base chits dir for legacy paths
                                imgFile = File(baseChitsDirectory, fileName)
                            }

                            if (imgFile.exists() && imgFile.canRead()) {
                                zipOut.putNextEntry(ZipEntry("images/$fileName"))
                                FileInputStream(imgFile).use { inStream ->
                                    inStream.copyTo(zipOut)
                                }
                                zipOut.closeEntry()
                                writtenImages.add(fileName)
                                imageCount++
                            }
                        }
                    }
                }

                // 4. Write Metadata
                val metadata = BackupMetadata(
                    version = 2,
                    appVersion = "1.0.0",
                    timestamp = System.currentTimeMillis(),
                    userId = userId,
                    recordCount = backupRecords.size,
                    imageCount = imageCount
                )
                zipOut.putNextEntry(ZipEntry("metadata.json"))
                gson.toJson(metadata, writer)
                writer.flush()
                zipOut.closeEntry()
            }

            BackupResult.Success(recordCount = backupRecords.size, imageCount = imageCount)
        } catch (e: Exception) {
            BackupResult.Error("Failed to create backup: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    suspend fun restoreBackup(inputStream: InputStream, targetUserId: String): RestoreResult = withContext(Dispatchers.IO) {
        try {
            var recordsJson: String? = null
            var categoriesJson: String? = null
            var metadataJson: String? = null
            var imagesRestored = 0

            // Temporary extract directory to prevent corrupting current data before validation
            val tempExtractDir = File(context.cacheDir, "restore_temp_${System.currentTimeMillis()}").apply { mkdirs() }
            val tempImagesDir = File(tempExtractDir, "images").apply { mkdirs() }

            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val name = entry.name
                    // Prevent zip slip vulnerability
                    if (name.contains("..")) {
                        throw SecurityException("Invalid zip entry: $name")
                    }

                    when {
                        name == "metadata.json" -> {
                            metadataJson = zipIn.reader(Charsets.UTF_8).readText()
                        }
                        name == "records.json" -> {
                            recordsJson = zipIn.reader(Charsets.UTF_8).readText()
                        }
                        name == "categories.json" -> {
                            categoriesJson = zipIn.reader(Charsets.UTF_8).readText()
                        }
                        name.startsWith("images/") -> {
                            val fileName = name.removePrefix("images/")
                            if (fileName.isNotEmpty()) {
                                val outFile = File(tempImagesDir, fileName)
                                FileOutputStream(outFile).use { fileOut ->
                                    zipIn.copyTo(fileOut)
                                }
                                imagesRestored++
                            }
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            if (recordsJson.isNullOrBlank()) {
                tempExtractDir.deleteRecursively()
                return@withContext RestoreResult.Error("Invalid backup archive: missing records database.")
            }

            val recordListType = object : TypeToken<List<BackupRecordDto>>() {}.type
            val restoredRecords: List<BackupRecordDto> = gson.fromJson(recordsJson, recordListType)

            // Validate and import categories
            if (!categoriesJson.isNullOrBlank()) {
                val catListType = object : TypeToken<List<ClothingCategoryEntity>>() {}.type
                val restoredCategories: List<ClothingCategoryEntity> = gson.fromJson(categoriesJson, catListType)
                for (cat in restoredCategories) {
                    val existing = categoryDao.getCategoryByName(cat.name)
                    if (existing == null) {
                        categoryDao.insertCategory(cat.copy(id = 0))
                    }
                }
            }

            // Copy images to destination user chits directory
            val targetUserChitsDir = getUserChitsDirectory(targetUserId)
            val safeUserId = targetUserId.replace(Regex("[^a-zA-Z0-9_-]"), "_")

            if (tempImagesDir.exists()) {
                tempImagesDir.listFiles()?.forEach { file ->
                    val destination = File(targetUserChitsDir, file.name)
                    file.copyTo(destination, overwrite = true)
                }
            }

            // Insert records and items re-scoped to targetUserId
            for (dto in restoredRecords) {
                val imageRelPath = if (!dto.record.chitImagePath.isNullOrBlank()) {
                    val filename = File(dto.record.chitImagePath).name
                    "chits/$safeUserId/$filename"
                } else null

                val scopedRecord = dto.record.copy(
                    userId = targetUserId,
                    chitImagePath = imageRelPath
                )
                recordDao.insertRecord(scopedRecord)
                itemDao.deleteItemsForRecord(scopedRecord.id)
                val itemsWithRecordId = dto.items.map { it.copy(id = 0, laundryRecordId = scopedRecord.id) }
                itemDao.insertItems(itemsWithRecordId)
            }

            tempExtractDir.deleteRecursively()
            RestoreResult.Success(recordsRestored = restoredRecords.size, imagesRestored = imagesRestored)
        } catch (e: Exception) {
            RestoreResult.Error("Restore failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
