package com.example

import com.example.util.NebGradingEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class NebGradingEngineTest {

    @Test
    fun testNebGradeBoundariesAndGpa() {
        // 90.0 - 100.0 -> A+ (4.0)
        assertEquals("A+", NebGradingEngine.calculateLetterGrade(100.0))
        assertEquals(4.0, NebGradingEngine.calculateGpa(100.0), 0.001)
        assertEquals("A+", NebGradingEngine.calculateLetterGrade(90.0))
        assertEquals(4.0, NebGradingEngine.calculateGpa(90.0), 0.001)

        // 80.0 - 89.9 -> A (3.6)
        assertEquals("A", NebGradingEngine.calculateLetterGrade(89.9))
        assertEquals(3.6, NebGradingEngine.calculateGpa(89.9), 0.001)
        assertEquals("A", NebGradingEngine.calculateLetterGrade(80.0))
        assertEquals(3.6, NebGradingEngine.calculateGpa(80.0), 0.001)

        // 70.0 - 79.9 -> B+ (3.2)
        assertEquals("B+", NebGradingEngine.calculateLetterGrade(79.9))
        assertEquals(3.2, NebGradingEngine.calculateGpa(79.9), 0.001)
        assertEquals("B+", NebGradingEngine.calculateLetterGrade(70.0))
        assertEquals(3.2, NebGradingEngine.calculateGpa(70.0), 0.001)

        // 60.0 - 69.9 -> B (2.8)
        assertEquals("B", NebGradingEngine.calculateLetterGrade(69.9))
        assertEquals(2.8, NebGradingEngine.calculateGpa(69.9), 0.001)
        assertEquals("B", NebGradingEngine.calculateLetterGrade(60.0))
        assertEquals(2.8, NebGradingEngine.calculateGpa(60.0), 0.001)

        // 50.0 - 59.9 -> C+ (2.4)
        assertEquals("C+", NebGradingEngine.calculateLetterGrade(59.9))
        assertEquals(2.4, NebGradingEngine.calculateGpa(59.9), 0.001)
        assertEquals("C+", NebGradingEngine.calculateLetterGrade(50.0))
        assertEquals(2.4, NebGradingEngine.calculateGpa(50.0), 0.001)

        // 40.0 - 49.9 -> C (2.0)
        assertEquals("C", NebGradingEngine.calculateLetterGrade(49.9))
        assertEquals(2.0, NebGradingEngine.calculateGpa(49.9), 0.001)
        assertEquals("C", NebGradingEngine.calculateLetterGrade(40.0))
        assertEquals(2.0, NebGradingEngine.calculateGpa(40.0), 0.001)

        // 20.0 - 39.9 -> D (1.6)
        assertEquals("D", NebGradingEngine.calculateLetterGrade(39.9))
        assertEquals(1.6, NebGradingEngine.calculateGpa(39.9), 0.001)
        assertEquals("D", NebGradingEngine.calculateLetterGrade(20.0))
        assertEquals(1.6, NebGradingEngine.calculateGpa(20.0), 0.001)

        // 0.0 - 19.9 -> E (0.8)
        assertEquals("E", NebGradingEngine.calculateLetterGrade(19.9))
        assertEquals(0.8, NebGradingEngine.calculateGpa(19.9), 0.001)
        assertEquals("E", NebGradingEngine.calculateLetterGrade(0.0))
        assertEquals(0.8, NebGradingEngine.calculateGpa(0.0), 0.001)
    }

    @Test
    fun testGradeDescriptions() {
        assertEquals("Outstanding", NebGradingEngine.getGradeDescription("A+"))
        assertEquals("Excellent", NebGradingEngine.getGradeDescription("A"))
        assertEquals("Very Good", NebGradingEngine.getGradeDescription("B+"))
        assertEquals("Good", NebGradingEngine.getGradeDescription("B"))
        assertEquals("Above Average", NebGradingEngine.getGradeDescription("C+"))
        assertEquals("Average", NebGradingEngine.getGradeDescription("C"))
        assertEquals("Below Average", NebGradingEngine.getGradeDescription("D"))
        assertEquals("Insufficient", NebGradingEngine.getGradeDescription("E"))
        assertEquals("N/A", NebGradingEngine.getGradeDescription("INVALID"))
    }
}
