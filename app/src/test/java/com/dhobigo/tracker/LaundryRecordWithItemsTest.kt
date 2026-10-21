package com.dhobigo.tracker

import com.dhobigo.tracker.data.local.entity.LaundryItemEntity
import com.dhobigo.tracker.data.local.entity.LaundryRecordEntity
import com.dhobigo.tracker.data.local.model.LaundryRecordWithItems
import com.dhobigo.tracker.data.local.model.LaundryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaundryRecordWithItemsTest {

    @Test
    fun testTotalsAndPendingStatus() {
        val record = LaundryRecordEntity(
            id = "DT-261020-001",
            slipNumber = "12345",
            dateGivenMillis = 1761000000000L
        )

        val items = listOf(
            LaundryItemEntity(id = 1, laundryRecordId = record.id, categoryName = "Shirt", quantityGiven = 3, quantityReturned = 0),
            LaundryItemEntity(id = 2, laundryRecordId = record.id, categoryName = "Pant", quantityGiven = 2, quantityReturned = 0),
            LaundryItemEntity(id = 3, laundryRecordId = record.id, categoryName = "Towel", quantityGiven = 1, quantityReturned = 0)
        )

        val recordWithItems = LaundryRecordWithItems(record = record, items = items)

        assertEquals(6, recordWithItems.totalGivenCount)
        assertEquals(0, recordWithItems.totalReturnedCount)
        assertEquals(6, recordWithItems.totalOutstandingCount)
        assertEquals(LaundryStatus.PENDING, recordWithItems.computedStatus)
        assertFalse(recordWithItems.isDiscrepantCollected)
    }

    @Test
    fun testPartiallyReturnedStatus() {
        val record = LaundryRecordEntity(
            id = "DT-261020-002",
            dateGivenMillis = 1761000000000L
        )

        val items = listOf(
            LaundryItemEntity(id = 1, laundryRecordId = record.id, categoryName = "Shirt", quantityGiven = 4, quantityReturned = 2),
            LaundryItemEntity(id = 2, laundryRecordId = record.id, categoryName = "Pant", quantityGiven = 2, quantityReturned = 0)
        )

        val recordWithItems = LaundryRecordWithItems(record = record, items = items)

        assertEquals(6, recordWithItems.totalGivenCount)
        assertEquals(2, recordWithItems.totalReturnedCount)
        assertEquals(4, recordWithItems.totalOutstandingCount)
        assertEquals(LaundryStatus.PARTIALLY_RETURNED, recordWithItems.computedStatus)
    }

    @Test
    fun testCollectedStatus() {
        val record = LaundryRecordEntity(
            id = "DT-261020-003",
            dateGivenMillis = 1761000000000L
        )

        val items = listOf(
            LaundryItemEntity(id = 1, laundryRecordId = record.id, categoryName = "Kurta", quantityGiven = 2, quantityReturned = 2),
            LaundryItemEntity(id = 2, laundryRecordId = record.id, categoryName = "Pajama", quantityGiven = 2, quantityReturned = 2)
        )

        val recordWithItems = LaundryRecordWithItems(record = record, items = items)

        assertEquals(4, recordWithItems.totalGivenCount)
        assertEquals(4, recordWithItems.totalReturnedCount)
        assertEquals(0, recordWithItems.totalOutstandingCount)
        assertEquals(LaundryStatus.COLLECTED, recordWithItems.computedStatus)
    }

    @Test
    fun testDiscrepantCollectedFlag() {
        val record = LaundryRecordEntity(
            id = "DT-261020-004",
            dateGivenMillis = 1761000000000L,
            status = "COLLECTED"
        )

        val items = listOf(
            LaundryItemEntity(id = 1, laundryRecordId = record.id, categoryName = "Shirt", quantityGiven = 3, quantityReturned = 2)
        )

        val recordWithItems = LaundryRecordWithItems(record = record, items = items)

        assertEquals(1, recordWithItems.totalOutstandingCount)
        assertTrue(recordWithItems.isDiscrepantCollected)
    }
}
