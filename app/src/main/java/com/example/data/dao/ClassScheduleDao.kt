package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.ClassScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassScheduleDao {
    @Query("SELECT * FROM class_schedules ORDER BY weekNumber ASC, startMinutes ASC")
    fun getAllSchedules(): Flow<List<ClassScheduleEntity>>

    @Query("SELECT * FROM class_schedules ORDER BY weekNumber ASC, startMinutes ASC")
    suspend fun getAllSchedulesOnce(): List<ClassScheduleEntity>

    @Query("SELECT * FROM class_schedules WHERE classroomId = :classroomId ORDER BY weekNumber ASC, startMinutes ASC")
    fun getSchedulesByClassroom(classroomId: Long): Flow<List<ClassScheduleEntity>>

    @Query("SELECT * FROM class_schedules WHERE classroomId = :classroomId ORDER BY weekNumber ASC, startMinutes ASC")
    suspend fun getSchedulesByClassroomOnce(classroomId: Long): List<ClassScheduleEntity>

    @Query("SELECT * FROM class_schedules WHERE weekNumber = :weekNumber ORDER BY startMinutes ASC")
    suspend fun getSchedulesForWeekOnce(weekNumber: Int): List<ClassScheduleEntity>

    @Query("SELECT * FROM class_schedules WHERE classroomId = :classroomId AND weekNumber = :weekNumber AND dayOfWeek = :dayOfWeek ORDER BY startMinutes ASC")
    fun getScheduleByWeekAndDay(classroomId: Long, weekNumber: Int, dayOfWeek: String): Flow<List<ClassScheduleEntity>>

    @Query("SELECT * FROM class_schedules WHERE classroomId = :classroomId AND weekNumber = :weekNumber AND dayOfWeek = :dayOfWeek ORDER BY startMinutes ASC")
    suspend fun getScheduleByWeekAndDayOnce(classroomId: Long, weekNumber: Int, dayOfWeek: String): List<ClassScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleEntry(entry: ClassScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleEntries(entries: List<ClassScheduleEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertScheduleEntriesSync(entries: List<ClassScheduleEntity>)

    @Update
    suspend fun updateScheduleEntry(entry: ClassScheduleEntity)

    @Delete
    suspend fun deleteScheduleEntry(entry: ClassScheduleEntity)

    @Query("DELETE FROM class_schedules WHERE id = :id")
    suspend fun deleteScheduleEntryById(id: Long)

    @Query("DELETE FROM class_schedules WHERE classroomId = :classroomId")
    suspend fun deleteScheduleByClassroom(classroomId: Long)

    @Query("DELETE FROM class_schedules WHERE weekNumber = :weekNumber")
    suspend fun deleteSchedulesForWeek(weekNumber: Int)

    @Query("DELETE FROM class_schedules WHERE weekNumber IN (:weekNumbers)")
    suspend fun deleteSchedulesForWeeks(weekNumbers: List<Int>)

    @Query("DELETE FROM class_schedules WHERE classroomName = :classroomName AND weekNumber IN (:weekNumbers)")
    suspend fun deleteSchedulesForSubjectInWeeks(classroomName: String, weekNumbers: List<Int>)

    @Transaction
    suspend fun replicateWeekSchedule(fromWeek: Int, targetWeeks: List<Int>) {
        val weekEntries = getSchedulesForWeekOnce(fromWeek)
        deleteSchedulesForWeeks(targetWeeks)
        val newEntries = mutableListOf<ClassScheduleEntity>()
        for (targetWeek in targetWeeks) {
            for (entry in weekEntries) {
                newEntries.add(entry.copy(id = 0, weekNumber = targetWeek))
            }
        }
        insertScheduleEntries(newEntries)
    }

    @Transaction
    suspend fun saveBatchScheduleEntries(entries: List<ClassScheduleEntity>) {
        insertScheduleEntries(entries)
    }

    @Transaction
    suspend fun updateSubjectScheduleAcrossWeeks(classroomName: String, targetWeeks: List<Int>, newEntriesTemplate: List<ClassScheduleEntity>) {
        deleteSchedulesForSubjectInWeeks(classroomName, targetWeeks)
        val newEntries = mutableListOf<ClassScheduleEntity>()
        for (targetWeek in targetWeeks) {
            for (tmpl in newEntriesTemplate) {
                newEntries.add(tmpl.copy(id = 0, weekNumber = targetWeek))
            }
        }
        insertScheduleEntries(newEntries)
    }
}
