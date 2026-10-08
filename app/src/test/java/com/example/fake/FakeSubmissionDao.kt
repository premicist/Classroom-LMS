package com.example.fake

import com.example.data.dao.SubmissionDao
import com.example.data.entity.SubmissionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeSubmissionDao : SubmissionDao {
    val submissionsFlow = MutableStateFlow<List<SubmissionEntity>>(emptyList())
    private var nextId = 1L

    override fun getSubmissionsByClassroom(classroomId: Long): Flow<List<SubmissionEntity>> {
        return submissionsFlow.map { list -> list.filter { it.classroomId == classroomId } }
    }

    override suspend fun getSubmissionsByClassroomOnce(classroomId: Long): List<SubmissionEntity> {
        return submissionsFlow.value.filter { it.classroomId == classroomId }
    }

    override fun getSubmissionsByAssignment(assignmentId: Long): Flow<List<SubmissionEntity>> {
        return submissionsFlow.map { list -> list.filter { it.assignmentId == assignmentId } }
    }

    override suspend fun getSubmissionsByAssignmentOnce(assignmentId: Long): List<SubmissionEntity> {
        return submissionsFlow.value.filter { it.assignmentId == assignmentId }
    }

    override fun getAllSubmissionsOnce(): List<SubmissionEntity> {
        return submissionsFlow.value
    }

    override fun getSubmissionsByStudent(studentId: Long): Flow<List<SubmissionEntity>> {
        return submissionsFlow.map { list -> list.filter { it.studentId == studentId } }
    }

    override suspend fun getSubmissionsByStudentOnce(studentId: Long): List<SubmissionEntity> {
        return submissionsFlow.value.filter { it.studentId == studentId }
    }

    override fun getSubmission(assignmentId: Long, studentId: Long): Flow<SubmissionEntity?> {
        return submissionsFlow.map { list -> list.find { it.assignmentId == assignmentId && it.studentId == studentId } }
    }

    override suspend fun getSubmissionOnce(assignmentId: Long, studentId: Long): SubmissionEntity? {
        return submissionsFlow.value.find { it.assignmentId == assignmentId && it.studentId == studentId }
    }

    override suspend fun insertSubmission(submission: SubmissionEntity): Long {
        val idToUse = if (submission.id == 0L) nextId++ else submission.id
        val item = submission.copy(id = idToUse)
        submissionsFlow.update { current ->
            val idx = current.indexOfFirst { it.id == idToUse || (it.assignmentId == submission.assignmentId && it.studentId == submission.studentId) }
            if (idx >= 0) current.toMutableList().apply { set(idx, item) } else current + item
        }
        return idToUse
    }

    override suspend fun insertSubmissions(submissions: List<SubmissionEntity>) {
        submissions.forEach { insertSubmission(it) }
    }

    override fun insertSubmissionsSync(submissions: List<SubmissionEntity>) {
        submissions.forEach { s ->
            val idToUse = if (s.id == 0L) nextId++ else s.id
            val item = s.copy(id = idToUse)
            submissionsFlow.update { current ->
                val idx = current.indexOfFirst { it.id == idToUse || (it.assignmentId == s.assignmentId && it.studentId == s.studentId) }
                if (idx >= 0) current.toMutableList().apply { set(idx, item) } else current + item
            }
        }
    }

    override suspend fun updateSubmission(submission: SubmissionEntity) {
        insertSubmission(submission)
    }

    override suspend fun deleteSubmission(submission: SubmissionEntity) {
        submissionsFlow.update { current -> current.filter { it.id != submission.id } }
    }

    override suspend fun deleteSubmissionsByAssignmentId(assignmentId: Long) {
        submissionsFlow.update { current -> current.filter { it.assignmentId != assignmentId } }
    }
}
