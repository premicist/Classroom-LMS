package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.database.PreferencesManager
import com.example.data.entity.AttendanceStatus
import com.example.data.entity.ClassroomEntity
import com.example.data.entity.StudentEntity
import com.example.fake.FakeAttendanceDao
import com.example.fake.FakeClassroomDao
import com.example.fake.FakeHomeworkRecordDao
import com.example.fake.FakePreferencesManager
import com.example.fake.FakeStudentDao
import com.example.testutil.MainDispatcherRule
import com.example.ui.viewmodel.AttendanceViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AttendanceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeAttendanceDao: FakeAttendanceDao
    private lateinit var fakeHomeworkRecordDao: FakeHomeworkRecordDao
    private lateinit var fakeStudentDao: FakeStudentDao
    private lateinit var fakeClassroomDao: FakeClassroomDao
    private lateinit var preferencesManager: FakePreferencesManager
    private lateinit var viewModel: AttendanceViewModel

    private val targetDate = "2026-10-08"
    private val classroomId = 1L

    @Before
    fun setUp() = runTest {
        fakeAttendanceDao = FakeAttendanceDao()
        fakeHomeworkRecordDao = FakeHomeworkRecordDao()
        fakeStudentDao = FakeStudentDao()
        fakeClassroomDao = FakeClassroomDao()

        preferencesManager = FakePreferencesManager(ApplicationProvider.getApplicationContext())
        preferencesManager.saveActiveClassroomId(classroomId)

        // Seed fake classroom and 3 fake students
        fakeClassroomDao.insertClassroom(
            ClassroomEntity(
                id = classroomId,
                name = "Grade 10-A",
                subject = "Science",
                gradeLevel = "10",
                roomNumber = "101",
                scheduleInfo = "Mon-Fri 8:00 AM"
            )
        )

        fakeStudentDao.insertStudent(
            StudentEntity(id = 101L, classroomId = classroomId, name = "Alice Smith", studentNumber = "S101")
        )
        fakeStudentDao.insertStudent(
            StudentEntity(id = 102L, classroomId = classroomId, name = "Bob Jones", studentNumber = "S102")
        )
        fakeStudentDao.insertStudent(
            StudentEntity(id = 103L, classroomId = classroomId, name = "Charlie Brown", studentNumber = "S103")
        )

        viewModel = AttendanceViewModel(
            attendanceDao = fakeAttendanceDao,
            homeworkRecordDao = fakeHomeworkRecordDao,
            studentDao = fakeStudentDao,
            preferencesManager = preferencesManager,
            classroomDao = fakeClassroomDao
        )

        advanceUntilIdle()
    }

    @Test
    fun loadsAndExposesAttendanceWhenClassroomContextAvailable() = runTest {
        viewModel.selectDate(targetDate)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(classroomId, state.activeClassroomId)
        assertEquals("Grade 10-A", state.activeClassroom?.name)
        assertEquals(3, state.students.size)
        assertEquals(targetDate, state.selectedDate)
    }

    @Test
    fun markAllPresentMarksAllStudentsPresentAndUpdateStateAndDao() = runTest {
        viewModel.selectDate(targetDate)
        viewModel.markAllPresent(targetDate)
        advanceUntilIdle()

        val recordsFromDao = fakeAttendanceDao.getAttendanceByDateOnce(classroomId, targetDate)
        assertEquals(3, recordsFromDao.size)
        assertTrue(recordsFromDao.all { it.status == AttendanceStatus.PRESENT })

        val stateRecords = viewModel.uiState.value.attendanceRecords
        assertEquals(3, stateRecords.size)
        assertTrue(stateRecords.all { it.status == AttendanceStatus.PRESENT })
    }

    @Test
    fun cycleAttendanceStatusChangesStudentStatusInExpectedOrder() = runTest {
        viewModel.selectDate(targetDate)
        // Initially mark all present (PRESENT)
        viewModel.markAllPresent(targetDate)
        advanceUntilIdle()

        // Cycle 1: PRESENT -> ABSENT
        viewModel.cycleAttendanceStatus(101L, targetDate)
        advanceUntilIdle()

        var studentRecord = fakeAttendanceDao.getAttendanceByDateOnce(classroomId, targetDate)
            .find { it.studentId == 101L }
        assertNotNull(studentRecord)
        assertEquals(AttendanceStatus.ABSENT, studentRecord?.status)

        // Cycle 2: ABSENT -> LATE
        viewModel.cycleAttendanceStatus(101L, targetDate)
        advanceUntilIdle()

        studentRecord = fakeAttendanceDao.getAttendanceByDateOnce(classroomId, targetDate)
            .find { it.studentId == 101L }
        assertEquals(AttendanceStatus.LATE, studentRecord?.status)

        // Cycle 3: LATE -> EXCUSED
        viewModel.cycleAttendanceStatus(101L, targetDate)
        advanceUntilIdle()

        studentRecord = fakeAttendanceDao.getAttendanceByDateOnce(classroomId, targetDate)
            .find { it.studentId == 101L }
        assertEquals(AttendanceStatus.EXCUSED, studentRecord?.status)

        // Cycle 4: EXCUSED -> PRESENT
        viewModel.cycleAttendanceStatus(101L, targetDate)
        advanceUntilIdle()

        studentRecord = fakeAttendanceDao.getAttendanceByDateOnce(classroomId, targetDate)
            .find { it.studentId == 101L }
        assertEquals(AttendanceStatus.PRESENT, studentRecord?.status)
    }

    @Test
    fun reportTextGeneratorReturnsNonEmptyContentWhenDataExists() = runTest {
        viewModel.selectDate(targetDate)
        viewModel.markAllPresent(targetDate)
        advanceUntilIdle()

        val reportText = viewModel.generateFormattedAttendanceReportText()
        assertTrue(reportText.isNotEmpty())
        assertTrue(reportText.contains("Grade 10-A"))
        assertTrue(reportText.contains("CLASSROOM ATTENDANCE AUDIT REPORT"))
        assertTrue(reportText.contains("Alice Smith"))
    }
}
