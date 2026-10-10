package com.example.fake

import com.example.data.dao.PlannerDao
import com.example.data.entity.DailyLogEntity
import com.example.data.entity.LessonPlanEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePlannerDao : PlannerDao {
    private val lessonPlans = MutableStateFlow<List<LessonPlanEntity>>(emptyList())
    private val dailyLogs = MutableStateFlow<List<DailyLogEntity>>(emptyList())
    private var nextPlanId = 1L
    private var nextLogId = 1L

    override fun getLessonPlansForClassroom(classroomId: Long): Flow<List<LessonPlanEntity>> {
        return lessonPlans.map { list -> list.filter { it.classroomId == classroomId } }
    }

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
    override fun insertLessonPlansSync(plans: List<LessonPlanEntity>) {
        lessonPlans.value = lessonPlans.value + plans
    }
    override fun getAllDailyLogsOnce(): List<DailyLogEntity> = dailyLogs.value
    override fun insertDailyLogsSync(logs: List<DailyLogEntity>) {
        dailyLogs.value = dailyLogs.value + logs
    }
}
