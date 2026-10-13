package com.aistudio.classroomlms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.aistudio.classroomlms.data.entity.AttendanceStatus
import com.aistudio.classroomlms.data.entity.ClassroomEntity
import com.aistudio.classroomlms.ui.components.AttendanceStatusSelector
import com.aistudio.classroomlms.ui.components.ClassroomHeader
import com.aistudio.classroomlms.ui.components.GradeBadge
import com.aistudio.classroomlms.ui.components.InlineGradeEntry
import com.aistudio.classroomlms.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ComposeUiFlowsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun gradeBadge_displaysCorrectLetterGradeAndPercentage() {
        composeTestRule.setContent {
            MyApplicationTheme {
                GradeBadge(letterGrade = "A", percentage = 92.5)
            }
        }

        composeTestRule.onNodeWithText("A").assertIsDisplayed()
        composeTestRule.onNodeWithText("92.5%").assertIsDisplayed()
    }

    @Test
    fun attendanceStatusSelector_recordsSelectedStatus() {
        var selectedStatus: AttendanceStatus? = null
        composeTestRule.setContent {
            MyApplicationTheme {
                AttendanceStatusSelector(
                    status = AttendanceStatus.PRESENT,
                    onStatusSelected = { selectedStatus = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Present").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("attendance_status_absent").performClick()
        assertEquals(AttendanceStatus.ABSENT, selectedStatus)
    }

    @Test
    fun inlineGradeEntry_savesEnteredScore() {
        var savedScore: Double? = null
        composeTestRule.setContent {
            MyApplicationTheme {
                InlineGradeEntry(
                    submissionId = 14L,
                    assignmentTitle = "Fractions worksheet",
                    maxPoints = 100.0,
                    initialScore = null,
                    isCompact = false,
                    onSave = { savedScore = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("inline_grade_score").performTextInput("84")
        composeTestRule.onNodeWithTag("inline_grade_save").performClick()
        assertEquals(84.0, savedScore)
    }

    @Test
    fun classroomHeader_displaysActiveClassroomAndHandlesSwitch() {
        var managedClicked = false
        var selectedClassroomId: Long? = null
        val activeClassroom = ClassroomEntity(
            id = 1L,
            name = "Grade 10 Science",
            subject = "Physics",
            gradeLevel = "10",
            roomNumber = "",
            scheduleInfo = ""
        )
        val nextClassroom = ClassroomEntity(
            id = 2L,
            name = "Grade 9 Math",
            subject = "Mathematics",
            gradeLevel = "9",
            roomNumber = "",
            scheduleInfo = ""
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                ClassroomHeader(
                    activeClassroom = activeClassroom,
                    classrooms = listOf(activeClassroom, nextClassroom),
                    onSelectClassroom = { selectedClassroomId = it.id },
                    onMenuClick = {},
                    onManageClassrooms = { managedClicked = true }
                )
            }
        }

        composeTestRule.onAllNodesWithText("Grade 10 Science")[0].assertIsDisplayed()
        composeTestRule.onNodeWithText("Physics • 10").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Grade 10 Science")[0].performClick()
        assertTrue(managedClicked)
        composeTestRule.onNodeWithText("Grade 9 Math").performClick()
        assertTrue(selectedClassroomId == nextClassroom.id)
    }
}
