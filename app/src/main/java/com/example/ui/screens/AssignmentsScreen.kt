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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.entity.AssignmentEntity
import com.example.data.entity.AssignmentType
import com.example.data.entity.StudentEntity
import com.example.data.entity.SubmissionEntity
import com.example.data.entity.SubmissionStatus
import com.example.ui.components.AssignmentTypeBadge
import com.example.ui.components.StudentAvatar
import com.example.ui.components.SubmissionStatusBadge
import com.example.ui.theme.EduPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.ClassroomViewModel
import com.example.ui.viewmodel.LmsUiState

@Composable
fun AssignmentsScreen(
    uiState: LmsUiState,
    viewModel: ClassroomViewModel,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Assignments, 1 = Submissions Checklist Matrix
    var selectedAssignmentForMatrix by remember { mutableStateOf<AssignmentEntity?>(null) }
    var assignmentToDelete by remember { mutableStateOf<AssignmentEntity?>(null) }

    // Synchronize default selected assignment for matrix
    val currentMatrixAssignment = selectedAssignmentForMatrix ?: uiState.assignments.firstOrNull()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sub-tabs: Assignment Cards vs Submissions Checklist Matrix
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = { Text("Assignments (${uiState.assignments.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = { Text("Submissions Checklist", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedSubTab == 0) {
                // --- TAB 0: ASSIGNMENTS LIST ---
                AssignmentsListView(
                    assignments = uiState.assignments,
                    students = uiState.students,
                    submissions = uiState.submissions,
                    filterType = uiState.assignmentFilterType,
                    onFilterChange = { viewModel.setAssignmentFilter(it, uiState.assignmentFilterStatus) },
                    onEdit = { viewModel.openEditAssignment(it) },
                    onDelete = { assignmentToDelete = it },
                    onViewChecklist = { assignment ->
                        selectedAssignmentForMatrix = assignment
                        selectedSubTab = 1
                    }
                )
            } else {
                // --- TAB 1: SUBMISSION CHECKLIST MATRIX ---
                SubmissionsChecklistMatrixView(
                    assignments = uiState.assignments,
                    students = uiState.students,
                    submissions = uiState.submissions,
                    selectedAssignment = currentMatrixAssignment,
                    onSelectAssignment = { selectedAssignmentForMatrix = it },
                    onGradeSubmission = { sub, assign, student ->
                        viewModel.openGradingDialog(sub, assign, student)
                    },
                    onToggleCheck = { sub ->
                        viewModel.toggleSubmissionCheck(sub)
                    },
                    onMarkAll = { assignId, status ->
                        viewModel.quickMarkAllSubmissionsForAssignment(assignId, status)
                    }
                )
            }
        }

        // Floating Action Button to Add New Assignment
        FloatingActionButton(
            onClick = { viewModel.openAddAssignment() },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 75.dp, end = 20.dp)
                .testTag("add_assignment_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Assignment")
        }
    }

    // Delete Confirmation Dialog
    if (assignmentToDelete != null) {
        AlertDialog(
            onDismissRequest = { assignmentToDelete = null },
            title = { Text("Delete Assignment") },
            text = { Text("Are you sure you want to delete '${assignmentToDelete?.title}'? All student submissions for this assignment will also be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        assignmentToDelete?.let { viewModel.deleteAssignment(it.id) }
                        assignmentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { assignmentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AssignmentsListView(
    assignments: List<AssignmentEntity>,
    students: List<StudentEntity>,
    submissions: List<SubmissionEntity>,
    filterType: AssignmentType?,
    onFilterChange: (AssignmentType?) -> Unit,
    onEdit: (AssignmentEntity) -> Unit,
    onDelete: (AssignmentEntity) -> Unit,
    onViewChecklist: (AssignmentEntity) -> Unit
) {
    val filtered = if (filterType != null) {
        assignments.filter { it.type == filterType }
    } else assignments

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Pills Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChipItem(
                    label = "All Types (${assignments.size})",
                    isSelected = filterType == null,
                    onClick = { onFilterChange(null) }
                )
            }
            items(AssignmentType.entries.toTypedArray(), key = { it.name }) { type ->
                val count = assignments.count { it.type == type }
                FilterChipItem(
                    label = "${type.displayName} ($count)",
                    isSelected = filterType == type,
                    onClick = { onFilterChange(type) }
                )
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No assignments found",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Create a new assignment by tapping the '+' button below.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { assignment ->
                    val subs = submissions.filter { it.assignmentId == assignment.id }
                    val total = students.size.coerceAtLeast(1)
                    val graded = subs.count { it.status == SubmissionStatus.GRADED }
                    val submitted = subs.count { it.status == SubmissionStatus.SUBMITTED || it.status == SubmissionStatus.LATE }
                    val missing = subs.count { it.status == SubmissionStatus.MISSING }
                    val progressFraction = (graded + submitted).toFloat() / total.toFloat()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AssignmentTypeBadge(type = assignment.type)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Due ${assignment.dueDate}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    IconButton(
                                        onClick = { onEdit(assignment) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDelete(assignment) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = StatusError,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = assignment.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (assignment.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = assignment.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Max Points: ${assignment.maxPoints.toInt()} pts  •  Weight: ${assignment.weightPercentage.toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$graded/$total Graded",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (graded == total) StatusSuccess else EduPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = if (graded == total) StatusSuccess else EduPrimary,
                                trackColor = Color(0xFFE2E8F0)
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onViewChecklist(assignment) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Grade,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check Submissions & Grade", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubmissionsChecklistMatrixView(
    assignments: List<AssignmentEntity>,
    students: List<StudentEntity>,
    submissions: List<SubmissionEntity>,
    selectedAssignment: AssignmentEntity?,
    onSelectAssignment: (AssignmentEntity) -> Unit,
    onGradeSubmission: (SubmissionEntity, AssignmentEntity, StudentEntity) -> Unit,
    onToggleCheck: (SubmissionEntity) -> Unit,
    onMarkAll: (Long, SubmissionStatus) -> Unit
) {
    if (assignments.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Create an assignment first to view submissions checklist.")
        }
        return
    }

    val currentAssign = selectedAssignment ?: assignments.first()
    val assignSubs = submissions.filter { it.assignmentId == currentAssign.id }
    val subMap = assignSubs.associateBy { it.studentId }

    Column(modifier = Modifier.fillMaxSize()) {
        // Assignment Picker Dropdown / Pill row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(assignments) { assign ->
                val isSelected = assign.id == currentAssign.id
                FilterChipItem(
                    label = assign.title,
                    isSelected = isSelected,
                    onClick = { onSelectAssignment(assign) }
                )
            }
        }

        // Active Assignment Info Card & Batch Action Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentAssign.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Due: ${currentAssign.dueDate} • Max: ${currentAssign.maxPoints.toInt()} pts (${currentAssign.type.displayName})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onMarkAll(currentAssign.id, SubmissionStatus.SUBMITTED) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text("All Submitted", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onMarkAll(currentAssign.id, SubmissionStatus.GRADED) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text("All Graded", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onMarkAll(currentAssign.id, SubmissionStatus.MISSING) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text("Unset Missing", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusError)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Student Submissions Checklist List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(students, key = { it.id }) { student ->
                val sub = subMap[student.id] ?: SubmissionEntity(
                    assignmentId = currentAssign.id,
                    studentId = student.id,
                    classroomId = currentAssign.classroomId,
                    status = SubmissionStatus.PENDING
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(0.5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Checkbox Icon
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (sub.isChecked) StatusSuccess.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { onToggleCheck(sub) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (sub.isChecked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Checked",
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))
                        StudentAvatar(
                            name = student.name,
                            colorHex = student.avatarColorHex,
                            photoPath = student.photoPath,
                            sizeDp = 32
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = student.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = student.studentNumber,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Status Badge with Tap to Grade
                        SubmissionStatusBadge(
                            status = sub.status,
                            score = sub.score,
                            maxPoints = currentAssign.maxPoints,
                            onClick = { onGradeSubmission(sub, currentAssign, student) }
                        )

                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { onGradeSubmission(sub, currentAssign, student) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Grade",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
