package com.example.data.repository

import com.example.data.dao.ClassScheduleDao
import com.example.data.entity.ClassScheduleEntity
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(private val dao: ClassScheduleDao) {

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

    suspend fun deleteScheduleEntry(entry: ClassScheduleEntity) {
        dao.deleteScheduleEntry(entry)
    }

    suspend fun deleteScheduleEntryById(id: Long) {
        dao.deleteScheduleEntryById(id)
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
