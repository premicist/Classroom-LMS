package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.InterventionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InterventionDao {
    @Query("SELECT * FROM interventions WHERE classroomId = :classroomId ORDER BY date DESC, createdAt DESC")
    fun getInterventionsByClassroom(classroomId: Long): Flow<List<InterventionEntity>>

    @Query("SELECT * FROM interventions WHERE studentId = :studentId ORDER BY date DESC, createdAt DESC")
    fun getInterventionsByStudent(studentId: Long): Flow<List<InterventionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntervention(intervention: InterventionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterventions(interventions: List<InterventionEntity>): List<Long>

    @Update
    suspend fun updateIntervention(intervention: InterventionEntity)

    @Delete
    suspend fun deleteIntervention(intervention: InterventionEntity)

    @Query("DELETE FROM interventions WHERE id = :id")
    suspend fun deleteInterventionById(id: Long)
}
