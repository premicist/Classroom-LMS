package com.aistudio.classroomlms.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.classroomlms.data.entity.BehaviorCategory
import com.aistudio.classroomlms.data.entity.BehaviorSeverity
import com.aistudio.classroomlms.data.entity.DisciplineRecordEntity
import com.aistudio.classroomlms.data.entity.StudentEntity
import com.aistudio.classroomlms.ui.components.VoiceDictationButton
import com.aistudio.classroomlms.ui.theme.statusSuccess
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
    onDelete: ((DisciplineRecordEntity) -> Unit)? = null,
    onSaveAndSendWhatsApp: ((DisciplineRecordEntity) -> Unit)? = null,
    onSaveAndSendSms: ((DisciplineRecordEntity) -> Unit)? = null
) {
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    var title by remember { mutableStateOf(initialRecord?.title ?: "") }
    var description by remember { mutableStateOf(initialRecord?.description ?: "") }
    var actionTaken by remember { mutableStateOf(initialRecord?.actionTaken ?: "") }
    var category by remember { mutableStateOf(initialRecord?.category ?: BehaviorCategory.PRAISE_MERIT) }
    var severity by remember { mutableStateOf(initialRecord?.severity ?: BehaviorSeverity.LOW_WARNING) }
    var parentNotified by remember { mutableStateOf(initialRecord?.parentNotified ?: false) }
    var resolved by remember { mutableStateOf(initialRecord?.resolved ?: false) }

    val presets = listOf(
        BehaviorPreset("Great Classroom Participation", BehaviorCategory.PRAISE_MERIT, BehaviorSeverity.LOW_WARNING, "Praise & Merit Recorded"),
        BehaviorPreset("Helping Classmates & Teamwork", BehaviorCategory.PRAISE_MERIT, BehaviorSeverity.LOW_WARNING, "Merit Points Awarded"),
        BehaviorPreset("Neat & Complete Classwork", BehaviorCategory.PRAISE_MERIT, BehaviorSeverity.LOW_WARNING, "Classroom Commendation"),
        BehaviorPreset("Exemplary Manners & Discipline", BehaviorCategory.PRAISE_MERIT, BehaviorSeverity.LOW_WARNING, "Honor Badge Given"),
        BehaviorPreset("Talking During Instruction", BehaviorCategory.CLASSROOM_DISRUPTION, BehaviorSeverity.LOW_WARNING, "Verbal Warning Given"),
        BehaviorPreset("Incomplete / Missing Homework", BehaviorCategory.UNPREPARED, BehaviorSeverity.LOW_WARNING, "Recorded in Diary / Complete Tomorrow"),
        BehaviorPreset("Off-Task / Device Distraction", BehaviorCategory.CLASSROOM_DISRUPTION, BehaviorSeverity.LOW_WARNING, "Device Stored / Seating Moved"),
        BehaviorPreset("Late Arrival to Class", BehaviorCategory.UNPREPARED, BehaviorSeverity.LOW_WARNING, "Late Logged"),
        BehaviorPreset("Disrespectful / Rowdy Behavior", BehaviorCategory.CLASSROOM_DISRUPTION, BehaviorSeverity.MEDIUM_DETENTION, "Counseling Session Scheduled"),
        BehaviorPreset("Academic Dishonesty / Copying", BehaviorCategory.ACADEMIC_DISHONESTY, BehaviorSeverity.HIGH_ADMIN_REFERRAL, "Zero on Task & Parent Contact")
    )

    fun buildRecord(markParentNotified: Boolean = parentNotified): DisciplineRecordEntity {
        return initialRecord?.copy(
            category = category,
            severity = severity,
            title = title.trim(),
            description = description.trim(),
            actionTaken = actionTaken.trim(),
            parentNotified = markParentNotified,
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
            parentNotified = markParentNotified,
            resolved = resolved
        )
    }

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
                        text = if (initialRecord == null) "Student Behavioral Remark & Diary" else "Edit Remark Log",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Student: ${student.name} (${student.studentNumber})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (initialRecord != null && onDelete != null) {
                    IconButton(onClick = { onDelete(initialRecord) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Record", tint = MaterialTheme.colorScheme.error)
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
                    text = "⚡ 1-Tap Quick Remarks",
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
                                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.statusSuccess, modifier = Modifier.size(14.dp))
                                } else {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                }
                            }
                        )
                    }
                }

                // Category Selection Chips
                Text(
                    text = "Remark Type *",
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

                // Severity Selection (only for non-positive)
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
                    label = { Text("Remark Title *") },
                    placeholder = { Text("e.g. Great effort in group project") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Teacher Observation / Context") },
                    placeholder = { Text("Optional context notes...") },
                    trailingIcon = {
                        VoiceDictationButton(
                            onTextSpoken = { spoken ->
                                description = if (description.isBlank()) spoken else "$description $spoken"
                            },
                            prompt = "Speak observation details..."
                        )
                    },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Action Taken
                OutlinedTextField(
                    value = actionTaken,
                    onValueChange = { actionTaken = it },
                    label = { Text("Action / Follow-up") },
                    placeholder = { Text("e.g. Merit awarded, verbal counseling") },
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

                // Switches
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { parentNotified = !parentNotified }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Parent Notified", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Switch(checked = parentNotified, onCheckedChange = { parentNotified = it })
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { resolved = !resolved }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Issue Resolved / Acknowledged", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Switch(checked = resolved, onCheckedChange = { resolved = it })
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary Action: Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(buildRecord())
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text("Save Remark")
                    }
                }

                // 1-Tap Parent Dispatch Actions
                if (student.guardianContact.isNotBlank() && (onSaveAndSendWhatsApp != null || onSaveAndSendSms != null)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onSaveAndSendSms != null) {
                            OutlinedButton(
                                onClick = {
                                    if (title.isNotBlank()) {
                                        onSaveAndSendSms(buildRecord(markParentNotified = true))
                                    }
                                },
                                enabled = title.isNotBlank(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Message, null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save & SMS", fontSize = 11.sp)
                            }
                        }

                        if (onSaveAndSendWhatsApp != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    if (title.isNotBlank()) {
                                        onSaveAndSendWhatsApp(buildRecord(markParentNotified = true))
                                    }
                                },
                                enabled = title.isNotBlank(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Send, null, modifier = Modifier.size(13.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save & WhatsApp", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {}
    )
}
