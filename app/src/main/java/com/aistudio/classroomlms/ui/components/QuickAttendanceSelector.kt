package com.aistudio.classroomlms.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.classroomlms.data.entity.AttendanceStatus
import com.aistudio.classroomlms.ui.theme.statusSuccess
import com.aistudio.classroomlms.ui.theme.statusWarning
import com.aistudio.classroomlms.ui.theme.statusInfo
import com.aistudio.classroomlms.ui.theme.errorContainer
import com.aistudio.classroomlms.ui.theme.onStatusSuccess
import com.aistudio.classroomlms.ui.theme.onStatusWarning
import com.aistudio.classroomlms.ui.theme.onStatusInfo
import com.aistudio.classroomlms.ui.theme.onError

/**
 * Enhanced attendance selector optimized for quick attendance taking.
 * Features large touch targets for common statuses (Present/Absent) 
 * and easy access to all status options.
 */
@Composable
fun QuickAttendanceSelector(
    status: AttendanceStatus,
    onStatusSelected: (AttendanceStatus) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Define status colors and icons
    val statusConfig = when (status) {
        AttendanceStatus.PRESENT -> {
            val bg = if (isEnabled) colorScheme.statusSuccessContainer else colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
            val fg = if (isEnabled) colorScheme.statusSuccess else colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            Triple(bg, fg, com.aistudio.classroomlms.R.drawable.ic_check_24)
        }
        AttendanceStatus.ABSENT -> {
            val bg = if (isEnabled) colorScheme.errorContainer else colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
            val fg = if (isEnabled) colorScheme.error else colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            Triple(bg, fg, com.aistudio.classroomlms.R.drawable.ic_close_24)
        }
        AttendanceStatus.LATE -> {
            val bg = if (isEnabled) colorScheme.statusWarningContainer else colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
            val fg = if (isEnabled) colorScheme.statusWarning else colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            Triple(bg, fg, com.aistudio.classroomlms.R.drawable.ic_access_time_24)
        }
        AttendanceStatus.EXCUSED -> {
            val bg = if (isEnabled) colorScheme.statusInfoContainer else colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
            val fg = if (isEnabled) colorScheme.statusInfo else colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            Triple(bg, fg, com.aistudio.classroomlms.R.drawable.ic_remove_24)
        }
    }
    
    val (bgColor, fgColor, iconRes) = statusConfig
    
    // Main selector row with quick actions
        Row(
            modifier = modifier
                .padding(8.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Quick action buttons for Present/Absent (most common)
            Row(
                modifier = Modifier
                    .width(80.dp)
                    .height(40.dp)
                    .background(bgColor)
                    .clip(RoundedCornerShape(20.dp)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                clickable = {
                    if (isEnabled) {
                        // Toggle between Present and Absent for quick access
                        val newStatus = when (status) {
                            AttendanceStatus.PRESENT -> AttendanceStatus.ABSENT
                            AttendanceStatus.ABSENT -> AttendanceStatus.PRESENT
                            else -> AttendanceStatus.PRESENT // Default to Present for others
                        }
                        onStatusSelected(newStatus)
                    }
                },
                enabled = isEnabled
            ) {
                // Present button (left)
                Column(
                    modifier = Modifier
                        .width(36.dp)
                        .height(36.dp)
                        .background(
                            if (status == AttendanceStatus.PRESENT && isEnabled)
                                colorScheme.statusSuccess
                            else
                                colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                        )
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = androidx.compose.material3.Icons.Default.Check,
                        contentDescription = "Present",
                        tint = if (status == AttendanceStatus.PRESENT && isEnabled)
                            colorScheme.onPrimary
                        else
                            colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            
                // Absent button (right)
                Column(
                    modifier = Modifier
                        .width(36.dp)
                        .height(36.dp)
                        .background(
                            if (status == AttendanceStatus.ABSENT && isEnabled)
                                colorScheme.error
                            else
                                colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                        )
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = androidx.compose.material3.Icons.Default.Close,
                        contentDescription = "Absent",
                        tint = if (status == AttendanceStatus.ABSENT && isEnabled)
                            colorScheme.onPrimary
                        else
                            colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(width = 8.dp)
            // Full status indicator (shows current status with tooltip for all options)
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .height(40.dp)
                    .clickable = {
                        if (isEnabled) {
                            // Show full status dialog or fallback to cycling through statuses
                            val statuses = listOf(
                                AttendanceStatus.PRESENT,
                                AttendanceStatus.ABSENT,
                                AttendanceStatus.LATE,
                                AttendanceStatus.EXCUSED
                            )
                            val currentIndex = statuses.indexOf(status)
                            val nextIndex = (currentIndex + 1) % statuses.size
                            onStatusSelected(statuses[nextIndex])
                        }
                    },
                    enabled = isEnabled
            ) {
                // Status icon
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material3.IconVectorResource(
                        id = iconRes,
                        name = "status_icon"
                    ),
                    contentDescription = status.displayName,
                    tint = fgColor,
                    modifier = Modifier.size(20.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 4.dp)
                )
            
                // Status text
                Text(
                    text = status.displayName.substring(0, 1), // First letter only for compact display
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = fgColor,
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                )
            }
        }
}

/**
 * Compact version for use in lists where space is limited
 */
@Composable
fun CompactQuickAttendanceSelector(
    status: AttendanceStatus,
    onStatusSelected: (AttendanceStatus) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Define status colors and icons
    val statusConfig = when (status) {
        AttendanceStatus.PRESENT -> {
            val bg = if (isEnabled) colorScheme.statusSuccessContainer else colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
            val fg = if (isEnabled) colorScheme.statusSuccess else colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            Triple(bg, fg, com.aistudio.classroomlms.R.drawable.ic_check_24)
        }
        AttendanceStatus.ABSENT -> {
            val bg = if (isEnabled) colorScheme.errorContainer else colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
            val fg = if (isEnabled) colorScheme.error else colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            Triple(bg, fg, com.aistudio.classroomlms.R.drawable.ic_close_24)
        }
        AttendanceStatus.LATE -> {
            val bg = if (isEnabled) colorScheme.statusWarningContainer else colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
            val fg = if (isEnabled) colorScheme.statusWarning else colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            Triple(bg, fg, com.aistudio.classroomlms.R.drawable.ic_access_time_24)
        }
        AttendanceStatus.EXCUSED -> {
            val bg = if (isEnabled) colorScheme.statusInfoContainer else colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
            val fg = if (isEnabled) colorScheme.statusInfo else colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            Triple(bg, fg, com.aistudio.classroomlms.R.drawable.ic_remove_24)
        }
    }
    
    val (bgColor, fgColor, iconRes) = statusConfig
    
    Row(
        modifier = modifier
            .padding(4.dp)
            .width(100%)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Student info placeholder
        Column(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Student",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isEnabled) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                maxLines = 1
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        
        // Quick toggle for Present/Absent
        Row(
            modifier = Modifier
                .width(60.dp)
                .height(32.dp)
                .background(bgColor)
                .clip(RoundedCornerShape(16.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            clickable = {
                if (isEnabled) {
                    val newStatus = when (status) {
                        AttendanceStatus.PRESENT -> AttendanceStatus.ABSENT
                        AttendanceStatus.ABSENT -> AttendanceStatus.PRESENT
                        else -> AttendanceStatus.PRESENT
                    }
                    onStatusSelected(newStatus)
                }
            },
            enabled = isEnabled
        ) {
            // Present indicator
            Column(
                modifier = Modifier
                    .width(28.dp)
                    .height(28.dp)
                    .background(
                        if (status == AttendanceStatus.PRESENT && isEnabled)
                            colorScheme.statusSuccess
                        else
                            Color.Transparent
                    )
                    .clip(CircleShape)
                    .border(
                        width = if (status == AttendanceStatus.PRESENT && isEnabled) 0.dp else 1.dp,
                        color = if (isEnabled) colorScheme.outline else colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
            ) {
                if (status == AttendanceStatus.PRESENT && isEnabled) {
                    Icon(
                        imageVector = androidx.compose.material3.Icons.Default.Check,
                        contentDescription = "Present",
                        tint = colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            // Absent indicator
            Column(
                modifier = Modifier
                    .width(28.dp)
                    .height(28.dp)
                    .background(
                        if (status == AttendanceStatus.ABSENT && isEnabled)
                            colorScheme.error
                        else
                            Color.Transparent
                    )
                    .clip(CircleShape)
                    .border(
                        width = if (status == AttendanceStatus.ABSENT && isEnabled) 0.dp else 1.dp,
                        color = if (isEnabled) colorScheme.outline else colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
            ) {
                if (status == AttendanceStatus.ABSENT && isEnabled) {
                    Icon(
                        imageVector = androidx.compose.material3.Icons.Default.Close,
                        contentDescription = "Absent",
                        tint = colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        // Compact status indicator
        Column(
            modifier = Modifier
                .width(32.dp)
                .height(32.dp)
                .clickable = {
                    if (isEnabled) {
                        val statuses = listOf(
                            AttendanceStatus.PRESENT,
                            AttendanceStatus.ABSENT,
                            AttendanceStatus.LATE,
                            AttendanceStatus.EXCUSED
                        )
                        val currentIndex = statuses.indexOf(status)
                        val nextIndex = (currentIndex + 1) % statuses.size
                        onStatusSelected(statuses[nextIndex])
                    }
                },
                enabled = isEnabled
        ) {
            // Status background
            Column(
                modifier = Modifier
                    .width(28.dp)
                    .height(28.dp)
                    .background(bgColor.copy(alpha = 0.2f))
                    .clip(RoundedCornerShape(6.dp))
                    .border(
                        width = 1.dp,
                        color = if (isEnabled) colorScheme.outline else colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
            ) {
                // Status icon
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material3.IconVectorResource(
                        id = iconRes,
                        name = "status_icon"
                    ),
                    contentDescription = status.displayName,
                    tint = fgColor,
                    modifier = Modifier.size(16.dp)
                        .align(Alignment.Center)
                )
            }
        }
    }
}