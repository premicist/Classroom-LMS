package com.example

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
}
