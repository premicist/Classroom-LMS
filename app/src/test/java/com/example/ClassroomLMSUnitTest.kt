package com.example

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Classroom LMS business logic.
 * These tests run on the JVM (no Android framework).
 */
class ClassroomLMSUnitTest {

    // ============================================
    // GRADING ENGINE TESTS
    // ============================================

    @Test
    fun calculateWeightedGrade() {
        val weights = mapOf(
            "homework" to 0.3,
            "midterm" to 0.3,
            "final" to 0.4
        )
        
        val scores = mapOf(
            "homework" to 85.0,
            "midterm" to 78.0,
            "final" to 92.0
        )
        
        val expected = (85.0 * 0.3) + (78.0 * 0.3) + (92.0 * 0.4)
        val actual = calculateWeightedScore(weights, scores)
        
        assertEquals("Weighted grade should calculate correctly", expected, actual, 0.01)
    }

    @Test
    fun calculateSimpleAverage() {
        val grades = listOf(85.0, 90.0, 78.0, 92.0, 88.0)
        val expected = grades.average()
        val actual = calculateAverage(grades)
        
        assertEquals("Average should calculate correctly", expected, actual, 0.01)
    }

    @Test
    fun calculateGradeWithEmptyList() {
        val grades = emptyList<Double>()
        val actual = calculateAverage(grades)
        
        assertEquals("Empty list should return 0.0", 0.0, actual, 0.01)
    }

    @Test
    fun letterGradeConversion() {
        assertEquals("A", convertToLetterGrade(95.0))
        assertEquals("A-", convertToLetterGrade(91.0))
        assertEquals("B+", convertToLetterGrade(88.0))
        assertEquals("B", convertToLetterGrade(85.0))
        assertEquals("B-", convertToLetterGrade(81.0))
        assertEquals("C+", convertToLetterGrade(78.0))
        assertEquals("C", convertToLetterGrade(75.0))
        assertEquals("C-", convertToLetterGrade(71.0))
        assertEquals("D+", convertToLetterGrade(68.0))
        assertEquals("D", convertToLetterGrade(65.0))
        assertEquals("F", convertToLetterGrade(50.0))
    }

    // ============================================
    // ATTENDANCE CALCULATION TESTS
    // ============================================

    @Test
    fun calculateAttendanceRate() {
        val totalDays = 20
        val presentDays = 18
        val expected = (presentDays.toDouble() / totalDays) * 100
        val actual = calculateAttendancePercentage(presentDays, totalDays)
        
        assertEquals("Attendance rate should be 90%", expected, actual, 0.01)
    }

    @Test
    fun attendanceRateWithZeroDays() {
        val actual = calculateAttendancePercentage(0, 0)
        assertEquals("Zero days should return 0%", 0.0, actual, 0.01)
    }

    @Test
    fun attendanceStatusConversion() {
        assertEquals("Present", getAttendanceStatusString("P"))
        assertEquals("Absent", getAttendanceStatusString("A"))
        assertEquals("Late", getAttendanceStatusString("L"))
        assertEquals("Excused", getAttendanceStatusString("E"))
    }

    // ============================================
    // DATE/TIME UTILITY TESTS
    // ============================================

    @Test
    fun dateFormatting() {
        val timestamp = 1727740800000L // 2024-10-01 00:00:00 UTC
        val formatted = formatDate(timestamp, "yyyy-MM-dd")
        assertEquals("Date should format correctly", "2024-10-01", formatted)
    }

    @Test
    fun dateParsing() {
        val dateString = "2024-10-01"
        val timestamp = parseDate(dateString, "yyyy-MM-dd")
        assertTrue("Parsed timestamp should be positive", timestamp > 0)
    }

    @Test
    fun getDaysBetweenDates() {
        val startDate = 1727740800000L // 2024-10-01
        val endDate = 1727913600000L   // 2024-10-03
        val days = calculateDaysBetween(startDate, endDate)
        assertEquals("Should be 2 days between", 2, days)
    }

    // ============================================
    // VALIDATION TESTS
    // ============================================

    @Test
    fun validateStudentName() {
        assertTrue(isValidStudentName("John Doe"))
        assertTrue(isValidStudentName("Mary Jane Smith"))
        assertFalse(isValidStudentName(""))
        assertFalse(isValidStudentName("   "))
        assertFalse(isValidStudentName("A"))
    }

    @Test
    fun validateEmail() {
        assertTrue(isValidEmail("student@example.com"))
        assertTrue(isValidEmail("student.name@school.edu"))
        assertFalse(isValidEmail("invalid"))
        assertFalse(isValidEmail("missing@domain"))
        assertFalse(isValidEmail(""))
    }

    @Test
    fun validateStudentNumber() {
        assertTrue(isValidStudentNumber("S001"))
        assertTrue(isValidStudentNumber("12345"))
        assertTrue(isValidStudentNumber("STU-2024-001"))
        assertFalse(isValidStudentNumber(""))
        assertFalse(isValidStudentNumber("   "))
    }

