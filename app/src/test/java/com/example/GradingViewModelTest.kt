package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.dao.AttendanceDao
import com.example.data.entity.AssignmentEntity
import com.example.data.entity.AssignmentType
import com.example.data.entity.ClassroomEntity
import com.example.data.entity.StudentEntity
import com.example.data.entity.SubmissionEntity
import com.example.data.entity.SubmissionStatus
import com.example.data.repository.ClassroomRepository
import com.example.fake.FakeAssignmentDao
import com.example.fake.FakeAttendanceDao
import com.example.fake.FakeClassroomDao
import com.example.data.dao.DisciplineDao
import com.example.data.dao.HomeworkRecordDao
import com.example.data.dao.InterventionDao
import com.example.data.dao.LiveAssessmentDao
import com.example.data.dao.PlannerDao
import com.example.data.database.AppDatabase
import com.example.data.entity.DailyLogEntity
import com.example.data.entity.DisciplineRecordEntity
import com.example.data.entity.InterventionEntity
import com.example.data.entity.LessonPlanEntity
import com.example.data.entity.LiveAssessmentEntity
import com.example.fake.FakeClassroomRepository
import com.example.fake.FakeHomeworkRecordDao
import com.example.fake.FakePreferencesManager
import com.example.fake.FakeStudentDao
import com.example.fake.FakeSubmissionDao
import com.example.testutil.MainDispatcherRule
import com.example.ui.viewmodel.GradingViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
class GradingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeAssignmentDao: FakeAssignmentDao
    private lateinit var fakeSubmissionDao: FakeSubmissionDao
    private lateinit var fakeStudentDao: FakeStudentDao
    private lateinit var fakeClassroomDao: FakeClassroomDao
    private lateinit var preferencesManager: FakePreferencesManager
    private lateinit var repository: ClassroomRepository
    private lateinit var viewModel: GradingViewModel

    private val classroomId = 1L

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        fakeAssignmentDao = FakeAssignmentDao()
        fakeSubmissionDao = FakeSubmissionDao()
        fakeStudentDao = FakeStudentDao()
        fakeClassroomDao = FakeClassroomDao()
        preferencesManager = FakePreferencesManager(context)

        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        // Create a fake repository
        repository = object : ClassroomRepository(db) {
            override suspend fun insertAssignment(assignment: AssignmentEntity, autoCreateSubmissions: Boolean): Long {
                val id = fakeAssignmentDao.insertAssignment(assignment)
                if (autoCreateSubmissions) {
                    val students = fakeStudentDao.getStudentsByClassroomOnce(assignment.classroomId)
                    val submissions = students.map {
                        SubmissionEntity(assignmentId = id, studentId = it.id, classroomId = assignment.classroomId)
                    }
                    fakeSubmissionDao.insertSubmissions(submissions)
                }
                return id
            }
            override suspend fun updateAssignment(assignment: AssignmentEntity) = fakeAssignmentDao.updateAssignment(assignment)
            override suspend fun deleteAssignment(id: Long) = fakeAssignmentDao.deleteAssignmentById(id)
            override suspend fun updateSubmission(submission: SubmissionEntity) = fakeSubmissionDao.updateSubmission(submission)
            override suspend fun insertOrUpdateSubmissions(submissions: List<SubmissionEntity>) {
                fakeSubmissionDao.insertSubmissions(submissions)
            }
        }

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

        fakeStudentDao.insertStudent(
            StudentEntity(id = 101L, classroomId = classroomId, name = "Alice", studentNumber = "S01")
        )

        viewModel = GradingViewModel(
            assignmentDao = fakeAssignmentDao,
            submissionDao = fakeSubmissionDao,
            studentDao = fakeStudentDao,
            classroomDao = fakeClassroomDao,
            repository = repository,
            preferencesManager = preferencesManager
        )

        advanceUntilIdle()
    }

    @Test
    fun loadsAssignmentsAndSubmissionsForActiveClassroom() = runTest {
        val assignment = AssignmentEntity(
            id = 1L,
            classroomId = classroomId,
            title = "Algebra Worksheet",
            description = "Solve for x",
            dueDate = "2026-10-10",
            maxPoints = 10.0,
            type = AssignmentType.HOMEWORK
        )
        fakeAssignmentDao.insertAssignment(assignment)

        val submission = SubmissionEntity(
            id = 1L,
            assignmentId = 1L,
            studentId = 101L,
            classroomId = classroomId,
            status = SubmissionStatus.GRADED,
            score = 9.5
        )
        fakeSubmissionDao.insertSubmission(submission)
        
        // Re-trigger the activeClassroomIdFlow to force Room Flow to emit the new data
        preferencesManager.saveActiveClassroomId(null)
        preferencesManager.saveActiveClassroomId(classroomId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(classroomId, state.activeClassroomId)
        assertEquals(1, state.assignments.size)
        assertEquals("Algebra Worksheet", state.assignments[0].title)
        assertEquals(1, state.submissions.size)
        assertEquals(9.5, state.submissions[0].score)
        assertEquals(1, state.studentGradeSummaries.size) // Analytics should compute automatically
    }

    @Test
    fun updateSubmissionGradeReflectsInState() = runTest {
        val assignment = AssignmentEntity(
            id = 1L,
            classroomId = classroomId,
            title = "Geometry Quiz",
            dueDate = "2026-10-15",
            maxPoints = 20.0,
            type = AssignmentType.QUIZ
        )
        fakeAssignmentDao.insertAssignment(assignment)
        
        val submission = SubmissionEntity(
            id = 1L,
            assignmentId = 1L,
            studentId = 101L,
            classroomId = classroomId,
            status = SubmissionStatus.SUBMITTED
        )
        fakeSubmissionDao.insertSubmission(submission)
        
        // Re-trigger flow
        preferencesManager.saveActiveClassroomId(null)
        preferencesManager.saveActiveClassroomId(classroomId)
        advanceUntilIdle()

        val updatedSubmission = submission.copy(status = SubmissionStatus.GRADED, score = 18.0)
        viewModel.saveSubmission(updatedSubmission)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val loadedSub = state.submissions.first()
        assertEquals(SubmissionStatus.GRADED, loadedSub.status)
        assertEquals(18.0, loadedSub.score)
    }

    @Test
    fun setAssignmentFilterUpdatesState() = runTest {
        viewModel.setAssignmentFilter(AssignmentType.PROJECT, SubmissionStatus.PENDING)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AssignmentType.PROJECT, state.assignmentFilterType)
        assertEquals(SubmissionStatus.PENDING, state.assignmentFilterStatus)
    }

    @Test
    fun emptyClassroomDoesNotCrashAndYieldsEmptyLists() = runTest {
        preferencesManager.saveActiveClassroomId(999L) // Unknown ID
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(999L, state.activeClassroomId)
        assertNull(state.activeClassroom)
        assertTrue(state.students.isEmpty())
        assertTrue(state.assignments.isEmpty())
        assertTrue(state.submissions.isEmpty())
        assertTrue(state.studentGradeSummaries.isEmpty())
    }
}
