package com.dhobigo.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dhobigo.tracker.data.local.entity.ClothingCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClothingCategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: ClothingCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<ClothingCategoryEntity>)

    @Update
    suspend fun updateCategory(category: ClothingCategoryEntity)

    @Delete
    suspend fun deleteCategory(category: ClothingCategoryEntity)

    @Query("SELECT * FROM clothing_categories WHERE isActive = 1 ORDER BY displayOrder ASC, name ASC")
    fun observeAllActiveCategories(): Flow<List<ClothingCategoryEntity>>

    @Query("SELECT * FROM clothing_categories ORDER BY displayOrder ASC, name ASC")
    fun observeAllCategories(): Flow<List<ClothingCategoryEntity>>

    @Query("SELECT * FROM clothing_categories ORDER BY displayOrder ASC, name ASC")
    suspend fun getAllCategories(): List<ClothingCategoryEntity>

    @Query("SELECT * FROM clothing_categories WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getCategoryByName(name: String): ClothingCategoryEntity?

    @Query("SELECT COUNT(*) FROM clothing_categories")
    suspend fun getCategoryCount(): Int
}
