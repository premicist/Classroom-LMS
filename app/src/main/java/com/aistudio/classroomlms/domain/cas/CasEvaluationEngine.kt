package com.aistudio.classroomlms.domain.cas

/**
 * Nepal CDC (Curriculum Development Centre) Continuous Assessment Scheme (CAS) Evaluator.
 * Standardizes internal marks evaluation (25 or 50 points scale) for Grades 4-10:
 * - Attendance & Punctuality: 4 points (scaled based on presence percentage)
 * - Classroom Participation & Discipline: 4 points
 * - Practical / Project Work & Portfolio: 12 points
 * - Unit / Terminal Tests: 5 points
 */
data class CasEvaluationComponents(
    val attendancePoints: Double,       // Max 4.0
    val participationPoints: Double,    // Max 4.0
    val projectPracticalPoints: Double, // Max 12.0
    val terminalUnitTestPoints: Double  // Max 5.0
) {
    val totalCasScore: Double
        get() = (attendancePoints + participationPoints + projectPracticalPoints + terminalUnitTestPoints)
            .coerceIn(0.0, 25.0)

    val percentage: Double
        get() = (totalCasScore / 25.0) * 100.0
}

object CasEvaluationEngine {

    /**
     * Calculates CDC-compliant attendance points (out of 4.0):
     * >= 90% -> 4.0
     * 80% to 89.9% -> 3.0
     * 70% to 79.9% -> 2.0
     * 60% to 69.9% -> 1.0
     * < 60% -> 0.0
     */
    fun calculateAttendancePoints(attendancePercentage: Double): Double {
        return when {
            attendancePercentage >= 90.0 -> 4.0
            attendancePercentage >= 80.0 -> 3.0
            attendancePercentage >= 70.0 -> 2.0
            attendancePercentage >= 60.0 -> 1.0
            else -> 0.0
        }
    }

    /**
     * Synthesizes 25-point CAS score from raw marks.
     */
    fun synthesizeCasScore(
        attendancePct: Double,
        participationScore: Double, // 0..4
        projectScore: Double,       // 0..12
        testScore: Double           // 0..5
    ): CasEvaluationComponents {
        val attPoints = calculateAttendancePoints(attendancePct)
        val partPoints = participationScore.coerceIn(0.0, 4.0)
        val projPoints = projectScore.coerceIn(0.0, 12.0)
        val testPoints = testScore.coerceIn(0.0, 5.0)

        return CasEvaluationComponents(
            attendancePoints = attPoints,
            participationPoints = partPoints,
            projectPracticalPoints = projPoints,
            terminalUnitTestPoints = testPoints
        )
    }

    /**
     * Computes composite final score combining Theory (75%) and CAS Internal (25%).
     */
    fun calculateCompositeScore(theoryMark75: Double, casMark25: Double): Double {
        return (theoryMark75.coerceIn(0.0, 75.0) + casMark25.coerceIn(0.0, 25.0))
            .coerceIn(0.0, 100.0)
    }
}
