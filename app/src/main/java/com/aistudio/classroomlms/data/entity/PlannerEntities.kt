package com.aistudio.classroomlms.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lesson_plans",
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
data class LessonPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val classroomId: Long,
    val unitTitle: String,
    val description: String,
    val targetDate: Long, // epoch millis
    val status: String = "PLANNED", // PLANNED, IN_PROGRESS, COMPLETED
    // Reminder fields
    val reminderEnabled: Boolean = false,
    val reminderTime: Long = 0, // epoch millis when reminder should trigger
    val reminderId: String = "", // unique identifier for the reminder
    // Optional structured unit fields
    val unitNumber: String = "",
    val allocatedHours: Double = 0.0,
    val unitLearningOutcomes: String = ""
)

@Entity(
    tableName = "lesson_plan_sessions",
    foreignKeys = [
        ForeignKey(
            entity = LessonPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonPlanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["lessonPlanId"]),
        Index(value = ["linkedAssignmentId"])
    ]
)
data class LessonPlanSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lessonPlanId: Long,
    val sessionOrder: Int = 1,
    val learningOutcome: String = "",
    val content: String = "",
    val lecturerActivity: String = "",
    val studentActivity: String = "",
    val teachingAids: String = "",
    val evaluationType: String = "NOTE", // NOTE, ASSIGNMENT, LIVE_ASSESSMENT
    val evaluationNote: String = "",
    val linkedAssignmentId: Long? = null,
    val hours: Double = 1.0,
    val isClosingSession: Boolean = false
)

@Entity(
    tableName = "daily_logs",
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
data class DailyLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val classroomId: Long,
    val date: Long,
    val reflectionNotes: String,
    val wasProxyClass: Boolean = false
)