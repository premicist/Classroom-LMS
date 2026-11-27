package com.aistudio.classroomlms.fake

import com.aistudio.classroomlms.data.dao.ClassroomActivityDao
import com.aistudio.classroomlms.data.entity.ClassroomActivityEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeClassroomActivityDao : ClassroomActivityDao {
    private val activitiesFlow = MutableStateFlow<List<ClassroomActivityEntity>>(emptyList())
    private var nextId = 1L

    override fun getActivitiesForClassroom(classroomId: Long): Flow<List<ClassroomActivityEntity>> =
        activitiesFlow.map { list -> list.filter { it.classroomId == classroomId } }

    override fun getActivitiesForStudent(studentId: Long): Flow<List<ClassroomActivityEntity>> =
        activitiesFlow.map { list ->
            list.filter {
                it.firstWinnerStudentId == studentId ||
                    it.secondWinnerStudentId == studentId ||
                    it.thirdWinnerStudentId == studentId
            }
        }

    override fun getAllActivitiesOnce(): List<ClassroomActivityEntity> = activitiesFlow.value

    override suspend fun insertActivity(activity: ClassroomActivityEntity): Long {
        val inserted = activity.copy(id = if (activity.id == 0L) nextId++ else activity.id)
        activitiesFlow.update { it + inserted }
        return inserted.id
    }

    override fun insertActivitiesSync(activities: List<ClassroomActivityEntity>) {
        activitiesFlow.update { it + activities }
    }

    override suspend fun deleteActivity(activity: ClassroomActivityEntity) {
        activitiesFlow.update { list -> list.filter { it.id != activity.id } }
    }
}
