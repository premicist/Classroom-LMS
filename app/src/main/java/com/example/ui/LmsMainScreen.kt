package com.example.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Grading
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Grading
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ClassroomEntity
import com.example.data.entity.StudentEntity
import com.example.ui.components.ClassroomHeader
import com.example.ui.dialogs.InterventionDialog
import com.example.ui.screens.AddEditAssignmentDialog
import com.example.ui.screens.AddEditClassroomDialog
import com.example.ui.screens.AddEditStudentDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AssignmentsScreen
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.ClassroomManagementDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GradeSubmissionDialog
import com.example.ui.screens.GradebookScreen
import com.example.ui.screens.HomeworkCheckScreen
import com.example.ui.screens.ReportExportDialog
import com.example.ui.screens.StudentDetailDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.ui.platform.LocalContext
import com.example.ui.dialogs.UpdateAlertDialog
import com.example.ui.screens.GenerateReportDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.BuildConfig
import com.example.ui.screens.ClassroomScheduleScreen
import com.example.ui.screens.DisciplineLogDialog
import com.example.ui.screens.LinkGoogleSheetDialog
import com.example.ui.screens.PlannerScreen
import com.example.ui.screens.StudentRosterScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import com.example.ui.theme.StatusError
import com.example.ui.viewmodel.ClassroomViewModel
import com.example.ui.viewmodel.LmsTab
import com.example.util.PdfReportExporter

