package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.example.data.entity.DailyLogEntity
import com.example.ui.components.VoiceDictationButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDailyLogDialog(
    initialLog: DailyLogEntity?,
    classrooms: List<ClassroomEntity>,
    activeClassroomId: Long?,
    onDismiss: () -> Unit,
    onSave: (date: Long, reflectionNotes: String, wasProxyClass: Boolean, targetClassroomId: Long?) -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    var dateString by remember {
        mutableStateOf(
            if (initialLog != null) dateFormat.format(Date(initialLog.date))
            else dateFormat.format(Date())
        )
    }
    var reflectionNotes by remember { mutableStateOf(initialLog?.reflectionNotes ?: "") }
    var wasProxyClass by remember { mutableStateOf(initialLog?.wasProxyClass ?: false) }
    var selectedClassroomId by remember {
        mutableStateOf(initialLog?.classroomId ?: activeClassroomId)
    }

    var isClassroomExpanded by remember { mutableStateOf(false) }

    val selectedClassroomName = when (selectedClassroomId) {
        null -> "Select Classroom"
        else -> classrooms.find { it.id == selectedClassroomId }?.let { "${it.name} (${it.subject})" } ?: "Select Classroom"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialLog != null) "Edit Diary Entry" else "New Daily Diary Entry",
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
                // Classroom Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = isClassroomExpanded,
                    onExpandedChange = { isClassroomExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedClassroomName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assign to Classroom *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isClassroomExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = isClassroomExpanded,
                        onDismissRequest = { isClassroomExpanded = false }
                    ) {
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

                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text("Log Date (YYYY-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reflectionNotes,
                    onValueChange = { reflectionNotes = it },
                    label = { Text("Reflection & Teaching Notes *") },
                    trailingIcon = {
                        VoiceDictationButton(
                            onTextSpoken = { spoken ->
                                reflectionNotes = if (reflectionNotes.isBlank()) spoken else "$reflectionNotes $spoken"
                            },
                            prompt = "Speak daily reflection..."
                        )
                    },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Proxy / Substitute Class",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Mark if taking this class on behalf of another teacher",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = wasProxyClass,
                        onCheckedChange = { wasProxyClass = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reflectionNotes.isBlank()) return@Button
                    val parsedDate = try {
                        dateFormat.parse(dateString)?.time ?: System.currentTimeMillis()
                    } catch (_: Exception) {
                        System.currentTimeMillis()
                    }
                    onSave(parsedDate, reflectionNotes.trim(), wasProxyClass, selectedClassroomId)
                },
                enabled = reflectionNotes.isNotBlank()
            ) {
                Text("Save Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AddEditDailyLogDialogPreview() {
    val mockClassrooms = listOf(
        ClassroomEntity(id = 1, name = "Grade 10 Mathematics", subject = "Algebra II", gradeLevel = "10th Grade", roomNumber = "101", scheduleInfo = "Mon/Wed 9:00 AM"),
        ClassroomEntity(id = 2, name = "Grade 11 Physics", subject = "Mechanics", gradeLevel = "11th Grade", roomNumber = "202", scheduleInfo = "Tue/Thu 11:00 AM")
    )
    AddEditDailyLogDialog(
        initialLog = null,
        classrooms = mockClassrooms,
        activeClassroomId = 1,
        onDismiss = {},
        onSave = { _, _, _, _ -> }
    )
}
