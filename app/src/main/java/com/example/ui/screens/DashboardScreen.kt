package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.SubmissionStatus
import com.example.ui.components.AlertBanner
import com.example.ui.components.AssignmentTypeBadge
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

data class ServicePortalTile(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    uiState: LmsUiState,
    viewModel: ClassroomViewModel,
    modifier: Modifier = Modifier
) {
    val analytics = uiState.analytics

    val portalTiles = listOf(
        ServicePortalTile(
            title = "Attendance",
            icon = Icons.Default.EventAvailable,
            color = StatusSuccess,
            onClick = { viewModel.selectTab(LmsTab.ATTENDANCE) }
        ),
        ServicePortalTile(
            title = "Homework",
            icon = Icons.Default.MenuBook,
            color = EduPrimary,
            onClick = { viewModel.selectTab(LmsTab.ACADEMIC) }
        ),
        ServicePortalTile(
            title = "New Task",
            icon = Icons.Default.Add,
            color = Color(0xFF7C3AED),
            onClick = { viewModel.openAddAssignment() }
        ),
        ServicePortalTile(
            title = "Roster",
            icon = Icons.Default.People,
            color = Color(0xFF0F766E),
            onClick = { viewModel.openStudentRoster() }
        ),
        ServicePortalTile(
            title = "Timetable",
            icon = Icons.Default.Schedule,
            color = Color(0xFF0284C7),
            onClick = { viewModel.openScheduleScreen() }
        ),
        ServicePortalTile(
            title = "Planner",
            icon = Icons.Default.EventNote,
            color = Color(0xFFD97706),
            onClick = { viewModel.openPlanner() }
        ),
        ServicePortalTile(
            title = "Behavior",
            icon = Icons.Default.Warning,
            color = Color(0xFFEA580C),
            onClick = {
                val firstStudent = uiState.students.firstOrNull()
                if (firstStudent != null) viewModel.openAddDiscipline(firstStudent)
                else viewModel.showToast("Add a student to log behavior")
            }
        ),
        ServicePortalTile(
            title = "PDF Reports",
            icon = Icons.Default.Share,
            color = StatusInfo,
            onClick = { viewModel.openGenerateReportDialog() }
        ),
        ServicePortalTile(
            title = "Google Sync",
            icon = Icons.Default.CloudSync,
            color = Color(0xFF0284C7),
            onClick = {
                uiState.activeClassroom?.id?.let { viewModel.syncGoogleSheet(it) }
                    ?: viewModel.showToast("No active classroom")
            }
        ),
        ServicePortalTile(
            title = "Assessment",
            icon = Icons.Default.Grade,
            color = Color(0xFFDC2626),
            onClick = { viewModel.openLiveAssessment() }
        ),
        ServicePortalTile(
            title = "Classrooms",
            icon = Icons.Default.School,
            color = Color(0xFF475569),
            onClick = { viewModel.openClassroomManagement() }
        ),
        ServicePortalTile(
            title = "AI Insights",
            icon = Icons.Default.AutoAwesome,
            color = Color(0xFF8B5CF6),
            onClick = {
                val report = viewModel.generateProgressAnalyticsReportText()
                viewModel.openExportReport(report, title = "TCMS AI Progress Insights & Audit")
            }
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Today's Routine Hero Banner
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
                            text = "Today's Schedule Routine",
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

        // Quick-Portal Service Grid
        item {
            Column {
                Text(
                    text = "Quick Portal & Services",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("portal_service_grid"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 3
                ) {
                    portalTiles.forEach { tile ->
                        PortalTileCard(
                            tile = tile,
                            modifier = Modifier.weight(1f)
                        )
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
                    onAction = { viewModel.selectTab(LmsTab.ANALYTICS) }
                )
            }
        }

        // Class Performance Metrics
        item {
            Column {
                Text(
                    text = "Class Performance Overview",
                    fontSize = 15.sp,
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
                        value = "${"%.1f".format(analytics.averagePercentage)}%",
                        subtitle = "GPA: ${"%.2f".format(analytics.averageGpa)}",
                        icon = Icons.Default.Grade,
                        accentColor = EduPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectTab(LmsTab.ANALYTICS) }
                    )
                    QuickStatCard(
                        title = "Attendance Rate",
                        value = "${"%.1f".format(analytics.todayAttendanceRate)}%",
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
                        onClick = { viewModel.selectTab(LmsTab.ACADEMIC) }
                    )
                }
            }
        }

        // Grade Distribution Histogram Card with Rounded Formatting Fix
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
                            text = "High: ${analytics.highestPercentage.toInt()}% • Low: ${analytics.lowestPercentage.toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
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
                                .clickable { viewModel.selectTab(LmsTab.ACADEMIC) }
                                .padding(4.dp)
                        )
                    }

                    if (uiState.assignments.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No assignments created yet. Tap '+ New Task' above to create one.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        uiState.assignments.take(3).forEach { assignment ->
                            val subs = uiState.submissions.filter { it.assignmentId == assignment.id }
                            val graded = subs.count { it.status == SubmissionStatus.GRADED }
                            val total = uiState.students.size.coerceAtLeast(1)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.selectTab(LmsTab.ACADEMIC) }
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

        // Top Performers
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
                                        text = "HW: ${"%.1f".format(summary.homeworkCompletionRate)}% • Attendance: ${"%.1f".format(summary.attendanceRate)}%",
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
private fun PortalTileCard(
    tile: ServicePortalTile,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { tile.onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(tile.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tile.icon,
                    contentDescription = tile.title,
                    tint = tile.color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tile.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
