package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AttendanceStatus(val displayName: String, val shortCode: String) {
    PRESENT("Present", "P"),
    ABSENT("Absent", "A"),
    LATE("Late", "L"),
    EXCUSED("Excused", "E")
}

@Entity(
    tableName = "attendance_records",
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
        Index(value = ["classroomId", "studentId", "date"], unique = true)
    ]
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val classroomId: Long,
    val studentId: Long,
    val date: String, // YYYY-MM-DD
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val remarks: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
