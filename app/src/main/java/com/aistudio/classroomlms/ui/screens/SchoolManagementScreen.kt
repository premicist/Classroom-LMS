package com.aistudio.classroomlms.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.classroomlms.data.entity.ClassroomEntity
import com.aistudio.classroomlms.data.entity.InstitutionType
import com.aistudio.classroomlms.data.entity.SchoolEntity
import com.aistudio.classroomlms.ui.viewmodel.SchoolViewModel
import com.aistudio.classroomlms.ui.viewmodel.UNASSIGNED_SCHOOL_ID

/**
 * Launch selector shown when the teacher has configured Schools / Colleges.
 * Selecting an institution (or Not Assigned / All) opens the Landing Screen filtered to it.
 */
@Composable
fun SchoolSelectionScreen(
    schools: List<SchoolEntity>,
    classrooms: List<ClassroomEntity>,
    teacherName: String?,
    onSelectSchool: (Long?) -> Unit,
    onManageSchools: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unassignedCount = classrooms.count { it.schoolId == null }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                if (!teacherName.isNullOrBlank()) {
                    Text(
                        text = "Welcome, $teacherName",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Select School / College",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Choose an institution to view today's schedule, calendar, and classes.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(schools, key = { it.id }) { school ->
            val count = classrooms.count { it.schoolId == school.id }
            val color = Color(school.colorHex)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectSchool(school.id) },
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Apartment,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = school.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val sub = listOf(school.institutionType.displayName, "$count classes", school.address)
                            .filter { it.isNotBlank() }
                            .joinToString(" • ")
                        Text(
                            text = sub,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Enter",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (unassignedCount > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSchool(UNASSIGNED_SCHOOL_ID) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Not Assigned Classes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "$unassignedCount classes not linked to a school yet",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onSelectSchool(null) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("View All (${classrooms.size})", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onManageSchools,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Manage Schools", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Sidebar Management screen for adding/editing/removing Schools & Colleges
 * and assigning classes (including 'Not Assigned').
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolManagementScreen(
    onBack: () -> Unit,
    schoolViewModel: SchoolViewModel = hiltViewModel()
) {
    BackHandler { onBack() }

    val uiState by schoolViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userNotificationMessage) {
        uiState.userNotificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            schoolViewModel.clearToast()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Schools & Colleges", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            "Setup institutions & assign your classes",
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
            FloatingActionButton(onClick = { schoolViewModel.openAddSchool() }) {
                Icon(Icons.Default.Add, contentDescription = "Add School / College")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Institutions (${uiState.schools.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            if (uiState.schools.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("No Schools / Colleges Added", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "Tap '+' to add your School or College, then assign your classes below.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.schools, key = { it.id }) { school ->
                    val count = uiState.classrooms.count { it.schoolId == school.id }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(school.colorHex)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Apartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(school.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "${school.institutionType.displayName} • $count classes",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { schoolViewModel.openEditSchool(school) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { schoolViewModel.deleteSchool(school) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            if (uiState.classrooms.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Text(
                        text = "Assign Classes (${uiState.classrooms.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                items(uiState.classrooms, key = { "cls_${it.id}" }) { cls ->
                    ClassroomSchoolAssignRow(
                        classroom = cls,
                        schools = uiState.schools,
                        onAssign = { schoolId -> schoolViewModel.assignClassroomToSchool(cls.id, schoolId) }
                    )
                }
            }
        }
    }

    if (uiState.isAddEditSchoolOpen) {
        AddEditSchoolDialog(
            initialSchool = uiState.editingSchool,
            onDismiss = { schoolViewModel.closeSchoolDialog() },
            onSave = { schoolViewModel.saveSchool(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassroomSchoolAssignRow(
    classroom: ClassroomEntity,
    schools: List<SchoolEntity>,
    onAssign: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val assignedName = schools.find { it.id == classroom.schoolId }?.name ?: "Not Assigned"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(classroom.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${classroom.subject} • ${classroom.gradeLevel}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.width(175.dp)
            ) {
                OutlinedTextField(
                    value = assignedName,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Not Assigned") },
                        onClick = {
                            onAssign(null)
                            expanded = false
                        }
                    )
                    schools.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.name) },
                            onClick = {
                                onAssign(s.id)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddEditSchoolDialog(
    initialSchool: SchoolEntity?,
    onDismiss: () -> Unit,
    onSave: (SchoolEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialSchool?.name.orEmpty()) }
    var type by remember { mutableStateOf(initialSchool?.institutionType ?: InstitutionType.SCHOOL) }
    var address by remember { mutableStateOf(initialSchool?.address.orEmpty()) }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialSchool == null) "Add School / College" else "Edit School / College",
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
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Institution Name *") },
                    placeholder = { Text("e.g. Shanti Secondary / BCA College") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Type", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InstitutionType.entries.forEach { item ->
                        FilterChip(
                            selected = type == item,
                            onClick = { type = item },
                            label = { Text(item.displayName, fontSize = 12.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Location / Address (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val colorHex = when (type) {
                        InstitutionType.SCHOOL -> 0xFF1E40AF
                        InstitutionType.COLLEGE -> 0xFF0F766E
                        InstitutionType.INSTITUTE -> 0xFF7C3AED
                    }
                    onSave(
                        SchoolEntity(
                            id = initialSchool?.id ?: 0L,
                            name = name.trim(),
                            institutionType = type,
                            address = address.trim(),
                            colorHex = initialSchool?.colorHex ?: colorHex
                        )
                    )
                },
                enabled = name.isNotBlank()
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
