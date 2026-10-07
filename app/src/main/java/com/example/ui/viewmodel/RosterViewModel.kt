package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.PreferencesManager
import com.example.data.entity.ClassroomEntity
import com.example.data.entity.StudentEntity
import com.example.data.repository.ClassroomRepository
import com.example.util.SpreadsheetUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RosterUiState(
    val classrooms: List<ClassroomEntity> = emptyList(),
    val students: List<StudentEntity> = emptyList(),
    val isAddEditClassroomOpen: Boolean = false,
    val editingClassroom: ClassroomEntity? = null,
    val isAddEditStudentOpen: Boolean = false,
    val editingStudent: StudentEntity? = null,
    val isClassroomModalOpen: Boolean = false,
    val isStudentRosterOpen: Boolean = false,
    val userNotificationMessage: String? = null
)

@HiltViewModel
class RosterViewModel @Inject constructor(
    private val application: Application,
    private val preferencesManager: PreferencesManager,
    database: AppDatabase
) : AndroidViewModel(application) {

    private val repository = ClassroomRepository(database)
    private val _uiState = MutableStateFlow(RosterUiState())
    val uiState: StateFlow<RosterUiState> = _uiState.asStateFlow()

    fun openAddClassroom() {
        _uiState.update { it.copy(isAddEditClassroomOpen = true, editingClassroom = null, isClassroomModalOpen = true) }
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

    fun openClassroomManagement() {
        _uiState.update { it.copy(isClassroomModalOpen = true) }
    }

    fun openStudentRoster() {
        _uiState.update { it.copy(isStudentRosterOpen = true) }
    }

    fun closeStudentRoster() {
        _uiState.update { it.copy(isStudentRosterOpen = false) }
    }

    fun closeDialogs() {
        _uiState.update {
            it.copy(
                isAddEditClassroomOpen = false, editingClassroom = null,
                isAddEditStudentOpen = false, editingStudent = null,
                isClassroomModalOpen = false
            )
        }
    }

    fun saveClassroom(classroom: ClassroomEntity) {
        viewModelScope.launch {
            if (classroom.id == 0L) {
                val newId = repository.insertClassroom(classroom)
                preferencesManager.saveActiveClassroomId(newId)
                showToast("Created classroom: ${classroom.name}")
            } else {
                repository.updateClassroom(classroom)
                showToast("Updated classroom: ${classroom.name}")
            }
            closeAddEditClassroom()
        }
    }

    fun deleteClassroom(id: Long) {
        viewModelScope.launch {
            repository.deleteClassroom(id)
            val remaining = _uiState.value.classrooms.filter { it.id != id }
            if (remaining.isNotEmpty()) {
                preferencesManager.saveActiveClassroomId(remaining.first().id)
            } else {
                preferencesManager.saveActiveClassroomId(null)
            }
            showToast("Classroom deleted")
        }
    }

    fun saveStudent(student: StudentEntity) {
        viewModelScope.launch {
            if (student.id == 0L) {
                repository.insertStudent(student)
                showToast("Added student: ${student.name}")
            } else {
                repository.updateStudent(student)
                showToast("Updated student: ${student.name}")
            }
            closeAddEditStudent()
        }
    }

    fun deleteStudent(id: Long) {
        viewModelScope.launch {
            repository.deleteStudent(id)
            showToast("Student removed from class")
        }
    }

    fun reassignStudentClassroom(student: StudentEntity, newClassroomId: Long) {
        if (student.classroomId == newClassroomId) return
        viewModelScope.launch {
            repository.updateStudent(student.copy(classroomId = newClassroomId))
            val className = _uiState.value.classrooms.find { it.id == newClassroomId }?.name ?: "new class"
            showToast("Moved ${student.name} to $className")
        }
    }

    fun updateClassrooms(classrooms: List<ClassroomEntity>) {
        _uiState.update { it.copy(classrooms = classrooms) }
    }

    fun updateStudents(students: List<StudentEntity>) {
        _uiState.update { it.copy(students = students) }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(userNotificationMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(userNotificationMessage = null) }
    }

    fun importStudentsFromCsv(classroomId: Long, uri: Uri) {
        viewModelScope.launch {
            try {
                val csvContent = application.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                } ?: throw IllegalStateException("Could not read file from selected URI")

                val parsed = SpreadsheetUtils.parseCsvRosterText(csvContent)
                if (parsed.isEmpty()) {
                    showToast("No valid student rows found in file")
                    return@launch
                }

                val avatarColors = listOf(
                    0xFF6750A4, 0xFF00639B, 0xFF825500, 0xFF059669, 0xFF2563EB, 0xFF7D5260, 0xFFD97706, 0xFF0284C7
                )

                val entities = parsed.mapIndexed { idx, p ->
                    StudentEntity(
                        classroomId = classroomId,
                        name = p.name,
                        studentNumber = p.studentNumber,
                        email = p.email,
                        guardianContact = p.guardianContact,
                        notes = p.notes,
                        avatarColorHex = avatarColors[idx % avatarColors.size]
                    )
                }

                repository.insertStudents(entities)
                showToast("Successfully imported ${entities.size} students!")
            } catch (e: Exception) {
                showToast("CSV Import failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }
}
