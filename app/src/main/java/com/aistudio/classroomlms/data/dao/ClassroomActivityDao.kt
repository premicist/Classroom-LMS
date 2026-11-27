package com.aistudio.classroomlms.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aistudio.classroomlms.data.entity.ClassroomActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassroomActivityDao {
    @Query("SELECT * FROM classroom_activities WHERE classroomId = :classroomId ORDER BY date DESC, createdAt DESC")
    fun getActivitiesForClassroom(classroomId: Long): Flow<List<ClassroomActivityEntity>>

    @Query(
        """
        SELECT * FROM classroom_activities
        WHERE firstWinnerStudentId = :studentId
           OR secondWinnerStudentId = :studentId
           OR thirdWinnerStudentId = :studentId
        ORDER BY date DESC, createdAt DESC
        """
    )
    fun getActivitiesForStudent(studentId: Long): Flow<List<ClassroomActivityEntity>>

    @Query("SELECT * FROM classroom_activities ORDER BY date DESC, createdAt DESC")
    fun getAllActivitiesOnce(): List<ClassroomActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ClassroomActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertActivitiesSync(activities: List<ClassroomActivityEntity>)

    @Delete
    suspend fun deleteActivity(activity: ClassroomActivityEntity)
}
