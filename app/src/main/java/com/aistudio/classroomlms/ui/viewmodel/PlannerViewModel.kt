package com.aistudio.classroomlms.ui.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.classroomlms.MainActivity
import com.aistudio.classroomlms.util.NotificationReceiver
import com.aistudio.classroomlms.data.dao.ClassScheduleDao
import com.aistudio.classroomlms.data.dao.ClassroomDao
import com.aistudio.classroomlms.data.dao.PlannerDao
import com.aistudio.classroomlms.data.database.AppDatabase
import com.aistudio.classroomlms.data.database.PreferencesManager
import com.aistudio.classroomlms.data.entity.ClassScheduleEntity
import com.aistudio.classroomlms.data.entity.ClassroomEntity
import com.aistudio.classroomlms.data.entity.DailyLogEntity
import com.aistudio.classroomlms.data.entity.LessonPlanEntity
import com.aistudio.classroomlms.data.entity.LessonPlanSessionEntity
import com.aistudio.classroomlms.data.repository.ClassroomRepository
import com.aistudio.classroomlms.data.repository.ScheduleRepository
import com.aistudio.classroomlms.util.PdfReportExporter
import com.aistudio.classroomlms.util.SafeLogger
import dagger.hilt.android.lifecycle.HiltViewModel
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class PlannerUiState(
    val activeClassroomId: Long? = null,
    val activeClassroom: ClassroomEntity? = null,
    val classrooms: List<ClassroomEntity> = emptyList(),
    val plannerClassroomFilterId: Long? = null,
    val lessonPlans: List<LessonPlanEntity> = emptyList(),
    val lessonPlanSessions: List<LessonPlanSessionEntity> = emptyList(),
    val dailyLogs: List<DailyLogEntity> = emptyList(),
    val schedules: List<ClassScheduleEntity> = emptyList(),
    val isAddEditLessonPlanOpen: Boolean = false,
    val editingLessonPlan: LessonPlanEntity? = null,
    val isAddEditDailyLogOpen: Boolean = false,
    val editingDailyLog: DailyLogEntity? = null,
    val isScheduleScreenOpen: Boolean = false,
    val userNotificationMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val plannerDao: PlannerDao,
    private val classScheduleDao: ClassScheduleDao,
    private val preferencesManager: PreferencesManager,
    private val classroomDao: ClassroomDao,
    private val application: Application
) : ViewModel() {

    private val scheduleRepository = ScheduleRepository(classScheduleDao)

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.activeClassroomIdFlow
                .flatMapLatest { classroomId ->
                    _uiState.update { it.copy(activeClassroomId = classroomId) }
                    if (classroomId != null) {
                        combine(
                            classroomDao.getAllClassrooms(),
                            plannerDao.getLessonPlansForClassroom(classroomId),
                            plannerDao.getSessionsForClassroom(classroomId),
                            plannerDao.getDailyLogsForClassroom(classroomId),
                            classScheduleDao.getSchedulesByClassroom(classroomId)
                        ) { classrooms, plans, sessions, logs, schedules ->
                            PlannerData(
                                classrooms = classrooms,
                                activeClassroom = classrooms.find { it.id == classroomId },
                                plans = plans,
                                sessions = sessions,
                                logs = logs,
                                schedules = schedules
                            )
                        }
                    } else {
                        combine(
                            classroomDao.getAllClassrooms(),
                            classScheduleDao.getAllSchedules()
                        ) { classrooms, schedules ->
                            PlannerData(
                                classrooms = classrooms,
                                activeClassroom = null,
                                plans = emptyList(),
                                sessions = emptyList(),
                                logs = emptyList(),
                                schedules = schedules
                            )
                        }
                    }
                }
                .collect { data ->
                    _uiState.update { current ->
                        current.copy(
                            classrooms = data.classrooms,
                            activeClassroom = data.activeClassroom,
                            lessonPlans = data.plans,
                            lessonPlanSessions = data.sessions,
                            dailyLogs = data.logs,
                            schedules = data.schedules
                        )
                    }
                }
        }
    }

    private data class PlannerData(
        val classrooms: List<ClassroomEntity>,
        val activeClassroom: ClassroomEntity?,
        val plans: List<LessonPlanEntity>,
        val sessions: List<LessonPlanSessionEntity>,
        val logs: List<DailyLogEntity>,
        val schedules: List<ClassScheduleEntity>
    )

    fun setPlannerClassroomFilter(classroomId: Long?) {
        _uiState.update { it.copy(plannerClassroomFilterId = classroomId) }
    }

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
        targetClassroomId: Long?,
        reminderEnabled: Boolean,
        reminderTime: Long,
        unitNumber: String = "",
        allocatedHours: Double = 0.0,
        unitLearningOutcomes: String = "",
        sessions: List<LessonPlanSessionEntity> = emptyList()
    ) {
        val classId = targetClassroomId ?: _uiState.value.activeClassroom?.id ?: return
        viewModelScope.launch {
            val editing = _uiState.value.editingLessonPlan
            if (editing != null) {
                val updated = editing.copy(
                    classroomId = classId,
                    unitTitle = unitTitle,
                    description = description,
                    targetDate = targetDate,
                    status = status,
                    reminderEnabled = reminderEnabled,
                    reminderTime = reminderTime,
                    reminderId = if (reminderEnabled && editing.reminderId.isBlank())
                        java.util.UUID.randomUUID().toString()
                    else
                        editing.reminderId,
                    unitNumber = unitNumber,
                    allocatedHours = allocatedHours,
                    unitLearningOutcomes = unitLearningOutcomes
                )
                plannerDao.updateLessonPlan(updated)
                plannerDao.replaceSessionsForPlan(updated.id, sessions)

                // Handle notification scheduling for reminders
                if (reminderEnabled && reminderTime > 0) {
                    scheduleReminderNotification(updated.id, unitTitle, reminderTime, classId)
                } else {
                    // Cancel reminder if disabled or time not set
                    cancelReminderNotification(editing.id)
                }

                showToast("Lesson plan updated")
            } else {
                val newPlan = LessonPlanEntity(
                    classroomId = classId,
                    unitTitle = unitTitle,
                    description = description,
                    targetDate = targetDate,
                    status = status,
                    reminderEnabled = reminderEnabled,
                    reminderTime = reminderTime,
                    reminderId = if (reminderEnabled) java.util.UUID.randomUUID().toString() else "",
                    unitNumber = unitNumber,
                    allocatedHours = allocatedHours,
                    unitLearningOutcomes = unitLearningOutcomes
                )
                val insertedId = plannerDao.insertLessonPlan(newPlan)
                plannerDao.replaceSessionsForPlan(insertedId, sessions)

                // Schedule notification for new lesson plan with reminder
                if (reminderEnabled && reminderTime > 0) {
                    scheduleReminderNotification(insertedId, unitTitle, reminderTime, classId)
                }

                showToast("Lesson plan created")
            }
            closeLessonPlanDialog()
        }
    }

    private fun scheduleReminderNotification(lessonPlanId: Long, title: String, reminderTime: Long, classroomId: Long) {
        // Use injected application context
        val context = application
        
        // Calculate trigger time
        val triggerTime = reminderTime
        
        // Only schedule if the reminder time is in the future
        if (triggerTime > System.currentTimeMillis()) {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = "ACTION_SHOW_REMINDER"
                putExtra("lesson_plan_id", lessonPlanId)
                putExtra("title", "Lesson Plan Reminder: $title")
                putExtra("message", "Your lesson plan '$title' is due now")
                putExtra("classroom_id", classroomId)
                putExtra("notification_type", "lesson_reminder")
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                lessonPlanId.toInt(), // Use lessonPlanId as request code for uniqueness
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            
            SafeLogger.d("PlannerViewModel", "Scheduled reminder for lesson plan $lessonPlanId at $triggerTime")
        }
    }

    private fun cancelReminderNotification(lessonPlanId: Long) {
        val context = application
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            lessonPlanId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        SafeLogger.d("PlannerViewModel", "Cancelled reminder for lesson plan $lessonPlanId")
    }

    fun updateLessonPlanStatus(plan: LessonPlanEntity, newStatus: String) {
        viewModelScope.launch {
            plannerDao.updateLessonPlan(plan.copy(status = newStatus))
            showToast("Plan status set to $newStatus")
        }
    }

    fun deleteLessonPlan(plan: LessonPlanEntity) {
        viewModelScope.launch {
            plannerDao.deleteLessonPlan(plan)
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
        val classId = targetClassroomId ?: _uiState.value.activeClassroom?.id ?: return
        viewModelScope.launch {
            val editing = _uiState.value.editingDailyLog
            if (editing != null) {
                val updated = editing.copy(
                    classroomId = classId,
                    date = date,
                    reflectionNotes = reflectionNotes,
                    wasProxyClass = wasProxyClass
                )
                plannerDao.updateDailyLog(updated)
                showToast("Daily diary log updated")
            } else {
                val newLog = DailyLogEntity(
                    classroomId = classId,
                    date = date,
                    reflectionNotes = reflectionNotes,
                    wasProxyClass = wasProxyClass
                )
                plannerDao.insertDailyLog(newLog)
                showToast("Daily diary entry added")
            }
            closeDailyLogDialog()
        }
    }

    fun deleteDailyLog(log: DailyLogEntity) {
        viewModelScope.launch {
            plannerDao.deleteDailyLog(log)
            showToast("Daily diary entry deleted")
        }
    }

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
            try {
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
                    showToast("Updated routine for '${baseEntry.classroomName}' across all weeks")
                } else {
                    scheduleRepository.saveBatchScheduleEntries(batchEntries)
                    val msg = if (batchEntries.size > 1) "Saved ${batchEntries.size} class periods across selected days/weeks" else "Class schedule entry saved"
                    showToast(msg)
                }
            } catch (e: Exception) {
                showToast("Failed to save schedule: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun replicateWeekSchedule(fromWeek: Int = 1, targetWeeks: List<Int> = listOf(2, 3, 4, 5)) {
        viewModelScope.launch {
            scheduleRepository.replicateWeekSchedule(fromWeek, targetWeeks)
            showToast("Copied Week $fromWeek schedule routine to Weeks 2–5")
        }
    }

    fun hasTimeOverlap(
        existingEntries: List<ClassScheduleEntity>,
        newEntry: ClassScheduleEntity,
        excludeId: Long = 0L
    ): Boolean {
        return scheduleRepository.hasTimeOverlap(existingEntries, newEntry.startMinutes, newEntry.endMinutes, excludeId)
    }

    fun generateLessonPlansReportText(): String {
        val activeClass = _uiState.value.activeClassroom
        val plans = _uiState.value.lessonPlans
        val sessions = _uiState.value.lessonPlanSessions
        val df = SimpleDateFormat("MMM dd, yyyy", Locale.US)

        val sb = StringBuilder()
        sb.appendLine("TEACHER LESSON PLANNER REPORT")
        sb.appendLine("Classroom: ${activeClass?.name ?: "All Classes"} (${activeClass?.subject ?: ""})")
        sb.appendLine("Total Units: ${plans.size}")
        sb.appendLine("==========================================")
        sb.appendLine()

        if (plans.isEmpty()) {
            sb.appendLine("No lesson plans recorded.")
        } else {
            plans.forEachIndexed { idx, plan ->
                val unitTag = if (plan.unitNumber.isNotBlank()) "${plan.unitNumber}: " else ""
                val hrsTag = if (plan.allocatedHours > 0) " (${plan.allocatedHours} Hrs)" else ""
                sb.appendLine("${idx + 1}. [${plan.status}] $unitTag${plan.unitTitle}$hrsTag")
                sb.appendLine("   Target Date: ${df.format(Date(plan.targetDate))}")
                if (plan.unitLearningOutcomes.isNotBlank()) {
                    sb.appendLine("   Learning Outcomes (LO): ${plan.unitLearningOutcomes}")
                }
                if (plan.description.isNotBlank()) {
                    sb.appendLine("   Notes: ${plan.description}")
                }
                val unitSessions = sessions.filter { it.lessonPlanId == plan.id }
                if (unitSessions.isNotEmpty()) {
                    sb.appendLine("   --- LO Session Matrix (${unitSessions.size} sessions) ---")
                    unitSessions.forEach { s ->
                        val label = if (s.isClosingSession) "Closing of Unit" else s.learningOutcome.ifBlank { "Session ${s.sessionOrder}" }
                        sb.appendLine("   [${s.sessionOrder}] $label (${s.hours} Hrs)")
                        if (s.content.isNotBlank()) sb.appendLine("       Content: ${s.content}")
                        if (s.lecturerActivity.isNotBlank()) sb.appendLine("       Lecturer: ${s.lecturerActivity}")
                        if (s.studentActivity.isNotBlank()) sb.appendLine("       Student: ${s.studentActivity}")
                        if (s.teachingAids.isNotBlank()) sb.appendLine("       Teaching Aids: ${s.teachingAids}")
                        if (s.evaluationNote.isNotBlank()) sb.appendLine("       Evaluation: ${s.evaluationNote}")
                    }
                }
                sb.appendLine("------------------------------------------")
            }
        }
        return sb.toString()
    }

    fun generateDailyDiaryReportText(): String {
        val activeClass = _uiState.value.activeClassroom
        val logs = _uiState.value.dailyLogs
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
        val activeClass = _uiState.value.activeClassroom
        val plans = _uiState.value.lessonPlans
        val sessions = _uiState.value.lessonPlanSessions
        if (sessions.isNotEmpty()) {
            PdfReportExporter.exportLessonPlanMatrixPdf(
                context = context,
                classTitle = activeClass?.name ?: "Classroom",
                subject = activeClass?.subject ?: "",
                plans = plans,
                sessions = sessions
            )
        } else {
            val reportText = generateLessonPlansReportText()
            val title = "Lesson Plans - ${activeClass?.name ?: "Classroom"}"
            PdfReportExporter.exportAndShare(context, title, reportText, "LessonPlans_${activeClass?.name ?: "Class"}")
        }
    }

    fun exportDailyDiaryPdf(context: Context) {
        val activeClass = _uiState.value.activeClassroom
        val reportText = generateDailyDiaryReportText()
        val title = "Daily Diary - ${activeClass?.name ?: "Classroom"}"
        PdfReportExporter.exportAndShare(context, title, reportText, "DailyDiary_${activeClass?.name ?: "Class"}")
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(userNotificationMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(userNotificationMessage = null) }
    }
}
