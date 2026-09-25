package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class HomeworkStatus(val displayName: String, val scoreMultiplier: Double) {
    DONE("Done", 1.0),
    PARTIAL("Partial", 0.5),
    MISSING("Missing", 0.0),
    EXCUSED("Excused", 1.0)
}

@Entity(
    tableName = "homework_records",
    foreignKeys = [
        ForeignKey(
            entity = ClassroomEntity::class,
            parentColumns = ["id"],
            childColumns = ["classroomId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["classroomId"]),
        Index(value = ["studentId"]),
        Index(value = ["classroomId", "studentId", "date", "topic"], unique = true)
    ]
)
data class HomeworkRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val classroomId: Long,
    val studentId: Long,
    val date: String, // YYYY-MM-DD
    val topic: String,
    val status: HomeworkStatus = HomeworkStatus.DONE,
    val notes: String = "",
    val checkedAt: Long = System.currentTimeMillis()
)
