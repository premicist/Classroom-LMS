package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.HomeworkRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeworkRecordDao {
    @Query("SELECT * FROM homework_records WHERE classroomId = :classroomId AND date = :date ORDER BY id ASC")
    fun getHomeworkRecords(classroomId: Long, date: String): Flow<List<HomeworkRecordEntity>>

    @Query("SELECT * FROM homework_records WHERE classroomId = :classroomId AND date = :date")
    suspend fun getHomeworkRecordsOnce(classroomId: Long, date: String): List<HomeworkRecordEntity>

    @Query("SELECT * FROM homework_records WHERE classroomId = :classroomId ORDER BY date DESC")
    fun getAllHomeworkRecordsForClassroom(classroomId: Long): Flow<List<HomeworkRecordEntity>>

    @Query("SELECT * FROM homework_records WHERE studentId = :studentId ORDER BY date DESC")
    fun getHomeworkRecordsForStudent(studentId: Long): Flow<List<HomeworkRecordEntity>>

    @Query("SELECT DISTINCT date FROM homework_records WHERE classroomId = :classroomId ORDER BY date DESC")
    fun getDistinctHomeworkDates(classroomId: Long): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeworkRecord(record: HomeworkRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeworkRecords(records: List<HomeworkRecordEntity>)

    @Update
    suspend fun updateHomeworkRecord(record: HomeworkRecordEntity)

    @Delete
    suspend fun deleteHomeworkRecord(record: HomeworkRecordEntity)

    @Query("DELETE FROM homework_records WHERE classroomId = :classroomId AND date = :date AND topic = :topic")
    suspend fun deleteHomeworkBatch(classroomId: Long, date: String, topic: String)
}
