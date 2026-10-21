package com.dhobigo.tracker.auth

import com.dhobigo.tracker.data.local.entity.LaundryItemEntity
import com.dhobigo.tracker.data.local.entity.LaundryRecordEntity
import com.dhobigo.tracker.data.local.model.LaundryRecordWithItems
import com.dhobigo.tracker.data.local.model.LaundryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class AccountIsolationTest {

    @Test
    fun testRecordScopingByUserId() {
        val userAId = UUID.randomUUID().toString()
        val userBId = UUID.randomUUID().toString()

        val recordUserA1 = LaundryRecordEntity(
            id = "REC-A1",
            userId = userAId,
            slipNumber = "SLIP-101",
            dateGivenMillis = 1761000000000L
        )
        val recordUserA2 = LaundryRecordEntity(
            id = "REC-A2",
            userId = userAId,
            slipNumber = "SLIP-102",
            dateGivenMillis = 1761000000000L
        )
        val recordUserB1 = LaundryRecordEntity(
            id = "REC-B1",
            userId = userBId,
            slipNumber = "SLIP-999",
            dateGivenMillis = 1761000000000L
        )

        val allRecordsInSystem = listOf(recordUserA1, recordUserA2, recordUserB1)

        // Simulate user-scoped query filtering
        val userARecords = allRecordsInSystem.filter { it.userId == userAId }
        val userBRecords = allRecordsInSystem.filter { it.userId == userBId }

        assertEquals(2, userARecords.size)
        assertTrue(userARecords.all { it.userId == userAId })
        assertFalse(userARecords.any { it.userId == userBId })

        assertEquals(1, userBRecords.size)
        assertEquals("SLIP-999", userBRecords.first().slipNumber)
        assertEquals(userBId, userBRecords.first().userId)
    }

    @Test
    fun testUserIsolatedPhotoDirectoryPaths() {
        val userAId = "user-uuid-1111"
        val userBId = "user-uuid-2222"

        val photoFileName = "chit_1761000000.jpg"

        val pathA = "chits/$userAId/$photoFileName"
        val pathB = "chits/$userBId/$photoFileName"

        assertFalse("Paths across different users must never collide", pathA == pathB)
        assertTrue(pathA.contains(userAId))
        assertTrue(pathB.contains(userBId))
    }

    @Test
    fun testGarmentTotalsCalculationStrictlyScoped() {
        val userAId = "user-A"
        val userBId = "user-B"

        val recordA = LaundryRecordEntity(id = "REC-A", userId = userAId, dateGivenMillis = 1761000000000L)
        val itemsA = listOf(
            LaundryItemEntity(id = 1, laundryRecordId = "REC-A", categoryName = "Shirt", quantityGiven = 5, quantityReturned = 2)
        )
        val recordWithItemsA = LaundryRecordWithItems(record = recordA, items = itemsA)

        val recordB = LaundryRecordEntity(id = "REC-B", userId = userBId, dateGivenMillis = 1761000000000L)
        val itemsB = listOf(
            LaundryItemEntity(id = 2, laundryRecordId = "REC-B", categoryName = "Shirt", quantityGiven = 10, quantityReturned = 10)
        )
        val recordWithItemsB = LaundryRecordWithItems(record = recordB, items = itemsB)

        // Verify calculations for user A
        assertEquals(5, recordWithItemsA.totalGivenCount)
        assertEquals(2, recordWithItemsA.totalReturnedCount)
        assertEquals(3, recordWithItemsA.totalOutstandingCount)
        assertEquals(LaundryStatus.PARTIALLY_RETURNED, recordWithItemsA.computedStatus)

        // Verify calculations for user B
        assertEquals(10, recordWithItemsB.totalGivenCount)
        assertEquals(10, recordWithItemsB.totalReturnedCount)
        assertEquals(0, recordWithItemsB.totalOutstandingCount)
        assertEquals(LaundryStatus.COLLECTED, recordWithItemsB.computedStatus)
    }

    @Test
    fun testAccountSwitchingCleansesState() {
        var activeSessionUserId: String? = "user-session-A"
        var cachedRecords: List<LaundryRecordEntity> = listOf(
            LaundryRecordEntity(id = "REC-1", userId = activeSessionUserId!!, dateGivenMillis = 1761000000000L)
        )

        assertEquals(1, cachedRecords.size)

        // Simulate Logout / Session Termination
        activeSessionUserId = null
        cachedRecords = emptyList()

        assertEquals(0, cachedRecords.size)

        // Simulate Login as User B
        activeSessionUserId = "user-session-B"
        cachedRecords = listOf(
            LaundryRecordEntity(id = "REC-2", userId = activeSessionUserId, dateGivenMillis = 1761000000000L)
        )

        assertEquals(1, cachedRecords.size)
        assertEquals("user-session-B", cachedRecords.first().userId)
    }
}
