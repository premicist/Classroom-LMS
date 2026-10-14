package com.aistudio.classroomlms.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.classroomlms.data.auth.AuthManager
import com.aistudio.classroomlms.data.dao.ClassroomDao
import com.aistudio.classroomlms.data.database.AppDatabase
import com.aistudio.classroomlms.data.database.PreferencesManager
import com.aistudio.classroomlms.data.entity.ClassroomEntity
import com.aistudio.classroomlms.data.repository.SyncRepository
import com.aistudio.classroomlms.util.SpreadsheetUtils
import com.aistudio.classroomlms.util.SafeLogger
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SyncUiState(
    val activeClassroomId: Long? = null,
    val activeClassroom: ClassroomEntity? = null,
    val isDownloading: Boolean = false,
    val isUploading: Boolean = false,
    val transferProgressMessage: String? = null,
    val isGoogleSignedIn: Boolean = false,
    val userNotificationMessage: String? = null,
    val lastDownloadResult: String? = null,
    val lastUploadResult: String? = null
) {
    val isTransferRunning: Boolean get() = isDownloading || isUploading
}

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
                    classroomId?.let(preferencesManager::sheetsLastDownloadFlow) ?: flowOf(null)
                }
                .collect { result -> _uiState.update { it.copy(lastDownloadResult = result) } }
        }
        viewModelScope.launch {
            preferencesManager.activeClassroomIdFlow
                .flatMapLatest { classroomId ->
                    classroomId?.let(preferencesManager::sheetsLastUploadFlow) ?: flowOf(null)
                }
                .collect { result -> _uiState.update { it.copy(lastUploadResult = result) } }
        }
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

    fun downloadFromSheet(classroomId: Long) {
        runSheetAction(classroomId, isUpload = false)
    }

    fun uploadToSheet(classroomId: Long) {
        runSheetAction(classroomId, isUpload = true)
    }

    private fun runSheetAction(classroomId: Long, isUpload: Boolean) {
        val classroom = _uiState.value.activeClassroom?.takeIf { it.id == classroomId }
            ?: return
        if (classroom.linkedSpreadsheetId.isNullOrBlank()) {
            showToast("No Google Sheet linked to this class")
            return
        }

        val cleanSpreadsheetId = SpreadsheetUtils.extractSpreadsheetId(classroom.linkedSpreadsheetId)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDownloading = !isUpload,
                    isUploading = isUpload,
                    transferProgressMessage = if (isUpload) "Uploading to Google Sheets..." else "Downloading from Google Sheets..."
                )
            }
            try {
                if (!authManager.isUserSignedIn()) {
                    showToast("Sign in with Google in Settings to use Sheets.")
                    return@launch
                }

                val token = authManager.getAccessToken()
                if (token == null) {
                    showToast("Could not authorize Google Sheets. Sign out and sign in again in Settings.")
                    return@launch
                }

                val syncRepo = SyncRepository(database, token)
                val result = if (isUpload) {
                    syncRepo.uploadToSheets(classroomId, cleanSpreadsheetId)
                } else {
                    syncRepo.downloadFromSheets(classroomId, cleanSpreadsheetId)
                }
                val status = "${timestamp()}: $result"
                if (isUpload) {
                    preferencesManager.saveSheetsLastUpload(classroomId, status)
                } else {
                    preferencesManager.saveSheetsLastDownload(classroomId, status)
                }
                showToast(result)
            } catch (e: Exception) {
                SafeLogger.e(
                    "SyncViewModel",
                    if (isUpload) "Google Sheets upload failed" else "Google Sheets download failed",
                    e
                )
                val result = "${timestamp()}: Failed — check connection, authorization, and Sheet access."
                if (isUpload) {
                    preferencesManager.saveSheetsLastUpload(classroomId, result)
                } else {
                    preferencesManager.saveSheetsLastDownload(classroomId, result)
                }
                showToast(if (isUpload) "Upload failed. Check connection, authorization, and Sheet access." else "Download failed. Check connection, authorization, and Sheet access.")
            } finally {
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        isUploading = false,
                        transferProgressMessage = null
                    )
                }
            }
        }
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

    fun showToast(message: String) {
        _uiState.update { it.copy(userNotificationMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(userNotificationMessage = null) }
    }
}
