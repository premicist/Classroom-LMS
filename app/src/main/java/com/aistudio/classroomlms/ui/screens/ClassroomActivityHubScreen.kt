package com.aistudio.classroomlms.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.classroomlms.data.entity.ClassroomActivityCategory
import com.aistudio.classroomlms.data.entity.ClassroomActivityEntity
import com.aistudio.classroomlms.data.entity.StudentEntity
import com.aistudio.classroomlms.ui.viewmodel.ClassroomActivityViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomActivityHubScreen(
    classroomName: String,
    onBack: () -> Unit,
    activityViewModel: ClassroomActivityViewModel = hiltViewModel()
) {
    BackHandler { onBack() }

    val uiState by activityViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(uiState.userNotificationMessage) {
        uiState.userNotificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            activityViewModel.clearToast()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Class Games & Toolkit", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            text = classroomName.ifBlank { "Active Classroom" },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Live Toolkit & Record", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Past Winners (${uiState.activities.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTab == 0) {
                LiveToolkitAndRecorderTab(
                    classroomId = uiState.activeClassroomId ?: 1L,
                    students = uiState.students,
                    generatedGroups = uiState.generatedGroups,
                    pickedRandomStudent = uiState.pickedRandomStudent,
                    onPickRandom = { activityViewModel.pickRandomStudent() },
                    onGenerateGroups = { size -> activityViewModel.generateRandomGroups(size) },
                    onClearGroups = { activityViewModel.clearGroups() },
                    onSaveActivity = { activity ->
                        activityViewModel.saveActivity(activity)
                        selectedTab = 1
                    }
                )
            } else {
                PastActivitiesLogTab(
                    activities = uiState.activities,
                    students = uiState.students,
                    onDelete = { activityViewModel.deleteActivity(it) }
                )
            }
        }
    }
}

