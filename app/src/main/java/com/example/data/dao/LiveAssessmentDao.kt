package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.LiveAssessmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LiveAssessmentDao {
    @Query("SELECT * FROM live_assessments WHERE classroomId = :classroomId ORDER BY date DESC, timestamp DESC")
    fun getAssessmentsByClassroom(classroomId: Long): Flow<List<LiveAssessmentEntity>>

    @Query("SELECT * FROM live_assessments WHERE classroomId = :classroomId ORDER BY date DESC, timestamp DESC")
    suspend fun getAssessmentsByClassroomOnce(classroomId: Long): List<LiveAssessmentEntity>

    @Query("SELECT * FROM live_assessments WHERE studentId = :studentId ORDER BY date DESC, timestamp DESC")
    fun getAssessmentsByStudent(studentId: Long): Flow<List<LiveAssessmentEntity>>

    @Query("SELECT * FROM live_assessments WHERE studentId = :studentId ORDER BY date DESC, timestamp DESC")
    suspend fun getAssessmentsByStudentOnce(studentId: Long): List<LiveAssessmentEntity>

    @Query("SELECT * FROM live_assessments WHERE classroomId = :classroomId AND date = :date ORDER BY timestamp DESC")
    fun getAssessmentsByDate(classroomId: Long, date: String): Flow<List<LiveAssessmentEntity>>

    @Query("SELECT * FROM live_assessments ORDER BY date DESC, timestamp DESC")
    fun getAllAssessmentsOnce(): List<LiveAssessmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessment: LiveAssessmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessments(assessments: List<LiveAssessmentEntity>): List<Long>

    @Update
    suspend fun updateAssessment(assessment: LiveAssessmentEntity)

    @Delete
    suspend fun deleteAssessment(assessment: LiveAssessmentEntity)

    @Query("DELETE FROM live_assessments WHERE id = :id")
    suspend fun deleteAssessmentById(id: Long)
}
