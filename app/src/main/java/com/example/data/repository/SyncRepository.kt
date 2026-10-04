package com.example.data.repository

import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.entity.AssignmentEntity
import com.example.data.entity.AttendanceRecordEntity
import com.example.data.entity.AttendanceStatus
import com.example.data.entity.BehaviorCategory
import com.example.data.entity.BehaviorSeverity
import com.example.data.entity.DisciplineRecordEntity
import com.example.data.entity.ExamCategory
import com.example.data.entity.ExamEntity
import com.example.data.entity.ExamMarkEntity
import com.example.data.entity.HomeworkRecordEntity
import com.example.data.entity.HomeworkStatus
import com.example.data.entity.InterventionEntity
import com.example.data.entity.InterventionType
import com.example.data.entity.StudentEntity
import com.example.data.entity.SubmissionEntity
import com.example.data.entity.SubmissionStatus
import com.example.data.network.AddSheetRequest
import com.example.data.network.BatchUpdateSpreadsheetRequest
import com.example.data.network.GoogleSheetsApi
import com.example.data.network.Request
import com.example.data.network.SheetProperties
import com.example.data.network.Spreadsheet
import com.example.data.network.ValueRange
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncRepository(
    private val database: AppDatabase,
    private val accessToken: String
) {
    private val TAG = "SyncRepository"
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://sheets.googleapis.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(GoogleSheetsApi::class.java)

    /**
     * Performs a full 2-way sync:
     * 1. Pulls & merges Roster from Sheets.
     * 2. Pulls & merges Attendance from Sheets.
     * 3. Pulls & merges Homework records from Sheets.
     * 4. Pulls & merges Grades/Assignments from Sheets.
     * 5. Pulls & merges Interventions from Sheets.
     */
    suspend fun syncClassroomRoster(classroomId: Long, spreadsheetId: String): String = withContext(Dispatchers.IO) {
        val authHeader = "Bearer $accessToken"
        
        // Fetch metadata to find the main sheet tab
        val spreadsheet = try {
            api.getSpreadsheet(spreadsheetId, authHeader)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to spreadsheet", e)
            return@withContext "Failed to connect to Google Sheet. Check Sheet ID and permissions."
        }

        // --- NEW: Sync Exam Marks from Multi-Tab Layouts ---
        syncExamMarks(classroomId, spreadsheet, authHeader)

        val rosterSheetTitle = spreadsheet.sheets?.firstOrNull()?.properties?.title ?: "Sheet1"
        val range = "$rosterSheetTitle!A:Z"

        val response = try {
            api.getSheetValues(spreadsheetId, range, authHeader)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read sheet values", e)
            return@withContext "Failed to read sheet '$rosterSheetTitle'."
        }
        
        val rows = response.values ?: return@withContext "Sheet '$rosterSheetTitle' is empty."
        if (rows.isEmpty()) return@withContext "Sheet '$rosterSheetTitle' is empty."
            
        // Assume Row 0 is headers
        val headers = rows[0]
        val studentNumberIndex = headers.indexOfFirst { it.equals("Student Number", ignoreCase = true) || it.equals("Student ID", ignoreCase = true) }
        val nameIndex = headers.indexOfFirst { it.equals("Name", ignoreCase = true) || it.equals("Student Name", ignoreCase = true) || it.equals("First Name", ignoreCase = true) }
        val emailIndex = headers.indexOfFirst { it.equals("Email", ignoreCase = true) }

        if (nameIndex == -1) {
            return@withContext "Sync failed: Could not find a 'Name' column in row 1 of '$rosterSheetTitle'."
        }

        val parsedStudents = mutableListOf<StudentEntity>()
        
        for (i in 1 until rows.size) {
            val row = rows[i]
            if (row.isEmpty()) continue
            
            val studentNumber = if (studentNumberIndex >= 0) row.getOrNull(studentNumberIndex) ?: "" else ""
            val name = if (nameIndex >= 0) row.getOrNull(nameIndex) ?: "" else ""
            val email = if (emailIndex >= 0) row.getOrNull(emailIndex) ?: "" else ""
            
            if (name.isBlank()) continue
            
            val customAttributes = mutableMapOf<String, String>()
            for (j in headers.indices) {
                if (j != studentNumberIndex && j != nameIndex && j != emailIndex) {
                    val headerName = headers[j]
                    val value = row.getOrNull(j)
                    if (headerName.isNotBlank() && !value.isNullOrBlank()) {
                        customAttributes[headerName] = value
                    }
                }
            }
            
            parsedStudents.add(StudentEntity(
                classroomId = classroomId,
                name = name,
                studentNumber = studentNumber,
                email = email,
                customAttributes = customAttributes
            ))
        }

        // Smart Sync (Upsert) - DO NOT DELETE ANYONE
        val existingStudents = database.studentDao().getStudentsByClassroomOnce(classroomId)
        var addedCount = 0
        var updatedCount = 0
        
        for (parsed in parsedStudents) {
            val existing = existingStudents.find { 
                (it.studentNumber.isNotBlank() && it.studentNumber == parsed.studentNumber) || 
                (it.name.equals(parsed.name, ignoreCase = true)) 
            }
            
            if (existing != null) {
                if (existing.name != parsed.name || 
                    existing.studentNumber != parsed.studentNumber || 
                    existing.email != parsed.email ||
                    existing.customAttributes != parsed.customAttributes) {
                    
                    val updatedStudent = existing.copy(
                        name = parsed.name,
                        studentNumber = parsed.studentNumber,
                        email = parsed.email,
                        customAttributes = parsed.customAttributes
                    )
                    database.studentDao().updateStudent(updatedStudent)
                    updatedCount++
                }
            } else {
                database.studentDao().insertStudent(parsed)
                addedCount++
            }
        }

        val allStudents = database.studentDao().getStudentsByClassroomOnce(classroomId)

        // 2-Way Sync Attendance, Homework, Grades, Interventions, and Discipline from Sheets
        syncAttendanceFromSheet(classroomId, spreadsheetId, authHeader, allStudents)
        syncHomeworkFromSheet(classroomId, spreadsheetId, authHeader, allStudents)
        syncGradesFromSheet(classroomId, spreadsheetId, authHeader, allStudents)
        syncInterventionsFromSheet(classroomId, spreadsheetId, authHeader, allStudents)
        syncDisciplineFromSheet(classroomId, spreadsheetId, authHeader, allStudents)

        if (addedCount == 0 && updatedCount == 0) {
            return@withContext "Roster up to date."
        } else {
            return@withContext "Roster synced: Added $addedCount, Updated $updatedCount."
        }
    }

    private suspend fun syncAttendanceFromSheet(classroomId: Long, spreadsheetId: String, authHeader: String, students: List<StudentEntity>) {
        val range = "Attendance!A:Z"
        val response = try {
            api.getSheetValues(spreadsheetId, range, authHeader)
        } catch (e: Exception) {
            Log.d(TAG, "Attendance tab not found or unreadable: ${e.message}")
            return
        }
        val rows = response.values ?: return
        if (rows.size < 2) return

        val headers = rows[0]
        val studentIdIdx = headers.indexOfFirst { it.equals("Student ID", ignoreCase = true) || it.equals("Student Number", ignoreCase = true) }
        val nameIdx = headers.indexOfFirst { it.equals("Student Name", ignoreCase = true) || it.equals("Name", ignoreCase = true) }

        val dateIndices = mutableListOf<Pair<Int, String>>()
        for (j in headers.indices) {
            if (j != studentIdIdx && j != nameIdx) {
                val header = headers[j].trim()
                if (header.isNotBlank()) {
                    dateIndices.add(j to header)
                }
            }
        }

        val existingRecords = database.attendanceDao().getAttendanceForClassroomOnce(classroomId)

        for (i in 1 until rows.size) {
            val row = rows[i]
            if (row.isEmpty()) continue
            val studentIdStr = if (studentIdIdx >= 0) row.getOrNull(studentIdIdx) ?: "" else ""
            val nameStr = if (nameIdx >= 0) row.getOrNull(nameIdx) ?: "" else ""

            val student = students.find {
                (studentIdStr.isNotBlank() && it.studentNumber == studentIdStr) ||
                (nameStr.isNotBlank() && it.name.equals(nameStr, ignoreCase = true))
            } ?: continue

            for ((colIdx, dateStr) in dateIndices) {
                val cellVal = row.getOrNull(colIdx)?.trim() ?: ""
                if (cellVal.isBlank()) continue

                val status = parseAttendanceStatus(cellVal)
                val existing = existingRecords.find { it.studentId == student.id && it.date == dateStr }

                if (existing != null) {
                    if (existing.status != status) {
                        database.attendanceDao().updateAttendanceRecord(existing.copy(status = status))
                    }
                } else {
                    database.attendanceDao().insertAttendanceRecord(
                        AttendanceRecordEntity(
                            classroomId = classroomId,
                            studentId = student.id,
                            date = dateStr,
                            status = status
                        )
                    )
                }
            }
        }
    }

    private fun parseAttendanceStatus(value: String): AttendanceStatus {
        return when (value.uppercase(Locale.US)) {
            "P", "PRESENT", "1" -> AttendanceStatus.PRESENT
            "A", "ABSENT", "0" -> AttendanceStatus.ABSENT
            "T", "TARDY", "L", "LATE" -> AttendanceStatus.LATE
            "E", "EXCUSED" -> AttendanceStatus.EXCUSED
            else -> AttendanceStatus.PRESENT
        }
    }

    private suspend fun syncHomeworkFromSheet(classroomId: Long, spreadsheetId: String, authHeader: String, students: List<StudentEntity>) {
        val range = "Homework!A:Z"
        val response = try {
            api.getSheetValues(spreadsheetId, range, authHeader)
        } catch (e: Exception) {
            Log.d(TAG, "Homework tab not found or unreadable: ${e.message}")
            return
        }
        val rows = response.values ?: return
        if (rows.size < 2) return

        val headers = rows[0]
        val studentIdIdx = headers.indexOfFirst { it.equals("Student ID", ignoreCase = true) || it.equals("Student Number", ignoreCase = true) }
        val nameIdx = headers.indexOfFirst { it.equals("Student Name", ignoreCase = true) || it.equals("Name", ignoreCase = true) }

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val taskIndices = mutableListOf<Triple<Int, String, String>>()
        for (j in headers.indices) {
            if (j != studentIdIdx && j != nameIdx) {
                val header = headers[j].trim()
                if (header.isNotBlank()) {
                    val parts = header.split(" - ", limit = 2)
                    val dateStr = if (parts.size == 2 && parts[0].matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) parts[0] else todayStr
                    val topicStr = if (parts.size == 2 && parts[0].matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) parts[1] else header
                    taskIndices.add(Triple(j, dateStr, topicStr))
                }
            }
        }

        val existingRecords = database.homeworkRecordDao().getAllHomeworkRecordsForClassroomOnce(classroomId)

        for (i in 1 until rows.size) {
            val row = rows[i]
            if (row.isEmpty()) continue
            val studentIdStr = if (studentIdIdx >= 0) row.getOrNull(studentIdIdx) ?: "" else ""
            val nameStr = if (nameIdx >= 0) row.getOrNull(nameIdx) ?: "" else ""

            val student = students.find {
                (studentIdStr.isNotBlank() && it.studentNumber == studentIdStr) ||
                (nameStr.isNotBlank() && it.name.equals(nameStr, ignoreCase = true))
            } ?: continue

            for ((colIdx, dateStr, topicStr) in taskIndices) {
                val cellVal = row.getOrNull(colIdx)?.trim() ?: ""
                if (cellVal.isBlank()) continue

                val status = parseHomeworkStatus(cellVal)
                val existing = existingRecords.find {
                    it.studentId == student.id && it.date == dateStr && it.topic.equals(topicStr, ignoreCase = true)
                }

                if (existing != null) {
                    if (existing.status != status) {
                        database.homeworkRecordDao().updateHomeworkRecord(existing.copy(status = status))
                    }
                } else {
                    database.homeworkRecordDao().insertHomeworkRecord(
                        HomeworkRecordEntity(
                            classroomId = classroomId,
                            studentId = student.id,
                            date = dateStr,
                            topic = topicStr,
                            status = status
                        )
                    )
                }
            }
        }
    }

    private fun parseHomeworkStatus(value: String): HomeworkStatus {
        return when (value.uppercase(Locale.US)) {
            "DONE", "D", "1", "1.0", "P", "PRESENT" -> HomeworkStatus.DONE
            "PARTIAL", "PART", "0.5", "HALF" -> HomeworkStatus.PARTIAL
            "MISSING", "M", "0", "0.0", "ABSENT" -> HomeworkStatus.MISSING
            "EXCUSED", "E", "EX" -> HomeworkStatus.EXCUSED
            else -> HomeworkStatus.DONE
        }
    }

    private suspend fun syncGradesFromSheet(classroomId: Long, spreadsheetId: String, authHeader: String, students: List<StudentEntity>) {
        val range = "Grades!A:Z"
        val response = try {
            api.getSheetValues(spreadsheetId, range, authHeader)
        } catch (e: Exception) {
            Log.d(TAG, "Grades tab not found or unreadable: ${e.message}")
            return
        }
        val rows = response.values ?: return
        if (rows.size < 2) return

        val headers = rows[0]
        val studentIdIdx = headers.indexOfFirst { it.equals("Student ID", ignoreCase = true) || it.equals("Student Number", ignoreCase = true) }
        val nameIdx = headers.indexOfFirst { it.equals("Student Name", ignoreCase = true) || it.equals("Name", ignoreCase = true) }

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val existingAssignments = database.assignmentDao().getAssignmentsByClassroomOnce(classroomId).toMutableList()
        val assignmentColMap = mutableListOf<Pair<Int, AssignmentEntity>>()

        for (j in headers.indices) {
            if (j != studentIdIdx && j != nameIdx) {
                val header = headers[j].trim()
                if (header.isNotBlank()) {
                    val title = header.substringBefore("(").trim()
                    val maxPts = if (header.contains("(") && header.contains("pts")) {
                        header.substringAfter("(").substringBefore("pts").trim().toDoubleOrNull() ?: 100.0
                    } else 100.0

                    var assignment = existingAssignments.find { it.title.equals(title, ignoreCase = true) }
                    if (assignment == null) {
                        val newAssign = AssignmentEntity(
                            classroomId = classroomId,
                            title = title,
                            maxPoints = maxPts,
                            dueDate = todayStr,
                            isGraded = true
                        )
                        val newId = database.assignmentDao().insertAssignment(newAssign)
                        assignment = newAssign.copy(id = newId)
                        existingAssignments.add(assignment)
                    }
                    assignmentColMap.add(j to assignment)
                }
            }
        }

        val existingSubmissions = database.submissionDao().getSubmissionsByClassroomOnce(classroomId)

        for (i in 1 until rows.size) {
            val row = rows[i]
            if (row.isEmpty()) continue
            val studentIdStr = if (studentIdIdx >= 0) row.getOrNull(studentIdIdx) ?: "" else ""
            val nameStr = if (nameIdx >= 0) row.getOrNull(nameIdx) ?: "" else ""

            val student = students.find {
                (studentIdStr.isNotBlank() && it.studentNumber == studentIdStr) ||
                (nameStr.isNotBlank() && it.name.equals(nameStr, ignoreCase = true))
            } ?: continue

            for ((colIdx, assignment) in assignmentColMap) {
                val cellVal = row.getOrNull(colIdx)?.trim() ?: ""
                if (cellVal.isBlank()) continue

                val scoreDouble = cellVal.toDoubleOrNull()
                val status = when {
                    scoreDouble != null -> SubmissionStatus.GRADED
                    cellVal.equals("MISSING", ignoreCase = true) || cellVal.equals("M", ignoreCase = true) -> SubmissionStatus.MISSING
                    cellVal.equals("LATE", ignoreCase = true) -> SubmissionStatus.LATE
                    cellVal.equals("SUBMITTED", ignoreCase = true) -> SubmissionStatus.SUBMITTED
                    else -> SubmissionStatus.GRADED
                }

                val existing = existingSubmissions.find { it.assignmentId == assignment.id && it.studentId == student.id }

                if (existing != null) {
                    val updated = existing.copy(
                        score = scoreDouble ?: existing.score,
                        status = status
                    )
                    database.submissionDao().updateSubmission(updated)
                } else {
                    database.submissionDao().insertSubmission(
                        SubmissionEntity(
                            assignmentId = assignment.id,
                            studentId = student.id,
                            classroomId = classroomId,
                            status = status,
                            score = scoreDouble
                        )
                    )
                }
            }
        }
    }

    private suspend fun syncInterventionsFromSheet(classroomId: Long, spreadsheetId: String, authHeader: String, students: List<StudentEntity>) {
        val range = "Interventions!A:Z"
        val response = try {
            api.getSheetValues(spreadsheetId, range, authHeader)
        } catch (e: Exception) {
            Log.d(TAG, "Interventions tab not found or unreadable: ${e.message}")
            return
        }
        val rows = response.values ?: return
        if (rows.size < 2) return

        val headers = rows[0]
        val studentIdIdx = headers.indexOfFirst { it.equals("Student ID", ignoreCase = true) || it.equals("Student Number", ignoreCase = true) }
        val nameIdx = headers.indexOfFirst { it.equals("Student Name", ignoreCase = true) || it.equals("Name", ignoreCase = true) }
        val dateIdx = headers.indexOfFirst { it.equals("Date", ignoreCase = true) }
        val typeIdx = headers.indexOfFirst { it.equals("Type", ignoreCase = true) }
        val titleIdx = headers.indexOfFirst { it.equals("Title", ignoreCase = true) }
        val notesIdx = headers.indexOfFirst { it.equals("Notes", ignoreCase = true) }
        val resolvedIdx = headers.indexOfFirst { it.equals("Resolved", ignoreCase = true) }

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val existingInterventions = database.interventionDao().getInterventionsByClassroomOnce(classroomId)

        for (i in 1 until rows.size) {
            val row = rows[i]
            if (row.isEmpty()) continue
            val studentIdStr = if (studentIdIdx >= 0) row.getOrNull(studentIdIdx) ?: "" else ""
            val nameStr = if (nameIdx >= 0) row.getOrNull(nameIdx) ?: "" else ""

            val student = students.find {
                (studentIdStr.isNotBlank() && it.studentNumber == studentIdStr) ||
                (nameStr.isNotBlank() && it.name.equals(nameStr, ignoreCase = true))
            } ?: continue

            val dateStr = if (dateIdx >= 0) row.getOrNull(dateIdx)?.trim()?.ifBlank { todayStr } ?: todayStr else todayStr
            val titleStr = if (titleIdx >= 0) row.getOrNull(titleIdx)?.trim() ?: "" else ""
            if (titleStr.isBlank()) continue

            val typeStr = if (typeIdx >= 0) row.getOrNull(typeIdx)?.trim() ?: "" else ""
            val notesStr = if (notesIdx >= 0) row.getOrNull(notesIdx)?.trim() ?: "" else ""
            val resolvedBool = if (resolvedIdx >= 0) row.getOrNull(resolvedIdx)?.trim()?.equals("true", ignoreCase = true) == true || row.getOrNull(resolvedIdx)?.trim()?.equals("yes", ignoreCase = true) == true else false

            val typeEnum = InterventionType.entries.find {
                it.displayName.equals(typeStr, ignoreCase = true) || it.name.equals(typeStr, ignoreCase = true)
            } ?: InterventionType.TUTORING

            val existing = existingInterventions.find {
                it.studentId == student.id && it.date == dateStr && it.title.equals(titleStr, ignoreCase = true)
            }

            if (existing != null) {
                database.interventionDao().updateIntervention(
                    existing.copy(
                        type = typeEnum,
                        notes = notesStr,
                        resolved = resolvedBool
                    )
                )
            } else {
                database.interventionDao().insertIntervention(
                    InterventionEntity(
                        studentId = student.id,
                        classroomId = classroomId,
                        date = dateStr,
                        type = typeEnum,
                        title = titleStr,
                        notes = notesStr,
                        resolved = resolvedBool
                    )
                )
            }
        }
    }

    private suspend fun syncDisciplineFromSheet(classroomId: Long, spreadsheetId: String, authHeader: String, students: List<StudentEntity>) {
        val range = "Discipline_Log!A:Z"
        val response = try {
            api.getSheetValues(spreadsheetId, range, authHeader)
        } catch (e: Exception) {
            Log.d(TAG, "Discipline_Log tab not found or unreadable: ${e.message}")
            return
        }
        val rows = response.values ?: return
        if (rows.size < 2) return

        val headers = rows[0]
        val studentIdIdx = headers.indexOfFirst { it.equals("Student ID", ignoreCase = true) || it.equals("Student Number", ignoreCase = true) }
        val nameIdx = headers.indexOfFirst { it.equals("Student Name", ignoreCase = true) || it.equals("Name", ignoreCase = true) }
        val dateIdx = headers.indexOfFirst { it.equals("Date", ignoreCase = true) }
        val categoryIdx = headers.indexOfFirst { it.equals("Category", ignoreCase = true) }
        val severityIdx = headers.indexOfFirst { it.equals("Severity", ignoreCase = true) }
        val titleIdx = headers.indexOfFirst { it.equals("Title", ignoreCase = true) }
        val descIdx = headers.indexOfFirst { it.equals("Description", ignoreCase = true) }
        val actionIdx = headers.indexOfFirst { it.equals("Action Taken", ignoreCase = true) }
        val parentNotifiedIdx = headers.indexOfFirst { it.equals("Parent Notified", ignoreCase = true) }
        val resolvedIdx = headers.indexOfFirst { it.equals("Resolved", ignoreCase = true) }

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val existingDiscipline = database.disciplineDao().getDisciplineRecordsByClassroomOnce(classroomId)

        for (i in 1 until rows.size) {
            val row = rows[i]
            if (row.isEmpty()) continue
            val studentIdStr = if (studentIdIdx >= 0) row.getOrNull(studentIdIdx) ?: "" else ""
            val nameStr = if (nameIdx >= 0) row.getOrNull(nameIdx) ?: "" else ""

            val student = students.find {
                (studentIdStr.isNotBlank() && it.studentNumber == studentIdStr) ||
                (nameStr.isNotBlank() && it.name.equals(nameStr, ignoreCase = true))
            } ?: continue

            val dateStr = if (dateIdx >= 0) row.getOrNull(dateIdx)?.trim()?.ifBlank { todayStr } ?: todayStr else todayStr
            val titleStr = if (titleIdx >= 0) row.getOrNull(titleIdx)?.trim() ?: "" else ""
            if (titleStr.isBlank()) continue

            val categoryStr = if (categoryIdx >= 0) row.getOrNull(categoryIdx)?.trim() ?: "" else ""
            val severityStr = if (severityIdx >= 0) row.getOrNull(severityIdx)?.trim() ?: "" else ""
            val descStr = if (descIdx >= 0) row.getOrNull(descIdx)?.trim() ?: "" else ""
            val actionStr = if (actionIdx >= 0) row.getOrNull(actionIdx)?.trim() ?: "" else ""
            val parentNotifiedBool = if (parentNotifiedIdx >= 0) row.getOrNull(parentNotifiedIdx)?.trim()?.equals("true", ignoreCase = true) == true || row.getOrNull(parentNotifiedIdx)?.trim()?.equals("yes", ignoreCase = true) == true else false
            val resolvedBool = if (resolvedIdx >= 0) row.getOrNull(resolvedIdx)?.trim()?.equals("true", ignoreCase = true) == true || row.getOrNull(resolvedIdx)?.trim()?.equals("yes", ignoreCase = true) == true else false

            val categoryEnum = BehaviorCategory.entries.find {
                it.displayName.equals(categoryStr, ignoreCase = true) || it.name.equals(categoryStr, ignoreCase = true)
            } ?: BehaviorCategory.CLASSROOM_DISRUPTION

            val severityEnum = BehaviorSeverity.entries.find {
                it.displayName.equals(severityStr, ignoreCase = true) || it.name.equals(severityStr, ignoreCase = true)
            } ?: BehaviorSeverity.LOW_WARNING

            val existing = existingDiscipline.find {
                it.studentId == student.id && it.date == dateStr && it.title.equals(titleStr, ignoreCase = true)
            }

            if (existing != null) {
                database.disciplineDao().updateDisciplineRecord(
                    existing.copy(
                        category = categoryEnum,
                        severity = severityEnum,
                        description = descStr,
                        actionTaken = actionStr,
                        parentNotified = parentNotifiedBool,
                        resolved = resolvedBool
                    )
                )
            } else {
                database.disciplineDao().insertDisciplineRecord(
                    DisciplineRecordEntity(
                        studentId = student.id,
                        classroomId = classroomId,
                        date = dateStr,
                        category = categoryEnum,
                        severity = severityEnum,
                        title = titleStr,
                        description = descStr,
                        actionTaken = actionStr,
                        parentNotified = parentNotifiedBool,
                        resolved = resolvedBool
                    )
                )
            }
        }
    }

    /**
     * Pulls Exam Marks from multi-tab sheet layout (`class-test1`, `term-exam1`, etc.)
     * This pushes missing exams and updates marks.
     */
    private suspend fun syncExamMarks(classroomId: Long, spreadsheet: Spreadsheet, authHeader: String) {
        val students = database.studentDao().getStudentsByClassroomOnce(classroomId)
        if (students.isEmpty()) return

        // 1. Identify sheets matching exam prefix templates
        val examCategories = ExamCategory.entries
        val examSheets = spreadsheet.sheets?.filter { sheet ->
            val title = sheet.properties.title
            examCategories.any { cat -> title.startsWith(cat.sheetPrefix, ignoreCase = true) }
        } ?: return

        for (sheet in examSheets) {
            val title = sheet.properties.title
            val category = examCategories.find { title.startsWith(it.sheetPrefix, ignoreCase = true) } ?: continue

            val range = "'$title'!A1:Z1000"
            val response = try {
                api.getSheetValues(spreadsheet.spreadsheetId, range, authHeader)
            } catch (e: Exception) { continue }

            val rows = response.values ?: continue
            if (rows.size < 2) continue // Need at least header + 1 row

            // First row might contain config: "Date: 2024-10-10", "Full Marks: 100", "Pass: 40"
            val configRow = rows[0]
            val headers = if (rows.size > 1) rows[1] else return

            val dateStr = configRow.find { it.toString().startsWith("Date:", ignoreCase = true) }?.toString()?.substringAfter(":")?.trim() ?: "2024-01-01"
            val fullMarks = configRow.find { it.toString().startsWith("Full Marks:", ignoreCase = true) }?.toString()?.substringAfter(":")?.trim()?.toDoubleOrNull() ?: 100.0
            val passMarks = configRow.find { it.toString().startsWith("Pass Marks:", ignoreCase = true) }?.toString()?.substringAfter(":")?.trim()?.toDoubleOrNull() ?: 40.0
            val topic = configRow.find { it.toString().startsWith("Topic:", ignoreCase = true) }?.toString()?.substringAfter(":")?.trim() ?: ""

            // Find or Create ExamEntity
            val existingExams = database.examDao().getExamsForClassroomOnce(classroomId)
            var exam = existingExams.find { it.title.equals(title, ignoreCase = true) && it.category == category }
            
            if (exam == null) {
                val newExam = ExamEntity(
                    classroomId = classroomId,
                    category = category,
                    title = title,
                    topicOrChapter = topic,
                    fullMarks = fullMarks,
                    passMarks = passMarks,
                    date = dateStr
                )
                val id = database.examDao().insertExam(newExam)
                exam = newExam.copy(id = id)
            } else {
                // Update config if changed
                if (exam.fullMarks != fullMarks || exam.passMarks != passMarks || exam.date != dateStr || exam.topicOrChapter != topic) {
                    exam = exam.copy(fullMarks = fullMarks, passMarks = passMarks, date = dateStr, topicOrChapter = topic)
                    database.examDao().updateExam(exam)
                }
            }

            // Sync Marks
            val existingMarks = database.examDao().getMarksForExamOnce(exam.id)
            val studentIdIdx = headers.indexOfFirst { it.toString().equals("Student ID", ignoreCase = true) }
            val nameIdx = headers.indexOfFirst { it.toString().equals("Name", ignoreCase = true) }
            val markIdx = headers.indexOfFirst { it.toString().equals("Marks Obtained", ignoreCase = true) }
            val absentIdx = headers.indexOfFirst { it.toString().equals("Absent", ignoreCase = true) }

            for (i in 2 until rows.size) {
                val row = rows[i]
                if (row.isEmpty()) continue

                val studentIdStr = if (studentIdIdx >= 0) row.getOrNull(studentIdIdx)?.toString() ?: "" else ""
                val nameStr = if (nameIdx >= 0) row.getOrNull(nameIdx)?.toString() ?: "" else ""
                val student = students.find {
                    (studentIdStr.isNotBlank() && it.studentNumber == studentIdStr) ||
                    (nameStr.isNotBlank() && it.name.equals(nameStr, ignoreCase = true))
                } ?: continue

                val isAbsent = if (absentIdx >= 0) row.getOrNull(absentIdx)?.toString()?.trim()?.equals("yes", ignoreCase = true) == true else false
                val marksStr = if (markIdx >= 0) row.getOrNull(markIdx)?.toString()?.trim() ?: "" else ""
                val marksVal = marksStr.toDoubleOrNull()

                val markEntity = existingMarks.find { it.studentId == student.id }
                if (markEntity != null) {
                    if (markEntity.marksObtained != marksVal || markEntity.isAbsent != isAbsent) {
                        database.examDao().updateMark(markEntity.copy(marksObtained = marksVal, isAbsent = isAbsent))
                    }
                } else {
                    database.examDao().insertMark(
                        ExamMarkEntity(
                            examId = exam.id,
                            studentId = student.id,
                            marksObtained = marksVal,
                            isAbsent = isAbsent
                        )
                    )
                }
            }
        }
    }

    suspend fun exportToSheets(classroomId: Long, spreadsheetId: String): String = withContext(Dispatchers.IO) {
        val authHeader = "Bearer $accessToken"
        val students = database.studentDao().getStudentsByClassroomOnce(classroomId).sortedBy { it.name }
        if (students.isEmpty()) return@withContext "No students to export."

        // Fetch sheet metadata and auto-create missing required tabs
        val spreadsheet = try {
            api.getSpreadsheet(spreadsheetId, authHeader)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get spreadsheet metadata", e)
            return@withContext "Export failed: Cannot access Google Sheet."
        }

        val existingTitles = spreadsheet.sheets?.map { it.properties.title }?.toSet() ?: emptySet()
        val requiredSheets = listOf("Attendance", "Homework", "Grades", "Interventions", "Discipline_Log")
        val missingSheets = requiredSheets.filter { !existingTitles.contains(it) }

        if (missingSheets.isNotEmpty()) {
            try {
                val requests = missingSheets.map { title ->
                    Request(addSheet = AddSheetRequest(properties = SheetProperties(title = title)))
                }
                api.batchUpdate(spreadsheetId, authHeader, BatchUpdateSpreadsheetRequest(requests))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to auto-create missing tabs, attempting export anyway: ${e.message}")
            }
        }

        try {
            // 1. Export Attendance
            val attendanceRecords = database.attendanceDao().getAttendanceForClassroomOnce(classroomId)
            val distinctDates = attendanceRecords.map { it.date }.distinct().sorted()

            val attendanceRows = mutableListOf<List<String>>()
            val attHeaderRow = mutableListOf("Student Name", "Student ID")
            attHeaderRow.addAll(distinctDates)
            attendanceRows.add(attHeaderRow)

            for (student in students) {
                val row = mutableListOf(student.name, student.studentNumber)
                val studentAtt = attendanceRecords.filter { it.studentId == student.id }
                for (date in distinctDates) {
                    val record = studentAtt.find { it.date == date }
                    row.add(record?.status?.displayName ?: "")
                }
                attendanceRows.add(row)
            }

            api.updateSheetValues(
                spreadsheetId = spreadsheetId,
                range = "Attendance!A1",
                authHeader = authHeader,
                body = ValueRange("Attendance!A1", "ROWS", attendanceRows)
            )

            // 2. Export Homework
            val homeworkRecords = database.homeworkRecordDao().getAllHomeworkRecordsForClassroomOnce(classroomId)
            val distinctHomeworkTasks = homeworkRecords.map { "${it.date} - ${it.topic}" }.distinct().sorted()

            val homeworkRows = mutableListOf<List<String>>()
            val hwHeaderRow = mutableListOf("Student Name", "Student ID")
            hwHeaderRow.addAll(distinctHomeworkTasks)
            homeworkRows.add(hwHeaderRow)

            for (student in students) {
                val row = mutableListOf(student.name, student.studentNumber)
                val studentHw = homeworkRecords.filter { it.studentId == student.id }
                for (task in distinctHomeworkTasks) {
                    val parts = task.split(" - ", limit = 2)
                    val date = parts.getOrNull(0) ?: ""
                    val topic = parts.getOrNull(1) ?: ""
                    val record = studentHw.find { it.date == date && it.topic.equals(topic, ignoreCase = true) }
                    row.add(record?.status?.displayName ?: "")
                }
                homeworkRows.add(row)
            }

            api.updateSheetValues(
                spreadsheetId = spreadsheetId,
                range = "Homework!A1",
                authHeader = authHeader,
                body = ValueRange("Homework!A1", "ROWS", homeworkRows)
            )

            // 3. Export Grades
            val assignments = database.assignmentDao().getAssignmentsByClassroomOnce(classroomId)
            val submissions = database.submissionDao().getSubmissionsByClassroomOnce(classroomId)

            val gradesRows = mutableListOf<List<String>>()
            val gradeHeaderRow = mutableListOf("Student Name", "Student ID")
            assignments.forEach { gradeHeaderRow.add("${it.title} (${it.maxPoints} pts)") }
            gradesRows.add(gradeHeaderRow)

            for (student in students) {
                val row = mutableListOf(student.name, student.studentNumber)
                val studentSubs = submissions.filter { it.studentId == student.id }
                for (assignment in assignments) {
                    val sub = studentSubs.find { it.assignmentId == assignment.id }
                    if (sub?.status == SubmissionStatus.GRADED && sub.score != null) {
                        row.add(sub.score.toString())
                    } else if (sub?.status == SubmissionStatus.MISSING) {
                        row.add("MISSING")
                    } else if (sub?.status?.name == "EXCUSED") {
                        row.add("EXCUSED")
                    } else {
                        row.add("")
                    }
                }
                gradesRows.add(row)
            }

            api.updateSheetValues(
                spreadsheetId = spreadsheetId,
                range = "Grades!A1",
                authHeader = authHeader,
                body = ValueRange("Grades!A1", "ROWS", gradesRows)
            )

            // 4. Export Interventions
            val interventions = database.interventionDao().getInterventionsByClassroomOnce(classroomId)
            val interventionRows = mutableListOf<List<String>>()
            val intHeaderRow = listOf("Student Name", "Student ID", "Date", "Type", "Title", "Notes", "Resolved")
            interventionRows.add(intHeaderRow)

            for (intervention in interventions) {
                val student = students.find { it.id == intervention.studentId }
                val row = listOf(
                    student?.name ?: "Unknown",
                    student?.studentNumber ?: "",
                    intervention.date,
                    intervention.type.displayName,
                    intervention.title,
                    intervention.notes,
                    if (intervention.resolved) "Yes" else "No"
                )
                interventionRows.add(row)
            }

            api.updateSheetValues(
                spreadsheetId = spreadsheetId,
                range = "Interventions!A1",
                authHeader = authHeader,
                body = ValueRange("Interventions!A1", "ROWS", interventionRows)
            )

            // 5. Export Discipline Log
            val disciplineRecords = database.disciplineDao().getDisciplineRecordsByClassroomOnce(classroomId)
            val disciplineRows = mutableListOf<List<String>>()
            val discHeaderRow = listOf("Student Name", "Student ID", "Date", "Category", "Severity", "Title", "Description", "Action Taken", "Parent Notified", "Resolved")
            disciplineRows.add(discHeaderRow)

            for (record in disciplineRecords) {
                val student = students.find { it.id == record.studentId }
                val row = listOf(
                    student?.name ?: "Unknown",
                    student?.studentNumber ?: "",
                    record.date,
                    record.category.displayName,
                    record.severity.displayName,
                    record.title,
                    record.description,
                    record.actionTaken,
                    if (record.parentNotified) "Yes" else "No",
                    if (record.resolved) "Yes" else "No"
                )
                disciplineRows.add(row)
            }

            api.updateSheetValues(
                spreadsheetId = spreadsheetId,
                range = "Discipline_Log!A1",
                authHeader = authHeader,
                body = ValueRange("Discipline_Log!A1", "ROWS", disciplineRows)
            )

            // 6. Export Exams & Marks
            val exams = database.examDao().getExamsForClassroomOnce(classroomId)
            for (exam in exams) {
                val sheetTitle = exam.title.ifBlank { "${exam.category.sheetPrefix}-${exam.id}" }
                val examMarks = database.examDao().getMarksForExamOnce(exam.id)
                val examRows = mutableListOf<List<String>>()

                // Config Header Row
                val configRow = listOf(
                    "Date: ${exam.date}",
                    "Full Marks: ${exam.fullMarks.toInt()}",
                    "Pass Marks: ${exam.passMarks.toInt()}",
                    "Topic: ${exam.topicOrChapter}"
                )
                examRows.add(configRow)

                // Table Header Row
                val tableHeaderRow = listOf("Student Name", "Student ID", "Marks Obtained", "Absent")
                examRows.add(tableHeaderRow)

                for (student in students) {
                    val markEntry = examMarks.find { it.studentId == student.id }
                    val marksStr = markEntry?.marksObtained?.toString() ?: ""
                    val absentStr = if (markEntry?.isAbsent == true) "YES" else "NO"
                    examRows.add(listOf(student.name, student.studentNumber, marksStr, absentStr))
                }

                try {
                    api.updateSheetValues(
                        spreadsheetId = spreadsheetId,
                        range = "'$sheetTitle'!A1",
                        authHeader = authHeader,
                        body = ValueRange("'$sheetTitle'!A1", "ROWS", examRows)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to push exam '$sheetTitle'", e)
                }
            }

            return@withContext "Sync & Export successful."
        } catch (e: Exception) {
            Log.e(TAG, "Export error", e)
            return@withContext "Export failed: ${e.message}"
        }
    }
}
