package com.aistudio.classroomlms.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.classroomlms.data.dao.ClassroomDao
import com.aistudio.classroomlms.data.dao.SchoolDao
import com.aistudio.classroomlms.data.entity.ClassroomEntity
import com.aistudio.classroomlms.data.entity.SchoolEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Filter sentinel for School selector:
 * - `null`: All Schools & Classes
 * - `UNASSIGNED_SCHOOL_ID` (-1L): Only classes with `schoolId == null`
 * - `> 0L`: Specific School / College
 */
const val UNASSIGNED_SCHOOL_ID = -1L

data class SchoolUiState(
    val schools: List<SchoolEntity> = emptyList(),
    val classrooms: List<ClassroomEntity> = emptyList(),
    val selectedSchoolFilterId: Long? = null,
    val isAddEditSchoolOpen: Boolean = false,
    val editingSchool: SchoolEntity? = null,
    val userNotificationMessage: String? = null
)

@HiltViewModel
class SchoolViewModel @Inject constructor(
    private val schoolDao: SchoolDao,
    private val classroomDao: ClassroomDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SchoolUiState())
    val uiState: StateFlow<SchoolUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                schoolDao.getAllSchools(),
                classroomDao.getAllClassrooms()
            ) { schools, classrooms ->
                schools to classrooms
            }.collect { (schools, classrooms) ->
                _uiState.update { it.copy(schools = schools, classrooms = classrooms) }
            }
        }
    }

    fun selectSchoolFilter(schoolId: Long?) {
        _uiState.update { it.copy(selectedSchoolFilterId = schoolId) }
    }

    fun openAddSchool() {
        _uiState.update { it.copy(isAddEditSchoolOpen = true, editingSchool = null) }
    }

    fun openEditSchool(school: SchoolEntity) {
        _uiState.update { it.copy(isAddEditSchoolOpen = true, editingSchool = school) }
    }

    fun closeSchoolDialog() {
        _uiState.update { it.copy(isAddEditSchoolOpen = false, editingSchool = null) }
    }

    fun saveSchool(school: SchoolEntity) {
        viewModelScope.launch {
            try {
                if (school.id == 0L) {
                    schoolDao.insertSchool(school)
                    showToast("${school.institutionType.displayName} added")
                } else {
                    schoolDao.updateSchool(school)
                    showToast("${school.institutionType.displayName} updated")
                }
                closeSchoolDialog()
            } catch (e: Exception) {
                showToast("Failed to save: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun deleteSchool(school: SchoolEntity) {
        viewModelScope.launch {
            try {
                classroomDao.clearSchoolAssignmentForSchool(school.id)
                schoolDao.deleteSchool(school)
                if (_uiState.value.selectedSchoolFilterId == school.id) {
                    _uiState.update { it.copy(selectedSchoolFilterId = null) }
                }
                showToast("${school.name} removed (classes moved to Not Assigned)")
            } catch (e: Exception) {
                showToast("Failed to remove school")
            }
        }
    }

    fun assignClassroomToSchool(classroomId: Long, schoolId: Long?) {
        viewModelScope.launch {
            try {
                classroomDao.assignClassroomToSchool(classroomId, schoolId)
                showToast("Class assignment updated")
            } catch (e: Exception) {
                showToast("Failed to assign class")
            }
        }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(userNotificationMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(userNotificationMessage = null) }
    }
}
