package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class BehaviorCategory(val displayName: String, val isPositive: Boolean) {
    PRAISE_MERIT("Praise / Merit", true),
    CLASSROOM_DISRUPTION("Classroom Disruption", false),
    ACADEMIC_DISHONESTY("Academic Dishonesty", false),
    UNPREPARED("Unprepared for Class", false),
    OTHER("Other / Note", false)
}

enum class BehaviorSeverity(val displayName: String) {
    LOW_WARNING("Low / Warning"),
    MEDIUM_DETENTION("Medium / Detention"),
    HIGH_ADMIN_REFERRAL("High / Admin Referral")
}

@Entity(
    tableName = "discipline_records",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ClassroomEntity::class,
            parentColumns = ["id"],
            childColumns = ["classroomId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["classroomId"])
    ]
)
data class DisciplineRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val classroomId: Long,
    val date: String, // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val category: BehaviorCategory = BehaviorCategory.CLASSROOM_DISRUPTION,
    val severity: BehaviorSeverity = BehaviorSeverity.LOW_WARNING,
    val title: String,
    val description: String = "",
    val actionTaken: String = "",
    val parentNotified: Boolean = false,
    val resolved: Boolean = false
)
