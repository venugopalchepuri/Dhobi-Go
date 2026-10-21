package com.dhobigo.tracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clothing_categories",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class ClothingCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false,
    val displayOrder: Int = 0,
    val isActive: Boolean = true
) {
    companion object {
        val DEFAULT_CATEGORIES = listOf(
            "Kurta",
            "Pajama",
            "Shirt",
            "T-Shirt",
            "Pant",
            "Lower",
            "Shorts",
            "Bedsheet",
            "Pillow Cover",
            "Towel",
            "Duppata",
            "Under Garments",
            "Socks",
            "Hankey"
        )
    }
}
