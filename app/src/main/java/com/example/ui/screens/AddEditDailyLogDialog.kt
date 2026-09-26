package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.DailyLogEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddEditDailyLogDialog(
    initialLog: DailyLogEntity?,
    onDismiss: () -> Unit,
    onSave: (date: Long, reflectionNotes: String, wasProxyClass: Boolean) -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-DD", Locale.US)

    var dateString by remember {
        mutableStateOf(
            if (initialLog != null) dateFormat.format(Date(initialLog.date))
            else dateFormat.format(Date())
        )
    }
    var reflectionNotes by remember { mutableStateOf(initialLog?.reflectionNotes ?: "") }
    var wasProxyClass by remember { mutableStateOf(initialLog?.wasProxyClass ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialLog != null) "Edit Diary Entry" else "New Daily Diary Entry",
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
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text("Log Date (YYYY-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reflectionNotes,
                    onValueChange = { reflectionNotes = it },
                    label = { Text("Reflection & Teaching Notes *") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Proxy / Substitute Class",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Mark if taking this class on behalf of another teacher",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = wasProxyClass,
                        onCheckedChange = { wasProxyClass = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reflectionNotes.isBlank()) return@Button
                    val parsedDate = try {
                        dateFormat.parse(dateString)?.time ?: System.currentTimeMillis()
                    } catch (_: Exception) {
                        System.currentTimeMillis()
                    }
                    onSave(parsedDate, reflectionNotes.trim(), wasProxyClass)
                },
                enabled = reflectionNotes.isNotBlank()
            ) {
                Text("Save Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
