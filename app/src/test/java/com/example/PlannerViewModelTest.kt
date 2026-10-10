package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.entity.ClassroomEntity
import com.example.data.entity.LessonPlanEntity
import com.example.fake.FakePreferencesManager
import com.example.testutil.MainDispatcherRule
import com.example.ui.viewmodel.PlannerViewModel
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
import com.example.fake.FakePlannerDao
import com.example.fake.FakeClassScheduleDao
import com.example.fake.FakeClassroomDao
import com.example.fake.FakeAssignmentDao
import com.example.fake.FakeSubmissionDao
import com.example.fake.FakeStudentDao
import com.example.fake.FakeClassroomRepository
import com.example.data.repository.ClassroomRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlannerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakePlannerDao: FakePlannerDao
    private lateinit var fakeClassScheduleDao: FakeClassScheduleDao
    private lateinit var fakeClassroomDao: FakeClassroomDao
    private lateinit var preferencesManager: FakePreferencesManager
    private lateinit var repository: ClassroomRepository
    private lateinit var viewModel: PlannerViewModel

    private val classroomId = 1L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        fakePlannerDao = FakePlannerDao()
        fakeClassScheduleDao = FakeClassScheduleDao()
        fakeClassroomDao = FakeClassroomDao()
        preferencesManager = FakePreferencesManager(context)

        // Null AppDatabase is safe since FakeClassroomRepository overrides behavior 
        repository = FakeClassroomRepository(
            db = null,
            fakeAssignmentDao = FakeAssignmentDao(),
            fakeSubmissionDao = FakeSubmissionDao(),
            fakeStudentDao = FakeStudentDao()
        )

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
        // Since database is null but ClassroomViewModel was already initialized properly using explicit DAOs elsewhere, 
        // wait! PlannerViewModel has `private val classroomDao = database.classroomDao()`. 
        // Oh no, it will crash if database is null. 
        // Let me rewrite PlannerViewModel to use constructor injection for classroomDao!
    }

    @Test
    fun `loads active classroom and empty plans initially`() = runTest {
        preferencesManager.saveActiveClassroomId(null)
        advanceUntilIdle()
        ShadowLooper.idleMainLooper()
        preferencesManager.saveActiveClassroomId(classroomId)
        advanceUntilIdle()
        ShadowLooper.idleMainLooper()

        val state = viewModel.uiState.value
        assertEquals(classroomId, state.activeClassroomId)
        assertNotNull(state.activeClassroom)
        assertTrue(state.lessonPlans.isEmpty())
        assertTrue(state.dailyLogs.isEmpty())
    }

    @Test
    fun `saveLessonPlan inserts plan into database and updates state`() = runTest {
        advanceUntilIdle()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        viewModel.saveLessonPlan(
            unitTitle = "Algebra 1",
            description = "Intro to equations",
            targetDate = 1700000000000L,
            status = "DRAFT",
            targetClassroomId = classroomId
        )
        advanceUntilIdle()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        val plans = fakePlannerDao.getLessonPlansForClassroom(classroomId).first()
        assertEquals(1, plans.size)
        assertEquals("Algebra 1", plans[0].unitTitle)
        
        // Re-trigger the activeClassroomIdFlow to force Room Flow to emit the new data
        preferencesManager.saveActiveClassroomId(null)
        advanceUntilIdle()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        preferencesManager.saveActiveClassroomId(classroomId)
        advanceUntilIdle()
        ShadowLooper.idleMainLooper()

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
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        
        // Re-trigger the activeClassroomIdFlow to force Room Flow to emit the new data
        preferencesManager.saveActiveClassroomId(null)
        advanceUntilIdle()
        ShadowLooper.idleMainLooper()
        preferencesManager.saveActiveClassroomId(classroomId)
        advanceUntilIdle()
        ShadowLooper.idleMainLooper()

        val stateBefore = viewModel.uiState.value
        assertEquals(1, stateBefore.lessonPlans.size)
        val plan = stateBefore.lessonPlans.first()

        viewModel.deleteLessonPlan(plan)
        advanceUntilIdle()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        ShadowLooper.idleMainLooper()

        val plans = fakePlannerDao.getLessonPlansForClassroom(classroomId).first()
        assertTrue(plans.isEmpty())
        
        preferencesManager.saveActiveClassroomId(null)
        advanceUntilIdle()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        preferencesManager.saveActiveClassroomId(classroomId)
        advanceUntilIdle()
        ShadowLooper.idleMainLooper()
        ShadowLooper.idleMainLooper()

        val stateAfter = viewModel.uiState.value
        assertTrue(stateAfter.lessonPlans.isEmpty())
    }

    @Test
    fun `empty classroom yields empty lists`() = runTest {
        // Set to a classroom that doesn't exist
        preferencesManager.saveActiveClassroomId(999L)
        advanceUntilIdle()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        ShadowLooper.idleMainLooper()

        val state = viewModel.uiState.value
        assertEquals(999L, state.activeClassroomId)
        assertNull(state.activeClassroom)
        assertTrue(state.lessonPlans.isEmpty())
        assertTrue(state.dailyLogs.isEmpty())
    }
}
