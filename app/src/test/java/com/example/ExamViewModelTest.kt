package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.database.PreferencesManager
import com.example.data.entity.ExamCategory
import com.example.data.entity.ExamEntity
import com.example.data.entity.ExamMarkEntity
import com.example.data.entity.StudentEntity
import com.example.fake.FakeExamDao
import com.example.fake.FakePreferencesManager
import com.example.fake.FakeStudentDao
import com.example.testutil.MainDispatcherRule
import com.example.ui.viewmodel.ExamViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class ExamViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeExamDao: FakeExamDao
    private lateinit var fakeStudentDao: FakeStudentDao
    private lateinit var preferencesManager: FakePreferencesManager
    private lateinit var viewModel: ExamViewModel

    private val classroomId = 1L

    @Before
    fun setUp() = runTest {
        fakeExamDao = FakeExamDao()
        fakeStudentDao = FakeStudentDao()

        preferencesManager = FakePreferencesManager(ApplicationProvider.getApplicationContext())
        preferencesManager.saveActiveClassroomId(classroomId)

        fakeStudentDao.insertStudent(
            StudentEntity(id = 101L, classroomId = classroomId, name = "Alice Smith", studentNumber = "S101")
        )
        fakeStudentDao.insertStudent(
            StudentEntity(id = 102L, classroomId = classroomId, name = "Bob Jones", studentNumber = "S102")
        )

        viewModel = ExamViewModel(
            examDao = fakeExamDao,
            studentDao = fakeStudentDao,
            preferencesManager = preferencesManager
        )

        advanceUntilIdle()
    }

    @Test
    fun examsAndMarksReflectedFromDaoForActiveClassroom() = runTest {
        val testExam = ExamEntity(
            id = 1L,
            classroomId = classroomId,
            category = ExamCategory.CLASS_TEST,
            title = "Unit Test 1",
            fullMarks = 100.0,
            passMarks = 40.0,
            date = "2026-10-08"
        )
        fakeExamDao.insertExam(testExam)

        val testMark = ExamMarkEntity(
            id = 1L,
            examId = 1L,
            studentId = 101L,
            marksObtained = 88.5
        )
        fakeExamDao.insertMark(testMark)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(classroomId, state.activeClassroomId)
        assertEquals(1, state.exams.size)
        assertEquals("Unit Test 1", state.exams.first().title)
        assertEquals(1, state.examMarks.size)
        assertEquals(88.5, state.examMarks.first().marksObtained ?: 0.0, 0.001)
        assertEquals(2, state.students.size)
    }

    @Test
    fun createAndSaveExamVisibleInStateAndDao() = runTest {
        val newExam = ExamEntity(
            classroomId = classroomId,
            category = ExamCategory.TERMINAL,
            title = "Midterm Examination",
            fullMarks = 100.0,
            passMarks = 50.0,
            date = "2026-11-15"
        )

        viewModel.saveExam(newExam)
        advanceUntilIdle()

        val examsFromDao = fakeExamDao.getExamsForClassroomOnce(classroomId)
        assertEquals(1, examsFromDao.size)
        assertEquals("Midterm Examination", examsFromDao.first().title)

        val stateExams = viewModel.uiState.value.exams
        assertEquals(1, stateExams.size)
        assertEquals("Midterm Examination", stateExams.first().title)
    }

    @Test
    fun updateExamUpdatesExistingRecord() = runTest {
        val initialExam = ExamEntity(
            id = 1L,
            classroomId = classroomId,
            category = ExamCategory.CLASS_TEST,
            title = "Initial Title",
            fullMarks = 50.0,
            passMarks = 20.0,
            date = "2026-10-01"
        )
        fakeExamDao.insertExam(initialExam)
        advanceUntilIdle()

        val updatedExam = initialExam.copy(title = "Updated Title", fullMarks = 100.0)
        viewModel.saveExam(updatedExam)
        advanceUntilIdle()

        val examsFromDao = fakeExamDao.getExamsForClassroomOnce(classroomId)
        assertEquals(1, examsFromDao.size)
        assertEquals("Updated Title", examsFromDao.first().title)
        assertEquals(100.0, examsFromDao.first().fullMarks, 0.001)
    }

    @Test
    fun deleteExamRemovesExamAndAssociatedMarks() = runTest {
        val examToDelete = ExamEntity(
            id = 1L,
            classroomId = classroomId,
            category = ExamCategory.PRE_BOARD,
            title = "Pre-Board Mock",
            fullMarks = 100.0,
            passMarks = 40.0,
            date = "2026-12-01"
        )
        fakeExamDao.insertExam(examToDelete)
        fakeExamDao.insertMark(ExamMarkEntity(id = 1L, examId = 1L, studentId = 101L, marksObtained = 75.0))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.exams.size)

        viewModel.deleteExam(examToDelete)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.exams.isEmpty())
        assertTrue(fakeExamDao.getAllExamsOnce().isEmpty())
        assertTrue(fakeExamDao.getAllExamMarksOnce().isEmpty())
    }

    @Test
    fun saveExamMarkSavesAndUpdatesStudentMark() = runTest {
        val exam = ExamEntity(
            id = 1L,
            classroomId = classroomId,
            category = ExamCategory.CLASS_TEST,
            title = "Quiz 1",
            fullMarks = 20.0,
            passMarks = 8.0,
            date = "2026-10-05"
        )
        fakeExamDao.insertExam(exam)
        advanceUntilIdle()

        val mark = ExamMarkEntity(
            examId = 1L,
            studentId = 101L,
            marksObtained = 18.0
        )
        viewModel.saveExamMark(mark)
        advanceUntilIdle()

        val marksFromDao = fakeExamDao.getMarksForExamOnce(1L)
        assertEquals(1, marksFromDao.size)
        assertEquals(18.0, marksFromDao.first().marksObtained ?: 0.0, 0.001)

        val stateMarks = viewModel.uiState.value.examMarks
        assertEquals(1, stateMarks.size)
        assertEquals(18.0, stateMarks.first().marksObtained ?: 0.0, 0.001)
    }

    @Test
    fun dialogOpenAndCloseFlagsUpdateStateCorrectly() = runTest {
        val sampleExam = ExamEntity(
            id = 1L,
            classroomId = classroomId,
            category = ExamCategory.TERMINAL,
            title = "Term 1",
            fullMarks = 100.0,
            passMarks = 40.0,
            date = "2026-10-10"
        )

        assertFalse(viewModel.uiState.value.isAddEditExamOpen)
        assertNull(viewModel.uiState.value.editingExam)

        viewModel.openAddEditExamDialog(ExamCategory.TERMINAL, sampleExam)
        assertTrue(viewModel.uiState.value.isAddEditExamOpen)
        assertEquals(ExamCategory.TERMINAL, viewModel.uiState.value.selectedExamCategory)
        assertEquals(sampleExam, viewModel.uiState.value.editingExam)

        viewModel.closeAddEditExamDialog()
        assertFalse(viewModel.uiState.value.isAddEditExamOpen)
        assertNull(viewModel.uiState.value.editingExam)
    }
}
