package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.LmsSettings
import com.example.data.database.PreferencesManager
import com.example.data.entity.ClassScheduleEntity
import com.example.data.repository.DatabaseBackupManager
import com.example.data.repository.UpdateInfo
import com.example.data.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class SettingsUiState(
    val settings: LmsSettings = LmsSettings(),
    val isAppLocked: Boolean = false,
    val isCheckingForUpdates: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val isUpdateDialogOpen: Boolean = false,
    val userNotificationMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val preferencesManager: PreferencesManager,
    private val database: AppDatabase,
    private val updateRepository: UpdateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val backupManager by lazy { DatabaseBackupManager(application, database) }
    private var isFirstSettingsLoad = true

    init {
        viewModelScope.launch {
            preferencesManager.settingsFlow.collect { settings ->
                _uiState.update { state ->
                    val shouldLock = if (isFirstSettingsLoad && settings.isBiometricLockEnabled) true else state.isAppLocked
                    state.copy(settings = settings, isAppLocked = shouldLock)
                }
                isFirstSettingsLoad = false
            }
        }
    }

    fun saveSettings(settings: LmsSettings) {
        viewModelScope.launch {
            try {
                preferencesManager.saveSettings(settings)
                showToast("Settings & Templates Saved Successfully!")
            } catch (e: Exception) {
                showToast("Failed to save settings")
            }
        }
    }

    fun setAppLocked(locked: Boolean) {
        _uiState.update { it.copy(isAppLocked = locked) }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = backupManager.exportDatabaseBackup(uri)
            result.onSuccess {
                _uiState.update { it.copy(userNotificationMessage = "Backup exported successfully!") }
            }.onFailure { e ->
                _uiState.update { it.copy(userNotificationMessage = "Failed to export backup: ${e.message}") }
            }
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = backupManager.importDatabaseBackup(uri)
            result.onSuccess {
                val classrooms = database.classroomDao().getAllClassroomsOnce()
                if (classrooms.isNotEmpty()) {
                    preferencesManager.saveActiveClassroomId(classrooms.first().id)
                } else {
                    preferencesManager.saveActiveClassroomId(null)
                }
                _uiState.update { it.copy(userNotificationMessage = "Database restored successfully!") }
            }.onFailure { e ->
                _uiState.update { it.copy(userNotificationMessage = "Restore failed: ${e.message}") }
            }
        }
    }

    fun checkForUpdates(isManual: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingForUpdates = true) }
            val result = updateRepository.checkLatestRelease()
            result.onSuccess { updateInfo ->
                _uiState.update {
                    it.copy(
                        isCheckingForUpdates = false,
                        updateInfo = updateInfo,
                        isUpdateDialogOpen = updateInfo.isUpdateAvailable
                    )
                }
                if (!updateInfo.isUpdateAvailable && isManual) {
                    showToast("Classroom LMS is up to date (${updateInfo.currentVersionName})")
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isCheckingForUpdates = false) }
                if (isManual) {
                    showToast("Unable to check for updates: ${error.localizedMessage ?: "Network error"}")
                }
            }
        }
    }

    fun closeUpdateDialog() {
        _uiState.update { it.copy(isUpdateDialogOpen = false) }
    }

    suspend fun getTodayClassesSummary(): Pair<Boolean, String> {
        val cal = Calendar.getInstance()
        val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "MONDAY"
            Calendar.TUESDAY -> "TUESDAY"
            Calendar.WEDNESDAY -> "WEDNESDAY"
            Calendar.THURSDAY -> "THURSDAY"
            Calendar.FRIDAY -> "FRIDAY"
            Calendar.SATURDAY -> "SATURDAY"
            Calendar.SUNDAY -> "SUNDAY"
            else -> "MONDAY"
        }
        if (dayOfWeek == "SATURDAY" || dayOfWeek == "SUNDAY") {
            return Pair(true, "Today is Holiday, Relax")
        }
        var masterSchedules: List<ClassScheduleEntity> = emptyList()
        var foundToday = false
        var todaySummary = ""
        viewModelScope.launch(Dispatchers.IO) {
            masterSchedules = database.classScheduleDao().getAllSchedulesOnce()
            val todaySchedules = masterSchedules
                .filter { it.dayOfWeek.equals(dayOfWeek, ignoreCase = true) }
                .distinctBy { "${it.classroomName}_${it.startMinutes}" }
                .sortedBy { it.startMinutes }
            if (todaySchedules.isEmpty()) {
                foundToday = false
                todaySummary = "No classes today."
            } else {
                foundToday = true
                todaySummary = todaySchedules.joinToString(", ") { entry ->
                    "\"${entry.classroomName}\" @ ${entry.startTime}"
                }
            }
        }
        return Pair(foundToday, todaySummary)
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(userNotificationMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(userNotificationMessage = null) }
    }
}
