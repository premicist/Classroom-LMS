package com.example

import com.example.data.repository.UpdateRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest

class UpdateRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testVersionComparisonHigher() {
        assertTrue(UpdateRepository.isVersionHigher("1.6.1", "1.6.0"))
        assertTrue(UpdateRepository.isVersionHigher("2.0.0", "1.9.9"))
        assertTrue(UpdateRepository.isVersionHigher("1.7.0", "1.6.9"))
    }

    @Test
    fun testVersionComparisonEqualOrLower() {
        assertFalse(UpdateRepository.isVersionHigher("1.6.0", "1.6.0"))
        assertFalse(UpdateRepository.isVersionHigher("1.5.9", "1.6.0"))
        assertFalse(UpdateRepository.isVersionHigher("1.0.0", "2.0.0"))
    }

    @Test
    fun testFileSha256VerificationValid() {
        val testFile = tempFolder.newFile("sample_app.apk")
        testFile.writeText("sample apk payload for testing checksum validation")

        val digest = MessageDigest.getInstance("SHA-256")
        val expectedHash = digest.digest(testFile.readBytes()).joinToString("") { "%02x".format(it) }

        assertTrue(UpdateRepository.verifyFileSha256(testFile, expectedHash))
    }

    @Test
    fun testFileSha256VerificationInvalid() {
        val testFile = tempFolder.newFile("sample_app_tampered.apk")
        testFile.writeText("corrupted payload")

        val fakeHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

        assertFalse(UpdateRepository.verifyFileSha256(testFile, fakeHash))
    }

    @Test
    fun testFileSha256NonExistentFile() {
        val missingFile = File(tempFolder.root, "does_not_exist.apk")
        assertFalse(UpdateRepository.verifyFileSha256(missingFile, "somehash"))
    }
}
