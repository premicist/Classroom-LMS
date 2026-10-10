package com.example.fake

import com.example.data.dao.ClassScheduleDao
import com.example.data.entity.ClassScheduleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeClassScheduleDao : ClassScheduleDao {
    private val schedules = MutableStateFlow<List<ClassScheduleEntity>>(emptyList())
    private var nextId = 1L

    override fun getAllSchedules(): Flow<List<ClassScheduleEntity>> = schedules

    override fun getSchedulesByClassroom(classroomId: Long): Flow<List<ClassScheduleEntity>> {
        return schedules.map { list -> list.filter { it.classroomId == classroomId } }
    }
    
    override suspend fun getSchedulesByClassroomOnce(classroomId: Long): List<ClassScheduleEntity> {
        return schedules.value.filter { it.classroomId == classroomId }
    }

    override suspend fun getSchedulesForWeekOnce(weekNumber: Int): List<ClassScheduleEntity> {
        return schedules.value.filter { it.weekNumber == weekNumber }
    }

    override fun getScheduleByWeekAndDay(classroomId: Long, weekNumber: Int, dayOfWeek: String): Flow<List<ClassScheduleEntity>> {
        return schedules.map { list -> list.filter { it.classroomId == classroomId && it.weekNumber == weekNumber && it.dayOfWeek == dayOfWeek } }
    }

    override suspend fun getScheduleByWeekAndDayOnce(classroomId: Long, weekNumber: Int, dayOfWeek: String): List<ClassScheduleEntity> {
        return schedules.value.filter { it.classroomId == classroomId && it.weekNumber == weekNumber && it.dayOfWeek == dayOfWeek }
    }

    override suspend fun insertScheduleEntry(entry: ClassScheduleEntity): Long {
        val newSchedule = entry.copy(id = if (entry.id == 0L) nextId++ else entry.id)
        schedules.value = schedules.value + newSchedule
        return newSchedule.id
    }

    override suspend fun insertScheduleEntries(entries: List<ClassScheduleEntity>): List<Long> {
        val ids = mutableListOf<Long>()
        val newEntries = entries.map {
            val id = if (it.id == 0L) nextId++ else it.id
            ids.add(id)
            it.copy(id = id)
        }
        schedules.value = schedules.value + newEntries
        return ids
    }

    override suspend fun updateScheduleEntry(entry: ClassScheduleEntity) {
        schedules.value = schedules.value.map { if (it.id == entry.id) entry else it }
    }

    override suspend fun deleteScheduleEntry(entry: ClassScheduleEntity) {
        schedules.value = schedules.value.filter { it.id != entry.id }
    }
    
    override suspend fun deleteScheduleEntryById(id: Long) {
        schedules.value = schedules.value.filter { it.id != id }
    }
    
    override suspend fun deleteScheduleByClassroom(classroomId: Long) {
        schedules.value = schedules.value.filter { it.classroomId != classroomId }
    }
    
    override suspend fun deleteSchedulesForWeek(weekNumber: Int) {
        schedules.value = schedules.value.filter { it.weekNumber != weekNumber }
    }
    
    override suspend fun deleteSchedulesForWeeks(weekNumbers: List<Int>) {
        schedules.value = schedules.value.filter { !weekNumbers.contains(it.weekNumber) }
    }
    
    override suspend fun deleteSchedulesForSubjectInWeeks(classroomName: String, weekNumbers: List<Int>) {
        schedules.value = schedules.value.filter { !(it.classroomName == classroomName && weekNumbers.contains(it.weekNumber)) }
    }
    
    override suspend fun getAllSchedulesOnce(): List<ClassScheduleEntity> = schedules.value
    
    override fun insertScheduleEntriesSync(entries: List<ClassScheduleEntity>) {
        val newEntries = entries.map {
            val id = if (it.id == 0L) nextId++ else it.id
            it.copy(id = id)
        }
        schedules.value = schedules.value + newEntries
    }
}
