package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class InterventionType(val displayName: String) {
    TUTORING("1-on-1 Tutoring"),
    PARENT_CONTACT("Parent Contact"),
    EXTRA_CREDIT("Remedial / Extra Credit"),
    BEHAVIOR_NOTE("Teacher Observation"),
    ENRICHMENT("Enrichment Challenge"),
    ACCOMMODATION("Learning Support")
}

@Entity(
    tableName = "interventions",
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
data class InterventionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val classroomId: Long,
    val date: String, // YYYY-MM-DD
    val type: InterventionType = InterventionType.TUTORING,
    val title: String,
    val notes: String = "",
    val resolved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
