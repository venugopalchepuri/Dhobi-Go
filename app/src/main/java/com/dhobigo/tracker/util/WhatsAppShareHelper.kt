package com.dhobigo.tracker.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.dhobigo.tracker.data.local.model.LaundryRecordWithItems
import com.dhobigo.tracker.data.local.model.LaundryStatus
import com.dhobigo.tracker.data.storage.ImageStorageManager

object WhatsAppShareHelper {

    fun generateShareMessage(recordWithItems: LaundryRecordWithItems): String {
        val record = recordWithItems.record
        val sb = StringBuilder()

        sb.append("🧺 *DhobiGo Laundry Chit:* ").append(record.id).append("\n")
        sb.append("📅 *Date:* ").append(DateUtils.formatFullDate(record.dateGivenMillis)).append("\n")

        if (!record.slipNumber.isNullOrBlank()) {
            sb.append("🔢 *Slip No.:* ").append(record.slipNumber).append("\n")
        }

        if (!record.dhobiName.isNullOrBlank()) {
            sb.append("🏪 *Shop/Dhobi:* ").append(record.dhobiName).append("\n")
        }

        if (record.amount != null && record.amount > 0) {
            sb.append("💰 *Amount:* ₹").append(String.format("%.2f", record.amount)).append("\n")
        }

        sb.append("\n*Clothing Inventory:*\n")
        val activeItems = recordWithItems.items.filter { it.quantityGiven > 0 }
        if (activeItems.isEmpty()) {
            sb.append("• No items recorded\n")
        } else {
            for (item in activeItems) {
                if (item.quantityReturned > 0) {
                    sb.append("• ").append(item.categoryName).append(": ").append(item.quantityGiven)
                        .append(" (Returned: ").append(item.quantityReturned)
                        .append(", Due: ").append(item.quantityOutstanding).append(")\n")
                } else {
                    sb.append("• ").append(item.categoryName).append(": ").append(item.quantityGiven).append("\n")
                }
            }
        }

        sb.append("\n*Total:* ").append(recordWithItems.totalGivenCount).append(" items\n")
        if (recordWithItems.totalReturnedCount > 0) {
            sb.append("✅ *Returned:* ").append(recordWithItems.totalReturnedCount).append(" items\n")
            sb.append("⏳ *Pending with Dhobi:* ").append(recordWithItems.totalOutstandingCount).append(" items\n")
        }

        val status = recordWithItems.computedStatus
        sb.append("📌 *Status:* ").append(
            when (status) {
                LaundryStatus.PENDING -> "Pending with Dhobi"
                LaundryStatus.PARTIALLY_RETURNED -> "Partially Returned"
                LaundryStatus.COLLECTED -> "Fully Returned & Collected"
            }
        ).append("\n")

        if (!record.notes.isNullOrBlank()) {
            sb.append("📝 *Note:* ").append(record.notes).append("\n")
        }

        sb.append("\nPlease confirm the count. Thank you!\n— Sent via DhobiGo")
        return sb.toString()
    }

    fun shareRecord(
        context: Context,
        recordWithItems: LaundryRecordWithItems,
        imageStorageManager: ImageStorageManager
    ) {
        val message = generateShareMessage(recordWithItems)
        val imageFile = imageStorageManager.getFile(recordWithItems.record.chitImagePath)

        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message)
            if (imageFile != null && imageFile.exists()) {
                val imageUri: Uri = imageStorageManager.getShareableUri(imageFile)
                putExtra(Intent.EXTRA_STREAM, imageUri)
                type = "image/jpeg"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
        }

        val chooserIntent = if (isPackageInstalled(context, "com.whatsapp")) {
            val whatsappIntent = Intent(intent).apply {
                `package` = "com.whatsapp"
            }
            Intent.createChooser(whatsappIntent, "Share Laundry Slip via WhatsApp")
        } else {
            Intent.createChooser(intent, "Share Laundry Slip")
        }

        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }
}
