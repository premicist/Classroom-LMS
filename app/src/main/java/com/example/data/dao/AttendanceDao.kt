package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.AttendanceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE classroomId = :classroomId AND date = :date")
    fun getAttendanceByDate(classroomId: Long, date: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE classroomId = :classroomId AND date = :date")
    suspend fun getAttendanceByDateOnce(classroomId: Long, date: String): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE classroomId = :classroomId ORDER BY date DESC")
    fun getAttendanceForClassroom(classroomId: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE classroomId = :classroomId ORDER BY date DESC")
    suspend fun getAttendanceForClassroomOnce(classroomId: Long): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId ORDER BY date DESC")
    fun getAttendanceForStudent(studentId: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT DISTINCT date FROM attendance_records WHERE classroomId = :classroomId ORDER BY date DESC")
    fun getDistinctAttendanceDates(classroomId: Long): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecord(record: AttendanceRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecords(records: List<AttendanceRecordEntity>)

    @Update
    suspend fun updateAttendanceRecord(record: AttendanceRecordEntity)

    @Delete
    suspend fun deleteAttendanceRecord(record: AttendanceRecordEntity)
}
