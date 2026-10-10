package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.entity.ClassroomEntity
import com.example.data.entity.LessonPlanEntity
import com.example.fake.FakeClassScheduleDao
import com.example.fake.FakeClassroomDao
import com.example.fake.FakePlannerDao
import com.example.fake.FakePreferencesManager
import com.example.testutil.MainDispatcherRule
import com.example.ui.viewmodel.PlannerViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlannerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakePlannerDao: FakePlannerDao
    private lateinit var fakeClassScheduleDao: FakeClassScheduleDao
    private lateinit var fakeClassroomDao: FakeClassroomDao
    private lateinit var preferencesManager: FakePreferencesManager
    private lateinit var viewModel: PlannerViewModel

    private val classroomId = 1L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        fakePlannerDao = FakePlannerDao()
        fakeClassScheduleDao = FakeClassScheduleDao()
        fakeClassroomDao = FakeClassroomDao()
        preferencesManager = FakePreferencesManager(context)

        runBlocking {
            preferencesManager.saveActiveClassroomId(classroomId)

            fakeClassroomDao.insertClassroom(
                ClassroomEntity(
                    id = classroomId,
                    name = "Grade 10 Math",
                    subject = "Mathematics",
                    gradeLevel = "10",
                    roomNumber = "102",
                    scheduleInfo = "Mon-Wed"
                )
            )
        }

        viewModel = PlannerViewModel(
            plannerDao = fakePlannerDao,
            classScheduleDao = fakeClassScheduleDao,
            preferencesManager = preferencesManager,
            classroomDao = fakeClassroomDao
        )
    }

    @Test
    fun `loads active classroom and empty plans initially`() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(classroomId, state.activeClassroomId)
        assertNotNull(state.activeClassroom)
        assertTrue(state.lessonPlans.isEmpty())
        assertTrue(state.dailyLogs.isEmpty())
    }

    @Test
    fun `saveLessonPlan inserts plan into database and updates state`() = runTest {
        advanceUntilIdle()

        viewModel.saveLessonPlan(
            unitTitle = "Algebra 1",
            description = "Intro to equations",
            targetDate = 1700000000000L,
            status = "DRAFT",
            targetClassroomId = classroomId
        )
        advanceUntilIdle()

        val plans = fakePlannerDao.getLessonPlansForClassroom(classroomId).first()
        assertEquals(1, plans.size)
        assertEquals("Algebra 1", plans[0].unitTitle)

        val state = viewModel.uiState.value
        assertEquals(1, state.lessonPlans.size)
        assertEquals("Algebra 1", state.lessonPlans[0].unitTitle)
    }

    @Test
    fun `deleteLessonPlan removes plan from database and state`() = runTest {
        fakePlannerDao.insertLessonPlan(
            LessonPlanEntity(
                classroomId = classroomId,
                unitTitle = "Geometry",
                description = "Triangles",
                targetDate = 1700000000000L,
                status = "DRAFT"
            )
        )
        advanceUntilIdle()

        val stateBefore = viewModel.uiState.value
        assertEquals(1, stateBefore.lessonPlans.size)
        val plan = stateBefore.lessonPlans.first()

        viewModel.deleteLessonPlan(plan)
        advanceUntilIdle()

        val plans = fakePlannerDao.getLessonPlansForClassroom(classroomId).first()
        assertTrue(plans.isEmpty())

        val stateAfter = viewModel.uiState.value
        assertTrue(stateAfter.lessonPlans.isEmpty())
    }

    @Test
    fun `empty classroom yields empty lists`() = runTest {
        preferencesManager.saveActiveClassroomId(999L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(999L, state.activeClassroomId)
        assertNull(state.activeClassroom)
        assertTrue(state.lessonPlans.isEmpty())
        assertTrue(state.dailyLogs.isEmpty())
    }
}
