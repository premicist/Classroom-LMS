package com.example.fake

import com.example.data.database.AppDatabase
import com.example.data.entity.AssignmentEntity
import com.example.data.entity.SubmissionEntity
import com.example.data.repository.ClassroomRepository

class FakeClassroomRepository(
    db: AppDatabase,
    private val fakeAssignmentDao: FakeAssignmentDao,
    private val fakeSubmissionDao: FakeSubmissionDao,
    private val fakeStudentDao: FakeStudentDao
) : ClassroomRepository(db) {

    override suspend fun insertAssignment(assignment: AssignmentEntity, autoCreateSubmissions: Boolean): Long {
        val id = fakeAssignmentDao.insertAssignment(assignment)
        if (autoCreateSubmissions) {
            val students = fakeStudentDao.getStudentsByClassroomOnce(assignment.classroomId)
            val submissions = students.map {
                SubmissionEntity(assignmentId = id, studentId = it.id, classroomId = assignment.classroomId)
            }
            fakeSubmissionDao.insertSubmissions(submissions)
        }
        return id
    }

    override suspend fun updateAssignment(assignment: AssignmentEntity) {
        fakeAssignmentDao.updateAssignment(assignment)
    }

    override suspend fun deleteAssignment(id: Long) {
        fakeAssignmentDao.deleteAssignmentById(id)
    }

    override suspend fun updateSubmission(submission: SubmissionEntity) {
        fakeSubmissionDao.updateSubmission(submission)
    }

    override suspend fun insertOrUpdateSubmissions(submissions: List<SubmissionEntity>) {
        fakeSubmissionDao.insertSubmissions(submissions)
    }
}
