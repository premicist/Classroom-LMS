package com.example.fake

import com.example.data.dao.AttendanceDao
import com.example.data.entity.AttendanceRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeAttendanceDao : AttendanceDao {
    val recordsFlow = MutableStateFlow<List<AttendanceRecordEntity>>(emptyList())
    private var nextId = 1L

    override fun getAttendanceByDate(classroomId: Long, date: String): Flow<List<AttendanceRecordEntity>> {
        return recordsFlow.map { list -> list.filter { it.classroomId == classroomId && it.date == date } }
    }

    override suspend fun getAttendanceByDateOnce(classroomId: Long, date: String): List<AttendanceRecordEntity> {
        return recordsFlow.value.filter { it.classroomId == classroomId && it.date == date }
    }

    override fun getAllAttendanceRecordsOnce(): List<AttendanceRecordEntity> {
        return recordsFlow.value
    }

    override fun getAttendanceForClassroom(classroomId: Long): Flow<List<AttendanceRecordEntity>> {
        return recordsFlow.map { list -> list.filter { it.classroomId == classroomId } }
    }

    override suspend fun getAttendanceForClassroomOnce(classroomId: Long): List<AttendanceRecordEntity> {
        return recordsFlow.value.filter { it.classroomId == classroomId }
    }

    override fun getAttendanceForStudent(studentId: Long): Flow<List<AttendanceRecordEntity>> {
        return recordsFlow.map { list -> list.filter { it.studentId == studentId } }
    }

    override fun getDistinctAttendanceDates(classroomId: Long): Flow<List<String>> {
        return recordsFlow.map { list -> list.filter { it.classroomId == classroomId }.map { it.date }.distinct() }
    }

    override suspend fun insertAttendanceRecord(record: AttendanceRecordEntity): Long {
        val idToUse = if (record.id == 0L) nextId++ else record.id
        val recWithId = record.copy(id = idToUse)
        recordsFlow.update { current ->
            val idx = current.indexOfFirst { it.id == idToUse || (it.studentId == record.studentId && it.date == record.date) }
            if (idx >= 0) {
                current.toMutableList().apply { set(idx, recWithId) }
            } else {
                current + recWithId
            }
        }
        return idToUse
    }

    override suspend fun insertAttendanceRecords(records: List<AttendanceRecordEntity>) {
        records.forEach { insertAttendanceRecord(it) }
    }

    override fun insertAttendanceRecordsSync(records: List<AttendanceRecordEntity>) {
        records.forEach {
            val idToUse = if (it.id == 0L) nextId++ else it.id
            val recWithId = it.copy(id = idToUse)
            recordsFlow.update { current ->
                val idx = current.indexOfFirst { item -> item.id == idToUse || (item.studentId == it.studentId && item.date == it.date) }
                if (idx >= 0) {
                    current.toMutableList().apply { set(idx, recWithId) }
                } else {
                    current + recWithId
                }
            }
        }
    }

    override suspend fun updateAttendanceRecord(record: AttendanceRecordEntity) {
        insertAttendanceRecord(record)
    }

    override suspend fun deleteAttendanceRecord(record: AttendanceRecordEntity) {
        recordsFlow.update { current -> current.filter { it.id != record.id } }
    }
}
