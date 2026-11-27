package com.aistudio.classroomlms.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.classroomlms.data.entity.AcademicEventCategory
import com.aistudio.classroomlms.data.entity.AcademicEventEntity
import com.aistudio.classroomlms.ui.viewmodel.AcademicCalendarViewModel
import com.aistudio.classroomlms.util.NepaliDateConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicCalendarScreen(
    onBack: () -> Unit,
    calendarViewModel: AcademicCalendarViewModel = hiltViewModel()
) {
    BackHandler { onBack() }

    val uiState by calendarViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userNotificationMessage) {
        uiState.userNotificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            calendarViewModel.clearToast()
        }
    }

    val filteredEvents = remember(uiState.events, uiState.selectedCategory) {
        if (uiState.selectedCategory == null) uiState.events
        else uiState.events.filter { it.category == uiState.selectedCategory }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Academic Year & Calendar", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            "Holidays, festival breaks, exam windows & events",
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { calendarViewModel.openAddEvent() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Compact Category Filter Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedCategory == null,
                    onClick = { calendarViewModel.selectCategoryFilter(null) },
                    label = { Text("All (${uiState.events.size})", fontSize = 11.sp) },
                    modifier = Modifier.height(30.dp)
                )
                AcademicEventCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = uiState.selectedCategory == cat,
                        onClick = { calendarViewModel.selectCategoryFilter(cat) },
                        label = { Text("${cat.emoji} ${cat.displayName}", fontSize = 11.sp) },
                        modifier = Modifier.height(30.dp)
                    )
                }
            }

            if (filteredEvents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No Academic Events Planned", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Add holidays, festival breaks, or exam windows. Upcoming dates automatically appear in your Morning Briefing.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredEvents, key = { it.id }) { event ->
                        AcademicEventCard(
                            event = event,
                            onEdit = { calendarViewModel.openEditEvent(event) },
                            onDelete = { calendarViewModel.deleteEvent(event) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.isAddEditDialogOpen) {
        AddEditAcademicEventDialog(
            initialEvent = uiState.editingEvent,
            onDismiss = { calendarViewModel.closeDialog() },
            onSave = { event -> calendarViewModel.saveEvent(event) }
        )
    }
}

@Composable
private fun AcademicEventCard(
    event: AcademicEventEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val bsStart = remember(event.startDate) {
        NepaliDateConverter.adDateStringToBsDevanagari(event.startDate, includeWeekday = false)
    }
    val bsEnd = remember(event.endDate) {
        if (event.endDate != event.startDate) {
            NepaliDateConverter.adDateStringToBsDevanagari(event.endDate, includeWeekday = false)
        } else {
            ""
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${event.category.emoji} ${event.title}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                    if (event.academicYearLabel.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = event.academicYearLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                val adRange = if (event.endDate != event.startDate) {
                    "${event.startDate} → ${event.endDate}"
                } else {
                    event.startDate
                }
                val bsRange = when {
                    bsStart.isBlank() -> ""
                    bsEnd.isNotBlank() -> "$bsStart – $bsEnd"
                    else -> bsStart
                }

                Text(
                    text = if (bsRange.isNotBlank()) "$bsRange (वि.सं.)  •  $adRange" else adRange,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                if (event.notes.isNotBlank()) {
                    Text(
                        text = event.notes,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddEditAcademicEventDialog(
    initialEvent: AcademicEventEntity?,
    onDismiss: () -> Unit,
    onSave: (AcademicEventEntity) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false } }
    val todayStr = remember { dateFormat.format(Date()) }

    var title by remember { mutableStateOf(initialEvent?.title.orEmpty()) }
    var category by remember { mutableStateOf(initialEvent?.category ?: AcademicEventCategory.HOLIDAY) }
    var startDate by remember { mutableStateOf(initialEvent?.startDate ?: todayStr) }
    var endDate by remember { mutableStateOf(initialEvent?.endDate ?: todayStr) }
    var academicYearLabel by remember { mutableStateOf(initialEvent?.academicYearLabel ?: "२०८३ / 2026-27") }
    var notes by remember { mutableStateOf(initialEvent?.notes.orEmpty()) }

    val startValid = remember(startDate) { runCatching { dateFormat.parse(startDate) }.getOrNull() != null }
    val endValid = remember(endDate) { runCatching { dateFormat.parse(endDate) }.getOrNull() != null }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialEvent == null) "Add Academic Calendar Event" else "Edit Academic Event",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event / Holiday Title *") },
                    placeholder = { Text("e.g. Dashain Break, First Terminal Exam") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AcademicEventCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text("${cat.emoji} ${cat.displayName}", fontSize = 11.sp) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Start (YYYY-MM-DD)") },
                        isError = !startValid,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("End (YYYY-MM-DD)") },
                        isError = !endValid,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                val bsPreview = remember(startDate) {
                    if (startValid) NepaliDateConverter.adDateStringToBsDevanagari(startDate, includeWeekday = true) else ""
                }
                if (bsPreview.isNotBlank()) {
                    Text(
                        text = "BS: $bsPreview",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = academicYearLabel,
                    onValueChange = { academicYearLabel = it },
                    label = { Text("Academic Year") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val normalizedEnd = if (endDate < startDate) startDate else endDate
                    onSave(
                        AcademicEventEntity(
                            id = initialEvent?.id ?: 0L,
                            title = title.trim(),
                            category = category,
                            startDate = startDate.trim(),
                            endDate = normalizedEnd.trim(),
                            notes = notes.trim(),
                            academicYearLabel = academicYearLabel.trim()
                        )
                    )
                },
                enabled = title.isNotBlank() && startValid && endValid
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
