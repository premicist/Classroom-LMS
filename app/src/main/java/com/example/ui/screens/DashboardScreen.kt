package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.AssignmentType
import com.example.ui.components.AlertBanner
import com.example.ui.components.AssignmentTypeBadge
import com.example.ui.components.CircularProgressGauge
import com.example.ui.components.GradeBadge
import com.example.ui.components.GradeDistributionBarChart
import com.example.ui.components.QuickStatCard
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.EduPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.ClassroomViewModel
import com.example.ui.viewmodel.LmsTab
import com.example.ui.viewmodel.LmsUiState
import com.example.ui.viewmodel.StudentGradeSummary

@Composable
fun DashboardScreen(
    uiState: LmsUiState,
    viewModel: ClassroomViewModel,
    modifier: Modifier = Modifier
) {
    val analytics = uiState.analytics
    val activeClassroom = uiState.activeClassroom

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Actions Row
        item {
            Column {
                Text(
                    text = "Teacher Quick Actions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        ActionChip(
                            icon = Icons.Default.EventAvailable,
                            label = "Take Attendance",
                            color = StatusSuccess,
                            onClick = { viewModel.selectTab(LmsTab.ATTENDANCE) }
                        )
                    }
                    item {
                        ActionChip(
                            icon = Icons.Default.MenuBook,
                            label = "Check Homework",
                            color = EduPrimary,
                            onClick = { viewModel.selectTab(LmsTab.HOMEWORK) }
                        )
                    }
                    item {
                        ActionChip(
                            icon = Icons.Default.Add,
                            label = "New Assignment",
                            color = Color(0xFF7C3AED),
                            onClick = { viewModel.openAddAssignment() }
                        )
                    }
                    item {
                        ActionChip(
                            icon = Icons.Default.People,
                            label = "Student Roster",
                            color = StatusWarning,
                            onClick = { viewModel.openStudentRoster() }
                        )
                    }
                    item {
                        ActionChip(
                            icon = Icons.Default.Share,
                            label = "Export PDF",
                            color = StatusInfo,
                            onClick = {
                                val report = viewModel.generateProgressAnalyticsReportText()
                                viewModel.openExportReport(
                                    report,
                                    title = "Class Progress & Intervention Report"
                                )
                            }
                        )
                    }
                }
            }
        }

        // Today's Classes Frontpage Card
        item {
            val (isHoliday, summaryMessage) = viewModel.getTodayClassesSummary()

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isHoliday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isHoliday) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isHoliday) Icons.Default.BeachAccess else Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (isHoliday) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Today's Classes",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = summaryMessage,
                            fontSize = 13.sp,
                            fontWeight = if (isHoliday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isHoliday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.openScheduleScreen() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Routine", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // At-Risk Alert Banner (if any)
        if (analytics.atRiskStudents.isNotEmpty()) {
            item {
                AlertBanner(
                    title = "${analytics.atRiskStudents.size} Students Require Attention",
                    message = "Low grades, chronic attendance issues, or missing assignments flagged.",
                    actionText = "Review",
                    onAction = { viewModel.selectTab(LmsTab.GRADEBOOK) }
                )
            }
        }

        // Key Performance Metrics Grid
        item {
            Column {
                Text(
                    text = "Class Performance Overview",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickStatCard(
                        title = "Class Average",
                        value = "${analytics.averagePercentage}%",
                        subtitle = "GPA: ${"%.2f".format(analytics.averageGpa)}",
                        icon = Icons.Default.Grade,
                        accentColor = EduPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectTab(LmsTab.GRADEBOOK) }
                    )
                    QuickStatCard(
                        title = "Attendance Rate",
                        value = "${analytics.todayAttendanceRate}%",
                        subtitle = "Today's check-in",
                        icon = Icons.Default.EventAvailable,
                        accentColor = StatusSuccess,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectTab(LmsTab.ATTENDANCE) }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickStatCard(
                        title = "Students",
                        value = "${analytics.totalStudents}",
                        subtitle = "Tap to manage roster",
                        icon = Icons.Default.People,
                        accentColor = Color(0xFF0F766E),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openStudentRoster() }
                    )
                    QuickStatCard(
                        title = "Pending Grading",
                        value = "${analytics.pendingGradingCount}",
                        subtitle = "${analytics.totalAssignments} total tasks",
                        icon = Icons.Default.Assignment,
                        accentColor = if (analytics.pendingGradingCount > 0) StatusWarning else StatusSuccess,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectTab(LmsTab.ASSIGNMENTS) }
                    )
                }
            }
        }

        // Grade Distribution Histogram Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Grade Distribution",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "High: ${analytics.highestPercentage.toInt()}%  •  Low: ${analytics.lowestPercentage.toInt()}%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    GradeDistributionBarChart(
                        distribution = analytics.distribution,
                        totalStudents = analytics.totalStudents
                    )
                }
            }
        }

        // Active Assignments Preview
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Upcoming & Recent Assignments",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "View All",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { viewModel.selectTab(LmsTab.ASSIGNMENTS) }
                                .padding(4.dp)
                        )
                    }

                    if (uiState.assignments.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No assignments created yet. Tap '+ New Assignment' above to create one.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        uiState.assignments.take(3).forEach { assignment ->
                            val subs = uiState.submissions.filter { it.assignmentId == assignment.id }
                            val graded = subs.count { it.status == com.example.data.entity.SubmissionStatus.GRADED }
                            val total = uiState.students.size.coerceAtLeast(1)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.selectTab(LmsTab.ASSIGNMENTS) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AssignmentTypeBadge(type = assignment.type)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = assignment.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Due: ${assignment.dueDate} • ${assignment.maxPoints.toInt()} pts",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "$graded/$total Graded",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (graded == total) StatusSuccess else EduPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Performers & Honor Roll
        if (uiState.studentGradeSummaries.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Top Academic Performers",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        uiState.studentGradeSummaries.take(3).forEachIndexed { index, summary ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.openStudentProfile(summary) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(28.dp)
                                )
                                StudentAvatar(
                                    name = summary.student.name,
                                    colorHex = summary.student.avatarColorHex,
                                    photoPath = summary.student.photoPath,
                                    sizeDp = 34
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = summary.student.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "HW: ${summary.homeworkCompletionRate.toInt()}% • Attendance: ${summary.attendanceRate.toInt()}%",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                GradeBadge(
                                    letterGrade = summary.letterGrade,
                                    percentage = summary.percentage
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
