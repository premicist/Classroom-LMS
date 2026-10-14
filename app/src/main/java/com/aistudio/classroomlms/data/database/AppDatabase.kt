package com.aistudio.classroomlms.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aistudio.classroomlms.BuildConfig
import com.aistudio.classroomlms.data.dao.AssignmentDao
import com.aistudio.classroomlms.data.dao.AttendanceDao
import com.aistudio.classroomlms.data.dao.ClassScheduleDao
import com.aistudio.classroomlms.data.dao.ClassroomDao
import com.aistudio.classroomlms.data.dao.DisciplineDao
import com.aistudio.classroomlms.data.dao.HomeworkRecordDao
import com.aistudio.classroomlms.data.dao.InterventionDao
import com.aistudio.classroomlms.data.dao.LiveAssessmentDao
import com.aistudio.classroomlms.data.dao.PlannerDao
import com.aistudio.classroomlms.data.dao.StudentDao
import com.aistudio.classroomlms.data.dao.SubmissionDao
import com.aistudio.classroomlms.data.entity.AssignmentEntity
import com.aistudio.classroomlms.data.entity.AttendanceRecordEntity
import com.aistudio.classroomlms.data.entity.ClassScheduleEntity
import com.aistudio.classroomlms.data.entity.ClassroomEntity
import com.aistudio.classroomlms.data.entity.DailyLogEntity
import com.aistudio.classroomlms.data.entity.DisciplineRecordEntity
import com.aistudio.classroomlms.data.entity.ExamEntity
import com.aistudio.classroomlms.data.entity.ExamMarkEntity
import com.aistudio.classroomlms.data.entity.HomeworkRecordEntity
import com.aistudio.classroomlms.data.entity.InterventionEntity
import com.aistudio.classroomlms.data.entity.LessonPlanEntity
import com.aistudio.classroomlms.data.entity.LiveAssessmentEntity
import com.aistudio.classroomlms.data.entity.StudentEntity
import com.aistudio.classroomlms.data.entity.SubmissionEntity
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        ClassroomEntity::class,
        StudentEntity::class,
        AssignmentEntity::class,
        SubmissionEntity::class,
        HomeworkRecordEntity::class,
        AttendanceRecordEntity::class,
        InterventionEntity::class,
        LessonPlanEntity::class,
        DailyLogEntity::class,
        ClassScheduleEntity::class,
        DisciplineRecordEntity::class,
        LiveAssessmentEntity::class,
        ExamEntity::class,
        ExamMarkEntity::class
    ],
    version = 10,
    exportSchema = true,
)
@TypeConverters(Converters::class, com.aistudio.classroomlms.data.entity.Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun classroomDao(): ClassroomDao
    abstract fun studentDao(): StudentDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun submissionDao(): SubmissionDao
    abstract fun homeworkRecordDao(): HomeworkRecordDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun interventionDao(): InterventionDao
    abstract fun plannerDao(): PlannerDao
    abstract fun classScheduleDao(): ClassScheduleDao
    abstract fun disciplineDao(): DisciplineDao
    abstract fun liveAssessmentDao(): LiveAssessmentDao
    abstract fun examDao(): com.aistudio.classroomlms.data.dao.ExamDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = INSTANCE ?: run {
                    // Check and perform one-time migration from unencrypted legacy database if present
                    val migrator = LegacyDatabaseMigrator.getInstance(context)
                    if (migrator.isMigrationRequired()) {
                        migrator.performMigration()
                    }

                    val passphrase = DatabaseSecurityHelper.getOrGeneratePassphrase(context)
                    val factory = SupportFactory(passphrase)
                    
                    val builder = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "classroom_lms_db_encrypted"
                    ).openHelperFactory(factory)
                    .addMigrations(
                        com.aistudio.classroomlms.data.database.Migrations.MIGRATION_6_7,
                        Migrations.MIGRATION_7_8,
                        Migrations.MIGRATION_8_9,
                        Migrations.MIGRATION_9_10
                    )

                    if (BuildConfig.DEBUG) {
                        // Dev-only: never ship destructive wipe to teachers
                        builder.fallbackToDestructiveMigration(dropAllTables = true)
                    }

                    builder.build()
                }
                INSTANCE = instance
                instance
            }
        }
    }
}
