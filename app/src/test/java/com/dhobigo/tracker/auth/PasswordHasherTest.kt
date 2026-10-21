package com.dhobigo.tracker.auth

import com.dhobigo.tracker.util.PasswordHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun testGenerateSaltIsRandomAndCorrectLength() {
        val salt1 = PasswordHasher.generateSalt()
        val salt2 = PasswordHasher.generateSalt()

        // Salt is 16 bytes decoded
        val decoded1 = java.util.Base64.getDecoder().decode(salt1)
        val decoded2 = java.util.Base64.getDecoder().decode(salt2)
        assertEquals(16, decoded1.size)
        assertEquals(16, decoded2.size)
        assertNotEquals("Two salts should be unique", salt1, salt2)
    }

    @Test
    fun testHashAndVerifyPasswordSuccess() {
        val password = "SecureLaundryPassword123!"
        val salt = PasswordHasher.generateSalt()

        val hash = PasswordHasher.hashPassword(password, salt)
        val decodedHash = java.util.Base64.getDecoder().decode(hash)
        assertEquals(32, decodedHash.size) // 256 bits = 32 bytes

        val verified = PasswordHasher.verifyPassword(password, salt, hash)
        assertTrue("Correct password must verify successfully", verified)
    }

    @Test
    fun testVerifyWrongPasswordFails() {
        val password = "CorrectPassword456"
        val wrongPassword = "WrongPassword789"
        val salt = PasswordHasher.generateSalt()

        val hash = PasswordHasher.hashPassword(password, salt)
        val verified = PasswordHasher.verifyPassword(wrongPassword, salt, hash)
        assertFalse("Wrong password must fail verification", verified)
    }

    @Test
    fun testSamePasswordDifferentSaltsProduceDifferentHashes() {
        val password = "IdenticalPassword"
        val salt1 = PasswordHasher.generateSalt()
        val salt2 = PasswordHasher.generateSalt()

        val hash1 = PasswordHasher.hashPassword(password, salt1)
        val hash2 = PasswordHasher.hashPassword(password, salt2)

        assertNotEquals("Hashes with different salts must differ", hash1, hash2)
    }
}
