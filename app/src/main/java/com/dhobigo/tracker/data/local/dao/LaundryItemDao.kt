package com.dhobigo.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dhobigo.tracker.data.local.entity.LaundryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LaundryItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<LaundryItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: LaundryItemEntity): Long

    @Update
    suspend fun updateItem(item: LaundryItemEntity)

    @Update
    suspend fun updateItems(items: List<LaundryItemEntity>)

    @Query("DELETE FROM laundry_items WHERE laundryRecordId = :recordId")
    suspend fun deleteItemsForRecord(recordId: String)

    @Query("SELECT * FROM laundry_items WHERE laundryRecordId = :recordId")
    suspend fun getItemsForRecord(recordId: String): List<LaundryItemEntity>

    @Query("SELECT * FROM laundry_items WHERE laundryRecordId = :recordId")
    fun observeItemsForRecord(recordId: String): Flow<List<LaundryItemEntity>>
}
