package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.dao.ExamDao
import com.example.data.dao.StudentDao
import com.example.data.database.PreferencesManager
import com.example.data.entity.ExamCategory
import com.example.data.entity.ExamEntity
import com.example.data.entity.ExamMarkEntity
import com.example.data.entity.StudentEntity
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
import javax.inject.Inject

data class ExamUiState(
    val activeClassroomId: Long? = null,
    val exams: List<ExamEntity> = emptyList(),
    val examMarks: List<ExamMarkEntity> = emptyList(),
    val students: List<StudentEntity> = emptyList(),
    val isAddEditExamOpen: Boolean = false,
    val selectedExamCategory: ExamCategory = ExamCategory.CLASS_TEST,
    val editingExam: ExamEntity? = null,
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
                        current.copy(
                            exams = exams,
                            examMarks = marks,
                            students = students
                        )
                    }
                }
        }
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
