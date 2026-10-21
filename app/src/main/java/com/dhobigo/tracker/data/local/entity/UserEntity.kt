package com.dhobigo.tracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    val passwordHash: String,
    val salt: String,
    val fullName: String,
    val phone: String? = null,
    val college: String? = null,
    val enrollmentNumber: String? = null,
    val course: String? = null,
    val academicYear: String? = null,
    val batch: String? = null,
    val profilePhotoPath: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
