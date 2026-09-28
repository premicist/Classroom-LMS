package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.DailyLogEntity
import com.example.data.entity.LessonPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {
    // Lesson Plans
    @Query("SELECT * FROM lesson_plans")
    fun getAllLessonPlansOnce(): List<LessonPlanEntity>

    @Query("SELECT * FROM lesson_plans WHERE classroomId = :classroomId ORDER BY targetDate ASC")
    fun getLessonPlansForClassroom(classroomId: Long): Flow<List<LessonPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessonPlan(plan: LessonPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLessonPlansSync(plans: List<LessonPlanEntity>)

    @Update
    suspend fun updateLessonPlan(plan: LessonPlanEntity)

    @Delete
    suspend fun deleteLessonPlan(plan: LessonPlanEntity)

    // Daily Logs
    @Query("SELECT * FROM daily_logs")
    fun getAllDailyLogsOnce(): List<DailyLogEntity>

    @Query("SELECT * FROM daily_logs WHERE classroomId = :classroomId ORDER BY date DESC")
    fun getDailyLogsForClassroom(classroomId: Long): Flow<List<DailyLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyLog(log: DailyLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDailyLogsSync(logs: List<DailyLogEntity>)

    @Update
    suspend fun updateDailyLog(log: DailyLogEntity)

    @Delete
    suspend fun deleteDailyLog(log: DailyLogEntity)
}