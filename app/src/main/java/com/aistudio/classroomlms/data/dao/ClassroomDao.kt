package com.aistudio.classroomlms.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aistudio.classroomlms.data.entity.ClassroomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassroomDao {
    @Query("SELECT * FROM classrooms WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun getAllClassrooms(): Flow<List<ClassroomEntity>>

    @Query("SELECT * FROM classrooms WHERE isArchived = 0 ORDER BY createdAt DESC")
    suspend fun getAllClassroomsOnce(): List<ClassroomEntity>

    @Query("SELECT * FROM classrooms WHERE isArchived = 1 ORDER BY createdAt DESC")
    fun getArchivedClassrooms(): Flow<List<ClassroomEntity>>

    @Query("SELECT * FROM classrooms ORDER BY createdAt DESC")
    suspend fun getAllClassroomsIncludingArchived(): List<ClassroomEntity>

    @Query("SELECT * FROM classrooms WHERE id = :id")
    fun getClassroomById(id: Long): Flow<ClassroomEntity?>

    @Query("SELECT * FROM classrooms WHERE id = :id")
    suspend fun getClassroomByIdOnce(id: Long): ClassroomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassroom(classroom: ClassroomEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertClassroomsSync(classrooms: List<ClassroomEntity>)

    @Update
    suspend fun updateClassroom(classroom: ClassroomEntity)

    @Delete
    suspend fun deleteClassroom(classroom: ClassroomEntity)

    @Query("DELETE FROM classrooms WHERE id = :id")
    suspend fun deleteClassroomById(id: Long)
}
