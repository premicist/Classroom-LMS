package com.example.util

import com.example.data.entity.TermWeightConfig

object NebGradingEngine {

    fun calculateLetterGrade(percentage: Double): String {
        return when {
            percentage >= 90.0 -> "A+"
            percentage >= 80.0 -> "A"
            percentage >= 70.0 -> "B+"
            percentage >= 60.0 -> "B"
            percentage >= 50.0 -> "C+"
            percentage >= 40.0 -> "C"
            percentage >= 20.0 -> "D"
            else -> "E"
        }
    }

    fun calculateGpa(percentage: Double): Double {
        return when {
            percentage >= 90.0 -> 4.0
            percentage >= 80.0 -> 3.6
            percentage >= 70.0 -> 3.2
            percentage >= 60.0 -> 2.8
            percentage >= 50.0 -> 2.4
            percentage >= 40.0 -> 2.0
            percentage >= 20.0 -> 1.6
            else -> 0.8
        }
    }

    fun getGradeDescription(letterGrade: String): String {
        return when (letterGrade.uppercase()) {
            "A+" -> "Outstanding"
            "A" -> "Excellent"
            "B+" -> "Very Good"
            "B" -> "Good"
            "C+" -> "Above Average"
            "C" -> "Average"
            "D" -> "Below Average"
            "E" -> "Insufficient"
            else -> "N/A"
        }
    }

    fun calculateWeightedCompositeScore(
        term1Score: Double?,
        term2Score: Double?,
        finalScore: Double?,
        config: TermWeightConfig
    ): Double {
        if (!config.isEnabled) {
            val scores = listOfNotNull(term1Score, term2Score, finalScore)
            return if (scores.isNotEmpty()) scores.average() else 0.0
        }
        var totalWeighted = 0.0
        var totalWeightApplied = 0.0

        term1Score?.let {
            totalWeighted += it * (config.term1Weight / 100.0)
            totalWeightApplied += config.term1Weight
        }
        term2Score?.let {
            totalWeighted += it * (config.term2Weight / 100.0)
            totalWeightApplied += config.term2Weight
        }
        finalScore?.let {
            totalWeighted += it * (config.finalExamWeight / 100.0)
            totalWeightApplied += config.finalExamWeight
        }

        return if (totalWeightApplied > 0) (totalWeighted / totalWeightApplied) * 100.0 else 0.0
    }
}