private fun playBellChime() {
    runCatching {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 450)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveToolkitAndRecorderTab(
    classroomId: Long,
    students: List<StudentEntity>,
    generatedGroups: List<List<StudentEntity>>,
    pickedRandomStudent: StudentEntity?,
    onPickRandom: () -> Unit,
    onGenerateGroups: (Int) -> Unit,
    onClearGroups: () -> Unit,
    onSaveActivity: (ClassroomActivityEntity) -> Unit
) {
    // Timer / Stopwatch state
    var isTimerMode by rememberSaveable { mutableStateOf(true) }
    var presetSeconds by rememberSaveable { mutableIntStateOf(180) } // 3 min default
    var remainingSeconds by rememberSaveable { mutableIntStateOf(180) }
    var elapsedSeconds by rememberSaveable { mutableIntStateOf(0) }
    var isRunning by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isRunning, isTimerMode) {
        while (isRunning) {
            delay(1000L)
            if (isTimerMode) {
                if (remainingSeconds > 0) {
                    remainingSeconds -= 1
                    if (remainingSeconds == 0) {
                        isRunning = false
                        playBellChime()
                    }
                } else {
                    isRunning = false
                }
            } else {
                elapsedSeconds += 1
            }
        }
    }

    // Activity & Winners state
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var title by rememberSaveable { mutableStateOf("") }
    var category by remember { mutableStateOf(ClassroomActivityCategory.EDUCATIONAL_GAME) }
    var firstWinnerId by rememberSaveable { mutableStateOf<Long?>(null) }
    var secondWinnerId by rememberSaveable { mutableStateOf<Long?>(null) }
    var thirdWinnerId by rememberSaveable { mutableStateOf<Long?>(null) }
    var notes by rememberSaveable { mutableStateOf("") }

    val displaySeconds = if (isTimerMode) remainingSeconds else elapsedSeconds
    val formattedClock = String.format(Locale.US, "%02d:%02d", displaySeconds / 60, displaySeconds % 60)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Timer / Stopwatch + Bell Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = isTimerMode,
                                onClick = {
                                    isRunning = false
                                    isTimerMode = true
                                },
                                label = { Text("⏱️ Timer", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = !isTimerMode,
                                onClick = {
                                    isRunning = false
                                    isTimerMode = false
                                },
                                label = { Text("⏱️ Stopwatch", fontSize = 11.sp) }
                            )
                        }

                        OutlinedButton(
                            onClick = { playBellChime() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = "Ring Bell", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bell", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedClock,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isTimerMode && remainingSeconds == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { isRunning = !isRunning },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isRunning) "Pause" else "Start", fontSize = 12.sp)
                            }
                            IconButton(
                                onClick = {
                                    isRunning = false
                                    if (isTimerMode) remainingSeconds = presetSeconds else elapsedSeconds = 0
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset")
                            }
                        }
                    }

                    if (isTimerMode) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(60 to "1 min", 180 to "3 min", 300 to "5 min", 600 to "10 min").forEach { (secs, label) ->
                                FilterChip(
                                    selected = presetSeconds == secs,
                                    onClick = {
                                        isRunning = false
                                        presetSeconds = secs
                                        remainingSeconds = secs
                                    },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Random Student Picker & Pair/Group Creator
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎲 Random Picker & Groups", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (generatedGroups.isNotEmpty() || pickedRandomStudent != null) {
                            TextButton(onClick = onClearGroups, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                                Text("Clear", fontSize = 11.sp)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPickRandom,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Random Student", fontSize = 11.sp)
                        }
                        listOf(2 to "Pairs (2)", 3 to "Groups of 3", 4 to "Groups of 4", 5 to "Groups of 5").forEach { (size, label) ->
                            OutlinedButton(
                                onClick = { onGenerateGroups(size) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(label, fontSize = 11.sp)
                            }
                        }
                    }

                    pickedRandomStudent?.let { picked ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🎯 Picked: ${picked.name} (Roll: ${picked.studentNumber})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (generatedGroups.isNotEmpty()) {
                        generatedGroups.forEachIndexed { idx, members ->
                            Text(
                                text = "Team ${idx + 1}: ${members.joinToString(", ") { it.name }}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 3. Record Game / Activity & 1st / 2nd / 3rd Winners
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🏆 Record Activity & Winners", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Activity / Game Name *") },
                        placeholder = { Text("e.g. Economics A-Z Terms (3 min)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ClassroomActivityCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text("${cat.emoji} ${cat.displayName}", fontSize = 11.sp) }
                            )
                        }
                    }

                    WinnerDropdownField(
                        label = "🥇 1st Place Winner",
                        students = students,
                        selectedId = firstWinnerId,
                        onSelect = { firstWinnerId = it }
                    )
                    WinnerDropdownField(
                        label = "🥈 2nd Place Winner",
                        students = students,
                        selectedId = secondWinnerId,
                        onSelect = { secondWinnerId = it }
                    )
                    WinnerDropdownField(
                        label = "🥉 3rd Place Winner",
                        students = students,
                        selectedId = thirdWinnerId,
                        onSelect = { thirdWinnerId = it }
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Observation / Enrichment Notes (Optional)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val recordedDuration = if (isTimerMode) presetSeconds else elapsedSeconds.coerceAtLeast(60)
                    Button(
                        onClick = {
                            onSaveActivity(
                                ClassroomActivityEntity(
                                    classroomId = classroomId,
                                    title = title.trim(),
                                    category = category,
                                    date = todayStr,
                                    durationSeconds = recordedDuration,
                                    firstWinnerStudentId = firstWinnerId,
                                    secondWinnerStudentId = secondWinnerId,
                                    thirdWinnerStudentId = thirdWinnerId,
                                    notes = notes.trim()
                                )
                            )
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Activity & Winners")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WinnerDropdownField(
    label: String,
    students: List<StudentEntity>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = students.find { it.id == selectedId }?.let { "${it.name} (${it.studentNumber})" } ?: "None"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            students.sortedBy { it.name }.forEach { s ->
                DropdownMenuItem(
                    text = { Text("${s.name} (Roll: ${s.studentNumber})") },
                    onClick = {
                        onSelect(s.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PastActivitiesLogTab(
    activities: List<ClassroomActivityEntity>,
    students: List<StudentEntity>,
    onDelete: (ClassroomActivityEntity) -> Unit
) {
    if (activities.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(44.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No Classroom Activities Recorded", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    "Use the Live Toolkit tab to run a timed game and record 1st, 2nd & 3rd winners.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(activities, key = { it.id }) { act ->
            val first = students.find { it.id == act.firstWinnerStudentId }?.name
            val second = students.find { it.id == act.secondWinnerStudentId }?.name
            val third = students.find { it.id == act.thirdWinnerStudentId }?.name
            val mins = act.durationSeconds / 60

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${act.category.emoji} ${act.title}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "${act.date} • ${if (mins > 0) "${mins}m" else "${act.durationSeconds}s"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDelete(act) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (first != null || second != null || third != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        first?.let { Text("🥇 1st: $it", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        second?.let { Text("🥈 2nd: $it", fontSize = 12.sp) }
                        third?.let { Text("🥉 3rd: $it", fontSize = 12.sp) }
                    }

                    if (act.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(act.notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
