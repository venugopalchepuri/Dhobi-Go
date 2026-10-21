package com.dhobigo.tracker.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dhobigo.tracker.data.local.dao.ClothingCategoryDao
import com.dhobigo.tracker.data.local.dao.LaundryItemDao
import com.dhobigo.tracker.data.local.dao.LaundryRecordDao
import com.dhobigo.tracker.data.local.dao.UserDao
import com.dhobigo.tracker.data.local.entity.ClothingCategoryEntity
import com.dhobigo.tracker.data.local.entity.LaundryItemEntity
import com.dhobigo.tracker.data.local.entity.LaundryRecordEntity
import com.dhobigo.tracker.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        LaundryRecordEntity::class,
        LaundryItemEntity::class,
        ClothingCategoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DhobiDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun laundryRecordDao(): LaundryRecordDao
    abstract fun laundryItemDao(): LaundryItemDao
    abstract fun clothingCategoryDao(): ClothingCategoryDao

    companion object {
        @Volatile
        private var INSTANCE: DhobiDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create users table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `users` (
                        `id` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `passwordHash` TEXT NOT NULL,
                        `salt` TEXT NOT NULL,
                        `fullName` TEXT NOT NULL,
                        `phone` TEXT,
                        `college` TEXT,
                        `enrollmentNumber` TEXT,
                        `course` TEXT,
                        `academicYear` TEXT,
                        `batch` TEXT,
                        `profilePhotoPath` TEXT,
                        `createdAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)")

                // 2. Add userId column to laundry_records table with default 'default_user'
                db.execSQL("ALTER TABLE `laundry_records` ADD COLUMN `userId` TEXT NOT NULL DEFAULT 'default_user'")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_laundry_records_userId` ON `laundry_records` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_laundry_records_userId_slipNumber` ON `laundry_records` (`userId`, `slipNumber`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_laundry_records_userId_status` ON `laundry_records` (`userId`, `status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_laundry_records_userId_dateGivenMillis` ON `laundry_records` (`userId`, `dateGivenMillis`)")
            }
        }

        fun getInstance(context: Context): DhobiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DhobiDatabase::class.java,
                    "dhobi_tracker.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default categories
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                val categoryDao = database.clothingCategoryDao()
                                val defaultEntities = ClothingCategoryEntity.DEFAULT_CATEGORIES
                                    .mapIndexed { index, name ->
                                        ClothingCategoryEntity(
                                            name = name,
                                            isDefault = true,
                                            displayOrder = index,
                                            isActive = true
                                        )
                                    }
                                categoryDao.insertCategories(defaultEntities)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
