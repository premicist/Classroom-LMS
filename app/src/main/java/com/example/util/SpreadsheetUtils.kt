package com.example.util

data class ParsedCsvStudent(
    val name: String,
    val studentNumber: String = "",
    val email: String = "",
    val guardianContact: String = "",
    val notes: String = ""
)

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

    /**
     * Parses raw CSV or TSV string into a list of [ParsedCsvStudent] records.
     * Supports commas, semicolons, and tabs, with header detection.
     */
    fun parseCsvRosterText(csvContent: String): List<ParsedCsvStudent> {
        if (csvContent.isBlank()) return emptyList()

        val rawLines = csvContent.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (rawLines.isEmpty()) return emptyList()

        // Auto-detect delimiter from first line: comma, tab, or semicolon
        val firstLine = rawLines.first()
        val delimiter = when {
            firstLine.count { it == '\t' } >= 1 -> '\t'
            firstLine.count { it == ';' } > firstLine.count { it == ',' } -> ';'
            else -> ','
        }

        val rows = rawLines.map { parseCsvLine(it, delimiter) }
        if (rows.isEmpty()) return emptyList()

        // Detect if first row is a header
        val headerRow = rows.first().map { it.trim().lowercase() }
        val hasNameHeader = headerRow.any { it.contains("name") }

        val nameIdx: Int
        val idIdx: Int
        val emailIdx: Int
        val phoneIdx: Int
        val notesIdx: Int
        val startIndex: Int

        if (hasNameHeader) {
            nameIdx = headerRow.indexOfFirst { it.contains("name") }
            idIdx = headerRow.indexOfFirst { it.contains("id") || it.contains("number") || it.contains("roll") }
            emailIdx = headerRow.indexOfFirst { it.contains("email") || it.contains("mail") }
            phoneIdx = headerRow.indexOfFirst { it.contains("phone") || it.contains("contact") || it.contains("guardian") || it.contains("mobile") }
            notesIdx = headerRow.indexOfFirst { it.contains("note") || it.contains("remark") }
            startIndex = 1
        } else {
            // Default column assumptions: Col 0 = Name or ID, Col 1 = Name/ID, Col 2 = Email
            nameIdx = 0
            idIdx = if (rows.first().size > 1) 1 else -1
            emailIdx = if (rows.first().size > 2) 2 else -1
            phoneIdx = if (rows.first().size > 3) 3 else -1
            notesIdx = if (rows.first().size > 4) 4 else -1
            startIndex = 0
        }

        val result = mutableListOf<ParsedCsvStudent>()
        for (i in startIndex until rows.size) {
            val row = rows[i]
            if (row.isEmpty()) continue

            val name = if (nameIdx in row.indices) row[nameIdx].trim() else ""
            if (name.isBlank()) continue

            val studentNumber = if (idIdx in row.indices) row[idIdx].trim() else "STU-${1000 + result.size + 1}"
            val email = if (emailIdx in row.indices) row[emailIdx].trim() else ""
            val phone = if (phoneIdx in row.indices) row[phoneIdx].trim() else ""
            val notes = if (notesIdx in row.indices) row[notesIdx].trim() else ""

            result.add(
                ParsedCsvStudent(
                    name = name,
                    studentNumber = studentNumber.ifBlank { "STU-${1000 + result.size + 1}" },
                    email = email,
                    guardianContact = phone,
                    notes = notes
                )
            )
        }

        return result
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val tokens = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var insideQuotes = false

        for (char in line) {
            when {
                char == '"' -> insideQuotes = !insideQuotes
                char == delimiter && !insideQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(char)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }
}
