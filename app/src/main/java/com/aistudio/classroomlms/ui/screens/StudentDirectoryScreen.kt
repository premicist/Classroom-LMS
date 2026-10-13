package com.aistudio.classroomlms.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
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
import com.aistudio.classroomlms.data.entity.StudentEntity
import com.aistudio.classroomlms.ui.components.GradeBadge
import com.aistudio.classroomlms.ui.components.StudentAvatar
import com.aistudio.classroomlms.ui.theme.statusSuccess
import com.aistudio.classroomlms.ui.theme.statusSuccessContainer
import com.aistudio.classroomlms.ui.theme.statusWarning
import com.aistudio.classroomlms.ui.theme.statusEnrichment
import com.aistudio.classroomlms.ui.theme.statusEnrichmentContainer
import androidx.hilt.navigation.compose.hiltViewModel
import com.aistudio.classroomlms.ui.viewmodel.ClassroomViewModel
import com.aistudio.classroomlms.ui.viewmodel.LmsUiState
import com.aistudio.classroomlms.ui.viewmodel.RosterViewModel
import com.aistudio.classroomlms.ui.viewmodel.StudentTier
import com.aistudio.classroomlms.ui.viewmodel.StudentSupportViewModel

@Composable
fun StudentDirectoryScreen(
    uiState: LmsUiState,
    viewModel: ClassroomViewModel,
    modifier: Modifier = Modifier,
    rosterViewModel: RosterViewModel = hiltViewModel(),
    studentSupportViewModel: StudentSupportViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
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
                onClick = { rosterViewModel.openAddStudent() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
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
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (summaries.isEmpty()) "No students have been added to this classroom yet."
                        else "No students match your search or filter.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (summaries.isEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { rosterViewModel.openAddStudent() }) {
                            Text("Add Student")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredSummaries, key = { it.student.id }) { summary ->
                        val s = summary.student
                        val (tierBg, tierText) = when (summary.tier) {
                            StudentTier.EXTRA_SUPPORT -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
                            StudentTier.ENRICHMENT_NEEDED -> MaterialTheme.colorScheme.statusEnrichmentContainer to MaterialTheme.colorScheme.statusEnrichment
                            StudentTier.ON_TRACK -> MaterialTheme.colorScheme.statusSuccessContainer to MaterialTheme.colorScheme.statusSuccess
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
                                                    rosterViewModel.openEditStudent(s)
                                                },
                                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Live Assessment") },
                                                onClick = {
                                                    showActionMenu = false
                                                    studentSupportViewModel.openLiveAssessment(s)
                                                },
                                                leadingIcon = { Icon(Icons.Default.AddComment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Log Discipline / Merit") },
                                                onClick = {
                                                    showActionMenu = false
                                                    studentSupportViewModel.openAddDiscipline(s)
                                                },
                                                leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.statusWarning) }
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
                                                text = { Text("Delete Student", color = MaterialTheme.colorScheme.error) },
                                                onClick = {
                                                    showActionMenu = false
                                                    studentToDelete = s
                                                },
                                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
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
                                        Text("%.2f".format(summary.gpa), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("HW", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${summary.homeworkCompletionRate.toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Attend", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${summary.attendanceRate.toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (summary.attendanceRate < 85.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.statusSuccess)
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
    }

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
                                rosterViewModel.reassignStudentClassroom(s, targetClass.id)
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

    if (studentToDelete != null) {
        val s = studentToDelete!!
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("Delete Student") },
            text = { Text("Are you sure you want to remove '${s.name}'? All grades, attendance, and homework records for this student will be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        rosterViewModel.deleteStudent(s.id)
                        studentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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
