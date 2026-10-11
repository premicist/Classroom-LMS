package com.example.util

import android.content.Context
import com.example.data.entity.AssignmentEntity
import com.example.ui.screens.DateRangeOption
import com.example.ui.screens.ReportType
import com.example.ui.viewmodel.LmsUiState

/**
 * Pure report-text builders extracted from ClassroomViewModel (ARCHITECTURE FREEZE F1).
 * Stateless helpers: they read from an immutable [LmsUiState] snapshot.
 */
object ReportTextBuilder {

    fun generateAndShareCustomPdf(
        context: Context,
        state: LmsUiState,
        reportType: ReportType,
        targetClassroomId: Long?,
        dateRange: DateRangeOption,
        includeAtRisk: Boolean,
        includeDifficulty: Boolean,
        includeDistribution: Boolean,
        includeAttendance: Boolean
    ) {
        val targetClass = targetClassroomId?.let { id -> state.classrooms.find { it.id == id } } ?: state.activeClassroom
        val className = if (targetClassroomId == null) "All Classrooms" else "${targetClass?.name ?: "Classroom"} (${targetClass?.subject ?: ""})"
        val sb = StringBuilder()
        sb.appendLine("CLASSROOM LMS CUSTOM REPORT")
        sb.appendLine("Report Type: ${reportType.title}")
        sb.appendLine("Target Scope: $className")
        sb.appendLine("Date Filter: ${dateRange.label}")
        sb.appendLine("==========================================")
        sb.appendLine()
        if (includeAtRisk) {
            sb.appendLine("== AT-RISK & SUPPORT NEEDED ALERTS ==")
            state.analytics.atRiskStudents.takeIf { it.isNotEmpty() }?.forEach { s ->
                sb.appendLine("• ${s.student.name}: ${s.riskReasons.joinToString(", ")}")
            }
            sb.appendLine()
        }
        if (includeDifficulty) {
            sb.appendLine("== HIGH DIFFICULTY ASSIGNMENT AREAS ==")
            state.analytics.difficultyAreas.takeIf { it.isNotEmpty() }?.forEach { area ->
                sb.appendLine("• ${area.assignment.title}: Avg ${area.averagePercentage.toInt()}% - ${area.teacherActionRecommendation}")
            }
            sb.appendLine()
        }
        if (includeDistribution) {
            val dist = state.analytics.distribution
            sb.appendLine("== GRADE DISTRIBUTION ==")
            sb.appendLine("A: ${dist.aCount} | B: ${dist.bCount} | C: ${dist.cCount} | D: ${dist.dCount} | F: ${dist.fCount}")
            sb.appendLine()
        }
        if (includeAttendance) {
            val att = state.attendanceReport
            if (att != null) {
                sb.appendLine("== ATTENDANCE OVERVIEW ==")
                sb.appendLine("Overall Rate: ${"%.1f".format(att.overallAttendanceRate)}% | Pres: ${att.totalPresent} | Abs: ${att.totalAbsent} | Late: ${att.totalLate} | Excused: ${att.totalExcused}")
                sb.appendLine()
            }
        }
        PdfReportExporter.exportAndShare(context, "${reportType.title} Report", sb.toString(), "Report_${className.replace(" ", "_")}")
    }

    fun generateStudentPortfolioReportText(state: LmsUiState, studentId: Long): String {
        val summary = state.studentGradeSummaries.find { it.student.id == studentId } ?: return "No data available."
        val sb = StringBuilder()
        sb.appendLine("STUDENT PORTFOLIO REPORT")
        sb.appendLine("Student: ${summary.student.name} (${summary.student.studentNumber})")
        sb.appendLine("Classroom: ${state.activeClassroom?.name ?: "N/A"}")
        sb.appendLine("------------------------------------------")
        sb.appendLine("Academic Standing:")
        sb.appendLine("  Overall Percentage: ${"%.1f".format(summary.percentage)}%")
        sb.appendLine("  Letter Grade: ${summary.letterGrade} (${"%.2f".format(summary.gpa)} GPA)")
        sb.appendLine("  Tier: ${summary.tier.label}")
        sb.appendLine()
        sb.appendLine("Assignments:")
        summary.submissions.forEach { sub ->
            val assign: AssignmentEntity? = state.assignments.find { it.id == sub.assignmentId }
            sb.appendLine("  [${assign?.type?.displayName ?: "?"}] ${assign?.title ?: "Unknown"}: ${sub.score?.let { "%.1f".format(it) } ?: "N/A"}/${assign?.maxPoints?.toInt()}")
        }
        sb.appendLine()
        sb.appendLine("At-Risk Indicators: ${if (summary.isAtRisk) "YES" else "NO"}")
        summary.riskReasons.takeIf { it.isNotEmpty() }?.forEach { sb.appendLine("  - $it") }
        sb.appendLine()
        sb.appendLine("Homework Completion: ${"%.1f".format(summary.homeworkCompletionRate)}%")
        sb.appendLine("Attendance Rate: ${"%.1f".format(summary.attendanceRate)}%")
        sb.appendLine()
        sb.appendLine("AI Insights:")
        val insight = AiInsightUtils.generateAiInsightForStudent(summary)
        sb.appendLine("  Risk Score: ${insight.riskScore}")
        insight.recommendations.forEach { sb.appendLine("  * $it") }
        return sb.toString()
    }
}
