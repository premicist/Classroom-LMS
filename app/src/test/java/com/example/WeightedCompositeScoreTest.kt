package com.example

import com.example.data.entity.TermWeightConfig
import com.example.util.NebGradingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightedCompositeScoreTest {

    @Test
    fun testTermWeightConfigValidation() {
        val validConfig = TermWeightConfig(term1Weight = 25.0, term2Weight = 25.0, finalExamWeight = 50.0, isEnabled = true)
        assertTrue(validConfig.isValid())

        val invalidConfig = TermWeightConfig(term1Weight = 30.0, term2Weight = 30.0, finalExamWeight = 50.0, isEnabled = true)
        assertFalse(invalidConfig.isValid())
    }

    @Test
    fun testWeightedCompositeCalculationWhenEnabled() {
        val config = TermWeightConfig(
            term1Weight = 25.0,   // 25%
            term2Weight = 25.0,   // 25%
            finalExamWeight = 50.0,// 50%
            isEnabled = true
        )

        // Term 1 = 80.0 (A), Term 2 = 80.0 (A), Final = 100.0 (A+)
        // Weighted = (80 * 0.25) + (80 * 0.25) + (100 * 0.50) = 20 + 20 + 50 = 90.0 (A+)
        val score = NebGradingEngine.calculateWeightedCompositeScore(
            term1Score = 80.0,
            term2Score = 80.0,
            finalScore = 100.0,
            config = config
        )

        assertEquals(90.0, score, 0.001)
        assertEquals("A+", NebGradingEngine.calculateLetterGrade(score))
        assertEquals(4.0, NebGradingEngine.calculateGpa(score), 0.001)
    }

    @Test
    fun testUnweightedAverageWhenDisabled() {
        val config = TermWeightConfig(isEnabled = false)

        // Unweighted average of 80, 80, 100 = 86.6666... (Grade A)
        val score = NebGradingEngine.calculateWeightedCompositeScore(
            term1Score = 80.0,
            term2Score = 80.0,
            finalScore = 100.0,
            config = config
        )

        assertEquals(86.666, score, 0.01)
        assertEquals("A", NebGradingEngine.calculateLetterGrade(score))
        assertEquals(3.6, NebGradingEngine.calculateGpa(score), 0.001)
    }
}
