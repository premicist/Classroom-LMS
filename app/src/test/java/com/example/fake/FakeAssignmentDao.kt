package com.example.fake

import com.example.data.dao.AssignmentDao
import com.example.data.entity.AssignmentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeAssignmentDao : AssignmentDao {
    val assignmentsFlow = MutableStateFlow<List<AssignmentEntity>>(emptyList())
    private var nextId = 1L

    override fun getAllAssignmentsOnce(): List<AssignmentEntity> {
        return assignmentsFlow.value
    }

    override fun getAssignmentsByClassroom(classroomId: Long): Flow<List<AssignmentEntity>> {
        return assignmentsFlow.map { list -> list.filter { it.classroomId == classroomId } }
    }

    override suspend fun getAssignmentsByClassroomOnce(classroomId: Long): List<AssignmentEntity> {
        return assignmentsFlow.value.filter { it.classroomId == classroomId }
    }

    override fun getAssignmentById(id: Long): Flow<AssignmentEntity?> {
        return assignmentsFlow.map { list -> list.find { it.id == id } }
    }

    override suspend fun getAssignmentByIdOnce(id: Long): AssignmentEntity? {
        return assignmentsFlow.value.find { it.id == id }
    }

    override suspend fun insertAssignment(assignment: AssignmentEntity): Long {
        val idToUse = if (assignment.id == 0L) nextId++ else assignment.id
        val item = assignment.copy(id = idToUse)
        assignmentsFlow.update { current ->
            val idx = current.indexOfFirst { it.id == idToUse }
            if (idx >= 0) current.toMutableList().apply { set(idx, item) } else current + item
        }
        return idToUse
    }

    override suspend fun insertAssignments(assignments: List<AssignmentEntity>): List<Long> {
        return assignments.map { insertAssignment(it) }
    }

    override fun insertAssignmentsSync(assignments: List<AssignmentEntity>) {
        assignments.forEach { a ->
            val idToUse = if (a.id == 0L) nextId++ else a.id
            val item = a.copy(id = idToUse)
            assignmentsFlow.update { current ->
                val idx = current.indexOfFirst { it.id == idToUse }
                if (idx >= 0) current.toMutableList().apply { set(idx, item) } else current + item
            }
        }
    }

    override suspend fun updateAssignment(assignment: AssignmentEntity) {
        insertAssignment(assignment)
    }

    override suspend fun deleteAssignment(assignment: AssignmentEntity) {
        assignmentsFlow.update { current -> current.filter { it.id != assignment.id } }
    }

    override suspend fun deleteAssignmentById(id: Long) {
        assignmentsFlow.update { current -> current.filter { it.id != id } }
    }
}
