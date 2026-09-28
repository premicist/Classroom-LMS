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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ClassroomEntity
import com.example.data.entity.StudentEntity

/**
 * Full student roster: view, search, add, edit, delete, and re-assign classroom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentRosterScreen(
    students: List<StudentEntity>,
    classrooms: List<ClassroomEntity>,
    activeClassroomId: Long?,
    onBack: () -> Unit,
    onAddStudent: () -> Unit,
    onEditStudent: (StudentEntity) -> Unit,
    onDeleteStudent: (StudentEntity) -> Unit,
    onReassignClassroom: (StudentEntity, Long) -> Unit,
    onLogDiscipline: (StudentEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterClassroomId by remember { mutableStateOf<Long?>(activeClassroomId) }
    var studentToDelete by remember { mutableStateOf<StudentEntity?>(null) }
    var studentToReassign by remember { mutableStateOf<StudentEntity?>(null) }

    val classroomMap = remember(classrooms) { classrooms.associateBy { it.id } }
    val activeClassroom = classroomMap[filterClassroomId]
    val isClassroomLinked = activeClassroom?.linkedSpreadsheetId != null

    val filtered = remember(students, searchQuery, filterClassroomId) {
        students.filter { s ->
            val matchesClass = filterClassroomId == null || s.classroomId == filterClassroomId
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                s.name.lowercase().contains(q) ||
                s.studentNumber.lowercase().contains(q) ||
                s.email.lowercase().contains(q)
            matchesClass && matchesSearch
        }.sortedBy { it.name.lowercase() }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Student Roster", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${filtered.size} student${if (filtered.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isClassroomLinked) {
                        FilledTonalButton(
                            onClick = onAddStudent,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (isClassroomLinked) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Roster managed via linked Google Sheet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search name, ID, or email") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Classroom filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterClassroomId == null,
                    onClick = { filterClassroomId = null },
                    label = { Text("All classes") }
                )
                classrooms.take(4).forEach { c ->
                    FilterChip(
                        selected = filterClassroomId == c.id,
                        onClick = { filterClassroomId = c.id },
                        label = {
                            Text(
                                text = c.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching students" else "No students yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onAddStudent) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add student")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { student ->
                        StudentRosterRow(
                            student = student,
                            classroomName = classroomMap[student.classroomId]?.name ?: "—",
                            onEdit = { onEditStudent(student) },
                            onDelete = { studentToDelete = student },
                            onReassign = { studentToReassign = student },
                            onLogDiscipline = { onLogDiscipline(student) }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation
    studentToDelete?.let { student ->
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("Remove student?") },
            text = {
                Text("Remove ${student.name} from the roster? Grades and attendance for this student will also be removed.")
            },
            confirmButton = {
                Button(onClick = {
                    onDeleteStudent(student)
                    studentToDelete = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Re-assign classroom dialog
    studentToReassign?.let { student ->
        ReassignClassroomDialog(
            student = student,
            classrooms = classrooms,
            currentClassroomId = student.classroomId,
            onDismiss = { studentToReassign = null },
            onConfirm = { newClassroomId ->
                onReassignClassroom(student, newClassroomId)
                studentToReassign = null
            }
        )
    }
}

@Composable
private fun StudentRosterRow(
    student: StudentEntity,
    classroomName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReassign: () -> Unit,
    onLogDiscipline: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(student.avatarColorHex)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = student.studentNumber,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = classroomName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(onClick = onLogDiscipline) {
                Icon(
                    Icons.Default.AddComment,
                    contentDescription = "Log Behavior / Discipline",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onReassign) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = "Re-assign class",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReassignClassroomDialog(
    student: StudentEntity,
    classrooms: List<ClassroomEntity>,
    currentClassroomId: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedId by remember { mutableLongStateOf(currentClassroomId) }
    val selectedName = classrooms.find { it.id == selectedId }?.name ?: "Select class"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Re-assign classroom") },
        text = {
            Column {
                Text(
                    text = "Move ${student.name} to another classroom.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Classroom") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        classrooms.forEach { c ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${c.name} (${c.subject})",
                                        fontWeight = if (c.id == currentClassroomId) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedId = c.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedId) },
                enabled = selectedId != currentClassroomId
            ) {
                Text("Move student")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
