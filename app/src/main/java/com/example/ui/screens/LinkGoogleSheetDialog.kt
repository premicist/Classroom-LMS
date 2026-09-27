package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import com.example.data.entity.ClassroomEntity
import com.example.util.SpreadsheetUtils

@Composable
fun LinkGoogleSheetDialog(
    classroom: ClassroomEntity,
    onDismiss: () -> Unit,
    onLinkAndSync: (String) -> Unit,
    onUnlink: () -> Unit
) {
    var inputUrlOrId by remember { mutableStateOf(classroom.linkedSpreadsheetId ?: "") }
    val isAlreadyLinked = !classroom.linkedSpreadsheetId.isNullOrBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isAlreadyLinked) "Linked Google Sheet" else "Link Google Sheet",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter a Google Sheet ID or paste the full Google Sheet link (URL) from your browser.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = inputUrlOrId,
                    onValueChange = { inputUrlOrId = it },
                    label = { Text("Google Sheet URL or ID") },
                    placeholder = { Text("https://docs.google.com/spreadsheets/d/...") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tip: Make sure the sheet is accessible by your Google account and includes student columns (Name, Student ID, Email).",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanId = SpreadsheetUtils.extractSpreadsheetId(inputUrlOrId)
                    if (cleanId.isNotBlank()) {
                        onLinkAndSync(cleanId)
                    }
                },
                enabled = inputUrlOrId.isNotBlank()
            ) {
                Text("Link & Sync")
            }
        },
        dismissButton = {
            Row {
                if (isAlreadyLinked) {
                    TextButton(onClick = onUnlink) {
                        Text("Unlink Sheet", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
