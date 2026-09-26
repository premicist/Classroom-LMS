package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.entity.StudentEntity
import com.example.data.network.GoogleSheetsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

import android.util.Log

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

    suspend fun syncClassroomRoster(classroomId: Long, spreadsheetId: String): String = withContext(Dispatchers.IO) {
        val authHeader = "Bearer $accessToken"
        
        // Fetch metadata to find the first sheet (usually Sheet1, which is the Roster)
        val spreadsheet = api.getSpreadsheet(spreadsheetId, authHeader)
        val rosterSheetTitle = spreadsheet.sheets.firstOrNull()?.properties?.title ?: "Sheet1"
        
        val range = "$rosterSheetTitle!A:Z"
        val response = try {
            api.getSheetValues(spreadsheetId, range, authHeader)
        } catch (e: Exception) {
            return@withContext "Failed to read sheet. Ensure it is not empty."
        }
        
        val rows = response.values ?: return@withContext "Sheet is empty."
        if (rows.isEmpty()) return@withContext "Sheet is empty."
            
        // Assume Row 0 is headers
        val headers = rows[0]
        val studentNumberIndex = headers.indexOfFirst { it.equals("Student Number", ignoreCase = true) || it.equals("Student ID", ignoreCase = true) }
        val nameIndex = headers.indexOfFirst { it.equals("Name", ignoreCase = true) || it.equals("Student Name", ignoreCase = true) || it.equals("First Name", ignoreCase = true) }
        val emailIndex = headers.indexOfFirst { it.equals("Email", ignoreCase = true) }

        if (nameIndex == -1) {
            return@withContext "Sync failed: Could not find a 'Name' column in row 1."
        }

        val parsedStudents = mutableListOf<StudentEntity>()
        
        for (i in 1 until rows.size) {
            val row = rows[i]
            if (row.isEmpty()) continue
            
            // Extract core fields if columns were found
            val studentNumber = if (studentNumberIndex >= 0) row.getOrNull(studentNumberIndex) ?: "" else ""
            val name = if (nameIndex >= 0) row.getOrNull(nameIndex) ?: "" else ""
            val email = if (emailIndex >= 0) row.getOrNull(emailIndex) ?: "" else ""
            
            if (name.isBlank()) {
                continue // Require at least a name
            }
            
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
        
        if (parsedStudents.isEmpty()) {
            return@withContext "No students found to sync from the sheet."
        }

        // Smart Sync (Upsert) - DO NOT DELETE ANYONE
        val existingStudents = database.studentDao().getStudentsByClassroomOnce(classroomId)
        
        var addedCount = 0
        var updatedCount = 0
        
        for (parsed in parsedStudents) {
            // Match by Student ID, or fallback to exact Name match
            val existing = existingStudents.find { 
                (it.studentNumber.isNotBlank() && it.studentNumber == parsed.studentNumber) || 
                (it.name.equals(parsed.name, ignoreCase = true)) 
            }
            
            if (existing != null) {
                // Did anything change?
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

        if (addedCount == 0 && updatedCount == 0) {
            return@withContext "Sync is up to date (no changes found)."
        } else {
            return@withContext "Sync successful: Added $addedCount, Updated $updatedCount."
        }
    }
}