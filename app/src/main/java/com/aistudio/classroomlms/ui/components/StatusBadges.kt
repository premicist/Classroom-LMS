package com.aistudio.classroomlms.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.classroomlms.data.entity.AssignmentType
import com.aistudio.classroomlms.data.entity.AttendanceStatus
import com.aistudio.classroomlms.data.entity.HomeworkStatus
import com.aistudio.classroomlms.data.entity.SubmissionStatus
import com.aistudio.classroomlms.ui.theme.statusSuccess
import com.aistudio.classroomlms.ui.theme.statusSuccessContainer
import com.aistudio.classroomlms.ui.theme.statusInfo
import com.aistudio.classroomlms.ui.theme.statusInfoContainer
import com.aistudio.classroomlms.ui.theme.statusWarning
import com.aistudio.classroomlms.ui.theme.statusWarningContainer
import java.util.Locale

@Composable
fun GradeBadge(
    letterGrade: String,
    modifier: Modifier = Modifier,
    percentage: Double? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val (bgColor, textColor) = when (letterGrade.uppercase(Locale.US)) {
        "A+", "A" -> colorScheme.statusSuccessContainer to colorScheme.statusSuccess
        "B+", "B" -> colorScheme.statusInfoContainer to colorScheme.statusInfo
        "C+", "C" -> colorScheme.statusWarningContainer to colorScheme.statusWarning
        "D", "E" -> colorScheme.errorContainer to colorScheme.error
        else -> colorScheme.errorContainer to colorScheme.error
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = letterGrade,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = textColor
            )
            if (percentage != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${"%.1f".format(percentage)}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
fun AttendanceBadge(
    status: AttendanceStatus,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val (bgColor, textColor, icon) = when (status) {
        AttendanceStatus.PRESENT -> Triple(colorScheme.statusSuccessContainer, colorScheme.statusSuccess, Icons.Default.Check)
        AttendanceStatus.ABSENT -> Triple(colorScheme.errorContainer, colorScheme.error, Icons.Default.Close)
        AttendanceStatus.LATE -> Triple(colorScheme.statusWarningContainer, colorScheme.statusWarning, Icons.Default.HourglassEmpty)
        AttendanceStatus.EXCUSED -> Triple(colorScheme.statusInfoContainer, colorScheme.statusInfo, Icons.Default.Remove)
    }

    val boxModifier = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(onClickLabel = "Change attendance status") { onClick() }
            .heightIn(min = 48.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    } else {
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    }

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = status.displayName,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

@Composable
fun AttendanceStatusSelector(
    status: AttendanceStatus,
    onStatusSelected: (AttendanceStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        AttendanceBadge(
            status = status,
            onClick = { isMenuExpanded = true }
        )
        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false }
        ) {
            AttendanceStatus.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        isMenuExpanded = false
                        onStatusSelected(option)
                    },
                    modifier = Modifier.testTag("attendance_status_${option.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
fun HomeworkStatusBadge(
    status: HomeworkStatus,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val (bgColor, textColor) = when (status) {
        HomeworkStatus.DONE -> colorScheme.statusSuccessContainer to colorScheme.statusSuccess
        HomeworkStatus.PARTIAL -> colorScheme.statusWarningContainer to colorScheme.statusWarning
        HomeworkStatus.MISSING -> colorScheme.errorContainer to colorScheme.error
        HomeworkStatus.EXCUSED -> colorScheme.statusInfoContainer to colorScheme.statusInfo
    }

    val boxModifier = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(onClickLabel = "Change homework status") { onClick() }
            .heightIn(min = 48.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    } else {
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    }

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status.displayName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun SubmissionStatusBadge(
    status: SubmissionStatus,
    score: Double? = null,
    maxPoints: Double? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val (bgColor, textColor) = when (status) {
        SubmissionStatus.GRADED -> colorScheme.statusSuccessContainer to colorScheme.statusSuccess
        SubmissionStatus.SUBMITTED -> colorScheme.statusInfoContainer to colorScheme.statusInfo
        SubmissionStatus.LATE -> colorScheme.statusWarningContainer to colorScheme.statusWarning
        SubmissionStatus.MISSING -> colorScheme.errorContainer to colorScheme.error
        SubmissionStatus.PENDING -> colorScheme.surfaceVariant to colorScheme.onSurfaceVariant
    }

    val boxModifier = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClickLabel = "Open grading") { onClick() }
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    } else {
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    }

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (status == SubmissionStatus.GRADED && score != null) {
                    if (maxPoints != null) "${score.toInt()}/${maxPoints.toInt()}" else "${score.toInt()} pts"
                } else status.displayName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun AssignmentTypeBadge(
    type: AssignmentType,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val (bgColor, textColor) = when (type) {
        AssignmentType.HOMEWORK -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
        AssignmentType.QUIZ -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
        AssignmentType.PROJECT -> colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
        AssignmentType.EXAM -> colorScheme.errorContainer to colorScheme.error
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = type.displayName.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun StudentAvatar(
    name: String,
    colorHex: Long,
    modifier: Modifier = Modifier,
    sizeDp: Int = 40,
    photoPath: String? = null
) {
    val initials = name.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(Color(colorHex)),
        contentAlignment = Alignment.Center
    ) {
        if (!photoPath.isNullOrBlank() && File(photoPath).exists()) {
            AsyncImage(
                model = File(photoPath),
                contentDescription = "$name's photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(CircleShape)
            )
        } else {
            Text(
                text = initials.ifEmpty { "S" },
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = (sizeDp * 0.38).sp
            )
        }
    }
}
