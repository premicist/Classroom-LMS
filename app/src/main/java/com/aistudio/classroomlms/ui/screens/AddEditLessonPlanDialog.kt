package com.aistudio.classroomlms.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.classroomlms.data.entity.AssignmentEntity
import com.aistudio.classroomlms.data.entity.ClassroomEntity
import com.aistudio.classroomlms.data.entity.LessonPlanEntity
import com.aistudio.classroomlms.data.entity.LessonPlanSessionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditLessonPlanDialog(
    initialPlan: LessonPlanEntity?,
    initialSessions: List<LessonPlanSessionEntity> = emptyList(),
    classrooms: List<ClassroomEntity>,
    activeClassroomId: Long?,
    assignments: List<AssignmentEntity> = emptyList(),
    onDismiss: () -> Unit,
    onCreateAssignmentFromSession: ((title: String, description: String) -> Unit)? = null,
    onLaunchLiveAssessmentFromSession: ((topic: String) -> Unit)? = null,
    onSave: (
        unitTitle: String,
        description: String,
        targetDate: Long,
        status: String,
        targetClassroomId: Long?,
        reminderEnabled: Boolean,
        reminderTime: Long,
        unitNumber: String,
        allocatedHours: Double,
        unitLearningOutcomes: String,
        sessions: List<LessonPlanSessionEntity>
    ) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    }
    val initialReminderCalendar = remember(initialPlan?.reminderTime) {
        Calendar.getInstance().apply {
            if (initialPlan != null && initialPlan.reminderTime > 0) {
                timeInMillis = initialPlan.reminderTime
            }
        }
    }

    var unitNumber by remember { mutableStateOf(initialPlan?.unitNumber.orEmpty()) }
    var unitTitle by remember { mutableStateOf(initialPlan?.unitTitle.orEmpty()) }
    var allocatedHoursText by remember {
        mutableStateOf(
            initialPlan?.allocatedHours?.takeIf { it > 0.0 }?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            } ?: ""
        )
    }
    var unitLearningOutcomes by remember { mutableStateOf(initialPlan?.unitLearningOutcomes.orEmpty()) }
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

    val sessions = remember(initialPlan?.id, initialSessions) {
        mutableStateListOf<LessonPlanSessionEntity>().apply {
            addAll(initialSessions.sortedBy { it.sessionOrder })
        }
    }
    var expandedSessionIndex by remember { mutableStateOf<Int?>(if (initialSessions.isEmpty()) null else 0) }

    val parsedTargetDate = remember(dateString) {
        runCatching { dateFormat.parse(dateString)?.time }
            .getOrNull()
            ?.takeIf { dateFormat.format(Date(it)) == dateString }
    }
    val selectedClassroomName = classrooms
        .find { it.id == selectedClassroomId }
        ?.let { "${it.name} (${it.subject})" }
        ?: "Select Classroom"
    val reminderTimeLabel = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute)
    val totalScheduledHours = sessions.sumOf { it.hours }
    val allocatedHours = allocatedHoursText.toDoubleOrNull() ?: 0.0
    val canSave = unitTitle.isNotBlank() && parsedTargetDate != null && selectedClassroomId != null &&
        (description.isNotBlank() || sessions.isNotEmpty())

    fun triggerSave() {
        val targetMillis = parsedTargetDate ?: return
        val reminderMillis = if (reminderEnabled) {
            Calendar.getInstance().apply {
                timeInMillis = targetMillis
                set(Calendar.HOUR_OF_DAY, selectedHour)
                set(Calendar.MINUTE, selectedMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } else {
            0L
        }
        val finalDescription = description.trim().ifBlank {
            sessions.joinToString("\n") { s ->
                val label = if (s.isClosingSession) "Closing of Unit" else s.learningOutcome.ifBlank { "Session ${s.sessionOrder}" }
                "• $label (${s.hours}h)"
            }
        }
        onSave(
            unitTitle.trim(),
            finalDescription,
            targetMillis,
            status,
            selectedClassroomId,
            reminderEnabled,
            reminderMillis,
            unitNumber.trim(),
            allocatedHours,
            unitLearningOutcomes.trim(),
            sessions.toList()
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .imePadding(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (initialPlan == null) "New Lesson Plan" else "Edit Lesson Plan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                        }
                    },
                    actions = {
                        Button(
                            onClick = { triggerSave() },
                            enabled = canSave,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Classroom + Unit Number + Hours
                item {
                    ExposedDropdownMenuBox(
                        expanded = isClassroomExpanded,
                        onExpandedChange = { isClassroomExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedClassroomName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Classroom *") },
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
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = unitNumber,
                            onValueChange = { unitNumber = it },
                            label = { Text("Unit No.") },
                            placeholder = { Text("Unit 1") },
                            singleLine = true,
                            modifier = Modifier.weight(0.35f)
                        )
                        OutlinedTextField(
                            value = allocatedHoursText,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.toDoubleOrNull() != null) allocatedHoursText = input
                            },
                            label = { Text("Total Hrs") },
                            placeholder = { Text("5") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.28f)
                        )
                        OutlinedTextField(
                            value = dateString,
                            onValueChange = { dateString = it },
                            label = { Text("Target Date *") },
                            isError = parsedTargetDate == null,
                            singleLine = true,
                            modifier = Modifier.weight(0.37f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = unitTitle,
                        onValueChange = { unitTitle = it },
                        label = { Text("Unit / Topic Title *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = unitLearningOutcomes,
                        onValueChange = { unitLearningOutcomes = it },
                        label = { Text("Unit Learning Outcomes (Optional)") },
                        placeholder = { Text("At the end of this unit, students will be able to...") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = {
                            Text(if (sessions.isEmpty()) "Objectives & Notes *" else "Quick Summary Notes (Optional)")
                        },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Status + Reminder Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = isStatusExpanded,
                            onExpandedChange = { isStatusExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = status,
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
                                        text = { Text(option) },
                                        onClick = {
                                            status = option
                                            isStatusExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Reminder", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                            }
                            if (reminderEnabled) {
                                TextButton(
                                    onClick = {
                                        TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                selectedHour = hourOfDay
                                                selectedMinute = minute
                                            },
                                            selectedHour,
                                            selectedMinute,
                                            true
                                        ).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) {
                                    Text("Time: $reminderTimeLabel", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // 2. Structured LO Sessions Header
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LO Sessions (${sessions.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Structured breakdown for teaching & PDF matrix",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            val hrsLabel = if (allocatedHours > 0) {
                                "${formatHrs(totalScheduledHours)} / ${formatHrs(allocatedHours)} Hrs"
                            } else {
                                "${formatHrs(totalScheduledHours)} Hrs"
                            }
                            Text(
                                text = hrsLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // 3. Collapsible Session Cards
                itemsIndexed(sessions) { index, session ->
                    LessonPlanSessionCard(
                        index = index,
                        session = session,
                        isExpanded = expandedSessionIndex == index,
                        assignments = assignments,
                        onToggleExpand = {
                            expandedSessionIndex = if (expandedSessionIndex == index) null else index
                        },
                        onUpdate = { updated -> sessions[index] = updated },
                        onDelete = {
                            sessions.removeAt(index)
                            if (expandedSessionIndex == index) expandedSessionIndex = null
                        },
                        onCreateAssignment = onCreateAssignmentFromSession,
                        onLaunchLiveAssessment = onLaunchLiveAssessmentFromSession
                    )
                }

                // 4. Add Session / Closing of Unit Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val nextOrder = sessions.size + 1
                                sessions.add(
                                    LessonPlanSessionEntity(
                                        lessonPlanId = initialPlan?.id ?: 0L,
                                        sessionOrder = nextOrder,
                                        learningOutcome = "LO$nextOrder: ",
                                        hours = 1.0
                                    )
                                )
                                expandedSessionIndex = sessions.lastIndex
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add LO Session", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val nextOrder = sessions.size + 1
                                sessions.add(
                                    LessonPlanSessionEntity(
                                        lessonPlanId = initialPlan?.id ?: 0L,
                                        sessionOrder = nextOrder,
                                        learningOutcome = "Closing of Unit",
                                        content = "Summary of key concepts, review of LOs, Q&A, and bridging to next unit.",
                                        lecturerActivity = "Summarize key takeaways, conduct rapid Q&A, assign review exercises.",
                                        studentActivity = "Ask clarification questions, take summary notes, note homework.",
                                        teachingAids = "Smartboard, Summary Slide",
                                        evaluationType = "NOTE",
                                        evaluationNote = "Oral summary feedback & homework",
                                        hours = 1.0,
                                        isClosingSession = true
                                    )
                                )
                                expandedSessionIndex = sessions.lastIndex
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Closing of Unit", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LessonPlanSessionCard(
    index: Int,
    session: LessonPlanSessionEntity,
    isExpanded: Boolean,
    assignments: List<AssignmentEntity>,
    onToggleExpand: () -> Unit,
    onUpdate: (LessonPlanSessionEntity) -> Unit,
    onDelete: () -> Unit,
    onCreateAssignment: ((title: String, description: String) -> Unit)?,
    onLaunchLiveAssessment: ((topic: String) -> Unit)?
) {
    val aidPresets = listOf("Smartboard", "Projector", "Slides", "Handout", "Lab Kit")
    val evalNotePresets = listOf("Short Quiz", "Worksheet", "Oral Q&A", "Assignment", "Lab Check")
    var hoursText by remember(session.hours, index) {
        mutableStateOf(formatHrs(session.hours))
    }
    var assignmentDropdownOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (session.isClosingSession) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            // Collapsed 2-line Summary Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val headerTitle = if (session.isClosingSession) {
                        "${index + 1}. Closing of Unit"
                    } else {
                        "${index + 1}. ${session.learningOutcome.ifBlank { "Session ${index + 1}" }}"
                    }
                    Text(
                        text = headerTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val evalLabel = session.evaluationNote.ifBlank { session.evaluationType }
                    Text(
                        text = "${formatHrs(session.hours)} Hrs • $evalLabel",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove session", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = session.learningOutcome,
                            onValueChange = { onUpdate(session.copy(learningOutcome = it)) },
                            label = { Text("Learning Outcome (LO)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = hoursText,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.toDoubleOrNull() != null) {
                                    hoursText = input
                                    onUpdate(session.copy(hours = input.toDoubleOrNull() ?: 0.0))
                                }
                            },
                            label = { Text("Hrs") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(72.dp)
                        )
                    }

                    OutlinedTextField(
                        value = session.content,
                        onValueChange = { onUpdate(session.copy(content = it)) },
                        label = { Text("Content / Subtopics") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = session.lecturerActivity,
                        onValueChange = { onUpdate(session.copy(lecturerActivity = it)) },
                        label = { Text("Lecturer Activity") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = session.studentActivity,
                        onValueChange = { onUpdate(session.copy(studentActivity = it)) },
                        label = { Text("Student Activity") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = session.teachingAids,
                        onValueChange = { onUpdate(session.copy(teachingAids = it)) },
                        label = { Text("Teaching Aids") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        aidPresets.forEach { preset ->
                            val active = session.teachingAids.contains(preset, ignoreCase = true)
                            FilterChip(
                                selected = active,
                                onClick = {
                                    val current = session.teachingAids.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                                    if (active) current.removeAll { it.equals(preset, ignoreCase = true) } else current.add(preset)
                                    onUpdate(session.copy(teachingAids = current.joinToString(", ")))
                                },
                                label = { Text(preset, fontSize = 11.sp) }
                            )
                        }
                    }

                    // ONE clean Evaluation mode switch
                    Text(
                        text = "Evaluation",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "NOTE" to "Note",
                            "ASSIGNMENT" to "Assignment",
                            "LIVE_ASSESSMENT" to "⚡ Live Assess"
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = session.evaluationType == mode,
                                onClick = { onUpdate(session.copy(evaluationType = mode)) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    when (session.evaluationType) {
                        "ASSIGNMENT" -> {
                            val linked = assignments.find { it.id == session.linkedAssignmentId }
                            ExposedDropdownMenuBox(
                                expanded = assignmentDropdownOpen,
                                onExpandedChange = { assignmentDropdownOpen = it }
                            ) {
                                OutlinedTextField(
                                    value = linked?.title ?: session.evaluationNote.ifBlank { "Select Assignment" },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Linked Assignment") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = assignmentDropdownOpen)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(
                                    expanded = assignmentDropdownOpen,
                                    onDismissRequest = { assignmentDropdownOpen = false }
                                ) {
                                    assignments.forEach { a ->
                                        DropdownMenuItem(
                                            text = { Text("${a.title} (${a.type.displayName})") },
                                            onClick = {
                                                onUpdate(
                                                    session.copy(
                                                        linkedAssignmentId = a.id,
                                                        evaluationNote = "Assignment: ${a.title}"
                                                    )
                                                )
                                                assignmentDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                            if (onCreateAssignment != null) {
                                TextButton(
                                    onClick = {
                                        val suggestedTitle = session.learningOutcome.ifBlank { session.content }.take(60)
                                        onCreateAssignment(suggestedTitle, session.content)
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Create New Assignment from Session", fontSize = 12.sp)
                                }
                            }
                        }

                        "LIVE_ASSESSMENT" -> {
                            OutlinedTextField(
                                value = session.evaluationNote,
                                onValueChange = { onUpdate(session.copy(evaluationNote = it)) },
                                label = { Text("Topic / Problem Prompt") },
                                placeholder = { Text("e.g. Label computer anatomy diagram") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (onLaunchLiveAssessment != null) {
                                OutlinedButton(
                                    onClick = {
                                        val prompt = session.evaluationNote.ifBlank {
                                            session.learningOutcome.ifBlank { session.content }
                                        }.trim()
                                        onLaunchLiveAssessment(prompt)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Launch Live Assessment with Prompt", fontSize = 12.sp)
                                }
                            }
                        }

                        else -> {
                            OutlinedTextField(
                                value = session.evaluationNote,
                                onValueChange = { onUpdate(session.copy(evaluationNote = it)) },
                                label = { Text("Evaluation Method / Note") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                evalNotePresets.forEach { preset ->
                                    FilterChip(
                                        selected = session.evaluationNote.equals(preset, ignoreCase = true),
                                        onClick = { onUpdate(session.copy(evaluationNote = preset)) },
                                        label = { Text(preset, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatHrs(hours: Double): String {
    return if (hours % 1.0 == 0.0) hours.toInt().toString() else String.format(Locale.US, "%.1f", hours)
}
