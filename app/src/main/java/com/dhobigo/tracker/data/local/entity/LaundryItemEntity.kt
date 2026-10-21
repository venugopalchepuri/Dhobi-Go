package com.dhobigo.tracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "laundry_items",
    foreignKeys = [
        ForeignKey(
            entity = LaundryRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["laundryRecordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["laundryRecordId"])
    ]
)
data class LaundryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val laundryRecordId: String,
    val categoryName: String,
    val quantityGiven: Int,
    val quantityReturned: Int = 0
) {
    val quantityOutstanding: Int
        get() = (quantityGiven - quantityReturned).coerceAtLeast(0)
}
