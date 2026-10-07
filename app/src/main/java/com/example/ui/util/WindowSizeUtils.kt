package com.example.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Categorization of screen width according to Material 3 adaptive design breakpoints.
 * - COMPACT: < 600dp (most smartphones in portrait)
 * - MEDIUM: 600dp .. 839dp (foldables, small tablets, phones in landscape)
 * - EXPANDED: >= 840dp (standard & large tablets, desktop/large displays)
 */
enum class WindowSizeCategory {
    COMPACT,
    MEDIUM,
    EXPANDED;

    val isCompact: Boolean get() = this == COMPACT
    val isTablet: Boolean get() = this != COMPACT
}

/**
 * Pure function to determine [WindowSizeCategory] from a width in dp.
 * Fully testable in unit tests without Android runtime dependencies.
 */
fun computeWindowSizeCategory(screenWidthDp: Int): WindowSizeCategory = when {
    screenWidthDp < 600 -> WindowSizeCategory.COMPACT
    screenWidthDp < 840 -> WindowSizeCategory.MEDIUM
    else -> WindowSizeCategory.EXPANDED
}

/**
 * Returns the adaptive column count based on [WindowSizeCategory].
 */
fun WindowSizeCategory.adaptiveGridColumns(
    compact: Int = 2,
    medium: Int = 3,
    expanded: Int = 4
): Int = when (this) {
    WindowSizeCategory.COMPACT -> compact
    WindowSizeCategory.MEDIUM -> medium
    WindowSizeCategory.EXPANDED -> expanded
}

/**
 * Returns adaptive horizontal padding for content lists and containers.
 */
fun WindowSizeCategory.adaptiveHorizontalPadding(
    compact: Dp = 12.dp,
    medium: Dp = 16.dp,
    expanded: Dp = 24.dp
): Dp = when (this) {
    WindowSizeCategory.COMPACT -> compact
    WindowSizeCategory.MEDIUM -> medium
    WindowSizeCategory.EXPANDED -> expanded
}

/**
 * CompositionLocal allowing UI components to read the current screen's [WindowSizeCategory].
 */
val LocalWindowSizeCategory = compositionLocalOf { WindowSizeCategory.COMPACT }

/**
 * Composable helper to remember the current [WindowSizeCategory] based on device configuration.
 */
@Composable
fun rememberWindowSizeCategory(): WindowSizeCategory {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp) {
        computeWindowSizeCategory(configuration.screenWidthDp)
    }
}

/**
 * Convenience helper to check if the current device is running on a compact smartphone screen.
 */
@Composable
fun isCompactScreen(): Boolean = rememberWindowSizeCategory().isCompact
