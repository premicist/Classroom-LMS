package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.AssignmentEntity
import com.example.data.entity.AssignmentType
import com.example.data.entity.AttendanceRecordEntity
import com.example.data.entity.AttendanceStatus
import com.example.data.entity.ClassroomEntity
import com.example.data.entity.DailyLogEntity
import com.example.data.entity.HomeworkRecordEntity
import com.example.data.entity.HomeworkStatus
import com.example.data.entity.InterventionEntity
import com.example.data.entity.InterventionType
import com.example.data.entity.LessonPlanEntity
import com.example.data.entity.StudentEntity
import com.example.data.entity.SubmissionEntity
import com.example.data.entity.SubmissionStatus
import com.example.data.repository.ClassroomRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import com.example.data.auth.AuthManager
import com.example.data.repository.SyncRepository
import com.example.data.repository.UpdateRepository
import com.example.ui.screens.DateRangeOption
import com.example.ui.screens.ReportType
import com.example.util.PdfReportExporter
import com.example.util.SpreadsheetUtils
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

import com.example.data.entity.ClassScheduleEntity
import com.example.data.repository.ScheduleRepository
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class ClassroomViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ClassroomRepository
    private val scheduleRepository: ScheduleRepository
    private val db = AppDatabase.getInstance(application)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val authManager = AuthManager(application)
    private val updateRepository = UpdateRepository()

    private val _uiState = MutableStateFlow(
        LmsUiState(
            selectedDate = dateFormat.format(Date()),
            activeHomeworkTopic = "Daily Homework Review",
        )
    )
    val uiState: StateFlow<LmsUiState> = _uiState.asStateFlow()

    private val _selectedClassroomId = MutableStateFlow<Long?>(null)

    init {
        repository = ClassroomRepository(db)
        scheduleRepository = ScheduleRepository(db.classScheduleDao())

        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }

        // Observe classrooms
        viewModelScope.launch {
            repository.getAllClassrooms().collect { classrooms ->
                _uiState.update { it.copy(classrooms = classrooms) }
                if ((_selectedClassroomId.value == null) && classrooms.isNotEmpty()) {
                    _selectedClassroomId.value = classrooms.first().id
                }
            }
        }

        // Full roster across all classrooms
        viewModelScope.launch {
            repository.getAllStudents().collect { all ->
                _uiState.update { it.copy(allStudents = all) }
            }
        }

        // Master Schedule across ALL classrooms
        viewModelScope.launch {
            scheduleRepository.getAllSchedules().collect { masterSchedules ->
                _uiState.update { it.copy(allSchedules = masterSchedules) }
            }
        }

        // React to selected classroom changes
        viewModelScope.launch {
            _selectedClassroomId.flatMapLatest { id ->
                if (id == null) flowOf(null to emptyList<StudentEntity>())
                else {
                    combine(
                        repository.getClassroomById(id),
                        repository.getStudents(id),
                        repository.getAssignments(id),
                        repository.getSubmissionsByClassroom(id),
                        repository.getAllHomeworkRecordsForClassroom(id),
                        repository.getAttendanceForClassroom(id),
                        repository.getInterventionsByClassroom(id),
                        repository.getLessonPlans(id),
                        repository.getDailyLogs(id),
                        scheduleRepository.getSchedulesByClassroom(id)
                    ) { args: Array<Any?> ->
                        @Suppress("UNCHECKED_CAST")
                        CombinedClassData(
                            classroom = args[0] as ClassroomEntity?,
                            students = args[1] as List<StudentEntity>,
                            assignments = args[2] as List<AssignmentEntity>,
                            submissions = args[3] as List<SubmissionEntity>,
                            homeworks = args[4] as List<HomeworkRecordEntity>,
                            attendance = args[5] as List<AttendanceRecordEntity>,
                            interventions = args[6] as List<InterventionEntity>,
                            lessonPlans = args[7] as List<LessonPlanEntity>,
                            dailyLogs = args[8] as List<DailyLogEntity>,
                            schedules = args[9] as List<ClassScheduleEntity>
                        )
                    }
                }
            }.collect { combined ->
                if (combined is CombinedClassData) {
                    val activeClassroom = combined.classroom
                    val students = combined.students
                    val assignments = combined.assignments
                    val submissions = combined.submissions
                    val homeworks = combined.homeworks
                    val attendance = combined.attendance
                    val interventions = combined.interventions
                    val lessonPlans = combined.lessonPlans
                    val dailyLogs = combined.dailyLogs
                    val schedules = combined.schedules

                    // Compute Grade Summaries
                    val studentSummaries = computeStudentGrades(students, assignments, submissions, homeworks, attendance)
                    val analytics = computeClassAnalytics(studentSummaries, assignments, submissions, attendance, interventions)
                    val attendanceReport = computeAttendanceReport(activeClassroom, students, attendance)
                    val homeworkDays = computeHomeworkDays(students, homeworks)

                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            activeClassroom = activeClassroom,
                            students = students,
                            assignments = assignments,
                            submissions = submissions,
                            homeworkRecords = homeworks,
                            attendanceRecords = attendance,
                            interventions = interventions,
                            lessonPlans = lessonPlans,
                            dailyLogs = dailyLogs,
                            schedules = schedules,
                            studentGradeSummaries = studentSummaries,
                            analytics = analytics,
                            attendanceReport = attendanceReport,
                            homeworkCheckDays = homeworkDays
                        )
                    }
                }
            }
        }
    }

    private data class CombinedClassData(
        val classroom: ClassroomEntity?,
        val students: List<StudentEntity>,
        val assignments: List<AssignmentEntity>,
        val submissions: List<SubmissionEntity>,
        val homeworks: List<HomeworkRecordEntity>,
        val attendance: List<AttendanceRecordEntity>,
        val interventions: List<InterventionEntity>,
        val lessonPlans: List<LessonPlanEntity>,
        val dailyLogs: List<DailyLogEntity>,
        val schedules: List<ClassScheduleEntity>
    )

    // --- NAVIGATION & TABS ---
    // ==========================================
    // UI Events & State Mutators
    // ==========================================

    fun syncGoogleSheet(classroomId: Long) {
        val classroom = uiState.value.activeClassroom ?: return
        if (classroom.linkedSpreadsheetId.isNullOrBlank()) {
            _uiState.update { it.copy(userNotificationMessage = "No Google Sheet linked to this classroom") }
            return
        }

        val cleanSpreadsheetId = SpreadsheetUtils.extractSpreadsheetId(classroom.linkedSpreadsheetId)

        viewModelScope.launch {
            try {
                if (!authManager.isUserSignedIn()) {
                    _uiState.update { it.copy(userNotificationMessage = "Please sign in with Google first (open navigation menu).") }
                    return@launch
                }

                val token = authManager.getAccessToken()
                if (token == null) {
                    _uiState.update { it.copy(userNotificationMessage = "Failed to get Google Token. Please sign out and sign in again to grant Sheets permission.") }
                    return@launch
                }
                
                _uiState.update { it.copy(userNotificationMessage = "Syncing roster with Google Sheets...") }
                
                val syncRepo = SyncRepository(db, token)
                // 1. Pull Students (Roster)
                val pullResult = syncRepo.syncClassroomRoster(classroomId, cleanSpreadsheetId)
                
                // Trigger a refresh of the UI state to ensure the new students show up immediately
                _selectedClassroomId.value = null
                _selectedClassroomId.value = classroomId
                
                _uiState.update { it.copy(userNotificationMessage = "Pushing attendance, homework & grades to Google Sheets...") }
                
                // 2. Push Grades, Attendance, and Homework
                val pushResult = syncRepo.exportToSheets(classroomId, cleanSpreadsheetId)
                
                _uiState.update { it.copy(userNotificationMessage = "Sync Complete: $pullResult | $pushResult") }
            } catch (e: Exception) {
                _uiState.update { it.copy(userNotificationMessage = "Sync failed: ${e.message}") }
            }
        }
    }

    fun linkSpreadsheet(classroomId: Long, spreadsheetId: String) {
        viewModelScope.launch {
            val classroom = db.classroomDao().getClassroomByIdOnce(classroomId) ?: return@launch
            val cleanId = SpreadsheetUtils.extractSpreadsheetId(spreadsheetId).ifBlank { null }
            val updated = classroom.copy(linkedSpreadsheetId = cleanId)
            repository.updateClassroom(updated)
            if (cleanId != null) {
                syncGoogleSheet(classroomId)
            } else {
                _uiState.update { it.copy(userNotificationMessage = "Unlinked Google Sheet from classroom") }
            }
        }
    }

    fun getAuthManager() = authManager

    fun selectTab(tab: LmsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    // selectClassroom(ClassroomEntity) removed as it was unused.

    fun selectClassroom(id: Long) {
        _selectedClassroomId.value = id
        _uiState.update { it.copy(isClassroomModalOpen = false) }
    }

    // --- SCHEDULE ACTIONS ---

    fun openScheduleScreen() {
        _uiState.update { it.copy(isScheduleScreenOpen = true) }
    }

    fun closeScheduleScreen() {
        _uiState.update { it.copy(isScheduleScreenOpen = false) }
    }

    fun saveScheduleBatch(
        baseEntry: ClassScheduleEntity,
        selectedDays: List<String>,
        applyToAllWeeks: Boolean,
        updateAllWeeksForSubject: Boolean = false
    ) {
        viewModelScope.launch {
            val targetWeeks = if (applyToAllWeeks || updateAllWeeksForSubject) (1..5).toList() else listOf(baseEntry.weekNumber)
            val targetDays = selectedDays.ifEmpty { listOf(baseEntry.dayOfWeek) }

            val batchEntries = mutableListOf<ClassScheduleEntity>()
            for (week in targetWeeks) {
                for (day in targetDays) {
                    batchEntries.add(
                        baseEntry.copy(
                            id = if (week == baseEntry.weekNumber && day.equals(baseEntry.dayOfWeek, ignoreCase = true)) baseEntry.id else 0,
                            weekNumber = week,
                            dayOfWeek = day
                        )
                    )
                }
            }

            if (updateAllWeeksForSubject) {
                scheduleRepository.updateSubjectScheduleAcrossWeeks(baseEntry.classroomName, targetWeeks, batchEntries)
                _uiState.update { it.copy(userNotificationMessage = "Updated routine for '${baseEntry.classroomName}' across all weeks") }
            } else {
                scheduleRepository.saveBatchScheduleEntries(batchEntries)
                val msg = if (batchEntries.size > 1) "Saved ${batchEntries.size} class periods across selected days/weeks" else "Class schedule entry saved"
                _uiState.update { it.copy(userNotificationMessage = msg) }
            }
        }
    }

    fun replicateWeekSchedule(fromWeek: Int = 1, targetWeeks: List<Int> = listOf(2, 3, 4, 5)) {
        viewModelScope.launch {
            scheduleRepository.replicateWeekSchedule(fromWeek, targetWeeks)
            _uiState.update { it.copy(userNotificationMessage = "Copied Week $fromWeek schedule routine to Weeks 2–5") }
        }
    }

    fun clearWeekSchedule(weekNumber: Int) {
        viewModelScope.launch {
            scheduleRepository.deleteSchedulesForWeek(weekNumber)
            _uiState.update { it.copy(userNotificationMessage = "Cleared Week $weekNumber schedule routine") }
        }
    }

    fun deleteScheduleEntry(entry: ClassScheduleEntity) {
        viewModelScope.launch {
            scheduleRepository.deleteScheduleEntry(entry)
            _uiState.update { it.copy(userNotificationMessage = "Schedule entry deleted") }
        }
    }

    /**
     * Returns Pair(isHoliday, summaryMessage) across ALL master subjects
     */
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

        if (dayOfWeek == "SATURDAY" || dayOfWeek == "SUNDAY") {
            return Pair(true, "Today is Holiday, Relax 😊")
        }

        // Find today's classes across ALL classrooms from master schedules
        val masterSchedules = uiState.value.allSchedules.ifEmpty { uiState.value.schedules }
        val todaySchedules = masterSchedules
            .filter { it.dayOfWeek.equals(dayOfWeek, ignoreCase = true) }
            .distinctBy { "${it.classroomName}_${it.startMinutes}" }
            .sortedBy { it.startMinutes }

        if (todaySchedules.isEmpty()) {
            return Pair(false, "No classes today.")
        }

        val formattedClasses = todaySchedules.joinToString(", ") { entry ->
            "\"${entry.classroomName}\" @ ${entry.startTime}"
        }
        return Pair(false, "Today you have classes in $formattedClasses.")
    }

    fun closeDialogs() {
        _uiState.update {
            it.copy(
                isAddEditClassroomOpen = false,
                editingClassroom = null,
                isAddEditStudentOpen = false,
                editingStudent = null,
                isAddEditAssignmentOpen = false,
                editingAssignment = null,
                isGradingSubmissionOpen = false,
                gradingSubmission = null,
                gradingAssignment = null,
                gradingStudent = null,
                selectedStudentProfile = null,
                isClassroomModalOpen = false,
                isStudentRosterOpen = false,
                isExportReportOpen = false,
                isAddInterventionOpen = false,
                editingIntervention = null,
                interventionStudent = null
            )
        }
    }

    // setSelectedDate(String) removed as it was unused.

    fun setActiveHomeworkTopic(topic: String) {
        _uiState.update { it.copy(activeHomeworkTopic = topic) }
    }

    fun setAssignmentFilter(type: AssignmentType?, status: SubmissionStatus?) {
        _uiState.update { it.copy(assignmentFilterType = type, assignmentFilterStatus = status) }
    }

    // setSearchQuery(String) removed as it was unused.

    // --- ANALYTICS CONTROLS ---
    fun setAnalyticsSelectedStudent(studentId: Long?) {
        _uiState.update { it.copy(analyticsSelectedStudentId = studentId) }
    }

    fun setAnalyticsTierFilter(tier: StudentTier?) {
        _uiState.update { it.copy(analyticsTierFilter = tier) }
    }

    fun setAnalyticsViewSection(section: Int) {
        _uiState.update { it.copy(analyticsViewSection = section) }
    }

    // --- MODAL CONTROLS ---
    fun setClassroomModalOpen(open: Boolean) {
        _uiState.update { it.copy(isClassroomModalOpen = open) }
    }

    fun openAddClassroom() {
        _uiState.update { it.copy(isAddEditClassroomOpen = true, editingClassroom = null) }
    }

    fun openEditClassroom(classroom: ClassroomEntity) {
        _uiState.update { it.copy(isAddEditClassroomOpen = true, editingClassroom = classroom) }
    }

    fun closeAddEditClassroom() {
        _uiState.update { it.copy(isAddEditClassroomOpen = false, editingClassroom = null) }
    }

    fun openAddStudent() {
        _uiState.update { it.copy(isAddEditStudentOpen = true, editingStudent = null) }
    }

    fun openEditStudent(student: StudentEntity) {
        _uiState.update { it.copy(isAddEditStudentOpen = true, editingStudent = student) }
    }

    fun closeAddEditStudent() {
        _uiState.update { it.copy(isAddEditStudentOpen = false, editingStudent = null) }
    }

    fun openAddAssignment() {
        _uiState.update { it.copy(isAddEditAssignmentOpen = true, editingAssignment = null) }
    }

    fun openEditAssignment(assignment: AssignmentEntity) {
        _uiState.update { it.copy(isAddEditAssignmentOpen = true, editingAssignment = assignment) }
    }

    fun closeAddEditAssignment() {
        _uiState.update { it.copy(isAddEditAssignmentOpen = false, editingAssignment = null) }
    }

    fun openGradingDialog(submission: SubmissionEntity, assignment: AssignmentEntity, student: StudentEntity) {
        _uiState.update {
            it.copy(
                isGradingSubmissionOpen = true,
                gradingSubmission = submission,
                gradingAssignment = assignment,
                gradingStudent = student
            )
        }
    }

    fun closeGradingDialog() {
        _uiState.update {
            it.copy(
                isGradingSubmissionOpen = false,
                gradingSubmission = null,
                gradingAssignment = null,
                gradingStudent = null
            )
        }
    }

    fun openStudentProfile(summary: StudentGradeSummary) {
        _uiState.update { it.copy(selectedStudentProfile = summary) }
    }

    fun closeStudentProfile() {
        _uiState.update { it.copy(selectedStudentProfile = null) }
    }

    // --- INTERVENTIONS CONTROLS ---
    fun openAddIntervention(student: StudentEntity? = null) {
        _uiState.update {
            it.copy(
                isAddInterventionOpen = true,
                editingIntervention = null,
                interventionStudent = student ?: it.students.firstOrNull()
            )
        }
    }

    // openEditIntervention(InterventionEntity) removed as it was unused.

    fun closeInterventionDialog() {
        _uiState.update {
            it.copy(
                isAddInterventionOpen = false,
                editingIntervention = null,
                interventionStudent = null
            )
        }
    }

    fun saveIntervention(
        studentId: Long,
        type: InterventionType,
        title: String,
        notes: String,
        date: String,
        resolved: Boolean
    ) {
        val classroomId = _selectedClassroomId.value ?: return
        viewModelScope.launch {
            val current = _uiState.value.editingIntervention
            if (current == null) {
                val newIntervention = InterventionEntity(
                    studentId = studentId,
                    classroomId = classroomId,
                    date = date.ifEmpty { dateFormat.format(Date()) },
                    type = type,
                    title = title,
                    notes = notes,
                    resolved = resolved
                )
                repository.saveIntervention(newIntervention)
                showToast("Intervention logged for teacher record")
            } else {
                val updated = current.copy(
                    studentId = studentId,
                    date = date.ifEmpty { current.date },
                    type = type,
                    title = title,
                    notes = notes,
                    resolved = resolved
                )
                repository.updateIntervention(updated)
                showToast("Updated intervention record")
            }
            closeInterventionDialog()
        }
    }

    // toggleInterventionResolved(InterventionEntity) removed as it was unused.

    fun deleteIntervention(id: Long) {
        viewModelScope.launch {
            repository.deleteIntervention(id)
            showToast("Intervention record deleted")
        }
    }

    fun exportPdfPortfolio(context: Context, summary: StudentGradeSummary) {
        val classroomName = uiState.value.activeClassroom?.name ?: "Classroom"
        PdfReportExporter.exportStudentPortfolio(context, summary, classroomName)
    }

    fun openExportReport(content: String, title: String = "Class Progress & Intervention Report") {
        _uiState.update { it.copy(isExportReportOpen = true, exportReportContent = content, exportReportTitle = title) }
    }

    fun closeExportReport() {
        _uiState.update { it.copy(isExportReportOpen = false, exportReportContent = "", exportReportTitle = "") }
    }

    fun openPlanner() {
        _uiState.update { it.copy(isPlannerOpen = true) }
    }

    fun closePlanner() {
        _uiState.update { it.copy(isPlannerOpen = false) }
    }

    fun checkForUpdates(isManual: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingForUpdates = true) }
            val result = updateRepository.checkLatestRelease()
            result.onSuccess { updateInfo ->
                _uiState.update {
                    it.copy(
                        isCheckingForUpdates = false,
                        updateInfo = updateInfo,
                        isUpdateDialogOpen = updateInfo.isUpdateAvailable
                    )
                }
                if (!updateInfo.isUpdateAvailable && isManual) {
                    showToast("Classroom LMS is up to date (${updateInfo.currentVersionName})")
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isCheckingForUpdates = false) }
                if (isManual) {
                    showToast("Unable to check for updates: ${error.localizedMessage ?: "Network error"}")
                }
            }
        }
    }

    fun closeUpdateDialog() {
        _uiState.update { it.copy(isUpdateDialogOpen = false) }
    }

    // --- LESSON PLANNER & DAILY DIARY ACTIONS ---
    fun openAddLessonPlan() {
        _uiState.update { it.copy(isAddEditLessonPlanOpen = true, editingLessonPlan = null) }
    }

    fun openEditLessonPlan(plan: LessonPlanEntity) {
        _uiState.update { it.copy(isAddEditLessonPlanOpen = true, editingLessonPlan = plan) }
    }

    fun closeLessonPlanDialog() {
        _uiState.update { it.copy(isAddEditLessonPlanOpen = false, editingLessonPlan = null) }
    }

    fun saveLessonPlan(
        unitTitle: String,
        description: String,
        targetDate: Long,
        status: String,
        targetClassroomId: Long? = null
    ) {
        val classId = targetClassroomId ?: uiState.value.activeClassroom?.id ?: return
        viewModelScope.launch {
            val editing = uiState.value.editingLessonPlan
            if (editing != null) {
                val updated = editing.copy(
                    classroomId = classId,
                    unitTitle = unitTitle,
                    description = description,
                    targetDate = targetDate,
                    status = status
                )
                repository.updateLessonPlan(updated)
                showToast("Lesson plan updated")
            } else {
                val newPlan = LessonPlanEntity(
                    classroomId = classId,
                    unitTitle = unitTitle,
                    description = description,
                    targetDate = targetDate,
                    status = status
                )
                repository.saveLessonPlan(newPlan)
                showToast("Lesson plan created")
            }
            closeLessonPlanDialog()
        }
    }

    fun updateLessonPlanStatus(plan: LessonPlanEntity, newStatus: String) {
        viewModelScope.launch {
            repository.updateLessonPlan(plan.copy(status = newStatus))
            showToast("Plan status set to $newStatus")
        }
    }

    fun deleteLessonPlan(plan: LessonPlanEntity) {
        viewModelScope.launch {
            repository.deleteLessonPlan(plan)
            showToast("Lesson plan deleted")
        }
    }

    fun openAddDailyLog() {
        _uiState.update { it.copy(isAddEditDailyLogOpen = true, editingDailyLog = null) }
    }

    fun openEditDailyLog(log: DailyLogEntity) {
        _uiState.update { it.copy(isAddEditDailyLogOpen = true, editingDailyLog = log) }
    }

    fun closeDailyLogDialog() {
        _uiState.update { it.copy(isAddEditDailyLogOpen = false, editingDailyLog = null) }
    }

    fun saveDailyLog(
        date: Long,
        reflectionNotes: String,
        wasProxyClass: Boolean,
        targetClassroomId: Long? = null
    ) {
        val classId = targetClassroomId ?: uiState.value.activeClassroom?.id ?: return
        viewModelScope.launch {
            val editing = uiState.value.editingDailyLog
            if (editing != null) {
                val updated = editing.copy(
                    classroomId = classId,
                    date = date,
                    reflectionNotes = reflectionNotes,
                    wasProxyClass = wasProxyClass
                )
                repository.updateDailyLog(updated)
                showToast("Daily diary log updated")
            } else {
                val newLog = DailyLogEntity(
                    classroomId = classId,
                    date = date,
                    reflectionNotes = reflectionNotes,
                    wasProxyClass = wasProxyClass
                )
                repository.saveDailyLog(newLog)
                showToast("Daily diary entry added")
            }
            closeDailyLogDialog()
        }
    }

    fun deleteDailyLog(log: DailyLogEntity) {
        viewModelScope.launch {
            repository.deleteDailyLog(log)
            showToast("Daily diary entry deleted")
        }
    }

    fun generateLessonPlansReportText(): String {
        val activeClass = uiState.value.activeClassroom
        val plans = uiState.value.lessonPlans
        val df = SimpleDateFormat("MMM dd, yyyy", Locale.US)

        val sb = StringBuilder()
        sb.appendLine("TEACHER LESSON PLANNER REPORT")
        sb.appendLine("Classroom: ${activeClass?.name ?: "All Classes"} (${activeClass?.subject ?: ""})")
        sb.appendLine("Total Plans: ${plans.size}")
        sb.appendLine("==========================================")
        sb.appendLine()

        if (plans.isEmpty()) {
            sb.appendLine("No lesson plans recorded.")
        } else {
            plans.forEachIndexed { idx, plan ->
                sb.appendLine("${idx + 1}. [${plan.status}] ${plan.unitTitle}")
                sb.appendLine("   Target Date: ${df.format(Date(plan.targetDate))}")
                sb.appendLine("   Objectives & Notes:")
                sb.appendLine("   ${plan.description}")
                sb.appendLine("------------------------------------------")
            }
        }
        return sb.toString()
    }

    fun generateDailyDiaryReportText(): String {
        val activeClass = uiState.value.activeClassroom
        val logs = uiState.value.dailyLogs
        val df = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US)

        val sb = StringBuilder()
        sb.appendLine("TEACHER DAILY DIARY LOG")
        sb.appendLine("Classroom: ${activeClass?.name ?: "All Classes"} (${activeClass?.subject ?: ""})")
        sb.appendLine("Total Entries: ${logs.size}")
        sb.appendLine("==========================================")
        sb.appendLine()

        if (logs.isEmpty()) {
            sb.appendLine("No diary entries recorded.")
        } else {
            logs.forEachIndexed { idx, log ->
                val proxyTag = if (log.wasProxyClass) " [PROXY CLASS]" else ""
                sb.appendLine("${idx + 1}. ${df.format(Date(log.date))}$proxyTag")
                sb.appendLine("   Reflection & Observations:")
                sb.appendLine("   ${log.reflectionNotes}")
                sb.appendLine("------------------------------------------")
            }
        }
        return sb.toString()
    }

    fun exportLessonPlansPdf(context: Context) {
        val activeClass = uiState.value.activeClassroom
        val reportText = generateLessonPlansReportText()
        val title = "Lesson Plans - ${activeClass?.name ?: "Classroom"}"
        PdfReportExporter.exportAndShare(context, title, reportText, "LessonPlans_${activeClass?.name ?: "Class"}")
    }

    fun exportDailyDiaryPdf(context: Context) {
        val activeClass = uiState.value.activeClassroom
        val reportText = generateDailyDiaryReportText()
        val title = "Daily Diary - ${activeClass?.name ?: "Classroom"}"
        PdfReportExporter.exportAndShare(context, title, reportText, "DailyDiary_${activeClass?.name ?: "Class"}")
    }

    // --- CUSTOM REPORT GENERATOR CENTER ---
    fun openGenerateReportDialog() {
        _uiState.update { it.copy(isGenerateReportDialogOpen = true) }
    }

    fun closeGenerateReportDialog() {
        _uiState.update { it.copy(isGenerateReportDialogOpen = false) }
    }

    fun setPlannerClassroomFilter(classroomId: Long?) {
        _uiState.update { it.copy(plannerClassroomFilterId = classroomId) }
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
        val targetClass = targetClassroomId?.let { id -> uiState.value.classrooms.find { it.id == id } }
            ?: uiState.value.activeClassroom

        val className = if (targetClassroomId == null) "All Classrooms" else "${targetClass?.name ?: "Classroom"} (${targetClass?.subject ?: ""})"

        val sb = StringBuilder()
        sb.appendLine("CLASSROOM LMS CUSTOM REPORT")
        sb.appendLine("Report Type: ${reportType.title}")
        sb.appendLine("Target Scope: $className")
        sb.appendLine("Date Filter: ${dateRange.label}")
        sb.appendLine("==========================================")
        sb.appendLine()

        when (reportType) {
            ReportType.ANALYTICS_SUMMARY -> {
                sb.append(generateProgressAnalyticsReportText())
            }
            ReportType.GRADEBOOK_FULL -> {
                sb.append(generateFormattedGradebookReportText())
            }
            ReportType.ATTENDANCE_LOG -> {
                sb.append(generateFormattedAttendanceReportText())
            }
            ReportType.LESSON_PLANS -> {
                sb.append(generateLessonPlansReportText())
            }
            ReportType.DAILY_DIARY -> {
                sb.append(generateDailyDiaryReportText())
            }
        }

        if (includeAtRisk) {
            sb.appendLine()
            sb.appendLine("== AT-RISK & SUPPORT NEEDED ALERTS ==")
            val atRisk = uiState.value.analytics.atRiskStudents
            if (atRisk.isEmpty()) {
                sb.appendLine("All students currently on track.")
            } else {
                atRisk.forEach { s ->
                    sb.appendLine("• ${s.student.name}: ${s.riskReasons.joinToString(", ")}")
                }
            }
        }

        if (includeDifficulty) {
            sb.appendLine()
            sb.appendLine("== HIGH DIFFICULTY ASSIGNMENT AREAS ==")
            val diffs = uiState.value.analytics.difficultyAreas
            if (diffs.isEmpty()) {
                sb.appendLine("No high difficulty assignments flagged.")
            } else {
                diffs.forEach { area ->
                    sb.appendLine("• ${area.assignment.title}: Avg ${area.averagePercentage.toInt()}% - ${area.teacherActionRecommendation}")
                }
            }
        }

        if (includeDistribution) {
            sb.appendLine()
            sb.appendLine("== GRADE DISTRIBUTION BREAKDOWN ==")
            val dist = uiState.value.analytics.distribution
            sb.appendLine("A: ${dist.aCount} | B: ${dist.bCount} | C: ${dist.cCount} | D: ${dist.dCount} | F: ${dist.fCount}")
        }

        if (includeAttendance) {
            sb.appendLine()
            sb.appendLine("== ATTENDANCE SUMMARY ==")
            val att = uiState.value.attendanceReport
            if (att != null) {
                sb.appendLine("Overall Attendance Rate: ${att.overallAttendanceRate.toInt()}%")
                sb.appendLine("Present: ${att.totalPresent} | Absent: ${att.totalAbsent} | Tardy: ${att.totalTardy} | Excused: ${att.totalExcused}")
            }
        }

        return sb.toString()
    }

    fun generateAndShareCustomPdf(
        context: Context,
        reportType: ReportType,
        targetClassroomId: Long?,
        dateRange: DateRangeOption,
        includeAtRisk: Boolean,
        includeDifficulty: Boolean,
        includeDistribution: Boolean,
        includeAttendance: Boolean
    ) {
        val reportText = generateCustomReportText(
            reportType, targetClassroomId, dateRange, includeAtRisk, includeDifficulty, includeDistribution, includeAttendance
        )
        val title = "${reportType.title} Report"
        PdfReportExporter.exportAndShare(context, title, reportText, "CustomReport_${reportType.name}")
        closeGenerateReportDialog()
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(userNotificationMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(userNotificationMessage = null) }
    }

    fun clearNotificationMessage() {
        clearToast()
    }

    fun openClassroomManagement() {
        setClassroomModalOpen(open = true)
    }

    fun openStudentRoster() {
        _uiState.update { it.copy(isStudentRosterOpen = true) }
    }

    fun closeStudentRoster() {
        _uiState.update { it.copy(isStudentRosterOpen = false) }
    }

    fun reassignStudentClassroom(student: StudentEntity, newClassroomId: Long) {
        if (student.classroomId == newClassroomId) return
        viewModelScope.launch {
            val updated = student.copy(classroomId = newClassroomId)
            repository.updateStudent(updated)
            // Create pending submissions for assignments in the destination classroom
            val destAssignments = repository.getAssignmentsByClassroomOnce(newClassroomId)
            if (destAssignments.isNotEmpty()) {
                val existing = repository.getSubmissionsForStudentOnce(student.id)
                val existingIds = existing.asSequence().map { it.assignmentId }.toSet()
                val missing = destAssignments.filter { it.id !in existingIds }.map { a ->
                    SubmissionEntity(
                        assignmentId = a.id,
                        studentId = student.id,
                        classroomId = newClassroomId,
                        status = SubmissionStatus.PENDING
                    )
                }
                if (missing.isNotEmpty()) {
                    repository.insertOrUpdateSubmissions(missing)
                }
            }
            val className = _uiState.value.classrooms.find { it.id == newClassroomId }?.name ?: "new class"
            showToast("Moved ${student.name} to $className")
        }
    }

    fun generateStudentPortfolioReportText(studentId: Long): String {
        val summary = _uiState.value.studentGradeSummaries.find { it.student.id == studentId }
            ?: return "Student not found."
        return generateStudentIndividualReportText(summary)
    }

    // saveIntervention(InterventionEntity) removed as it was unused.

    // --- CRUD OPERATIONS ---

    fun saveClassroom(classroom: ClassroomEntity) {
        viewModelScope.launch {
            if (classroom.id == 0L) {
                val newId = repository.insertClassroom(classroom)
                _selectedClassroomId.value = newId
                showToast("Created classroom: ${classroom.name}")
            } else {
                repository.updateClassroom(classroom)
                showToast("Updated classroom: ${classroom.name}")
            }
            closeAddEditClassroom()
        }
    }

    // saveClassroom(String, ...) removed as it was unused.

    fun deleteClassroom(id: Long) {
        viewModelScope.launch {
            repository.deleteClassroom(id)
            val remaining = _uiState.value.classrooms.filter { it.id != id }
            if (remaining.isNotEmpty()) {
                _selectedClassroomId.value = remaining.first().id
            } else {
                _selectedClassroomId.value = null
            }
            showToast("Classroom deleted")
        }
    }

    fun saveStudent(student: StudentEntity) {
        val classroomId = _selectedClassroomId.value ?: return
        viewModelScope.launch {
            if (student.id == 0L) {
                val sId = repository.insertStudent(student.copy(classroomId = classroomId))
                val assignments = _uiState.value.assignments
                val newSubmissions = assignments.map { a ->
                    SubmissionEntity(
                        assignmentId = a.id,
                        studentId = sId,
                        classroomId = classroomId,
                        status = SubmissionStatus.PENDING
                    )
                }
                if (newSubmissions.isNotEmpty()) {
                    repository.insertOrUpdateSubmissions(newSubmissions)
                }
                showToast("Added student: ${student.name}")
            } else {
                repository.updateStudent(student)
                showToast("Updated student: ${student.name}")
            }
            closeAddEditStudent()
        }
    }

    // saveStudent(String, ...) removed as it was unused.

    fun deleteStudent(id: Long) {
        viewModelScope.launch {
            repository.deleteStudent(id)
            showToast("Student removed from class")
            if (_uiState.value.selectedStudentProfile?.student?.id == id) {
                closeStudentProfile()
            }
        }
    }

    fun saveAssignment(assignment: AssignmentEntity) {
        val classroomId = _selectedClassroomId.value ?: return
        viewModelScope.launch {
            if (assignment.id == 0L) {
                repository.insertAssignment(assignment.copy(classroomId = classroomId), autoCreateSubmissions = true)
                showToast("Created assignment: ${assignment.title}")
            } else {
                repository.updateAssignment(assignment)
                showToast("Updated assignment: ${assignment.title}")
            }
            closeAddEditAssignment()
        }
    }

    // saveAssignment(String, ...) removed as it was unused.

    fun deleteAssignment(id: Long) {
        viewModelScope.launch {
            repository.deleteAssignment(id)
            showToast("Assignment deleted")
        }
    }

    fun saveSubmission(submission: SubmissionEntity) {
        viewModelScope.launch {
            repository.updateSubmission(submission)
            showToast("Grade saved for submission")
            closeGradingDialog()
        }
    }

    // saveSubmissionGrade(...) removed as it was unused.

    // quickGradeSubmission(...) removed as it was unused.

    // markSubmissionStatus(...) removed as it was unused.

    fun toggleSubmissionCheck(submission: SubmissionEntity) {
        viewModelScope.launch {
            val updated = submission.copy(
                isChecked = !submission.isChecked,
                status = if (!submission.isChecked && submission.status == SubmissionStatus.PENDING) SubmissionStatus.SUBMITTED else submission.status,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateSubmission(updated)
        }
    }

    fun quickMarkAllSubmissionsForAssignment(assignmentId: Long, status: SubmissionStatus) {
        markAllSubmissionsAs(assignmentId, status)
    }

    fun markAllSubmissionsAs(assignmentId: Long, status: SubmissionStatus) {
        viewModelScope.launch {
            val relevant = _uiState.value.submissions.filter { it.assignmentId == assignmentId }
            val updated = relevant.map { sub ->
                sub.copy(
                    status = status,
                    isChecked = status == SubmissionStatus.GRADED || status == SubmissionStatus.SUBMITTED,
                    updatedAt = System.currentTimeMillis()
                )
            }
            repository.insertOrUpdateSubmissions(updated)
            showToast("Marked ${updated.size} submissions as ${status.displayName}")
        }
    }

    fun recordHomeworkStatus(studentId: Long, topic: String, date: String, status: HomeworkStatus, notes: String = "") {
        val classroomId = _selectedClassroomId.value ?: return
        viewModelScope.launch {
            val existing = _uiState.value.homeworkRecords.find {
                it.studentId == studentId && it.date == date && it.topic == topic
            }
            if (existing != null) {
                val updated = existing.copy(status = status, notes = notes.ifEmpty { existing.notes })
                repository.updateHomeworkRecord(updated)
            } else {
                val newRec = HomeworkRecordEntity(
                    classroomId = classroomId,
                    studentId = studentId,
                    date = date,
                    topic = topic,
                    status = status,
                    notes = notes
                )
                repository.saveHomeworkRecord(newRec)
            }
        }
    }

    fun markAllHomeworkAs(topic: String, date: String, status: HomeworkStatus) {
        val classroomId = _selectedClassroomId.value ?: return
        val students = _uiState.value.students
        viewModelScope.launch {
            val records = students.map { s ->
                val existing = _uiState.value.homeworkRecords.find { it.studentId == s.id && it.date == date && it.topic == topic }
                existing?.copy(status = status) ?: HomeworkRecordEntity(
                    classroomId = classroomId,
                    studentId = s.id,
                    date = date,
                    topic = topic,
                    status = status
                )
            }
            repository.saveHomeworkBatch(records)
            showToast("Marked all HW as ${status.displayName} for $topic")
        }
    }

    fun markAllHomeworkDone(date: String, topic: String) {
        markAllHomeworkAs(topic, date, HomeworkStatus.DONE)
    }

    fun cycleHomeworkStatus(studentId: Long, date: String, topic: String) {
        val classroomId = _selectedClassroomId.value ?: return
        viewModelScope.launch {
            val existing = _uiState.value.homeworkRecords.find {
                it.studentId == studentId && it.date == date && it.topic == topic
            }
            val nextStatus = when (existing?.status) {
                HomeworkStatus.DONE -> HomeworkStatus.PARTIAL
                HomeworkStatus.PARTIAL -> HomeworkStatus.MISSING
                HomeworkStatus.MISSING -> HomeworkStatus.EXCUSED
                HomeworkStatus.EXCUSED -> HomeworkStatus.DONE
                null -> HomeworkStatus.PARTIAL
            }
            recordHomeworkStatus(studentId, topic, date, nextStatus)
        }
    }

    // deleteHomeworkDay(...) removed as it was unused.

    fun recordAttendanceStatus(studentId: Long, date: String, status: AttendanceStatus, remarks: String = "") {
        val classroomId = _selectedClassroomId.value ?: return
        viewModelScope.launch {
            val existing = _uiState.value.attendanceRecords.find {
                it.studentId == studentId && it.date == date
            }
            if (existing != null) {
                val updated = existing.copy(status = status, remarks = remarks.ifEmpty { existing.remarks })
                repository.updateAttendanceRecord(updated)
            } else {
                val newRec = AttendanceRecordEntity(
                    classroomId = classroomId,
                    studentId = studentId,
                    date = date,
                    status = status,
                    remarks = remarks
                )
                repository.saveAttendanceRecord(newRec)
            }
        }
    }

    fun cycleAttendanceStatus(studentId: Long, date: String) {
        val classroomId = _selectedClassroomId.value ?: return
        viewModelScope.launch {
            val existing = _uiState.value.attendanceRecords.find {
                it.studentId == studentId && it.date == date
            }
            val nextStatus = when (existing?.status) {
                AttendanceStatus.PRESENT -> AttendanceStatus.ABSENT
                AttendanceStatus.ABSENT -> AttendanceStatus.TARDY
                AttendanceStatus.TARDY -> AttendanceStatus.EXCUSED
                AttendanceStatus.EXCUSED -> AttendanceStatus.PRESENT
                null -> AttendanceStatus.ABSENT
            }
            recordAttendanceStatus(studentId, date, nextStatus)
        }
    }

    fun markAllPresent(date: String) {
        markAllAttendanceAs(date, AttendanceStatus.PRESENT)
    }

    fun markAllAttendanceAs(date: String, status: AttendanceStatus) {
        val classroomId = _selectedClassroomId.value ?: return
        val students = _uiState.value.students
        viewModelScope.launch {
            val records = students.map { s ->
                val existing = _uiState.value.attendanceRecords.find { it.studentId == s.id && it.date == date }
                if (existing != null) {
                    existing.copy(status = status)
                } else {
                    AttendanceRecordEntity(
                        classroomId = classroomId,
                        studentId = s.id,
                        date = date,
                        status = status
                    )
                }
            }
            repository.saveAttendanceBatch(records)
            showToast("Marked all attendance as ${status.displayName} for $date")
        }
    }

    fun resetSampleData() {
        resetDemoData()
    }

    fun resetDemoData() {
        viewModelScope.launch {
            repository.resetWithSampleData()
            showToast("Database restored with fresh teacher records!")
        }
    }

    // --- REPORT EXPORT GENERATION ---

    fun generateProgressAnalyticsReportText(): String {
        val activeClass = _uiState.value.activeClassroom ?: return "No active classroom selected."
        val analytics = _uiState.value.analytics
        val interventions = _uiState.value.interventions
        val sb = StringBuilder()

        sb.append("====================================================\n")
        sb.append("      STUDENT PROGRESS ANALYTICS & TEACHER AUDIT    \n")
        sb.append("====================================================\n\n")
        sb.append("Classroom: ${activeClass.name} • Subject: ${activeClass.subject}\n")
        sb.append("Grade Level: ${activeClass.gradeLevel} | Room: ${activeClass.roomNumber}\n")
        sb.append("Report Date: ${dateFormat.format(Date())}\n\n")

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

    fun generateStudentIndividualReportText(summary: StudentGradeSummary): String {
        val activeClass = _uiState.value.activeClassroom ?: return "No active classroom."
        val student = summary.student
        val interventions = _uiState.value.interventions.filter { it.studentId == student.id }
        val trajectory = _uiState.value.analytics.studentTrajectories.find { it.student.id == student.id }

        val sb = StringBuilder()
        sb.append("====================================================\n")
        sb.append("          STUDENT PROGRESS PORTFOLIO REPORT         \n")
        sb.append("====================================================\n\n")
        sb.append("Student Name: ${student.name}\n")
        sb.append("Student ID: ${student.studentNumber} | Email: ${student.email}\n")
        sb.append("Guardian Contact: ${student.guardianContact.ifEmpty { "N/A" }}\n")
        sb.append("Class: ${activeClass.name} (${activeClass.subject})\n")
        sb.append("Academic Standing Tier: ${summary.tier.label}\n")
        sb.append("Progress Trajectory: ${trajectory?.trend?.label ?: "Steady"} (${if ((trajectory?.trendDelta ?: 0.0) >= 0) "+" else ""}${"%.1f".format(trajectory?.trendDelta ?: 0.0)}%)\n\n")

        sb.append("ACADEMIC PERFORMANCE SUMMARY\n")
        sb.append("----------------------------------------------------\n")
        sb.append("• Current Overall Score: ${"%.1f".format(summary.percentage)}%\n")
        sb.append("• Letter Grade: ${summary.letterGrade} (GPA ${"%.2f".format(summary.gpa)})\n")
        sb.append("• Points Earned: ${summary.earnedPoints.toInt()} / ${summary.possiblePoints.toInt()} pts\n")
        sb.append("• Graded Submissions: ${summary.gradedCount} | Missing: ${summary.missingCount} | Pending: ${summary.pendingCount}\n")
        sb.append("• Homework Completion: ${"%.1f".format(summary.homeworkCompletionRate)}%\n")
        sb.append("• Attendance Record: ${"%.1f".format(summary.attendanceRate)}%\n\n")

        if (summary.riskReasons.isNotEmpty()) {
            sb.append("AREAS OF FOCUS & SUPPORT TRIGGERS\n")
            sb.append("----------------------------------------------------\n")
            summary.riskReasons.forEach { r -> sb.append("⚠️ $r\n") }
            sb.append("\n")
        }

        if (summary.enrichmentReasons.isNotEmpty()) {
            sb.append("STRENGTHS & ENRICHMENT MILESTONES\n")
            sb.append("----------------------------------------------------\n")
            summary.enrichmentReasons.forEach { e -> sb.append("🌟 $e\n") }
            sb.append("\n")
        }

        sb.append("TEACHER INTERVENTIONS & RECORD-KEEPING LOGS\n")
        sb.append("----------------------------------------------------\n")
        if (interventions.isNotEmpty()) {
            interventions.forEachIndexed { idx, inRec ->
                sb.append("${idx + 1}. [${inRec.date}] ${inRec.type.displayName}: ${inRec.title}\n")
                if (inRec.notes.isNotEmpty()) {
                    sb.append("   Notes: ${inRec.notes}\n")
                }
                sb.append("   Status: ${if (inRec.resolved) "Resolved / Completed" else "Active / In Progress"}\n\n")
            }
        } else {
            sb.append("No teacher intervention records logged to date.\n")
        }

        sb.append("\n====================================================\n")
        return sb.toString()
    }

    fun generateFormattedAttendanceReportText(): String {
        val report = _uiState.value.attendanceReport ?: return "No attendance data available."
        val sb = StringBuilder()
        sb.append("====================================================\n")
        sb.append("          CLASSROOM ATTENDANCE AUDIT REPORT         \n")
        sb.append("====================================================\n\n")
        sb.append("Class: ${report.classroomName} (${report.subject})\n")
        sb.append("Report Date: ${report.reportDate}\n")
        sb.append("Date Range: ${report.dateRangeText}\n")
        sb.append("Overall Rate: ${"%.1f".format(report.overallAttendanceRate)}%\n")
        sb.append("Total Records: ${report.totalRecords} (Present: ${report.totalPresent}, Absent: ${report.totalAbsent}, Tardy: ${report.totalTardy}, Excused: ${report.totalExcused})\n\n")

        sb.append(String.format(Locale.US, "%-22s | %-8s | %-6s | %-4s | %-4s | %-5s | %-7s\n", "Student Name", "ID", "Rate", "Pres", "Abs", "Tardy", "Status"))
        sb.append("-------------------------------------------------------------------------\n")
        report.studentSummaries.forEach { s ->
            val statusStr = if (s.attendanceRate < 85.0) "AT RISK" else if (s.attendanceRate == 100.0) "PERFECT" else "GOOD"
            sb.append(String.format(Locale.US, "%-22s | %-8s | %5.1f%% | %-4d | %-4d | %-5d | %-7s\n",
                s.student.name.take(22),
                s.student.studentNumber,
                s.attendanceRate,
                s.presentCount,
                s.absentCount,
                s.tardyCount,
                statusStr
            ))
        }

        if (report.chronicAbsentees.isNotEmpty()) {
            sb.append("\nCHRONIC ABSENTEEISM WATCHLIST (<85%):\n")
            report.chronicAbsentees.forEach { s ->
                sb.append(" ⚠️ ${s.student.name} (${s.student.studentNumber}) - ${"%.1f".format(s.attendanceRate)}% (${s.absentCount} abs, ${s.tardyCount} tardy)\n")
            }
        }
        sb.append("\n====================================================\n")
        return sb.toString()
    }

    fun generateFormattedGradebookReportText(): String {
        val activeClass = _uiState.value.activeClassroom ?: return "No active classroom."
        val analytics = _uiState.value.analytics
        val summaries = _uiState.value.studentGradeSummaries
        val sb = StringBuilder()
        sb.append("====================================================\n")
        sb.append("           CLASSROOM REAL-TIME GRADEBOOK            \n")
        sb.append("====================================================\n\n")
        sb.append("Class: ${activeClass.name} (${activeClass.subject})\n")
        sb.append("Class Average Score: ${"%.1f".format(analytics.averagePercentage)}% (GPA ${"%.2f".format(analytics.averageGpa)})\n")
        sb.append("Grade Distribution: A: ${analytics.distribution.aCount} | B: ${analytics.distribution.bCount} | C: ${analytics.distribution.cCount} | D: ${analytics.distribution.dCount} | F: ${analytics.distribution.fCount}\n\n")
        sb.append(String.format(Locale.US, "%-22s | %-8s | %-6s | %-4s | %-4s | %-7s | %-7s\n", "Student Name", "ID", "Score", "Ltr", "GPA", "HW %", "Att %"))
        sb.append("-------------------------------------------------------------------------\n")
        summaries.forEach { s ->
            sb.append(String.format(Locale.US, "%-22s | %-8s | %5.1f%% | %-4s | %-4.2f | %5.1f%% | %5.1f%%\n",
                s.student.name.take(22),
                s.student.studentNumber,
                s.percentage,
                s.letterGrade,
                s.gpa,
                s.homeworkCompletionRate,
                s.attendanceRate
            ))
        }
        sb.append("\n====================================================\n")
        return sb.toString()
    }

    // --- COMPUTATION HELPER LOGIC ---

    private fun computeStudentGrades(
        students: List<StudentEntity>,
        assignments: List<AssignmentEntity>,
        submissions: List<SubmissionEntity>,
        homeworks: List<HomeworkRecordEntity>,
        attendance: List<AttendanceRecordEntity>
    ): List<StudentGradeSummary> {
        val assignmentMap = assignments.associateBy { it.id }

        return students.map { student ->
            val studentSubs = submissions.filter { it.studentId == student.id }
            val studentHws = homeworks.filter { it.studentId == student.id }
            val studentAtt = attendance.filter { it.studentId == student.id }

            var earnedPoints = 0.0
            var possiblePoints = 0.0
            var gradedCount = 0
            var missingCount = 0
            var pendingCount = 0

            studentSubs.forEach { sub ->
                val assign = assignmentMap[sub.assignmentId]
                if (assign != null) {
                    when (sub.status) {
                        SubmissionStatus.GRADED -> {
                            gradedCount++
                            earnedPoints += (sub.score ?: 0.0)
                            possiblePoints += assign.maxPoints
                        }
                        SubmissionStatus.MISSING -> {
                            missingCount++
                            possiblePoints += assign.maxPoints
                        }
                        SubmissionStatus.PENDING, SubmissionStatus.SUBMITTED, SubmissionStatus.LATE -> {
                            pendingCount++
                        }
                    }
                }
            }

            val percentage = if (possiblePoints > 0) {
                (earnedPoints / possiblePoints) * 100.0
            } else 100.0

            val letterGrade = calculateLetterGrade(percentage)
            val gpa = calculateGpa(percentage)

            // Homework completion rate
            val hwTotal = studentHws.size
            val hwEarned = studentHws.sumOf { it.status.scoreMultiplier }
            val hwRate = if (hwTotal > 0) (hwEarned / hwTotal) * 100.0 else 100.0

            // Attendance rate
            val attTotal = studentAtt.size
            val attPresent = studentAtt.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.EXCUSED }
            val attRate = if (attTotal > 0) (attPresent.toDouble() / attTotal) * 100.0 else 100.0

            // Risk Assessment
            val riskReasons = mutableListOf<String>()
            if (percentage < 70.0 && possiblePoints > 0) riskReasons.add("Low Cumulative Grade (${"%.1f".format(percentage)}%)")
            if (attRate < 85.0 && attTotal >= 3) riskReasons.add("Low Attendance (${"%.1f".format(attRate)}%)")
            if (missingCount >= 2) riskReasons.add("$missingCount Missing Submissions")
            if (hwRate < 70.0 && hwTotal >= 3) riskReasons.add("Low Homework Completion (${"%.1f".format(hwRate)}%)")

            // Enrichment Assessment
            val enrichmentReasons = mutableListOf<String>()
            if (percentage >= 92.0 && gradedCount >= 2) enrichmentReasons.add("High Academic Mastery (${"%.1f".format(percentage)}%)")
            if (hwRate >= 95.0 && hwTotal >= 3) enrichmentReasons.add("Exceptional Homework Consistency (${"%.0f".format(hwRate)}%)")
            if (missingCount == 0 && gradedCount >= 3) enrichmentReasons.add("Zero Missing Assignments Streak")

            val tier = when {
                riskReasons.isNotEmpty() -> StudentTier.EXTRA_SUPPORT
                enrichmentReasons.isNotEmpty() && percentage >= 90.0 -> StudentTier.ENRICHMENT_NEEDED
                else -> StudentTier.ON_TRACK
            }

            StudentGradeSummary(
                student = student,
                earnedPoints = earnedPoints,
                possiblePoints = possiblePoints,
                percentage = (percentage * 10).roundToInt() / 10.0,
                letterGrade = letterGrade,
                gpa = (gpa * 100).roundToInt() / 100.0,
                submissions = studentSubs,
                gradedCount = gradedCount,
                missingCount = missingCount,
                pendingCount = pendingCount,
                homeworkCompletionRate = (hwRate * 10).roundToInt() / 10.0,
                attendanceRate = (attRate * 10).roundToInt() / 10.0,
                isAtRisk = tier == StudentTier.EXTRA_SUPPORT,
                isEnrichmentCandidate = tier == StudentTier.ENRICHMENT_NEEDED,
                riskReasons = riskReasons,
                enrichmentReasons = enrichmentReasons,
                tier = tier
            )
        }.sortedByDescending { it.percentage }
    }

    private fun calculateLetterGrade(percent: Double): String {
        return when {
            percent >= 97.0 -> "A+"
            percent >= 93.0 -> "A"
            percent >= 90.0 -> "A-"
            percent >= 87.0 -> "B+"
            percent >= 83.0 -> "B"
            percent >= 80.0 -> "B-"
            percent >= 77.0 -> "C+"
            percent >= 73.0 -> "C"
            percent >= 70.0 -> "C-"
            percent >= 60.0 -> "D"
            else -> "F"
        }
    }

    private fun calculateGpa(percent: Double): Double {
        return when {
            percent >= 93.0 -> 4.0
            percent >= 90.0 -> 3.7
            percent >= 87.0 -> 3.3
            percent >= 83.0 -> 3.0
            percent >= 80.0 -> 2.7
            percent >= 77.0 -> 2.3
            percent >= 73.0 -> 2.0
            percent >= 70.0 -> 1.7
            percent >= 60.0 -> 1.0
            else -> 0.0
        }
    }

    private fun computeClassAnalytics(
        summaries: List<StudentGradeSummary>,
        assignments: List<AssignmentEntity>,
        submissions: List<SubmissionEntity>,
        attendance: List<AttendanceRecordEntity>,
        interventions: List<InterventionEntity>
    ): ClassroomAnalytics {
        if (summaries.isEmpty()) return ClassroomAnalytics()

        val avgScore = summaries.map { it.percentage }.average()
        val avgGpa = summaries.map { it.gpa }.average()
        val highest = summaries.maxOfOrNull { it.percentage } ?: 0.0
        val lowest = summaries.minOfOrNull { it.percentage } ?: 0.0

        var a = 0
        var b = 0
        var c = 0
        var d = 0
        var f = 0

        summaries.forEach { s ->
            when (s.letterGrade.firstOrNull()) {
                'A' -> a++
                'B' -> b++
                'C' -> c++
                'D' -> d++
                'F' -> f++
            }
        }

        val pending = submissions.count { it.status == SubmissionStatus.SUBMITTED || it.status == SubmissionStatus.LATE }
        val todayStr = dateFormat.format(Date())
        val todayAtt = attendance.filter { it.date == todayStr }
        val todayRate = if (todayAtt.isNotEmpty()) {
            val presentCount = todayAtt.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.EXCUSED }
            (presentCount.toDouble() / todayAtt.size) * 100.0
        } else 100.0

        val atRisk = summaries.filter { it.tier == StudentTier.EXTRA_SUPPORT }
        val enrichment = summaries.filter { it.tier == StudentTier.ENRICHMENT_NEEDED }
        val onTrack = summaries.filter { it.tier == StudentTier.ON_TRACK }

        // 1. Compute Chronological Timeline Points
        val sortedAssignments = assignments.sortedBy { it.dueDate }
        val timelinePoints = sortedAssignments.mapNotNull { assign ->
            val assignSubs = submissions.filter { it.assignmentId == assign.id && it.status == SubmissionStatus.GRADED }
            if (assignSubs.isEmpty()) null
            else {
                val scoreMap = mutableMapOf<Long, Double>()
                assignSubs.forEach { sub ->
                    val p = if (assign.maxPoints > 0) ((sub.score ?: 0.0) / assign.maxPoints) * 100.0 else 0.0
                    scoreMap[sub.studentId] = (p * 10).roundToInt() / 10.0
                }
                val avg = if (scoreMap.isNotEmpty()) scoreMap.values.average() else 0.0
                val high = scoreMap.values.maxOrNull() ?: 0.0
                val low = scoreMap.values.minOrNull() ?: 0.0
                val isDiff = avg < 75.0 || scoreMap.values.count { it < 70.0 } >= 2

                AssignmentTimelinePoint(
                    assignmentId = assign.id,
                    title = assign.title,
                    dueDate = assign.dueDate,
                    type = assign.type,
                    maxPoints = assign.maxPoints,
                    classAveragePercentage = (avg * 10).roundToInt() / 10.0,
                    highestPercentage = high,
                    lowestPercentage = low,
                    studentScores = scoreMap,
                    isDifficult = isDiff
                )
            }
        }

        // 2. Compute Trajectory Trends for each student
        val studentTrajectories = summaries.map { summary ->
            val sId = summary.student.id
            val history = mutableListOf<Pair<String, Double>>()
            timelinePoints.forEach { point ->
                point.studentScores[sId]?.let { score ->
                    history.add(point.title to score)
                }
            }

            val trendDelta: Double
            val trendStatus: TrajectoryTrend
            if (history.size >= 2) {
                val mid = history.size / 2
                val earlier = history.take(mid.coerceAtLeast(1)).map { it.second }.average()
                val recent = history.takeLast(history.size - mid).map { it.second }.average()
                val diff = recent - earlier
                trendDelta = (diff * 10).roundToInt() / 10.0
                trendStatus = when {
                    trendDelta >= 3.0 -> TrajectoryTrend.IMPROVING
                    trendDelta <= -3.0 -> TrajectoryTrend.DECLINING
                    else -> TrajectoryTrend.STEADY
                }
            } else {
                trendDelta = 0.0
                trendStatus = TrajectoryTrend.STEADY
            }

            val studentInters = interventions.count { it.studentId == sId }
            val recommendations = mutableListOf<String>()
            if (summary.tier == StudentTier.EXTRA_SUPPORT) {
                recommendations.add("Schedule 1-on-1 Concept Review")
                recommendations.add("Provide Remedial Practice Packet")
                if (summary.homeworkCompletionRate < 75.0) recommendations.add("Guardian Contact for HW Accountability")
            } else if (summary.tier == StudentTier.ENRICHMENT_NEEDED) {
                recommendations.add("Assign Advanced Honors Extension Project")
                recommendations.add("Designate as Peer Study Group Leader")
            } else {
                recommendations.add("Maintain Current Milestone Checkpoints")
            }

            StudentProgressTrajectory(
                student = summary.student,
                summary = summary,
                trend = trendStatus,
                trendDelta = trendDelta,
                scoreHistory = history,
                homeworkRate = summary.homeworkCompletionRate,
                attendanceRate = summary.attendanceRate,
                tier = summary.tier,
                interventionsCount = studentInters,
                recommendations = recommendations
            )
        }

        // Overall class trend
        val classTrendDelta = if (timelinePoints.size >= 2) {
            val mid = timelinePoints.size / 2
            val earlier = timelinePoints.take(mid).map { it.classAveragePercentage }.average()
            val recent = timelinePoints.takeLast(timelinePoints.size - mid).map { it.classAveragePercentage }.average()
            ((recent - earlier) * 10).roundToInt() / 10.0
        } else 0.0

        val classTrend = when {
            classTrendDelta >= 2.0 -> TrajectoryTrend.IMPROVING
            classTrendDelta <= -2.0 -> TrajectoryTrend.DECLINING
            else -> TrajectoryTrend.STEADY
        }

        // 3. Compute Common Areas of Difficulty Across the Class
        val studentMap = summaries.map { it.student }.associateBy { it.id }
        val difficultyAreas = assignments.mapNotNull { assign ->
            val assignSubs = submissions.filter { it.assignmentId == assign.id }
            val gradedSubs = assignSubs.filter { it.status == SubmissionStatus.GRADED }
            if (gradedSubs.isEmpty()) null
            else {
                val percentages = gradedSubs.map { sub ->
                    if (assign.maxPoints > 0) ((sub.score ?: 0.0) / assign.maxPoints) * 100.0 else 0.0
                }
                val avg = percentages.average()
                val sortedP = percentages.sorted()
                val median = if (sortedP.isNotEmpty()) sortedP[sortedP.size / 2] else avg
                val failingSubs = gradedSubs.filter { sub ->
                    val p = if (assign.maxPoints > 0) ((sub.score ?: 0.0) / assign.maxPoints) * 100.0 else 0.0
                    p < 70.0
                }
                val missingCount = assignSubs.count { it.status == SubmissionStatus.MISSING }
                val strugglingStudents = failingSubs.mapNotNull { studentMap[it.studentId] }

                val severity = when {
                    avg < 75.0 || failingSubs.size >= 3 -> DifficultySeverity.HIGH_DIFFICULTY
                    avg < 84.0 || failingSubs.isNotEmpty() -> DifficultySeverity.MODERATE_CHALLENGE
                    else -> DifficultySeverity.WELL_MASTERED
                }

                val recommendation = when (severity) {
                    DifficultySeverity.HIGH_DIFFICULTY -> "High difficulty detected (${failingSubs.size} failing). Schedule a full class review lecture and reteach foundational prerequisites before the upcoming unit assessment."
                    DifficultySeverity.MODERATE_CHALLENGE -> "Moderate challenge. Offer optional office hours remediation and share step-by-step worked answer keys."
                    DifficultySeverity.WELL_MASTERED -> "Concepts solidly mastered by the class. Ready to transition to the next curriculum unit."
                }

                ClassDifficultyArea(
                    assignment = assign,
                    averagePercentage = (avg * 10).roundToInt() / 10.0,
                    medianPercentage = (median * 10).roundToInt() / 10.0,
                    failingCount = failingSubs.size,
                    missingCount = missingCount,
                    totalGraded = gradedSubs.size,
                    severity = severity,
                    keyConcept = assign.title,
                    teacherActionRecommendation = recommendation,
                    strugglingStudents = strugglingStudents
                )
            }
        }.sortedWith(compareBy<ClassDifficultyArea> {
            when (it.severity) {
                DifficultySeverity.HIGH_DIFFICULTY -> 0
                DifficultySeverity.MODERATE_CHALLENGE -> 1
                DifficultySeverity.WELL_MASTERED -> 2
            }
        }.thenBy { it.averagePercentage })

        // 4. Category Masteries (Homework vs Quiz vs Project vs Exam)
        val categoryMasteries = AssignmentType.entries.mapNotNull { type ->
            val typeAssigns = assignments.filter { it.type == type }
            val typeSubs = submissions.filter { sub ->
                typeAssigns.any { it.id == sub.assignmentId } && sub.status == SubmissionStatus.GRADED
            }
            if (typeSubs.isEmpty()) null
            else {
                val percentages = typeSubs.map { sub ->
                    val a = assignments.find { it.id == sub.assignmentId }
                    val maxP = a?.maxPoints ?: 100.0
                    if (maxP > 0) ((sub.score ?: 0.0) / maxP) * 100.0 else 0.0
                }
                val avg = (percentages.average() * 10).roundToInt() / 10.0
                val status = when {
                    avg >= 88.0 -> "Strong Performance"
                    avg >= 78.0 -> "Proficient"
                    else -> "Needs Focus"
                }
                CategoryMastery(
                    type = type,
                    averagePercentage = avg,
                    assignmentCount = typeAssigns.size,
                    gradedCount = typeSubs.size,
                    status = status
                )
            }
        }

        return ClassroomAnalytics(
            averagePercentage = (avgScore * 10).roundToInt() / 10.0,
            averageGpa = (avgGpa * 100).roundToInt() / 100.0,
            highestPercentage = highest,
            lowestPercentage = lowest,
            distribution = ClassGradeDistribution(a, b, c, d, f),
            totalStudents = summaries.size,
            totalAssignments = assignments.size,
            pendingGradingCount = pending,
            todayAttendanceRate = (todayRate * 10).roundToInt() / 10.0,
            atRiskStudents = atRisk,
            enrichmentStudents = enrichment,
            onTrackStudents = onTrack,
            timelinePoints = timelinePoints,
            studentTrajectories = studentTrajectories,
            difficultyAreas = difficultyAreas,
            categoryMasteries = categoryMasteries,
            classTrend = classTrend,
            classTrendDelta = classTrendDelta
        )
    }

    private fun computeAttendanceReport(
        classroom: ClassroomEntity?,
        students: List<StudentEntity>,
        records: List<AttendanceRecordEntity>
    ): AttendanceReport {
        val dates = records.map { it.date }.distinct().sortedDescending()
        val totalDays = dates.size
        val totalRecords = records.size

        val totalPres = records.count { it.status == AttendanceStatus.PRESENT }
        val totalAbs = records.count { it.status == AttendanceStatus.ABSENT }
        val totalTardy = records.count { it.status == AttendanceStatus.TARDY }
        val totalExcus = records.count { it.status == AttendanceStatus.EXCUSED }

        val overallRate = if (totalRecords > 0) {
            ((totalPres + totalExcus).toDouble() / totalRecords) * 100.0
        } else 100.0

        val studentSummaries = students.map { s ->
            val sRecs = records.filter { it.studentId == s.id }
            val pres = sRecs.count { it.status == AttendanceStatus.PRESENT }
            val abs = sRecs.count { it.status == AttendanceStatus.ABSENT }
            val tardy = sRecs.count { it.status == AttendanceStatus.TARDY }
            val excus = sRecs.count { it.status == AttendanceStatus.EXCUSED }
            val rate = if (sRecs.isNotEmpty()) {
                ((pres + excus).toDouble() / sRecs.size) * 100.0
            } else 100.0

            StudentAttendanceSummary(
                student = s,
                presentCount = pres,
                absentCount = abs,
                tardyCount = tardy,
                excusedCount = excus,
                totalDays = sRecs.size,
                attendanceRate = (rate * 10).roundToInt() / 10.0
            )
        }.sortedByDescending { it.attendanceRate }

        val chronic = studentSummaries.filter { it.attendanceRate < 85.0 && it.totalDays >= 3 }
        val perfect = studentSummaries.filter { it.attendanceRate == 100.0 && it.totalDays > 0 }

        val dateRangeText = if (dates.isNotEmpty()) {
            "${dates.last()} to ${dates.first()} ($totalDays recorded days)"
        } else "No records logged yet"

        return AttendanceReport(
            classroomName = classroom?.name ?: "Classroom",
            subject = classroom?.subject ?: "",
            reportDate = dateFormat.format(Date()),
            dateRangeText = dateRangeText,
            totalDaysRecorded = totalDays,
            totalRecords = totalRecords,
            overallAttendanceRate = (overallRate * 10).roundToInt() / 10.0,
            totalPresent = totalPres,
            totalAbsent = totalAbs,
            totalTardy = totalTardy,
            totalExcused = totalExcus,
            studentSummaries = studentSummaries,
            chronicAbsentees = chronic,
            perfectAttendance = perfect
        )
    }

    private fun computeHomeworkDays(
        students: List<StudentEntity>,
        records: List<HomeworkRecordEntity>
    ): List<HomeworkCheckDay> {
        val grouped = records.groupBy { "${it.date}___${it.topic}" }
        val studentCount = students.size.coerceAtLeast(1)

        return grouped.map { (key, recs) ->
            val parts = key.split("___")
            val date = parts.getOrNull(0) ?: ""
            val topic = parts.getOrNull(1) ?: "Homework Check"
            val done = recs.count { it.status == HomeworkStatus.DONE }
            val partial = recs.count { it.status == HomeworkStatus.PARTIAL }
            val missing = recs.count { it.status == HomeworkStatus.MISSING }
            val excused = recs.count { it.status == HomeworkStatus.EXCUSED }
            val earnedScore = recs.sumOf { it.status.scoreMultiplier }
            val rate = if (recs.isNotEmpty()) (earnedScore / recs.size) * 100.0 else 0.0

            HomeworkCheckDay(
                date = date,
                topic = topic,
                records = recs,
                totalStudents = studentCount,
                doneCount = done,
                partialCount = partial,
                missingCount = missing,
                excusedCount = excused,
                completionPercentage = (rate * 10).roundToInt() / 10.0
            )
        }.sortedByDescending { it.date }
    }
}
