package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.data.database.LmsSettings
import com.example.ui.viewmodel.ClassroomViewModel
import com.example.ui.viewmodel.LmsUiState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: LmsUiState,
    viewModel: ClassroomViewModel,
    onDismiss: () -> Unit
) {
    var schoolName by remember(uiState.settings) { mutableStateOf(uiState.settings.schoolName) }
    var reportHeader by remember(uiState.settings) { mutableStateOf(uiState.settings.reportHeader) }
    var reportFooter by remember(uiState.settings) { mutableStateOf(uiState.settings.reportFooter) }
    var signaturePlaceholder by remember(uiState.settings) { mutableStateOf(uiState.settings.signaturePlaceholder) }
    var backgroundTemplatePath by remember(uiState.settings) { mutableStateOf(uiState.settings.backgroundTemplatePath) }
    var isLandscapeA4 by remember(uiState.settings) { mutableStateOf(uiState.settings.isLandscapeA4) }
    var isBiometricLockEnabled by remember(uiState.settings) { mutableStateOf(uiState.settings.isBiometricLockEnabled) }

    // Grading Rules & Weightage
    var hwWeight by remember(uiState.settings) { mutableStateOf(uiState.settings.hwWeight.toInt().toString()) }
    var quizWeight by remember(uiState.settings) { mutableStateOf(uiState.settings.quizWeight.toInt().toString()) }
    var projectWeight by remember(uiState.settings) { mutableStateOf(uiState.settings.projectWeight.toInt().toString()) }
    var examWeight by remember(uiState.settings) { mutableStateOf(uiState.settings.examWeight.toInt().toString()) }

    fun performSave() {
        val updatedSettings = LmsSettings(
            schoolName = schoolName,
            reportHeader = reportHeader,
            reportFooter = reportFooter,
            signaturePlaceholder = signaturePlaceholder,
            backgroundTemplatePath = backgroundTemplatePath,
            isLandscapeA4 = isLandscapeA4,
            isBiometricLockEnabled = isBiometricLockEnabled,
            hwWeight = hwWeight.toDoubleOrNull() ?: 20.0,
            quizWeight = quizWeight.toDoubleOrNull() ?: 30.0,
            projectWeight = projectWeight.toDoubleOrNull() ?: 20.0,
            examWeight = examWeight.toDoubleOrNull() ?: 30.0
        )
        viewModel.saveSettings(updatedSettings)
        onDismiss()
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            backgroundTemplatePath = uri.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings Hub & Templates", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { performSave() }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: EXAM, REPORT & PORTFOLIO TEMPLATE CUSTOMIZATION
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Exam, Report & Portfolio Templates",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Create Exam Marks Templates, customize headers, footers, and upload background A4 images for official PDF exports.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = schoolName,
                        onValueChange = { schoolName = it },
                        label = { Text("Institution / School Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = reportHeader,
                        onValueChange = { reportHeader = it },
                        label = { Text("Default Report Header Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = reportFooter,
                        onValueChange = { reportFooter = it },
                        label = { Text("Default Report Footer Text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = signaturePlaceholder,
                        onValueChange = { signaturePlaceholder = it },
                        label = { Text("Signature Line Placeholder") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create Exam Marks Template (PDF Background Overlay)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Upload a designed A4 school letterhead or marksheet layout. Exam results will be overlaid onto this image.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Background")
                        }

                        if (backgroundTemplatePath != null) {
                            Text(
                                text = "Background Selected",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "No background image",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("A4 Orientation: ${if (isLandscapeA4) "Landscape" else "Portrait"}", fontSize = 13.sp)
                        Switch(
                            checked = isLandscapeA4,
                            onCheckedChange = { isLandscapeA4 = it }
                        )
                    }
                }
            }

            // SECTION 2: GRADING RULES & CATEGORY WEIGHTAGE
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Grading Rules & Category Weightage",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Set category weight percentages for final grade and GPA calculations (must sum to 100%):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = hwWeight,
                            onValueChange = { hwWeight = it },
                            label = { Text("Homework %") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = quizWeight,
                            onValueChange = { quizWeight = it },
                            label = { Text("Quizzes %") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = projectWeight,
                            onValueChange = { projectWeight = it },
                            label = { Text("Projects %") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = examWeight,
                            onValueChange = { examWeight = it },
                            label = { Text("Exams %") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val totalWeight = (hwWeight.toIntOrNull() ?: 0) +
                            (quizWeight.toIntOrNull() ?: 0) +
                            (projectWeight.toIntOrNull() ?: 0) +
                            (examWeight.toIntOrNull() ?: 0)

                    Text(
                        text = "Total Weight: $totalWeight% ${if (totalWeight == 100) "✓ Valid" else "⚠️ Must equal 100%"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalWeight == 100) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            // SECTION 3: SECURITY & STUDENT DATA PRIVACY
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Security & Student Data Privacy",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Require device biometric authentication (Fingerprint / Face ID / Screen PIN) when opening Classroom LMS to safeguard student grades, disciplinary records, and guardian contact details.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Biometric / Screen PIN Lock", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (isBiometricLockEnabled) "Enabled — Authenticate on app launch" else "Disabled — Instant access without lock",
                                fontSize = 12.sp,
                                color = if (isBiometricLockEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isBiometricLockEnabled,
                            onCheckedChange = { isBiometricLockEnabled = it }
                        )
                    }
                }
            }

            Button(
                onClick = { performSave() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save All Settings & Rules", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
