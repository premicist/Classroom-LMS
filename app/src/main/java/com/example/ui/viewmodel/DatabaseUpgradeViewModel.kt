package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.LegacyDatabaseMigrator
import com.example.data.database.MigrationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DatabaseUpgradeState {
    object Idle : DatabaseUpgradeState()
    object Upgrading : DatabaseUpgradeState()
    object Completed : DatabaseUpgradeState()
    data class Failed(val error: Throwable, val message: String) : DatabaseUpgradeState()
}

@HiltViewModel
class DatabaseUpgradeViewModel @Inject constructor(
    private val migrator: LegacyDatabaseMigrator
) : ViewModel() {

    private val _upgradeState = MutableStateFlow<DatabaseUpgradeState>(DatabaseUpgradeState.Idle)
    val upgradeState: StateFlow<DatabaseUpgradeState> = _upgradeState.asStateFlow()

    init {
        checkUpgradeState()
    }

    fun checkUpgradeState() {
        if (migrator.isMigrationFailed()) {
            val failure = migrator.getLastFailure()
            _upgradeState.value = DatabaseUpgradeState.Failed(
                error = failure?.error ?: Exception("Database upgrade failed"),
                message = failure?.message ?: "Unable to safely migrate legacy database."
            )
        } else if (migrator.isMigrationRequired()) {
            _upgradeState.value = DatabaseUpgradeState.Upgrading
            viewModelScope.launch(Dispatchers.IO) {
                val result = migrator.performMigration()
                when (result) {
                    is MigrationResult.Success, is MigrationResult.AlreadyMigrated, is MigrationResult.NotRequired -> {
                        _upgradeState.value = DatabaseUpgradeState.Completed
                    }
                    is MigrationResult.Failure -> {
                        _upgradeState.value = DatabaseUpgradeState.Failed(
                            error = result.error,
                            message = result.message
                        )
                    }
                }
            }
        } else {
            _upgradeState.value = DatabaseUpgradeState.Completed
        }
    }

    fun retryMigration() {
        _upgradeState.value = DatabaseUpgradeState.Upgrading
        viewModelScope.launch(Dispatchers.IO) {
            val result = migrator.performMigration()
            when (result) {
                is MigrationResult.Success, is MigrationResult.AlreadyMigrated, is MigrationResult.NotRequired -> {
                    _upgradeState.value = DatabaseUpgradeState.Completed
                }
                is MigrationResult.Failure -> {
                    _upgradeState.value = DatabaseUpgradeState.Failed(
                        error = result.error,
                        message = result.message
                    )
                }
            }
        }
    }

    fun exportLegacyDatabaseBackup(uri: Uri): Result<Unit> {
        return migrator.exportLegacyDatabaseToJson(uri)
    }
}
