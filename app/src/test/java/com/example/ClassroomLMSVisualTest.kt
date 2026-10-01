package com.example

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class ClassroomLMSVisualTest {

  @get:Rule val composeTestRule = createComposeRule()

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