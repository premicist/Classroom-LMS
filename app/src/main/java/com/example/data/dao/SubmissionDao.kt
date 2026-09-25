package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.SubmissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubmissionDao {
    @Query("SELECT * FROM submissions WHERE assignmentId = :assignmentId")
    fun getSubmissionsByAssignment(assignmentId: Long): Flow<List<SubmissionEntity>>

    @Query("SELECT * FROM submissions WHERE assignmentId = :assignmentId")
    suspend fun getSubmissionsByAssignmentOnce(assignmentId: Long): List<SubmissionEntity>

    @Query("SELECT * FROM submissions WHERE classroomId = :classroomId")
    fun getSubmissionsByClassroom(classroomId: Long): Flow<List<SubmissionEntity>>

    @Query("SELECT * FROM submissions WHERE classroomId = :classroomId")
    suspend fun getSubmissionsByClassroomOnce(classroomId: Long): List<SubmissionEntity>

    @Query("SELECT * FROM submissions WHERE studentId = :studentId")
    fun getSubmissionsByStudent(studentId: Long): Flow<List<SubmissionEntity>>

    @Query("SELECT * FROM submissions WHERE studentId = :studentId")
    suspend fun getSubmissionsByStudentOnce(studentId: Long): List<SubmissionEntity>

    @Query("SELECT * FROM submissions WHERE assignmentId = :assignmentId AND studentId = :studentId")
    fun getSubmission(assignmentId: Long, studentId: Long): Flow<SubmissionEntity?>

    @Query("SELECT * FROM submissions WHERE assignmentId = :assignmentId AND studentId = :studentId")
    suspend fun getSubmissionOnce(assignmentId: Long, studentId: Long): SubmissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(submission: SubmissionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmissions(submissions: List<SubmissionEntity>)

    @Update
    suspend fun updateSubmission(submission: SubmissionEntity)

    @Delete
    suspend fun deleteSubmission(submission: SubmissionEntity)

    @Query("DELETE FROM submissions WHERE assignmentId = :assignmentId")
    suspend fun deleteSubmissionsByAssignmentId(assignmentId: Long)
}
