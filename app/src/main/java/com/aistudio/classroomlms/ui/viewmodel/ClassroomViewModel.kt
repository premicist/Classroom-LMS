package com.aistudio.classroomlms.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.classroomlms.data.auth.AuthManager
import com.aistudio.classroomlms.data.database.PreferencesManager
import com.aistudio.classroomlms.data.entity.AssignmentEntity
import com.aistudio.classroomlms.data.entity.AttendanceRecordEntity
import com.aistudio.classroomlms.data.entity.ClassScheduleEntity
import com.aistudio.classroomlms.data.entity.ClassroomEntity
import com.aistudio.classroomlms.data.entity.DailyLogEntity
import com.aistudio.classroomlms.data.entity.DisciplineRecordEntity
import com.aistudio.classroomlms.data.entity.HomeworkRecordEntity
import com.aistudio.classroomlms.data.entity.InterventionEntity
import com.aistudio.classroomlms.data.entity.LessonPlanEntity
import com.aistudio.classroomlms.data.entity.LiveAssessmentEntity
import com.aistudio.classroomlms.data.entity.StudentEntity
import com.aistudio.classroomlms.data.entity.SubmissionEntity
import com.aistudio.classroomlms.data.entity.TermWeightConfig
import com.aistudio.classroomlms.data.repository.ClassroomRepository
import com.aistudio.classroomlms.data.repository.DatabaseBackupManager
import com.aistudio.classroomlms.data.repository.ScheduleRepository
import com.aistudio.classroomlms.ui.screens.DateRangeOption
import com.aistudio.classroomlms.ui.screens.ReportType
import com.aistudio.classroomlms.util.ReportTextBuilder
import com.aistudio.classroomlms.util.ClassroomAnalyticsEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ARCHITECTURE FREEZE: Thin session/orchestrator only.
 * Feature-specific logic is extracted to AttendanceViewModel, ExamViewModel,
 * GradingViewModel, PlannerViewModel, RosterViewModel, SettingsViewModel,
 * StudentSupportViewModel, and SyncViewModel.
 * See ARCHITECTURE_FREEZE.md in the repo root.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ClassroomViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ClassroomRepository,
    private val scheduleRepository: ScheduleRepository,
    private val authManager: AuthManager,
    private val preferencesManager: PreferencesManager,
    private val backupManager: DatabaseBackupManager
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val _uiState = MutableStateFlow(
        LmsUiState(
            selectedDate = dateFormat.format(Date()),
            activeHomeworkTopic = "Daily Homework Review"
        )
    )
    val uiState: StateFlow<LmsUiState> = _uiState.asStateFlow()

    private val _selectedClassroomId = MutableStateFlow<Long?>(null)
    private var isFirstSettingsLoad = true

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }

        viewModelScope.launch {
            combine(
                preferencesManager.activeClassroomIdFlow,
                repository.getAllClassrooms()
            ) { savedId, classrooms ->
                _uiState.update { it.copy(classrooms = classrooms) }
                if (classrooms.isNotEmpty()) {
                    if (savedId != null && classrooms.any { it.id == savedId }) {
                        if (_selectedClassroomId.value != savedId) {
                            _selectedClassroomId.value = savedId
                        }
                    } else if (_selectedClassroomId.value == null) {
                        val firstId = classrooms.first().id
                        _selectedClassroomId.value = firstId
                        preferencesManager.saveActiveClassroomId(firstId)
                    }
                }
            }.collect { }
        }

        viewModelScope.launch {
            preferencesManager.settingsFlow.collect { settings ->
                _uiState.update { state ->
                    val shouldLock = if (isFirstSettingsLoad && settings.isBiometricLockEnabled) true else state.isAppLocked
                    state.copy(settings = settings, isAppLocked = shouldLock)
                }
                isFirstSettingsLoad = false
            }
        }

        viewModelScope.launch {
            preferencesManager.activeClassroomIdFlow
                .flatMapLatest { classroomId ->
                    if (classroomId != null) {
                        combine(
                            repository.getClassroomById(classroomId),
                            repository.getStudents(classroomId),
                            repository.getAssignments(classroomId),
                            repository.getSubmissionsByClassroom(classroomId),
                            repository.getAttendanceForClassroom(classroomId),
                            repository.getInterventionsByClassroom(classroomId),
                            repository.getLessonPlans(classroomId),
                            repository.getDailyLogs(classroomId),
                            repository.getDisciplineRecordsByClassroom(classroomId),
                            repository.getLiveAssessmentsByClassroom(classroomId),
                            scheduleRepository.getSchedulesByClassroom(classroomId)
                        ) { args: Array<Any?> ->
                            @Suppress("UNCHECKED_CAST")
                            CombinedClassData(
                                classroom = args[0] as ClassroomEntity?,
                                students = args[1] as List<StudentEntity>,
                                assignments = args[2] as List<AssignmentEntity>,
                                submissions = args[3] as List<SubmissionEntity>,
                                attendance = args[4] as List<AttendanceRecordEntity>,
                                interventions = args[5] as List<InterventionEntity>,
                                lessonPlans = args[6] as List<LessonPlanEntity>,
                                dailyLogs = args[7] as List<DailyLogEntity>,
                                disciplineRecords = args[8] as List<DisciplineRecordEntity>,
                                liveAssessments = args[9] as List<LiveAssessmentEntity>,
                                schedules = args[10] as List<ClassScheduleEntity>
                            )
                        }
                    } else {
                        flowOf(null)
                    }
                }
                .collect { combined ->
                    if (combined != null) {
                        val currentConfig = _uiState.value.termWeightConfig
                        val studentSummaries = withContext(Dispatchers.Default) {
                            ClassroomAnalyticsEngine.computeStudentGrades(
                                combined.students, combined.assignments, combined.submissions,
                                emptyList(), combined.attendance, currentConfig
                            )
                        }
                        val analytics = withContext(Dispatchers.Default) {
                            ClassroomAnalyticsEngine.computeClassAnalytics(
                                studentSummaries, combined.assignments, combined.submissions,
                                combined.attendance, combined.interventions
                            )
                        }
                        val attendanceReport = withContext(Dispatchers.Default) {
                            ClassroomAnalyticsEngine.computeAttendanceReport(
                                combined.classroom, combined.students, combined.attendance
                            )
                        }
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                activeClassroom = combined.classroom,
                                students = combined.students,
                                assignments = combined.assignments,
                                submissions = combined.submissions,
                                attendanceRecords = combined.attendance,
                                interventions = combined.interventions,
                                lessonPlans = combined.lessonPlans,
                                dailyLogs = combined.dailyLogs,
                                schedules = combined.schedules,
                                disciplineRecords = combined.disciplineRecords,
                                liveAssessments = combined.liveAssessments,
                                studentGradeSummaries = studentSummaries,
                                analytics = analytics,
                                attendanceReport = attendanceReport
                            )
                        }
                    }
                }
        }
    }

    // ---- Navigation & Session ----
    fun selectTab(tab: LmsTab) { _uiState.update { it.copy(selectedTab = tab) } }

    fun selectClassroom(id: Long) {
        viewModelScope.launch {
            preferencesManager.saveActiveClassroomId(id)
            _selectedClassroomId.value = id
        }
    }

    // ---- Session Overlays & Dialogs ----
    fun openPlanner() { _uiState.update { it.copy(isPlannerOpen = true) } }
    fun closePlanner() { _uiState.update { it.copy(isPlannerOpen = false) } }

    fun openScheduleScreen() { _uiState.update { it.copy(isScheduleScreenOpen = true) } }
    fun closeScheduleScreen() { _uiState.update { it.copy(isScheduleScreenOpen = false) } }

    fun openSettings() { _uiState.update { it.copy(isSettingsOpen = true) } }
    fun closeSettings() { _uiState.update { it.copy(isSettingsOpen = false) } }

    fun openStudentRoster() { _uiState.update { it.copy(isStudentRosterOpen = true) } }
    fun closeStudentRoster() { _uiState.update { it.copy(isStudentRosterOpen = false) } }

    fun openStudentProfile(summary: StudentGradeSummary) { _uiState.update { it.copy(selectedStudentProfile = summary) } }
    fun closeStudentProfile() { _uiState.update { it.copy(selectedStudentProfile = null) } }

    fun openAddIntervention(student: StudentEntity? = null) {
        _uiState.update { it.copy(isAddInterventionOpen = true, interventionStudent = student, editingIntervention = null) }
    }
    fun closeInterventionDialog() {
        _uiState.update { it.copy(isAddInterventionOpen = false, interventionStudent = null, editingIntervention = null) }
    }

    fun openAddDiscipline(student: StudentEntity) {
        _uiState.update { it.copy(isAddEditDisciplineOpen = true, disciplineStudent = student, editingDisciplineRecord = null) }
    }
    fun closeDisciplineDialog() {
        _uiState.update { it.copy(isAddEditDisciplineOpen = false, disciplineStudent = null, editingDisciplineRecord = null) }
    }

    fun openLiveAssessment(student: StudentEntity? = null) {
        _uiState.update { it.copy(isLiveAssessmentDialogOpen = true, liveAssessmentStudent = student, editingLiveAssessment = null) }
    }
    fun closeLiveAssessmentDialog() {
        _uiState.update { it.copy(isLiveAssessmentDialogOpen = false, liveAssessmentStudent = null, editingLiveAssessment = null) }
    }

    fun openClassroomManagement() { _uiState.update { it.copy(isClassroomModalOpen = true) } }
    fun setClassroomModalOpen(open: Boolean) { _uiState.update { it.copy(isClassroomModalOpen = open) } }

    fun openExportReport(content: String, title: String = "Class Progress & Intervention Report") {
        _uiState.update { it.copy(isExportReportOpen = true, exportReportContent = content, exportReportTitle = title) }
    }
    fun closeExportReport() {
        _uiState.update { it.copy(isExportReportOpen = false, exportReportContent = "", exportReportTitle = "Class Progress & Intervention Report") }
    }

    fun openGenerateReportDialog() { _uiState.update { it.copy(isGenerateReportDialogOpen = true) } }
    fun closeGenerateReportDialog() { _uiState.update { it.copy(isGenerateReportDialogOpen = false) } }

    fun openRemedialPlanDialog(area: ClassDifficultyArea) {
        _uiState.update { it.copy(isRemedialPlanDialogOpen = true, selectedDifficultyAreaForRemedial = area) }
    }
    fun setAnalyticsSelectedStudent(studentId: Long?) { _uiState.update { it.copy(analyticsSelectedStudentId = studentId) } }
    fun setAnalyticsTierFilter(tier: StudentTier?) { _uiState.update { it.copy(analyticsTierFilter = tier) } }
    fun setAnalyticsViewSection(section: Int) { _uiState.update { it.copy(analyticsViewSection = section) } }
    fun closeRemedialPlanDialog() {
        _uiState.update { it.copy(isRemedialPlanDialogOpen = false, selectedDifficultyAreaForRemedial = null) }
    }

    fun openTermWeightingDialog() { _uiState.update { it.copy(isTermWeightingDialogOpen = true) } }
    fun closeTermWeightingDialog() { _uiState.update { it.copy(isTermWeightingDialogOpen = false) } }

    fun saveTermWeightConfig(config: TermWeightConfig) {
        _uiState.update { it.copy(termWeightConfig = config) }
        closeTermWeightingDialog()
    }

    fun openAddClassroom() { _uiState.update { it.copy(isAddEditClassroomOpen = true, editingClassroom = null, isClassroomModalOpen = true) } }
    fun openAddStudent() { _uiState.update { it.copy(isAddEditStudentOpen = true, editingStudent = null) } }
    fun openAddAssignment() { _uiState.update { it.copy(isAddEditAssignmentOpen = true, editingAssignment = null) } }

    fun closeDialogs() {
        _uiState.update { state ->
            state.copy(
                isAddEditClassroomOpen = false, editingClassroom = null,
                isAddEditStudentOpen = false, editingStudent = null,
                isAddEditAssignmentOpen = false, editingAssignment = null
            )
        }
    }

    // ---- Feedback & Lock ----
    fun showToast(message: String) { _uiState.update { it.copy(userNotificationMessage = message) } }
    fun clearToast() { _uiState.update { it.copy(userNotificationMessage = null) } }
    fun clearNotificationMessage() { clearToast() }
    fun setAppLocked(locked: Boolean) { _uiState.update { it.copy(isAppLocked = locked) } }

    // ---- Routine Summary ----
    fun getTodayClassesSummary(): Pair<Boolean, String> {
        val cal = Calendar.getInstance()
        val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "MONDAY"
            Calendar.TUESDAY -> "TUESDAY"
            Calendar.WEDNESDAY -> "WEDNESDAY"
            Calendar.THURSDAY -> "THURSDAY"
            Calendar.FRIDAY -> "FRIDAY"
            Calendar.SATURDAY -> "SATURDAY"
            Calendar.SUNDAY -> "SUNDAY"
            else -> "MONDAY"
        }
        if (dayOfWeek == "SATURDAY" || dayOfWeek == "SUNDAY") return Pair(true, "Today is Holiday, Relax")
        val masterSchedules = uiState.value.allSchedules.ifEmpty { uiState.value.schedules }
        val todaySchedules = masterSchedules.filter { it.dayOfWeek.equals(dayOfWeek, ignoreCase = true) }
            .distinctBy { "${it.classroomName}_${it.startMinutes}" }.sortedBy { it.startMinutes }
        return if (todaySchedules.isEmpty()) {
            Pair(false, "No classes today.")
        } else {
            val formatted = todaySchedules.joinToString(", ") { "\"${it.classroomName}\" @ ${it.startTime}" }
            Pair(true, formatted)
        }
    }

    fun saveScheduleBatch(baseEntry: ClassScheduleEntity, selectedDays: List<String>, applyAllWeeks: Boolean, updateSubjectAllWeeks: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val entriesToInsert = mutableListOf<ClassScheduleEntity>()
            val weeksToApply = if (applyAllWeeks) listOf(1, 2, 3, 4, 5) else listOf(baseEntry.weekNumber)
            weeksToApply.forEach { week ->
                selectedDays.forEach { day ->
                    entriesToInsert.add(baseEntry.copy(id = 0L, weekNumber = week, dayOfWeek = day))
                }
            }
            scheduleRepository.saveBatchScheduleEntries(entriesToInsert)
            showToast("Schedule saved")
        }
    }

    fun replicateWeekSchedule(fromWeek: Int, targetWeeks: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduleRepository.replicateWeekSchedule(fromWeek, targetWeeks)
            showToast("Schedule replicated to weeks ${targetWeeks.joinToString(", ")}")
        }
    }

    fun clearWeekSchedule(weekNumber: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduleRepository.deleteSchedulesForWeek(weekNumber)
            showToast("Week $weekNumber schedule cleared")
        }
    }

    fun deleteScheduleEntry(entry: ClassScheduleEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduleRepository.deleteScheduleEntry(entry)
            showToast("Schedule deleted")
        }
    }

    fun resetSampleData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetWithSampleData()
            showToast("Sample data reset")
        }
    }

    // ---- Reports & Export (builders live in util/ReportTextBuilder) ----
    fun generateAndShareCustomPdf(
        reportType: ReportType,
        targetClassroomId: Long?,
        dateRange: DateRangeOption,
        includeAtRisk: Boolean,
        includeDifficulty: Boolean,
        includeDistribution: Boolean,
        includeAttendance: Boolean
    ) = ReportTextBuilder.generateAndShareCustomPdf(
        context, uiState.value, reportType, targetClassroomId, dateRange,
        includeAtRisk, includeDifficulty, includeDistribution, includeAttendance
    )

    fun generateStudentPortfolioReportText(studentId: Long): String =
        ReportTextBuilder.generateStudentPortfolioReportText(uiState.value, studentId)


    private data class CombinedClassData(
        val classroom: ClassroomEntity?,
        val students: List<StudentEntity>,
        val assignments: List<AssignmentEntity>,
        val submissions: List<SubmissionEntity>,
        val attendance: List<AttendanceRecordEntity>,
        val interventions: List<InterventionEntity>,
        val lessonPlans: List<LessonPlanEntity>,
        val dailyLogs: List<DailyLogEntity>,
        val disciplineRecords: List<DisciplineRecordEntity>,
        val liveAssessments: List<LiveAssessmentEntity>,
        val schedules: List<ClassScheduleEntity>
    )
}
