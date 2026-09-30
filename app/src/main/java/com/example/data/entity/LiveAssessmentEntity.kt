package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class LiveAssessmentTaskType(val displayName: String, val iconEmoji: String) {
    DIAGRAM("Diagram / Graph Drawing", "📐"),
    NUMERICAL("Numerical / Derivation", "🔢"),
    CONCEPT_EXPLANATION("Concept Explanation", "💡"),
    LAB_PRACTICAL("Lab / Practical Experiment", "🧪"),
    READING_ORAL("Reading & Pronunciation", "📖"),
    PEER_PROBLEM_SOLVING("Peer Problem Solving", "🤝"),
    QUICK_QUIZ_QA("Live Q&A / Rapid Fire", "⚡")
}

enum class MasteryLevel(val points: Double, val maxPoints: Double = 3.0, val displayName: String, val shortLabel: String) {
    MASTERED(3.0, 3.0, "Mastered 🌟", "Mastered"),
    DEVELOPING(2.0, 3.0, "Developing 📈", "Developing"),
    NEEDS_SUPPORT(1.0, 3.0, "Needs Support 🛠️", "Needs Support")
}

@Entity(
    tableName = "live_assessments",
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
        Index("studentId"),
        Index("classroomId"),
        Index("date")
    ]
)
data class LiveAssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val classroomId: Long,
    val date: String, // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val taskType: LiveAssessmentTaskType = LiveAssessmentTaskType.CONCEPT_EXPLANATION,
    val topic: String, // e.g. "Newton's 3rd Law", "Quadratic Formula"
    val masteryLevel: MasteryLevel = MasteryLevel.MASTERED,
    val diagnosticTags: String = "", // Comma-separated tags e.g. "Clear Steps, Calculation Slip"
    val remarks: String = "",
    val photoEvidencePath: String? = null,
    val includeInCasGrade: Boolean = true
)
