package com.aistudio.classroomlms.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ClassroomActivityCategory(val displayName: String, val emoji: String) {
    EDUCATIONAL_GAME("Educational Game", "🎮"),
    QUIZ_RAPID_FIRE("Rapid-Fire Quiz", "⚡"),
    DEBATE_DISCUSSION("Debate / Discussion", "🗣️"),
    GROUP_CHALLENGE("Group Challenge", "🤝"),
    CREATIVE_TASK("Creative / Board Task", "🎨")
}

@Entity(
    tableName = "classroom_activities",
    foreignKeys = [
        ForeignKey(
            entity = ClassroomEntity::class,
            parentColumns = ["id"],
            childColumns = ["classroomId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["classroomId"]),
        Index(value = ["date"])
    ]
)
data class ClassroomActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val classroomId: Long,
    val title: String,
    val category: ClassroomActivityCategory = ClassroomActivityCategory.EDUCATIONAL_GAME,
    val date: String, // YYYY-MM-DD
    val durationSeconds: Int = 180,
    val firstWinnerStudentId: Long? = null,
    val secondWinnerStudentId: Long? = null,
    val thirdWinnerStudentId: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
