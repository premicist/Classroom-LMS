package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "class_schedules",
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
        Index(value = ["classroomId", "weekNumber", "dayOfWeek"])
    ]
)
data class ClassScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val classroomId: Long,
    val weekNumber: Int, // 1 to 5
    val dayOfWeek: String, // MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    val classroomName: String,
    val startTime: String, // e.g. "09:00 AM"
    val endTime: String = "", // e.g. "10:30 AM"
    val startMinutes: Int = 0, // Minutes from midnight (e.g., 540 for 09:00)
    val endMinutes: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
