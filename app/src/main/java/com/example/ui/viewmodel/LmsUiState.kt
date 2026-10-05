package com.example.ui.viewmodel

import com.example.data.database.LmsSettings
import com.example.data.entity.AssignmentEntity
import com.example.data.entity.AssignmentType
import com.example.data.entity.AttendanceRecordEntity
import com.example.data.entity.ClassScheduleEntity
import com.example.data.entity.ClassroomEntity
import com.example.data.entity.DailyLogEntity
import com.example.data.entity.DisciplineRecordEntity
import com.example.data.entity.ExamCategory
import com.example.data.entity.ExamEntity
import com.example.data.entity.ExamMarkEntity
import com.example.data.entity.HomeworkRecordEntity
import com.example.data.entity.HomeworkStatus
import com.example.data.entity.InterventionEntity
import com.example.data.entity.InterventionType
import com.example.data.entity.LessonPlanEntity
import com.example.data.entity.LiveAssessmentEntity
import com.example.data.entity.StudentEntity
import com.example.data.entity.SubmissionEntity
import com.example.data.entity.SubmissionStatus
import com.example.data.entity.TermWeightConfig
import com.example.data.repository.AppUpdateInfo

enum class TrajectoryTrend(val label: String) {
    IMPROVING("Improving Trajectory"),
    STEADY("Steady Progress"),
    DECLINING("Declining Trend")
}

enum class StudentTier(val label: String, val shortLabel: String) {
    EXTRA_SUPPORT("Needs Extra Support", "Support Needed"),
    ENRICHMENT_NEEDED("Needs Enrichment", "Enrichment Ready"),
    ON_TRACK("On Track", "On Track")
}

enum class DifficultySeverity(val label: String) {
    HIGH_DIFFICULTY("High Difficulty"),
    MODERATE_CHALLENGE("Moderate Challenge"),
    WELL_MASTERED("Well Mastered")
}

data class StudentGradeSummary(
    val student: StudentEntity,
    val earnedPoints: Double,
    val possiblePoints: Double,
    val percentage: Double,
    val letterGrade: String,
    val gpa: Double,
    val submissions: List<SubmissionEntity>,
    val gradedCount: Int,
    val missingCount: Int,
    val pendingCount: Int,
    val homeworkCompletionRate: Double,
    val attendanceRate: Double,
    val isAtRisk: Boolean,
    val isEnrichmentCandidate: Boolean,
    val riskReasons: List<String>,
    val enrichmentReasons: List<String>,
    val tier: StudentTier,
)

data class ClassGradeDistribution(
    val aCount: Int = 0,
    val bCount: Int = 0,
    val cCount: Int = 0,
    val dCount: Int = 0,
    val fCount: Int = 0
)

data class AssignmentTimelinePoint(
    val assignmentId: Long,
    val title: String,
    val dueDate: String,
    val type: AssignmentType,
    val maxPoints: Double,
    val classAveragePercentage: Double,
    val highestPercentage: Double,
    val lowestPercentage: Double,
    val studentScores: Map<Long, Double> = emptyMap(), // studentId -> percentage
    val isDifficult: Boolean = false
)

data class StudentProgressTrajectory(
    val student: StudentEntity,
    val summary: StudentGradeSummary,
    val trend: TrajectoryTrend,
    val trendDelta: Double, // positive or negative %
    val scoreHistory: List<Pair<String, Double>>, // (Assignment Title, Percentage)
    val homeworkRate: Double,
    val attendanceRate: Double,
    val tier: StudentTier,
    val interventionsCount: Int = 0,
    val recommendations: List<String> = emptyList()
)

data class ClassDifficultyArea(
    val assignment: AssignmentEntity,
    val averagePercentage: Double,
    val medianPercentage: Double,
    val failingCount: Int,
    val missingCount: Int,
    val totalGraded: Int,
    val severity: DifficultySeverity,
    val keyConcept: String,
    val teacherActionRecommendation: String,
    val strugglingStudents: List<StudentEntity>
)

