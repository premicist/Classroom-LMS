package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.ui.theme.StatusError
import com.example.ui.viewmodel.ClassroomViewModel
import com.example.ui.viewmodel.LmsUiState

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

    val daysOfWeek = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val currentDayOfWeek = daysOfWeek[selectedDayIndex]

    val activeClassName = uiState.activeClassroom?.name ?: "Classroom A"

    // Filtered schedules for selected week & day
    val selectedDaySchedules = uiState.schedules.filter {
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
                            text = uiState.activeClassroom?.name ?: "5-Week Schedule Routine",
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
            if (viewMode == ScheduleViewMode.WEEKLY_DAY) {
                // Week Selector Chips (1 to 5)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Week:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    (1..5).forEach { week ->
                        val isSelected = week == selectedWeek
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedWeek = week },
                            label = { Text("Week $week") }
                        )
                    }
                }

                // Day Tabs
                SecondaryTabRow(
                    selectedTabIndex = selectedDayIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    dayLabels.forEachIndexed { index, label ->
                        Tab(
                            selected = selectedDayIndex == index,
                            onClick = { selectedDayIndex = index },
                            text = { Text(label, fontSize = 12.sp, fontWeight = if (selectedDayIndex == index) FontWeight.Bold else FontWeight.Normal) }
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
                        text = "Week $selectedWeek • ${daysOfWeek[selectedDayIndex]} (${selectedDaySchedules.size} class${if (selectedDaySchedules.size == 1) "" else "es"})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Button(
                        onClick = { showAddDialog = true },
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
                            Button(onClick = { showAddDialog = true }) {
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
                        items(selectedDaySchedules) { entry ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = entry.classroomName,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (entry.endTime.isNotBlank()) "${entry.startTime} – ${entry.endTime}" else entry.startTime,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
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
                                text = "Complete 5-Week Schedule Routine",
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
                                    val entriesForDay = uiState.schedules.filter {
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
                                                items(entriesForDay) { cls ->
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = "${cls.classroomName} @ ${cls.startTime}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
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

    // Add Schedule Entry Dialog
    if (showAddDialog) {
        AddScheduleEntryDialog(
            defaultClassName = activeClassName,
            existingEntries = selectedDaySchedules,
            weekNumber = selectedWeek,
            dayOfWeek = currentDayOfWeek,
            onDismiss = { showAddDialog = false },
            onSave = { name, startTime, endTime, startMin, endMin ->
                val activeClassId = uiState.activeClassroom?.id ?: 1L
                val entry = ClassScheduleEntity(
                    classroomId = activeClassId,
                    weekNumber = selectedWeek,
                    dayOfWeek = currentDayOfWeek,
                    classroomName = name,
                    startTime = startTime,
                    endTime = endTime,
                    startMinutes = startMin,
                    endMinutes = endMin
                )
                viewModel.saveScheduleEntry(entry)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddScheduleEntryDialog(
    defaultClassName: String,
    existingEntries: List<ClassScheduleEntity>,
    weekNumber: Int,
    dayOfWeek: String,
    onDismiss: () -> Unit,
    onSave: (name: String, startTime: String, endTime: String, startMin: Int, endMin: Int) -> Unit
) {
    var className by remember { mutableStateOf(defaultClassName) }
    var startTimeStr by remember { mutableStateOf("09:00 AM") }
    var endTimeStr by remember { mutableStateOf("10:00 AM") }

    // Convert e.g. "09:00 AM" or "14:30" to minutes from midnight
    fun parseMinutes(timeStr: String): Int {
        return try {
            val clean = timeStr.trim().uppercase()
            val isPm = clean.contains("PM")
            val isAm = clean.contains("AM")
            val rawTime = clean.replace("AM", "").replace("PM", "").trim()
            val parts = rawTime.split(":")
            var hours = parts[0].trim().toInt()
            val mins = if (parts.size > 1) parts[1].trim().toInt() else 0
            if (isPm && hours < 12) hours += 12
            if (isAm && hours == 12) hours = 0
            (hours * 60) + mins
        } catch (e: Exception) {
            540 // Default 9:00 AM
        }
    }

    val startMin = parseMinutes(startTimeStr)
    val endMin = parseMinutes(endTimeStr)

    // Overlap Check
    val hasOverlap = existingEntries.any { existing ->
        val existStart = existing.startMinutes
        val existEnd = if (existing.endMinutes > 0) existing.endMinutes else existStart + 60
        startMin < existEnd && endMin > existStart
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Class Schedule Entry", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Week $weekNumber • $dayOfWeek",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text("Classroom / Subject Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTimeStr,
                        onValueChange = { startTimeStr = it },
                        label = { Text("Start Time *") },
                        placeholder = { Text("e.g. 09:00 AM") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTimeStr,
                        onValueChange = { endTimeStr = it },
                        label = { Text("End Time") },
                        placeholder = { Text("e.g. 10:00 AM") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
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
                                text = "Warning: Time overlaps with another class scheduled on $dayOfWeek!",
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
                    if (className.isNotBlank() && startTimeStr.isNotBlank()) {
                        onSave(className.trim(), startTimeStr.trim(), endTimeStr.trim(), startMin, endMin)
                    }
                },
                enabled = className.isNotBlank() && startTimeStr.isNotBlank()
            ) {
                Text("Save Class")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
