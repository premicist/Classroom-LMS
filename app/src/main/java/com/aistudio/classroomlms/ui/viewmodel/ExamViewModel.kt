package com.aistudio.classroomlms.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.classroomlms.data.dao.ExamDao
import com.aistudio.classroomlms.data.dao.StudentDao
import com.aistudio.classroomlms.data.database.PreferencesManager
import com.aistudio.classroomlms.data.entity.ExamCategory
import com.aistudio.classroomlms.data.entity.ExamEntity
import com.aistudio.classroomlms.data.entity.ExamMarkEntity
import com.aistudio.classroomlms.data.entity.StudentEntity
import com.aistudio.classroomlms.domain.merit.LeaderboardSummary
import com.aistudio.classroomlms.domain.merit.MeritLeaderboardEngine
import com.aistudio.classroomlms.domain.merit.StudentExamAggregate
import com.aistudio.classroomlms.util.ParentCommunicationHelper
import com.aistudio.classroomlms.util.PdfReportExporter
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
import javax.inject.Inject

enum class ExamHubTab(val title: String) {
    MARKS("Marks Entry"),
    LEADERBOARD("Toppers Podium"),
    DATESHEET("Exam Routine"),
    REPORTS("Marksheets")
}

data class ExamUiState(
    val activeClassroomId: Long? = null,
    val exams: List<ExamEntity> = emptyList(),
    val examMarks: List<ExamMarkEntity> = emptyList(),
    val students: List<StudentEntity> = emptyList(),
    val isAddEditExamOpen: Boolean = false,
    val selectedExamCategory: ExamCategory = ExamCategory.CLASS_TEST,
    val selectedHubTab: ExamHubTab = ExamHubTab.MARKS,
    val leaderboardSummary: LeaderboardSummary = LeaderboardSummary(),
    val editingExam: ExamEntity? = null,
    val gradingExam: ExamEntity? = null,
    val userNotificationMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExamViewModel @Inject constructor(
    private val examDao: ExamDao,
    private val studentDao: StudentDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.activeClassroomIdFlow
                .flatMapLatest { classroomId ->
                    _uiState.update { it.copy(activeClassroomId = classroomId) }
                    if (classroomId != null) {
                        combine(
                            examDao.getExamsForClassroom(classroomId),
                            examDao.getMarksForClassroom(classroomId),
                            studentDao.getStudentsByClassroom(classroomId)
                        ) { exams, marks, students ->
                            Triple(exams, marks, students)
                        }
                    } else {
                        flowOf(Triple(emptyList<ExamEntity>(), emptyList<ExamMarkEntity>(), emptyList<StudentEntity>()))
                    }
                }
                .collect { (exams, marks, students) ->
                    _uiState.update { current ->
                        val categoryExams = exams.filter { it.category == current.selectedExamCategory }
                        val leaderboard = MeritLeaderboardEngine.computeLeaderboard(
                            students = students,
                            exams = categoryExams,
                            marks = marks
                        )
                        current.copy(
                            exams = exams,
                            examMarks = marks,
                            students = students,
                            leaderboardSummary = leaderboard
                        )
                    }
                }
        }
    }

    fun selectHubTab(tab: ExamHubTab) {
        _uiState.update { it.copy(selectedHubTab = tab) }
    }

    fun selectCategory(category: ExamCategory) {
        _uiState.update { current ->
            val categoryExams = current.exams.filter { it.category == category }
            val leaderboard = MeritLeaderboardEngine.computeLeaderboard(
                students = current.students,
                exams = categoryExams,
                marks = current.examMarks
            )
            current.copy(
                selectedExamCategory = category,
                leaderboardSummary = leaderboard
            )
        }
    }

    fun openGradingWorkspace(exam: ExamEntity) {
        _uiState.update { it.copy(gradingExam = exam) }
    }

    fun closeGradingWorkspace() {
        _uiState.update { it.copy(gradingExam = null) }
    }

    fun openAddEditExamDialog(category: ExamCategory, exam: ExamEntity? = null) {
        _uiState.update {
            it.copy(
                isAddEditExamOpen = true,
                selectedExamCategory = category,
                editingExam = exam
            )
        }
    }

    fun closeAddEditExamDialog() {
        _uiState.update {
            it.copy(
                isAddEditExamOpen = false,
                editingExam = null
            )
        }
    }

    fun saveExam(exam: ExamEntity) {
        viewModelScope.launch {
            try {
                if (exam.id == 0L) {
                    examDao.insertExam(exam)
                } else {
                    examDao.updateExam(exam)
                }
            } catch (e: Exception) {
                showToast("Failed to save exam")
            }
        }
    }

    fun saveExamMark(mark: ExamMarkEntity) {
        viewModelScope.launch {
            try {
                if (mark.id == 0L) {
                    examDao.insertMark(mark)
                } else {
                    examDao.updateMark(mark)
                }
            } catch (e: Exception) {
                showToast("Failed to save mark")
            }
        }
    }

    fun deleteExam(exam: ExamEntity) {
        viewModelScope.launch {
            try {
                examDao.deleteExam(exam)
                showToast("Exam deleted")
            } catch (e: Exception) {
                showToast("Failed to delete exam")
            }
        }
    }

    fun sendWhatsAppReport(context: Context, aggregate: StudentExamAggregate, classroomName: String = "") {
        val categoryName = _uiState.value.selectedExamCategory.displayName
        val message = ParentCommunicationHelper.formatExamReportMessage(
            studentName = aggregate.studentName,
            studentRoll = aggregate.studentNumber,
            examCategory = categoryName,
            totalMarks = aggregate.totalMarksObtained,
            fullMarks = aggregate.totalFullMarks,
            percentage = aggregate.percentage,
            gpa = aggregate.gpa,
            letterGrade = aggregate.letterGrade,
            rank = aggregate.rank,
            classroomName = classroomName
        )
        val success = ParentCommunicationHelper.launchWhatsApp(context, aggregate.guardianContact, message)
        if (!success) {
            showToast("Could not open WhatsApp. Ensure WhatsApp is installed.")
        }
    }

    fun sendSmsReport(context: Context, aggregate: StudentExamAggregate, classroomName: String = "") {
        val categoryName = _uiState.value.selectedExamCategory.displayName
        val message = ParentCommunicationHelper.formatExamReportMessage(
            studentName = aggregate.studentName,
            studentRoll = aggregate.studentNumber,
            examCategory = categoryName,
            totalMarks = aggregate.totalMarksObtained,
            fullMarks = aggregate.totalFullMarks,
            percentage = aggregate.percentage,
            gpa = aggregate.gpa,
            letterGrade = aggregate.letterGrade,
            rank = aggregate.rank,
            classroomName = classroomName
        )
        val success = ParentCommunicationHelper.launchSms(context, aggregate.guardianContact, message)
        if (!success) {
            showToast("Could not open SMS application.")
        }
    }

    fun exportExamReport(context: Context, exam: ExamEntity) {
        val marks = _uiState.value.examMarks.filter { it.examId == exam.id }
        val students = _uiState.value.students
        PdfReportExporter.exportExamReport(context, exam, marks, students)
        showToast("Exporting PDF for ${exam.title}...")
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(userNotificationMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(userNotificationMessage = null) }
    }
}
