package com.dhobigo.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.dhobigo.tracker.data.local.entity.LaundryRecordEntity
import com.dhobigo.tracker.data.local.model.LaundryRecordWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface LaundryRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: LaundryRecordEntity)

    @Update
    suspend fun updateRecord(record: LaundryRecordEntity)

    @Delete
    suspend fun deleteRecord(record: LaundryRecordEntity)

    @Query("DELETE FROM laundry_records WHERE id = :id AND userId = :userId")
    suspend fun deleteRecordById(id: String, userId: String)

    @Query("DELETE FROM laundry_records WHERE userId = :userId")
    suspend fun deleteAllRecordsForUser(userId: String)

    @Transaction
    @Query("SELECT * FROM laundry_records WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getRecordWithItemsById(id: String, userId: String): LaundryRecordWithItems?

    @Transaction
    @Query("SELECT * FROM laundry_records WHERE id = :id AND userId = :userId LIMIT 1")
    fun observeRecordWithItemsById(id: String, userId: String): Flow<LaundryRecordWithItems?>

    @Transaction
    @Query("SELECT * FROM laundry_records WHERE userId = :userId ORDER BY dateGivenMillis DESC, createdAtMillis DESC")
    fun observeAllRecordsWithItems(userId: String): Flow<List<LaundryRecordWithItems>>

    @Transaction
    @Query("SELECT * FROM laundry_records WHERE userId = :userId ORDER BY dateGivenMillis DESC, createdAtMillis DESC LIMIT :limit")
    fun observeRecentRecordsWithItems(userId: String, limit: Int = 5): Flow<List<LaundryRecordWithItems>>

    @Transaction
    @Query("""
        SELECT * FROM laundry_records 
        WHERE userId = :userId
          AND (:query = '' OR id LIKE '%' || :query || '%' 
               OR slipNumber LIKE '%' || :query || '%' 
               OR dhobiName LIKE '%' || :query || '%'
               OR notes LIKE '%' || :query || '%')
          AND (:status IS NULL OR status = :status)
          AND (:fromDate IS NULL OR dateGivenMillis >= :fromDate)
          AND (:toDate IS NULL OR dateGivenMillis <= :toDate)
        ORDER BY dateGivenMillis DESC, createdAtMillis DESC
    """)
    fun observeFilteredRecords(
        userId: String,
        query: String = "",
        status: String? = null,
        fromDate: Long? = null,
        toDate: Long? = null
    ): Flow<List<LaundryRecordWithItems>>

    @Query("SELECT COUNT(*) FROM laundry_records WHERE userId = :userId")
    suspend fun getRecordCount(userId: String): Int

    @Transaction
    @Query("SELECT * FROM laundry_records WHERE userId = :userId ORDER BY dateGivenMillis DESC, createdAtMillis DESC")
    suspend fun getAllRecordsSnapshot(userId: String): List<LaundryRecordWithItems>

    @Query("SELECT * FROM laundry_records WHERE userId = :userId AND LOWER(slipNumber) = LOWER(:slipNumber) LIMIT 1")
    suspend fun findRecordBySlipNumber(userId: String, slipNumber: String): LaundryRecordEntity?
}
