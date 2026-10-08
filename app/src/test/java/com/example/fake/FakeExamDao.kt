package com.example.fake

import com.example.data.dao.ExamDao
import com.example.data.entity.ExamEntity
import com.example.data.entity.ExamMarkEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeExamDao : ExamDao {
    val examsFlow = MutableStateFlow<List<ExamEntity>>(emptyList())
    val marksFlow = MutableStateFlow<List<ExamMarkEntity>>(emptyList())
    private var nextExamId = 1L
    private var nextMarkId = 1L

    override fun getExamsForClassroom(classroomId: Long): Flow<List<ExamEntity>> {
        return examsFlow.map { list -> list.filter { it.classroomId == classroomId } }
    }

    override fun getExamsForClassroomOnce(classroomId: Long): List<ExamEntity> {
        return examsFlow.value.filter { it.classroomId == classroomId }
    }

    override fun getAllExamsOnce(): List<ExamEntity> {
        return examsFlow.value
    }

    override suspend fun insertExam(exam: ExamEntity): Long {
        val idToUse = if (exam.id == 0L) nextExamId++ else exam.id
        val item = exam.copy(id = idToUse)
        examsFlow.update { current ->
            val idx = current.indexOfFirst { it.id == idToUse }
            if (idx >= 0) current.toMutableList().apply { set(idx, item) } else current + item
        }
        return idToUse
    }

    override fun insertExamsSync(exams: List<ExamEntity>) {
        exams.forEach { e ->
            val idToUse = if (e.id == 0L) nextExamId++ else e.id
            val item = e.copy(id = idToUse)
            examsFlow.update { current ->
                val idx = current.indexOfFirst { it.id == idToUse }
                if (idx >= 0) current.toMutableList().apply { set(idx, item) } else current + item
            }
        }
    }

    override suspend fun updateExam(exam: ExamEntity) {
        insertExam(exam)
    }

    override suspend fun deleteExam(exam: ExamEntity) {
        examsFlow.update { current -> current.filter { it.id != exam.id } }
        marksFlow.update { current -> current.filter { it.examId != exam.id } }
    }

    override fun getMarksForClassroom(classroomId: Long): Flow<List<ExamMarkEntity>> {
        return combine(examsFlow, marksFlow) { exams, marks ->
            val classExamIds = exams.filter { it.classroomId == classroomId }.map { it.id }.toSet()
            marks.filter { it.examId in classExamIds }
        }
    }

    override fun getMarksForExam(examId: Long): Flow<List<ExamMarkEntity>> {
        return marksFlow.map { list -> list.filter { it.examId == examId } }
    }

    override fun getMarksForExamOnce(examId: Long): List<ExamMarkEntity> {
        return marksFlow.value.filter { it.examId == examId }
    }

    override fun getAllExamMarksOnce(): List<ExamMarkEntity> {
        return marksFlow.value
    }

    override fun getMarksForStudentOnce(studentId: Long): List<ExamMarkEntity> {
        return marksFlow.value.filter { it.studentId == studentId }
    }

    override suspend fun insertMark(mark: ExamMarkEntity): Long {
        val idToUse = if (mark.id == 0L) nextMarkId++ else mark.id
        val item = mark.copy(id = idToUse)
        marksFlow.update { current ->
            val idx = current.indexOfFirst { it.id == idToUse || (it.examId == mark.examId && it.studentId == mark.studentId) }
            if (idx >= 0) current.toMutableList().apply { set(idx, item) } else current + item
        }
        return idToUse
    }

    override suspend fun insertMarks(marks: List<ExamMarkEntity>) {
        marks.forEach { insertMark(it) }
    }

    override fun insertExamMarksSync(marks: List<ExamMarkEntity>) {
        marks.forEach { m ->
            val idToUse = if (m.id == 0L) nextMarkId++ else m.id
            val item = m.copy(id = idToUse)
            marksFlow.update { current ->
                val idx = current.indexOfFirst { it.id == idToUse || (it.examId == m.examId && it.studentId == m.studentId) }
                if (idx >= 0) current.toMutableList().apply { set(idx, m) } else current + item
            }
        }
    }

    override suspend fun updateMark(mark: ExamMarkEntity) {
        insertMark(mark)
    }

    override suspend fun updateMarks(marks: List<ExamMarkEntity>) {
        marks.forEach { insertMark(it) }
    }

    override suspend fun deleteMarksByExamId(examId: Long) {
        marksFlow.update { current -> current.filter { it.examId != examId } }
    }
}
