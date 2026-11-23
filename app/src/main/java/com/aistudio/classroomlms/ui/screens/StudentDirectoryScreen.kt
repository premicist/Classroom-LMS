package com.aistudio.classroomlms.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aistudio.classroomlms.data.entity.BehaviorCategory
import com.aistudio.classroomlms.data.entity.DisciplineRecordEntity
import com.aistudio.classroomlms.data.entity.StudentEntity
import com.aistudio.classroomlms.ui.components.GradeBadge
import com.aistudio.classroomlms.ui.components.StudentAvatar
import com.aistudio.classroomlms.ui.components.StudentSortControl
import com.aistudio.classroomlms.ui.theme.statusEnrichment
import com.aistudio.classroomlms.ui.theme.statusEnrichmentContainer
import com.aistudio.classroomlms.ui.theme.statusSuccess
import com.aistudio.classroomlms.ui.theme.statusSuccessContainer
import com.aistudio.classroomlms.ui.theme.statusWarning
import com.aistudio.classroomlms.ui.util.StudentSortOrder
import com.aistudio.classroomlms.ui.util.sortedByStudent
import com.aistudio.classroomlms.ui.viewmodel.ClassroomViewModel
import com.aistudio.classroomlms.ui.viewmodel.LmsUiState
import com.aistudio.classroomlms.ui.viewmodel.RosterViewModel
import com.aistudio.classroomlms.ui.viewmodel.StudentSupportViewModel
import com.aistudio.classroomlms.ui.viewmodel.StudentTier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StudentDirectoryScreen(
    uiState: LmsUiState,
    viewModel: ClassroomViewModel,
    modifier: Modifier = Modifier,
    rosterViewModel: RosterViewModel = hiltViewModel(),
    studentSupportViewModel: StudentSupportViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var studentSortOrder by rememberSaveable { mutableStateOf(StudentSortOrder.NAME) }
    var studentToDelete by remember { mutableStateOf<StudentEntity?>(null) }
    var studentToReassign by remember { mutableStateOf<StudentEntity?>(null) }
    var showQuickStudentPickerForRemark by remember { mutableStateOf(false) }

    val tabs = listOf("Student Directory", "Behavioral Diary & Remarks")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { rosterViewModel.openAddStudent() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Student")
                }
            } else {
                FloatingActionButton(
                    onClick = { showQuickStudentPickerForRemark = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.AddComment, contentDescription = "Log Remark")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Tab Row: Roster vs Behavioral Diary
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (index == 0) {
                                    Icon(Icons.Default.People, null, modifier = Modifier.size(16.dp))
                                } else {
                                    Icon(Icons.Default.AutoStories, null, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                }
            }

            if (selectedTab == 0) {
                // -------------------------------------------------------------
                // TAB 0: STUDENT ROSTER DIRECTORY
                // -------------------------------------------------------------
                val filters = listOf("All", "⚠️ Needs Support", "🌟 Enrichment", "Low Attendance (<85%)")
                val summaries = uiState.studentGradeSummaries
                val filteredSummaries = summaries.filter { summary ->
                    val matchesSearch = if (searchQuery.isNotEmpty()) {
                        summary.student.name.contains(searchQuery, ignoreCase = true) || summary.student.studentNumber.contains(searchQuery, ignoreCase = true)
                    } else true

                    val matchesFilter = when (selectedFilter) {
                        "⚠️ Needs Support" -> summary.tier == StudentTier.EXTRA_SUPPORT
                        "🌟 Enrichment" -> summary.tier == StudentTier.ENRICHMENT_NEEDED
                        "Low Attendance (<85%)" -> summary.attendanceRate < 85.0
                        else -> true
                    }

                    matchesSearch && matchesFilter
                }.sortedByStudent(studentSortOrder) { it.student }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by student name or ID...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        StudentSortControl(
                            order = studentSortOrder,
                            onOrderSelected = { studentSortOrder = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filters) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (filteredSummaries.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (summaries.isEmpty()) "No students have been added to this classroom yet."
                                else "No students match your search or filter.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (summaries.isEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = { rosterViewModel.openAddStudent() }) {
                                    Text("Add Student")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredSummaries, key = { it.student.id }) { summary ->
                                val s = summary.student
                                val (tierBg, tierText) = when (summary.tier) {
                                    StudentTier.EXTRA_SUPPORT -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
                                    StudentTier.ENRICHMENT_NEEDED -> MaterialTheme.colorScheme.statusEnrichmentContainer to MaterialTheme.colorScheme.statusEnrichment
                                    StudentTier.ON_TRACK -> MaterialTheme.colorScheme.statusSuccessContainer to MaterialTheme.colorScheme.statusSuccess
                                }
                                val trajectory = uiState.analytics.studentTrajectories.find { it.student.id == s.id }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.openStudentProfile(summary) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            StudentAvatar(name = s.name, colorHex = s.avatarColorHex, photoPath = s.photoPath, sizeDp = 44)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = s.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = "ID: ${s.studentNumber}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Box(
                                                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(tierBg).padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(text = summary.tier.shortLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = tierText)
                                                    }
                                                }
                                            }
                                            GradeBadge(letterGrade = summary.letterGrade, percentage = summary.percentage)

                                            var showActionMenu by remember { mutableStateOf(false) }
                                            Box {
                                                IconButton(onClick = { showActionMenu = true }) {
                                                    Icon(
                                                        imageVector = Icons.Default.MoreVert,
                                                        contentDescription = "Options",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = showActionMenu,
                                                    onDismissRequest = { showActionMenu = false }
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("Log Remark / Praise") },
                                                        onClick = {
                                                            showActionMenu = false
                                                            studentSupportViewModel.openAddDiscipline(s)
                                                        },
                                                        leadingIcon = { Icon(Icons.Default.AddComment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Edit Student") },
                                                        onClick = {
                                                            showActionMenu = false
                                                            rosterViewModel.openEditStudent(s)
                                                        },
                                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Move to Another Class") },
                                                        onClick = {
                                                            showActionMenu = false
                                                            studentToReassign = s
                                                        },
                                                        leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Delete Student", color = MaterialTheme.colorScheme.error) },
                                                        onClick = {
                                                            showActionMenu = false
                                                            studentToDelete = s
                                                        },
                                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("GPA", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("%.2f".format(summary.gpa), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("HW", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("${summary.homeworkCompletionRate.toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Attend", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("${summary.attendanceRate.toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (summary.attendanceRate < 85.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.statusSuccess)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Trend", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                val trendStr = if ((trajectory?.trendDelta ?: 0.0) >= 0) "↗" else "↘"
                                                Text("$trendStr ${trajectory?.trend?.label?.take(8) ?: "Steady"}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // -------------------------------------------------------------
                // TAB 1: CLASSROOM BEHAVIORAL DIARY & REMARKS FEED
                // -------------------------------------------------------------
                ClassroomDiaryFeedView(
                    uiState = uiState,
                    onOpenAddRemarkForStudent = { s -> studentSupportViewModel.openAddDiscipline(s) },
                    onEditRemark = { disc, s -> studentSupportViewModel.openEditDiscipline(disc, s) },
                    onDeleteRemark = { disc -> studentSupportViewModel.deleteDisciplineRecord(disc) },
                    onSendWhatsAppRemark = { disc, s -> studentSupportViewModel.sendWhatsAppRemark(context, s, disc) },
                    onSendSmsRemark = { disc, s -> studentSupportViewModel.sendSmsRemark(context, s, disc) }
                )
            }
        }
    }

    // Quick Student Picker for logging remark from the diary feed
    if (showQuickStudentPickerForRemark) {
        AlertDialog(
            onDismissRequest = { showQuickStudentPickerForRemark = false },
            title = { Text("Select Student for Remark", fontWeight = FontWeight.Bold) },
            text = {
                val students = uiState.students
                if (students.isEmpty()) {
                    Text("No students in this classroom.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(students, key = { it.id }) { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        showQuickStudentPickerForRemark = false
                                        studentSupportViewModel.openAddDiscipline(s)
                                    }
                                    .padding(vertical = 8.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StudentAvatar(name = s.name, colorHex = s.avatarColorHex, photoPath = s.photoPath, sizeDp = 32)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(s.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Roll: ${s.studentNumber}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showQuickStudentPickerForRemark = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (studentToReassign != null) {
        val s = studentToReassign!!
        AlertDialog(
            onDismissRequest = { studentToReassign = null },
            title = { Text("Move ${s.name} to Classroom") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select the destination classroom:", style = MaterialTheme.typography.bodyMedium)
                    uiState.classrooms.filter { it.id != s.classroomId }.forEach { targetClass ->
                        Button(
                            onClick = {
                                rosterViewModel.reassignStudentClassroom(s, targetClass.id)
                                studentToReassign = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${targetClass.name} (${targetClass.subject})")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { studentToReassign = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (studentToDelete != null) {
        val s = studentToDelete!!
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("Delete Student") },
            text = { Text("Are you sure you want to remove '${s.name}'? All grades, attendance, and homework records for this student will be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        rosterViewModel.deleteStudent(s.id)
                        studentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// CLASSROOM BEHAVIORAL DIARY FEED COMPOSABLE
// -------------------------------------------------------------
@Composable
fun ClassroomDiaryFeedView(
    uiState: LmsUiState,
    onOpenAddRemarkForStudent: (StudentEntity) -> Unit,
    onEditRemark: (DisciplineRecordEntity, StudentEntity) -> Unit,
    onDeleteRemark: (DisciplineRecordEntity) -> Unit,
    onSendWhatsAppRemark: (DisciplineRecordEntity, StudentEntity) -> Unit,
    onSendSmsRemark: (DisciplineRecordEntity, StudentEntity) -> Unit
) {
    val records = uiState.disciplineRecords
    val studentMap = remember(uiState.students) { uiState.students.associateBy { it.id } }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "🌟 Praise / Merits", "⚠️ Cautions", "Today")

    val meritsCount = records.count { it.category.isPositive }
    val cautionsCount = records.count { !it.category.isPositive }

    val filteredRecords = records.filter { record ->
        when (selectedFilter) {
            "🌟 Praise / Merits" -> record.category.isPositive
            "⚠️ Cautions" -> !record.category.isPositive
            "Today" -> record.date == todayStr
            else -> true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp)
    ) {
        // Summary Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.statusSuccessContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🌟 Merits & Praise", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.statusSuccess)
                    Text("$meritsCount Logged", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.statusSuccess)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("⚠️ Cautions", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text("$cautionsCount Logged", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filters
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredRecords.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.AutoStories, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No Remarks in Diary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tap the '+' button to record praise, merits, or behavioral observations for any student in this class.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredRecords, key = { it.id }) { record ->
                    val student = studentMap[record.studentId]
                    val isPraise = record.category.isPositive

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Top Row: Student Avatar + Name + Category Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    StudentAvatar(
                                        name = student?.name ?: "Student",
                                        colorHex = student?.avatarColorHex ?: 0xFF3B82F6,
                                        photoPath = student?.photoPath,
                                        sizeDp = 34
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(student?.name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Roll: ${student?.studentNumber ?: "-"} • ${record.date}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isPraise) MaterialTheme.colorScheme.statusSuccessContainer else MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = if (isPraise) "🌟 ${record.category.displayName}" else "⚠️ ${record.category.displayName}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPraise) MaterialTheme.colorScheme.statusSuccess else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(record.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            if (record.description.isNotBlank()) {
                                Text(record.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (record.actionTaken.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Action: ${record.actionTaken}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Bottom Row: Parent Notification status + 1-Tap WhatsApp & SMS
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (record.parentNotified) "✓ Parent Notified" else "Pending Parent Notice",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (record.parentNotified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (student != null && student.guardianContact.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = { onSendSmsRemark(record, student) },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Message, null, modifier = Modifier.size(11.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("SMS", fontSize = 10.sp)
                                        }

                                        Button(
                                            onClick = { onSendWhatsAppRemark(record, student) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(11.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("WA", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteRemark(record) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
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
