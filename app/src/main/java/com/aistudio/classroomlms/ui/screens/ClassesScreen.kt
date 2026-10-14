package com.aistudio.classroomlms.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.aistudio.classroomlms.data.entity.ClassroomEntity

@Composable
fun ClassesScreen(
    classrooms: List<ClassroomEntity>,
    archivedClassrooms: List<ClassroomEntity>,
    activeClassroomId: Long?,
    onSelectClassroom: (ClassroomEntity) -> Unit,
    onAddClassroom: () -> Unit,
    onEditClassroom: (ClassroomEntity) -> Unit,
    onArchiveClassroom: (ClassroomEntity) -> Unit,
    onRestoreClassroom: (ClassroomEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var classroomToArchive by remember { mutableStateOf<ClassroomEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Classes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Create, switch, edit, or archive classes. Class records stay on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = onAddClassroom) {
                Text("Add class")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (classrooms.isEmpty()) {
                item {
                    Text(
                        "No active classes yet. Add a class to get started.",
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(classrooms, key = { it.id }) { classroom ->
                val isActive = classroom.id == activeClassroomId
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSelectClassroom(classroom) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(classroom.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${classroom.subject} • ${classroom.gradeLevel} • ${classroom.roomNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isActive) {
                                Text("Active class", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        if (isActive) {
                            Icon(Icons.Default.Check, contentDescription = "Active class", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onEditClassroom(classroom) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit ${classroom.name}")
                        }
                        IconButton(onClick = { classroomToArchive = classroom }) {
                            Icon(Icons.Default.Archive, contentDescription = "Archive ${classroom.name}")
                        }
                    }
                }
            }

            if (archivedClassrooms.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Archived classes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(archivedClassrooms, key = { "archived-${it.id}" }) { classroom ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(classroom.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${classroom.subject} • ${classroom.gradeLevel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(onClick = { onRestoreClassroom(classroom) }) {
                                Icon(Icons.Default.Restore, contentDescription = null)
                                Text("Restore")
                            }
                        }
                    }
                }
            }
        }
    }

    classroomToArchive?.let { classroom ->
        AlertDialog(
            onDismissRequest = { classroomToArchive = null },
            title = { Text("Archive class?") },
            text = {
                Text("“${classroom.name}” will be removed from active class lists. Its student and classroom records will be retained and can be restored later.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onArchiveClassroom(classroom)
                    classroomToArchive = null
                }) {
                    Text("Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { classroomToArchive = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
