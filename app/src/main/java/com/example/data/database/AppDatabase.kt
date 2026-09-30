package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.dao.AssignmentDao
import com.example.data.dao.AttendanceDao
import com.example.data.dao.ClassScheduleDao
import com.example.data.dao.ClassroomDao
import com.example.data.dao.DisciplineDao
import com.example.data.dao.HomeworkRecordDao
import com.example.data.dao.InterventionDao
import com.example.data.dao.LiveAssessmentDao
import com.example.data.dao.PlannerDao
import com.example.data.dao.StudentDao
import com.example.data.dao.SubmissionDao
import com.example.data.entity.AssignmentEntity
import com.example.data.entity.AttendanceRecordEntity
import com.example.data.entity.ClassScheduleEntity
import com.example.data.entity.ClassroomEntity
import com.example.data.entity.DailyLogEntity
import com.example.data.entity.DisciplineRecordEntity
import com.example.data.entity.HomeworkRecordEntity
import com.example.data.entity.InterventionEntity
import com.example.data.entity.LessonPlanEntity
import com.example.data.entity.LiveAssessmentEntity
import com.example.data.entity.StudentEntity
import com.example.data.entity.SubmissionEntity

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
        LiveAssessmentEntity::class
    ],
    version = 8,
    exportSchema = true,
)
@TypeConverters(Converters::class, com.example.data.entity.Converters::class)
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "classroom_lms_db"
                )
                    .addMigrations(
                        com.example.data.database.Migrations.MIGRATION_6_7,
                        com.example.data.database.Migrations.MIGRATION_7_8
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
