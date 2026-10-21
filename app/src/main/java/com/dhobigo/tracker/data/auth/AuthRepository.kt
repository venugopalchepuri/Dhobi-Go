package com.dhobigo.tracker.data.auth

import com.dhobigo.tracker.data.local.dao.LaundryItemDao
import com.dhobigo.tracker.data.local.dao.LaundryRecordDao
import com.dhobigo.tracker.data.local.dao.UserDao
import com.dhobigo.tracker.data.local.entity.UserEntity
import com.dhobigo.tracker.data.storage.ImageStorageManager
import com.dhobigo.tracker.util.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

sealed class AuthResult {
    data class Success(val user: UserEntity) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(
    private val userDao: UserDao,
    private val recordDao: LaundryRecordDao,
    private val itemDao: LaundryItemDao,
    private val sessionManager: SessionManager,
    private val imageStorageManager: ImageStorageManager
) {
    val currentUserFlow: Flow<UserEntity?> = sessionManager.currentUserFlow

    val activeUserId: String?
        get() = sessionManager.getActiveUserId()

    val isLoggedIn: Boolean
        get() = sessionManager.isLoggedIn

    suspend fun register(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        phone: String? = null,
        college: String? = null,
        enrollmentNumber: String? = null,
        course: String? = null,
        academicYear: String? = null,
        batch: String? = null
    ): AuthResult = withContext(Dispatchers.IO) {
        val trimmedName = fullName.trim()
        val trimmedEmail = email.trim().lowercase()
        val trimmedPhone = phone?.trim()?.ifBlank { null }

        if (trimmedName.isBlank()) {
            return@withContext AuthResult.Error("Please enter your full name.")
        }
        if (trimmedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return@withContext AuthResult.Error("Please enter a valid email address.")
        }
        if (password.length < 6) {
            return@withContext AuthResult.Error("Password must be at least 6 characters.")
        }
        if (password != confirmPassword) {
            return@withContext AuthResult.Error("Passwords do not match.")
        }

        val existingUser = userDao.getUserByEmail(trimmedEmail)
        if (existingUser != null) {
            return@withContext AuthResult.Error("An account with this email already exists.")
        }

        val salt = PasswordHasher.generateSalt()
        val passwordHash = PasswordHasher.hashPassword(password, salt)

        val newUser = UserEntity(
            id = UUID.randomUUID().toString(),
            email = trimmedEmail,
            passwordHash = passwordHash,
            salt = salt,
            fullName = trimmedName,
            phone = trimmedPhone,
            college = college?.trim()?.ifBlank { null },
            enrollmentNumber = enrollmentNumber?.trim()?.ifBlank { null },
            course = course?.trim()?.ifBlank { null },
            academicYear = academicYear?.trim()?.ifBlank { null },
            batch = batch?.trim()?.ifBlank { null },
            createdAtMillis = System.currentTimeMillis()
        )

        userDao.insertUser(newUser)
        sessionManager.login(newUser.id)
        AuthResult.Success(newUser)
    }

    suspend fun login(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank()) {
            return@withContext AuthResult.Error("Please enter your email.")
        }
        if (password.isBlank()) {
            return@withContext AuthResult.Error("Please enter your password.")
        }

        val user = userDao.getUserByEmail(trimmedEmail)
            ?: return@withContext AuthResult.Error("Invalid email or password.")

        val isValid = PasswordHasher.verifyPassword(password, user.salt, user.passwordHash)
        if (!isValid) {
            return@withContext AuthResult.Error("Invalid email or password.")
        }

        sessionManager.login(user.id)
        AuthResult.Success(user)
    }

    suspend fun resetPassword(email: String, newPassword: String): AuthResult = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank()) {
            return@withContext AuthResult.Error("Please enter your registered email.")
        }
        if (newPassword.length < 6) {
            return@withContext AuthResult.Error("New password must be at least 6 characters.")
        }

        val user = userDao.getUserByEmail(trimmedEmail)
            ?: return@withContext AuthResult.Error("No account found with this email.")

        val newSalt = PasswordHasher.generateSalt()
        val newHash = PasswordHasher.hashPassword(newPassword, newSalt)
        val updatedUser = user.copy(passwordHash = newHash, salt = newSalt)
        userDao.updateUser(updatedUser)

        AuthResult.Success(updatedUser)
    }

    suspend fun updateProfile(
        fullName: String,
        phone: String? = null,
        college: String? = null,
        enrollmentNumber: String? = null,
        course: String? = null,
        academicYear: String? = null,
        batch: String? = null
    ): AuthResult = withContext(Dispatchers.IO) {
        val userId = sessionManager.getActiveUserId()
            ?: return@withContext AuthResult.Error("No active user session.")

        val user = userDao.getUserById(userId)
            ?: return@withContext AuthResult.Error("User not found.")

        val updatedUser = user.copy(
            fullName = fullName.trim().ifBlank { user.fullName },
            phone = phone?.trim()?.ifBlank { null },
            college = college?.trim()?.ifBlank { null },
            enrollmentNumber = enrollmentNumber?.trim()?.ifBlank { null },
            course = course?.trim()?.ifBlank { null },
            academicYear = academicYear?.trim()?.ifBlank { null },
            batch = batch?.trim()?.ifBlank { null }
        )

        userDao.updateUser(updatedUser)
        AuthResult.Success(updatedUser)
    }

    fun logout() {
        sessionManager.logout()
    }

    suspend fun deleteAccount(): Boolean = withContext(Dispatchers.IO) {
        val userId = sessionManager.getActiveUserId() ?: return@withContext false
        val records = recordDao.getAllRecordsSnapshot(userId)

        // Delete all associated chit images from disk
        for (item in records) {
            val img = item.record.chitImagePath
            if (!img.isNullOrBlank()) {
                imageStorageManager.deleteImage(img)
            }
            itemDao.deleteItemsForRecord(item.record.id)
        }

        // Delete records and user
        recordDao.deleteAllRecordsForUser(userId)
        userDao.deleteUserById(userId)

        // Logout
        sessionManager.logout()
        true
    }
}
