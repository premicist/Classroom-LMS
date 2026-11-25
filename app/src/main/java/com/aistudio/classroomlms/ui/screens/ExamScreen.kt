package com.aistudio.classroomlms.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.classroomlms.data.entity.ExamCategory
import com.aistudio.classroomlms.data.entity.ExamEntity
import com.aistudio.classroomlms.data.entity.ExamMarkEntity
import com.aistudio.classroomlms.data.entity.StudentEntity
import com.aistudio.classroomlms.domain.merit.LeaderboardSummary
import com.aistudio.classroomlms.domain.merit.StudentExamAggregate
import com.aistudio.classroomlms.domain.merit.SubjectTopper
import com.aistudio.classroomlms.ui.components.StudentSortControl
import com.aistudio.classroomlms.ui.theme.statusSuccess
import com.aistudio.classroomlms.ui.util.LocalWindowSizeCategory
import com.aistudio.classroomlms.ui.util.StudentSortOrder
import com.aistudio.classroomlms.ui.util.adaptiveHorizontalPadding
import com.aistudio.classroomlms.ui.util.sortedByStudent
import com.aistudio.classroomlms.ui.viewmodel.ClassroomViewModel
import com.aistudio.classroomlms.ui.viewmodel.ExamHubTab
import com.aistudio.classroomlms.ui.viewmodel.ExamUiState
import com.aistudio.classroomlms.ui.viewmodel.ExamViewModel
import com.aistudio.classroomlms.ui.viewmodel.LmsUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExamScreen(
    uiState: LmsUiState,
    viewModel: ClassroomViewModel,
    modifier: Modifier = Modifier,
    examViewModel: ExamViewModel = hiltViewModel()
) {
    val examUiState by examViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val classroomName = uiState.activeClassroom?.name ?: ""

    val categoryTabs = listOf(
        ExamCategory.CLASS_TEST to "Class Test",
        ExamCategory.TERMINAL to "Terminal Exam",
        ExamCategory.PRE_BOARD to "Pre-Board"
    )

    // Full Marks Entry Workspace if active
    if (examUiState.gradingExam != null) {
        FullMarksEntryWorkspace(
            exam = examUiState.gradingExam!!,
            uiState = uiState,
            examViewModel = examViewModel,
            examUiState = examUiState,
            onBack = { examViewModel.closeGradingWorkspace() },
            modifier = modifier
        )
        return
    }

    // Add / Edit Exam Dialog wired to ExamViewModel
    if (examUiState.isAddEditExamOpen) {
        val activeClassId = examUiState.activeClassroomId ?: uiState.activeClassroom?.id ?: 1L
        AddEditExamDialog(
            category = examUiState.selectedExamCategory,
            initialExam = examUiState.editingExam,
            classroomId = activeClassId,
            onDismiss = { examViewModel.closeAddEditExamDialog() },
            onSave = { exam: ExamEntity ->
                examViewModel.saveExam(exam)
                examViewModel.closeAddEditExamDialog()
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Examination Hub Feature Tabs: Marks, Toppers, DateSheet, Marksheets
        val hubTabs = listOf(
            ExamHubTab.MARKS to "Marks & Papers",
            ExamHubTab.LEADERBOARD to "Toppers Podium",
            ExamHubTab.DATESHEET to "DateSheet Routine",
            ExamHubTab.REPORTS to "Marksheets & Dispatch"
        )

        ScrollableTabRow(
            selectedTabIndex = hubTabs.indexOfFirst { it.first == examUiState.selectedHubTab }.coerceAtLeast(0),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 8.dp
        ) {
            hubTabs.forEach { (tab, label) ->
                Tab(
                    selected = examUiState.selectedHubTab == tab,
                    onClick = { examViewModel.selectHubTab(tab) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            when (tab) {
                                ExamHubTab.MARKS -> Icon(Icons.Default.FormatListNumbered, null, modifier = Modifier.size(14.dp))
                                ExamHubTab.LEADERBOARD -> Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(14.dp), tint = Color(0xFFD97706))
                                ExamHubTab.DATESHEET -> Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp))
                                ExamHubTab.REPORTS -> Icon(Icons.Default.Assessment, null, modifier = Modifier.size(14.dp))
                            }
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (examUiState.selectedHubTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                )
            }
        }

        // Sleek Category Filter Chips Row (replaces bulky TabRow to reclaim viewport space)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categoryTabs) { (category, label) ->
                val isSelected = examUiState.selectedExamCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { examViewModel.selectCategory(category) },
                    label = {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(30.dp)
                )
            }
        }

        val examsForCategory = examUiState.exams.filter { it.category == examUiState.selectedExamCategory }

        when (examUiState.selectedHubTab) {
            ExamHubTab.MARKS -> {
                ExamMarksListView(
                    examsForCategory = examsForCategory,
                    uiState = uiState,
                    viewModel = viewModel,
                    examViewModel = examViewModel,
                    examUiState = examUiState
                )
            }
            ExamHubTab.LEADERBOARD -> {
                ToppersPodiumView(
                    summary = examUiState.leaderboardSummary,
                    category = examUiState.selectedExamCategory,
                    exams = examsForCategory
                )
            }
            ExamHubTab.DATESHEET -> {
                ExamDateSheetView(
                    exams = examsForCategory,
                    category = examUiState.selectedExamCategory,
                    onAddExam = { examViewModel.openAddEditExamDialog(examUiState.selectedExamCategory) }
                )
            }
            ExamHubTab.REPORTS -> {
                TerminalMarksheetsView(
                    summary = examUiState.leaderboardSummary,
                    classroomName = classroomName,
                    onSendWhatsApp = { agg -> examViewModel.sendWhatsAppReport(context, agg, classroomName) },
                    onSendSms = { agg -> examViewModel.sendSmsReport(context, agg, classroomName) }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 1. MARKS ENTRY VIEW
// -------------------------------------------------------------
@Composable
fun ExamMarksListView(
    examsForCategory: List<ExamEntity>,
    uiState: LmsUiState,
    viewModel: ClassroomViewModel,
    examViewModel: ExamViewModel,
    examUiState: ExamUiState
) {
    if (examsForCategory.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No ${examUiState.selectedExamCategory.displayName}s found",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Create a new exam by tapping the '+' button below.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { examViewModel.openAddEditExamDialog(examUiState.selectedExamCategory) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New ${examUiState.selectedExamCategory.displayName}")
                }
            }
        }
    } else {
        val windowSize = LocalWindowSizeCategory.current
        val isCompact = windowSize.isCompact
        val horizontalPadding = windowSize.adaptiveHorizontalPadding()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = horizontalPadding, top = 12.dp, end = horizontalPadding, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${examUiState.selectedExamCategory.displayName} Papers (${examsForCategory.size})",
                        fontSize = if (isCompact) 14.sp else 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = { examViewModel.openAddEditExamDialog(examUiState.selectedExamCategory) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = if (isCompact) 8.dp else 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Add Paper", fontSize = if (isCompact) 11.sp else 12.sp)
                    }
                }
            }

            items(examsForCategory, key = { it.id }) { exam ->
                ExamRecordCard(
                    exam = exam,
                    uiState = uiState,
                    viewModel = viewModel,
                    examViewModel = examViewModel,
                    examUiState = examUiState
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// -------------------------------------------------------------
// 2. TOPPERS PODIUM & MERIT LEADERBOARD
// -------------------------------------------------------------
@Composable
fun ToppersPodiumView(
    summary: LeaderboardSummary,
    category: ExamCategory,
    exams: List<ExamEntity>
) {
    if (summary.rankedStudents.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color(0xFFD97706))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No Marks Recorded Yet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Enter marks for ${category.displayName} papers to calculate class toppers and merit rankings automatically.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Podium Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "🏆 Class Merit Podium",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "${category.displayName} • ${summary.rankedStudents.size} Students Evaluated",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Avg: ${String.format(Locale.US, "%.1f", summary.classAveragePercentage)}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Pass: ${String.format(Locale.US, "%.0f", summary.overallPassRate)}%",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Top 3 Podium Cards
        if (summary.topThree.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    summary.topThree.take(3).forEach { student ->
                        PodiumCard(
                            student = student,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Subject-Wise Toppers
        if (summary.subjectToppers.isNotEmpty()) {
            item {
                Text(
                    "🌟 Subject-Wise Toppers",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(summary.subjectToppers) { topper ->
                        SubjectTopperCard(topper = topper)
                    }
                }
            }
        }

        // Full Merit Table
        item {
            Text(
                "📜 Complete Merit Roll",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(summary.rankedStudents, key = { it.studentId }) { student ->
            MeritRollRow(student = student)
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

@Composable
fun PodiumCard(student: StudentExamAggregate, modifier: Modifier = Modifier) {
    val (badgeText, badgeColor, borderColor) = when (student.rank) {
        1 -> Triple("🥇 1st Rank", Color(0xFFFEF3C7), Color(0xFFD97706))
        2 -> Triple("🥈 2nd Rank", Color(0xFFF1F5F9), Color(0xFF64748B))
        3 -> Triple("🥉 3rd Rank", Color(0xFFFFEDD5), Color(0xFFC2410C))
        else -> Triple("#${student.rank}", Color.LightGray, Color.Gray)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = badgeColor),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(badgeText, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = borderColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                student.studentName,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("Roll: ${student.studentNumber}", fontSize = 10.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "${String.format(Locale.US, "%.1f", student.percentage)}%",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = borderColor
            )
            Text(
                "GPA ${String.format(Locale.US, "%.2f", student.gpa)} (${student.letterGrade})",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun SubjectTopperCard(topper: SubjectTopper) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        modifier = Modifier.width(160.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                topper.examTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                topper.studentName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("Roll: ${topper.studentNumber}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${topper.marksObtained.toInt()} / ${topper.fullMarks.toInt()} (${String.format(Locale.US, "%.0f", topper.percentage)}%)",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color(0xFF16A34A)
            )
        }
    }
}

@Composable
fun MeritRollRow(student: StudentExamAggregate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when (student.rank) {
                            1 -> Color(0xFFFEF3C7)
                            2 -> Color(0xFFF1F5F9)
                            3 -> Color(0xFFFFEDD5)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${student.rank}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = when (student.rank) {
                        1 -> Color(0xFFD97706)
                        2 -> Color(0xFF64748B)
                        3 -> Color(0xFFC2410C)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(student.studentName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "Roll: ${student.studentNumber} • Total: ${String.format(Locale.US, "%.1f", student.totalMarksObtained)} / ${student.totalFullMarks.toInt()}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${String.format(Locale.US, "%.1f", student.percentage)}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (student.isPassed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(
                    "GPA ${String.format(Locale.US, "%.2f", student.gpa)} • ${student.letterGrade}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 3. EXAM DATESHEET & ROUTINE VIEW (From One Flawless ERP)
// -------------------------------------------------------------
@Composable
fun ExamDateSheetView(
    exams: List<ExamEntity>,
    category: ExamCategory,
    onAddExam: () -> Unit
) {
    val sortedExams = exams.sortedBy { it.date }

    if (sortedExams.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Text("No Exam Schedule Configured", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Add subjects and scheduled dates to build the Exam Routine / DateSheet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onAddExam, shape = RoundedCornerShape(10.dp)) {
                    Text("Add Exam Subject")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "📅 ${category.displayName} DateSheet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "${sortedExams.size} Papers Scheduled",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onAddExam, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Subject", fontSize = 11.sp)
                }
            }
        }

        items(sortedExams, key = { it.id }) { exam ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date Badge
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                exam.date.takeLast(2),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                exam.date.take(7),
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(exam.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (exam.topicOrChapter.isNotBlank()) {
                            Text(
                                "📖 Syllabus: ${exam.topicOrChapter}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            "Full: ${exam.fullMarks.toInt()} • Pass: ${exam.passMarks.toInt()}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

// -------------------------------------------------------------
// 4. TERMINAL MARKSHEETS & 1-TAP PARENT DISPATCH VIEW
// -------------------------------------------------------------
@Composable
fun TerminalMarksheetsView(
    summary: LeaderboardSummary,
    classroomName: String,
    onSendWhatsApp: (StudentExamAggregate) -> Unit,
    onSendSms: (StudentExamAggregate) -> Unit
) {
    if (summary.rankedStudents.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No marks available to generate marksheets.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "📋 Terminal Marksheets & Parent Dispatch",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                "Instantly dispatch terminal progress reports to parents via WhatsApp or SMS.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(summary.rankedStudents, key = { it.studentId }) { student ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(student.studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "Roll: ${student.studentNumber} • Guardian: ${student.guardianContact.ifBlank { "N/A" }}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Rank Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "Rank #${student.rank}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Marks Breakdown Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Score", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${String.format(Locale.US, "%.1f", student.totalMarksObtained)} / ${student.totalFullMarks.toInt()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Column {
                            Text("Percentage", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${String.format(Locale.US, "%.1f", student.percentage)}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Column {
                            Text("GPA", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${String.format(Locale.US, "%.2f", student.gpa)} (${student.letterGrade})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Column {
                            Text("Status", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (student.isPassed) "PASS" else "FAIL",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (student.isPassed) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 1-Tap Communication Dispatch Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onSendSms(student) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SMS", fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { onSendWhatsApp(student) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

// -------------------------------------------------------------
// EXISTING EXAM RECORD CARD & ADD/EDIT DIALOG (PRESERVED)
// -------------------------------------------------------------
@Composable
fun ExamRecordCard(
    exam: ExamEntity,
    uiState: LmsUiState,
    viewModel: ClassroomViewModel,
    examViewModel: ExamViewModel,
    examUiState: ExamUiState
) {
    var expanded by remember { mutableStateOf(false) }
    var studentSortOrder by rememberSaveable { mutableStateOf(StudentSortOrder.NAME) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val marks = examUiState.examMarks.filter { it.examId == exam.id }
    val students = if (examUiState.students.isNotEmpty()) examUiState.students else uiState.students
    val sortedStudents = students.sortedByStudent(studentSortOrder) { it }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Exam", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${exam.title}' and all associated student marks?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        examViewModel.deleteExam(exam)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val totalStudents = students.size.coerceAtLeast(1)
    val gradedCount = marks.count { it.marksObtained != null }
    val passedCount = marks.count { it.marksObtained != null && it.marksObtained >= exam.passMarks }

    val isCompact = LocalWindowSizeCategory.current.isCompact

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(if (isCompact) 12.dp else 16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exam.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isCompact) 15.sp else 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (exam.topicOrChapter.isNotBlank()) {
                        Text(
                            text = "Topic: ${exam.topicOrChapter}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        examViewModel.openAddEditExamDialog(exam.category, exam)
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Exam", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Exam",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Info Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoBadge(label = "Date", value = exam.date)
                InfoBadge(label = "Full", value = "${exam.fullMarks.toInt()}")
                InfoBadge(label = "Pass", value = "${exam.passMarks.toInt()}")
                InfoBadge(label = "Graded", value = "$gradedCount/$totalStudents")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions: Export, Quick Preview, Enter / Grade Marks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val context = LocalContext.current
                OutlinedButton(
                    onClick = { examViewModel.exportExamReport(context, exam) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export PDF", fontSize = 11.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(if (expanded) "Hide Preview" else "Quick Preview", fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = { examViewModel.openGradingWorkspace(exam) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Enter / Grade Marks", fontSize = 12.sp)
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Student Marks ($passedCount passed)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StudentSortControl(
                        order = studentSortOrder,
                        onOrderSelected = { studentSortOrder = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                sortedStudents.forEach { student ->
                    val markRecord = marks.find { it.studentId == student.id }
                    MarksEntryRow(
                        student = student,
                        exam = exam,
                        markRecord = markRecord,
                        onSaveMark = { mark -> examViewModel.saveExamMark(mark) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
fun InfoBadge(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
            Text(
                text = "$label: ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MarksEntryRow(
    student: StudentEntity,
    exam: ExamEntity,
    markRecord: ExamMarkEntity?,
    onSaveMark: (ExamMarkEntity) -> Unit
) {
    var textValue by remember(markRecord?.marksObtained) {
        mutableStateOf(markRecord?.marksObtained?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }
    var isAbsent by remember(markRecord?.isAbsent) {
        mutableStateOf(markRecord?.isAbsent ?: false)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isAbsent) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = student.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Roll: ${student.studentNumber.ifBlank { student.id.toString() }}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = {
                    val newAbsent = !isAbsent
                    isAbsent = newAbsent
                    if (newAbsent) {
                        textValue = ""
                        onSaveMark(
                            ExamMarkEntity(
                                id = markRecord?.id ?: 0L,
                                examId = exam.id,
                                studentId = student.id,
                                marksObtained = null,
                                isAbsent = true
                            )
                        )
                    } else {
                        onSaveMark(
                            ExamMarkEntity(
                                id = markRecord?.id ?: 0L,
                                examId = exam.id,
                                studentId = student.id,
                                marksObtained = null,
                                isAbsent = false
                            )
                        )
                    }
                },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isAbsent) "ABSENT" else "Absent?",
                    fontSize = 10.sp,
                    color = if (isAbsent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isAbsent) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            OutlinedTextField(
                value = textValue,
                onValueChange = { input ->
                    if (input.isEmpty() || input.toDoubleOrNull() != null) {
                        textValue = input
                        val markDouble = input.toDoubleOrNull()
                        if (markDouble != null) {
                            isAbsent = false
                            onSaveMark(
                                ExamMarkEntity(
                                    id = markRecord?.id ?: 0L,
                                    examId = exam.id,
                                    studentId = student.id,
                                    marksObtained = markDouble.coerceIn(0.0, exam.fullMarks),
                                    isAbsent = false
                                )
                            )
                        } else if (input.isEmpty()) {
                            onSaveMark(
                                ExamMarkEntity(
                                    id = markRecord?.id ?: 0L,
                                    examId = exam.id,
                                    studentId = student.id,
                                    marksObtained = null,
                                    isAbsent = false
                                )
                            )
                        }
                    }
                },
                placeholder = { Text("-", fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .width(78.dp)
                    .height(48.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                shape = RoundedCornerShape(8.dp),
                enabled = !isAbsent
            )

            Spacer(modifier = Modifier.width(6.dp))

            val currentScore = markRecord?.marksObtained
            val isPassed = currentScore != null && currentScore >= exam.passMarks
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isAbsent -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            currentScore == null -> MaterialTheme.colorScheme.surfaceVariant
                            isPassed -> MaterialTheme.colorScheme.statusSuccess.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        isAbsent -> "A"
                        currentScore == null -> "-"
                        isPassed -> "P"
                        else -> "F"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isAbsent -> MaterialTheme.colorScheme.error
                        currentScore == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        isPassed -> MaterialTheme.colorScheme.statusSuccess
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 5. FULL-SCREEN DEDICATED MARKS ENTRY WORKSPACE
// -------------------------------------------------------------
@Composable
fun FullMarksEntryWorkspace(
    exam: ExamEntity,
    uiState: LmsUiState,
    examViewModel: ExamViewModel,
    examUiState: ExamUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler(onBack = onBack)

    var studentSortOrder by rememberSaveable { mutableStateOf(StudentSortOrder.NAME) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }

    val allStudents = if (examUiState.students.isNotEmpty()) examUiState.students else uiState.students
    val marks = examUiState.examMarks.filter { it.examId == exam.id }

    val sortedStudents = allStudents.sortedByStudent(studentSortOrder) { it }
    val filteredStudents = remember(sortedStudents, searchQuery) {
        if (searchQuery.isBlank()) {
            sortedStudents
        } else {
            val q = searchQuery.trim().lowercase(Locale.getDefault())
            sortedStudents.filter {
                it.name.lowercase(Locale.getDefault()).contains(q) ||
                it.studentNumber.lowercase(Locale.getDefault()).contains(q)
            }
        }
    }

    val totalStudents = allStudents.size.coerceAtLeast(1)
    val gradedCount = marks.count { it.marksObtained != null || it.isAbsent }
    val absentCount = marks.count { it.isAbsent }
    val passedCount = marks.count { it.marksObtained != null && !it.isAbsent && it.marksObtained >= exam.passMarks }
    val failedCount = marks.count { it.marksObtained != null && !it.isAbsent && it.marksObtained < exam.passMarks }
    val progressFraction = (gradedCount.toFloat() / totalStudents.toFloat()).coerceIn(0f, 1f)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // 1. Sleek Top Bar
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exam.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${exam.category.displayName} • Full: ${exam.fullMarks.toInt()} • Pass: ${exam.passMarks.toInt()}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                            Icon(
                                if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search Student",
                                tint = if (isSearchVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        StudentSortControl(
                            order = studentSortOrder,
                            onOrderSelected = { studentSortOrder = it }
                        )

                        IconButton(onClick = { examViewModel.exportExamReport(context, exam) }) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Export PDF",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (isSearchVisible) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by student name or roll number...", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // 2. Grading Progress & Statistics Strip
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Grading Progress: $gradedCount / $totalStudents (${(progressFraction * 100).toInt()}%)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.statusSuccess.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "$passedCount Pass",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.statusSuccess,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (failedCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "$failedCount Fail",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (absentCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF97316).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "$absentCount Absent",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEA580C),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // 3. Virtualized Students LazyColumn with imePadding and bottom spacing
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredStudents, key = { it.id }) { student ->
                    val markRecord = marks.find { it.studentId == student.id }
                    FullMarksEntryStudentCard(
                        student = student,
                        exam = exam,
                        markRecord = markRecord,
                        onSaveMark = { mark -> examViewModel.saveExamMark(mark) }
                    )
                }
            }
        }
    }
}

@Composable
fun FullMarksEntryStudentCard(
    student: StudentEntity,
    exam: ExamEntity,
    markRecord: ExamMarkEntity?,
    onSaveMark: (ExamMarkEntity) -> Unit
) {
    var textValue by remember(markRecord?.marksObtained) {
        mutableStateOf(markRecord?.marksObtained?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }
    var isAbsent by remember(markRecord?.isAbsent) {
        mutableStateOf(markRecord?.isAbsent ?: false)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAbsent) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Student Initials / Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isAbsent) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "S" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isAbsent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Student Name & Roll Number
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Roll #${student.studentNumber.ifBlank { student.id.toString() }}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(3.dp))
                // Absent Toggle Chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAbsent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.clickable {
                        val newAbsent = !isAbsent
                        isAbsent = newAbsent
                        if (newAbsent) {
                            textValue = ""
                            onSaveMark(
                                ExamMarkEntity(
                                    id = markRecord?.id ?: 0L,
                                    examId = exam.id,
                                    studentId = student.id,
                                    marksObtained = null,
                                    isAbsent = true
                                )
                            )
                        } else {
                            onSaveMark(
                                ExamMarkEntity(
                                    id = markRecord?.id ?: 0L,
                                    examId = exam.id,
                                    studentId = student.id,
                                    marksObtained = null,
                                    isAbsent = false
                                )
                            )
                        }
                    }
                ) {
                    Text(
                        text = if (isAbsent) "ABSENT" else "Mark Absent",
                        fontSize = 10.sp,
                        fontWeight = if (isAbsent) FontWeight.Bold else FontWeight.Medium,
                        color = if (isAbsent) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right side: Spacious Marks Box + Max Suffix + Pass/Fail Pill
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.toDoubleOrNull() != null) {
                                textValue = input
                                val markDouble = input.toDoubleOrNull()
                                if (markDouble != null) {
                                    isAbsent = false
                                    onSaveMark(
                                        ExamMarkEntity(
                                            id = markRecord?.id ?: 0L,
                                            examId = exam.id,
                                            studentId = student.id,
                                            marksObtained = markDouble.coerceIn(0.0, exam.fullMarks),
                                            isAbsent = false
                                        )
                                    )
                                } else if (input.isEmpty()) {
                                    onSaveMark(
                                        ExamMarkEntity(
                                            id = markRecord?.id ?: 0L,
                                            examId = exam.id,
                                            studentId = student.id,
                                            marksObtained = null,
                                            isAbsent = false
                                        )
                                    )
                                }
                            }
                        },
                        placeholder = { Text("-", fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .width(86.dp)
                            .height(50.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isAbsent
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "/ ${exam.fullMarks.toInt()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Pass / Fail / Absent status badge
                val currentScore = markRecord?.marksObtained
                val isPassed = currentScore != null && currentScore >= exam.passMarks
                val percentage = if (currentScore != null && exam.fullMarks > 0) (currentScore / exam.fullMarks * 100).toInt() else null

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        isAbsent -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        currentScore == null -> MaterialTheme.colorScheme.surfaceVariant
                        isPassed -> MaterialTheme.colorScheme.statusSuccess.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = when {
                            isAbsent -> "ABSENT"
                            currentScore == null -> "PENDING"
                            isPassed -> "PASS • $percentage%"
                            else -> "FAIL • $percentage%"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isAbsent -> MaterialTheme.colorScheme.error
                            currentScore == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            isPassed -> MaterialTheme.colorScheme.statusSuccess
                            else -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
