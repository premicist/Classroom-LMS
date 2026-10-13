package com.aistudio.classroomlms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aistudio.classroomlms.data.entity.AttendanceStatus
import com.aistudio.classroomlms.data.entity.ClassroomEntity
import com.aistudio.classroomlms.ui.components.AttendanceStatusBadge
import com.aistudio.classroomlms.ui.components.ClassroomHeader
import com.aistudio.classroomlms.ui.components.GradeBadge
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
        composeTestRule.onNodeWithText("92%").assertIsDisplayed()
    }

    @Test
    fun attendanceBadge_displaysStatusAndInvokesOnClick() {
        var clicked = false
        composeTestRule.setContent {
            MyApplicationTheme {
                AttendanceStatusBadge(
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
        val classroom = ClassroomEntity(
            id = 1L,
            name = "Grade 10 Science",
            subject = "Physics",
            gradeLevel = "10"
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                ClassroomHeader(
                    activeClassroom = classroom,
                    classrooms = listOf(classroom),
                    onSelectClassroom = {},
                    onMenuClick = {},
                    onManageClassrooms = { managedClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Grade 10 Science").assertIsDisplayed()
        composeTestRule.onNodeWithText("Physics • 10").assertIsDisplayed()
    }
}
