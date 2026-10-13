package com.aistudio.classroomlms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aistudio.classroomlms.data.entity.AttendanceStatus
import com.aistudio.classroomlms.data.entity.ClassroomEntity
import com.aistudio.classroomlms.ui.components.AttendanceBadge
import com.aistudio.classroomlms.ui.components.ClassroomHeader
import com.aistudio.classroomlms.ui.components.GradeBadge
import com.aistudio.classroomlms.ui.theme.MyApplicationTheme
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
    fun attendanceBadge_displaysStatusAndInvokesOnClick() {
        var clicked = false
        composeTestRule.setContent {
            MyApplicationTheme {
                AttendanceBadge(
                    status = AttendanceStatus.PRESENT,
                    onClick = { clicked = true }
                )
            }
        }

        val badge = composeTestRule.onNodeWithText("Present")
        badge.assertIsDisplayed()
        badge.performClick()
        assertTrue(clicked)
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
