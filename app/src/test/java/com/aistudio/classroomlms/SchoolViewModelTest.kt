package com.aistudio.classroomlms

import com.aistudio.classroomlms.data.entity.ClassroomEntity
import com.aistudio.classroomlms.data.entity.InstitutionType
import com.aistudio.classroomlms.data.entity.SchoolEntity
import com.aistudio.classroomlms.fake.FakeClassroomDao
import com.aistudio.classroomlms.fake.FakeSchoolDao
import com.aistudio.classroomlms.testutil.MainDispatcherRule
import com.aistudio.classroomlms.ui.viewmodel.SchoolViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SchoolViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var schoolDao: FakeSchoolDao
    private lateinit var classroomDao: FakeClassroomDao
    private lateinit var viewModel: SchoolViewModel

    @Before
    fun setUp() {
        schoolDao = FakeSchoolDao()
        classroomDao = FakeClassroomDao()
        viewModel = SchoolViewModel(schoolDao, classroomDao)
    }

    @Test
    fun `assignClassroomToSchool and deleteSchool safely falls back to Not Assigned`() = runTest {
        classroomDao.insertClassroom(
            ClassroomEntity(id = 10L, name = "Grade 10", subject = "Economics", gradeLevel = "10", roomNumber = "101", scheduleInfo = "")
        )
        viewModel.saveSchool(SchoolEntity(name = "Shanti Secondary", institutionType = InstitutionType.SCHOOL))
        advanceUntilIdle()

        val school = viewModel.uiState.value.schools.first()
        viewModel.assignClassroomToSchool(10L, school.id)
        advanceUntilIdle()
        assertEquals(school.id, viewModel.uiState.value.classrooms.first().schoolId)

        viewModel.deleteSchool(school)
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.schools.size)
        assertNull(viewModel.uiState.value.classrooms.first().schoolId)
    }
}
