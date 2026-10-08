package com.example.di

import android.content.Context
import com.example.data.auth.AuthManager
import com.example.data.dao.ExamDao
import com.example.data.database.AppDatabase
import com.example.data.database.PreferencesManager
import com.example.data.repository.ClassroomRepository
import com.example.data.repository.DatabaseBackupManager
import com.example.data.repository.ScheduleRepository
import com.example.data.repository.UpdateRepository
import dagger.BindsInstance
import dagger.Component
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance @ApplicationContext context: Context): AppComponent
    }

    fun appDatabase(): AppDatabase
    fun examDao(): ExamDao
    fun classroomRepository(): ClassroomRepository
    fun scheduleRepository(): ScheduleRepository
    fun preferencesManager(): PreferencesManager
    fun authManager(): AuthManager
    fun databaseBackupManager(): DatabaseBackupManager
    fun updateRepository(): UpdateRepository
}
