package com.aistudio.classroomlms

import com.aistudio.classroomlms.data.database.DatabaseMigrationEvaluator
import com.aistudio.classroomlms.data.database.MigrationResult
import com.aistudio.classroomlms.data.database.MigrationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseMigrationEvaluatorTest {

    @Test
    fun `State A - returns FIRST_INSTALL when neither database exists`() {
        val state = DatabaseMigrationEvaluator.evaluate(
            legacyDbExists = false,
            encryptedDbExists = false,
            isMigrationDoneInPrefs = false,
            encryptedDbHasData = false
        )
        assertEquals(MigrationState.FIRST_INSTALL, state)
    }

    @Test
    fun `State B - returns NORMAL_ENCRYPTED when only encrypted database exists`() {
        val state = DatabaseMigrationEvaluator.evaluate(
            legacyDbExists = false,
            encryptedDbExists = true,
            isMigrationDoneInPrefs = false,
            encryptedDbHasData = true
        )
        assertEquals(MigrationState.NORMAL_ENCRYPTED, state)
    }

    @Test
    fun `State B - returns NORMAL_ENCRYPTED when migration was previously marked complete in prefs`() {
        val state = DatabaseMigrationEvaluator.evaluate(
            legacyDbExists = true,
            encryptedDbExists = true,
            isMigrationDoneInPrefs = true,
            encryptedDbHasData = true
        )
        assertEquals(MigrationState.NORMAL_ENCRYPTED, state)
    }

    @Test
    fun `State C - returns MIGRATION_REQUIRED when legacy exists and encrypted database is missing`() {
        val state = DatabaseMigrationEvaluator.evaluate(
            legacyDbExists = true,
            encryptedDbExists = false,
            isMigrationDoneInPrefs = false,
            encryptedDbHasData = false
        )
        assertEquals(MigrationState.MIGRATION_REQUIRED, state)
    }

    @Test
    fun `State C - returns MIGRATION_REQUIRED when legacy exists and encrypted database is empty`() {
        val state = DatabaseMigrationEvaluator.evaluate(
            legacyDbExists = true,
            encryptedDbExists = true,
            isMigrationDoneInPrefs = false,
            encryptedDbHasData = false
        )
        assertEquals(MigrationState.MIGRATION_REQUIRED, state)
    }

    @Test
    fun `State D - returns BOTH_EXIST_PREFER_ENCRYPTED when both exist and encrypted has data`() {
        val state = DatabaseMigrationEvaluator.evaluate(
            legacyDbExists = true,
            encryptedDbExists = true,
            isMigrationDoneInPrefs = false,
            encryptedDbHasData = true
        )
        assertEquals(MigrationState.BOTH_EXIST_PREFER_ENCRYPTED, state)
    }

    @Test
    fun `Prefs override - returns NORMAL_ENCRYPTED if flag is set even if legacy file is still present`() {
        val state = DatabaseMigrationEvaluator.evaluate(
            legacyDbExists = true,
            encryptedDbExists = false,
            isMigrationDoneInPrefs = true,
            encryptedDbHasData = false
        )
        assertEquals(MigrationState.NORMAL_ENCRYPTED, state)
    }

    @Test
    fun `MigrationResult models capture success and failure accurately`() {
        val success = MigrationResult.Success(classroomsMigrated = 4, studentsMigrated = 42)
        assertEquals(4, success.classroomsMigrated)
        assertEquals(42, success.studentsMigrated)

        val exception = IllegalStateException("Disk full")
        val failure = MigrationResult.Failure(exception, "Disk full error")
        assertEquals("Disk full error", failure.message)
        assertTrue(failure.error is IllegalStateException)
    }
}
