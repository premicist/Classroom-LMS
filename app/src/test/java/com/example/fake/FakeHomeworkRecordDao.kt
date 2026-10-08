package com.example.fake

import com.example.data.dao.HomeworkRecordDao
import com.example.data.entity.HomeworkRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeHomeworkRecordDao : HomeworkRecordDao {
    val recordsFlow = MutableStateFlow<List<HomeworkRecordEntity>>(emptyList())
    private var nextId = 1L

    override fun getHomeworkRecords(classroomId: Long, date: String): Flow<List<HomeworkRecordEntity>> {
        return recordsFlow.map { list -> list.filter { it.classroomId == classroomId && it.date == date } }
    }

    override suspend fun getHomeworkRecordsOnce(classroomId: Long, date: String): List<HomeworkRecordEntity> {
        return recordsFlow.value.filter { it.classroomId == classroomId && it.date == date }
    }

    override fun getAllHomeworkRecordsOnce(): List<HomeworkRecordEntity> {
        return recordsFlow.value
    }

    override fun getAllHomeworkRecordsForClassroom(classroomId: Long): Flow<List<HomeworkRecordEntity>> {
        return recordsFlow.map { list -> list.filter { it.classroomId == classroomId } }
    }

    override suspend fun getAllHomeworkRecordsForClassroomOnce(classroomId: Long): List<HomeworkRecordEntity> {
        return recordsFlow.value.filter { it.classroomId == classroomId }
    }

    override fun getHomeworkRecordsForStudent(studentId: Long): Flow<List<HomeworkRecordEntity>> {
        return recordsFlow.map { list -> list.filter { it.studentId == studentId } }
    }

    override fun getDistinctHomeworkDates(classroomId: Long): Flow<List<String>> {
        return recordsFlow.map { list -> list.filter { it.classroomId == classroomId }.map { it.date }.distinct() }
    }

    override suspend fun insertHomeworkRecord(record: HomeworkRecordEntity): Long {
        val idToUse = if (record.id == 0L) nextId++ else record.id
        val recWithId = record.copy(id = idToUse)
        recordsFlow.update { current ->
            val idx = current.indexOfFirst { it.id == idToUse || (it.studentId == record.studentId && it.date == record.date && it.topic == record.topic) }
            if (idx >= 0) {
                current.toMutableList().apply { set(idx, recWithId) }
            } else {
                current + recWithId
            }
        }
        return idToUse
    }

    override suspend fun insertHomeworkRecords(records: List<HomeworkRecordEntity>) {
        records.forEach { insertHomeworkRecord(it) }
    }

    override fun insertHomeworkRecordsSync(records: List<HomeworkRecordEntity>) {
        records.forEach {
            val idToUse = if (it.id == 0L) nextId++ else it.id
            val recWithId = it.copy(id = idToUse)
            recordsFlow.update { current ->
                val idx = current.indexOfFirst { item -> item.id == idToUse || (item.studentId == it.studentId && item.date == it.date && item.topic == it.topic) }
                if (idx >= 0) {
                    current.toMutableList().apply { set(idx, recWithId) }
                } else {
                    current + recWithId
                }
            }
        }
    }

    override suspend fun updateHomeworkRecord(record: HomeworkRecordEntity) {
        insertHomeworkRecord(record)
    }

    override suspend fun deleteHomeworkRecord(record: HomeworkRecordEntity) {
        recordsFlow.update { current -> current.filter { it.id != record.id } }
    }

    override suspend fun deleteHomeworkBatch(classroomId: Long, date: String, topic: String) {
        recordsFlow.update { current -> current.filterNot { it.classroomId == classroomId && it.date == date && it.topic == topic } }
    }
}
