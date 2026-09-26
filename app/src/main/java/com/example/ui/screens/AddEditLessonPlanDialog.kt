package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.LessonPlanEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLessonPlanDialog(
    initialPlan: LessonPlanEntity?,
    onDismiss: () -> Unit,
    onSave: (unitTitle: String, description: String, targetDate: Long, status: String) -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    var unitTitle by remember { mutableStateOf(initialPlan?.unitTitle ?: "") }
    var description by remember { mutableStateOf(initialPlan?.description ?: "") }
    var dateString by remember {
        mutableStateOf(
            if (initialPlan != null) dateFormat.format(Date(initialPlan.targetDate))
            else dateFormat.format(Date())
        )
    }
    var status by remember { mutableStateOf(initialPlan?.status ?: "PLANNED") }
    var isStatusExpanded by remember { mutableStateOf(false) }

    val statusOptions = listOf("PLANNED", "IN_PROGRESS", "COMPLETED")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialPlan != null) "Edit Lesson Plan" else "New Lesson Plan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = unitTitle,
                    onValueChange = { unitTitle = it },
                    label = { Text("Unit / Topic Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Objectives & Detailed Notes *") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text("Target Date (YYYY-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = isStatusExpanded,
                    onExpandedChange = { isStatusExpanded = it }
                ) {
                    OutlinedTextField(
                        value = status.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStatusExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = isStatusExpanded,
                        onDismissRequest = { isStatusExpanded = false }
                    ) {
                        statusOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.replace("_", " ")) },
                                onClick = {
                                    status = option
                                    isStatusExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (unitTitle.isBlank() || description.isBlank()) return@Button
                    val parsedDate = try {
                        dateFormat.parse(dateString)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }
                    onSave(unitTitle.trim(), description.trim(), parsedDate, status)
                },
                enabled = unitTitle.isNotBlank() && description.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
