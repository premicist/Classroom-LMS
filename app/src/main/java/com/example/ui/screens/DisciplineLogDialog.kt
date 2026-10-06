package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.BehaviorCategory
import com.example.data.entity.BehaviorSeverity
import com.example.data.entity.DisciplineRecordEntity
import com.example.data.entity.StudentEntity
import com.example.ui.components.VoiceDictationButton
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BehaviorPreset(
    val title: String,
    val category: BehaviorCategory,
    val severity: BehaviorSeverity,
    val defaultAction: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DisciplineLogDialog(
    student: StudentEntity,
    initialRecord: DisciplineRecordEntity?,
    classroomId: Long,
    onDismiss: () -> Unit,
    onSave: (DisciplineRecordEntity) -> Unit,
    onDelete: ((DisciplineRecordEntity) -> Unit)? = null
) {
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    var title by remember { mutableStateOf(initialRecord?.title ?: "") }
    var description by remember { mutableStateOf(initialRecord?.description ?: "") }
    var actionTaken by remember { mutableStateOf(initialRecord?.actionTaken ?: "") }
    var category by remember { mutableStateOf(initialRecord?.category ?: BehaviorCategory.CLASSROOM_DISRUPTION) }
    var severity by remember { mutableStateOf(initialRecord?.severity ?: BehaviorSeverity.LOW_WARNING) }
    var parentNotified by remember { mutableStateOf(initialRecord?.parentNotified ?: false) }
    var resolved by remember { mutableStateOf(initialRecord?.resolved ?: false) }

    val presets = listOf(
        BehaviorPreset("Great Participation & Effort", BehaviorCategory.PRAISE_MERIT, BehaviorSeverity.LOW_WARNING, "Praise & Merit Recorded"),
        BehaviorPreset("Helping Classmates & Leadership", BehaviorCategory.PRAISE_MERIT, BehaviorSeverity.LOW_WARNING, "Merit Points Awarded"),
        BehaviorPreset("Talking During Instruction", BehaviorCategory.CLASSROOM_DISRUPTION, BehaviorSeverity.LOW_WARNING, "Verbal Warning Given"),
        BehaviorPreset("Off-Task / Distracting Classmates", BehaviorCategory.CLASSROOM_DISRUPTION, BehaviorSeverity.LOW_WARNING, "Seating Moved"),
        BehaviorPreset("Unprepared / Missing Book/Laptop", BehaviorCategory.UNPREPARED, BehaviorSeverity.LOW_WARNING, "Borrowed Classroom Supplies"),
        BehaviorPreset("Late Entry to Class", BehaviorCategory.UNPREPARED, BehaviorSeverity.LOW_WARNING, "Late Logged"),
        BehaviorPreset("Phone / Device Distraction", BehaviorCategory.CLASSROOM_DISRUPTION, BehaviorSeverity.MEDIUM_DETENTION, "Phone Stored on Teacher Desk"),
        BehaviorPreset("Academic Dishonesty / Copying", BehaviorCategory.ACADEMIC_DISHONESTY, BehaviorSeverity.HIGH_ADMIN_REFERRAL, "Zero on Task & Parent Contact")
    )

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (initialRecord == null) "Log Behavior / Discipline" else "Edit Behavior Record",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Student: ${student.name} (${student.studentNumber})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (initialRecord != null && onDelete != null) {
                    IconButton(onClick = { onDelete(initialRecord) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Record", tint = StatusError)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Preset Chips
                Text(
                    text = "⚡ Quick 1-Tap Behaviors",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = title == preset.title,
                            onClick = {
                                title = preset.title
                                category = preset.category
                                severity = preset.severity
                                if (actionTaken.isBlank()) actionTaken = preset.defaultAction
                            },
                            label = { Text(preset.title, fontSize = 11.sp) },
                            leadingIcon = {
                                if (preset.category.isPositive) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(14.dp))
                                } else {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError, modifier = Modifier.size(14.dp))
                                }
                            }
                        )
                    }
                }

                // Category Selection Chips
                Text(
                    text = "Category *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BehaviorCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                // Severity Selection
                if (!category.isPositive) {
                    Text(
                        text = "Severity Level",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BehaviorSeverity.entries.forEach { sev ->
                            FilterChip(
                                selected = severity == sev,
                                onClick = { severity = sev },
                                label = { Text(sev.displayName, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Custom Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Behavior Title *") },
                    placeholder = { Text("e.g. Disrupted class during instruction") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Incident Details / Context") },
                    placeholder = { Text("Optional additional notes...") },
                    trailingIcon = {
                        VoiceDictationButton(
                            onTextSpoken = { spoken ->
                                description = if (description.isBlank()) spoken else "$description $spoken"
                            },
                            prompt = "Speak incident details..."
                        )
                    },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Action Taken
                OutlinedTextField(
                    value = actionTaken,
                    onValueChange = { actionTaken = it },
                    label = { Text("Action Taken / Teacher Response") },
                    placeholder = { Text("e.g. Moved seat, verbal warning, parent phone call") },
                    trailingIcon = {
                        VoiceDictationButton(
                            onTextSpoken = { spoken ->
                                actionTaken = if (actionTaken.isBlank()) spoken else "$actionTaken $spoken"
                            },
                            prompt = "Speak action taken..."
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Parent Notified Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { parentNotified = !parentNotified }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Parent / Guardian Notified", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Switch(checked = parentNotified, onCheckedChange = { parentNotified = it })
                }

                // Resolved Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { resolved = !resolved }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Issue Resolved", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Switch(checked = resolved, onCheckedChange = { resolved = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val record = initialRecord?.copy(
                            category = category,
                            severity = severity,
                            title = title.trim(),
                            description = description.trim(),
                            actionTaken = actionTaken.trim(),
                            parentNotified = parentNotified,
                            resolved = resolved
                        ) ?: DisciplineRecordEntity(
                            studentId = student.id,
                            classroomId = classroomId,
                            date = todayStr,
                            category = category,
                            severity = severity,
                            title = title.trim(),
                            description = description.trim(),
                            actionTaken = actionTaken.trim(),
                            parentNotified = parentNotified,
                            resolved = resolved
                        )
                        onSave(record)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
