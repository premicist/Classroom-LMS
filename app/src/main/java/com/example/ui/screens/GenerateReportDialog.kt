package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.data.entity.ClassroomEntity

enum class ReportType(val title: String, val description: String) {
    ANALYTICS_SUMMARY("Progress & Analytics Summary", "Overview of grades, risk alerts, and performance trends"),
    GRADEBOOK_FULL("Comprehensive Gradebook Report", "Full breakdown of all assignments, scores, and averages"),
    ATTENDANCE_LOG("Attendance & Absence Log", "Detailed records of present, absent, late, and excused days"),
    LESSON_PLANS("Lesson Plans & Objectives", "Unit titles, target dates, and planning statuses"),
    DAILY_DIARY("Teacher Daily Diary Log", "Classroom reflections, observations, and proxy class notes")
}

enum class DateRangeOption(val label: String) {
    ALL_TIME("All Time"),
    CURRENT_MONTH("Current Month"),
    LAST_30_DAYS("Last 30 Days")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateReportDialog(
    classrooms: List<ClassroomEntity>,
    activeClassroomId: Long?,
    onDismiss: () -> Unit,
    onGeneratePdf: (
        reportType: ReportType,
        classroomId: Long?,
        dateRange: DateRangeOption,
        includeAtRisk: Boolean,
        includeDifficulty: Boolean,
        includeDistribution: Boolean,
        includeAttendance: Boolean
    ) -> Unit,
    onPreviewText: (
        reportType: ReportType,
        classroomId: Long?,
        dateRange: DateRangeOption,
        includeAtRisk: Boolean,
        includeDifficulty: Boolean,
        includeDistribution: Boolean,
        includeAttendance: Boolean
    ) -> Unit
) {
    var selectedReportType by remember { mutableStateOf(ReportType.ANALYTICS_SUMMARY) }
    var selectedClassroomId by remember { mutableStateOf(activeClassroomId) }
    var selectedDateRange by remember { mutableStateOf(DateRangeOption.ALL_TIME) }

    var includeAtRisk by remember { mutableStateOf(true) }
    var includeDifficulty by remember { mutableStateOf(true) }
    var includeDistribution by remember { mutableStateOf(true) }
    var includeAttendance by remember { mutableStateOf(true) }

    var isReportTypeExpanded by remember { mutableStateOf(false) }
    var isClassroomExpanded by remember { mutableStateOf(false) }

    val selectedClassroomName = when (selectedClassroomId) {
        null -> "All Classrooms"
        else -> classrooms.find { it.id == selectedClassroomId }?.let { "${it.name} (${it.subject})" } ?: "All Classrooms"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Generate Custom Report",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Report Type Selector
                Text("Select Report Type", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(
                    expanded = isReportTypeExpanded,
                    onExpandedChange = { isReportTypeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedReportType.title,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isReportTypeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = isReportTypeExpanded,
                        onDismissRequest = { isReportTypeExpanded = false }
                    ) {
                        ReportType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(type.title, fontWeight = FontWeight.Bold)
                                        Text(type.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    selectedReportType = type
                                    isReportTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                // 2. Classroom Selector
                Text("Select Classroom Target", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(
                    expanded = isClassroomExpanded,
                    onExpandedChange = { isClassroomExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedClassroomName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isClassroomExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = isClassroomExpanded,
                        onDismissRequest = { isClassroomExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Classrooms", fontWeight = FontWeight.Bold) },
                            onClick = {
                                selectedClassroomId = null
                                isClassroomExpanded = false
                            }
                        )
                        classrooms.forEach { cls ->
                            DropdownMenuItem(
                                text = { Text("${cls.name} (${cls.subject})") },
                                onClick = {
                                    selectedClassroomId = cls.id
                                    isClassroomExpanded = false
                                }
                            )
                        }
                    }
                }

                // 3. Time Period Filter
                Text("Time Period", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateRangeOption.entries.forEach { range ->
                        FilterChip(
                            selected = selectedDateRange == range,
                            onClick = { selectedDateRange = range },
                            label = { Text(range.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // 4. Fillable Option Checkboxes
                Text("Include Sections (Fillable Toggles)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeAtRisk, onCheckedChange = { includeAtRisk = it })
                    Text("At-Risk & Support Needed Alerts", style = MaterialTheme.typography.bodyMedium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeDifficulty, onCheckedChange = { includeDifficulty = it })
                    Text("High Difficulty Topic Areas", style = MaterialTheme.typography.bodyMedium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeDistribution, onCheckedChange = { includeDistribution = it })
                    Text("Grade Distribution Breakdown", style = MaterialTheme.typography.bodyMedium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeAttendance, onCheckedChange = { includeAttendance = it })
                    Text("Attendance & Absence Summary", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onGeneratePdf(
                            selectedReportType,
                            selectedClassroomId,
                            selectedDateRange,
                            includeAtRisk,
                            includeDifficulty,
                            includeDistribution,
                            includeAttendance
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generate & Share A4 PDF")
                }

                OutlinedButton(
                    onClick = {
                        onPreviewText(
                            selectedReportType,
                            selectedClassroomId,
                            selectedDateRange,
                            includeAtRisk,
                            includeDifficulty,
                            includeDistribution,
                            includeAttendance
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Preview Text Report")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun GenerateReportDialogPreview() {
    val mockClassrooms = listOf(
        ClassroomEntity(id = 1, name = "Grade 10 Mathematics", subject = "Algebra II", gradeLevel = "10th Grade", roomNumber = "101", scheduleInfo = "Mon/Wed 9:00 AM"),
        ClassroomEntity(id = 2, name = "Grade 11 Physics", subject = "Mechanics", gradeLevel = "11th Grade", roomNumber = "202", scheduleInfo = "Tue/Thu 11:00 AM")
    )
    GenerateReportDialog(
        classrooms = mockClassrooms,
        activeClassroomId = 1,
        onDismiss = {},
        onGeneratePdf = { _, _, _, _, _, _, _ -> },
        onPreviewText = { _, _, _, _, _, _, _ -> }
    )
}
