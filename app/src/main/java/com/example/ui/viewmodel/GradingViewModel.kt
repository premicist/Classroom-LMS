package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.dao.AssignmentDao
import com.example.data.dao.ClassroomDao
import com.example.data.dao.StudentDao
import com.example.data.dao.SubmissionDao
import com.example.data.database.AppDatabase
import com.example.data.database.PreferencesManager
import com.example.data.entity.AssignmentEntity
import com.example.data.entity.AssignmentType
import com.example.data.entity.ClassroomEntity
import com.example.data.entity.StudentEntity
import com.example.data.entity.SubmissionEntity
import com.example.data.entity.SubmissionStatus
import com.example.data.entity.TermWeightConfig
import com.example.data.repository.ClassroomRepository
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
import java.util.Locale
import javax.inject.Inject

import com.example.ui.viewmodel.ClassroomAnalytics
import com.example.ui.viewmodel.StudentGradeSummary

data class GradingUiState(
    val activeClassroomId: Long? = null,
    val activeClassroom: ClassroomEntity? = null,
    val students: List<StudentEntity> = emptyList(),
    val assignments: List<AssignmentEntity> = emptyList(),
    val submissions: List<SubmissionEntity> = emptyList(),
    val studentGradeSummaries: List<StudentGradeSummary> = emptyList(),
    val analytics: ClassroomAnalytics = ClassroomAnalytics(),
    val termWeightConfig: TermWeightConfig = TermWeightConfig(),
    val isTermWeightingDialogOpen: Boolean = false,
    val isAddEditAssignmentOpen: Boolean = false,
    val editingAssignment: AssignmentEntity? = null,
    val isGradingSubmissionOpen: Boolean = false,
    val gradingSubmission: SubmissionEntity? = null,
    val gradingAssignment: AssignmentEntity? = null,
    val gradingStudent: StudentEntity? = null,
    val assignmentFilterType: AssignmentType? = null,
    val assignmentFilterStatus: SubmissionStatus? = null,
    val userNotificationMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GradingViewModel @Inject constructor(
    private val assignmentDao: AssignmentDao,
    private val submissionDao: SubmissionDao,
    private val studentDao: StudentDao,
    private val preferencesManager: PreferencesManager,
    database: AppDatabase
) : ViewModel() {

    private val repository = ClassroomRepository(database)
    private val classroomDao: ClassroomDao = database.classroomDao()

    private val _uiState = MutableStateFlow(GradingUiState())
    val uiState: StateFlow<GradingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.activeClassroomIdFlow
                .flatMapLatest { classroomId ->
                    _uiState.update { it.copy(activeClassroomId = classroomId) }
                    if (classroomId != null) {
                        combine(
                            classroomDao.getClassroomById(classroomId),
                            studentDao.getStudentsByClassroom(classroomId),
                            assignmentDao.getAssignmentsByClassroom(classroomId),
                            submissionDao.getSubmissionsByClassroom(classroomId)
                        ) { classroom, students, assignments, submissions ->
                            GradingData(classroom, students, assignments, submissions)
                        }
                    } else {
                        flowOf(GradingData(null, emptyList(), emptyList(), emptyList()))
                    }
                }
                .collect { data ->
                    val summaries = ClassroomAnalyticsEngine.computeStudentGrades(
                        students = data.students,
                        assignments = data.assignments,
                        submissions = data.submissions,
                        homeworks = emptyList(),
                        attendance = emptyList(),
                        termWeightConfig = _uiState.value.termWeightConfig
                    )
                    val analytics = ClassroomAnalyticsEngine.computeClassAnalytics(
                        summaries = summaries,
                        assignments = data.assignments,
                        submissions = data.submissions,
                        attendance = emptyList(),
                        interventions = emptyList()
                    )
                    _uiState.update { current ->
                        current.copy(
                            activeClassroom = data.classroom,
                            students = data.students,
                            assignments = data.assignments,
                            submissions = data.submissions,
                            studentGradeSummaries = summaries,
                            analytics = analytics
                        )
                    }
                }
        }
    }

    private data class GradingData(
        val classroom: ClassroomEntity?,
        val students: List<StudentEntity>,
        val assignments: List<AssignmentEntity>,
        val submissions: List<SubmissionEntity>
    )

    fun openAddAssignment() {
        _uiState.update { it.copy(isAddEditAssignmentOpen = true, editingAssignment = null) }
    }

    fun openEditAssignment(assignment: AssignmentEntity) {
        _uiState.update { it.copy(isAddEditAssignmentOpen = true, editingAssignment = assignment) }
    }

    fun closeAddEditAssignment() {
        _uiState.update { it.copy(isAddEditAssignmentOpen = false, editingAssignment = null) }
    }

    fun saveAssignment(assignment: AssignmentEntity) {
        val classroomId = _uiState.value.activeClassroomId ?: return
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

    fun deleteAssignment(id: Long) {
        viewModelScope.launch {
            repository.deleteAssignment(id)
            showToast("Assignment deleted")
        }
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

    fun saveSubmission(submission: SubmissionEntity) {
        viewModelScope.launch {
            if (submission.id == 0L) {
                repository.insertOrUpdateSubmissions(listOf(submission))
            } else {
                repository.updateSubmission(submission)
            }
            showToast("Grade saved for submission")
            closeGradingDialog()
        }
    }

    fun toggleSubmissionCheck(submission: SubmissionEntity) {
        viewModelScope.launch {
            val updated = submission.copy(
                isChecked = !submission.isChecked,
                status = if (!submission.isChecked && submission.status == SubmissionStatus.PENDING) SubmissionStatus.SUBMITTED else submission.status,
                updatedAt = System.currentTimeMillis()
            )
            if (updated.id == 0L) {
                repository.insertOrUpdateSubmissions(listOf(updated))
            } else {
                repository.updateSubmission(updated)
            }
        }
    }

    fun quickMarkAllSubmissionsForAssignment(assignmentId: Long, status: SubmissionStatus) {
        viewModelScope.launch {
            val relevant = _uiState.value.submissions.filter { it.assignmentId == assignmentId }
            if (relevant.isNotEmpty()) {
                val updated = relevant.map {
                    it.copy(
                        status = status,
                        isChecked = status == SubmissionStatus.SUBMITTED || status == SubmissionStatus.GRADED,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                repository.insertOrUpdateSubmissions(updated)
                showToast("Marked all as ${status.name}")
            }
        }
    }

    fun openTermWeightingDialog() {
        _uiState.update { it.copy(isTermWeightingDialogOpen = true) }
    }

    fun closeTermWeightingDialog() {
        _uiState.update { it.copy(isTermWeightingDialogOpen = false) }
    }

    fun saveTermWeightConfig(config: TermWeightConfig) {
        val current = _uiState.value
        val summaries = ClassroomAnalyticsEngine.computeStudentGrades(
            students = current.students,
            assignments = current.assignments,
            submissions = current.submissions,
            homeworks = emptyList(),
            attendance = emptyList(),
            termWeightConfig = config
        )
        val analytics = ClassroomAnalyticsEngine.computeClassAnalytics(
            summaries = summaries,
            assignments = current.assignments,
            submissions = current.submissions,
            attendance = emptyList(),
            interventions = emptyList()
        )
        _uiState.update {
            it.copy(
                termWeightConfig = config,
                studentGradeSummaries = summaries,
                analytics = analytics,
                isTermWeightingDialogOpen = false,
                userNotificationMessage = "Term weighting configuration updated"
            )
        }
    }

    fun setAssignmentFilter(type: AssignmentType?, status: SubmissionStatus?) {
        _uiState.update { it.copy(assignmentFilterType = type, assignmentFilterStatus = status) }
    }

    fun generateFormattedGradebookReportText(): String {
        val activeClass = _uiState.value.activeClassroom ?: return "No active classroom."
        val assignments = _uiState.value.assignments
        val submissions = _uiState.value.submissions
        val students = _uiState.value.students

        val sb = StringBuilder()
        sb.append("====================================================\n")
        sb.append("              OFFICIAL CLASSROOM GRADEBOOK          \n")
        sb.append("====================================================\n\n")
        sb.append("Class: ${activeClass.name} • Subject: ${activeClass.subject}\n")
        sb.append("Grade Level: ${activeClass.gradeLevel} | Room: ${activeClass.roomNumber}\n\n")

        sb.append(String.format(Locale.US, "%-22s | %-8s | %-6s\n", "Student Name", "ID", "Subs"))
        sb.append("----------------------------------------------------\n")
        students.forEach { s ->
            val sSubs = submissions.filter { it.studentId == s.id }
            sb.append(String.format(Locale.US, "%-22s | %-8s | %-6d\n", s.name.take(22), s.studentNumber, sSubs.size))
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
