package com.dhobigo.tracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "laundry_records",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "slipNumber"]),
        Index(value = ["userId", "status"]),
        Index(value = ["userId", "dateGivenMillis"])
    ]
)
data class LaundryRecordEntity(
    @PrimaryKey
    val id: String, // e.g. "DT-261020-001" or UUID
    val userId: String = "default_user", // Scopes record to authenticated account
    val slipNumber: String? = null,
    val dateGivenMillis: Long,
    val dhobiName: String? = null,
    val totalItems: Int = 0,
    val amount: Double? = null,
    val expectedReturnDateMillis: Long? = null,
    val notes: String? = null,
    val status: String = "PENDING", // PENDING, PARTIALLY_RETURNED, COLLECTED
    val chitImagePath: String? = null, // relative path in app files dir: "chits/<filename>.jpg"
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)
