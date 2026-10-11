package com.example

import com.example.util.SafeLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeLoggerTest {

    @Test
    fun `sanitize redacts Bearer tokens and OAuth tokens`() {
        val input = "Error with authorization Bearer ya29.a0AfH6SMDh123456789"
        val sanitized = SafeLogger.sanitize(input)
        assertFalse(sanitized.contains("ya29.a0AfH6SMDh123456789"))
        assertTrue(sanitized.contains("[REDACTED_TOKEN]"))
    }

    @Test
    fun `sanitize redacts Google API keys`() {
        val input = "Request failed with key AIzaSyD1234567890abcdefghijklmnopqrstuv"
        val sanitized = SafeLogger.sanitize(input)
        assertFalse(sanitized.contains("AIzaSyD1234567890abcdefghijklmnopqrstuv"))
        assertTrue(sanitized.contains("[REDACTED_TOKEN]"))
    }

    @Test
    fun `sanitize redacts email addresses`() {
        val input = "Teacher john.doe@example.school.edu failed to sync student alice.smith@domain.org"
        val sanitized = SafeLogger.sanitize(input)
        assertFalse(sanitized.contains("john.doe@example.school.edu"))
        assertFalse(sanitized.contains("alice.smith@domain.org"))
        assertEquals("Teacher [REDACTED_EMAIL] failed to sync student [REDACTED_EMAIL]", sanitized)
    }

    @Test
    fun `sanitize redacts spreadsheet IDs in paths`() {
        val input = "Accessing https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit"
        val sanitized = SafeLogger.sanitize(input)
        assertFalse(sanitized.contains("1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms"))
        assertTrue(sanitized.contains("/d/[REDACTED_SHEET_ID]"))
    }

    @Test
    fun `sanitize returns null string for null input`() {
        assertEquals("null", SafeLogger.sanitize(null))
    }

    @Test
    fun `sanitize leaves harmless text unchanged`() {
        val input = "Roster sync completed successfully: Added 3, Updated 5."
        assertEquals(input, SafeLogger.sanitize(input))
    }
}
