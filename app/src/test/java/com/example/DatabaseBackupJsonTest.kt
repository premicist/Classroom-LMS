package com.example

import com.example.data.entity.AssignmentEntity
import com.example.data.entity.AssignmentType
import com.example.data.entity.BehaviorCategory
import com.example.data.entity.BehaviorSeverity
import com.example.data.entity.ClassroomEntity
import com.example.data.entity.DisciplineRecordEntity
import com.example.data.entity.ExamCategory
import com.example.data.entity.ExamEntity
import com.example.data.entity.ExamMarkEntity
import com.example.data.entity.LiveAssessmentEntity
import com.example.data.entity.LiveAssessmentTaskType
import com.example.data.entity.MasteryLevel
import com.example.data.entity.StudentEntity
import com.example.data.repository.DatabaseBackupData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DatabaseBackupJsonTest {

    @Test
    fun testBackupSerializationAndDeserialization() {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(DatabaseBackupData::class.java)

        val classroom = ClassroomEntity(
            id = 1L,
            name = "AP Calculus BC",
            subject = "Mathematics",
            gradeLevel = "Grade 12",
            roomNumber = "301",
            scheduleInfo = "Mon-Fri 09:00 AM"
        )

        val student = StudentEntity(
            id = 101L,
            classroomId = 1L,
            name = "Aarav Sharma",
            studentNumber = "STU-101",
            email = "aarav@example.com"
        )

        val assignment = AssignmentEntity(
            id = 201L,
            classroomId = 1L,
            title = "Midterm Calculus Assessment",
            type = AssignmentType.EXAM,
            maxPoints = 100.0,
            dueDate = "2025-05-15"
        )

        val discipline = DisciplineRecordEntity(
            id = 301L,
            studentId = 101L,
            classroomId = 1L,
            date = "2025-05-10",
            category = BehaviorCategory.PRAISE_MERIT,
            severity = BehaviorSeverity.LOW_WARNING,
            title = "Outstanding Peer Mentoring",
            description = "Helped classmates solve differential equations during lab.",
            actionTaken = "Merit Certificate Issued",
            parentNotified = true,
            resolved = true
        )

        val liveAssessment = LiveAssessmentEntity(
            id = 401L,
            studentId = 101L,
            classroomId = 1L,
            date = "2025-05-12",
            timestamp = System.currentTimeMillis(),
            taskType = LiveAssessmentTaskType.QUICK_QUIZ_QA,
            topic = "Limits & Continuity",
            masteryLevel = MasteryLevel.MASTERED,
            diagnosticTags = "Strong intuition",
            remarks = "Answered all concept checks accurately"
        )

        val exam = ExamEntity(
            id = 501L,
            classroomId = 1L,
            category = ExamCategory.TERMINAL,
            title = "Term 1 Final Exam",
            topicOrChapter = "Units 1-5",
            fullMarks = 100.0,
            passMarks = 40.0,
            date = "2025-05-20"
        )

        val examMark = ExamMarkEntity(
            id = 601L,
            examId = 501L,
            studentId = 101L,
            marksObtained = 88.5,
            remarks = "Strong performance"
        )

        val originalData = DatabaseBackupData(
            classrooms = listOf(classroom),
            students = listOf(student),
            assignments = listOf(assignment),
            submissions = emptyList(),
            homeworkRecords = emptyList(),
            attendanceRecords = emptyList(),
            interventions = emptyList(),
            lessonPlans = emptyList(),
            dailyLogs = emptyList(),
            classSchedules = emptyList(),
            disciplineRecords = listOf(discipline),
            liveAssessments = listOf(liveAssessment),
            exams = listOf(exam),
            examMarks = listOf(examMark)
        )

        // 1. Serialize to JSON
        val json = adapter.toJson(originalData)
        assertNotNull(json)

        // 2. Deserialize back from JSON
        val deserialized = adapter.fromJson(json)
        assertNotNull(deserialized)

        // 3. Assert equality
        assertEquals(1, deserialized!!.classrooms.size)
        assertEquals("AP Calculus BC", deserialized.classrooms[0].name)

        assertEquals(1, deserialized.students.size)
        assertEquals("Aarav Sharma", deserialized.students[0].name)
        assertEquals("STU-101", deserialized.students[0].studentNumber)

        assertEquals(1, deserialized.assignments.size)
        assertEquals("Midterm Calculus Assessment", deserialized.assignments[0].title)

        assertEquals(1, deserialized.disciplineRecords.size)
        assertEquals(BehaviorCategory.PRAISE_MERIT, deserialized.disciplineRecords[0].category)
        assertEquals("Outstanding Peer Mentoring", deserialized.disciplineRecords[0].title)
        assertEquals(true, deserialized.disciplineRecords[0].parentNotified)

        assertEquals(1, deserialized.liveAssessments.size)
        assertEquals(LiveAssessmentTaskType.QUICK_QUIZ_QA, deserialized.liveAssessments[0].taskType)
        assertEquals(MasteryLevel.MASTERED, deserialized.liveAssessments[0].masteryLevel)

        assertEquals(1, deserialized.exams.size)
        assertEquals("Term 1 Final Exam", deserialized.exams[0].title)
        assertEquals(100.0, deserialized.exams[0].fullMarks, 0.001)

        assertEquals(1, deserialized.examMarks.size)
        assertEquals(88.5, deserialized.examMarks[0].marksObtained ?: 0.0, 0.001)
    }
}
