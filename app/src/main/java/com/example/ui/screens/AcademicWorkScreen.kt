package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Grading
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Grading
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ClassroomViewModel
import com.example.ui.viewmodel.LmsUiState

import androidx.compose.material3.ExperimentalMaterial3Api

/**
 * Unified Academic Hub containing Grades (Gradebook), Tasks (Assignments), and Homework check.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicWorkScreen(
    uiState: LmsUiState,
    viewModel: ClassroomViewModel,
    initialSubTab: Int = 0,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }

    val subTabs = listOf(
        Triple("Gradebook", Icons.AutoMirrored.Filled.Grading, Icons.AutoMirrored.Outlined.Grading),
        Triple("Exams", Icons.AutoMirrored.Filled.Assignment, Icons.AutoMirrored.Outlined.Assignment),
        Triple("Assignments", Icons.AutoMirrored.Filled.Assignment, Icons.AutoMirrored.Outlined.Assignment),
        Triple("Homework Check", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook)
    )

    Column(modifier = modifier.fillMaxSize()) {
        // Unified Segmented Sub-Tabs
        PrimaryTabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            subTabs.forEachIndexed { index, (label, filledIcon, outlinedIcon) ->
                val isSelected = selectedSubTab == index
                Tab(
                    selected = isSelected,
                    onClick = { selectedSubTab = index },
                    text = {
                        Text(
                            text = label,
                            fontSize = 11.sp, // Reduced font size to fit 4 tabs
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) filledIcon else outlinedIcon,
                            contentDescription = label,
                            modifier = Modifier.size(16.dp) // Slightly smaller icon
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedSubTab) {
                0 -> GradebookScreen(uiState = uiState, viewModel = viewModel)
                1 -> ExamScreen(uiState = uiState, viewModel = viewModel)
                2 -> AssignmentsScreen(uiState = uiState, viewModel = viewModel)
                3 -> HomeworkCheckScreen(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}
