package com.aistudio.classroomlms

import com.aistudio.classroomlms.data.entity.AcademicEventCategory
import com.aistudio.classroomlms.data.entity.AcademicEventEntity
import com.aistudio.classroomlms.fake.FakeAcademicEventDao
import com.aistudio.classroomlms.testutil.MainDispatcherRule
import com.aistudio.classroomlms.ui.viewmodel.AcademicCalendarViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AcademicCalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeDao: FakeAcademicEventDao
    private lateinit var viewModel: AcademicCalendarViewModel

    @Before
    fun setUp() {
        fakeDao = FakeAcademicEventDao()
        viewModel = AcademicCalendarViewModel(fakeDao)
    }

    @Test
    fun `saveEvent and deleteEvent update calendar state`() = runTest {
        val holiday = AcademicEventEntity(
            title = "Dashain Break",
            category = AcademicEventCategory.FESTIVAL_BREAK,
            startDate = "2026-10-18",
            endDate = "2026-10-25",
            academicYearLabel = "२०८३"
        )
        viewModel.saveEvent(holiday)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.events.size)
        val saved = viewModel.uiState.value.events.first()
        assertEquals("Dashain Break", saved.title)

        viewModel.deleteEvent(saved)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.events.isEmpty())
    }
}
