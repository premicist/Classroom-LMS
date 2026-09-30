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
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.StudentEntity
import com.example.ui.components.GradeBadge
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.EduPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorBg
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.viewmodel.ClassroomViewModel
import com.example.ui.viewmodel.LmsUiState
import com.example.ui.viewmodel.StudentTier

@Composable
fun StudentDirectoryScreen(
    uiState: LmsUiState,
    viewModel: ClassroomViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // All, Needs Support, Enrichment, Low Attendance
    var studentToDelete by remember { mutableStateOf<StudentEntity?>(null) }
    var studentToReassign by remember { mutableStateOf<StudentEntity?>(null) }

    val filters = listOf("All", "⚠️ Needs Support", "🌟 Enrichment", "Low Attendance (<85%)")

    val summaries = uiState.studentGradeSummaries
    val filteredSummaries = summaries.filter { summary ->
        val matchesSearch = if (searchQuery.isNotEmpty()) {
            summary.student.name.contains(searchQuery, ignoreCase = true) || summary.student.studentNumber.contains(searchQuery, ignoreCase = true)
        } else true

        val matchesFilter = when (selectedFilter) {
            "⚠️ Needs Support" -> summary.tier == StudentTier.EXTRA_SUPPORT
            "🌟 Enrichment" -> summary.tier == StudentTier.ENRICHMENT_NEEDED
            "Low Attendance (<85%)" -> summary.attendanceRate < 85.0
            else -> true
        }

        matchesSearch && matchesFilter
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddStudent() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Student")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by student name or ID...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 12.sp) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        if (filteredSummaries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No students match your search/filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredSummaries, key = { it.student.id }) { summary ->
                    val s = summary.student
                    val (tierBg, tierText) = when (summary.tier) {
                        StudentTier.EXTRA_SUPPORT -> StatusErrorBg to StatusError
                        StudentTier.ENRICHMENT_NEEDED -> Color(0xFFF3E8FF) to Color(0xFF7E22CE)
                        StudentTier.ON_TRACK -> StatusSuccessBg to StatusSuccess
                    }
                    val trajectory = uiState.analytics.studentTrajectories.find { it.student.id == s.id }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openStudentProfile(summary) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StudentAvatar(name = s.name, colorHex = s.avatarColorHex, photoPath = s.photoPath, sizeDp = 44)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = s.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "ID: ${s.studentNumber}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(tierBg).padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(text = summary.tier.shortLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = tierText)
                                        }
                                    }
                                }
                                GradeBadge(letterGrade = summary.letterGrade, percentage = summary.percentage)

                                var showActionMenu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { showActionMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showActionMenu,
                                        onDismissRequest = { showActionMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Edit Student") },
                                            onClick = {
                                                showActionMenu = false
                                                viewModel.openEditStudent(s)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Live Assessment") },
                                            onClick = {
                                                showActionMenu = false
                                                viewModel.openLiveAssessment(s)
                                            },
                                            leadingIcon = { Icon(Icons.Default.AddComment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Log Discipline / Merit") },
                                            onClick = {
                                                showActionMenu = false
                                                viewModel.openAddDiscipline(s)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Move to Another Class") },
                                            onClick = {
                                                showActionMenu = false
                                                studentToReassign = s
                                            },
                                            leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete Student", color = StatusError) },
                                            onClick = {
                                                showActionMenu = false
                                                studentToDelete = s
                                            },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusError) }
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("GPA", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${"%.2f".format(summary.gpa)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("HW", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${summary.homeworkCompletionRate.toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EduPrimary)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Attend", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${summary.attendanceRate.toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (summary.attendanceRate < 85.0) StatusError else StatusSuccess)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Trend", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val trendStr = if ((trajectory?.trendDelta ?: 0.0) >= 0) "↗" else "↘"
                                    Text("$trendStr ${trajectory?.trend?.label?.take(8) ?: "Steady"}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Reassign Classroom Dialog
    if (studentToReassign != null) {
        val s = studentToReassign!!
        AlertDialog(
            onDismissRequest = { studentToReassign = null },
            title = { Text("Move ${s.name} to Classroom") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select the destination classroom:", style = MaterialTheme.typography.bodyMedium)
                    uiState.classrooms.filter { it.id != s.classroomId }.forEach { targetClass ->
                        Button(
                            onClick = {
                                viewModel.reassignStudentClassroom(s, targetClass.id)
                                studentToReassign = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${targetClass.name} (${targetClass.subject})")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { studentToReassign = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Student Confirmation Dialog
    if (studentToDelete != null) {
        val s = studentToDelete!!
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("Delete Student") },
            text = { Text("Are you sure you want to remove '${s.name}'? All grades, attendance, and homework records for this student will be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStudent(s.id)
                        studentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
