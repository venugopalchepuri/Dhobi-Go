package com.dhobigo.tracker

import com.dhobigo.tracker.data.local.entity.LaundryItemEntity
import com.dhobigo.tracker.data.local.entity.LaundryRecordEntity
import com.dhobigo.tracker.data.local.model.LaundryRecordWithItems
import com.dhobigo.tracker.util.WhatsAppShareHelper
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppShareHelperTest {

    @Test
    fun testGenerateShareMessage() {
        val record = LaundryRecordEntity(
            id = "DT-0001",
            slipNumber = "12345",
            dhobiName = "Raju Dhobi",
            dateGivenMillis = 1792440000000L, // 20 Oct 2026 approx
            amount = 140.0,
            notes = "Please press neatly"
        )

        val items = listOf(
            LaundryItemEntity(id = 1, laundryRecordId = record.id, categoryName = "Shirt", quantityGiven = 2, quantityReturned = 0),
            LaundryItemEntity(id = 2, laundryRecordId = record.id, categoryName = "T-Shirt", quantityGiven = 3, quantityReturned = 0),
            LaundryItemEntity(id = 3, laundryRecordId = record.id, categoryName = "Pant", quantityGiven = 1, quantityReturned = 0),
            LaundryItemEntity(id = 4, laundryRecordId = record.id, categoryName = "Towel", quantityGiven = 1, quantityReturned = 0)
        )

        val recordWithItems = LaundryRecordWithItems(record = record, items = items)
        val message = WhatsAppShareHelper.generateShareMessage(recordWithItems)

        assertTrue(message.contains("DT-0001"))
        assertTrue(message.contains("12345"))
        assertTrue(message.contains("Raju Dhobi"))
        assertTrue(message.contains("Shirt: 2"))
        assertTrue(message.contains("T-Shirt: 3"))
        assertTrue(message.contains("Pant: 1"))
        assertTrue(message.contains("Towel: 1"))
        assertTrue(message.contains("Total:* 7 items"))
        assertTrue(message.contains("Please confirm the count"))
    }
}
