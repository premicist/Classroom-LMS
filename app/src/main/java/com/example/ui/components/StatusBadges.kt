package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
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
import com.example.data.entity.AssignmentType
import com.example.data.entity.AttendanceStatus
import com.example.data.entity.HomeworkStatus
import com.example.data.entity.SubmissionStatus
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorBg
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoBg
import com.example.ui.theme.StatusNeutral
import com.example.ui.theme.StatusNeutralBg
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningBg

@Composable
fun GradeBadge(
    letterGrade: String,
    modifier: Modifier = Modifier,
    percentage: Double? = null
) {
    val (bgColor, textColor) = when (letterGrade.firstOrNull()) {
        'A' -> StatusSuccessBg to StatusSuccess
        'B' -> Color(0xFFE0F2FE) to Color(0xFF0284C7)
        'C' -> StatusWarningBg to StatusWarning
        'D' -> Color(0xFFFFEDD5) to Color(0xFFEA580C)
        else -> StatusErrorBg to StatusError
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
    val (bgColor, textColor, icon) = when (status) {
        AttendanceStatus.PRESENT -> Triple(StatusSuccessBg, StatusSuccess, Icons.Default.Check)
        AttendanceStatus.ABSENT -> Triple(StatusErrorBg, StatusError, Icons.Default.Close)
        AttendanceStatus.TARDY -> Triple(StatusWarningBg, StatusWarning, Icons.Default.HourglassEmpty)
        AttendanceStatus.EXCUSED -> Triple(StatusInfoBg, StatusInfo, Icons.Default.Remove)
    }

    val boxModifier = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable { onClick() }
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
fun HomeworkStatusBadge(
    status: HomeworkStatus,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val (bgColor, textColor) = when (status) {
        HomeworkStatus.DONE -> StatusSuccessBg to StatusSuccess
        HomeworkStatus.PARTIAL -> StatusWarningBg to StatusWarning
        HomeworkStatus.MISSING -> StatusErrorBg to StatusError
        HomeworkStatus.EXCUSED -> StatusInfoBg to StatusInfo
    }

    val boxModifier = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable { onClick() }
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
    val (bgColor, textColor) = when (status) {
        SubmissionStatus.GRADED -> StatusSuccessBg to StatusSuccess
        SubmissionStatus.SUBMITTED -> StatusInfoBg to StatusInfo
        SubmissionStatus.LATE -> StatusWarningBg to StatusWarning
        SubmissionStatus.MISSING -> StatusErrorBg to StatusError
        SubmissionStatus.PENDING -> StatusNeutralBg to StatusNeutral
    }

    val boxModifier = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onClick() }
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
    val (bgColor, textColor) = when (type) {
        AssignmentType.HOMEWORK -> Color(0xFFEFF6FF) to Color(0xFF2563EB)
        AssignmentType.QUIZ -> Color(0xFFF5F3FF) to Color(0xFF7C3AED)
        AssignmentType.PROJECT -> Color(0xFFECFDF5) to Color(0xFF059669)
        AssignmentType.EXAM -> Color(0xFFFFF1F2) to Color(0xFFE11D48)
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
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (sizeDp * 0.38).sp
            )
        }
    }
}
