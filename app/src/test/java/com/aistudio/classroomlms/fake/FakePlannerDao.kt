package com.aistudio.classroomlms.fake

import com.aistudio.classroomlms.data.dao.PlannerDao
import com.aistudio.classroomlms.data.entity.DailyLogEntity
import com.aistudio.classroomlms.data.entity.LessonPlanEntity
import com.aistudio.classroomlms.data.entity.LessonPlanSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePlannerDao : PlannerDao {
    private val lessonPlans = MutableStateFlow<List<LessonPlanEntity>>(emptyList())
    private val lessonPlanSessions = MutableStateFlow<List<LessonPlanSessionEntity>>(emptyList())
    private val dailyLogs = MutableStateFlow<List<DailyLogEntity>>(emptyList())
    private var nextPlanId = 1L
    private var nextSessionId = 1L
    private var nextLogId = 1L

    override fun getLessonPlansForClassroom(classroomId: Long): Flow<List<LessonPlanEntity>> {
        return lessonPlans.map { list -> list.filter { it.classroomId == classroomId } }
    }

    override fun getLessonPlansWithReminders(): List<LessonPlanEntity> =
        lessonPlans.value.filter { it.reminderEnabled }

    override suspend fun insertLessonPlan(plan: LessonPlanEntity): Long {
        val newPlan = plan.copy(id = if (plan.id == 0L) nextPlanId++ else plan.id)
        lessonPlans.value = lessonPlans.value + newPlan
        return newPlan.id
    }

    override suspend fun updateLessonPlan(plan: LessonPlanEntity) {
        lessonPlans.value = lessonPlans.value.map { if (it.id == plan.id) plan else it }
    }

    override suspend fun deleteLessonPlan(plan: LessonPlanEntity) {
        lessonPlans.value = lessonPlans.value.filter { it.id != plan.id }
        lessonPlanSessions.value = lessonPlanSessions.value.filter { it.lessonPlanId != plan.id }
    }

    override fun getAllLessonPlanSessionsOnce(): List<LessonPlanSessionEntity> = lessonPlanSessions.value

    override fun getSessionsForClassroom(classroomId: Long): Flow<List<LessonPlanSessionEntity>> {
        return lessonPlans.map { plans ->
            val planIds = plans.filter { it.classroomId == classroomId }.map { it.id }.toSet()
            lessonPlanSessions.value.filter { it.lessonPlanId in planIds }.sortedBy { it.sessionOrder }
        }
    }

    override suspend fun getSessionsForPlanOnce(lessonPlanId: Long): List<LessonPlanSessionEntity> =
        lessonPlanSessions.value.filter { it.lessonPlanId == lessonPlanId }.sortedBy { it.sessionOrder }

    override suspend fun insertLessonPlanSessions(sessions: List<LessonPlanSessionEntity>) {
        val inserted = sessions.map { s -> s.copy(id = if (s.id == 0L) nextSessionId++ else s.id) }
        lessonPlanSessions.value = lessonPlanSessions.value + inserted
    }

    override fun insertLessonPlanSessionsSync(sessions: List<LessonPlanSessionEntity>) {
        val inserted = sessions.map { s -> s.copy(id = if (s.id == 0L) nextSessionId++ else s.id) }
        lessonPlanSessions.value = lessonPlanSessions.value + inserted
    }

    override suspend fun deleteSessionsForPlan(lessonPlanId: Long) {
        lessonPlanSessions.value = lessonPlanSessions.value.filter { it.lessonPlanId != lessonPlanId }
    }

    override suspend fun replaceSessionsForPlan(lessonPlanId: Long, sessions: List<LessonPlanSessionEntity>) {
        deleteSessionsForPlan(lessonPlanId)
        if (sessions.isNotEmpty()) {
            val normalized = sessions.mapIndexed { idx, s ->
                s.copy(id = 0L, lessonPlanId = lessonPlanId, sessionOrder = idx + 1)
            }
            insertLessonPlanSessions(normalized)
        }
    }

    override fun getDailyLogsForClassroom(classroomId: Long): Flow<List<DailyLogEntity>> {
        return dailyLogs.map { list -> list.filter { it.classroomId == classroomId } }
    }

    override suspend fun insertDailyLog(log: DailyLogEntity): Long {
        val newLog = log.copy(id = if (log.id == 0L) nextLogId++ else log.id)
        dailyLogs.value = dailyLogs.value + newLog
        return newLog.id
    }

    override suspend fun updateDailyLog(log: DailyLogEntity) {
        dailyLogs.value = dailyLogs.value.map { if (it.id == log.id) log else it }
    }

    override suspend fun deleteDailyLog(log: DailyLogEntity) {
        dailyLogs.value = dailyLogs.value.filter { it.id != log.id }
    }
    
    override fun getAllLessonPlansOnce(): List<LessonPlanEntity> = lessonPlans.value
    override fun getAllLessonPlans(): Flow<List<LessonPlanEntity>> = lessonPlans
    override fun insertLessonPlansSync(plans: List<LessonPlanEntity>) {
        lessonPlans.value = lessonPlans.value + plans
    }
    override fun getAllDailyLogsOnce(): List<DailyLogEntity> = dailyLogs.value
    override fun insertDailyLogsSync(logs: List<DailyLogEntity>) {
        dailyLogs.value = dailyLogs.value + logs
    }
}
