package com.aistudio.classroomlms.ui.screens

import android.app.TimePickerDialog
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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.classroomlms.data.entity.ClassroomEntity
import com.aistudio.classroomlms.data.entity.LessonPlanEntity
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
    onSave: (
        unitTitle: String,
        description: String,
        targetDate: Long,
        status: String,
        targetClassroomId: Long?,
        reminderEnabled: Boolean,
        reminderTime: Long
    ) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false } }
    val initialReminderCalendar = remember(initialPlan?.reminderTime) {
        Calendar.getInstance().apply {
            if (initialPlan != null && initialPlan.reminderTime > 0) {
                timeInMillis = initialPlan.reminderTime
            }
        }
    }

    var unitTitle by remember { mutableStateOf(initialPlan?.unitTitle.orEmpty()) }
    var description by remember { mutableStateOf(initialPlan?.description.orEmpty()) }
    var dateString by remember {
        mutableStateOf(dateFormat.format(Date(initialPlan?.targetDate ?: System.currentTimeMillis())))
    }
    var status by remember { mutableStateOf(initialPlan?.status ?: "PLANNED") }
    var selectedClassroomId by remember {
        mutableStateOf(initialPlan?.classroomId ?: activeClassroomId)
    }
    var reminderEnabled by remember { mutableStateOf(initialPlan?.reminderEnabled ?: false) }
    var selectedHour by remember { mutableStateOf(initialReminderCalendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(initialReminderCalendar.get(Calendar.MINUTE)) }
    var isClassroomExpanded by remember { mutableStateOf(false) }
    var isStatusExpanded by remember { mutableStateOf(false) }

    val parsedTargetDate = remember(dateString) {
        runCatching { dateFormat.parse(dateString)?.time }.getOrNull()
            ?.takeIf { dateFormat.format(Date(it)) == dateString }
    }
    val selectedClassroomName = classrooms
        .find { it.id == selectedClassroomId }
        ?.let { "${it.name} (${it.subject})" }
        ?: "Select Classroom"
    val reminderTimeLabel = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialPlan == null) "New Lesson Plan" else "Edit Lesson Plan",
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
                ExposedDropdownMenuBox(
                    expanded = isClassroomExpanded,
                    onExpandedChange = { isClassroomExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedClassroomName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assign to Classroom *") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isClassroomExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = isClassroomExpanded,
                        onDismissRequest = { isClassroomExpanded = false }
                    ) {
                        classrooms.forEach { classroom ->
                            DropdownMenuItem(
                                text = { Text("${classroom.name} (${classroom.subject})") },
                                onClick = {
                                    selectedClassroomId = classroom.id
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
                    isError = parsedTargetDate == null,
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
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStatusExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = isStatusExpanded,
                        onDismissRequest = { isStatusExpanded = false }
                    ) {
                        listOf("PLANNED", "IN_PROGRESS", "COMPLETED").forEach { option ->
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Reminder")
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it }
                    )
                }
                if (reminderEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Reminder time: $reminderTimeLabel")
                        TextButton(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        selectedHour = hour
                                        selectedMinute = minute
                                    },
                                    selectedHour,
                                    selectedMinute,
                                    true
                                ).show()
                            }
                        ) {
                            Text("Set time")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = unitTitle.isNotBlank() &&
                    description.isNotBlank() &&
                    selectedClassroomId != null &&
                    parsedTargetDate != null,
                onClick = {
                    val targetDate = requireNotNull(parsedTargetDate)
                    val reminderTime = if (reminderEnabled) {
                        Calendar.getInstance().apply {
                            timeInMillis = targetDate
                            set(Calendar.HOUR_OF_DAY, selectedHour)
                            set(Calendar.MINUTE, selectedMinute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    } else {
                        0L
                    }
                    onSave(
                        unitTitle.trim(),
                        description.trim(),
                        targetDate,
                        status,
                        selectedClassroomId,
                        reminderEnabled,
                        reminderTime
                    )
                }
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
