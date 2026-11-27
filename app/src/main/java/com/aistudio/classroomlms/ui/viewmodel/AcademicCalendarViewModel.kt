package com.aistudio.classroomlms.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.classroomlms.data.dao.AcademicEventDao
import com.aistudio.classroomlms.data.entity.AcademicEventCategory
import com.aistudio.classroomlms.data.entity.AcademicEventEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AcademicCalendarUiState(
    val events: List<AcademicEventEntity> = emptyList(),
    val selectedCategory: AcademicEventCategory? = null,
    val isAddEditDialogOpen: Boolean = false,
    val editingEvent: AcademicEventEntity? = null,
    val userNotificationMessage: String? = null
)

@HiltViewModel
class AcademicCalendarViewModel @Inject constructor(
    private val academicEventDao: AcademicEventDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AcademicCalendarUiState())
    val uiState: StateFlow<AcademicCalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            academicEventDao.getAllEvents().collect { list ->
                _uiState.update { it.copy(events = list) }
            }
        }
    }

    fun selectCategoryFilter(category: AcademicEventCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun openAddEvent() {
        _uiState.update { it.copy(isAddEditDialogOpen = true, editingEvent = null) }
    }

    fun openEditEvent(event: AcademicEventEntity) {
        _uiState.update { it.copy(isAddEditDialogOpen = true, editingEvent = event) }
    }

    fun closeDialog() {
        _uiState.update { it.copy(isAddEditDialogOpen = false, editingEvent = null) }
    }

    fun saveEvent(event: AcademicEventEntity) {
        viewModelScope.launch {
            try {
                if (event.id == 0L) {
                    academicEventDao.insertEvent(event)
                    showToast("Academic event added")
                } else {
                    academicEventDao.updateEvent(event)
                    showToast("Academic event updated")
                }
                closeDialog()
            } catch (e: Exception) {
                showToast("Failed to save event: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun deleteEvent(event: AcademicEventEntity) {
        viewModelScope.launch {
            try {
                academicEventDao.deleteEvent(event)
                showToast("Event removed")
            } catch (e: Exception) {
                showToast("Failed to delete event")
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