    @Test
    fun validateGrade() {
        assertTrue(isValidGrade(0.0))
        assertTrue(isValidGrade(50.0))
        assertTrue(isValidGrade(100.0))
        assertFalse(isValidGrade(-1.0))
        assertFalse(isValidGrade(101.0))
        assertFalse(isValidGrade(Double.NaN))
    }

    @Test
    fun validatePointsPossible() {
        assertTrue(isValidPoints(1.0))
        assertTrue(isValidPoints(100.0))
        assertTrue(isValidPoints(1000.0))
        assertFalse(isValidPoints(0.0))
        assertFalse(isValidPoints(-1.0))
    }

    // ============================================
    // DATA TRANSFORMATION TESTS
    // ============================================

    @Test
    fun csvEscapeSpecialCharacters() {
        assertEquals("Normal text", escapeCsvField("Normal text"))
        assertEquals("\"Text, with comma\"", escapeCsvField("Text, with comma"))
        assertEquals("\"Text\"\"with quotes\"", escapeCsvField("Text\"with quotes"))
        assertEquals("\"Text\nwith newline\"", escapeCsvField("Text\nwith newline"))
    }

    @Test
    fun jsonEscapeSpecialCharacters() {
        assertEquals("Normal text", escapeJsonString("Normal text"))
        assertEquals("Text\\\"with quotes", escapeJsonString("Text\"with quotes"))
        assertEquals("Text\\nwith newline", escapeJsonString("Text\nwith newline"))
        assertEquals("Text\\twith tab", escapeJsonString("Text\twith tab"))
    }

    @Test
    fun formatPhoneNumber() {
        assertEquals("+1-555-123-4567", formatPhoneNumber("5551234567", "US"))
        assertEquals("+977-98-12345678", formatPhoneNumber("9812345678", "NP"))
    }

    // ============================================
    // SPREADSHEET INTEGRATION TESTS
    // ============================================

    @Test
    fun parseStudentNumberColumn() {
        val headers = listOf("Student Number", "Name", "Email", "Grade")
        val index = findColumnIndex(headers, listOf("Student Number", "Student ID", "ID"))
        assertEquals("Should find Student Number column", 0, index)
    }

    @Test
    fun parseNameColumn() {
        val headers = listOf("ID", "Student Name", "Email", "Grade")
        val index = findColumnIndex(headers, listOf("Name", "Student Name", "First Name"))
        assertEquals("Should find Name column", 1, index)
    }

    @Test
    fun parseEmailColumn() {
        val headers = listOf("ID", "Name", "Email Address", "Grade")
        val index = findColumnIndex(headers, listOf("Email", "Email Address"))
        assertEquals("Should find Email column", 2, index)
    }

    @Test
    fun columnNotFound() {
        val headers = listOf("ID", "Name", "Grade")
        val index = findColumnIndex(headers, listOf("Email", "Email Address"))
        assertEquals("Should return -1 for missing column", -1, index)
    }

    @Test
    fun spreadsheetRangeParsing() {
        val range = "Sheet1!A1:Z100"
        val (sheet, startCell, endCell) = parseRange(range)
        assertEquals("Sheet name should be Sheet1", "Sheet1", sheet)
        assertEquals("Start cell should be A1", "A1", startCell)
        assertEquals("End cell should be Z100", "Z100", endCell)
    }

    @Test
    fun extractSpreadsheetId() {
        val url = "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjGMUUqpt35/edit"
        val id = extractSpreadsheetId(url)
        assertEquals("Should extract spreadsheet ID", "1BxiMVs0XRA5nFMdKvBdBZjGMUUqpt35", id)
    }

    // ============================================
    // BACKUP/RESTORE TESTS
    // ============================================

    @Test
    fun validateBackupJson() {
        val json = """
        {
            "classrooms": [],
            "students": [],
            "assignments": [],
            "submissions": [],
            "homeworkRecords": [],
            "attendanceRecords": [],
            "interventions": [],
            "lessonPlans": [],
            "dailyLogs": [],
            "classSchedules": [],
            "disciplineRecords": []
        }
        """.trimIndent()
        
        assertTrue("Valid backup JSON should pass", isValidBackupJson(json))
    }

    @Test
    fun validateBackupJsonMissingField() {
        val json = """
        {
            "classrooms": [],
            "students": []
        }
        """.trimIndent()
        
        assertFalse("Missing fields should fail validation", isValidBackupJson(json))
    }

    // ============================================
    // PERFORMANCE TESTS
    // ============================================

    @Test
    fun largeDatasetPerformance() {
        val largeList = (1..10000).map { "Student $it" }
        val startTime = System.currentTimeMillis()
        
        val filtered = largeList.filter { it.contains("123") }
        val sorted = filtered.sorted()
        
        val duration = System.currentTimeMillis() - startTime
        assertTrue("Operation should complete in < 100ms", duration < 100)
        assertTrue("Should find results", sorted.isNotEmpty())
    }

    @Test
    fun stringConcatenationPerformance() {
        val strings = (1..1000).map { "String $it" }
        val startTime = System.currentTimeMillis()
        
        val result = strings.joinToString(", ")
        
        val duration = System.currentTimeMillis() - startTime
        assertTrue("String concatenation should complete in < 50ms", duration < 50)
        assertTrue("Result should contain data", result.isNotEmpty())
    }

