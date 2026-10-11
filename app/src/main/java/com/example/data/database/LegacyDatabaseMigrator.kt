package com.example.data.database

import android.content.Context
import android.net.Uri
import androidx.room.Room
import com.example.data.repository.DatabaseBackupData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import net.sqlcipher.database.SupportFactory
import java.io.File
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class MigrationResult {
    data class Success(val classroomsMigrated: Int, val studentsMigrated: Int) : MigrationResult()
    object AlreadyMigrated : MigrationResult()
    object NotRequired : MigrationResult()
    data class Failure(val error: Throwable, val message: String) : MigrationResult()
}

/**
 * Executes safe, atomic, one-time migration from unencrypted Room DB (classroom_lms_db)
 * to encrypted SQLCipher Room DB (classroom_lms_db_encrypted).
 */
@Singleton
class LegacyDatabaseMigrator @Inject constructor(
    private val context: Context
) {
    companion object {
        @Volatile
        private var INSTANCE: LegacyDatabaseMigrator? = null

        @Volatile
        var hasMigrationFailed: Boolean = false
            private set

        @Volatile
        var lastFailure: MigrationResult.Failure? = null
            private set

        fun getInstance(context: Context): LegacyDatabaseMigrator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LegacyDatabaseMigrator(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val migrationLock = Any()

    fun isMigrationRequired(): Boolean = isMigrationRequired(context)

    fun isMigrationRequired(ctx: Context): Boolean {
        val legacyDbFile = ctx.getDatabasePath(DatabaseMigrationEvaluator.LEGACY_DB_NAME)
        val encryptedDbFile = ctx.getDatabasePath(DatabaseMigrationEvaluator.ENCRYPTED_DB_NAME)
        val isDone = DatabaseSecurityHelper.isMigrationV1Completed(ctx)

        val legacyExists = legacyDbFile.exists() && legacyDbFile.length() > 0L
        val encryptedExists = encryptedDbFile.exists()

        val state = DatabaseMigrationEvaluator.evaluate(
            legacyDbExists = legacyExists,
            encryptedDbExists = encryptedExists,
            isMigrationDoneInPrefs = isDone,
            encryptedDbHasData = false
        )

        return state == MigrationState.MIGRATION_REQUIRED
    }

    fun isMigrationFailed(): Boolean = hasMigrationFailed

    fun getLastFailure(): MigrationResult.Failure? = lastFailure

    /**
     * Performs migration from the unencrypted legacy database to the encrypted database.
     * This operation is thread-safe and idempotent.
     */
    fun performMigration(): MigrationResult = synchronized(migrationLock) {
        if (!isMigrationRequired(context)) {
            val isDone = DatabaseSecurityHelper.isMigrationV1Completed(context)
            return if (isDone) MigrationResult.AlreadyMigrated else MigrationResult.NotRequired
        }

        var legacyDb: AppDatabase? = null
        var encryptedDb: AppDatabase? = null

        return kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 1. Open unencrypted legacy database with Room
                val sourceDb = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DatabaseMigrationEvaluator.LEGACY_DB_NAME
                )
                .addMigrations(
                    Migrations.MIGRATION_6_7,
                    Migrations.MIGRATION_7_8,
                    Migrations.MIGRATION_8_9
                )
                .allowMainThreadQueries()
                .build()
                legacyDb = sourceDb

                // 2. Fetch all data from legacy database
                val classrooms = sourceDb.classroomDao().getAllClassroomsOnce()
                val students = sourceDb.studentDao().getAllStudentsOnce()
                val assignments = sourceDb.assignmentDao().getAllAssignmentsOnce()
                val submissions = sourceDb.submissionDao().getAllSubmissionsOnce()
                val homeworkRecords = sourceDb.homeworkRecordDao().getAllHomeworkRecordsOnce()
                val attendanceRecords = sourceDb.attendanceDao().getAllAttendanceRecordsOnce()
                val interventions = sourceDb.interventionDao().getAllInterventionsOnce()
                val lessonPlans = sourceDb.plannerDao().getAllLessonPlansOnce()
                val dailyLogs = sourceDb.plannerDao().getAllDailyLogsOnce()
                val classSchedules = sourceDb.classScheduleDao().getAllSchedulesOnce()
                val disciplineRecords = sourceDb.disciplineDao().getAllDisciplineRecordsOnce()
                val liveAssessments = sourceDb.liveAssessmentDao().getAllAssessmentsOnce()
                val exams = sourceDb.examDao().getAllExamsOnce()
                val examMarks = sourceDb.examDao().getAllExamMarksOnce()

                // 3. Open or create encrypted SQLCipher database
                val passphrase = DatabaseSecurityHelper.getOrGeneratePassphrase(context)
                val factory = SupportFactory(passphrase)
                val targetDb = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DatabaseMigrationEvaluator.ENCRYPTED_DB_NAME
                )
                .openHelperFactory(factory)
                .addMigrations(
                    Migrations.MIGRATION_6_7,
                    Migrations.MIGRATION_7_8,
                    Migrations.MIGRATION_8_9
                )
                .allowMainThreadQueries()
                .build()
                encryptedDb = targetDb

                // 4. Copy all tables in a single atomic transaction
                targetDb.runInTransaction {
                    if (classrooms.isNotEmpty()) targetDb.classroomDao().insertClassroomsSync(classrooms)
                    if (students.isNotEmpty()) targetDb.studentDao().insertStudentsSync(students)
                    if (assignments.isNotEmpty()) targetDb.assignmentDao().insertAssignmentsSync(assignments)
                    if (submissions.isNotEmpty()) targetDb.submissionDao().insertSubmissionsSync(submissions)
                    if (homeworkRecords.isNotEmpty()) targetDb.homeworkRecordDao().insertHomeworkRecordsSync(homeworkRecords)
                    if (attendanceRecords.isNotEmpty()) targetDb.attendanceDao().insertAttendanceRecordsSync(attendanceRecords)
                    if (interventions.isNotEmpty()) targetDb.interventionDao().insertInterventionsSync(interventions)
                    if (lessonPlans.isNotEmpty()) targetDb.plannerDao().insertLessonPlansSync(lessonPlans)
                    if (dailyLogs.isNotEmpty()) targetDb.plannerDao().insertDailyLogsSync(dailyLogs)
                    if (classSchedules.isNotEmpty()) targetDb.classScheduleDao().insertScheduleEntriesSync(classSchedules)
                    if (disciplineRecords.isNotEmpty()) targetDb.disciplineDao().insertDisciplineRecordsSync(disciplineRecords)
                    if (liveAssessments.isNotEmpty()) targetDb.liveAssessmentDao().insertAssessmentsSync(liveAssessments)
                    if (exams.isNotEmpty()) targetDb.examDao().insertExamsSync(exams)
                    if (examMarks.isNotEmpty()) targetDb.examDao().insertExamMarksSync(examMarks)
                }

                // 5. Verification sanity check
                val encryptedClassroomCount = targetDb.classroomDao().getAllClassroomsOnce().size
                val encryptedStudentCount = targetDb.studentDao().getAllStudentsOnce().size
                if (encryptedClassroomCount < classrooms.size || encryptedStudentCount < students.size) {
                    throw IllegalStateException(
                        "Sanity check failed: Expected ${classrooms.size} classrooms and ${students.size} students, " +
                        "found $encryptedClassroomCount classrooms and $encryptedStudentCount students."
                    )
                }

                // Close databases cleanly before renaming legacy files
                try { sourceDb.close() } catch (_: Exception) {}
                try { targetDb.close() } catch (_: Exception) {}
                legacyDb = null
                encryptedDb = null

                // 6. Mark migration complete and rename legacy database to backup
                DatabaseSecurityHelper.setMigrationV1Completed(context, true)
                renameLegacyDbToBackup(context)

                hasMigrationFailed = false
                lastFailure = null

                MigrationResult.Success(
                    classroomsMigrated = classrooms.size,
                    studentsMigrated = students.size
                )
            } catch (e: Exception) {
                hasMigrationFailed = true
                val failure = MigrationResult.Failure(e, e.localizedMessage ?: "Unknown migration error")
                lastFailure = failure

                // On failure, clean up partial/corrupted encrypted database file to leave clean slate for retry
                try {
                    val encFile = context.getDatabasePath(DatabaseMigrationEvaluator.ENCRYPTED_DB_NAME)
                    if (encFile.exists()) encFile.delete()
                    listOf("-wal", "-shm", "-journal").forEach { suffix ->
                        val extra = File(encFile.parentFile, "${DatabaseMigrationEvaluator.ENCRYPTED_DB_NAME}$suffix")
                        if (extra.exists()) extra.delete()
                    }
                } catch (_: Exception) {}

                failure
            } finally {
                try { legacyDb?.close() } catch (_: Exception) {}
                try { encryptedDb?.close() } catch (_: Exception) {}
            }
        }
    }

    /**
     * Safely exports legacy unencrypted database content to a JSON backup stream.
     * Can be invoked even when SQLCipher migration fails, providing teachers an emergency rescue path.
     */
    fun exportLegacyDatabaseToJson(uri: Uri): Result<Unit> {
        val legacyDbFile = context.getDatabasePath(DatabaseMigrationEvaluator.LEGACY_DB_NAME)
        val backupDbFile = context.getDatabasePath(DatabaseMigrationEvaluator.LEGACY_BACKUP_DB_NAME)

        val targetName = when {
            legacyDbFile.exists() -> DatabaseMigrationEvaluator.LEGACY_DB_NAME
            backupDbFile.exists() -> DatabaseMigrationEvaluator.LEGACY_BACKUP_DB_NAME
            else -> return Result.failure(IllegalStateException("No legacy database file found to export."))
        }

        var legacyDb: AppDatabase? = null
        return kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val sourceDb = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    targetName
                )
                .addMigrations(
                    Migrations.MIGRATION_6_7,
                    Migrations.MIGRATION_7_8,
                    Migrations.MIGRATION_8_9
                )
                .allowMainThreadQueries()
                .build()
                legacyDb = sourceDb

                val backupData = DatabaseBackupData(
                    classrooms = sourceDb.classroomDao().getAllClassroomsOnce(),
                    students = sourceDb.studentDao().getAllStudentsOnce(),
                    assignments = sourceDb.assignmentDao().getAllAssignmentsOnce(),
                    submissions = sourceDb.submissionDao().getAllSubmissionsOnce(),
                    homeworkRecords = sourceDb.homeworkRecordDao().getAllHomeworkRecordsOnce(),
                    attendanceRecords = sourceDb.attendanceDao().getAllAttendanceRecordsOnce(),
                    interventions = sourceDb.interventionDao().getAllInterventionsOnce(),
                    lessonPlans = sourceDb.plannerDao().getAllLessonPlansOnce(),
                    dailyLogs = sourceDb.plannerDao().getAllDailyLogsOnce(),
                    classSchedules = sourceDb.classScheduleDao().getAllSchedulesOnce(),
                    disciplineRecords = sourceDb.disciplineDao().getAllDisciplineRecordsOnce(),
                    liveAssessments = sourceDb.liveAssessmentDao().getAllAssessmentsOnce(),
                    exams = sourceDb.examDao().getAllExamsOnce(),
                    examMarks = sourceDb.examDao().getAllExamMarksOnce()
                )

                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(DatabaseBackupData::class.java)
                val json = adapter.toJson(backupData)

                context.contentResolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                    outputStream.write(json.toByteArray())
                } ?: throw IllegalStateException("Could not open output stream for destination URI")

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                try { legacyDb?.close() } catch (_: Exception) {}
            }
        }
    }

    private fun renameLegacyDbToBackup(ctx: Context) {
        val dbFile = ctx.getDatabasePath(DatabaseMigrationEvaluator.LEGACY_DB_NAME)
        if (dbFile.exists()) {
            val bakFile = ctx.getDatabasePath(DatabaseMigrationEvaluator.LEGACY_BACKUP_DB_NAME)
            if (bakFile.exists()) bakFile.delete()
            dbFile.renameTo(bakFile)
        }
        listOf("-wal", "-shm", "-journal").forEach { suffix ->
            val extra = File(dbFile.parentFile, "${DatabaseMigrationEvaluator.LEGACY_DB_NAME}$suffix")
            if (extra.exists()) {
                val bakExtra = File(dbFile.parentFile, "${DatabaseMigrationEvaluator.LEGACY_BACKUP_DB_NAME}$suffix")
                if (bakExtra.exists()) bakExtra.delete()
                extra.renameTo(bakExtra)
            }
        }
    }
}
