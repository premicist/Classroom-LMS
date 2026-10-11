package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.dao.AssignmentDao
import com.example.data.dao.AttendanceDao
import com.example.data.dao.ClassroomDao
import com.example.data.dao.DisciplineDao
import com.example.data.dao.ExamDao
import com.example.data.dao.HomeworkRecordDao
import com.example.data.dao.InterventionDao
import com.example.data.dao.LiveAssessmentDao
import com.example.data.dao.StudentDao
import com.example.data.dao.SubmissionDao
import com.example.data.database.AppDatabase
import com.example.data.database.PreferencesManager
import com.example.data.entity.AssignmentEntity
import com.example.data.entity.AttendanceRecordEntity
import com.example.data.entity.ClassroomEntity
import com.example.data.entity.DisciplineRecordEntity
import com.example.data.entity.HomeworkRecordEntity
import com.example.data.entity.InterventionEntity
import com.example.data.entity.LiveAssessmentEntity
import com.example.data.entity.StudentEntity
import com.example.data.entity.SubmissionEntity
import com.example.data.entity.TermWeightConfig
import com.example.ui.screens.DateRangeOption
import com.example.ui.screens.ReportType
import com.example.util.ClassroomAnalyticsEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AnalyticsUiState(
    val activeClassroomId: Long? = null,
    val activeClassroom: ClassroomEntity? = null,
    val classrooms: List<ClassroomEntity> = emptyList(),
    val students: List<StudentEntity> = emptyList(),
    val analytics: ClassroomAnalytics = ClassroomAnalytics(),
    val studentGradeSummaries: List<StudentGradeSummary> = emptyList(),
    val attendanceReport: AttendanceReport? = null,
    val interventions: List<InterventionEntity> = emptyList(),
    val isGenerateReportDialogOpen: Boolean = false,
    val isExportReportOpen: Boolean = false,
    val exportReportContent: String = "",
    val exportReportTitle: String = "Classroom LMS Report",
    val userNotificationMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val studentDao: StudentDao,
    private val assignmentDao: AssignmentDao,
    private val submissionDao: SubmissionDao,
    private val attendanceDao: AttendanceDao,
    private val homeworkRecordDao: HomeworkRecordDao,
    private val interventionDao: InterventionDao,
    private val disciplineDao: DisciplineDao,
    private val liveAssessmentDao: LiveAssessmentDao,
    private val classroomDao: ClassroomDao,
    private val preferencesManager: PreferencesManager,
    database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.activeClassroomIdFlow
                .flatMapLatest { classroomId ->
                    _uiState.update { it.copy(activeClassroomId = classroomId) }
                    if (classroomId != null) {
                        combine(
                            classroomDao.getAllClassrooms(),
                            classroomDao.getClassroomById(classroomId),
                            studentDao.getStudentsByClassroom(classroomId),
                            assignmentDao.getAssignmentsByClassroom(classroomId),
                            submissionDao.getSubmissionsByClassroom(classroomId),
                            attendanceDao.getAttendanceForClassroom(classroomId),
                            homeworkRecordDao.getAllHomeworkRecordsForClassroom(classroomId),
                            interventionDao.getInterventionsByClassroom(classroomId)
                        ) { args: Array<Any?> ->
                            @Suppress("UNCHECKED_CAST")
                            FullData(
                                classrooms = args[0] as List<ClassroomEntity>,
                                activeClassroom = args[1] as ClassroomEntity?,
                                students = args[2] as List<StudentEntity>,
                                assignments = args[3] as List<AssignmentEntity>,
                                submissions = args[4] as List<SubmissionEntity>,
                                attendance = args[5] as List<AttendanceRecordEntity>,
                                homeworks = args[6] as List<HomeworkRecordEntity>,
                                interventions = args[7] as List<InterventionEntity>
                            )
                        }
                    } else {
                        flowOf(FullData(emptyList(), null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))
                    }
                }
                .collect { data ->
                    val summaries = ClassroomAnalyticsEngine.computeStudentGrades(
                        students = data.students,
                        assignments = data.assignments,
                        submissions = data.submissions,
                        homeworks = data.homeworks,
                        attendance = data.attendance,
                        termWeightConfig = TermWeightConfig()
                    )
                    val analytics = ClassroomAnalyticsEngine.computeClassAnalytics(
                        summaries = summaries,
                        assignments = data.assignments,
                        submissions = data.submissions,
                        attendance = data.attendance,
                        interventions = data.interventions
                    )
                    val attReport = ClassroomAnalyticsEngine.computeAttendanceReport(
                        classroom = data.activeClassroom,
                        students = data.students,
                        records = data.attendance
                    )

                    _uiState.update { current ->
                        current.copy(
                            classrooms = data.classrooms,
                            activeClassroom = data.activeClassroom,
                            students = data.students,
                            studentGradeSummaries = summaries,
                            analytics = analytics,
                            attendanceReport = attReport,
                            interventions = data.interventions
                        )
                    }
                }
        }
    }

    private data class FullData(
        val classrooms: List<ClassroomEntity>,
        val activeClassroom: ClassroomEntity?,
        val students: List<StudentEntity>,
        val assignments: List<AssignmentEntity>,
        val submissions: List<SubmissionEntity>,
        val attendance: List<AttendanceRecordEntity>,
        val homeworks: List<HomeworkRecordEntity>,
        val interventions: List<InterventionEntity>
    )

    fun openGenerateReportDialog() {
        _uiState.update { it.copy(isGenerateReportDialogOpen = true) }
    }

    fun closeGenerateReportDialog() {
        _uiState.update { it.copy(isGenerateReportDialogOpen = false) }
    }

    fun openExportReport(reportText: String, title: String = "Classroom LMS Report") {
        _uiState.update {
            it.copy(
                isExportReportOpen = true,
                exportReportContent = reportText,
                exportReportTitle = title
            )
        }
    }

    fun closeExportReport() {
        _uiState.update {
            it.copy(
                isExportReportOpen = false,
                exportReportContent = "",
                exportReportTitle = "Classroom LMS Report"
            )
        }
    }

    fun generateCustomReportText(
        reportType: ReportType,
        targetClassroomId: Long?,
        dateRange: DateRangeOption,
        includeAtRisk: Boolean,
        includeDifficulty: Boolean,
        includeDistribution: Boolean,
        includeAttendance: Boolean
    ): String {
        val targetClass = targetClassroomId?.let { id -> _uiState.value.classrooms.find { it.id == id } }
            ?: _uiState.value.activeClassroom

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
            val atRisk = _uiState.value.analytics.atRiskStudents
            if (atRisk.isEmpty()) {
                sb.appendLine("All students currently on track.")
            } else {
                atRisk.forEach { s ->
                    sb.appendLine("• ${s.student.name}: ${s.riskReasons.joinToString(", ")}")
                }
            }
            sb.appendLine()
        }

        if (includeDifficulty) {
            sb.appendLine("== HIGH DIFFICULTY ASSIGNMENT AREAS ==")
            val diffs = _uiState.value.analytics.difficultyAreas
            if (diffs.isEmpty()) {
                sb.appendLine("No high difficulty assignments flagged.")
            } else {
                diffs.forEach { area ->
                    sb.appendLine("• ${area.assignment.title}: Avg ${area.averagePercentage.toInt()}% - ${area.teacherActionRecommendation}")
                }
            }
            sb.appendLine()
        }

        if (includeDistribution) {
            sb.appendLine("== GRADE DISTRIBUTION BREAKDOWN ==")
            val dist = _uiState.value.analytics.distribution
            sb.appendLine("A: ${dist.aCount} | B: ${dist.bCount} | C: ${dist.cCount} | D: ${dist.dCount} | F: ${dist.fCount}")
            sb.appendLine()
        }

        if (includeAttendance) {
            sb.appendLine("== ATTENDANCE OVERVIEW ==")
            val att = _uiState.value.attendanceReport
            if (att != null) {
                sb.appendLine("Overall Attendance Rate: ${"%.1f".format(att.overallAttendanceRate)}%")
                sb.appendLine("Present: ${att.totalPresent} | Absent: ${att.totalAbsent} | Late: ${att.totalLate} | Excused: ${att.totalExcused}")
            } else {
                sb.appendLine("No attendance records logged for this scope.")
            }
        }

        return sb.toString()
    }

    fun generateProgressAnalyticsReportText(): String {
        val activeClass = _uiState.value.activeClassroom ?: return "No active classroom selected."
        val analytics = _uiState.value.analytics
        val interventions = _uiState.value.interventions
        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        val sb = StringBuilder()

        sb.append("====================================================\n")
        sb.append("      STUDENT PROGRESS ANALYTICS & TEACHER AUDIT    \n")
        sb.append("====================================================\n\n")
        sb.append("Classroom: ${activeClass.name} • Subject: ${activeClass.subject}\n")
        sb.append("Grade Level: ${activeClass.gradeLevel} | Room: ${activeClass.roomNumber}\n")
        sb.append("Report Date: ${dateFormat.format(java.util.Date())}\n\n")

        sb.append("1. CLASS PERFORMANCE OVERVIEW\n")
        sb.append("----------------------------------------------------\n")
        sb.append("• Class Average Score: ${"%.1f".format(analytics.averagePercentage)}% (GPA ${"%.2f".format(analytics.averageGpa)})\n")
        sb.append("• Highest Score: ${"%.1f".format(analytics.highestPercentage)}% | Lowest Score: ${"%.1f".format(analytics.lowestPercentage)}%\n")
        sb.append("• Overall Trajectory: ${analytics.classTrend.label} (${if (analytics.classTrendDelta >= 0) "+" else ""}${"%.1f".format(analytics.classTrendDelta)}%)\n")
        sb.append("• Grade Breakdown: A: ${analytics.distribution.aCount} | B: ${analytics.distribution.bCount} | C: ${analytics.distribution.cCount} | D: ${analytics.distribution.dCount} | F: ${analytics.distribution.fCount}\n")
        sb.append("• Today's Attendance Rate: ${"%.1f".format(analytics.todayAttendanceRate)}%\n\n")

        sb.append("2. COMMON AREAS OF DIFFICULTY ACROSS THE CLASS\n")
        sb.append("----------------------------------------------------\n")
        if (analytics.difficultyAreas.isNotEmpty()) {
            analytics.difficultyAreas.forEachIndexed { idx, diff ->
                sb.append("${idx + 1}. [${diff.severity.label.uppercase()}] ${diff.assignment.title}\n")
                sb.append("   • Class Avg: ${"%.1f".format(diff.averagePercentage)}% | Failing (<70%): ${diff.failingCount} students | Missing: ${diff.missingCount}\n")
                sb.append("   • Action Plan: ${diff.teacherActionRecommendation}\n\n")
            }
        } else {
            sb.append("No major difficulty areas identified across current assessments.\n\n")
        }

        sb.append("3. STUDENTS REQUIRING EXTRA SUPPORT (AT-RISK / INTERVENTION)\n")
        sb.append("----------------------------------------------------\n")
        if (analytics.atRiskStudents.isNotEmpty()) {
            analytics.atRiskStudents.forEach { s ->
                val sInters = interventions.filter { it.studentId == s.student.id }
                sb.append("• ${s.student.name} (${s.student.studentNumber}): ${"%.1f".format(s.percentage)}% (${s.letterGrade})\n")
                sb.append("  Triggers: ${s.riskReasons.joinToString(", ")}\n")
                sb.append("  Logged Interventions: ${sInters.size} records on file\n")
                if (sInters.isNotEmpty()) {
                    sInters.take(2).forEach { inRec ->
                        sb.append("   - [${inRec.date} • ${inRec.type.displayName}]: ${inRec.title}\n")
                    }
                }
                sb.append("\n")
            }
        } else {
            sb.append("All students are meeting baseline performance thresholds!\n\n")
        }

        sb.append("4. STUDENTS READY FOR ENRICHMENT (HONORS / ADVANCED)\n")
        sb.append("----------------------------------------------------\n")
        if (analytics.enrichmentStudents.isNotEmpty()) {
            analytics.enrichmentStudents.forEach { s ->
                sb.append("• ${s.student.name} (${s.student.studentNumber}): ${"%.1f".format(s.percentage)}% (${s.letterGrade}) | HW: ${"%.1f".format(s.homeworkCompletionRate)}%\n")
                sb.append("  Enrichment Indicators: ${s.enrichmentReasons.joinToString(", ")}\n")
            }
        } else {
            sb.append("No students currently categorized in the enrichment tier.\n")
        }

        sb.append("\n====================================================\n")
        return sb.toString()
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(userNotificationMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(userNotificationMessage = null) }
    }
}
