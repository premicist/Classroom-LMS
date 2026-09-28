package com.example.util

import com.example.data.entity.DisciplineRecordEntity
import com.example.data.entity.StudentEntity
import com.example.ui.viewmodel.StudentGradeSummary

object ParentMessageFormatter {

    fun formatAcademicProgressMessage(
        student: StudentEntity,
        summary: StudentGradeSummary,
        classroomName: String,
        teacherName: String = "Class Teacher"
    ): String {
        return """
        📚 *Academic Progress Update*
        Dear Parent/Guardian,
        
        Here is the latest progress report for *${student.name}* (${classroomName}):
        • Current Grade: *${summary.percentage}% (${summary.letterGrade})*
        • GPA: *${summary.gpa} / 4.0* (${NebGradingEngine.getGradeDescription(summary.letterGrade)})
        • Attendance Rate: *${summary.attendanceRate.toInt()}%*
        • Homework Completion: *${summary.homeworkCompletionRate.toInt()}%*
        ${if (summary.riskReasons.isNotEmpty()) "⚠️ Focus Areas: ${summary.riskReasons.joinToString(", ")}" else "🌟 Standing: On Track"}
        
        Regards,
        $teacherName
        """.trimIndent()
    }

    fun formatAttendanceAlertMessage(
        student: StudentEntity,
        classroomName: String,
        status: String,
        date: String
    ): String {
        return """
        🚨 *Attendance Alert*
        Dear Parent/Guardian,
        
        Please be informed that *${student.name}* was marked *${status.uppercase()}* today ($date) in *${classroomName}*.
        
        If this was an excused absence, please submit an absence note.
        """.trimIndent()
    }

    fun formatDisciplineNotificationMessage(
        student: StudentEntity,
        record: DisciplineRecordEntity,
        classroomName: String
    ): String {
        return """
        📋 *School Behavior & Discipline Notice*
        Dear Parent/Guardian,
        
        This is to notify you regarding *${student.name}* in *${classroomName}*:
        • Category: *${record.category.displayName}*
        • Date: *${record.date}*
        • Note: ${record.description.ifEmpty { record.title }}
        • Action Taken: ${record.actionTaken.ifEmpty { "Verbal Counseling" }}
        
        Please contact the school if you have any questions.
        """.trimIndent()
    }
}
