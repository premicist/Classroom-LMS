package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.database.SampleDataGenerator
import com.example.data.entity.AssignmentEntity
import com.example.data.entity.AttendanceRecordEntity
import com.example.data.entity.ClassroomEntity
import com.example.data.entity.DailyLogEntity
import com.example.data.entity.DisciplineRecordEntity
import com.example.data.entity.HomeworkRecordEntity
import com.example.data.entity.InterventionEntity
import com.example.data.entity.LessonPlanEntity
import com.example.data.entity.LiveAssessmentEntity
import com.example.data.entity.StudentEntity
import com.example.data.entity.SubmissionEntity
import com.example.data.entity.SubmissionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class ClassroomRepository(private val database: AppDatabase) {

    private val classroomDao = database.classroomDao()
    private val studentDao = database.studentDao()
    private val assignmentDao = database.assignmentDao()
    private val submissionDao = database.submissionDao()
    private val homeworkDao = database.homeworkRecordDao()
    private val attendanceDao = database.attendanceDao()
    private val interventionDao = database.interventionDao()
    private val plannerDao = database.plannerDao()
    private val disciplineDao = database.disciplineDao()
    private val liveAssessmentDao = database.liveAssessmentDao()

    // Classrooms
    fun getAllClassrooms(): Flow<List<ClassroomEntity>> = classroomDao.getAllClassrooms()
    fun getClassroomById(id: Long): Flow<ClassroomEntity?> = classroomDao.getClassroomById(id)
    suspend fun insertClassroom(classroom: ClassroomEntity): Long = classroomDao.insertClassroom(classroom)
    suspend fun updateClassroom(classroom: ClassroomEntity) = classroomDao.updateClassroom(classroom)
    suspend fun deleteClassroom(id: Long) = classroomDao.deleteClassroomById(id)

    // Students
    fun getStudents(classroomId: Long): Flow<List<StudentEntity>> = studentDao.getStudentsByClassroom(classroomId)
    fun getAllStudents(): Flow<List<StudentEntity>> = studentDao.getAllStudents()
    fun getStudentById(id: Long): Flow<StudentEntity?> = studentDao.getStudentById(id)
    suspend fun insertStudent(student: StudentEntity): Long = studentDao.insertStudent(student)
    suspend fun insertStudents(students: List<StudentEntity>) = studentDao.insertStudents(students)
    suspend fun updateStudent(student: StudentEntity) = studentDao.updateStudent(student)
    suspend fun deleteStudent(id: Long) = studentDao.deleteStudentById(id)

    // Assignments
    fun getAssignments(classroomId: Long): Flow<List<AssignmentEntity>> = assignmentDao.getAssignmentsByClassroom(classroomId)
    suspend fun getAssignmentsByClassroomOnce(classroomId: Long): List<AssignmentEntity> = assignmentDao.getAssignmentsByClassroomOnce(classroomId)
    fun getAssignmentById(id: Long): Flow<AssignmentEntity?> = assignmentDao.getAssignmentById(id)
    suspend fun insertAssignment(assignment: AssignmentEntity, autoCreateSubmissions: Boolean = true): Long {
        val assignmentId = assignmentDao.insertAssignment(assignment)
        if (autoCreateSubmissions) {
            val students = studentDao.getStudentsByClassroomOnce(assignment.classroomId)
            val submissions = students.map { s ->
                SubmissionEntity(
                    assignmentId = assignmentId,
                    studentId = s.id,
                    classroomId = assignment.classroomId,
                    status = SubmissionStatus.PENDING,
                    score = null
                )
            }
            if (submissions.isNotEmpty()) {
                submissionDao.insertSubmissions(submissions)
            }
        }
        return assignmentId
    }
    suspend fun updateAssignment(assignment: AssignmentEntity) = assignmentDao.updateAssignment(assignment)
    suspend fun deleteAssignment(id: Long) {
        submissionDao.deleteSubmissionsByAssignmentId(id)
        assignmentDao.deleteAssignmentById(id)
    }

    // Submissions
    fun getSubmissionsByAssignment(assignmentId: Long): Flow<List<SubmissionEntity>> = submissionDao.getSubmissionsByAssignment(assignmentId)
    fun getSubmissionsByClassroom(classroomId: Long): Flow<List<SubmissionEntity>> = submissionDao.getSubmissionsByClassroom(classroomId)
    suspend fun getSubmissionsForStudentOnce(studentId: Long): List<SubmissionEntity> = submissionDao.getSubmissionsByStudentOnce(studentId)
    fun getSubmissionsByStudent(studentId: Long): Flow<List<SubmissionEntity>> = submissionDao.getSubmissionsByStudent(studentId)
    suspend fun updateSubmission(submission: SubmissionEntity) = submissionDao.updateSubmission(submission)
    suspend fun insertOrUpdateSubmissions(submissions: List<SubmissionEntity>) = submissionDao.insertSubmissions(submissions)

    // Homework Records
    fun getHomeworkRecords(classroomId: Long, date: String): Flow<List<HomeworkRecordEntity>> = homeworkDao.getHomeworkRecords(classroomId, date)
    fun getAllHomeworkRecordsForClassroom(classroomId: Long): Flow<List<HomeworkRecordEntity>> = homeworkDao.getAllHomeworkRecordsForClassroom(classroomId)
    fun getHomeworkRecordsForStudent(studentId: Long): Flow<List<HomeworkRecordEntity>> = homeworkDao.getHomeworkRecordsForStudent(studentId)
    fun getDistinctHomeworkDates(classroomId: Long): Flow<List<String>> = homeworkDao.getDistinctHomeworkDates(classroomId)
    suspend fun saveHomeworkRecord(record: HomeworkRecordEntity) = homeworkDao.insertHomeworkRecord(record)
    suspend fun saveHomeworkBatch(records: List<HomeworkRecordEntity>) = homeworkDao.insertHomeworkRecords(records)
    suspend fun updateHomeworkRecord(record: HomeworkRecordEntity) = homeworkDao.updateHomeworkRecord(record)
    suspend fun deleteHomeworkRecord(record: HomeworkRecordEntity) = homeworkDao.deleteHomeworkRecord(record)
    suspend fun deleteHomeworkBatch(classroomId: Long, date: String, topic: String) = homeworkDao.deleteHomeworkBatch(classroomId, date, topic)

    // Attendance Records
    fun getAttendanceByDate(classroomId: Long, date: String): Flow<List<AttendanceRecordEntity>> = attendanceDao.getAttendanceByDate(classroomId, date)
    fun getAttendanceForClassroom(classroomId: Long): Flow<List<AttendanceRecordEntity>> = attendanceDao.getAttendanceForClassroom(classroomId)
    fun getAttendanceForStudent(studentId: Long): Flow<List<AttendanceRecordEntity>> = attendanceDao.getAttendanceForStudent(studentId)
    fun getDistinctAttendanceDates(classroomId: Long): Flow<List<String>> = attendanceDao.getDistinctAttendanceDates(classroomId)
    suspend fun saveAttendanceRecord(record: AttendanceRecordEntity) = attendanceDao.insertAttendanceRecord(record)
    suspend fun saveAttendanceBatch(records: List<AttendanceRecordEntity>) = attendanceDao.insertAttendanceRecords(records)
    suspend fun updateAttendanceRecord(record: AttendanceRecordEntity) = attendanceDao.updateAttendanceRecord(record)

    // Teacher Intervention Records
    fun getInterventionsByClassroom(classroomId: Long): Flow<List<InterventionEntity>> = interventionDao.getInterventionsByClassroom(classroomId)
    fun getInterventionsByStudent(studentId: Long): Flow<List<InterventionEntity>> = interventionDao.getInterventionsByStudent(studentId)
    suspend fun saveIntervention(intervention: InterventionEntity): Long = interventionDao.insertIntervention(intervention)
    suspend fun updateIntervention(intervention: InterventionEntity) = interventionDao.updateIntervention(intervention)
    suspend fun deleteIntervention(id: Long) = interventionDao.deleteInterventionById(id)

    // Behavior & Discipline Records
    fun getDisciplineRecordsByClassroom(classroomId: Long): Flow<List<DisciplineRecordEntity>> = disciplineDao.getDisciplineRecordsByClassroom(classroomId)
    suspend fun getDisciplineRecordsByClassroomOnce(classroomId: Long): List<DisciplineRecordEntity> = disciplineDao.getDisciplineRecordsByClassroomOnce(classroomId)
    fun getDisciplineRecordsByStudent(studentId: Long): Flow<List<DisciplineRecordEntity>> = disciplineDao.getDisciplineRecordsByStudent(studentId)
    suspend fun getDisciplineRecordsByStudentOnce(studentId: Long): List<DisciplineRecordEntity> = disciplineDao.getDisciplineRecordsByStudentOnce(studentId)
    suspend fun saveDisciplineRecord(record: DisciplineRecordEntity): Long = disciplineDao.insertDisciplineRecord(record)
    suspend fun deleteDisciplineRecord(record: DisciplineRecordEntity) = disciplineDao.deleteDisciplineRecord(record)

    // Formative Live Class Assessment Records
    fun getLiveAssessmentsByClassroom(classroomId: Long): Flow<List<LiveAssessmentEntity>> = liveAssessmentDao.getAssessmentsByClassroom(classroomId)
    suspend fun getLiveAssessmentsByClassroomOnce(classroomId: Long): List<LiveAssessmentEntity> = liveAssessmentDao.getAssessmentsByClassroomOnce(classroomId)
    fun getLiveAssessmentsByStudent(studentId: Long): Flow<List<LiveAssessmentEntity>> = liveAssessmentDao.getAssessmentsByStudent(studentId)
    suspend fun saveLiveAssessment(assessment: LiveAssessmentEntity): Long = liveAssessmentDao.insertAssessment(assessment)
    suspend fun updateLiveAssessment(assessment: LiveAssessmentEntity) = liveAssessmentDao.updateAssessment(assessment)
    suspend fun deleteLiveAssessment(assessment: LiveAssessmentEntity) = liveAssessmentDao.deleteAssessment(assessment)

    // Lesson Plans & Daily Diary
    fun getLessonPlans(classroomId: Long): Flow<List<LessonPlanEntity>> = plannerDao.getLessonPlansForClassroom(classroomId)
    suspend fun saveLessonPlan(plan: LessonPlanEntity): Long = plannerDao.insertLessonPlan(plan)
    suspend fun updateLessonPlan(plan: LessonPlanEntity) = plannerDao.updateLessonPlan(plan)
    suspend fun deleteLessonPlan(plan: LessonPlanEntity) = plannerDao.deleteLessonPlan(plan)

    fun getDailyLogs(classroomId: Long): Flow<List<DailyLogEntity>> = plannerDao.getDailyLogsForClassroom(classroomId)
    suspend fun saveDailyLog(log: DailyLogEntity): Long = plannerDao.insertDailyLog(log)
    suspend fun updateDailyLog(log: DailyLogEntity) = plannerDao.updateDailyLog(log)
    suspend fun deleteDailyLog(log: DailyLogEntity) = plannerDao.deleteDailyLog(log)

    // Initialize or Reset Demo Data
    suspend fun checkAndSeedInitialData() {
        val existing = classroomDao.getAllClassrooms().firstOrNull()
        if (existing.isNullOrEmpty()) {
            SampleDataGenerator.populateSampleData(database)
        }
    }

    suspend fun resetWithSampleData() {
        database.clearAllTables()
        SampleDataGenerator.populateSampleData(database)
    }
}