data class CategoryMastery(
    val type: AssignmentType,
    val averagePercentage: Double,
    val assignmentCount: Int,
    val gradedCount: Int,
    val status: String
)

data class ClassroomAnalytics(
    val averagePercentage: Double = 0.0,
    val averageGpa: Double = 0.0,
    val highestPercentage: Double = 0.0,
    val lowestPercentage: Double = 0.0,
    val distribution: ClassGradeDistribution = ClassGradeDistribution(),
    val totalStudents: Int = 0,
    val totalAssignments: Int = 0,
    val pendingGradingCount: Int = 0,
    val todayAttendanceRate: Double = 100.0,
    val atRiskStudents: List<StudentGradeSummary> = emptyList(),
    val enrichmentStudents: List<StudentGradeSummary> = emptyList(),
    val onTrackStudents: List<StudentGradeSummary> = emptyList(),
    
    // Progress over time & difficulty breakdown
    val timelinePoints: List<AssignmentTimelinePoint> = emptyList(),
    val studentTrajectories: List<StudentProgressTrajectory> = emptyList(),
    val difficultyAreas: List<ClassDifficultyArea> = emptyList(),
    val categoryMasteries: List<CategoryMastery> = emptyList(),
    val classTrend: TrajectoryTrend = TrajectoryTrend.STEADY,
    val classTrendDelta: Double = 0.0
)

data class StudentAttendanceSummary(
    val student: StudentEntity,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val excusedCount: Int,
    val totalDays: Int,
    val attendanceRate: Double
)

data class AttendanceReport(
    val classroomName: String,
    val subject: String,
    val reportDate: String,
    val dateRangeText: String,
    val totalDaysRecorded: Int,
    val totalRecords: Int,
    val overallAttendanceRate: Double,
    val totalPresent: Int,
    val totalAbsent: Int,
    val totalLate: Int,
    val totalExcused: Int,
    val studentSummaries: List<StudentAttendanceSummary>,
    val chronicAbsentees: List<StudentAttendanceSummary>,
    val perfectAttendance: List<StudentAttendanceSummary>
)

data class HomeworkCheckDay(
    val date: String,
    val topic: String,
    val records: List<HomeworkRecordEntity>,
    val totalStudents: Int,
    val doneCount: Int,
    val partialCount: Int,
    val missingCount: Int,
    val excusedCount: Int,
    val completionPercentage: Double
)

enum class LmsTab(val label: String) {
    DASHBOARD("Overview"),
    ACADEMIC("Academic"),
    STUDENTS("Students"),
    ATTENDANCE("Attendance"),
    ANALYTICS("Analytics")
}

