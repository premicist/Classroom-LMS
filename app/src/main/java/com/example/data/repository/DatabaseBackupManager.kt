package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.database.AppDatabase
import com.example.data.entity.AssignmentEntity
import com.example.data.entity.AttendanceRecordEntity
import com.example.data.entity.ClassScheduleEntity
import com.example.data.entity.ClassroomEntity
import com.example.data.entity.DailyLogEntity
import com.example.data.entity.DisciplineRecordEntity
import com.example.data.entity.ExamEntity
import com.example.data.entity.ExamMarkEntity
import com.example.data.entity.HomeworkRecordEntity
import com.example.data.entity.InterventionEntity
import com.example.data.entity.LessonPlanEntity
import com.example.data.entity.LiveAssessmentEntity
import com.example.data.entity.StudentEntity
import com.example.data.entity.SubmissionEntity
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

@JsonClass(generateAdapter = true)
data class DatabaseBackupData(
    val classrooms: List<ClassroomEntity> = emptyList(),
    val students: List<StudentEntity> = emptyList(),
    val assignments: List<AssignmentEntity> = emptyList(),
    val submissions: List<SubmissionEntity> = emptyList(),
    val homeworkRecords: List<HomeworkRecordEntity> = emptyList(),
    val attendanceRecords: List<AttendanceRecordEntity> = emptyList(),
    val interventions: List<InterventionEntity> = emptyList(),
    val lessonPlans: List<LessonPlanEntity> = emptyList(),
    val dailyLogs: List<DailyLogEntity> = emptyList(),
    val classSchedules: List<ClassScheduleEntity> = emptyList(),
    val disciplineRecords: List<DisciplineRecordEntity> = emptyList(),
    val liveAssessments: List<LiveAssessmentEntity> = emptyList(),
    val exams: List<ExamEntity> = emptyList(),
    val examMarks: List<ExamMarkEntity> = emptyList()
)

class DatabaseBackupManager(private val context: Context, private val db: AppDatabase) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(DatabaseBackupData::class.java)

    suspend fun exportDatabaseBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val backupData = DatabaseBackupData(
                classrooms = db.classroomDao().getAllClassroomsOnce(),
                students = db.studentDao().getAllStudentsOnce(),
                assignments = db.assignmentDao().getAllAssignmentsOnce(),
                submissions = db.submissionDao().getAllSubmissionsOnce(),
                homeworkRecords = db.homeworkRecordDao().getAllHomeworkRecordsOnce(),
                attendanceRecords = db.attendanceDao().getAllAttendanceRecordsOnce(),
                interventions = db.interventionDao().getAllInterventionsOnce(),
                lessonPlans = db.plannerDao().getAllLessonPlansOnce(),
                dailyLogs = db.plannerDao().getAllDailyLogsOnce(),
                classSchedules = db.classScheduleDao().getAllSchedulesOnce(),
                disciplineRecords = db.disciplineDao().getAllDisciplineRecordsOnce(),
                liveAssessments = db.liveAssessmentDao().getAllAssessmentsOnce(),
                exams = db.examDao().getAllExamsOnce(),
                examMarks = db.examDao().getAllExamMarksOnce()
            )

            val json = adapter.toJson(backupData)

            context.contentResolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                outputStream.write(json.toByteArray())
            } ?: throw IllegalStateException("Could not open output stream for URI")

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun importDatabaseBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { inputStream: InputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: throw IllegalStateException("Could not open input stream for URI")

            val backupData = adapter.fromJson(json) ?: throw IllegalStateException("Failed to parse JSON backup")

            db.runInTransaction {
                db.clearAllTables()
                db.classroomDao().insertClassroomsSync(backupData.classrooms)
                db.studentDao().insertStudentsSync(backupData.students)
                db.assignmentDao().insertAssignmentsSync(backupData.assignments)
                db.submissionDao().insertSubmissionsSync(backupData.submissions)
                db.homeworkRecordDao().insertHomeworkRecordsSync(backupData.homeworkRecords)
                db.attendanceDao().insertAttendanceRecordsSync(backupData.attendanceRecords)
                db.interventionDao().insertInterventionsSync(backupData.interventions)
                db.plannerDao().insertLessonPlansSync(backupData.lessonPlans)
                db.plannerDao().insertDailyLogsSync(backupData.dailyLogs)
                db.classScheduleDao().insertScheduleEntriesSync(backupData.classSchedules)
                db.disciplineDao().insertDisciplineRecordsSync(backupData.disciplineRecords)
                db.liveAssessmentDao().insertAssessmentsSync(backupData.liveAssessments)
                db.examDao().insertExamsSync(backupData.exams)
                db.examDao().insertExamMarksSync(backupData.examMarks)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
