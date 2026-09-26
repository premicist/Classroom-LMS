package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.entity.DailyLogEntity
import com.example.data.entity.LessonPlanEntity
import com.example.ui.viewmodel.ClassroomViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.emptyFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    classroomId: Long,
    viewModel: ClassroomViewModel,
    onBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Lesson Plans", "Daily Diary")

    // In a real app we'd expose these flows from ViewModel, mocking for this boilerplate
    val plans by remember { mutableStateOf(emptyList<LessonPlanEntity>()) }
    val logs by remember { mutableStateOf(emptyList<DailyLogEntity>()) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Teacher Planner") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Open Add Dialog based on tab */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTabIndex) {
                0 -> LessonPlansTab(plans)
                1 -> DailyDiaryTab(logs)
            }
        }
    }
}

@Composable
fun LessonPlansTab(plans: List<LessonPlanEntity>) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (plans.isEmpty()) {
            item { Text("No lesson plans yet.") }
        }
        items(plans) { plan ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(plan.unitTitle, style = MaterialTheme.typography.titleMedium)
                    Text(plan.description, style = MaterialTheme.typography.bodyMedium)
                    Text("Status: ${plan.status}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun DailyDiaryTab(logs: List<DailyLogEntity>) {
    val df = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (logs.isEmpty()) {
            item { Text("No diary logs yet.") }
        }
        items(logs) { log ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(df.format(Date(log.date)), style = MaterialTheme.typography.titleMedium)
                    Text(log.reflectionNotes, style = MaterialTheme.typography.bodyMedium)
                    if (log.wasProxyClass) {
                        Badge { Text("Proxy Class") }
                    }
                }
            }
        }
    }
}