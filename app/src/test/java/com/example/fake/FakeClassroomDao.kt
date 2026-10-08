package com.example.fake

import com.example.data.dao.ClassroomDao
import com.example.data.entity.ClassroomEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeClassroomDao : ClassroomDao {
    val classroomsFlow = MutableStateFlow<List<ClassroomEntity>>(emptyList())
    private var nextId = 1L

    override fun getAllClassrooms(): Flow<List<ClassroomEntity>> {
        return classroomsFlow
    }

    override suspend fun getAllClassroomsOnce(): List<ClassroomEntity> {
        return classroomsFlow.value
    }

    override fun getClassroomById(id: Long): Flow<ClassroomEntity?> {
        return classroomsFlow.map { list -> list.find { it.id == id } }
    }

    override suspend fun getClassroomByIdOnce(id: Long): ClassroomEntity? {
        return classroomsFlow.value.find { it.id == id }
    }

    override suspend fun insertClassroom(classroom: ClassroomEntity): Long {
        val idToUse = if (classroom.id == 0L) nextId++ else classroom.id
        val item = classroom.copy(id = idToUse)
        classroomsFlow.update { current ->
            val idx = current.indexOfFirst { it.id == idToUse }
            if (idx >= 0) current.toMutableList().apply { set(idx, item) } else current + item
        }
        return idToUse
    }

    override fun insertClassroomsSync(classrooms: List<ClassroomEntity>) {
        classrooms.forEach { c ->
            val idToUse = if (c.id == 0L) nextId++ else c.id
            val item = c.copy(id = idToUse)
            classroomsFlow.update { current ->
                val idx = current.indexOfFirst { it.id == idToUse }
                if (idx >= 0) current.toMutableList().apply { set(idx, item) } else current + item
            }
        }
    }

    override suspend fun updateClassroom(classroom: ClassroomEntity) {
        insertClassroom(classroom)
    }

    override suspend fun deleteClassroom(classroom: ClassroomEntity) {
        classroomsFlow.update { current -> current.filter { it.id != classroom.id } }
    }

    override suspend fun deleteClassroomById(id: Long) {
        classroomsFlow.update { current -> current.filter { it.id != id } }
    }
}
