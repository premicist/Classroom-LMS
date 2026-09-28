package com.example.data.repository

import com.example.data.dao.ClassScheduleDao
import com.example.data.entity.ClassScheduleEntity
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(private val dao: ClassScheduleDao) {

    fun getAllSchedules(): Flow<List<ClassScheduleEntity>> {
        return dao.getAllSchedules()
    }

    suspend fun getAllSchedulesOnce(): List<ClassScheduleEntity> {
        return dao.getAllSchedulesOnce()
    }

    fun getSchedulesByClassroom(classroomId: Long): Flow<List<ClassScheduleEntity>> {
        return dao.getSchedulesByClassroom(classroomId)
    }

    suspend fun getSchedulesByClassroomOnce(classroomId: Long): List<ClassScheduleEntity> {
        return dao.getSchedulesByClassroomOnce(classroomId)
    }

    fun getScheduleByWeekAndDay(classroomId: Long, weekNumber: Int, dayOfWeek: String): Flow<List<ClassScheduleEntity>> {
        return dao.getScheduleByWeekAndDay(classroomId, weekNumber, dayOfWeek)
    }

    suspend fun getScheduleByWeekAndDayOnce(classroomId: Long, weekNumber: Int, dayOfWeek: String): List<ClassScheduleEntity> {
        return dao.getScheduleByWeekAndDayOnce(classroomId, weekNumber, dayOfWeek)
    }

    suspend fun insertScheduleEntry(entry: ClassScheduleEntity): Long {
        return dao.insertScheduleEntry(entry)
    }

    suspend fun saveBatchScheduleEntries(entries: List<ClassScheduleEntity>) {
        dao.saveBatchScheduleEntries(entries)
    }

    suspend fun deleteScheduleEntry(entry: ClassScheduleEntity) {
        dao.deleteScheduleEntry(entry)
    }

    suspend fun deleteScheduleEntryById(id: Long) {
        dao.deleteScheduleEntryById(id)
    }

    suspend fun deleteSchedulesForWeek(weekNumber: Int) {
        dao.deleteSchedulesForWeek(weekNumber)
    }

    suspend fun replicateWeekSchedule(fromWeek: Int = 1, targetWeeks: List<Int> = listOf(2, 3, 4, 5)) {
        dao.replicateWeekSchedule(fromWeek, targetWeeks)
    }

    suspend fun updateSubjectScheduleAcrossWeeks(
        classroomName: String,
        targetWeeks: List<Int>,
        templates: List<ClassScheduleEntity>
    ) {
        dao.updateSubjectScheduleAcrossWeeks(classroomName, targetWeeks, templates)
    }

    /**
     * Checks if a proposed start/end time overlaps with any existing schedule entries on the same day.
     * Returns true if there is a conflict.
     */
    fun hasTimeOverlap(
        existingEntries: List<ClassScheduleEntity>,
        newStartMinutes: Int,
        newEndMinutes: Int,
        ignoreEntryId: Long = 0
    ): Boolean {
        return existingEntries.any { existing ->
            if (existing.id == ignoreEntryId) return@any false
            val existStart = existing.startMinutes
            val existEnd = if (existing.endMinutes > 0) existing.endMinutes else existStart + 60
            val newEnd = if (newEndMinutes > 0) newEndMinutes else newStartMinutes + 60

            // Overlap condition: start1 < end2 AND end1 > start2
            newStartMinutes < existEnd && newEnd > existStart
        }
    }
}
