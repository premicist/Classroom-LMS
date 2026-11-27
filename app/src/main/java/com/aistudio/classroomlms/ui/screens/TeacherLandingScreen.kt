package com.aistudio.classroomlms.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Grading
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.aistudio.classroomlms.data.entity.SchoolEntity
import com.aistudio.classroomlms.ui.theme.statusInfo
import com.aistudio.classroomlms.ui.theme.statusSuccess
import com.aistudio.classroomlms.ui.theme.statusWarning
import com.aistudio.classroomlms.ui.viewmodel.LandingAlert
import com.aistudio.classroomlms.ui.viewmodel.LandingAlertType
import com.aistudio.classroomlms.ui.viewmodel.LandingClassCard
import com.aistudio.classroomlms.ui.viewmodel.LandingViewModel
import com.aistudio.classroomlms.ui.viewmodel.LmsTab
import com.aistudio.classroomlms.ui.viewmodel.UNASSIGNED_SCHOOL_ID
import com.aistudio.classroomlms.util.NepaliDateConverter
import java.util.Calendar
import java.util.Date

@Composable
fun TeacherLandingScreen(
    teacherName: String?,
    onLoginClick: () -> Unit,
    onMenuClick: () -> Unit,
    onOpenAcademicCalendar: () -> Unit,
    onOpenClass: (ClassroomEntity, LmsTab) -> Unit,
    onCreateClass: () -> Unit,
    onOpenPlanner: () -> Unit,
    onOpenTimetable: () -> Unit,
    modifier: Modifier = Modifier,
    selectedSchoolFilterId: Long? = null,
    schools: List<SchoolEntity> = emptyList(),
    onSwitchSchool: (() -> Unit)? = null,
    landingViewModel: LandingViewModel = hiltViewModel()
) {
    val landingState by landingViewModel.uiState.collectAsStateWithLifecycle()

    val visibleClassCards = remember(landingState.classCards, selectedSchoolFilterId) {
        when (selectedSchoolFilterId) {
            null -> landingState.classCards
            UNASSIGNED_SCHOOL_ID -> landingState.classCards.filter { it.classroom.schoolId == null }
            else -> landingState.classCards.filter { it.classroom.schoolId == selectedSchoolFilterId }
        }
    }
    val activeSchoolLabel = when (selectedSchoolFilterId) {
        null -> "All Institutions"
        UNASSIGNED_SCHOOL_ID -> "Not Assigned Classes"
        else -> schools.find { it.id == selectedSchoolFilterId }?.name ?: "Selected School"
    }

    val greeting = remember { NepaliDateConverter.getTimeBasedGreeting() }
    val adDateText = remember { NepaliDateConverter.formatAdDate(Date()) }
    val bsDateText = remember {
        NepaliDateConverter.adToBs(Calendar.getInstance())?.formatDevanagari(includeWeekday = true).orEmpty()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Greeting + Login + Sidebar Menu Row
        item {
            if (schools.isNotEmpty() && onSwitchSchool != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .clickable { onSwitchSchool() }
                ) {
                    Text(
                        text = "🏫 $activeSchoolLabel  ▾",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (!teacherName.isNullOrBlank()) {
                        Text(
                            text = "Hi $teacherName,",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = greeting,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    } else {
                        Text(
                            text = "$greeting!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable { onLoginClick() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = "Sign in",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sign in to sync profile",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open menu",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. Dual AD + Nepali Devanagari BS Calendar Card (tap opens Academic Calendar)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAcademicCalendar() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Calendar",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            if (bsDateText.isNotBlank()) {
                                Text(
                                    text = "$bsDateText (वि.सं.)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "$adDateText (AD)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Calendar →",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 3. Notifications / Actionable Alerts Strip
        if (landingState.alerts.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Notifications & Today's Focus (${landingState.alerts.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(landingState.alerts, key = { it.id }) { alert ->
                            LandingAlertCard(
                                alert = alert,
                                onClick = {
                                    when (alert.type) {
                                        LandingAlertType.HOLIDAY,
                                        LandingAlertType.ACADEMIC_EVENT -> onOpenAcademicCalendar()
                                        LandingAlertType.PENDING_ATTENDANCE -> {
                                            landingState.classCards.find { it.classroom.id == alert.classroomId }
                                                ?.let { onOpenClass(it.classroom, LmsTab.ATTENDANCE) }
                                        }
                                        LandingAlertType.EXAM_UPCOMING -> {
                                            landingState.classCards.find { it.classroom.id == alert.classroomId }
                                                ?.let { onOpenClass(it.classroom, LmsTab.ACADEMIC) }
                                        }
                                        LandingAlertType.LESSON_PLAN_DUE -> onOpenPlanner()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // 4. Quick Utilities Strip (Timetable, Planner, Academic Calendar)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onOpenTimetable,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Timetable", fontSize = 11.sp, maxLines = 1)
                }
                FilledTonalButton(
                    onClick = onOpenPlanner,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Planner", fontSize = 11.sp, maxLines = 1)
                }
                FilledTonalButton(
                    onClick = onOpenAcademicCalendar,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Year Plan", fontSize = 11.sp, maxLines = 1)
                }
            }
        }

        // 5. All Created Classes Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Classes (${visibleClassCards.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "+ New Class",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onCreateClass() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        if (visibleClassCards.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No Classes Found", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Create a classroom or switch school filter to view classes.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = onCreateClass, shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create First Class")
                        }
                    }
                }
            }
        } else {
            items(visibleClassCards, key = { it.classroom.id }) { cardData ->
                LandingClassroomCard(
                    data = cardData,
                    isHolidayToday = landingState.todayHoliday != null,
                    onOpen = { onOpenClass(cardData.classroom, LmsTab.DASHBOARD) },
                    onTakeAttendance = { onOpenClass(cardData.classroom, LmsTab.ATTENDANCE) },
                    onOpenAcademic = { onOpenClass(cardData.classroom, LmsTab.ACADEMIC) }
                )
            }
        }
    }
}

@Composable
private fun LandingAlertCard(
    alert: LandingAlert,
    onClick: () -> Unit
) {
    val accent = when (alert.type) {
        LandingAlertType.HOLIDAY -> MaterialTheme.colorScheme.statusSuccess
        LandingAlertType.PENDING_ATTENDANCE -> MaterialTheme.colorScheme.statusWarning
        LandingAlertType.EXAM_UPCOMING -> MaterialTheme.colorScheme.error
        LandingAlertType.LESSON_PLAN_DUE -> MaterialTheme.colorScheme.primary
        LandingAlertType.ACADEMIC_EVENT -> MaterialTheme.colorScheme.statusInfo
    }

    Card(
        modifier = Modifier
            .width(235.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = alert.title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = alert.subtitle,
                fontSize = 11.sp,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LandingClassroomCard(
    data: LandingClassCard,
    isHolidayToday: Boolean,
    onOpen: () -> Unit,
    onTakeAttendance: () -> Unit,
    onOpenAcademic: () -> Unit
) {
    val cls = data.classroom
    val classColor = Color(cls.colorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            // Top Row: Color Avatar + Class Title + Student Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(classColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cls.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val meta = listOf(cls.subject, cls.gradeLevel, cls.roomNumber)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${data.studentCount}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Today's Schedule inside the Class Card
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (data.todaySchedules.isNotEmpty() && !isHolidayToday) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (data.todaySchedules.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val scheduleText = when {
                            isHolidayToday -> "Holiday today"
                            data.todaySchedules.isEmpty() -> "No period scheduled today"
                            else -> "Today: " + data.todaySchedules.joinToString(", ") {
                                if (it.endTime.isNotBlank()) "${it.startTime}–${it.endTime}" else it.startTime
                            }
                        }
                        Text(
                            text = scheduleText,
                            fontSize = 12.sp,
                            fontWeight = if (data.todaySchedules.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (data.todaySchedules.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (data.isAttendanceMarkedToday) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Attendance marked",
                                tint = MaterialTheme.colorScheme.statusSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                "Marked",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.statusSuccess
                            )
                        }
                    }
                }
            }

            if (!data.activeLessonPlanTitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "📖 Next Plan: ${data.activeLessonPlanTitle}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1-Tap Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTakeAttendance,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Attend", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onOpenAcademic,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Grading, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Academic", fontSize = 11.sp)
                }

                Button(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text("Open", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}
