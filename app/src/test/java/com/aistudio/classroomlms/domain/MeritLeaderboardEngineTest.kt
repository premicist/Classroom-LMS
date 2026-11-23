package com.aistudio.classroomlms.domain

import com.aistudio.classroomlms.data.entity.ExamCategory
import com.aistudio.classroomlms.data.entity.ExamEntity
import com.aistudio.classroomlms.data.entity.ExamMarkEntity
import com.aistudio.classroomlms.data.entity.StudentEntity
import com.aistudio.classroomlms.domain.cas.CasEvaluationEngine
import com.aistudio.classroomlms.domain.merit.MeritLeaderboardEngine
import com.aistudio.classroomlms.util.ParentCommunicationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeritLeaderboardEngineTest {

    private val classroomId = 1L

    private val student1 = StudentEntity(id = 1L, classroomId = classroomId, name = "Aayush Sharma", studentNumber = "01", guardianContact = "+9779841234567")
    private val student2 = StudentEntity(id = 2L, classroomId = classroomId, name = "Bishal Thapa", studentNumber = "02", guardianContact = "9847111222")
    private val student3 = StudentEntity(id = 3L, classroomId = classroomId, name = "Chahana Rai", studentNumber = "03", guardianContact = "9800000000")

    private val mathExam = ExamEntity(id = 10L, classroomId = classroomId, category = ExamCategory.TERMINAL, title = "Compulsory Mathematics", fullMarks = 100.0, passMarks = 35.0, date = "2026-11-20")
    private val scienceExam = ExamEntity(id = 20L, classroomId = classroomId, category = ExamCategory.TERMINAL, title = "Science & Tech", fullMarks = 100.0, passMarks = 35.0, date = "2026-11-22")

    @Test
    fun testLeaderboardRankingAndTopThree() {
        val marks = listOf(
            // Student 1: Math 95, Science 90 -> Total 185 (92.5%) -> Rank 1 (A+)
            ExamMarkEntity(id = 1L, examId = 10L, studentId = 1L, marksObtained = 95.0),
            ExamMarkEntity(id = 2L, examId = 20L, studentId = 1L, marksObtained = 90.0),
            // Student 2: Math 80, Science 85 -> Total 165 (82.5%) -> Rank 2 (A)
            ExamMarkEntity(id = 3L, examId = 10L, studentId = 2L, marksObtained = 80.0),
            ExamMarkEntity(id = 4L, examId = 20L, studentId = 2L, marksObtained = 85.0),
            // Student 3: Math 70, Science 60 -> Total 130 (65.0%) -> Rank 3 (B)
            ExamMarkEntity(id = 5L, examId = 10L, studentId = 3L, marksObtained = 70.0),
            ExamMarkEntity(id = 6L, examId = 20L, studentId = 3L, marksObtained = 60.0)
        )

        val summary = MeritLeaderboardEngine.computeLeaderboard(
            students = listOf(student1, student2, student3),
            exams = listOf(mathExam, scienceExam),
            marks = marks
        )

        assertEquals(3, summary.rankedStudents.size)
        assertEquals(3, summary.topThree.size)

        // Rank 1
        val first = summary.rankedStudents[0]
        assertEquals("Aayush Sharma", first.studentName)
        assertEquals(1, first.rank)
        assertEquals(185.0, first.totalMarksObtained, 0.001)
        assertEquals(92.5, first.percentage, 0.001)
        assertEquals("A+", first.letterGrade)
        assertEquals(4.0, first.gpa, 0.001)
        assertTrue(first.isPassed)

        // Rank 2
        val second = summary.rankedStudents[1]
        assertEquals("Bishal Thapa", second.studentName)
        assertEquals(2, second.rank)
        assertEquals(82.5, second.percentage, 0.001)
        assertEquals("A", second.letterGrade)

        // Rank 3
        val third = summary.rankedStudents[2]
        assertEquals("Chahana Rai", third.studentName)
        assertEquals(3, third.rank)
        assertEquals(65.0, third.percentage, 0.001)
        assertEquals("B", third.letterGrade)

        // Subject Toppers
        assertEquals(2, summary.subjectToppers.size)
        val mathTopper = summary.subjectToppers.first { it.examId == 10L }
        assertEquals("Aayush Sharma", mathTopper.studentName)
        assertEquals(95.0, mathTopper.marksObtained, 0.001)

        val scienceTopper = summary.subjectToppers.first { it.examId == 20L }
        assertEquals("Aayush Sharma", scienceTopper.studentName)
        assertEquals(90.0, scienceTopper.marksObtained, 0.001)
    }

    @Test
    fun testTieHandlingInRanks() {
        val marks = listOf(
            // Student 1 & 2 have identical scores
            ExamMarkEntity(id = 1L, examId = 10L, studentId = 1L, marksObtained = 80.0),
            ExamMarkEntity(id = 2L, examId = 10L, studentId = 2L, marksObtained = 80.0),
            // Student 3 has lower score
            ExamMarkEntity(id = 3L, examId = 10L, studentId = 3L, marksObtained = 60.0)
        )

        val summary = MeritLeaderboardEngine.computeLeaderboard(
            students = listOf(student1, student2, student3),
            exams = listOf(mathExam),
            marks = marks
        )

        // Tied students both get Rank 1
        assertEquals(1, summary.rankedStudents[0].rank)
        assertEquals(1, summary.rankedStudents[1].rank)
        // Next student gets Rank 3 (standard competition ranking: 1, 1, 3)
        assertEquals(3, summary.rankedStudents[2].rank)
    }

    @Test
    fun testCasEvaluationScaling() {
        // Attendance 92% -> 4.0 points
        assertEquals(4.0, CasEvaluationEngine.calculateAttendancePoints(92.0), 0.001)
        // Attendance 85% -> 3.0 points
        assertEquals(3.0, CasEvaluationEngine.calculateAttendancePoints(85.0), 0.001)
        // Attendance 72% -> 2.0 points
        assertEquals(2.0, CasEvaluationEngine.calculateAttendancePoints(72.0), 0.001)
        // Attendance 65% -> 1.0 point
        assertEquals(1.0, CasEvaluationEngine.calculateAttendancePoints(65.0), 0.001)
        // Attendance 50% -> 0.0 points
        assertEquals(0.0, CasEvaluationEngine.calculateAttendancePoints(50.0), 0.001)

        val casComponents = CasEvaluationEngine.synthesizeCasScore(
            attendancePct = 95.0,
            participationScore = 3.5,
            projectScore = 11.0,
            testScore = 4.5
        )
        // 4.0 + 3.5 + 11.0 + 4.5 = 23.0 out of 25.0
        assertEquals(23.0, casComponents.totalCasScore, 0.001)
        assertEquals(92.0, casComponents.percentage, 0.001)

        val finalScore = CasEvaluationEngine.calculateCompositeScore(
            theoryMark75 = 68.0,
            casMark25 = 23.0
        )
        assertEquals(91.0, finalScore, 0.001)
    }

    @Test
    fun testParentCommunicationFormatting() {
        val message = ParentCommunicationHelper.formatExamReportMessage(
            studentName = "Aayush Sharma",
            studentRoll = "01",
            examCategory = "First Terminal Examination",
            totalMarks = 185.0,
            fullMarks = 200.0,
            percentage = 92.5,
            gpa = 4.0,
            letterGrade = "A+",
            rank = 1,
            classroomName = "Grade 10 - Sagarmatha"
        )

        assertTrue(message.contains("Aayush Sharma"))
        assertTrue(message.contains("Grade 10 - Sagarmatha"))
        assertTrue(message.contains("Position #1"))
        assertTrue(message.contains("92.5%"))
        assertTrue(message.contains("4.00"))

        val cleanPhone = ParentCommunicationHelper.sanitizePhoneNumber("+977 984-1234567")
        assertEquals("+9779841234567", cleanPhone)
    }
}
