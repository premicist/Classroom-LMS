package com.aistudio.classroomlms.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aistudio.classroomlms.data.entity.AcademicEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicEventDao {
    @Query("SELECT * FROM academic_events ORDER BY startDate ASC")
    fun getAllEvents(): Flow<List<AcademicEventEntity>>

    @Query("SELECT * FROM academic_events ORDER BY startDate ASC")
    fun getAllEventsOnce(): List<AcademicEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AcademicEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEventsSync(events: List<AcademicEventEntity>)

    @Update
    suspend fun updateEvent(event: AcademicEventEntity)

    @Delete
    suspend fun deleteEvent(event: AcademicEventEntity)
}
