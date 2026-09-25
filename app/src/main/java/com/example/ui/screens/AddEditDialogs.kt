package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.entity.AssignmentEntity
import com.example.data.entity.AssignmentType
import com.example.data.entity.ClassroomEntity
import com.example.data.entity.StudentEntity
import com.example.data.entity.SubmissionEntity
import com.example.data.entity.SubmissionStatus
import com.example.ui.theme.EduPrimary
import com.example.ui.theme.SubjectArt
import com.example.ui.theme.SubjectEnglish
import com.example.ui.theme.SubjectHistory
import com.example.ui.theme.SubjectMath
import com.example.ui.theme.SubjectScience
import com.example.ui.theme.SubjectTech
import com.example.util.StudentPhotoStorage
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddEditClassroomDialog(
    initialClassroom: ClassroomEntity?,
    onDismiss: () -> Unit,
    onSave: (ClassroomEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialClassroom?.name ?: "") }
    var subject by remember { mutableStateOf(initialClassroom?.subject ?: "") }
    var gradeLevel by remember { mutableStateOf(initialClassroom?.gradeLevel ?: "Grade 10") }
    var roomNumber by remember { mutableStateOf(initialClassroom?.roomNumber ?: "Room 204") }
    var scheduleInfo by remember { mutableStateOf(initialClassroom?.scheduleInfo ?: "Mon-Fri 09:00 AM") }
    var selectedColorHex by remember { mutableLongStateOf(initialClassroom?.colorHex ?: 0xFF1E40AF) }

    val colorOptions = listOf(
        0xFF1E40AF, // Primary Indigo
        0xFF059669, // Emerald
        0xFF7C3AED, // Violet
        0xFFD97706, // Amber
        0xFFDB2777, // Pink
        0xFF0891B2  // Cyan
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialClassroom == null) "Create New Classroom" else "Edit Classroom",
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
                    label = { Text("Classroom Name *") },
                    placeholder = { Text("e.g. AP Calculus BC") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject *") },
                    placeholder = { Text("e.g. Mathematics") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = gradeLevel,
                    onValueChange = { gradeLevel = it },
                    label = { Text("Grade Level") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = roomNumber,
                        onValueChange = { roomNumber = it },
                        label = { Text("Room #") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = scheduleInfo,
                        onValueChange = { scheduleInfo = it },
                        label = { Text("Schedule") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f)
                    )
                }

                Text(
                    text = "Classroom Theme Color",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colorOptions.forEach { hex ->
                        val isSelected = selectedColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .clickable { selectedColorHex = hex }
                                .then(
                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && subject.isNotBlank()) {
                        val classroom = initialClassroom?.copy(
                            name = name.trim(),
                            subject = subject.trim(),
                            gradeLevel = gradeLevel.trim(),
                            roomNumber = roomNumber.trim(),
                            scheduleInfo = scheduleInfo.trim(),
                            colorHex = selectedColorHex
                        ) ?: ClassroomEntity(
                            name = name.trim(),
                            subject = subject.trim(),
                            gradeLevel = gradeLevel.trim(),
                            roomNumber = roomNumber.trim(),
                            scheduleInfo = scheduleInfo.trim(),
                            colorHex = selectedColorHex
                        )
                        onSave(classroom)
                    }
                },
                enabled = name.isNotBlank() && subject.isNotBlank()
            ) {
                Text(if (initialClassroom == null) "Create" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddEditStudentDialog(
    classroomId: Long,
    initialStudent: StudentEntity?,
    onDismiss: () -> Unit,
    onSave: (StudentEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialStudent?.name ?: "") }
    var studentNumber by remember {
        mutableStateOf(initialStudent?.studentNumber ?: "STU-${(100..999).random()}")
    }
    var email by remember { mutableStateOf(initialStudent?.email ?: "") }
    var guardianContact by remember { mutableStateOf(initialStudent?.guardianContact ?: "") }
    var notes by remember { mutableStateOf(initialStudent?.notes ?: "") }
    var photoPath by remember { mutableStateOf(initialStudent?.photoPath) }
    var isSavingPhoto by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isSavingPhoto = true
            coroutineScope.launch {
                val previousPath = photoPath
                val savedPath = StudentPhotoStorage.savePhoto(context, uri)
                if (savedPath != null) {
                    photoPath = savedPath
                    // Clean up the old photo file now that it's been replaced.
                    StudentPhotoStorage.deletePhoto(previousPath)
                }
                isSavingPhoto = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialStudent == null) "Add Student" else "Edit Student",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isSavingPhoto -> CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                !photoPath.isNullOrBlank() && File(photoPath!!).exists() -> AsyncImage(
                                    model = File(photoPath!!),
                                    contentDescription = "Student photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(CircleShape)
                                )
                                else -> Icon(
                                    imageVector = Icons.Filled.PhotoCamera,
                                    contentDescription = "Add photo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        if (!photoPath.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                                    .clickable {
                                        val previousPath = photoPath
                                        photoPath = null
                                        coroutineScope.launch {
                                            StudentPhotoStorage.deletePhoto(previousPath)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "Tap to add a photo (optional)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    placeholder = { Text("e.g. Alex Morgan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = studentNumber,
                    onValueChange = { studentNumber = it },
                    label = { Text("Student ID Number *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Student Email") },
                    placeholder = { Text("e.g. student@school.edu") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = guardianContact,
                    onValueChange = { guardianContact = it },
                    label = { Text("Guardian Contact") },
                    placeholder = { Text("e.g. parent@mail.com / (555) 012-3456") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Teacher Notes") },
                    placeholder = { Text("e.g. Needs front row seating") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && studentNumber.isNotBlank()) {
                        val student = initialStudent?.copy(
                            name = name.trim(),
                            studentNumber = studentNumber.trim(),
                            email = email.trim(),
                            guardianContact = guardianContact.trim(),
                            notes = notes.trim(),
                            photoPath = photoPath
                        ) ?: StudentEntity(
                            classroomId = classroomId,
                            name = name.trim(),
                            studentNumber = studentNumber.trim(),
                            email = email.trim(),
                            guardianContact = guardianContact.trim(),
                            notes = notes.trim(),
                            photoPath = photoPath
                        )
                        onSave(student)
                    }
                },
                enabled = name.isNotBlank() && studentNumber.isNotBlank() && !isSavingPhoto
            ) {
                Text(if (initialStudent == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAssignmentDialog(
    classroomId: Long,
    initialAssignment: AssignmentEntity?,
    onDismiss: () -> Unit,
    onSave: (AssignmentEntity) -> Unit
) {
    val defaultDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() + 7 * 86400000L))
    }

    var title by remember { mutableStateOf(initialAssignment?.title ?: "") }
    var description by remember { mutableStateOf(initialAssignment?.description ?: "") }
    var dueDate by remember { mutableStateOf(initialAssignment?.dueDate ?: defaultDate) }
    var maxPointsStr by remember { mutableStateOf(initialAssignment?.maxPoints?.toInt()?.toString() ?: "100") }
    var weightStr by remember { mutableStateOf(initialAssignment?.weightPercentage?.toInt()?.toString() ?: "20") }
    var selectedType by remember { mutableStateOf(initialAssignment?.type ?: AssignmentType.HOMEWORK) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialAssignment == null) "Create Assignment" else "Edit Assignment",
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
                    label = { Text("Assignment Title *") },
                    placeholder = { Text("e.g. Unit 3 Exam") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Assignment Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category / Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        AssignmentType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedType = type
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = maxPointsStr,
                        onValueChange = { maxPointsStr = it },
                        label = { Text("Max Points") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weightStr,
                        onValueChange = { weightStr = it },
                        label = { Text("Weight %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Rubric") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val maxPoints = maxPointsStr.toDoubleOrNull() ?: 100.0
                        val weight = weightStr.toDoubleOrNull() ?: 20.0
                        val assignment = initialAssignment?.copy(
                            title = title.trim(),
                            description = description.trim(),
                            type = selectedType,
                            maxPoints = maxPoints,
                            weightPercentage = weight,
                            dueDate = dueDate.trim()
                        ) ?: AssignmentEntity(
                            classroomId = classroomId,
                            title = title.trim(),
                            description = description.trim(),
                            type = selectedType,
                            maxPoints = maxPoints,
                            weightPercentage = weight,
                            dueDate = dueDate.trim(),
                            assignedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        )
                        onSave(assignment)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(if (initialAssignment == null) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeSubmissionDialog(
    submission: SubmissionEntity,
    assignment: AssignmentEntity,
    student: StudentEntity,
    onDismiss: () -> Unit,
    onSave: (SubmissionEntity) -> Unit
) {
    var scoreStr by remember { mutableStateOf(submission.score?.toInt()?.toString() ?: "") }
    var selectedStatus by remember { mutableStateOf(submission.status) }
    var feedback by remember { mutableStateOf(submission.feedback) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Grade Submission", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("${student.name} • ${assignment.title}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Score Input
                OutlinedTextField(
                    value = scoreStr,
                    onValueChange = {
                        scoreStr = it
                        if (it.isNotBlank()) selectedStatus = SubmissionStatus.GRADED
                    },
                    label = { Text("Score (Out of ${assignment.maxPoints.toInt()} pts)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Submission Status
                ExposedDropdownMenuBox(
                    expanded = statusDropdownExpanded,
                    onExpandedChange = { statusDropdownExpanded = !statusDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedStatus.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Submission Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = statusDropdownExpanded,
                        onDismissRequest = { statusDropdownExpanded = false }
                    ) {
                        SubmissionStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.displayName) },
                                onClick = {
                                    selectedStatus = status
                                    statusDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Teacher Feedback
                OutlinedTextField(
                    value = feedback,
                    onValueChange = { feedback = it },
                    label = { Text("Feedback to Student") },
                    placeholder = { Text("e.g. Great analysis, check question 4 calculation.") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val score = scoreStr.toDoubleOrNull()
                    val updated = submission.copy(
                        score = score,
                        status = if (score != null) SubmissionStatus.GRADED else selectedStatus,
                        feedback = feedback.trim(),
                        submittedDate = if (selectedStatus == SubmissionStatus.SUBMITTED || selectedStatus == SubmissionStatus.GRADED)
                            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) else submission.submittedDate
                    )
                    onSave(updated)
                }
            ) {
                Text("Save Grade")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
