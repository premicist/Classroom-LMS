package com.example.util

object SpreadsheetUtils {
    /**
     * Extracts the Google Spreadsheet ID from either a full Google Sheets URL
     * (e.g. "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit#gid=0")
     * or a raw spreadsheet ID.
     */
    fun extractSpreadsheetId(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val trimmed = input.trim()
        if (trimmed.contains("/d/")) {
            return trimmed.substringAfter("/d/").substringBefore("/").substringBefore("?").substringBefore("#")
        }
        return trimmed
    }
}
