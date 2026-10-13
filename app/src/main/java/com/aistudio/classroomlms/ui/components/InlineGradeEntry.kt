package com.aistudio.classroomlms.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InlineGradeEntry(
    submissionId: Long,
    assignmentTitle: String,
    maxPoints: Double,
    initialScore: Double?,
    isCompact: Boolean,
    onSave: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var scoreInput by remember(submissionId, initialScore) {
        mutableStateOf(initialScore?.toInt()?.toString() ?: "")
    }

    if (isCompact) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = assignmentTitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Max: ${maxPoints.toInt()} pts",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScoreField(scoreInput, { scoreInput = it }, Modifier.width(76.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    SaveGradeButton(scoreInput, onSave)
                }
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = assignmentTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "Max: ${maxPoints.toInt()}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            ScoreField(scoreInput, { scoreInput = it }, Modifier.width(72.dp))
            Spacer(modifier = Modifier.width(6.dp))
            SaveGradeButton(scoreInput, onSave)
        }
    }
}

@Composable
private fun ScoreField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Score", fontSize = 10.sp) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next
        ),
        modifier = modifier.testTag("inline_grade_score"),
        singleLine = true
    )
}

@Composable
private fun SaveGradeButton(
    scoreInput: String,
    onSave: (Double) -> Unit
) {
    Button(
        onClick = { scoreInput.toDoubleOrNull()?.let(onSave) },
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("inline_grade_save")
    ) {
        Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
