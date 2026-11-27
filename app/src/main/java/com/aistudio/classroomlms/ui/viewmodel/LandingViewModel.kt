package com.aistudio.classroomlms.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.classroomlms.data.dao.AcademicEventDao
import com.aistudio.classroomlms.data.dao.AttendanceDao
import com.aistudio.classroomlms.data.dao.ClassScheduleDao
import com.aistudio.classroomlms.data.dao.ClassroomDao
import com.aistudio.classroomlms.data.dao.ExamDao
import com.aistudio.classroomlms.data.dao.PlannerDao
import com.aistudio.classroomlms.data.dao.StudentDao
import com.aistudio.classroomlms.data.entity.AcademicEventCategory
import com.aistudio.classroomlms.data.entity.AcademicEventEntity
import com.aistudio.classroomlms.data.entity.ClassScheduleEntity
import com.aistudio.classroomlms.data.entity.ClassroomEntity
import com.aistudio.classroomlms.data.entity.ExamEntity
import com.aistudio.classroomlms.data.entity.LessonPlanEntity
import com.aistudio.classroomlms.data.entity.StudentEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class LandingAlertType {
    HOLIDAY,
    PENDING_ATTENDANCE,
    EXAM_UPCOMING,
    LESSON_PLAN_DUE,
    ACADEMIC_EVENT
}

data class LandingAlert(
    val id: String,
    val type: LandingAlertType,
    val title: String,
    val subtitle: String,
    val classroomId: Long? = null
)

data class LandingClassCard(
    val classroom: ClassroomEntity,
    val studentCount: Int,
    val todaySchedules: List<ClassScheduleEntity>,
    val isAttendanceMarkedToday: Boolean,
    val currentOrNextSchedule: ClassScheduleEntity?,
    val activeLessonPlanTitle: String?
)

