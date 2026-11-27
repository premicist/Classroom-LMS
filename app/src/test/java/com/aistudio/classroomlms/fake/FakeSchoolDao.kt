package com.aistudio.classroomlms.fake

import com.aistudio.classroomlms.data.dao.SchoolDao
import com.aistudio.classroomlms.data.entity.SchoolEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSchoolDao : SchoolDao {
    private val schoolsFlow = MutableStateFlow<List<SchoolEntity>>(emptyList())
    private var nextId = 1L

    override fun getAllSchools(): Flow<List<SchoolEntity>> = schoolsFlow
    override fun getAllSchoolsOnce(): List<SchoolEntity> = schoolsFlow.value

    override suspend fun insertSchool(school: SchoolEntity): Long {
        val inserted = school.copy(id = if (school.id == 0L) nextId++ else school.id)
        schoolsFlow.update { it + inserted }
        return inserted.id
    }

    override fun insertSchoolsSync(schools: List<SchoolEntity>) {
        schoolsFlow.update { it + schools }
    }

    override suspend fun updateSchool(school: SchoolEntity) {
        schoolsFlow.update { list -> list.map { if (it.id == school.id) school else it } }
    }

    override suspend fun deleteSchool(school: SchoolEntity) {
        schoolsFlow.update { list -> list.filter { it.id != school.id } }
    }
}
