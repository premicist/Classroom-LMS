package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.AssignmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentDao {
    @Query("SELECT * FROM assignments WHERE classroomId = :classroomId ORDER BY dueDate ASC, createdAt DESC")
    fun getAssignmentsByClassroom(classroomId: Long): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE classroomId = :classroomId ORDER BY dueDate ASC, createdAt DESC")
    suspend fun getAssignmentsByClassroomOnce(classroomId: Long): List<AssignmentEntity>

    @Query("SELECT * FROM assignments WHERE id = :id")
    fun getAssignmentById(id: Long): Flow<AssignmentEntity?>

    @Query("SELECT * FROM assignments WHERE id = :id")
    suspend fun getAssignmentByIdOnce(id: Long): AssignmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: AssignmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignments(assignments: List<AssignmentEntity>): List<Long>

    @Update
    suspend fun updateAssignment(assignment: AssignmentEntity)

    @Delete
    suspend fun deleteAssignment(assignment: AssignmentEntity)

    @Query("DELETE FROM assignments WHERE id = :id")
    suspend fun deleteAssignmentById(id: Long)
}
