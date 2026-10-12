package com.aistudio.classroomlms.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.classroomlms.data.database.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Manages biometric / PIN app lock state across configuration changes, rotations, and process lifecycle.
 * Activities and Composables observe this state; authentication prompts trigger unlock upon success.
 */
@HiltViewModel
class SessionLockViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var hasEvaluatedInitialLock = false

    init {
        viewModelScope.launch {
            preferencesManager.settingsFlow.collect { settings ->
                if (!hasEvaluatedInitialLock) {
                    if (settings.isBiometricLockEnabled) {
                        _isLocked.update { true }
                    }
                    hasEvaluatedInitialLock = true
                } else if (!settings.isBiometricLockEnabled) {
                    _isLocked.update { false }
                }
            }
        }
    }

    fun lock() {
        _isLocked.update { true }
    }

    fun unlock() {
        _isLocked.update { false }
    }
}