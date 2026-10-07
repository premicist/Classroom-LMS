package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.dao.AttendanceDao
import com.example.data.dao.ClassroomDao
import com.example.data.dao.HomeworkRecordDao
import com.example.data.dao.StudentDao
import com.example.data.database.AppDatabase
import com.example.data.database.PreferencesManager
import com.example.data.entity.AttendanceRecordEntity
import com.example.data.entity.AttendanceStatus
import com.example.data.entity.ClassroomEntity
import com.example.data.entity.HomeworkRecordEntity
import com.example.data.entity.HomeworkStatus
import com.example.data.entity.StudentEntity
import com.example.util.ClassroomAnalyticsEngine
import com.example.util.PdfReportExporter
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class AttendanceUiState(
    val activeClassroomId: Long? = null,
    val activeClassroom: ClassroomEntity? = null,
    val selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val students: List<StudentEntity> = emptyList(),
    val attendanceRecords: List<AttendanceRecordEntity> = emptyList(),
    val attendanceReport: AttendanceReport? = null,
    val homeworkRecords: List<HomeworkRecordEntity> = emptyList(),
    val activeHomeworkTopic: String = "Daily Homework Review",
    val userNotificationMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val attendanceDao: AttendanceDao,
    private val homeworkRecordDao: HomeworkRecordDao,
    private val studentDao: StudentDao,
    private val preferencesManager: PreferencesManager,
    database: AppDatabase
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val classroomDao: ClassroomDao = database.classroomDao()

    private val _uiState = MutableStateFlow(
        AttendanceUiState(selectedDate = dateFormat.format(Date()))
    )
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.activeClassroomIdFlow
                .flatMapLatest { classroomId ->
                    _uiState.update { it.copy(activeClassroomId = classroomId) }
                    if (classroomId != null) {
                        combine(
                            classroomDao.getClassroomById(classroomId),
                            studentDao.getStudentsByClassroom(classroomId),
                            attendanceDao.getAttendanceForClassroom(classroomId),
                            homeworkRecordDao.getAllHomeworkRecordsForClassroom(classroomId)
                        ) { classroom, students, attendance, homework ->
                            val report = ClassroomAnalyticsEngine.computeAttendanceReport(classroom, students, attendance)
                            AttendanceData(classroom, students, attendance, homework, report)
                        }
                    } else {
                        flowOf(AttendanceData(null, emptyList(), emptyList(), emptyList(), null))
                    }
                }
                .collect { data ->
                    _uiState.update { current ->
                        current.copy(
                            activeClassroom = data.classroom,
                            students = data.students,
                            attendanceRecords = data.attendance,
                            attendanceReport = data.report,
                            homeworkRecords = data.homework
                        )
                    }
                }
        }
    }

    private data class AttendanceData(
        val classroom: ClassroomEntity?,
        val students: List<StudentEntity>,
        val attendance: List<AttendanceRecordEntity>,
        val homework: List<HomeworkRecordEntity>,
        val report: AttendanceReport?
    )

    fun selectDate(date: String) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun setActiveHomeworkTopic(topic: String) {
        _uiState.update { it.copy(activeHomeworkTopic = topic) }
    }

    fun recordAttendanceStatus(studentId: Long, date: String, status: AttendanceStatus, remarks: String = "") {
        val classroomId = _uiState.value.activeClassroomId ?: return
        viewModelScope.launch {
            val existing = _uiState.value.attendanceRecords.find {
                it.studentId == studentId && it.date == date
            }
            if (existing != null) {
                val updated = existing.copy(status = status, remarks = remarks.ifEmpty { existing.remarks })
                attendanceDao.updateAttendanceRecord(updated)
            } else {
                val newRec = AttendanceRecordEntity(
                    classroomId = classroomId,
                    studentId = studentId,
                    date = date,
                    status = status,
                    remarks = remarks
                )
                attendanceDao.insertAttendanceRecord(newRec)
            }
        }
    }

    fun cycleAttendanceStatus(studentId: Long, date: String) {
        val classroomId = _uiState.value.activeClassroomId ?: return
        viewModelScope.launch {
            val existing = _uiState.value.attendanceRecords.find {
                it.studentId == studentId && it.date == date
            }
            val nextStatus = when (existing?.status) {
                AttendanceStatus.PRESENT -> AttendanceStatus.ABSENT
                AttendanceStatus.ABSENT -> AttendanceStatus.LATE
                AttendanceStatus.LATE -> AttendanceStatus.EXCUSED
                AttendanceStatus.EXCUSED -> AttendanceStatus.PRESENT
                null -> AttendanceStatus.ABSENT
            }
            recordAttendanceStatus(studentId, date, nextStatus)
        }
    }

    fun markAllAttendanceAs(date: String, status: AttendanceStatus) {
        val classroomId = _uiState.value.activeClassroomId ?: return
        val students = _uiState.value.students
        viewModelScope.launch {
            val records = students.map { s ->
                val existing = _uiState.value.attendanceRecords.find { it.studentId == s.id && it.date == date }
                existing?.copy(status = status) ?: AttendanceRecordEntity(
                    classroomId = classroomId,
                    studentId = s.id,
                    date = date,
                    status = status
                )
            }
            attendanceDao.insertAttendanceRecords(records)
            showToast("Marked all attendance as ${status.displayName} for $date")
        }
    }

    fun markAllPresent(date: String) {
        markAllAttendanceAs(date, AttendanceStatus.PRESENT)
    }

    fun recordHomeworkStatus(studentId: Long, topic: String, date: String, status: HomeworkStatus) {
        val classroomId = _uiState.value.activeClassroomId ?: return
        viewModelScope.launch {
            val existing = _uiState.value.homeworkRecords.find {
                it.studentId == studentId && it.date == date && it.topic == topic
            }
            if (existing != null) {
                homeworkRecordDao.updateHomeworkRecord(existing.copy(status = status))
            } else {
                val newRec = HomeworkRecordEntity(
                    classroomId = classroomId,
                    studentId = studentId,
                    date = date,
                    topic = topic,
                    status = status
                )
                homeworkRecordDao.insertHomeworkRecord(newRec)
            }
        }
    }

    fun markAllHomeworkAs(topic: String, date: String, status: HomeworkStatus) {
        val classroomId = _uiState.value.activeClassroomId ?: return
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
            homeworkRecordDao.insertHomeworkRecords(records)
            showToast("Marked all HW as ${status.displayName} for $topic")
        }
    }

    fun markAllHomeworkDone(date: String, topic: String) {
        markAllHomeworkAs(topic, date, HomeworkStatus.DONE)
    }

    fun cycleHomeworkStatus(studentId: Long, date: String, topic: String) {
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

    fun deleteHomeworkRecord(studentId: Long, date: String, topic: String) {
        viewModelScope.launch {
            val existing = _uiState.value.homeworkRecords.find {
                it.studentId == studentId && it.date == date && it.topic == topic
            }
            if (existing != null) {
                homeworkRecordDao.deleteHomeworkRecord(existing)
                showToast("Homework record cleared")
            }
        }
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
        sb.append("Total Records: ${report.totalRecords} (Present: ${report.totalPresent}, Absent: ${report.totalAbsent}, Late: ${report.totalLate}, Excused: ${report.totalExcused})\n\n")

        sb.append(String.format(Locale.US, "%-22s | %-8s | %-6s | %-4s | %-4s | %-5s | %-7s\n", "Student Name", "ID", "Rate", "Pres", "Abs", "Late", "Status"))
        sb.append("-------------------------------------------------------------------------\n")
        report.studentSummaries.forEach { s ->
            val statusStr = if (s.attendanceRate < 85.0) "AT RISK" else if (s.attendanceRate == 100.0) "PERFECT" else "GOOD"
            sb.append(String.format(Locale.US, "%-22s | %-8s | %5.1f%% | %-4d | %-4d | %-5d | %-7s\n",
                s.student.name.take(22),
                s.student.studentNumber,
                s.attendanceRate,
                s.presentCount,
                s.absentCount,
                s.lateCount,
                statusStr
            ))
        }

        if (report.chronicAbsentees.isNotEmpty()) {
            sb.append("\nCHRONIC ABSENTEEISM WATCHLIST (<85%):\n")
            report.chronicAbsentees.forEach { s ->
                sb.append(" ⚠️ ${s.student.name} (${s.student.studentNumber}) - ${"%.1f".format(s.attendanceRate)}% (${s.absentCount} abs, ${s.lateCount} late)\n")
            }
        }
        sb.append("\n====================================================\n")
        return sb.toString()
    }

    fun exportAttendanceRollSheet(context: Context) {
        val report = _uiState.value.attendanceReport
        if (report == null) {
            showToast("No attendance data available to export")
            return
        }
        PdfReportExporter.exportAttendanceRollSheet(context, report)
        showToast("Exporting Attendance Roll Sheet...")
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(userNotificationMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(userNotificationMessage = null) }
    }
}
