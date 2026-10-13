package com.aistudio.classroomlms

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.graphics.Color
import com.aistudio.classroomlms.ui.theme.CalmBackgroundDark
import com.aistudio.classroomlms.ui.theme.CalmBackgroundLight
import com.aistudio.classroomlms.ui.theme.CalmPrimary
import com.aistudio.classroomlms.ui.theme.CalmPrimaryDark
import com.aistudio.classroomlms.ui.theme.CalmSecondary
import com.aistudio.classroomlms.ui.theme.CalmSecondaryDark
import com.aistudio.classroomlms.ui.theme.StatusSuccess
import com.aistudio.classroomlms.ui.theme.StatusSuccessDark
import com.aistudio.classroomlms.ui.theme.MyApplicationTheme
import com.aistudio.classroomlms.ui.theme.statusSuccess
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.junit.Assert.assertEquals

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class ClassroomLMSVisualTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun themeUsesCalmAcademicPaletteInLightAndDarkModes() {
    var lightPrimary: Color? = null
    var lightSecondary: Color? = null
    var lightBackground: Color? = null
    var lightSuccess: Color? = null
    var darkPrimary: Color? = null
    var darkSecondary: Color? = null
    var darkBackground: Color? = null
    var darkSuccess: Color? = null

    composeTestRule.setContent {
      Column {
        MyApplicationTheme(darkTheme = false) {
          val colors = MaterialTheme.colorScheme
          val successColor = colors.statusSuccess
          SideEffect {
            lightPrimary = colors.primary
            lightSecondary = colors.secondary
            lightBackground = colors.background
            lightSuccess = successColor
          }
          Text("Light theme")
        }
        MyApplicationTheme(darkTheme = true) {
          val colors = MaterialTheme.colorScheme
          val successColor = colors.statusSuccess
          SideEffect {
            darkPrimary = colors.primary
            darkSecondary = colors.secondary
            darkBackground = colors.background
            darkSuccess = successColor
          }
          Text("Dark theme")
        }
      }
    }
    composeTestRule.runOnIdle {
      assertEquals(CalmPrimary, lightPrimary)
      assertEquals(CalmSecondary, lightSecondary)
      assertEquals(CalmBackgroundLight, lightBackground)
      assertEquals(StatusSuccess, lightSuccess)
      assertEquals(CalmPrimaryDark, darkPrimary)
      assertEquals(CalmSecondaryDark, darkSecondary)
      assertEquals(CalmBackgroundDark, darkBackground)
      assertEquals(StatusSuccessDark, darkSuccess)
    }
  }

  @Test
  fun dashboard_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Text("Classroom LMS Dashboard")
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/dashboard.png")
  }

  @Test
  fun student_roster_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Text("Student Roster - 24 Students")
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/student_roster.png")
  }

  @Test
  fun attendance_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Text("Attendance - 18/24 Present")
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/attendance.png")
  }

  @Test
  fun gradebook_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Text("Gradebook - Math 10th")
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/gradebook.png")
  }

  @Test
  fun homework_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Text("Homework Check - 22/24 Submitted")
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/homework.png")
  }

  @Test
  fun planner_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Text("Lesson Planner - Week 42")
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/planner.png")
  }

  @Test
  fun discipline_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Text("Discipline Log - 3 Interventions")
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/discipline.png")
  }

  @Test
  fun reports_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Text("Reports & Analytics")
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/reports.png")
  }

  @Test
  fun sync_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Text("Google Sheets Sync - Last Sync: 2 hours ago")
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/sync.png")
  }

  @Test
  fun backup_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Text("Backup & Restore - Last Backup: Today")
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/backup.png")
  }
}