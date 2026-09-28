package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE classroomId = :classroomId ORDER BY name ASC")
    fun getStudentsByClassroom(classroomId: Long): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE classroomId = :classroomId ORDER BY name ASC")
    suspend fun getStudentsByClassroomOnce(classroomId: Long): List<StudentEntity>

    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudentsOnce(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :id")
    fun getStudentById(id: Long): Flow<StudentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStudentsSync(students: List<StudentEntity>)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteStudentById(id: Long)
}
