package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.ClassScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassScheduleDao {
    @Query("SELECT * FROM class_schedules WHERE classroomId = :classroomId ORDER BY weekNumber ASC, startMinutes ASC")
    fun getSchedulesByClassroom(classroomId: Long): Flow<List<ClassScheduleEntity>>

    @Query("SELECT * FROM class_schedules WHERE classroomId = :classroomId ORDER BY weekNumber ASC, startMinutes ASC")
    suspend fun getSchedulesByClassroomOnce(classroomId: Long): List<ClassScheduleEntity>

    @Query("SELECT * FROM class_schedules WHERE classroomId = :classroomId AND weekNumber = :weekNumber AND dayOfWeek = :dayOfWeek ORDER BY startMinutes ASC")
    fun getScheduleByWeekAndDay(classroomId: Long, weekNumber: Int, dayOfWeek: String): Flow<List<ClassScheduleEntity>>

    @Query("SELECT * FROM class_schedules WHERE classroomId = :classroomId AND weekNumber = :weekNumber AND dayOfWeek = :dayOfWeek ORDER BY startMinutes ASC")
    suspend fun getScheduleByWeekAndDayOnce(classroomId: Long, weekNumber: Int, dayOfWeek: String): List<ClassScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleEntry(entry: ClassScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleEntries(entries: List<ClassScheduleEntity>): List<Long>

    @Update
    suspend fun updateScheduleEntry(entry: ClassScheduleEntity)

    @Delete
    suspend fun deleteScheduleEntry(entry: ClassScheduleEntity)

    @Query("DELETE FROM class_schedules WHERE id = :id")
    suspend fun deleteScheduleEntryById(id: Long)

    @Query("DELETE FROM class_schedules WHERE classroomId = :classroomId")
    suspend fun deleteScheduleByClassroom(classroomId: Long)
}
