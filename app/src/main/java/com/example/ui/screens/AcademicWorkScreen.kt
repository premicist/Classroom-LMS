package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.util.LocalWindowSizeCategory
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
    val isCompact = LocalWindowSizeCategory.current.isCompact
    var selectedSubTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }

    val subTabs = listOf(
        Triple("Gradebook", Icons.AutoMirrored.Filled.Grading, Icons.AutoMirrored.Outlined.Grading),
        Triple("Exams", Icons.AutoMirrored.Filled.Assignment, Icons.AutoMirrored.Outlined.Assignment),
        Triple("Assignments", Icons.AutoMirrored.Filled.Assignment, Icons.AutoMirrored.Outlined.Assignment),
        Triple("Homework Check", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook)
    )

    Column(modifier = modifier.fillMaxSize()) {
        // Unified Segmented Sub-Tabs
        if (isCompact) {
            ScrollableTabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                subTabs.forEachIndexed { index, (label, filledIcon, outlinedIcon) ->
                    val isSelected = selectedSubTab == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedSubTab = index },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) filledIcon else outlinedIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    )
                }
            }
        } else {
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) filledIcon else outlinedIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    )
                }
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

