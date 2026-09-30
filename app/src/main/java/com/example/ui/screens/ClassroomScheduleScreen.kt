package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.entity.ClassScheduleEntity
import com.example.ui.theme.EduPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.ClassroomViewModel
import com.example.ui.viewmodel.LmsUiState
import java.util.Calendar
import java.util.Locale

enum class ScheduleViewMode {
    WEEKLY_DAY,
    FULL_5_WEEK_MATRIX
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomScheduleScreen(
    uiState: LmsUiState,
    viewModel: ClassroomViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedWeek by remember { mutableIntStateOf(1) } // 1 to 5
    var selectedDayIndex by remember { mutableIntStateOf(0) } // 0..6 (Mon..Sun)
    var viewMode by remember { mutableStateOf(ScheduleViewMode.WEEKLY_DAY) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<ClassScheduleEntity?>(null) }
    var showActionsMenu by remember { mutableStateOf(false) }
    var showReplicateConfirmDialog by remember { mutableStateOf(false) }
    var showClearWeekConfirmDialog by remember { mutableStateOf(false) }

    val daysOfWeek = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val currentDayOfWeek = daysOfWeek[selectedDayIndex]

    // Determine current day of week and current time in minutes for "NOW ONGOING" highlight
    val calNow = Calendar.getInstance()
    val nowMinutes = calNow.get(Calendar.HOUR_OF_DAY) * 60 + calNow.get(Calendar.MINUTE)
    val todayDayOfWeekStr = when (calNow.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "MONDAY"
        Calendar.TUESDAY -> "TUESDAY"
        Calendar.WEDNESDAY -> "WEDNESDAY"
        Calendar.THURSDAY -> "THURSDAY"
        Calendar.FRIDAY -> "FRIDAY"
        Calendar.SATURDAY -> "SATURDAY"
        Calendar.SUNDAY -> "SUNDAY"
        else -> "MONDAY"
    }

    // Master schedule list across ALL subjects/classrooms
    val masterSchedules = uiState.allSchedules.ifEmpty { uiState.schedules }

    // Map classroom names to theme color
    val classroomColorMap = remember(uiState.classrooms) {
        uiState.classrooms.associate { it.name.lowercase(Locale.US) to Color(it.colorHex) }
    }

    // Filtered master schedules for selected week & day
    val selectedDaySchedules = masterSchedules.filter {
        it.weekNumber == selectedWeek && it.dayOfWeek.equals(currentDayOfWeek, ignoreCase = true)
    }.sortedBy { it.startMinutes }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Classroom Schedule", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "Unified Timetable & Period Management",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // View mode toggle
                    IconButton(
                        onClick = {
                            viewMode = if (viewMode == ScheduleViewMode.WEEKLY_DAY) ScheduleViewMode.FULL_5_WEEK_MATRIX else ScheduleViewMode.WEEKLY_DAY
                        }
                    ) {
                        Icon(
                            imageVector = if (viewMode == ScheduleViewMode.WEEKLY_DAY) Icons.Default.GridView else Icons.AutoMirrored.Filled.List,
                            contentDescription = "Toggle View Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Actions Menu
                    Box {
                        IconButton(onClick = { showActionsMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                        }
                        DropdownMenu(
                            expanded = showActionsMenu,
                            onDismissRequest = { showActionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Copy Week 1 Routine to Weeks 2–5") },
                                onClick = {
                                    showActionsMenu = false
                                    showReplicateConfirmDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Current Week Routine") },
                                onClick = {
                                    showActionsMenu = false
                                    showClearWeekConfirmDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusError) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = viewMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScheduleViewTransition"
            ) { mode ->
                if (mode == ScheduleViewMode.WEEKLY_DAY) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Week Selector Chips (1 to 5) - Horizontally scrollable and responsive
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                Text(
                                    text = "Week:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items((1..5).toList(), key = { it }) { week ->
                                val isSelected = week == selectedWeek
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedWeek = week },
                                    label = { Text("Week $week") }
                                )
                            }
                        }

                        // Day Tabs
                        ScrollableTabRow(
                            selectedTabIndex = selectedDayIndex,
                            modifier = Modifier.fillMaxWidth(),
                            edgePadding = 12.dp
                        ) {
                            dayLabels.forEachIndexed { index, label ->
                                Tab(
                                    selected = selectedDayIndex == index,
                                    onClick = { selectedDayIndex = index },
                                    text = { Text(label, fontSize = 13.sp, fontWeight = if (selectedDayIndex == index) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Week $selectedWeek • ${daysOfWeek[selectedDayIndex]} (${selectedDaySchedules.size} period${if (selectedDaySchedules.size == 1) "" else "s"})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Button(
                                onClick = {
                                    editingEntry = null
                                    showAddDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Class", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Schedule Entries List
                        if (selectedDaySchedules.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No classes scheduled for Week $selectedWeek, ${daysOfWeek[selectedDayIndex]}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(onClick = {
                                        editingEntry = null
                                        showAddDialog = true
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Class Entry")
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(selectedDaySchedules, key = { it.id }) { entry ->
                                    val subjectColor = classroomColorMap[entry.classroomName.lowercase(Locale.US)] ?: EduPrimary
                                    val isToday = currentDayOfWeek.equals(todayDayOfWeekStr, ignoreCase = true)
                                    val isOngoing = isToday && (nowMinutes >= entry.startMinutes && (entry.endMinutes == 0 || nowMinutes < entry.endMinutes))

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                editingEntry = entry
                                                showAddDialog = true
                                            }
                                            .then(
                                                if (isOngoing) Modifier.border(2.dp, StatusSuccess, RoundedCornerShape(12.dp))
                                                else Modifier
                                            ),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(if (isOngoing) 4.dp else 1.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(subjectColor),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Schedule,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = entry.classroomName,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        if (isOngoing) {
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Surface(
                                                                color = StatusSuccess,
                                                                shape = RoundedCornerShape(4.dp)
                                                            ) {
                                                                Text(
                                                                    text = "NOW ONGOING",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color.White,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        text = if (entry.endTime.isNotBlank()) "${entry.startTime} – ${entry.endTime}" else entry.startTime,
                                                        fontSize = 12.sp,
                                                        color = subjectColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            IconButton(onClick = { viewModel.deleteScheduleEntry(entry) }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = StatusError,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Full 5-Week Routine Matrix Overview
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.GridView, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Complete 5-Week Master Timetable Matrix",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        items((1..5).toList()) { weekNum ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Week $weekNum Routine",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    daysOfWeek.forEach { dayName ->
                                        val entriesForDay = masterSchedules.filter {
                                            it.weekNumber == weekNum && it.dayOfWeek.equals(dayName, ignoreCase = true)
                                        }.sortedBy { it.startMinutes }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = dayName.take(3),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.width(42.dp)
                                            )

                                            if (entriesForDay.isEmpty()) {
                                                Text(
                                                    text = if (dayName == "SATURDAY" || dayName == "SUNDAY") "Weekend / Holiday" else "No classes",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            } else {
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    items(entriesForDay, key = { it.id }) { cls ->
                                                        val color = classroomColorMap[cls.classroomName.lowercase(Locale.US)] ?: MaterialTheme.colorScheme.primaryContainer
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(color.copy(alpha = 0.85f))
                                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                        ) {
                                                            Text(
                                                                text = "${cls.classroomName} @ ${cls.startTime}",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
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
                    }
                }
            }
        }
    }

    // Add / Edit Schedule Entry Dialog
    if (showAddDialog) {
        AddEditScheduleEntryDialog(
            initialEntry = editingEntry,
            existingClassrooms = uiState.classrooms.map { it.name },
            allSchedules = masterSchedules,
            defaultWeekNumber = selectedWeek,
            defaultDayOfWeek = currentDayOfWeek,
            onDismiss = {
                showAddDialog = false
                editingEntry = null
            },
            onSaveBatch = { baseEntry, selectedDays, applyAllWeeks, updateSubjectAllWeeks ->
                viewModel.saveScheduleBatch(baseEntry, selectedDays, applyAllWeeks, updateSubjectAllWeeks)
                showAddDialog = false
                editingEntry = null
            }
        )
    }

    // Replicate Confirmation Dialog
    if (showReplicateConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showReplicateConfirmDialog = false },
            title = { Text("Copy Week 1 to Weeks 2–5", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to copy the complete Week 1 routine to Weeks 2, 3, 4, and 5? Existing entries in Weeks 2–5 will be replaced.") },
            confirmButton = {
                Button(onClick = {
                    showReplicateConfirmDialog = false
                    viewModel.replicateWeekSchedule(fromWeek = 1, targetWeeks = listOf(2, 3, 4, 5))
                }) {
                    Text("Replicate Routine")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplicateConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Week Confirmation Dialog
    if (showClearWeekConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearWeekConfirmDialog = false },
            title = { Text("Clear Week $selectedWeek Routine", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete all scheduled entries for Week $selectedWeek?") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearWeekConfirmDialog = false
                        viewModel.clearWeekSchedule(selectedWeek)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Clear Week $selectedWeek")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearWeekConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditScheduleEntryDialog(
    initialEntry: ClassScheduleEntity?,
    existingClassrooms: List<String>,
    allSchedules: List<ClassScheduleEntity>,
    defaultWeekNumber: Int,
    defaultDayOfWeek: String,
    onDismiss: () -> Unit,
    onSaveBatch: (
        baseEntry: ClassScheduleEntity,
        selectedDays: List<String>,
        applyAllWeeks: Boolean,
        updateSubjectAllWeeks: Boolean
    ) -> Unit
) {
    val allDays = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    val dayLabelsShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    var selectedClassName by remember { mutableStateOf(initialEntry?.classroomName ?: existingClassrooms.firstOrNull() ?: "General Class") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var startTimeStr by remember { mutableStateOf(initialEntry?.startTime ?: "09:00 AM") }
    var endTimeStr by remember { mutableStateOf(initialEntry?.endTime ?: "10:00 AM") }

    var applyAllWeeks by remember { mutableStateOf(false) }
    var updateSubjectAllWeeks by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableStateOf(setOf(initialEntry?.dayOfWeek ?: defaultDayOfWeek)) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    // Parse time to minutes from midnight
    fun parseMinutes(timeStr: String): Int {
        return try {
            val clean = timeStr.trim().uppercase(Locale.US)
            val isPm = clean.contains("PM")
            val isAm = clean.contains("AM")
            val rawTime = clean.replace("AM", "").replace("PM", "").trim()
            val separator = if (rawTime.contains(":")) ":" else if (rawTime.contains(".")) "." else " "
            val parts = rawTime.split(separator).filter { it.isNotBlank() }
            var hours = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 9
            val mins = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            if (isPm && hours < 12) hours += 12
            if (isAm && hours == 12) hours = 0
            (hours * 60) + mins
        } catch (e: Exception) {
            540
        }
    }

    val startMin = parseMinutes(startTimeStr)
    val endMin = parseMinutes(endTimeStr)

    // Overlap Check across selected days
    val hasOverlap = selectedDays.any { day ->
        val dayEntries = allSchedules.filter {
            it.weekNumber == defaultWeekNumber && it.dayOfWeek.equals(day, ignoreCase = true)
        }
        dayEntries.any { existing ->
            if (initialEntry != null && existing.id == initialEntry.id) return@any false
            val existStart = existing.startMinutes
            val existEnd = if (existing.endMinutes > 0) existing.endMinutes else existStart + 60
            startMin < existEnd && endMin > existStart
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialEntry == null) "Add Schedule Entry" else "Edit Schedule Entry", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Week $defaultWeekNumber • ${selectedDays.joinToString(", ") { it.take(3) }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // Dropdown for Subject / Classroom Name
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedClassName,
                        onValueChange = { selectedClassName = it },
                        label = { Text("Subject / Classroom Name *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                    )

                    val options = (existingClassrooms + listOf("Homeroom", "Office Hours", "Lab Practice", "Study Hall")).distinct()
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        options.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    selectedClassName = opt
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Time Pickers
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTimeStr,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Start Time *") },
                        trailingIcon = {
                            IconButton(onClick = { showStartTimePicker = true }) {
                                Icon(Icons.Default.Schedule, contentDescription = "Pick Start Time")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showStartTimePicker = true }
                    )
                    OutlinedTextField(
                        value = endTimeStr,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("End Time") },
                        trailingIcon = {
                            IconButton(onClick = { showEndTimePicker = true }) {
                                Icon(Icons.Default.Schedule, contentDescription = "Pick End Time")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showEndTimePicker = true }
                    )
                }

                // Repeat on Days Multi-Select Chips
                Text(
                    text = "Repeat on Days",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    allDays.forEachIndexed { idx, day ->
                        val isSelected = selectedDays.contains(day)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDays = if (isSelected && selectedDays.size > 1) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                            },
                            label = { Text(dayLabelsShort[idx]) }
                        )
                    }
                }

                // Apply to All Weeks Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { applyAllWeeks = !applyAllWeeks }
                ) {
                    Checkbox(
                        checked = applyAllWeeks,
                        onCheckedChange = { applyAllWeeks = it }
                    )
                    Text("Apply to All Weeks (1–5)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                if (initialEntry != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { updateSubjectAllWeeks = !updateSubjectAllWeeks }
                    ) {
                        Checkbox(
                            checked = updateSubjectAllWeeks,
                            onCheckedChange = { updateSubjectAllWeeks = it }
                        )
                        Text("Update across all weeks for this subject", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                if (hasOverlap) {
                    Surface(
                        color = StatusError.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Warning: Time slot conflicts with an existing class on selected day(s)!",
                                fontSize = 11.sp,
                                color = StatusError,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedClassName.isNotBlank()) {
                        val activeClassId = 1L
                        val baseEntry = ClassScheduleEntity(
                            id = initialEntry?.id ?: 0L,
                            classroomId = activeClassId,
                            weekNumber = defaultWeekNumber,
                            dayOfWeek = selectedDays.firstOrNull() ?: defaultDayOfWeek,
                            classroomName = selectedClassName.trim(),
                            startTime = startTimeStr,
                            endTime = endTimeStr,
                            startMinutes = startMin,
                            endMinutes = endMin
                        )
                        onSaveBatch(baseEntry, selectedDays.toList(), applyAllWeeks, updateSubjectAllWeeks)
                    }
                },
                enabled = selectedClassName.isNotBlank()
            ) {
                Text("Save Routine")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Material 3 Start Time Picker Dialog
    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = startMin / 60,
            initialMinute = startMin % 60,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("Select Start Time", fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                Button(onClick = {
                    val h = timePickerState.hour
                    val m = timePickerState.minute
                    val amPm = if (h >= 12) "PM" else "AM"
                    val formattedH = if (h % 12 == 0) 12 else h % 12
                    startTimeStr = String.format(Locale.US, "%02d:%02d %s", formattedH, m, amPm)
                    showStartTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Material 3 End Time Picker Dialog
    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = endMin / 60,
            initialMinute = endMin % 60,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("Select End Time", fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                Button(onClick = {
                    val h = timePickerState.hour
                    val m = timePickerState.minute
                    val amPm = if (h >= 12) "PM" else "AM"
                    val formattedH = if (h % 12 == 0) 12 else h % 12
                    endTimeStr = String.format(Locale.US, "%02d:%02d %s", formattedH, m, amPm)
                    showEndTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
