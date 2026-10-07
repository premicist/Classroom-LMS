package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.dao.ClassScheduleDao
import com.example.data.dao.ClassroomDao
import com.example.data.dao.PlannerDao
import com.example.data.database.AppDatabase
import com.example.data.database.PreferencesManager
import com.example.data.entity.ClassScheduleEntity
import com.example.data.entity.ClassroomEntity
import com.example.data.entity.DailyLogEntity
import com.example.data.entity.LessonPlanEntity
import com.example.data.repository.ClassroomRepository
import com.example.data.repository.ScheduleRepository
import com.example.util.PdfReportExporter
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
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class PlannerUiState(
    val activeClassroomId: Long? = null,
    val activeClassroom: ClassroomEntity? = null,
    val classrooms: List<ClassroomEntity> = emptyList(),
    val plannerClassroomFilterId: Long? = null,
    val lessonPlans: List<LessonPlanEntity> = emptyList(),
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
    database: AppDatabase
) : ViewModel() {

    private val classroomDao: ClassroomDao = database.classroomDao()
    private val repository = ClassroomRepository(database)
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
                            plannerDao.getDailyLogsForClassroom(classroomId),
                            classScheduleDao.getSchedulesByClassroom(classroomId)
                        ) { classrooms, plans, logs, schedules ->
                            PlannerData(
                                classrooms = classrooms,
                                activeClassroom = classrooms.find { it.id == classroomId },
                                plans = plans,
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
        targetClassroomId: Long? = null
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
                showToast("Updated routine for '${baseEntry.classroomName}' across all weeks")
            } else {
                scheduleRepository.saveBatchScheduleEntries(batchEntries)
                val msg = if (batchEntries.size > 1) "Saved ${batchEntries.size} class periods across selected days/weeks" else "Class schedule entry saved"
                showToast(msg)
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
        val reportText = generateLessonPlansReportText()
        val title = "Lesson Plans - ${activeClass?.name ?: "Classroom"}"
        PdfReportExporter.exportAndShare(context, title, reportText, "LessonPlans_${activeClass?.name ?: "Class"}")
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
