package com.example.data.database

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "classroom_settings")

class PreferencesManager(private val context: Context) {
    companion object {
        val ACTIVE_CLASSROOM_ID = longPreferencesKey("active_classroom_id")
    }

    val activeClassroomIdFlow: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            val id = preferences[ACTIVE_CLASSROOM_ID]
            if (id == -1L) null else id
        }

    suspend fun saveActiveClassroomId(id: Long?) {
        context.dataStore.edit { preferences ->
            if (id != null) {
                preferences[ACTIVE_CLASSROOM_ID] = id
            } else {
                preferences.remove(ACTIVE_CLASSROOM_ID)
            }
        }
    }
}