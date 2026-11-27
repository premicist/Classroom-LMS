package com.aistudio.classroomlms.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AcademicEventCategory(val displayName: String, val emoji: String) {
    HOLIDAY("Public Holiday", "🏖️"),
    FESTIVAL_BREAK("Festival Break", "🪔"),
    EXAM_WINDOW("Exam Window", "📝"),
    SCHOOL_EVENT("School Event", "🎓")
}

@Entity(
    tableName = "academic_events",
    indices = [
        Index(value = ["startDate"]),
        Index(value = ["category"])
    ]
)
data class AcademicEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: AcademicEventCategory = AcademicEventCategory.HOLIDAY,
    val startDate: String, // YYYY-MM-DD
    val endDate: String,   // YYYY-MM-DD
    val notes: String = "",
    val academicYearLabel: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
