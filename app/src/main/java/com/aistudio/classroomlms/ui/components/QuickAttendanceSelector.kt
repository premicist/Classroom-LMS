package com.aistudio.classroomlms.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aistudio.classroomlms.data.entity.AttendanceStatus
import com.aistudio.classroomlms.ui.theme.statusInfo
import com.aistudio.classroomlms.ui.theme.statusInfoContainer
import com.aistudio.classroomlms.ui.theme.statusSuccess
import com.aistudio.classroomlms.ui.theme.statusSuccessContainer
import com.aistudio.classroomlms.ui.theme.statusWarning
import com.aistudio.classroomlms.ui.theme.statusWarningContainer

@Composable
fun QuickAttendanceSelector(
    status: AttendanceStatus,
    onStatusSelected: (AttendanceStatus) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    AttendanceStatusActions(
        status = status,
        onStatusSelected = onStatusSelected,
        modifier = modifier.padding(horizontal = 4.dp),
        isEnabled = isEnabled,
        buttonSize = 44.dp
    )
}

@Composable
fun CompactQuickAttendanceSelector(
    status: AttendanceStatus,
    onStatusSelected: (AttendanceStatus) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    AttendanceStatusActions(
        status = status,
        onStatusSelected = onStatusSelected,
        modifier = modifier,
        isEnabled = isEnabled,
        buttonSize = 36.dp
    )
}

@Composable
private fun AttendanceStatusActions(
    status: AttendanceStatus,
    onStatusSelected: (AttendanceStatus) -> Unit,
    modifier: Modifier,
    isEnabled: Boolean,
    buttonSize: Dp
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AttendanceStatusButton(
            status = AttendanceStatus.PRESENT,
            selectedStatus = status,
            enabled = isEnabled,
            buttonSize = buttonSize,
            onClick = { onStatusSelected(AttendanceStatus.PRESENT) }
        )
        AttendanceStatusButton(
            status = AttendanceStatus.ABSENT,
            selectedStatus = status,
            enabled = isEnabled,
            buttonSize = buttonSize,
            onClick = { onStatusSelected(AttendanceStatus.ABSENT) }
        )
        Box {
            Surface(
                onClick = { menuExpanded = true },
                enabled = isEnabled,
                modifier = Modifier.size(buttonSize),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More attendance statuses"
                    )
                }
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                AttendanceStatus.values().forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayName) },
                        leadingIcon = {
                            Icon(
                                imageVector = option.icon(),
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onStatusSelected(option)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceStatusButton(
    status: AttendanceStatus,
    selectedStatus: AttendanceStatus,
    enabled: Boolean,
    buttonSize: Dp,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val (containerColor, contentColor) = if (status == selectedStatus) {
        status.colors()
    } else {
        colorScheme.surfaceVariant to colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(buttonSize),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = status.icon(),
                contentDescription = status.displayName
            )
        }
    }
}

@Composable
private fun AttendanceStatus.colors(): Pair<Color, Color> {
    val colorScheme = MaterialTheme.colorScheme
    return when (this) {
        AttendanceStatus.PRESENT -> colorScheme.statusSuccessContainer to colorScheme.statusSuccess
        AttendanceStatus.ABSENT -> colorScheme.errorContainer to colorScheme.error
        AttendanceStatus.LATE -> colorScheme.statusWarningContainer to colorScheme.statusWarning
        AttendanceStatus.EXCUSED -> colorScheme.statusInfoContainer to colorScheme.statusInfo
    }
}

private fun AttendanceStatus.icon(): ImageVector = when (this) {
    AttendanceStatus.PRESENT -> Icons.Default.Check
    AttendanceStatus.ABSENT -> Icons.Default.Close
    AttendanceStatus.LATE -> Icons.Default.HourglassEmpty
    AttendanceStatus.EXCUSED -> Icons.Default.Remove
}
