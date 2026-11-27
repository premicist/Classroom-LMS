package com.aistudio.classroomlms.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aistudio.classroomlms.data.entity.SchoolEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {
    @Query("SELECT * FROM schools ORDER BY createdAt ASC")
    fun getAllSchools(): Flow<List<SchoolEntity>>

    @Query("SELECT * FROM schools ORDER BY createdAt ASC")
    fun getAllSchoolsOnce(): List<SchoolEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchool(school: SchoolEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSchoolsSync(schools: List<SchoolEntity>)

    @Update
    suspend fun updateSchool(school: SchoolEntity)

    @Delete
    suspend fun deleteSchool(school: SchoolEntity)
}
