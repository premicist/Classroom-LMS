package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ExamCategory
import com.example.data.entity.ExamEntity
import com.example.data.entity.ExamMarkEntity
import com.example.data.entity.StudentEntity
import com.example.ui.theme.EduPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.ClassroomViewModel
import com.example.ui.viewmodel.LmsUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExamScreen(
    uiState: LmsUiState,
    viewModel: ClassroomViewModel,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(ExamCategory.CLASS_TEST) }
    
    val categoryTabs = listOf(
        ExamCategory.CLASS_TEST to "Class Test",
        ExamCategory.TERMINAL to "Terminal Exam",
        ExamCategory.PRE_BOARD to "Pre-Board"
    )

    Column(modifier = modifier.fillMaxSize()) {
        // Sub-Category Tabs
        TabRow(
            selectedTabIndex = categoryTabs.indexOfFirst { it.first == selectedCategory },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            categoryTabs.forEachIndexed { index, (category, label) ->
                Tab(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    text = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        // Exam List for selected category
        val examsForCategory = uiState.exams.filter { it.category == selectedCategory }

        if (examsForCategory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No ${selectedCategory.displayName}s found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create a new exam by tapping the '+' button below.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.openAddEditExamDialog(selectedCategory) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New ${selectedCategory.displayName}")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedCategory.displayName} Records (${examsForCategory.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!uiState.activeClassroom?.linkedSpreadsheetId.isNullOrBlank()) {
                                Button(
                                    onClick = { 
                                        uiState.activeClassroom?.id?.let { id -> viewModel.syncGoogleSheet(id) } 
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sync", fontSize = 12.sp)
                                }
                            }
                            Button(
                                onClick = { viewModel.openAddEditExamDialog(selectedCategory) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add New", fontSize = 12.sp)
                            }
                        }
                    }
                }

                items(examsForCategory, key = { it.id }) { exam ->
                    ExamRecordCard(
                        exam = exam,
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun ExamRecordCard(
    exam: ExamEntity,
    uiState: LmsUiState,
    viewModel: ClassroomViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val marks = uiState.examMarks.filter { it.examId == exam.id }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Exam", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${exam.title}' and all associated student marks?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteExam(exam)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    val totalStudents = uiState.students.size.coerceAtLeast(1)
    val gradedCount = marks.count { it.marksObtained != null }
    val passedCount = marks.count { it.marksObtained != null && it.marksObtained >= exam.passMarks }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exam.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = exam.date,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (exam.topicOrChapter.isNotBlank()) {
                            Text(
                                text = " • ${exam.topicOrChapter}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = { viewModel.openAddEditExamDialog(exam.category, exam) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Exam", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Exam", tint = StatusError)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Full Marks", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${exam.fullMarks.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Pass Marks", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${exam.passMarks.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Graded", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$gradedCount / $totalStudents", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (gradedCount == totalStudents) StatusSuccess else EduPrimary)
                }
                Column {
                    Text("Passed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$passedCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (passedCount > 0) StatusSuccess else StatusError)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Hide Mark Entry" else "Enter / View Marks",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { expanded = !expanded }
                        .padding(vertical = 8.dp)
                )
                
                val context = LocalContext.current
                Button(
                    onClick = { viewModel.exportExamReport(context, exam) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export PDF", fontSize = 12.sp)
                }
            }

            // Expanded Marks Grid
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))
                
                uiState.students.forEach { student ->
                    val markEntry = marks.find { it.studentId == student.id }
                    ExamStudentMarkRow(
                        student = student,
                        exam = exam,
                        initialMark = markEntry,
                        onSaveMark = { updatedMark ->
                            viewModel.saveExamMark(updatedMark)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExamStudentMarkRow(
    student: StudentEntity,
    exam: ExamEntity,
    initialMark: ExamMarkEntity?,
    onSaveMark: (ExamMarkEntity) -> Unit
) {
    var marksInput by remember(initialMark) { 
        mutableStateOf(initialMark?.marksObtained?.toString() ?: "") 
    }
    var isAbsent by remember(initialMark) { 
        mutableStateOf(initialMark?.isAbsent ?: false) 
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = student.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = student.studentNumber,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Absent Toggle
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isAbsent) StatusError else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { 
                    isAbsent = !isAbsent
                    if (isAbsent) marksInput = ""
                    val markEntity = initialMark?.copy(
                        marksObtained = null,
                        isAbsent = isAbsent
                    ) ?: ExamMarkEntity(
                        examId = exam.id,
                        studentId = student.id,
                        marksObtained = null,
                        isAbsent = isAbsent
                    )
                    onSaveMark(markEntity)
                }
            ) {
                Text(
                    text = "ABS", 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = if (isAbsent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            OutlinedTextField(
                value = marksInput,
                onValueChange = { newVal ->
                    marksInput = newVal
                    val parsedMarks = newVal.toDoubleOrNull()
                    if (parsedMarks != null && parsedMarks <= exam.fullMarks) {
                        isAbsent = false
                        val markEntity = initialMark?.copy(
                            marksObtained = parsedMarks,
                            isAbsent = false
                        ) ?: ExamMarkEntity(
                            examId = exam.id,
                            studentId = student.id,
                            marksObtained = parsedMarks,
                            isAbsent = false
                        )
                        onSaveMark(markEntity)
                    } else if (newVal.isBlank()) {
                        val markEntity = initialMark?.copy(
                            marksObtained = null
                        ) ?: ExamMarkEntity(
                            examId = exam.id,
                            studentId = student.id,
                            marksObtained = null
                        )
                        onSaveMark(markEntity)
                    }
                },
                placeholder = { Text("0 - ${exam.fullMarks.toInt()}") },
                enabled = !isAbsent,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(90.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )
        }
    }
}
