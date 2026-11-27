package com.aistudio.classroomlms

import android.content.Context
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.aistudio.classroomlms.data.entity.ClassroomEntity
import com.aistudio.classroomlms.data.entity.LessonPlanEntity
import com.aistudio.classroomlms.fake.FakeClassScheduleDao
import com.aistudio.classroomlms.fake.FakeClassroomDao
import com.aistudio.classroomlms.fake.FakePlannerDao
import com.aistudio.classroomlms.fake.FakePreferencesManager
import com.aistudio.classroomlms.testutil.MainDispatcherRule
import com.aistudio.classroomlms.ui.viewmodel.PlannerViewModel
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
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
        val application = ApplicationProvider.getApplicationContext<Application>()

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
            classroomDao = fakeClassroomDao,
            application = application
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
            targetClassroomId = classroomId,
            reminderEnabled = false,
            reminderTime = 0L
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

    @Test
    fun `saveLessonPlan persists structured LO sessions and formats report`() = runTest {
        val sessions = listOf(
            com.aistudio.classroomlms.data.entity.LessonPlanSessionEntity(
                lessonPlanId = 0L,
                sessionOrder = 1,
                learningOutcome = "LO1: Define CPU components",
                content = "ALU, CU, Registers",
                lecturerActivity = "Explain block diagram",
                studentActivity = "Label components",
                teachingAids = "Smartboard",
                evaluationType = "LIVE_ASSESSMENT",
                evaluationNote = "CPU block diagram check",
                hours = 2.0
            ),
            com.aistudio.classroomlms.data.entity.LessonPlanSessionEntity(
                lessonPlanId = 0L,
                sessionOrder = 2,
                learningOutcome = "Closing of Unit",
                content = "Unit summary and Q&A",
                evaluationType = "NOTE",
                evaluationNote = "Oral summary",
                hours = 1.0,
                isClosingSession = true
            )
        )

        viewModel.saveLessonPlan(
            unitTitle = "Computer Hardware",
            description = "Overview",
            targetDate = 1700000000000L,
            status = "PLANNED",
            targetClassroomId = classroomId,
            reminderEnabled = false,
            reminderTime = 0L,
            unitNumber = "Unit 2",
            allocatedHours = 3.0,
            unitLearningOutcomes = "Identify CPU sub-components",
            sessions = sessions
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.lessonPlans.size)
        assertEquals("Unit 2", state.lessonPlans.first().unitNumber)
        assertEquals(2, state.lessonPlanSessions.size)
        assertEquals("LO1: Define CPU components", state.lessonPlanSessions.first().learningOutcome)
        assertTrue(state.lessonPlanSessions.last().isClosingSession)

        val report = viewModel.generateLessonPlansReportText()
        assertTrue(report.contains("Unit 2: Computer Hardware (3.0 Hrs)"))
        assertTrue(report.contains("LO1: Define CPU components"))
        assertTrue(report.contains("Closing of Unit"))
    }
}
