package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "students",
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
data class StudentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val classroomId: Long,
    val name: String,
    val studentNumber: String,
    val email: String = "",
    val guardianContact: String = "",
    val avatarColorHex: Long = 0xFF3B82F6,
    val photoPath: String? = null,
    val notes: String = "",
    val customAttributes: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
)
