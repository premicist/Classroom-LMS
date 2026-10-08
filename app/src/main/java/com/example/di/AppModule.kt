package com.example.di

import android.content.Context
import com.example.data.dao.AssignmentDao
import com.example.data.dao.AttendanceDao
import com.example.data.dao.ClassScheduleDao
import com.example.data.dao.ExamDao
import com.example.data.dao.HomeworkRecordDao
import com.example.data.dao.PlannerDao
import com.example.data.dao.StudentDao
import com.example.data.dao.SubmissionDao
import com.example.data.database.AppDatabase
import com.example.data.database.PreferencesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.example.data.auth.AuthManager
import com.example.data.dao.ClassroomDao
import com.example.data.dao.DisciplineDao
import com.example.data.dao.InterventionDao
import com.example.data.dao.LiveAssessmentDao
import com.example.data.repository.ClassroomRepository
import com.example.data.repository.DatabaseBackupManager
import com.example.data.repository.ScheduleRepository
import com.example.data.repository.UpdateRepository

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideClassroomDao(database: AppDatabase): ClassroomDao {
        return database.classroomDao()
    }

    @Provides
    fun provideExamDao(database: AppDatabase): ExamDao {
        return database.examDao()
    }

    @Provides
    fun provideStudentDao(database: AppDatabase): StudentDao {
        return database.studentDao()
    }

    @Provides
    fun provideAttendanceDao(database: AppDatabase): AttendanceDao {
        return database.attendanceDao()
    }

    @Provides
    fun provideHomeworkRecordDao(database: AppDatabase): HomeworkRecordDao {
        return database.homeworkRecordDao()
    }

    @Provides
    fun provideAssignmentDao(database: AppDatabase): AssignmentDao {
        return database.assignmentDao()
    }

    @Provides
    fun provideSubmissionDao(database: AppDatabase): SubmissionDao {
        return database.submissionDao()
    }

    @Provides
    fun providePlannerDao(database: AppDatabase): PlannerDao {
        return database.plannerDao()
    }

    @Provides
    fun provideClassScheduleDao(database: AppDatabase): ClassScheduleDao {
        return database.classScheduleDao()
    }

    @Provides
    fun provideDisciplineDao(database: AppDatabase): DisciplineDao {
        return database.disciplineDao()
    }

    @Provides
    fun provideInterventionDao(database: AppDatabase): InterventionDao {
        return database.interventionDao()
    }

    @Provides
    fun provideLiveAssessmentDao(database: AppDatabase): LiveAssessmentDao {
        return database.liveAssessmentDao()
    }

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideAuthManager(@ApplicationContext context: Context): AuthManager {
        return AuthManager(context)
    }



    @Provides
    @Singleton
    fun provideScheduleRepository(classScheduleDao: ClassScheduleDao): ScheduleRepository {
        return ScheduleRepository(classScheduleDao)
    }

    @Provides
    @Singleton
    fun provideClassroomRepository(database: AppDatabase): ClassroomRepository {
        return ClassroomRepository(database)
    }

    @Provides
    @Singleton
    fun provideDatabaseBackupManager(@ApplicationContext context: Context, database: AppDatabase): DatabaseBackupManager {
        return DatabaseBackupManager(context, database)
    }

    @Provides
    @Singleton
    fun provideUpdateRepository(): UpdateRepository {
        return UpdateRepository()
    }
}
