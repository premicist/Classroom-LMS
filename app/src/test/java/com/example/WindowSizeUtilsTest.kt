package com.example

import androidx.compose.ui.unit.dp
import com.example.ui.util.WindowSizeCategory
import com.example.ui.util.adaptiveGridColumns
import com.example.ui.util.adaptiveHorizontalPadding
import com.example.ui.util.computeWindowSizeCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowSizeUtilsTest {

    @Test
    fun testComputeWindowSizeCategoryCompactPhones() {
        assertEquals(WindowSizeCategory.COMPACT, computeWindowSizeCategory(320))
        assertEquals(WindowSizeCategory.COMPACT, computeWindowSizeCategory(360))
        assertEquals(WindowSizeCategory.COMPACT, computeWindowSizeCategory(411))
        assertEquals(WindowSizeCategory.COMPACT, computeWindowSizeCategory(599))
        assertTrue(computeWindowSizeCategory(360).isCompact)
        assertFalse(computeWindowSizeCategory(360).isTablet)
    }

    @Test
    fun testComputeWindowSizeCategoryMediumFoldablesAndSmallTablets() {
        assertEquals(WindowSizeCategory.MEDIUM, computeWindowSizeCategory(600))
        assertEquals(WindowSizeCategory.MEDIUM, computeWindowSizeCategory(720))
        assertEquals(WindowSizeCategory.MEDIUM, computeWindowSizeCategory(839))
        assertFalse(computeWindowSizeCategory(600).isCompact)
        assertTrue(computeWindowSizeCategory(600).isTablet)
    }

    @Test
    fun testComputeWindowSizeCategoryExpandedTabletsAndDesktops() {
        assertEquals(WindowSizeCategory.EXPANDED, computeWindowSizeCategory(840))
        assertEquals(WindowSizeCategory.EXPANDED, computeWindowSizeCategory(1024))
        assertEquals(WindowSizeCategory.EXPANDED, computeWindowSizeCategory(1280))
        assertFalse(computeWindowSizeCategory(840).isCompact)
        assertTrue(computeWindowSizeCategory(840).isTablet)
    }

    @Test
    fun testAdaptiveGridColumns() {
        assertEquals(2, WindowSizeCategory.COMPACT.adaptiveGridColumns())
        assertEquals(3, WindowSizeCategory.MEDIUM.adaptiveGridColumns())
        assertEquals(4, WindowSizeCategory.EXPANDED.adaptiveGridColumns())
        
        assertEquals(1, WindowSizeCategory.COMPACT.adaptiveGridColumns(compact = 1, medium = 2, expanded = 3))
        assertEquals(2, WindowSizeCategory.MEDIUM.adaptiveGridColumns(compact = 1, medium = 2, expanded = 3))
        assertEquals(3, WindowSizeCategory.EXPANDED.adaptiveGridColumns(compact = 1, medium = 2, expanded = 3))
    }

    @Test
    fun testAdaptiveHorizontalPadding() {
        assertEquals(12.dp, WindowSizeCategory.COMPACT.adaptiveHorizontalPadding())
        assertEquals(16.dp, WindowSizeCategory.MEDIUM.adaptiveHorizontalPadding())
        assertEquals(24.dp, WindowSizeCategory.EXPANDED.adaptiveHorizontalPadding())

        assertEquals(8.dp, WindowSizeCategory.COMPACT.adaptiveHorizontalPadding(compact = 8.dp, medium = 12.dp, expanded = 16.dp))
        assertEquals(12.dp, WindowSizeCategory.MEDIUM.adaptiveHorizontalPadding(compact = 8.dp, medium = 12.dp, expanded = 16.dp))
        assertEquals(16.dp, WindowSizeCategory.EXPANDED.adaptiveHorizontalPadding(compact = 8.dp, medium = 12.dp, expanded = 16.dp))
    }

    @Test
    fun testBoundaryValues() {
        assertEquals(WindowSizeCategory.COMPACT, computeWindowSizeCategory(0))
        assertEquals(WindowSizeCategory.COMPACT, computeWindowSizeCategory(599))
        assertEquals(WindowSizeCategory.MEDIUM, computeWindowSizeCategory(600))
        assertEquals(WindowSizeCategory.MEDIUM, computeWindowSizeCategory(839))
        assertEquals(WindowSizeCategory.EXPANDED, computeWindowSizeCategory(840))
        assertEquals(WindowSizeCategory.EXPANDED, computeWindowSizeCategory(2560))
    }
}
