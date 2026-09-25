package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classrooms")
data class ClassroomEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val subject: String,
    val gradeLevel: String,
    val roomNumber: String,
    val scheduleInfo: String,
    val colorHex: Long = 0xFF2563EB, // default vibrant blue
    val iconName: String = "SCHOOL",
    val academicYear: String = "2025-2026",
    val createdAt: Long = System.currentTimeMillis()
)
