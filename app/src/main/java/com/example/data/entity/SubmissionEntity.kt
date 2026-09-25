package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SubmissionStatus(val displayName: String) {
    PENDING("Pending"),
    SUBMITTED("Submitted"),
    LATE("Late"),
    GRADED("Graded"),
    MISSING("Missing")
}

@Entity(
    tableName = "submissions",
    foreignKeys = [
        ForeignKey(
            entity = AssignmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["assignmentId"],
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
        Index(value = ["assignmentId"]),
        Index(value = ["studentId"]),
        Index(value = ["assignmentId", "studentId"], unique = true)
    ]
)
data class SubmissionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val assignmentId: Long,
    val studentId: Long,
    val classroomId: Long,
    val status: SubmissionStatus = SubmissionStatus.PENDING,
    val score: Double? = null,
    val submittedDate: String? = null, // YYYY-MM-DD HH:mm
    val feedback: String = "",
    val isChecked: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
