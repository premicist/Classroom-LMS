package com.aistudio.classroomlms.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.classroomlms.data.dao.ClassroomActivityDao
import com.aistudio.classroomlms.data.dao.StudentDao
import com.aistudio.classroomlms.data.database.PreferencesManager
import com.aistudio.classroomlms.data.entity.ClassroomActivityEntity
import com.aistudio.classroomlms.data.entity.StudentEntity
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

data class ClassroomActivityUiState(
    val activeClassroomId: Long? = null,
    val students: List<StudentEntity> = emptyList(),
    val activities: List<ClassroomActivityEntity> = emptyList(),
    val generatedGroups: List<List<StudentEntity>> = emptyList(),
    val pickedRandomStudent: StudentEntity? = null,
    val userNotificationMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ClassroomActivityViewModel @Inject constructor(
    private val activityDao: ClassroomActivityDao,
    private val studentDao: StudentDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClassroomActivityUiState())
    val uiState: StateFlow<ClassroomActivityUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.activeClassroomIdFlow
                .flatMapLatest { classroomId ->
                    _uiState.update { it.copy(activeClassroomId = classroomId) }
                    if (classroomId != null) {
                        combine(
                            studentDao.getStudentsByClassroom(classroomId),
                            activityDao.getActivitiesForClassroom(classroomId)
                        ) { students, activities ->
                            students to activities
                        }
                    } else {
                        flowOf(emptyList<StudentEntity>() to emptyList())
                    }
                }
                .collect { (students, activities) ->
                    _uiState.update {
                        it.copy(students = students, activities = activities)
                    }
                }
        }
    }

    fun saveActivity(activity: ClassroomActivityEntity) {
        viewModelScope.launch {
            try {
                activityDao.insertActivity(activity)
                showToast("Activity & winners recorded!")
            } catch (e: Exception) {
                showToast("Failed to save activity: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun deleteActivity(activity: ClassroomActivityEntity) {
        viewModelScope.launch {
            try {
                activityDao.deleteActivity(activity)
                showToast("Activity removed")
            } catch (e: Exception) {
                showToast("Failed to remove activity")
            }
        }
    }

    fun pickRandomStudent() {
        val list = _uiState.value.students
        if (list.isEmpty()) {
            showToast("No students in this class")
            return
        }
        _uiState.update { it.copy(pickedRandomStudent = list.random()) }
    }

    fun generateRandomGroups(groupSize: Int) {
        val list = _uiState.value.students
        if (list.isEmpty()) {
            showToast("No students in this class")
            return
        }
        val size = groupSize.coerceAtLeast(2)
        val groups = list.shuffled().chunked(size)
        _uiState.update { it.copy(generatedGroups = groups) }
    }

    fun clearGroups() {
        _uiState.update { it.copy(generatedGroups = emptyList(), pickedRandomStudent = null) }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(userNotificationMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(userNotificationMessage = null) }
    }
}
