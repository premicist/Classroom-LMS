package com.aistudio.classroomlms.domain.merit

import com.aistudio.classroomlms.data.entity.ExamEntity
import com.aistudio.classroomlms.data.entity.ExamMarkEntity
import com.aistudio.classroomlms.data.entity.StudentEntity
import com.aistudio.classroomlms.util.NebGradingEngine

data class StudentExamAggregate(
    val studentId: Long,
    val studentName: String,
    val studentNumber: String,
    val guardianContact: String,
    val totalMarksObtained: Double,
    val totalFullMarks: Double,
    val percentage: Double,
    val gpa: Double,
    val letterGrade: String,
    val gradeDescription: String,
    val rank: Int,
    val isPassed: Boolean,
    val subjectMarks: Map<Long, Double?>, // examId -> marksObtained
    val examCount: Int
)

data class SubjectTopper(
    val examId: Long,
    val examTitle: String,
    val studentId: Long,
    val studentName: String,
    val studentNumber: String,
    val marksObtained: Double,
    val fullMarks: Double,
    val percentage: Double
)

data class LeaderboardSummary(
    val rankedStudents: List<StudentExamAggregate> = emptyList(),
    val topThree: List<StudentExamAggregate> = emptyList(),
    val subjectToppers: List<SubjectTopper> = emptyList(),
    val classAveragePercentage: Double = 0.0,
    val overallPassRate: Double = 0.0
)

object MeritLeaderboardEngine {

    /**
     * Computes consolidated rankings and subject toppers for a list of exams and their marks.
     */
    fun computeLeaderboard(
        students: List<StudentEntity>,
        exams: List<ExamEntity>,
        marks: List<ExamMarkEntity>
    ): LeaderboardSummary {
        if (students.isEmpty() || exams.isEmpty()) {
            return LeaderboardSummary()
        }

        val examMap = exams.associateBy { it.id }
        val marksByExamAndStudent = marks.groupBy { it.examId to it.studentId }

        // Compute aggregate for each student
        val unrankedAggregates = students.map { student ->
            var marksObtainedSum = 0.0
            var fullMarksSum = 0.0
            var allPassed = true
            var examsAppeared = 0
            val subjectMarks = mutableMapOf<Long, Double?>()

            for (exam in exams) {
                fullMarksSum += exam.fullMarks
                val studentMark = marksByExamAndStudent[exam.id to student.id]?.firstOrNull()
                val score = studentMark?.marksObtained
                subjectMarks[exam.id] = score

                if (score != null) {
                    marksObtainedSum += score
                    examsAppeared++
                    if (score < exam.passMarks) {
                        allPassed = false
                    }
                } else {
                    // Unmarked or absent
                    allPassed = false
                }
            }

            val percentage = if (fullMarksSum > 0.0) {
                (marksObtainedSum / fullMarksSum) * 100.0
            } else 0.0

            val gpa = NebGradingEngine.calculateGpa(percentage)
            val letterGrade = NebGradingEngine.calculateLetterGrade(percentage)
            val description = NebGradingEngine.getGradeDescription(letterGrade)

            StudentExamAggregate(
                studentId = student.id,
                studentName = student.name,
                studentNumber = student.studentNumber,
                guardianContact = student.guardianContact,
                totalMarksObtained = marksObtainedSum,
                totalFullMarks = fullMarksSum,
                percentage = percentage,
                gpa = gpa,
                letterGrade = letterGrade,
                gradeDescription = description,
                rank = 0, // Assigned below
                isPassed = allPassed && examsAppeared == exams.size,
                subjectMarks = subjectMarks,
                examCount = exams.size
            )
        }

        // Sort descending by percentage, then by total marks obtained
        val sorted = unrankedAggregates.sortedWith(
            compareByDescending<StudentExamAggregate> { it.percentage }
                .thenByDescending { it.totalMarksObtained }
                .thenBy { it.studentName.lowercase() }
        )

        // Assign standard competition ranks (e.g., 1, 2, 2, 4 if there's a tie)
        val rankedList = mutableListOf<StudentExamAggregate>()
        var currentRank = 1
        for (i in sorted.indices) {
            if (i > 0) {
                val prev = sorted[i - 1]
                val curr = sorted[i]
                if (Math.abs(curr.percentage - prev.percentage) > 0.001) {
                    currentRank = i + 1
                }
            }
            rankedList.add(sorted[i].copy(rank = currentRank))
        }

        // Extract Top 3 podium (taking students whose rank <= 3)
        val topThree = rankedList.filter { it.rank <= 3 && it.totalMarksObtained > 0.0 }

        // Compute Subject Toppers
        val studentMap = students.associateBy { it.id }
        val subjectToppers = mutableListOf<SubjectTopper>()
        for (exam in exams) {
            val examMarks = marks.filter { it.examId == exam.id && it.marksObtained != null }
            val highestMark = examMarks.maxByOrNull { it.marksObtained ?: 0.0 }
            if (highestMark != null && (highestMark.marksObtained ?: 0.0) > 0.0) {
                val student = studentMap[highestMark.studentId]
                if (student != null) {
                    val score = highestMark.marksObtained ?: 0.0
                    val pct = if (exam.fullMarks > 0) (score / exam.fullMarks) * 100.0 else 0.0
                    subjectToppers.add(
                        SubjectTopper(
                            examId = exam.id,
                            examTitle = exam.title,
                            studentId = student.id,
                            studentName = student.name,
                            studentNumber = student.studentNumber,
                            marksObtained = score,
                            fullMarks = exam.fullMarks,
                            percentage = pct
                        )
                    )
                }
            }
        }

        val classAvg = if (rankedList.isNotEmpty()) {
            rankedList.map { it.percentage }.average()
        } else 0.0

        val passRate = if (rankedList.isNotEmpty()) {
            (rankedList.count { it.isPassed }.toDouble() / rankedList.size) * 100.0
        } else 0.0

        return LeaderboardSummary(
            rankedStudents = rankedList,
            topThree = topThree,
            subjectToppers = subjectToppers,
            classAveragePercentage = classAvg,
            overallPassRate = passRate
        )
    }
}
