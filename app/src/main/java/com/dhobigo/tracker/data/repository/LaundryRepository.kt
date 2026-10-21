package com.dhobigo.tracker.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.room.withTransaction
import com.dhobigo.tracker.data.auth.SessionManager
import com.dhobigo.tracker.data.backup.BackupRestoreManager
import com.dhobigo.tracker.data.backup.BackupResult
import com.dhobigo.tracker.data.backup.RestoreResult
import com.dhobigo.tracker.data.local.database.DhobiDatabase
import com.dhobigo.tracker.data.local.entity.ClothingCategoryEntity
import com.dhobigo.tracker.data.local.entity.LaundryItemEntity
import com.dhobigo.tracker.data.local.entity.LaundryRecordEntity
import com.dhobigo.tracker.data.local.model.DashboardStats
import com.dhobigo.tracker.data.local.model.LaundryRecordWithItems
import com.dhobigo.tracker.data.local.model.LaundryStatus
import com.dhobigo.tracker.data.storage.ImageStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LaundryRepository(
    private val context: Context,
    private val database: DhobiDatabase,
    val imageStorageManager: ImageStorageManager,
    val sessionManager: SessionManager
) {
    private val recordDao = database.laundryRecordDao()
    private val itemDao = database.laundryItemDao()
    private val categoryDao = database.clothingCategoryDao()

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("dhobi_tracker_prefs", Context.MODE_PRIVATE)
    }

    private val backupManager by lazy {
        BackupRestoreManager(context, recordDao, itemDao, categoryDao)
    }

    fun getRequiredUserId(): String {
        return sessionManager.getRequiredUserId()
    }

    // Reactively observe dashboard stats for current authenticated user
    fun observeDashboardStats(): Flow<DashboardStats> {
        return sessionManager.currentUserId.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) {
                flowOf(DashboardStats(0, 0, 0, 0))
            } else {
                recordDao.observeAllRecordsWithItems(userId).map { records ->
                    var bagsAtDhobi = 0
                    var itemsAwaiting = 0
                    var partiallyReturnedBags = 0
                    var totalCollected = 0

                    for (recordWithItems in records) {
                        val status = recordWithItems.computedStatus
                        when (status) {
                            LaundryStatus.PENDING -> {
                                bagsAtDhobi++
                                itemsAwaiting += recordWithItems.totalOutstandingCount
                            }
                            LaundryStatus.PARTIALLY_RETURNED -> {
                                bagsAtDhobi++
                                partiallyReturnedBags++
                                itemsAwaiting += recordWithItems.totalOutstandingCount
                            }
                            LaundryStatus.COLLECTED -> {
                                totalCollected++
                            }
                        }
                    }

                    DashboardStats(
                        bagsAtDhobi = bagsAtDhobi,
                        itemsAwaitingReturn = itemsAwaiting,
                        partiallyReturnedBags = partiallyReturnedBags,
                        totalCollectedBags = totalCollected
                    )
                }
            }
        }
    }

    fun observeRecentRecords(limit: Int = 5): Flow<List<LaundryRecordWithItems>> {
        return sessionManager.currentUserId.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                recordDao.observeRecentRecordsWithItems(userId, limit)
            }
        }
    }

    fun observeFilteredRecords(
        query: String = "",
        status: String? = null,
        fromDate: Long? = null,
        toDate: Long? = null
    ): Flow<List<LaundryRecordWithItems>> {
        return sessionManager.currentUserId.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                recordDao.observeFilteredRecords(
                    userId = userId,
                    query = query.trim(),
                    status = status,
                    fromDate = fromDate,
                    toDate = toDate
                )
            }
        }
    }

    fun observeRecordById(id: String): Flow<LaundryRecordWithItems?> {
        return sessionManager.currentUserId.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) {
                flowOf(null)
            } else {
                recordDao.observeRecordWithItemsById(id, userId)
            }
        }
    }

    suspend fun getRecordById(id: String): LaundryRecordWithItems? = withContext(Dispatchers.IO) {
        val userId = sessionManager.getActiveUserId() ?: return@withContext null
        recordDao.getRecordWithItemsById(id, userId)
    }

    suspend fun findRecordBySlipNumber(slipNumber: String): LaundryRecordEntity? = withContext(Dispatchers.IO) {
        val userId = sessionManager.getActiveUserId() ?: return@withContext null
        recordDao.findRecordBySlipNumber(userId, slipNumber.trim())
    }

    /**
     * Generates a clean human-readable record ID: "DT-YYMMDD-XXX" scoped to current user count.
     */
    suspend fun generateNextRecordId(): String = withContext(Dispatchers.IO) {
        val userId = sessionManager.getActiveUserId() ?: "anon"
        val dateFormat = SimpleDateFormat("yyMMdd", Locale.getDefault())
        val datePart = dateFormat.format(Date())
        val count = recordDao.getRecordCount(userId) + 1
        val sequencePart = String.format(Locale.getDefault(), "%03d", count % 1000)
        "DT-$datePart-$sequencePart"
    }

    /**
     * Saves a new Laundry Record and its itemized clothing list in a transaction.
     */
    suspend fun saveNewRecord(
        record: LaundryRecordEntity,
        items: List<LaundryItemEntity>
    ) = withContext(Dispatchers.IO) {
        val userId = getRequiredUserId()
        database.withTransaction {
            val totalCount = items.sumOf { it.quantityGiven }
            val recordToSave = record.copy(
                userId = userId,
                totalItems = totalCount,
                status = LaundryStatus.PENDING.name,
                createdAtMillis = System.currentTimeMillis(),
                updatedAtMillis = System.currentTimeMillis()
            )
            recordDao.insertRecord(recordToSave)

            val itemsToSave = items
                .filter { it.quantityGiven > 0 }
                .map { it.copy(id = 0, laundryRecordId = recordToSave.id, quantityReturned = 0) }

            itemDao.insertItems(itemsToSave)
        }
    }

    /**
     * Updates an existing record and its items in a transaction.
     */
    suspend fun updateRecord(
        record: LaundryRecordEntity,
        items: List<LaundryItemEntity>
    ) = withContext(Dispatchers.IO) {
        val userId = getRequiredUserId()
        database.withTransaction {
            val totalCount = items.sumOf { it.quantityGiven }
            val existing = recordDao.getRecordWithItemsById(record.id, userId)
                ?: throw IllegalStateException("Record not found or access denied.")

            val currentStatus = existing.computedStatus

            val recordToSave = record.copy(
                userId = userId,
                totalItems = totalCount,
                status = currentStatus.name,
                updatedAtMillis = System.currentTimeMillis()
            )
            recordDao.updateRecord(recordToSave)

            // Re-sync items while preserving return counts
            val existingItemMap = existing.items.associateBy { it.categoryName }
            itemDao.deleteItemsForRecord(record.id)

            val itemsToSave = items
                .filter { it.quantityGiven > 0 }
                .map { item ->
                    val returned = existingItemMap[item.categoryName]?.quantityReturned ?: 0
                    item.copy(
                        id = 0,
                        laundryRecordId = record.id,
                        quantityReturned = returned.coerceAtMost(item.quantityGiven)
                    )
                }
            itemDao.insertItems(itemsToSave)
        }
    }

    /**
     * Deletes a laundry record, its items, and its chit image from disk.
     */
    suspend fun deleteRecord(id: String) = withContext(Dispatchers.IO) {
        val userId = getRequiredUserId()
        val existing = recordDao.getRecordWithItemsById(id, userId) ?: return@withContext
        val imagePath = existing.record.chitImagePath

        database.withTransaction {
            itemDao.deleteItemsForRecord(id)
            recordDao.deleteRecordById(id, userId)
        }

        if (!imagePath.isNullOrBlank()) {
            imageStorageManager.deleteImage(imagePath)
        }
    }

    /**
     * Updates return quantities for items.
     * Prevents returned from exceeding given.
     * Automatically calculates new status (PENDING, PARTIALLY_RETURNED, COLLECTED).
     */
    suspend fun updateCollectionProgress(
        recordId: String,
        returnedQuantitiesByItemId: Map<Long, Int>,
        forceCollected: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val userId = getRequiredUserId()
        database.withTransaction {
            val recordWithItems = recordDao.getRecordWithItemsById(recordId, userId) ?: return@withTransaction
            val updatedItems = recordWithItems.items.map { item ->
                val newReturned = returnedQuantitiesByItemId[item.id] ?: item.quantityReturned
                val boundedReturned = newReturned.coerceIn(0, item.quantityGiven)
                item.copy(quantityReturned = boundedReturned)
            }
            itemDao.updateItems(updatedItems)

            val totalGiven = updatedItems.sumOf { it.quantityGiven }
            val totalReturned = updatedItems.sumOf { it.quantityReturned }

            val newStatus = when {
                forceCollected -> LaundryStatus.COLLECTED
                totalReturned == 0 -> LaundryStatus.PENDING
                totalReturned >= totalGiven -> LaundryStatus.COLLECTED
                else -> LaundryStatus.PARTIALLY_RETURNED
            }

            val updatedRecord = recordWithItems.record.copy(
                status = newStatus.name,
                updatedAtMillis = System.currentTimeMillis()
            )
            recordDao.updateRecord(updatedRecord)
        }
    }

    /**
     * Marks all items in a record as returned.
     */
    suspend fun markAllItemsReturned(recordId: String) = withContext(Dispatchers.IO) {
        val userId = getRequiredUserId()
        database.withTransaction {
            val recordWithItems = recordDao.getRecordWithItemsById(recordId, userId) ?: return@withTransaction
            val allReturnedItems = recordWithItems.items.map { item ->
                item.copy(quantityReturned = item.quantityGiven)
            }
            itemDao.updateItems(allReturnedItems)

            val updatedRecord = recordWithItems.record.copy(
                status = LaundryStatus.COLLECTED.name,
                updatedAtMillis = System.currentTimeMillis()
            )
            recordDao.updateRecord(updatedRecord)
        }
    }

    // Categories
    fun observeActiveCategories(): Flow<List<ClothingCategoryEntity>> {
        return categoryDao.observeAllActiveCategories()
    }

    fun observeAllCategories(): Flow<List<ClothingCategoryEntity>> {
        return categoryDao.observeAllCategories()
    }

    suspend fun addCustomCategory(name: String) = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@withContext
        val existing = categoryDao.getCategoryByName(trimmed)
        if (existing == null) {
            val count = categoryDao.getCategoryCount()
            categoryDao.insertCategory(
                ClothingCategoryEntity(
                    name = trimmed,
                    isDefault = false,
                    displayOrder = count,
                    isActive = true
                )
            )
        }
    }

    suspend fun updateCategory(category: ClothingCategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: ClothingCategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.deleteCategory(category)
    }

    // User Preferences (Scoped by user ID)
    fun getDefaultDhobiName(): String {
        val userId = sessionManager.getActiveUserId() ?: "global"
        return prefs.getString("default_dhobi_name_$userId", "") ?: ""
    }

    fun setDefaultDhobiName(name: String) {
        val userId = sessionManager.getActiveUserId() ?: "global"
        prefs.edit().putString("default_dhobi_name_$userId", name.trim()).apply()
    }

    // Backup & Restore
    suspend fun createBackup(outputStream: OutputStream): BackupResult {
        val userId = getRequiredUserId()
        return backupManager.createBackup(outputStream, userId)
    }

    suspend fun restoreBackup(inputStream: InputStream): RestoreResult {
        val userId = getRequiredUserId()
        return backupManager.restoreBackup(inputStream, userId)
    }
}
