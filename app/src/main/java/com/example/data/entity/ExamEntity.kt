package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ExamCategory(val displayName: String, val sheetPrefix: String) {
    CLASS_TEST("Class Test", "class-test"),
    TERMINAL("Terminal", "term-exam"),
    PRE_BOARD("Pre-Board", "pre-board")
}

@Entity(
    tableName = "exams",
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
data class ExamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val classroomId: Long,
    val category: ExamCategory,
    val title: String, // Exam Name
    val topicOrChapter: String = "", // Optional
    val fullMarks: Double,
    val passMarks: Double,
    val date: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "exam_marks",
    foreignKeys = [
        ForeignKey(
            entity = ExamEntity::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["examId"]), Index(value = ["studentId"])]
)
data class ExamMarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val examId: Long,
    val studentId: Long,
    val marksObtained: Double?, // Null if absent or not yet graded
    val remarks: String = "",
    val isAbsent: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
