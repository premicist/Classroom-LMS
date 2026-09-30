package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.SubmissionStatus
import com.example.ui.components.GradeBadge
import com.example.ui.components.GradeDistributionBarChart
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.EduPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.ClassroomViewModel
import com.example.ui.viewmodel.LmsUiState
import com.example.ui.viewmodel.StudentGradeSummary
import androidx.compose.material3.Switch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import com.example.data.entity.TermWeightConfig

@Composable
fun GradebookScreen(
    uiState: LmsUiState,
    viewModel: ClassroomViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var expandedStudentId by remember { mutableStateOf<Long?>(null) }
    var isInlineGradeMode by remember { mutableStateOf(false) }

    val filteredSummaries = if (searchQuery.isNotEmpty()) {
        uiState.studentGradeSummaries.filter {
            it.student.name.contains(searchQuery, ignoreCase = true) ||
            it.student.studentNumber.contains(searchQuery, ignoreCase = true)
        }
    } else uiState.studentGradeSummaries

    val analytics = uiState.analytics

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Gradebook Analytics Header Card
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
                        Column {
                            Text(
                                text = "Real-Time Gradebook",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Weighted score & letter grade calculation",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.openTermWeightingDialog() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Weights", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val text = viewModel.generateFormattedGradebookReportText()
                                    viewModel.openExportReport(text)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Class Summary Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Class Average", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${analytics.averagePercentage}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EduPrimary)
                        }
                        Column {
                            Text(text = "Average GPA", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${"%.2f".format(analytics.averageGpa)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column {
                            Text(text = "Highest", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${analytics.highestPercentage.toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StatusSuccess)
                        }
                        Column {
                            Text(text = "Lowest", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${analytics.lowestPercentage.toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StatusError)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    GradeDistributionBarChart(
                        distribution = analytics.distribution,
                        totalStudents = analytics.totalStudents
                    )
                }
            }
        }

        // Search & Add Student Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by student name or ID...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Button(
                    onClick = { viewModel.openAddStudent() },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add Student")
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Inline Quick-Grade Mode", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Switch(checked = isInlineGradeMode, onCheckedChange = { isInlineGradeMode = it })
            }
        }

        // Student Grade Cards List
        itemsIndexed(filteredSummaries, key = { _, summary -> summary.student.id }) { index, summary ->
            val isExpanded = expandedStudentId == summary.student.id
            val assignMap = uiState.assignments.associateBy { it.id }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedStudentId = if (isExpanded) null else summary.student.id },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rank Index
                        Text(
                            text = "#${index + 1}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(26.dp)
                        )

                        StudentAvatar(
                            name = summary.student.name,
                            colorHex = summary.student.avatarColorHex,
                            photoPath = summary.student.photoPath,
                            sizeDp = 38
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = summary.student.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (summary.isAtRisk) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "At Risk",
                                        tint = StatusError,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "ID: ${summary.student.studentNumber} • GPA: ${"%.2f".format(summary.gpa)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Letter Grade Badge
                        GradeBadge(
                            letterGrade = summary.letterGrade,
                            percentage = summary.percentage
                        )

                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Expanded Assignments Breakdown & Performance Stats
                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Stats Chips Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MiniMetricPill(label = "HW Completion", value = "${summary.homeworkCompletionRate.toInt()}%", color = EduPrimary)
                                MiniMetricPill(label = "Attendance", value = "${summary.attendanceRate.toInt()}%", color = StatusSuccess)
                                MiniMetricPill(label = "Missing Tasks", value = "${summary.missingCount}", color = if (summary.missingCount > 0) StatusError else StatusSuccess)
                            }

                            if (summary.riskReasons.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Alerts: ${summary.riskReasons.joinToString(" • ")}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StatusError
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isInlineGradeMode) "Quick Grade Tasks" else "Assignments & Exam Scores",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            if (summary.submissions.isEmpty()) {
                                Text(
                                    text = "No assignments recorded.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                summary.submissions.forEach { sub ->
                                    val assign = assignMap[sub.assignmentId]
                                    if (assign != null) {
                                        if (isInlineGradeMode) {
                                            var scoreInput by remember(sub.id) { mutableStateOf(sub.score?.toInt()?.toString() ?: "") }
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = assign.title,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "Max: ${assign.maxPoints.toInt()}",
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    OutlinedTextField(
                                                        value = scoreInput,
                                                        onValueChange = { scoreInput = it },
                                                        label = { Text("Score", fontSize = 10.sp) },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                                        modifier = Modifier.width(72.dp),
                                                        singleLine = true
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Button(
                                                        onClick = {
                                                            val s = scoreInput.toDoubleOrNull()
                                                            if (s != null) {
                                                                viewModel.saveSubmission(sub.copy(score = s, status = SubmissionStatus.GRADED))
                                                            }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.height(36.dp)
                                                    ) {
                                                        Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                        } else {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = assign.title,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = if (sub.status == SubmissionStatus.GRADED && sub.score != null) {
                                                        "${sub.score.toInt()}/${assign.maxPoints.toInt()} pts"
                                                    } else sub.status.displayName,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (sub.status) {
                                                        SubmissionStatus.GRADED -> StatusSuccess
                                                        SubmissionStatus.MISSING -> StatusError
                                                        SubmissionStatus.LATE -> StatusWarning
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { viewModel.openStudentProfile(summary) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text("View Full Student Profile & Reports", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isTermWeightingDialogOpen) {
        TermWeightingDialog(
            initialConfig = uiState.termWeightConfig,
            onDismiss = { viewModel.closeTermWeightingDialog() },
            onSave = { config -> viewModel.saveTermWeightConfig(config) }
        )
    }
}

@Composable
private fun MiniMetricPill(
    label: String,
    value: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TermWeightingDialog(
    initialConfig: TermWeightConfig,
    onDismiss: () -> Unit,
    onSave: (TermWeightConfig) -> Unit
) {
    var term1 by remember { mutableStateOf(initialConfig.term1Weight.toInt().toString()) }
    var term2 by remember { mutableStateOf(initialConfig.term2Weight.toInt().toString()) }
    var finalExam by remember { mutableStateOf(initialConfig.finalExamWeight.toInt().toString()) }
    var isEnabled by remember { mutableStateOf(initialConfig.isEnabled) }

    val t1Val = term1.toDoubleOrNull() ?: 0.0
    val t2Val = term2.toDoubleOrNull() ?: 0.0
    val finalVal = finalExam.toDoubleOrNull() ?: 0.0
    val totalSum = t1Val + t2Val + finalVal
    val isValidSum = totalSum == 100.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Term & Semester Weighting", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Configure composite percentage weights for calculating NEB final academic standing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isEnabled = !isEnabled }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Weighted Calculation", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                }

                if (isEnabled) {
                    OutlinedTextField(
                        value = term1,
                        onValueChange = { term1 = it },
                        label = { Text("Term 1 Weight (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = term2,
                        onValueChange = { term2 = it },
                        label = { Text("Term 2 Weight (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = finalExam,
                        onValueChange = { finalExam = it },
                        label = { Text("Final Exam / Assessment Weight (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Total Weight Sum: ${totalSum.toInt()}% (Must equal 100%)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isValidSum) StatusSuccess else StatusError
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val config = TermWeightConfig(
                        term1Weight = t1Val,
                        term2Weight = t2Val,
                        finalExamWeight = finalVal,
                        isEnabled = isEnabled
                    )
                    onSave(config)
                },
                enabled = !isEnabled || isValidSum
            ) {
                Text("Save Configuration")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
