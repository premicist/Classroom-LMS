package com.example

import com.example.data.dao.ClassScheduleDao
import com.example.data.entity.ClassScheduleEntity
import com.example.data.repository.ScheduleRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleConflictTest {

    // Dummy DAO for testing pure repository logic
    private val dummyDao = object : ClassScheduleDao {
        override fun getAllSchedules() = error("Not implemented")
        override suspend fun getAllSchedulesOnce() = emptyList<ClassScheduleEntity>()
        override fun getSchedulesByClassroom(classroomId: Long) = error("Not implemented")
        override suspend fun getSchedulesByClassroomOnce(classroomId: Long) = emptyList<ClassScheduleEntity>()
        override suspend fun getSchedulesForWeekOnce(weekNumber: Int) = emptyList<ClassScheduleEntity>()
        override fun getScheduleByWeekAndDay(classroomId: Long, weekNumber: Int, dayOfWeek: String) = error("Not implemented")
        override suspend fun getScheduleByWeekAndDayOnce(classroomId: Long, weekNumber: Int, dayOfWeek: String) = emptyList<ClassScheduleEntity>()
        override suspend fun insertScheduleEntry(entry: ClassScheduleEntity) = 1L
        override suspend fun insertScheduleEntries(entries: List<ClassScheduleEntity>) = listOf(1L)
        override fun insertScheduleEntriesSync(entries: List<ClassScheduleEntity>) {}
        override suspend fun updateScheduleEntry(entry: ClassScheduleEntity) {}
        override suspend fun deleteScheduleEntry(entry: ClassScheduleEntity) {}
        override suspend fun deleteScheduleEntryById(id: Long) {}
        override suspend fun deleteScheduleByClassroom(classroomId: Long) {}
        override suspend fun deleteSchedulesForWeek(weekNumber: Int) {}
        override suspend fun deleteSchedulesForWeeks(weekNumbers: List<Int>) {}
        override suspend fun deleteSchedulesForSubjectInWeeks(classroomName: String, weekNumbers: List<Int>) {}
    }

    private val repository = ScheduleRepository(dummyDao)

    @Test
    fun testTimeOverlapDetection() {
        val existingClass = ClassScheduleEntity(
            id = 100L,
            classroomId = 1L,
            weekNumber = 1,
            dayOfWeek = "MONDAY",
            classroomName = "AP Calculus",
            startTime = "09:00 AM",
            endTime = "10:00 AM",
            startMinutes = 540, // 09:00
            endMinutes = 600    // 10:00
        )
        val existingList = listOf(existingClass)

        // 1. Overlapping times (09:30 AM - 10:30 AM)
        val hasOverlap1 = repository.hasTimeOverlap(
            existingEntries = existingList,
            newStartMinutes = 570, // 09:30
            newEndMinutes = 630    // 10:30
        )
        assertTrue(hasOverlap1)

        // 2. Back-to-back classes (10:00 AM - 11:00 AM) -> NO OVERLAP
        val hasOverlap2 = repository.hasTimeOverlap(
            existingEntries = existingList,
            newStartMinutes = 600, // 10:00
            newEndMinutes = 660    // 11:00
        )
        assertFalse(hasOverlap2)

        // 3. Back-to-back earlier class (08:00 AM - 09:00 AM) -> NO OVERLAP
        val hasOverlap3 = repository.hasTimeOverlap(
            existingEntries = existingList,
            newStartMinutes = 480, // 08:00
            newEndMinutes = 540    // 09:00
        )
        assertFalse(hasOverlap3)

        // 4. Editing existing entry (pass ignoreEntryId = 100L) -> NO OVERLAP WITH ITSELF
        val hasOverlap4 = repository.hasTimeOverlap(
            existingEntries = existingList,
            newStartMinutes = 540, // 09:00
            newEndMinutes = 600,   // 10:00
            ignoreEntryId = 100L
        )
        assertFalse(hasOverlap4)
    }
}