data class LandingUiState(
    val classCards: List<LandingClassCard> = emptyList(),
    val alerts: List<LandingAlert> = emptyList(),
    val todayHoliday: AcademicEventEntity? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class LandingViewModel @Inject constructor(
    classroomDao: ClassroomDao,
    studentDao: StudentDao,
    classScheduleDao: ClassScheduleDao,
    attendanceDao: AttendanceDao,
    examDao: ExamDao,
    plannerDao: PlannerDao,
    academicEventDao: AcademicEventDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(LandingUiState())
    val uiState: StateFlow<LandingUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    init {
        val todayStr = dateFormat.format(Date())
        viewModelScope.launch {
            val baseFlow = combine(
                classroomDao.getAllClassrooms(),
                studentDao.getAllStudents(),
                classScheduleDao.getAllSchedules(),
                attendanceDao.getAttendanceByDateAll(todayStr)
            ) { classrooms, students, schedules, todayAttendance ->
                BaseSnapshot(classrooms, students, schedules, todayAttendance)
            }

            combine(
                baseFlow,
                examDao.getAllExams(),
                plannerDao.getAllLessonPlans(),
                academicEventDao.getAllEvents()
            ) { base, exams, plans, events ->
                buildLandingState(base, exams, plans, events, todayStr)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private data class BaseSnapshot(
        val classrooms: List<ClassroomEntity>,
        val students: List<StudentEntity>,
        val schedules: List<ClassScheduleEntity>,
        val todayAttendance: List<com.aistudio.classroomlms.data.entity.AttendanceRecordEntity>
    )

    private fun buildLandingState(
        base: BaseSnapshot,
        exams: List<ExamEntity>,
        plans: List<LessonPlanEntity>,
        events: List<AcademicEventEntity>,
        todayStr: String
    ): LandingUiState {
        val cal = Calendar.getInstance()
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val todayDayName = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "MONDAY"
            Calendar.TUESDAY -> "TUESDAY"
            Calendar.WEDNESDAY -> "WEDNESDAY"
            Calendar.THURSDAY -> "THURSDAY"
            Calendar.FRIDAY -> "FRIDAY"
            Calendar.SATURDAY -> "SATURDAY"
            else -> "SUNDAY"
        }
        val weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH).coerceIn(1, 5)
        val sevenDaysLaterStr = dateFormat.format(Date(cal.timeInMillis + 7L * 86_400_000L))

        val todayHoliday = events.firstOrNull { ev ->
            (ev.category == AcademicEventCategory.HOLIDAY || ev.category == AcademicEventCategory.FESTIVAL_BREAK) &&
                todayStr >= ev.startDate && todayStr <= ev.endDate
        }

        val studentsByClass = base.students.groupBy { it.classroomId }
        val attendanceClassIds = base.todayAttendance.map { it.classroomId }.toSet()

        val cards = base.classrooms.map { cls ->
            val classStudents = studentsByClass[cls.id].orEmpty()
            val todayPeriods = base.schedules
                .filter { s ->
                    (s.classroomId == cls.id || s.classroomName.equals(cls.name, ignoreCase = true)) &&
                        s.dayOfWeek.equals(todayDayName, ignoreCase = true) &&
                        s.weekNumber == weekOfMonth
                }
                .ifEmpty {
                    // Fallback to Week 1 routine if current weekOfMonth isn't populated separately
                    base.schedules.filter { s ->
                        (s.classroomId == cls.id || s.classroomName.equals(cls.name, ignoreCase = true)) &&
                            s.dayOfWeek.equals(todayDayName, ignoreCase = true) &&
                            s.weekNumber == 1
                    }
                }
                .distinctBy { "${it.startMinutes}_${it.endMinutes}" }
                .sortedBy { it.startMinutes }

            val nextOrActive = todayPeriods.firstOrNull { period ->
                val end = if (period.endMinutes > 0) period.endMinutes else period.startMinutes + 60
                end >= currentMinutes
            }

            val latestPlan = plans
                .filter { it.classroomId == cls.id && it.status != "COMPLETED" }
                .sortedBy { it.targetDate }
                .firstOrNull()

            LandingClassCard(
                classroom = cls,
                studentCount = classStudents.size,
                todaySchedules = todayPeriods,
                isAttendanceMarkedToday = cls.id in attendanceClassIds,
                currentOrNextSchedule = nextOrActive,
                activeLessonPlanTitle = latestPlan?.let {
                    if (it.unitNumber.isNotBlank()) "${it.unitNumber}: ${it.unitTitle}" else it.unitTitle
                }
            )
        }.sortedWith(
            compareByDescending<LandingClassCard> { it.todaySchedules.isNotEmpty() }
                .thenBy { it.currentOrNextSchedule?.startMinutes ?: Int.MAX_VALUE }
                .thenBy { it.classroom.name }
        )

        val alerts = mutableListOf<LandingAlert>()

        if (todayHoliday != null) {
            alerts.add(
                LandingAlert(
                    id = "holiday_${todayHoliday.id}",
                    type = LandingAlertType.HOLIDAY,
                    title = "${todayHoliday.category.emoji} ${todayHoliday.title}",
                    subtitle = "Scheduled ${todayHoliday.category.displayName} today"
                )
            )
        } else {
            // Pending attendance for classes scheduled today (with students enrolled)
            cards.filter { it.todaySchedules.isNotEmpty() && !it.isAttendanceMarkedToday && it.studentCount > 0 }
                .forEach { card ->
                    alerts.add(
                        LandingAlert(
                            id = "att_${card.classroom.id}",
                            type = LandingAlertType.PENDING_ATTENDANCE,
                            title = "Attendance pending: ${card.classroom.name}",
                            subtitle = "Today's period at ${card.todaySchedules.first().startTime}",
                            classroomId = card.classroom.id
                        )
                    )
                }
        }

        // Upcoming exams in next 7 days
        exams.filter { it.date >= todayStr && it.date <= sevenDaysLaterStr }
            .take(3)
            .forEach { exam ->
                val clsName = base.classrooms.find { it.id == exam.classroomId }?.name ?: "Class"
                alerts.add(
                    LandingAlert(
                        id = "exam_${exam.id}",
                        type = LandingAlertType.EXAM_UPCOMING,
                        title = "Exam: ${exam.title} ($clsName)",
                        subtitle = "Scheduled on ${exam.date}",
                        classroomId = exam.classroomId
                    )
                )
            }

        // Lesson plans due today
        plans.filter { it.status != "COMPLETED" && dateFormat.format(Date(it.targetDate)) == todayStr }
            .take(3)
            .forEach { plan ->
                val clsName = base.classrooms.find { it.id == plan.classroomId }?.name ?: "Class"
                alerts.add(
                    LandingAlert(
                        id = "plan_${plan.id}",
                        type = LandingAlertType.LESSON_PLAN_DUE,
                        title = "Lesson Plan today: ${plan.unitTitle}",
                        subtitle = "$clsName • Tap to open Planner",
                        classroomId = plan.classroomId
                    )
                )
            }

        // Upcoming Academic Calendar events (next 7 days, excluding today's already-shown holiday)
        events.filter { ev ->
            ev.id != todayHoliday?.id && ev.startDate > todayStr && ev.startDate <= sevenDaysLaterStr
        }.take(3).forEach { ev ->
            alerts.add(
                LandingAlert(
                    id = "cal_${ev.id}",
                    type = LandingAlertType.ACADEMIC_EVENT,
                    title = "${ev.category.emoji} Upcoming: ${ev.title}",
                    subtitle = if (ev.endDate != ev.startDate) "${ev.startDate} to ${ev.endDate}" else ev.startDate
                )
            )
        }

        return LandingUiState(
            classCards = cards,
            alerts = alerts,
            todayHoliday = todayHoliday,
            isLoading = false
        )
    }
}
