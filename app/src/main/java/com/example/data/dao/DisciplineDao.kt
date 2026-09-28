package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.DisciplineRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DisciplineDao {
    @Query("SELECT * FROM discipline_records WHERE classroomId = :classroomId ORDER BY date DESC, timestamp DESC")
    fun getDisciplineRecordsByClassroom(classroomId: Long): Flow<List<DisciplineRecordEntity>>

    @Query("SELECT * FROM discipline_records WHERE classroomId = :classroomId ORDER BY date DESC, timestamp DESC")
    suspend fun getDisciplineRecordsByClassroomOnce(classroomId: Long): List<DisciplineRecordEntity>

    @Query("SELECT * FROM discipline_records WHERE studentId = :studentId ORDER BY date DESC, timestamp DESC")
    fun getDisciplineRecordsByStudent(studentId: Long): Flow<List<DisciplineRecordEntity>>

    @Query("SELECT * FROM discipline_records WHERE studentId = :studentId ORDER BY date DESC, timestamp DESC")
    suspend fun getDisciplineRecordsByStudentOnce(studentId: Long): List<DisciplineRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDisciplineRecord(record: DisciplineRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDisciplineRecords(records: List<DisciplineRecordEntity>): List<Long>

    @Update
    suspend fun updateDisciplineRecord(record: DisciplineRecordEntity)

    @Delete
    suspend fun deleteDisciplineRecord(record: DisciplineRecordEntity)

    @Query("DELETE FROM discipline_records WHERE id = :id")
    suspend fun deleteDisciplineRecordById(id: Long)
}
