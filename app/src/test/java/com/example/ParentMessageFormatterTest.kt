package com.example

import com.example.data.entity.BehaviorCategory
import com.example.data.entity.BehaviorSeverity
import com.example.data.entity.DisciplineRecordEntity
import com.example.data.entity.StudentEntity
import com.example.ui.viewmodel.StudentGradeSummary
import com.example.ui.viewmodel.StudentTier
import com.example.util.ParentMessageFormatter
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentMessageFormatterTest {

    @Test
    fun testAcademicProgressMessageFormatting() {
        val student = StudentEntity(
            id = 1L,
            classroomId = 10L,
            name = "Aarav Sharma",
            studentNumber = "STU-101",
            guardianContact = "+9779801234567"
        )

        val summary = StudentGradeSummary(
            student = student,
            earnedPoints = 90.0,
            possiblePoints = 100.0,
            percentage = 90.0,
            letterGrade = "A+",
            gpa = 4.0,
            submissions = emptyList(),
            gradedCount = 1,
            missingCount = 0,
            pendingCount = 0,
            homeworkCompletionRate = 100.0,
            attendanceRate = 95.0,
            isAtRisk = false,
            isEnrichmentCandidate = true,
            riskReasons = emptyList(),
            enrichmentReasons = listOf("High Academic Mastery"),
            tier = StudentTier.ENRICHMENT_NEEDED
        )

        val message = ParentMessageFormatter.formatAcademicProgressMessage(
            student = student,
            summary = summary,
            classroomName = "Grade 10 Mathematics"
        )

        assertTrue(message.contains("Aarav Sharma"))
        assertTrue(message.contains("Grade 10 Mathematics"))
        assertTrue(message.contains("90.0% (A+)"))
        assertTrue(message.contains("GPA: *4.0 / 4.0*"))
        assertTrue(message.contains("Attendance Rate: *95%*"))
    }

    @Test
    fun testAttendanceAlertMessageFormatting() {
        val student = StudentEntity(
            id = 2L,
            classroomId = 10L,
            name = "Suman Thapa",
            studentNumber = "STU-102"
        )

        val message = ParentMessageFormatter.formatAttendanceAlertMessage(
            student = student,
            classroomName = "Physics Lab",
            status = "Absent",
            date = "2025-05-10"
        )

        assertTrue(message.contains("Suman Thapa"))
        assertTrue(message.contains("ABSENT"))
        assertTrue(message.contains("2025-05-10"))
    }

    @Test
    fun testDisciplineNotificationMessageFormatting() {
        val student = StudentEntity(
            id = 3L,
            classroomId = 10L,
            name = "Bina Rai",
            studentNumber = "STU-103"
        )

        val record = DisciplineRecordEntity(
            id = 50L,
            studentId = 3L,
            classroomId = 10L,
            date = "2025-05-12",
            category = BehaviorCategory.CLASSROOM_DISRUPTION,
            severity = BehaviorSeverity.LOW_WARNING,
            title = "Talking during instruction",
            description = "Repeatedly talking after verbal warning",
            actionTaken = "Moved to front row"
        )

        val message = ParentMessageFormatter.formatDisciplineNotificationMessage(
            student = student,
            record = record,
            classroomName = "Grade 10 Mathematics"
        )

        assertTrue(message.contains("Bina Rai"))
        assertTrue(message.contains("Classroom Disruption"))
        assertTrue(message.contains("Repeatedly talking after verbal warning"))
        assertTrue(message.contains("Moved to front row"))
    }
}
