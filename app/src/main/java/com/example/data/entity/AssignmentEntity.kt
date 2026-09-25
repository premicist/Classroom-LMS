package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AssignmentType(val displayName: String, val weightDefault: Double) {
    HOMEWORK("Homework", 20.0),
    QUIZ("Quiz", 25.0),
    PROJECT("Project", 25.0),
    EXAM("Exam", 30.0)
}

@Entity(
    tableName = "assignments",
    foreignKeys = [
        ForeignKey(
            entity = ClassroomEntity::class,
            parentColumns = ["id"],
            childColumns = ["classroomId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["classroomId"])]
)
data class AssignmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val classroomId: Long,
    val title: String,
    val description: String = "",
    val type: AssignmentType = AssignmentType.HOMEWORK,
    val maxPoints: Double = 100.0,
    val weightPercentage: Double = 25.0,
    val dueDate: String, // YYYY-MM-DD
    val assignedDate: String = "", // YYYY-MM-DD
    val isGraded: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