data class LmsUiState(
    val isLoading: Boolean = true,
    val selectedTab: LmsTab = LmsTab.DASHBOARD,
    val classrooms: List<ClassroomEntity> = emptyList(),
    val activeClassroom: ClassroomEntity? = null,
    val students: List<StudentEntity> = emptyList(),
    /** All students across every classroom (for full roster). */
    val allStudents: List<StudentEntity> = emptyList(),
    val assignments: List<AssignmentEntity> = emptyList(),
    val submissions: List<SubmissionEntity> = emptyList(),
    val homeworkRecords: List<HomeworkRecordEntity> = emptyList(),
    val attendanceRecords: List<AttendanceRecordEntity> = emptyList(),
    val interventions: List<InterventionEntity> = emptyList(),
    val schedules: List<ClassScheduleEntity> = emptyList(),
    val allSchedules: List<ClassScheduleEntity> = emptyList(),
    val exams: List<ExamEntity> = emptyList(),
    val examMarks: List<ExamMarkEntity> = emptyList(),
    
    // Computed analytics
    val studentGradeSummaries: List<StudentGradeSummary> = emptyList(),
    val analytics: ClassroomAnalytics = ClassroomAnalytics(),
    val attendanceReport: AttendanceReport? = null,
    val homeworkCheckDays: List<HomeworkCheckDay> = emptyList(),
    
    // Active filters & selections
    val selectedDate: String = "", // Today YYYY-MM-DD
    val activeHomeworkTopic: String = "",
    val assignmentFilterType: AssignmentType? = null,
    val assignmentFilterStatus: SubmissionStatus? = null,
    val searchQuery: String = "",
    
    // Analytics screen filters
    val analyticsSelectedStudentId: Long? = null, // null for class view
    val analyticsTierFilter: StudentTier? = null, // null for all tiers
    val analyticsViewSection: Int = 0, // 0 = Trends Over Time, 1 = Difficulty Areas, 2 = Student Support & Enrichment Roster
    
    // Dialog states
    val isClassroomModalOpen: Boolean = false,
    val isStudentRosterOpen: Boolean = false,
    val isAddEditClassroomOpen: Boolean = false,
    val editingClassroom: ClassroomEntity? = null,
    val isAddEditStudentOpen: Boolean = false,
    val editingStudent: StudentEntity? = null,
    val isAddEditAssignmentOpen: Boolean = false,
    val editingAssignment: AssignmentEntity? = null,
    val isGradingSubmissionOpen: Boolean = false,
    val gradingSubmission: SubmissionEntity? = null,
    val gradingAssignment: AssignmentEntity? = null,
    val gradingStudent: StudentEntity? = null,
    val selectedStudentProfile: StudentGradeSummary? = null,
    val isAddInterventionOpen: Boolean = false,
    val editingIntervention: InterventionEntity? = null,
    val interventionStudent: StudentEntity? = null,
    val disciplineRecords: List<DisciplineRecordEntity> = emptyList(),
    val isAddEditDisciplineOpen: Boolean = false,
    val editingDisciplineRecord: DisciplineRecordEntity? = null,
    val disciplineStudent: StudentEntity? = null,
    val liveAssessments: List<LiveAssessmentEntity> = emptyList(),
    val isLiveAssessmentDialogOpen: Boolean = false,
    val liveAssessmentStudent: StudentEntity? = null,
    val editingLiveAssessment: LiveAssessmentEntity? = null,
    val isExportReportOpen: Boolean = false,
    val exportReportContent: String = "",
    val exportReportTitle: String = "Class Progress & Intervention Report",
    val userNotificationMessage: String? = null,
    val isPlannerOpen: Boolean = false,
    val isScheduleScreenOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val selectedDifficultyAreaForRemedial: ClassDifficultyArea? = null,
    val isRemedialPlanDialogOpen: Boolean = false,

    // App Update State
    val isCheckingForUpdates: Boolean = false,
    val updateInfo: AppUpdateInfo? = null,
    val isUpdateDialogOpen: Boolean = false,

    // Lesson Planner & Daily Diary State
    val lessonPlans: List<LessonPlanEntity> = emptyList(),
    val dailyLogs: List<DailyLogEntity> = emptyList(),
    val isAddEditLessonPlanOpen: Boolean = false,
    val editingLessonPlan: LessonPlanEntity? = null,
    val isAddEditDailyLogOpen: Boolean = false,
    val editingDailyLog: DailyLogEntity? = null,
    val plannerClassroomFilterId: Long? = null, // null = All Classrooms

    // Custom Report Generator State
    val isGenerateReportDialogOpen: Boolean = false,

    // Term Weighting Configuration State
    val termWeightConfig: TermWeightConfig = TermWeightConfig(),
    val isTermWeightingDialogOpen: Boolean = false,

    // Exams
    val isAddEditExamOpen: Boolean = false,
    val editingExam: ExamEntity? = null,
    val selectedExamCategory: ExamCategory = ExamCategory.CLASS_TEST,

    // Persisted App Settings
    val settings: LmsSettings = LmsSettings()
)
