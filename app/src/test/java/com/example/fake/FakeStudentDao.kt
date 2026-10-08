package com.example.fake

import com.example.data.dao.StudentDao
import com.example.data.entity.StudentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeStudentDao : StudentDao {
    val studentsFlow = MutableStateFlow<List<StudentEntity>>(emptyList())
    private var nextId = 1L

    override fun getStudentsByClassroom(classroomId: Long): Flow<List<StudentEntity>> {
        return studentsFlow.map { list -> list.filter { it.classroomId == classroomId } }
    }

    override suspend fun getStudentsByClassroomOnce(classroomId: Long): List<StudentEntity> {
        return studentsFlow.value.filter { it.classroomId == classroomId }
    }

    override fun getAllStudents(): Flow<List<StudentEntity>> {
        return studentsFlow
    }

    override fun getAllStudentsOnce(): List<StudentEntity> {
        return studentsFlow.value
    }

    override fun getStudentById(id: Long): Flow<StudentEntity?> {
        return studentsFlow.map { list -> list.find { it.id == id } }
    }

    override suspend fun insertStudent(student: StudentEntity): Long {
        val idToUse = if (student.id == 0L) nextId++ else student.id
        val studentWithId = student.copy(id = idToUse)
        studentsFlow.update { current ->
            val idx = current.indexOfFirst { it.id == idToUse }
            if (idx >= 0) {
                current.toMutableList().apply { set(idx, studentWithId) }
            } else {
                current + studentWithId
            }
        }
        return idToUse
    }

    override suspend fun insertStudents(students: List<StudentEntity>): List<Long> {
        return students.map { insertStudent(it) }
    }

    override fun insertStudentsSync(students: List<StudentEntity>) {
        students.forEach { s ->
            val idToUse = if (s.id == 0L) nextId++ else s.id
            val studentWithId = s.copy(id = idToUse)
            studentsFlow.update { current ->
                val idx = current.indexOfFirst { it.id == idToUse }
                if (idx >= 0) {
                    current.toMutableList().apply { set(idx, studentWithId) }
                } else {
                    current + studentWithId
                }
            }
        }
    }

    override suspend fun updateStudent(student: StudentEntity) {
        insertStudent(student)
    }

    override suspend fun deleteStudent(student: StudentEntity) {
        studentsFlow.update { current -> current.filter { it.id != student.id } }
    }

    override suspend fun deleteStudentById(id: Long) {
        studentsFlow.update { current -> current.filter { it.id != id } }
    }
}
