package com.aistudio.classroomlms.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.text
import com.aistudio.classroomlms.data.entity.ClassroomEntity
import com.aistudio.classroomlms.data.entity.LessonPlanEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLessonPlanDialog(
    initialPlan: LessonPlanEntity?,
    classrooms: List<ClassroomEntity>,
    activeClassroomId: Long?,
    onDismiss: () -> Unit,
    onSave: (unitTitle: String, description: String, targetDate: Long, status: String, targetClassroomId: Long?, reminderEnabled: Boolean, reminderTime: Long) -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    var unitTitle by remember { mutableStateOf(initialPlan?.unitTitle ?: "") }
    var description by remember { mutableStateOf(initialPlan?.description ?: "") }
    var dateString by remember {
        mutableStateOf(
            if (initialPlan != null) dateFormat.format(Date(initialPlan.targetDate))
            else dateFormat.format(Date())
        )
    }
    var status by remember { mutableStateOf(initialPlan?.status ?: "PLANNED") }
    var selectedClassroomId by remember {
        mutableStateOf(initialPlan?.classroomId ?: activeClassroomId)
    }

    var isStatusExpanded by remember { mutableStateOf(false) }
    var isClassroomExpanded by remember { mutableStateOf(false) }
    var isReminderEnabled by remember { mutableStateOf(initialPlan?.reminderEnabled ?: false) }
        var selectedHour by remember { 
            mutableStateOf(
                initialPlan?.reminderTime?.let { time ->
                    val calendar = Calendar.getInstance().apply { timeInMillis = time }
                    calendar.get(Calendar.HOUR_OF_DAY)
                } ?: -1
            )
        }
        var selectedMinute by remember { 
            mutableStateOf(
                initialPlan?.reminderTime?.let { time ->
                    val calendar = Calendar.getInstance().apply { timeInMillis = time }
                    calendar.get(Calendar.MINUTE)
                } ?: -1
            )
        }
        var reminderTimeString by remember { 
            mutableStateOf( 
                if (selectedHour != -1 && selectedMinute != -1) { 
                    "${selectedHour.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')}" 
                } else { 
                    "Not set" 
                } 
            ) 
        }

    val statusOptions = listOf("PLANNED", "IN_PROGRESS", "COMPLETED")
    val timePickerLauncher = remember {
        ActivityResultLauncher(ActivityResultContracts.PickVisualMedia()) { uri ->
            // This is not actually used for time picking, we'll use a different approach
        }
    }
    
    // For time picking, we'll use a simple approach with a custom dialog
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MINUTE)) }

    val selectedClassroomName = when (selectedClassroomId) {
        null -> "Select Classroom"
        else -> classrooms.find { it.id == selectedClassroomId }?.let { "${it.name} (${it.subject})" } ?: "Select Classroom"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialPlan != null) "Edit Lesson Plan" else "New Lesson Plan",
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
                    value = unitTitle,
                    onValueChange = { unitTitle = it },
                    label = { Text("Unit / Topic Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Objectives & Detailed Notes *") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text("Target Date (YYYY-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = isStatusExpanded,
                    onExpandedChange = { isStatusExpanded = it }
                ) {
                    OutlinedTextField(
                        value = status.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStatusExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = isStatusExpanded,
                        onDismissRequest = { isStatusExpanded = false }
                    ) {
                        statusOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.replace("_", " ")) },
                                onClick = {
                                    status = option
                                    isStatusExpanded = false
                                }
                            )
                        }
                    }
                }

                // Reminder Toggle
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "Reminder",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alignVerticallyByCenter(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = isReminderEnabled,
                            onCheckedChange = { isReminderEnabled = it },
                            thumbContent = { SwitchDefaults.SwitchThumbContent(isChecked = it) },
                            track = { SwitchDefaults.switchTrack(unselectedColor = Color.Gray, selectedColor = MaterialTheme.colorScheme.primary) }
                        )
                        Text(
                            text = if (isReminderEnabled) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (isReminderEnabled) {
                            Text(
                                text = reminderTimeString,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            IconButton(
                                onClick = { /* Show time picker */ },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = "Set reminder time")
                            }
                        }
                    }
                }

                // Reminder Time Picker (shown when enabled)
                if (isReminderEnabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text(
                            text = "Set Reminder Time",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .alignVerticallyByCenter()
                        ) {
                            Text(
                                text = "${selectedHour.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showTimePicker = true }
                                    .padding(8.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                    )
                            )
                            IconButton(
                                onClick = { showTimePicker = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = "Set reminder time")
                            }
                        }
                    }
                }
                
                // Time Picker Dialog
                if (showTimePicker) {
                    AlertDialog(
                        onDismissRequest = { showTimePicker = false },
                        title = { Text("Select Reminder Time") },
                        text = {
                            TimePicker(
                                hour = selectedHour,
                                minute = selectedMinute,
                                onHourChange = { selectedHour = it },
                                onMinuteChange = { selectedMinute = it }
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val calendar = Calendar.getInstance()
                                    calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                                    calendar.set(Calendar.MINUTE, selectedMinute)
                                    calendar.set(Calendar.SECOND, 0)
                                    reminderTimeMillis = calendar.timeInMillis
                                    showTimePicker = false
                                }
                            ) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showTimePicker = false }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            } 
        }, 
        confirmButton = {
            Button(
                onClick = {
                    if (unitTitle.isBlank() || description.isBlank()) return@Button
                    val parsedDate = try {
                        dateFormat.parse(dateString)?.time ?: System.currentTimeMillis()
                    } catch (_: Exception) {
                        System.currentTimeMillis()
                    }
                    onSave(unitTitle.trim(), description.trim(), parsedDate, status, selectedClassroomId, isReminderEnabled, if (isReminderEnabled) reminderTimeMillis else 0L)
                },
                enabled = unitTitle.isNotBlank() && description.isNotBlank()
            ) {
                Text("Save")
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
fun AddEditLessonPlanDialogPreview() {
    val mockClassrooms = listOf(
        ClassroomEntity(id = 1, name = "Grade 10 Mathematics", subject = "Algebra II", gradeLevel = "10th Grade", roomNumber = "101", scheduleInfo = "Mon/Wed 9:00 AM"),
        ClassroomEntity(id = 2, name = "Grade 11 Physics", subject = "Mechanics", gradeLevel = "11th Grade", roomNumber = "202", scheduleInfo = "Tue/Thu 11:00 AM")
    )
    AddEditLessonPlanDialog(
        initialPlan = null,
        classrooms = mockClassrooms,
        activeClassroomId = 1,
        onDismiss = {},
        onSave = { _, _, _, _, _ -> }
    )
}
