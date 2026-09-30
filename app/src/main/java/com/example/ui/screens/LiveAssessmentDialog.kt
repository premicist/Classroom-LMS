package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.entity.LiveAssessmentEntity
import com.example.data.entity.LiveAssessmentTaskType
import com.example.data.entity.MasteryLevel
import com.example.data.entity.StudentEntity
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorBg
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoBg
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningBg
import com.example.util.StudentPhotoStorage
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LiveAssessmentDialog(
    initialStudent: StudentEntity?,
    students: List<StudentEntity>,
    initialAssessment: LiveAssessmentEntity?,
    classroomId: Long,
    onDismiss: () -> Unit,
    onSave: (LiveAssessmentEntity) -> Unit,
    onDelete: ((LiveAssessmentEntity) -> Unit)? = null
) {
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedStudent by remember {
        mutableStateOf(initialStudent ?: students.firstOrNull())
    }
    var taskType by remember {
        mutableStateOf(initialAssessment?.taskType ?: LiveAssessmentTaskType.DIAGRAM)
    }
    var topic by remember {
        mutableStateOf(initialAssessment?.topic ?: "")
    }
    var masteryLevel by remember {
        mutableStateOf(initialAssessment?.masteryLevel ?: MasteryLevel.MASTERED)
    }
    var selectedTags by remember {
        mutableStateOf(
            if (!initialAssessment?.diagnosticTags.isNullOrBlank()) {
                initialAssessment!!.diagnosticTags.split(", ").filter { it.isNotBlank() }.toSet()
            } else {
                emptySet()
            }
        )
    }
    var remarks by remember {
        mutableStateOf(initialAssessment?.remarks ?: "")
    }
    var photoEvidencePath by remember {
        mutableStateOf(initialAssessment?.photoEvidencePath)
    }
    var isSavingPhoto by remember { mutableStateOf(false) }
    var isStudentDropdownExpanded by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isSavingPhoto = true
            coroutineScope.launch {
                val savedPath = StudentPhotoStorage.savePhoto(context, uri)
                if (savedPath != null) {
                    photoEvidencePath = savedPath
                }
                isSavingPhoto = false
            }
        }
    }

    val diagnosticTagPresets = listOf(
        "✓ Clear Steps",
        "✓ Confident Delivery",
        "✓ Accurate Diagram/Graph",
        "✓ Strong Recall",
        "⚠️ Calculation Slip",
        "⚠️ Missing Labels/Units",
        "⚠️ Inverted Steps",
        "⚠️ Needs Guided Practice"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (initialAssessment == null) "⚡ Live Class Assessment" else "Edit Live Assessment",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Instant Formative & Observational Evaluation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (initialAssessment != null && onDelete != null) {
                    IconButton(onClick = { onDelete(initialAssessment) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusError)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Student Selector + Random Student Picker (Cold Caller)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = isStudentDropdownExpanded,
                        onExpandedChange = { isStudentDropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedStudent?.name ?: "Select Student",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Student *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStudentDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = isStudentDropdownExpanded,
                            onDismissRequest = { isStudentDropdownExpanded = false }
                        ) {
                            students.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("${s.name} (${s.studentNumber})") },
                                    onClick = {
                                        selectedStudent = s
                                        isStudentDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Cold-Call Randomizer
                    Button(
                        onClick = {
                            if (students.isNotEmpty()) {
                                selectedStudent = students.random()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Casino, contentDescription = "Random", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Random", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 2. Task Type Selector
                Column {
                    Text(
                        text = "Task Type *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LiveAssessmentTaskType.entries.forEach { type ->
                            FilterChip(
                                selected = taskType == type,
                                onClick = { taskType = type },
                                label = { Text("${type.iconEmoji} ${type.displayName}", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // 3. Topic / Subject Context
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic / Problem Prompt *") },
                    placeholder = { Text("e.g. Ray diagram for convex mirror, Q3 page 45") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 4. Mastery Level (3-Tier Formative Grading)
                Column {
                    Text(
                        text = "Mastery Level (Instant Score) *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MasteryLevel.entries.forEach { level ->
                            val isSelected = masteryLevel == level
                            val (bg, fg) = when (level) {
                                MasteryLevel.MASTERED -> StatusSuccessBg to StatusSuccess
                                MasteryLevel.DEVELOPING -> StatusWarningBg to StatusWarning
                                MasteryLevel.NEEDS_SUPPORT -> StatusErrorBg to StatusError
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { masteryLevel = level },
                                color = if (isSelected) bg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, fg) else null
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = level.shortLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) fg else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${level.points.toInt()}/3 pts",
                                        fontSize = 10.sp,
                                        color = if (isSelected) fg.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Diagnostic Quick Observation Tags
                Column {
                    Text(
                        text = "Diagnostic Observation Tags",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        diagnosticTagPresets.forEach { tag ->
                            val isSelected = tag in selectedTags
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedTags = if (isSelected) selectedTags - tag else selectedTags + tag
                                },
                                label = { Text(tag, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // 6. Optional Photo Evidence (Whiteboard / Work Snapshot)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!photoEvidencePath.isNullOrBlank() && File(photoEvidencePath!!).exists()) {
                                AsyncImage(
                                    model = File(photoEvidencePath!!),
                                    contentDescription = "Whiteboard photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Photo Attached", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StatusSuccess)
                            } else {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Snap Whiteboard/Work", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        if (!photoEvidencePath.isNullOrBlank()) {
                            IconButton(onClick = { photoEvidencePath = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove Photo", tint = StatusError)
                            }
                        } else {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isSavingPhoto) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                                } else {
                                    Text("Attach Photo", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // 7. Teacher Remarks
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Teacher Remarks / Diagnostic Feedback") },
                    placeholder = { Text("e.g. Good grasp of formula, reviewed algebraic sign rule with student.") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedStudent != null && topic.isNotBlank()) {
                        val assessment = initialAssessment?.copy(
                            studentId = selectedStudent!!.id,
                            classroomId = classroomId,
                            taskType = taskType,
                            topic = topic.trim(),
                            masteryLevel = masteryLevel,
                            diagnosticTags = selectedTags.joinToString(", "),
                            remarks = remarks.trim(),
                            photoEvidencePath = photoEvidencePath
                        ) ?: LiveAssessmentEntity(
                            studentId = selectedStudent!!.id,
                            classroomId = classroomId,
                            date = todayStr,
                            taskType = taskType,
                            topic = topic.trim(),
                            masteryLevel = masteryLevel,
                            diagnosticTags = selectedTags.joinToString(", "),
                            remarks = remarks.trim(),
                            photoEvidencePath = photoEvidencePath
                        )
                        onSave(assessment)
                    }
                },
                enabled = selectedStudent != null && topic.isNotBlank() && !isSavingPhoto
            ) {
                Text(if (initialAssessment == null) "Record Assessment" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
