package com.aistudio.classroomlms

import androidx.test.core.app.ApplicationProvider
import com.aistudio.classroomlms.data.entity.ClassroomActivityCategory
import com.aistudio.classroomlms.data.entity.ClassroomActivityEntity
import com.aistudio.classroomlms.data.entity.StudentEntity
import com.aistudio.classroomlms.fake.FakeClassroomActivityDao
import com.aistudio.classroomlms.fake.FakePreferencesManager
import com.aistudio.classroomlms.fake.FakeStudentDao
import com.aistudio.classroomlms.testutil.MainDispatcherRule
import com.aistudio.classroomlms.ui.viewmodel.ClassroomActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ClassroomActivityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var activityDao: FakeClassroomActivityDao
    private lateinit var studentDao: FakeStudentDao
    private lateinit var prefs: FakePreferencesManager
    private lateinit var viewModel: ClassroomActivityViewModel

    @Before
    fun setUp() = runTest {
        activityDao = FakeClassroomActivityDao()
        studentDao = FakeStudentDao()
        prefs = FakePreferencesManager(ApplicationProvider.getApplicationContext())
        prefs.saveActiveClassroomId(1L)

        listOf("Aarav", "Bina", "Chandra", "Deepa", "Esha").forEachIndexed { idx, name ->
            studentDao.insertStudent(StudentEntity(id = idx + 1L, classroomId = 1L, name = name, studentNumber = "0${idx + 1}"))
        }
        viewModel = ClassroomActivityViewModel(activityDao, studentDao, prefs)
        advanceUntilIdle()
    }

    @Test
    fun `saveActivity and generateRandomGroups work for classroom`() = runTest {
        viewModel.generateRandomGroups(2)
        assertEquals(3, viewModel.uiState.value.generatedGroups.size)

        viewModel.pickRandomStudent()
        assertNotNull(viewModel.uiState.value.pickedRandomStudent)

        viewModel.saveActivity(
            ClassroomActivityEntity(
                classroomId = 1L,
                title = "Economics A-Z Terms",
                category = ClassroomActivityCategory.EDUCATIONAL_GAME,
                date = "2026-11-27",
                durationSeconds = 180,
                firstWinnerStudentId = 1L,
                secondWinnerStudentId = 2L,
                thirdWinnerStudentId = 3L
            )
        )
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.activities.size)
        assertEquals("Economics A-Z Terms", viewModel.uiState.value.activities.first().title)
    }
}
