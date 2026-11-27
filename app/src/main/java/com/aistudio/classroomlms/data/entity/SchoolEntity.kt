package com.aistudio.classroomlms.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class InstitutionType(val displayName: String) {
    SCHOOL("School"),
    COLLEGE("College"),
    INSTITUTE("Institute")
}

@Entity(tableName = "schools")
data class SchoolEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val institutionType: InstitutionType = InstitutionType.SCHOOL,
    val address: String = "",
    val colorHex: Long = 0xFF1E40AF,
    val createdAt: Long = System.currentTimeMillis()
)
