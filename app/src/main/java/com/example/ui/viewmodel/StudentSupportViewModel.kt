package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.dao.ClassroomDao
import com.example.data.dao.DisciplineDao
import com.example.data.dao.InterventionDao
import com.example.data.dao.LiveAssessmentDao
import com.example.data.dao.StudentDao
import com.example.data.database.AppDatabase
import com.example.data.database.PreferencesManager
import com.example.data.entity.ClassroomEntity
import com.example.data.entity.DisciplineRecordEntity
import com.example.data.entity.InterventionEntity
import com.example.data.entity.InterventionType
import com.example.data.entity.LiveAssessmentEntity
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

data class StudentSupportUiState(
    val activeClassroomId: Long? = null,
    val activeClassroom: ClassroomEntity? = null,
    val students: List<StudentEntity> = emptyList(),
    val disciplineRecords: List<DisciplineRecordEntity> = emptyList(),
    val interventions: List<InterventionEntity> = emptyList(),
    val liveAssessments: List<LiveAssessmentEntity> = emptyList(),
    val isAddEditDisciplineOpen: Boolean = false,
    val disciplineStudent: StudentEntity? = null,
    val editingDisciplineRecord: DisciplineRecordEntity? = null,
    val isAddInterventionOpen: Boolean = false,
    val interventionStudent: StudentEntity? = null,
    val editingIntervention: InterventionEntity? = null,
    val isLiveAssessmentDialogOpen: Boolean = false,
    val liveAssessmentStudent: StudentEntity? = null,
    val editingLiveAssessment: LiveAssessmentEntity? = null,
    val userNotificationMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StudentSupportViewModel @Inject constructor(
    private val disciplineDao: DisciplineDao,
    private val interventionDao: InterventionDao,
    private val liveAssessmentDao: LiveAssessmentDao,
    private val studentDao: StudentDao,
    private val classroomDao: ClassroomDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentSupportUiState())
    val uiState: StateFlow<StudentSupportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.activeClassroomIdFlow
                .flatMapLatest { classroomId ->
                    _uiState.update { it.copy(activeClassroomId = classroomId) }
                    if (classroomId != null) {
                        combine(
                            classroomDao.getClassroomById(classroomId),
                            studentDao.getStudentsByClassroom(classroomId),
                            disciplineDao.getDisciplineRecordsByClassroom(classroomId),
                            interventionDao.getInterventionsByClassroom(classroomId),
                            liveAssessmentDao.getAssessmentsByClassroom(classroomId)
                        ) { classroom, students, disciplines, interventions, liveAssessments ->
                            SupportData(classroom, students, disciplines, interventions, liveAssessments)
                        }
                    } else {
                        flowOf(SupportData(null, emptyList(), emptyList(), emptyList(), emptyList()))
                    }
                }
                .collect { data ->
                    _uiState.update { current ->
                        current.copy(
                            activeClassroom = data.classroom,
                            students = data.students,
                            disciplineRecords = data.disciplines,
                            interventions = data.interventions,
                            liveAssessments = data.liveAssessments
                        )
                    }
                }
        }
    }

    private data class SupportData(
        val classroom: ClassroomEntity?,
        val students: List<StudentEntity>,
        val disciplines: List<DisciplineRecordEntity>,
        val interventions: List<InterventionEntity>,
        val liveAssessments: List<LiveAssessmentEntity>
    )

    // --- DISCIPLINE & BEHAVIOR LOGS ---

    fun openAddDiscipline(student: StudentEntity) {
        _uiState.update {
            it.copy(
                isAddEditDisciplineOpen = true,
                disciplineStudent = student,
                editingDisciplineRecord = null
            )
        }
    }

    fun openEditDiscipline(record: DisciplineRecordEntity, student: StudentEntity) {
        _uiState.update {
            it.copy(
                isAddEditDisciplineOpen = true,
                disciplineStudent = student,
                editingDisciplineRecord = record
            )
        }
    }

    fun closeDisciplineDialog() {
        _uiState.update {
            it.copy(
                isAddEditDisciplineOpen = false,
                disciplineStudent = null,
                editingDisciplineRecord = null
            )
        }
    }

    fun saveDisciplineRecord(record: DisciplineRecordEntity) {
        viewModelScope.launch {
            if (record.id == 0L) {
                disciplineDao.insertDisciplineRecord(record)
                showToast("Disciplinary incident logged")
            } else {
                disciplineDao.updateDisciplineRecord(record)
                showToast("Disciplinary record updated")
            }
            closeDisciplineDialog()
        }
    }

    fun deleteDisciplineRecord(record: DisciplineRecordEntity) {
        viewModelScope.launch {
            disciplineDao.deleteDisciplineRecord(record)
            showToast("Disciplinary record deleted")
            closeDisciplineDialog()
        }
    }

    fun exportDisciplineIncidentSlip(context: Context, student: StudentEntity, record: DisciplineRecordEntity) {
        val classroomName = _uiState.value.activeClassroom?.name ?: "Classroom"
        PdfReportExporter.exportDisciplineIncidentSlip(context, student, record, classroomName)
        showToast("Generating Discipline Incident Slip PDF...")
    }

    // --- TEACHER INTERVENTIONS ---

    fun openAddIntervention(student: StudentEntity) {
        _uiState.update {
            it.copy(
                isAddInterventionOpen = true,
                interventionStudent = student,
                editingIntervention = null
            )
        }
    }

    fun openEditIntervention(intervention: InterventionEntity) {
        _uiState.update {
            it.copy(
                isAddInterventionOpen = true,
                interventionStudent = _uiState.value.students.find { s -> s.id == intervention.studentId },
                editingIntervention = intervention
            )
        }
    }

    fun closeInterventionDialog() {
        _uiState.update {
            it.copy(
                isAddInterventionOpen = false,
                interventionStudent = null,
                editingIntervention = null
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
        val classroomId = _uiState.value.activeClassroomId ?: return
        viewModelScope.launch {
            val editing = _uiState.value.editingIntervention
            if (editing != null) {
                val updated = editing.copy(
                    studentId = studentId,
                    type = type,
                    title = title,
                    notes = notes,
                    date = date,
                    resolved = resolved
                )
                interventionDao.updateIntervention(updated)
                showToast("Intervention note updated")
            } else {
                val newIntervention = InterventionEntity(
                    classroomId = classroomId,
                    studentId = studentId,
                    type = type,
                    title = title,
                    notes = notes,
                    date = date,
                    resolved = resolved
                )
                interventionDao.insertIntervention(newIntervention)
                showToast("Teacher intervention recorded")
            }
            closeInterventionDialog()
        }
    }

    fun deleteIntervention(id: Long) {
        viewModelScope.launch {
            interventionDao.deleteInterventionById(id)
            showToast("Intervention removed")
            closeInterventionDialog()
        }
    }

    // --- LIVE CLASS FORMATIVE ASSESSMENTS ---

    fun openLiveAssessment(student: StudentEntity? = null) {
        _uiState.update {
            it.copy(
                isLiveAssessmentDialogOpen = true,
                liveAssessmentStudent = student,
                editingLiveAssessment = null
            )
        }
    }

    fun openEditLiveAssessment(assessment: LiveAssessmentEntity) {
        _uiState.update {
            it.copy(
                isLiveAssessmentDialogOpen = true,
                liveAssessmentStudent = _uiState.value.students.find { s -> s.id == assessment.studentId },
                editingLiveAssessment = assessment
            )
        }
    }

    fun closeLiveAssessmentDialog() {
        _uiState.update {
            it.copy(
                isLiveAssessmentDialogOpen = false,
                liveAssessmentStudent = null,
                editingLiveAssessment = null
            )
        }
    }

    fun saveLiveAssessment(assessment: LiveAssessmentEntity) {
        val classroomId = _uiState.value.activeClassroomId ?: return
        viewModelScope.launch {
            if (assessment.id == 0L) {
                liveAssessmentDao.insertAssessment(assessment.copy(classroomId = classroomId))
                showToast("Formative observation recorded")
            } else {
                liveAssessmentDao.updateAssessment(assessment)
                showToast("Formative observation updated")
            }
            closeLiveAssessmentDialog()
        }
    }

    fun deleteLiveAssessment(assessment: LiveAssessmentEntity) {
        viewModelScope.launch {
            liveAssessmentDao.deleteAssessment(assessment)
            showToast("Observation deleted")
            closeLiveAssessmentDialog()
        }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(userNotificationMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(userNotificationMessage = null) }
    }
}
