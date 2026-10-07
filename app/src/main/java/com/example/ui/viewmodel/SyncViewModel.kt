package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthManager
import com.example.data.dao.ClassroomDao
import com.example.data.database.AppDatabase
import com.example.data.database.PreferencesManager
import com.example.data.entity.ClassroomEntity
import com.example.data.repository.SyncRepository
import com.example.util.SpreadsheetUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncUiState(
    val activeClassroomId: Long? = null,
    val activeClassroom: ClassroomEntity? = null,
    val isSyncing: Boolean = false,
    val syncProgressMessage: String? = null,
    val isGoogleSignedIn: Boolean = false,
    val userNotificationMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SyncViewModel @Inject constructor(
    val authManager: AuthManager,
    private val classroomDao: ClassroomDao,
    private val preferencesManager: PreferencesManager,
    private val database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    init {
        checkSignInStatus()
        viewModelScope.launch {
            preferencesManager.activeClassroomIdFlow
                .flatMapLatest { classroomId ->
                    _uiState.update { it.copy(activeClassroomId = classroomId) }
                    if (classroomId != null) {
                        classroomDao.getClassroomById(classroomId)
                    } else {
                        flowOf(null)
                    }
                }
                .collect { classroom ->
                    _uiState.update { it.copy(activeClassroom = classroom) }
                }
        }
    }

    fun checkSignInStatus() {
        _uiState.update { it.copy(isGoogleSignedIn = authManager.isUserSignedIn()) }
    }

    fun linkSpreadsheet(classroomId: Long, spreadsheetId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanId = SpreadsheetUtils.extractSpreadsheetId(spreadsheetId)
            val classroom = classroomDao.getClassroomByIdOnce(classroomId) ?: return@launch
            classroomDao.updateClassroom(classroom.copy(linkedSpreadsheetId = cleanId))
            val msg = if (cleanId.isNotBlank()) "Linked classroom to Google Sheet: $cleanId" else "Unlinked Google Sheet from classroom"
            showToast(msg)
        }
    }

    fun syncGoogleSheet(classroomId: Long) {
        val classroom = _uiState.value.activeClassroom ?: return
        if (classroom.linkedSpreadsheetId.isNullOrBlank()) {
            showToast("No Google Sheet linked to this classroom")
            return
        }

        val cleanSpreadsheetId = SpreadsheetUtils.extractSpreadsheetId(classroom.linkedSpreadsheetId)

        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncProgressMessage = "Connecting to Google Sheets...") }
            try {
                if (!authManager.isUserSignedIn()) {
                    showToast("Please sign in with Google first (open navigation menu).")
                    _uiState.update { it.copy(isSyncing = false, syncProgressMessage = null) }
                    return@launch
                }

                val token = authManager.getAccessToken()
                if (token == null) {
                    showToast("Failed to get Google Token. Please sign out and sign in again.")
                    _uiState.update { it.copy(isSyncing = false, syncProgressMessage = null) }
                    return@launch
                }

                _uiState.update { it.copy(syncProgressMessage = "Syncing roster with Google Sheets...") }
                val syncRepo = SyncRepository(database, token)

                // 1. Pull Students (Roster)
                val pullResult = syncRepo.syncClassroomRoster(classroomId, cleanSpreadsheetId)

                _uiState.update { it.copy(syncProgressMessage = "Pushing attendance, homework & grades...") }

                // 2. Push Grades, Attendance, and Homework
                val pushResult = syncRepo.exportToSheets(classroomId, cleanSpreadsheetId)

                showToast("Sync Complete: $pullResult | $pushResult")
            } catch (e: Exception) {
                showToast("Sync Error: ${e.localizedMessage ?: "Unknown network error"}")
            } finally {
                _uiState.update { it.copy(isSyncing = false, syncProgressMessage = null) }
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