@Composable
fun LmsMainScreen(
    viewModel: ClassroomViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var classroomToDelete by remember { mutableStateOf<ClassroomEntity?>(null) }
    var studentToDelete by remember { mutableStateOf<StudentEntity?>(null) }

    // Display messages via Snackbar
    LaunchedEffect(uiState.userNotificationMessage) {
        uiState.userNotificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
            viewModel.clearNotificationMessage()
        }
    }

    var showLinkSheetDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val authManager = viewModel.getAuthManager()
    
    // We observe a simple state variable for the Google Account to trigger recomposition when signed in/out
    var googleAccount by remember { mutableStateOf(authManager.getSignedInAccount()) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                task.getResult(ApiException::class.java)
                googleAccount = authManager.getSignedInAccount()
                viewModel.showToast("Signed in successfully")
            } catch (e: Exception) {
                viewModel.showToast("Sign in failed: ${e.message}")
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(24.dp))
                // Profile Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (googleAccount != null) {
                        Text(
                            text = googleAccount?.displayName ?: "User",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = googleAccount?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { 
                            authManager.signOut()
                            googleAccount = null
                            viewModel.showToast("Signed out")
                        }) {
                            Text("Sign Out")
                        }
                    } else {
                        Text(
                            text = "Not signed in",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { signInLauncher.launch(authManager.getSignInIntent()) }) {
                            Text("Sign in with Google")
                        }
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                NavigationDrawerItem(
                    label = { Text("Add Classroom") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        viewModel.openAddClassroom() 
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Student Roster") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        viewModel.openStudentRoster()
                    },
                    icon = { Icon(Icons.Default.People, contentDescription = "Student Roster") },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                
                NavigationDrawerItem(
                    label = { Text("Teacher Planner & Diary") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        viewModel.openPlanner()
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Planner") },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Classroom Schedule") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        viewModel.openScheduleScreen()
                    },
                    icon = { Icon(Icons.Default.Schedule, contentDescription = "Classroom Schedule") },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Generate Reports") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        viewModel.openGenerateReportDialog()
                    },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Generate Reports") },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Settings (Coming Soon)") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { 
                        Text(if (uiState.isCheckingForUpdates) "Checking for Updates..." else "Check for Updates") 
                    },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        viewModel.checkForUpdates(isManual = true)
                    },
                    icon = { Icon(Icons.Default.SystemUpdate, contentDescription = "Check for Updates") },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.weight(1f))
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Classroom LMS v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                NavigationDrawerItem(
                    label = { Text("Go Back") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back") },
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ClassroomHeader(
                activeClassroom = uiState.activeClassroom,
                classrooms = uiState.classrooms,
                onSelectClassroom = { viewModel.selectClassroom(it.id) },
                onMenuClick = { scope.launch { drawerState.open() } },
                onManageClassrooms = { viewModel.openClassroomManagement() },
                onSyncClick = {
                    val classroom = uiState.activeClassroom
                    if (classroom != null) {
                        if (classroom.linkedSpreadsheetId.isNullOrBlank()) {
                            showLinkSheetDialog = true
                        } else {
                            viewModel.syncGoogleSheet(classroom.id)
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                val tabs = listOf(
                    Triple(LmsTab.DASHBOARD, Icons.Filled.Dashboard to Icons.Outlined.Dashboard, "Overview"),
                    Triple(LmsTab.ANALYTICS, Icons.Filled.AutoGraph to Icons.Outlined.AutoGraph, "Analytics"),
                    Triple(LmsTab.STUDENTS, Icons.Filled.People to Icons.Outlined.People, "Students"),
                    Triple(LmsTab.GRADEBOOK, Icons.AutoMirrored.Filled.Grading to Icons.AutoMirrored.Outlined.Grading, "Grades"),
                    Triple(LmsTab.ASSIGNMENTS, Icons.AutoMirrored.Filled.Assignment to Icons.AutoMirrored.Outlined.Assignment, "Tasks"),
                    Triple(LmsTab.HOMEWORK, Icons.AutoMirrored.Filled.MenuBook to Icons.AutoMirrored.Outlined.MenuBook, "HW"),
                    Triple(LmsTab.ATTENDANCE, Icons.Filled.EventAvailable to Icons.Outlined.EventAvailable, "Attend")
                )

                tabs.forEach { (tab, icons, label) ->
                    val isSelected = uiState.selectedTab == tab
                    val (filledIcon, outlinedIcon) = icons

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(tab) },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) filledIcon else outlinedIcon,
                                contentDescription = label
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_${label.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (uiState.selectedTab) {
                    LmsTab.DASHBOARD -> DashboardScreen(uiState = uiState, viewModel = viewModel)
                    LmsTab.ANALYTICS -> AnalyticsScreen(uiState = uiState, viewModel = viewModel)
                    LmsTab.STUDENTS -> com.example.ui.screens.StudentDirectoryScreen(uiState = uiState, viewModel = viewModel)
                    LmsTab.GRADEBOOK -> GradebookScreen(uiState = uiState, viewModel = viewModel)
                    LmsTab.ASSIGNMENTS -> AssignmentsScreen(uiState = uiState, viewModel = viewModel)
                    LmsTab.HOMEWORK -> HomeworkCheckScreen(uiState = uiState, viewModel = viewModel)
                    LmsTab.ATTENDANCE -> AttendanceScreen(uiState = uiState, viewModel = viewModel)
                }
            }
        }
    }

    // --- MODAL DIALOGS & BOTTOM SHEETS ---

    if (uiState.isScheduleScreenOpen) {
        ClassroomScheduleScreen(
            uiState = uiState,
            viewModel = viewModel,
            onBack = { viewModel.closeScheduleScreen() }
        )
    }

    // Link Google Sheet Dialog
    if (showLinkSheetDialog && uiState.activeClassroom != null) {
        val currentClassroom = uiState.activeClassroom!!
        LinkGoogleSheetDialog(
            classroom = currentClassroom,
            onDismiss = { showLinkSheetDialog = false },
            onLinkAndSync = { spreadsheetId ->
                showLinkSheetDialog = false
                viewModel.linkSpreadsheet(currentClassroom.id, spreadsheetId)
            },
            onUnlink = {
                showLinkSheetDialog = false
                viewModel.linkSpreadsheet(currentClassroom.id, "")
            }
        )
    }

    // 1. Add / Edit Classroom Dialog
    if (uiState.isAddEditClassroomOpen) {
        AddEditClassroomDialog(
            initialClassroom = uiState.editingClassroom,
            onDismiss = { viewModel.closeDialogs() },
            onSave = { classroom ->
                viewModel.saveClassroom(classroom)
            }
        )
    }

    // 2. Add / Edit Student Dialog
    if (uiState.isAddEditStudentOpen) {
        val activeClassId = uiState.activeClassroom?.id ?: 1L
        AddEditStudentDialog(
            classroomId = activeClassId,
            initialStudent = uiState.editingStudent,
            onDismiss = { viewModel.closeDialogs() },
            onSave = { student ->
                viewModel.saveStudent(student)
            }
        )
    }

    // 3. Add / Edit Assignment Dialog
    if (uiState.isAddEditAssignmentOpen) {
        val activeClassId = uiState.activeClassroom?.id ?: 1L
        AddEditAssignmentDialog(
            classroomId = activeClassId,
            initialAssignment = uiState.editingAssignment,
            onDismiss = { viewModel.closeDialogs() },
            onSave = { assignment ->
                viewModel.saveAssignment(assignment)
            }
        )
    }

    // 4. Grading Dialog
    if (uiState.isGradingSubmissionOpen && uiState.gradingSubmission != null && uiState.gradingAssignment != null && uiState.gradingStudent != null) {
        GradeSubmissionDialog(
            submission = uiState.gradingSubmission!!,
            assignment = uiState.gradingAssignment!!,
            student = uiState.gradingStudent!!,
            onDismiss = { viewModel.closeDialogs() },
            onSave = { updatedSub ->
                viewModel.saveSubmission(updatedSub)
            }
        )
    }

    // 5. Student Detail Modal Sheet
    if (uiState.selectedStudentProfile != null) {
        val student = uiState.selectedStudentProfile!!.student
        val trajectory = uiState.analytics.studentTrajectories.find { it.student.id == student.id }
        val studentInterventions = uiState.interventions.filter { it.studentId == student.id }

        val studentDiscipline = uiState.disciplineRecords.filter { it.studentId == student.id }

        StudentDetailDialog(
            summary = uiState.selectedStudentProfile!!,
            trajectory = trajectory,
            interventions = studentInterventions,
            disciplineRecords = studentDiscipline,
            onDismiss = { viewModel.closeStudentProfile() },
            onEditStudent = {
                viewModel.closeStudentProfile()
                viewModel.openEditStudent(student)
            },
            onDeleteStudent = {
                studentToDelete = student
                viewModel.closeStudentProfile()
            },
            onLogIntervention = {
                viewModel.openAddIntervention(student)
            },
            onLogDiscipline = {
                viewModel.openAddDiscipline(student)
            },
            onExportDisciplineIncidentSlip = { record ->
                PdfReportExporter.exportDisciplineIncidentSlip(context, student, record, uiState.activeClassroom?.name ?: "Classroom")
            },
            onExportReport = {
                val report = viewModel.generateStudentPortfolioReportText(student.id)
                viewModel.openExportReport(report, "Student Progress Portfolio - ${student.name}")
            }
        )
    }

    // 6. Teacher Intervention & Action Note Dialog
    if (uiState.isAddInterventionOpen) {
        InterventionDialog(
            intervention = uiState.editingIntervention,
            initialStudent = uiState.interventionStudent,
            students = uiState.students,
            onDismiss = { viewModel.closeDialogs() },
            onSave = { studentId, type, title, notes, date, resolved ->
                viewModel.saveIntervention(studentId, type, title, notes, date, resolved)
            },
            onDelete = { id ->
                viewModel.deleteIntervention(id)
            }
        )
    }

    // 7. Classroom Management Sheet
    if (uiState.isClassroomModalOpen) {
        ClassroomManagementDialog(
            classrooms = uiState.classrooms,
            activeClassroomId = uiState.activeClassroom?.id,
            onSelectClassroom = { viewModel.selectClassroom(it.id) },
            onAddClassroom = {
                viewModel.closeDialogs()
                viewModel.openAddClassroom()
            },
            onEditClassroom = { cls ->
                viewModel.closeDialogs()
                viewModel.openEditClassroom(cls)
            },
            onDeleteClassroom = { cls ->
                classroomToDelete = cls
                viewModel.closeDialogs()
            },
            onOpenStudentRoster = { viewModel.openStudentRoster() },
            onResetSampleData = { viewModel.resetSampleData() },
            onDismiss = { viewModel.closeDialogs() }
        )
    }

    // 8. Report Export Sheet
    if (uiState.isExportReportOpen && uiState.exportReportContent.isNotEmpty()) {
        ReportExportDialog(
            reportText = uiState.exportReportContent,
            reportTitle = uiState.exportReportTitle,
            onDismiss = { viewModel.closeExportReport() }
        )
    }

    // Full student roster (add / edit / remove / re-assign)
    if (uiState.isStudentRosterOpen) {
        val displayList = if (uiState.allStudents.isEmpty()) uiState.students else uiState.allStudents
        StudentRosterScreen(
            students = displayList,
            classrooms = uiState.classrooms,
            activeClassroomId = uiState.activeClassroom?.id,
            onBack = { viewModel.closeStudentRoster() },
            onAddStudent = { viewModel.openAddStudent() },
            onEditStudent = { viewModel.openEditStudent(it) },
            onDeleteStudent = { viewModel.deleteStudent(it.id) },
            onReassignClassroom = { student, newId ->
                viewModel.reassignStudentClassroom(student, newId)
            },
            onLogDiscipline = { student ->
                viewModel.openAddDiscipline(student)
            }
        )
    }

    // Discipline & Behavior Logging Dialog
    if (uiState.isAddEditDisciplineOpen && uiState.disciplineStudent != null) {
        val student = uiState.disciplineStudent!!
        val activeClassId = uiState.activeClassroom?.id ?: 1L
        DisciplineLogDialog(
            student = student,
            initialRecord = uiState.editingDisciplineRecord,
            classroomId = activeClassId,
            onDismiss = { viewModel.closeDisciplineDialog() },
            onSave = { record -> viewModel.saveDisciplineRecord(record) },
            onDelete = { record -> viewModel.deleteDisciplineRecord(record) }
        )
    }

    // Delete Student Confirmation Dialog
    if (studentToDelete != null) {
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("Delete Student") },
            text = { Text("Are you sure you want to remove '${studentToDelete?.name}'? All grades, attendance, and homework records for this student will be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        studentToDelete?.let { viewModel.deleteStudent(it.id) }
                        studentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
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

    // Delete Classroom Confirmation Dialog
    if (classroomToDelete != null) {
        AlertDialog(
            onDismissRequest = { classroomToDelete = null },
            title = { Text("Delete Classroom") },
            text = { Text("Are you sure you want to delete '${classroomToDelete?.name}'? All students, assignments, submissions, attendance, and homework in this classroom will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        classroomToDelete?.let { viewModel.deleteClassroom(it.id) }
                        classroomToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { classroomToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.isPlannerOpen && uiState.activeClassroom != null) {
        PlannerScreen(
            classroomId = uiState.activeClassroom!!.id,
            viewModel = viewModel,
            onBack = { viewModel.closePlanner() }
        )
    }

    // App Update Dialog
    if (uiState.isUpdateDialogOpen && uiState.updateInfo != null) {
        UpdateAlertDialog(
            updateInfo = uiState.updateInfo!!,
            onDismiss = { viewModel.closeUpdateDialog() }
        )
    }

    // Generate Custom Report Dialog Center
    if (uiState.isGenerateReportDialogOpen) {
        GenerateReportDialog(
            classrooms = uiState.classrooms,
            activeClassroomId = uiState.activeClassroom?.id,
            onDismiss = { viewModel.closeGenerateReportDialog() },
            onGeneratePdf = { reportType, targetClassId, dateRange, atRisk, diff, dist, att ->
                viewModel.generateAndShareCustomPdf(
                    context = context,
                    reportType = reportType,
                    targetClassroomId = targetClassId,
                    dateRange = dateRange,
                    includeAtRisk = atRisk,
                    includeDifficulty = diff,
                    includeDistribution = dist,
                    includeAttendance = att
                )
            },
            onPreviewText = { reportType, targetClassId, dateRange, atRisk, diff, dist, att ->
                val reportText = viewModel.generateCustomReportText(
                    reportType = reportType,
                    targetClassroomId = targetClassId,
                    dateRange = dateRange,
                    includeAtRisk = atRisk,
                    includeDifficulty = diff,
                    includeDistribution = dist,
                    includeAttendance = att
                )
                viewModel.closeGenerateReportDialog()
                viewModel.openExportReport(reportText, "${reportType.title} Preview")
            }
        )
    }
}
}
