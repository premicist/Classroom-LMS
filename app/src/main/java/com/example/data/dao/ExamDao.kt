package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.ExamEntity
import com.example.data.entity.ExamMarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    // Exams
    @Query("SELECT * FROM exams WHERE classroomId = :classroomId ORDER BY date DESC, createdAt DESC")
    fun getExamsForClassroom(classroomId: Long): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE classroomId = :classroomId ORDER BY date DESC, createdAt DESC")
    fun getExamsForClassroomOnce(classroomId: Long): List<ExamEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity): Long

    @Update
    suspend fun updateExam(exam: ExamEntity)

    @Delete
    suspend fun deleteExam(exam: ExamEntity)

    // Marks
    @Query("SELECT * FROM exam_marks WHERE examId = :examId")
    fun getMarksForExam(examId: Long): Flow<List<ExamMarkEntity>>

    @Query("SELECT * FROM exam_marks WHERE examId = :examId")
    fun getMarksForExamOnce(examId: Long): List<ExamMarkEntity>

    @Query("SELECT * FROM exam_marks WHERE studentId = :studentId")
    fun getMarksForStudentOnce(studentId: Long): List<ExamMarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMark(mark: ExamMarkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarks(marks: List<ExamMarkEntity>)

    @Update
    suspend fun updateMark(mark: ExamMarkEntity)

    @Update
    suspend fun updateMarks(marks: List<ExamMarkEntity>)

    @Transaction
    suspend fun deleteMarksForExam(examId: Long) {
        // Simple manual delete query
        deleteMarksByExamId(examId)
    }

    @Query("DELETE FROM exam_marks WHERE examId = :examId")
    suspend fun deleteMarksByExamId(examId: Long)
}