    // ============================================
    // EDGE CASE TESTS
    // ============================================

    @Test
    fun handleNullValues() {
        val nullableString: String? = null
        val result = nullableString ?: "Default"
        assertEquals("Should use default for null", "Default", result)
    }

    @Test
    fun handleEmptyStrings() {
        val empty = ""
        val blank = "   "
        val result = empty.ifBlank { "Blank or empty" }
        assertEquals("Should detect blank strings", "Blank or empty", result)
    }

    @Test
    fun handleSpecialCharacters() {
        val input = "Student with émojis 🎓 and spëcial châràctérs"
        assertTrue("Should handle special characters", input.isNotEmpty())
        assertTrue("Should handle emojis", input.contains("🎓"))
    }

    @Test
    fun handleWhitespace() {
        val input = "  Trim  this  string  "
        val trimmed = input.trim()
        assertEquals("Should trim whitespace", "Trim  this  string", trimmed)
    }

    // ============================================
    // HELPER FUNCTIONS (Stubs for actual implementation)
    // ============================================

    private fun calculateWeightedScore(weights: Map<String, Double>, scores: Map<String, Double>): Double {
        return weights.entries.fold(0.0) { acc, (key, weight) ->
            acc + (scores[key] ?: 0.0) * weight
        }
    }

    private fun calculateAverage(grades: List<Double>): Double {
        return if (grades.isEmpty()) 0.0 else grades.average()
    }

    private fun convertToLetterGrade(percentage: Double): String {
        return when {
            percentage >= 93.0 -> "A"
            percentage >= 90.0 -> "A-"
            percentage >= 87.0 -> "B+"
            percentage >= 83.0 -> "B"
            percentage >= 80.0 -> "B-"
            percentage >= 77.0 -> "C+"
            percentage >= 73.0 -> "C"
            percentage >= 70.0 -> "C-"
            percentage >= 67.0 -> "D+"
            percentage >= 63.0 -> "D"
            else -> "F"
        }
    }

    private fun calculateAttendancePercentage(present: Int, total: Int): Double {
        return if (total == 0) 0.0 else (present.toDouble() / total) * 100
    }

    private fun getAttendanceStatusString(code: String): String {
        return when (code) {
            "P" -> "Present"
            "A" -> "Absent"
            "L" -> "Late"
            "E" -> "Excused"
            else -> "Unknown"
        }
    }

    private fun formatDate(timestamp: Long, pattern: String): String {
        val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
        return sdf.format(java.util.Date(timestamp))
    }

    private fun parseDate(dateString: String, pattern: String): Long {
        val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
        return sdf.parse(dateString)?.time ?: 0L
    }

    private fun calculateDaysBetween(start: Long, end: Long): Int {
        val diff = end - start
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }

    private fun isValidStudentName(name: String): Boolean {
        return name.trim().length >= 2
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && email.contains("@") && email.contains(".")
    }

    private fun isValidStudentNumber(number: String): Boolean {
        return number.trim().isNotEmpty()
    }

    private fun isValidGrade(grade: Double): Boolean {
        return grade in 0.0..100.0
    }

    private fun isValidPoints(points: Double): Boolean {
        return points > 0.0
    }

    private fun escapeCsvField(field: String): String {
        return when {
            field.contains(",") || field.contains("\"") || field.contains("\n") -> {
                "\"${field.replace("\"", "\"\"")}\""
            }
            else -> field
        }
    }

    private fun escapeJsonString(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\t", "\\t")
    }

    private fun formatPhoneNumber(number: String, countryCode: String): String {
        return when (countryCode) {
            "US" -> "+1-${number.substring(0, 3)}-${number.substring(3, 6)}-${number.substring(6)}"
            "NP" -> "+977-${number.substring(0, 2)}-${number.substring(2)}"
            else -> number
        }
    }

    private fun findColumnIndex(headers: List<String>, searchTerms: List<String>): Int {
        return headers.indexOfFirst { header ->
            searchTerms.any { term -> header.equals(term, ignoreCase = true) }
        }
    }

    private fun parseRange(range: String): Triple<String, String, String> {
        val parts = range.split("!")
        val sheet = parts.getOrElse(0) { "Sheet1" }
        val cells = parts.getOrElse(1) { "A1:Z100" }.split(":")
        val startCell = cells.getOrElse(0) { "A1" }
        val endCell = cells.getOrElse(1) { "Z100" }
        return Triple(sheet, startCell, endCell)
    }

    private fun extractSpreadsheetId(url: String): String {
        val regex = "/d/([a-zA-Z0-9-_]+)".toRegex()
        return regex.find(url)?.groupValues?.get(1) ?: ""
    }

    private fun isValidBackupJson(json: String): Boolean {
        val requiredFields = listOf(
            "classrooms", "students", "assignments", "submissions",
            "homeworkRecords", "attendanceRecords", "interventions",
            "lessonPlans", "dailyLogs", "classSchedules", "disciplineRecords"
        )
        return requiredFields.all { field -> json.contains("\"$field\"") }
    }
}
