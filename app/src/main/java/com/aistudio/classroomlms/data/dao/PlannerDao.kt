package com.aistudio.classroomlms.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.aistudio.classroomlms.data.entity.DailyLogEntity
import com.aistudio.classroomlms.data.entity.LessonPlanEntity
import com.aistudio.classroomlms.data.entity.LessonPlanSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {
    // Lesson Plans
    @Query("SELECT * FROM lesson_plans")
    fun getAllLessonPlansOnce(): List<LessonPlanEntity>

    @Query("SELECT * FROM lesson_plans ORDER BY targetDate ASC")
    fun getAllLessonPlans(): Flow<List<LessonPlanEntity>>

    @Query("SELECT * FROM lesson_plans WHERE classroomId = :classroomId ORDER BY targetDate ASC")
    fun getLessonPlansForClassroom(classroomId: Long): Flow<List<LessonPlanEntity>>

    @Query("SELECT * FROM lesson_plans WHERE reminderEnabled = 1")
    fun getLessonPlansWithReminders(): List<LessonPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessonPlan(plan: LessonPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLessonPlansSync(plans: List<LessonPlanEntity>)

    @Update
    suspend fun updateLessonPlan(plan: LessonPlanEntity)

    @Delete
    suspend fun deleteLessonPlan(plan: LessonPlanEntity)

    // Lesson Plan Sessions (LO Matrix Rows)
    @Query("SELECT * FROM lesson_plan_sessions ORDER BY sessionOrder ASC")
    fun getAllLessonPlanSessionsOnce(): List<LessonPlanSessionEntity>

    @Query(
        """
        SELECT s.* FROM lesson_plan_sessions s
        INNER JOIN lesson_plans p ON p.id = s.lessonPlanId
        WHERE p.classroomId = :classroomId
        ORDER BY s.lessonPlanId ASC, s.sessionOrder ASC
        """
    )
    fun getSessionsForClassroom(classroomId: Long): Flow<List<LessonPlanSessionEntity>>

    @Query("SELECT * FROM lesson_plan_sessions WHERE lessonPlanId = :lessonPlanId ORDER BY sessionOrder ASC")
    suspend fun getSessionsForPlanOnce(lessonPlanId: Long): List<LessonPlanSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessonPlanSessions(sessions: List<LessonPlanSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLessonPlanSessionsSync(sessions: List<LessonPlanSessionEntity>)

    @Query("DELETE FROM lesson_plan_sessions WHERE lessonPlanId = :lessonPlanId")
    suspend fun deleteSessionsForPlan(lessonPlanId: Long)

    @Transaction
    suspend fun replaceSessionsForPlan(lessonPlanId: Long, sessions: List<LessonPlanSessionEntity>) {
        deleteSessionsForPlan(lessonPlanId)
        if (sessions.isNotEmpty()) {
            val normalized = sessions.mapIndexed { idx, s ->
                s.copy(id = 0L, lessonPlanId = lessonPlanId, sessionOrder = idx + 1)
            }
            insertLessonPlanSessions(normalized)
        }
    }

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