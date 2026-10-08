package com.example.fake

import android.content.Context
import com.example.data.database.LmsSettings
import com.example.data.database.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePreferencesManager(context: Context) : PreferencesManager(context) {

    val activeClassroomIdState = MutableStateFlow<Long?>(null)
    val settingsState = MutableStateFlow(LmsSettings())

    override val activeClassroomIdFlow: Flow<Long?> = activeClassroomIdState
    override val settingsFlow: Flow<LmsSettings> = settingsState

    override suspend fun saveActiveClassroomId(id: Long?) {
        activeClassroomIdState.value = id
    }

    override suspend fun saveSettings(settings: LmsSettings) {
        settingsState.value = settings
    }
}
