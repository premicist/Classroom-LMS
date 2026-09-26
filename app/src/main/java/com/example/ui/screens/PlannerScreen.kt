package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.DailyLogEntity
import com.example.data.entity.LessonPlanEntity
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.ClassroomViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    classroomId: Long,
    viewModel: ClassroomViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Lesson Plans (${uiState.lessonPlans.size})",
        "Daily Diary (${uiState.dailyLogs.size})"
    )

    var planToDelete by remember { mutableStateOf<LessonPlanEntity?>(null) }
    var logToDelete by remember { mutableStateOf<DailyLogEntity?>(null) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Teacher Planner & Diary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            uiState.activeClassroom?.let { cls ->
                                Text(
                                    text = "${cls.name} • ${cls.subject}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (selectedTabIndex == 0) {
                                    viewModel.exportLessonPlansPdf(context)
                                } else {
                                    viewModel.exportDailyDiaryPdf(context)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Export PDF",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 0) {
                        viewModel.openAddLessonPlan()
                    } else {
                        viewModel.openAddDailyLog()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (selectedTabIndex) {
                0 -> LessonPlansTab(
                    plans = uiState.lessonPlans,
                    onEdit = { viewModel.openEditLessonPlan(it) },
                    onDelete = { planToDelete = it },
                    onCycleStatus = { plan ->
                        val nextStatus = when (plan.status) {
                            "PLANNED" -> "IN_PROGRESS"
                            "IN_PROGRESS" -> "COMPLETED"
                            else -> "PLANNED"
                        }
                        viewModel.updateLessonPlanStatus(plan, nextStatus)
                    }
                )
                1 -> DailyDiaryTab(
                    logs = uiState.dailyLogs,
                    onEdit = { viewModel.openEditDailyLog(it) },
                    onDelete = { logToDelete = it }
                )
            }
        }
    }

    // Modal Dialogs
    if (uiState.isAddEditLessonPlanOpen) {
        AddEditLessonPlanDialog(
            initialPlan = uiState.editingLessonPlan,
            onDismiss = { viewModel.closeLessonPlanDialog() },
            onSave = { unitTitle, description, targetDate, status ->
                viewModel.saveLessonPlan(unitTitle, description, targetDate, status)
            }
        )
    }

    if (uiState.isAddEditDailyLogOpen) {
        AddEditDailyLogDialog(
            initialLog = uiState.editingDailyLog,
            onDismiss = { viewModel.closeDailyLogDialog() },
            onSave = { date, reflectionNotes, wasProxyClass ->
                viewModel.saveDailyLog(date, reflectionNotes, wasProxyClass)
            }
        )
    }

    // Delete Confirmation Dialogs
    planToDelete?.let { plan ->
        AlertDialog(
            onDismissRequest = { planToDelete = null },
            title = { Text("Delete Lesson Plan") },
            text = { Text("Are you sure you want to delete '${plan.unitTitle}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLessonPlan(plan)
                        planToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = StatusError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { planToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    logToDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("Delete Diary Entry") },
            text = { Text("Are you sure you want to delete this daily diary entry?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDailyLog(log)
                        logToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = StatusError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LessonPlansTab(
    plans: List<LessonPlanEntity>,
    onEdit: (LessonPlanEntity) -> Unit,
    onDelete: (LessonPlanEntity) -> Unit,
    onCycleStatus: (LessonPlanEntity) -> Unit
) {
    val df = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (plans.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Lesson Plans Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the '+' button below to create your first lesson plan for this classroom.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(plans, key = { it.id }) { plan ->
                val (badgeBg, badgeText) = when (plan.status) {
                    "IN_PROGRESS" -> StatusWarning to "In Progress"
                    "COMPLETED" -> StatusSuccess to "Completed"
                    else -> StatusInfo to "Planned"
                }

                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = plan.unitTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AssistChip(
                                onClick = { onCycleStatus(plan) },
                                label = { Text(badgeText, style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = badgeBg.copy(alpha = 0.15f),
                                    labelColor = badgeBg
                                )
                            )
                        }

                        Text(
                            text = "Target Date: ${df.format(Date(plan.targetDate))}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = plan.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(onClick = { onCycleStatus(plan) }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cycle Status")
                            }

                            Row {
                                IconButton(onClick = { onEdit(plan) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { onDelete(plan) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusError)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyDiaryTab(
    logs: List<DailyLogEntity>,
    onEdit: (DailyLogEntity) -> Unit,
    onDelete: (DailyLogEntity) -> Unit
) {
    val df = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (logs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Daily Diary Entries Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the '+' button below to add reflection notes or record proxy classes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(logs, key = { it.id }) { log ->
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = df.format(Date(log.date)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            if (log.wasProxyClass) {
                                Spacer(modifier = Modifier.width(8.dp))
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Proxy Class", style = MaterialTheme.typography.labelSmall) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = StatusWarning.copy(alpha = 0.2f),
                                        labelColor = StatusWarning
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = log.reflectionNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { onEdit(log) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { onDelete(log) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusError)
                            }
                        }
                    }
                }
            }
        }
    }
}
