package com.example.util

import com.example.ui.viewmodel.StudentGradeSummary
import java.util.Locale

object AiInsightUtils {

    data class AiStudentInsight(
        val riskScore: String,
        val riskColor: Long,
        val recommendations: List<String>,
        val englishMessage: String,
        val nepaliMessage: String
    )

    fun generateAiInsightForStudent(summary: StudentGradeSummary): AiStudentInsight {
        val student = summary.student
        val percentage = summary.percentage
        val missingCount = summary.missingCount
        val attendance = summary.attendanceRate

        val riskScore = when {
            percentage < 60.0 || missingCount >= 3 || attendance < 80.0 -> "High Attention Needed"
            percentage < 75.0 || missingCount >= 1 || attendance < 90.0 -> "Moderate Risk"
            else -> "Low Risk"
        }

        val riskColor = when (riskScore) {
            "High Attention Needed" -> 0xFFEF4444L
            "Moderate Risk" -> 0xFFF59E0BL
            else -> 0xFF10B981L
        }

        val recommendations = mutableListOf<String>()
        if (missingCount > 0) {
            recommendations.add("Student has $missingCount missing submission(s). Prompt for makeup assignment submission.")
        }
        if (attendance < 85.0) {
            recommendations.add("Attendance is low (${String.format(Locale.US, "%.1f", attendance)}%). Check in with guardian regarding absenteeism.")
        }
        if (percentage >= 90.0) {
            recommendations.add("High academic standing (A/A+). Recommend enrichment projects or peer mentoring.")
        } else if (percentage < 70.0) {
            recommendations.add("Score is below standard (${String.format(Locale.US, "%.1f", percentage)}%). Provide targeted concept review on recent topics.")
        } else {
            recommendations.add("Maintaining steady progress. Encourage consistent participation.")
        }

        val englishMsg = "Dear Parent/Guardian,\n\nWe wanted to share an update regarding ${student.name}'s progress in class. " +
                "Their current academic standing is ${summary.letterGrade} (${String.format(Locale.US, "%.1f", percentage)}%) with ${String.format(Locale.US, "%.1f", attendance)}% attendance. " +
                (if (missingCount > 0) "They have $missingCount pending assignment(s) to complete. " else "") +
                "Thank you for your continued support.\n\nBest regards,\nClass Teacher"

        val nepaliMsg = "आदरणीय अभिभावक,\n\nनमस्ते! कक्षामा ${student.name} को प्रगति बारे जानकारी गराउन चाहन्छौँ। " +
                "हाल उनको प्राप्तांक ${summary.letterGrade} (${String.format(Locale.US, "%.1f", percentage)}%) र हाजिरी ${String.format(Locale.US, "%.1f", attendance)}% रहेको छ। " +
                (if (missingCount > 0) "उनले बुझाउन बाँकी $missingCount गृहकार्य/कार्यहरू छन्। " else "") +
                "तपाईँको निरन्तर सहयोगको लागि धन्यवाद।\n\nभवदीय,\nकक्षा शिक्षक"

        return AiStudentInsight(riskScore, riskColor, recommendations, englishMsg, nepaliMsg)
    }

    fun generateRemedialPlanForDifficulty(topic: String): String {
        return "REMEDIAL LESSON PLAN: Focus on '$topic'\n\n" +
                "1. Concept Review (15 mins): Re-explain core principles with visual diagrams.\n" +
                "2. Guided Practice (20 mins): Work through 2 sample problems step-by-step with the class.\n" +
                "3. Formative Assessment (10 mins): Quick 3-question exit ticket to verify understanding.\n" +
                "4. Differentiated Support: Pair struggling students with peer mentors."
    }
}
