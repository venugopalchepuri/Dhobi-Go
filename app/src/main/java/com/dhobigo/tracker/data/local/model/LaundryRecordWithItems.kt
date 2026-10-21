package com.dhobigo.tracker.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.dhobigo.tracker.data.local.entity.LaundryItemEntity
import com.dhobigo.tracker.data.local.entity.LaundryRecordEntity

data class LaundryRecordWithItems(
    @Embedded
    val record: LaundryRecordEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "laundryRecordId"
    )
    val items: List<LaundryItemEntity> = emptyList()
) {
    val totalGivenCount: Int
        get() = items.sumOf { it.quantityGiven }

    val totalReturnedCount: Int
        get() = items.sumOf { it.quantityReturned }

    val totalOutstandingCount: Int
        get() = items.sumOf { it.quantityOutstanding }

    val computedStatus: LaundryStatus
        get() {
            if (items.isEmpty()) return LaundryStatus.fromString(record.status)
            val given = totalGivenCount
            val returned = totalReturnedCount
            return when {
                returned == 0 -> LaundryStatus.PENDING
                returned >= given -> LaundryStatus.COLLECTED
                else -> LaundryStatus.PARTIALLY_RETURNED
            }
        }

    val isDiscrepantCollected: Boolean
        get() = record.status == LaundryStatus.COLLECTED.name && totalOutstandingCount > 0
}
