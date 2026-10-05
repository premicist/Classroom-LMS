package com.example

import com.example.util.ParsedCsvStudent
import com.example.util.SpreadsheetUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class SpreadsheetUtilsTest {

    @Test
    fun testExtractSpreadsheetIdFromRawId() {
        val rawId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms"
        val result = SpreadsheetUtils.extractSpreadsheetId(rawId)
        assertEquals("1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms", result)
    }

    @Test
    fun testExtractSpreadsheetIdFromFullUrl() {
        val url = "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit#gid=0"
        val result = SpreadsheetUtils.extractSpreadsheetId(url)
        assertEquals("1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms", result)
    }

    @Test
    fun testExtractSpreadsheetIdFromUrlWithParams() {
        val url = "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit?usp=sharing"
        val result = SpreadsheetUtils.extractSpreadsheetId(url)
        assertEquals("1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms", result)
    }

    @Test
    fun testExtractSpreadsheetIdFromNullOrEmpty() {
        assertEquals("", SpreadsheetUtils.extractSpreadsheetId(null))
        assertEquals("", SpreadsheetUtils.extractSpreadsheetId(""))
        assertEquals("", SpreadsheetUtils.extractSpreadsheetId("   "))
    }

    @Test
    fun testParseCsvRosterTextSimpleCsv() {
        val csvContent = """John Doe,1001,john@example.com,555-1234,Good student
        Jane Smith,1002,jane@example.com,555-5678,Needs improvement"""
        val result = SpreadsheetUtils.parseCsvRosterText(csvContent)
        assertEquals(2, result.size)
        assertEquals("John Doe", result[0].name)
        assertEquals("1001", result[0].studentNumber)
        assertEquals("john@example.com", result[0].email)
        assertEquals("555-1234", result[0].guardianContact)
        assertEquals("Good student", result[0].notes)
        assertEquals("Jane Smith", result[1].name)
        assertEquals("1002", result[1].studentNumber)
        assertEquals("jane@example.com", result[1].email)
        assertEquals("555-5678", result[1].guardianContact)
        assertEquals("Needs improvement", result[1].notes)
    }

    @Test
    fun testParseCsvRosterTextWithHeader() {
        val csvContent = """Student Name,ID,Email,Phone,Notes
        John Doe,1001,john@example.com,555-1234,Good student
        Jane Smith,1002,jane@example.com,555-5678,Needs improvement"""
        val result = SpreadsheetUtils.parseCsvRosterText(csvContent)
        assertEquals(2, result.size)
        assertEquals("John Doe", result[0].name)
        assertEquals("1001", result[0].studentNumber)
        assertEquals("john@example.com", result[0].email)
        assertEquals("555-1234", result[0].guardianContact)
        assertEquals("Good student", result[0].notes)
        assertEquals("Jane Smith", result[1].name)
        assertEquals("1002", result[1].studentNumber)
        assertEquals("jane@example.com", result[1].email)
        assertEquals("555-5678", result[1].guardianContact)
        assertEquals("Needs improvement", result[1].notes)
    }

    @Test
    fun testParseCsvRosterTextTabSeparated() {
        val csvContent = """John Doe\t1001\tjohn@example.com\t555-1234\tGood student
        Jane Smith\t1002\tjane@example.com\t555-5678\tNeeds improvement"""
        val result = SpreadsheetUtils.parseCsvRosterText(csvContent)
        assertEquals(2, result.size)
        assertEquals("John Doe", result[0].name)
        assertEquals("1001", result[0].studentNumber)
        assertEquals("john@example.com", result[0].email)
        assertEquals("555-1234", result[0].guardianContact)
        assertEquals("Good student", result[0].notes)
        assertEquals("Jane Smith", result[1].name)
        assertEquals("1002", result[1].studentNumber)
        assertEquals("jane@example.com", result[1].email)
        assertEquals("555-5678", result[1].guardianContact)
        assertEquals("Needs improvement", result[1].notes)
    }

    @Test
    fun testParseCsvRosterTextSemicolonSeparated() {
        val csvContent = """John Doe;1001;john@example.com;555-1234;Good student
        Jane Smith;1002;jane@example.com;555-5678;Needs improvement"""
        val result = SpreadsheetUtils.parseCsvRosterText(csvContent)
        assertEquals(2, result.size)
        assertEquals("John Doe", result[0].name)
        assertEquals("1001", result[0].studentNumber)
        assertEquals("john@example.com", result[0].email)
        assertEquals("555-1234", result[0].guardianContact)
        assertEquals("Good student", result[0].notes)
        assertEquals("Jane Smith", result[1].name)
        assertEquals("1002", result[1].studentNumber)
        assertEquals("jane@example.com", result[1].email)
        assertEquals("555-5678", result[1].guardianContact)
        assertEquals("Needs improvement", result[1].notes)
    }

    @Test
    fun testParseCsvRosterTextEmptyAndBlankLines() {
        val csvContent = """John Doe,1001,john@example.com,555-1234,Good student

        Jane Smith,1002,jane@example.com,555-5678,Needs improvement"""
        val result = SpreadsheetUtils.parseCsvRosterText(csvContent)
        assertEquals(2, result.size)
        assertEquals("John Doe", result[0].name)
        assertEquals("Jane Smith", result[1].name)
    }

    @Test
    fun testParseCsvRosterTextMissingColumns() {
        val csvContent = """John Doe,1001,john@example.com
        Jane Smith,1002"""
        val result = SpreadsheetUtils.parseCsvRosterText(csvContent)
        assertEquals(2, result.size)
        assertEquals("John Doe", result[0].name)
        assertEquals("1001", result[0].studentNumber)
        assertEquals("john@example.com", result[0].email)
        assertEquals("", result[0].guardianContact)
        assertEquals("", result[0].notes)
        assertEquals("Jane Smith", result[1].name)
        assertEquals("1002", result[1].studentNumber)
        assertEquals("", result[1].email)
        assertEquals("", result[1].guardianContact)
        assertEquals("", result[1].notes)
    }

    @Test
    fun testParseCsvRosterTextQuotedFields() {
        val csvContent = """"John Doe, Jr.",1001,"john@example.com","555-1234","Good, student"
        "Jane Smith",1002,"jane@example.com","555-5678","Needs improvement""""
        val result = SpreadsheetUtils.parseCsvRosterText(csvContent)
        assertEquals(2, result.size)
        assertEquals("John Doe, Jr.", result[0].name)
        assertEquals("1001", result[0].studentNumber)
        assertEquals("john@example.com", result[0].email)
        assertEquals("555-1234", result[0].guardianContact)
        assertEquals("Good, student", result[0].notes)
        assertEquals("Jane Smith", result[1].name)
        assertEquals("1002", result[1].studentNumber)
        assertEquals("jane@example.com", result[1].email)
        assertEquals("555-5678", result[1].guardianContact)
        assertEquals("Needs improvement", result[1].notes)
    }

    @Test
    fun testParseCsvRosterTextEmptyInput() {
        val result = SpreadsheetUtils.parseCsvRosterText("")
        assertEquals(0, result.size)
        
        val result2 = SpreadsheetUtils.parseCsvRosterText("   \n\t\n  ")
        assertEquals(0, result2.size)
    }

    @Test
    fun testParseCsvRosterTextBlankLinesOnly() {
        val result = SpreadsheetUtils.parseCsvRosterText("\n\n\n")
        assertEquals(0, result.size)
    }
}
