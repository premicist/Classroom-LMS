package com.aistudio.classroomlms.di

import android.content.Context
import com.aistudio.classroomlms.data.dao.AssignmentDao
import com.aistudio.classroomlms.data.dao.AttendanceDao
import com.aistudio.classroomlms.data.dao.ClassScheduleDao
import com.aistudio.classroomlms.data.dao.ExamDao
import com.aistudio.classroomlms.data.dao.HomeworkRecordDao
import com.aistudio.classroomlms.data.dao.PlannerDao
import com.aistudio.classroomlms.data.dao.StudentDao
import com.aistudio.classroomlms.data.dao.SubmissionDao
import com.aistudio.classroomlms.data.database.AppDatabase
import com.aistudio.classroomlms.data.database.PreferencesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.aistudio.classroomlms.data.auth.AuthManager
import com.aistudio.classroomlms.data.dao.ClassroomDao
import com.aistudio.classroomlms.data.dao.DisciplineDao
import com.aistudio.classroomlms.data.dao.InterventionDao
import com.aistudio.classroomlms.data.dao.LiveAssessmentDao
import com.aistudio.classroomlms.data.repository.ClassroomRepository
import com.aistudio.classroomlms.data.repository.DatabaseBackupManager
import com.aistudio.classroomlms.data.repository.ScheduleRepository
import com.aistudio.classroomlms.data.repository.UpdateRepository

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLegacyDatabaseMigrator(@ApplicationContext context: Context): com.aistudio.classroomlms.data.database.LegacyDatabaseMigrator {
        return com.aistudio.classroomlms.data.database.LegacyDatabaseMigrator.getInstance(context)
    }

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
    fun provideClassroomRepository(database: AppDatabase): ClassroomRepository {
        return ClassroomRepository(database)
    }
}
