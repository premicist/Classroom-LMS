package com.example.util

import com.example.data.entity.AssignmentEntity
import com.example.data.entity.AssignmentType
import com.example.data.entity.AttendanceRecordEntity
import com.example.data.entity.AttendanceStatus
import com.example.data.entity.ClassroomEntity
import com.example.data.entity.HomeworkRecordEntity
import com.example.data.entity.HomeworkStatus
import com.example.data.entity.InterventionEntity
import com.example.data.entity.StudentEntity
import com.example.data.entity.SubmissionEntity
import com.example.data.entity.SubmissionStatus
import com.example.data.entity.TermWeightConfig
import com.example.ui.viewmodel.AssignmentTimelinePoint
import com.example.ui.viewmodel.AttendanceReport
import com.example.ui.viewmodel.CategoryMastery
import com.example.ui.viewmodel.ClassDifficultyArea
import com.example.ui.viewmodel.ClassGradeDistribution
import com.example.ui.viewmodel.ClassroomAnalytics
import com.example.ui.viewmodel.DifficultySeverity
import com.example.ui.viewmodel.HomeworkCheckDay
import com.example.ui.viewmodel.StudentAttendanceSummary
import com.example.ui.viewmodel.StudentGradeSummary
import com.example.ui.viewmodel.StudentProgressTrajectory
import com.example.ui.viewmodel.StudentTier
import com.example.ui.viewmodel.TrajectoryTrend
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Isolated calculation engine for student grades, analytics, and attendance reports.
 */
object ClassroomAnalyticsEngine {

    fun computeStudentGrades(
        students: List<StudentEntity>,
        assignments: List<AssignmentEntity>,
        submissions: List<SubmissionEntity>,
        homeworks: List<HomeworkRecordEntity>,
        attendance: List<AttendanceRecordEntity>,
        termWeightConfig: TermWeightConfig = TermWeightConfig()
    ): List<StudentGradeSummary> {
        val assignmentMap = assignments.associateBy { it.id }

        return students.map { student ->
            val studentSubs = submissions.filter { it.studentId == student.id }
            val studentHws = homeworks.filter { it.studentId == student.id }
            val studentAtt = attendance.filter { it.studentId == student.id }

            var earnedPoints = 0.0
            var possiblePoints = 0.0
            var gradedCount = 0
            var missingCount = 0
            var pendingCount = 0

            studentSubs.forEach { sub ->
                val assign = assignmentMap[sub.assignmentId]
                if (assign != null) {
                    when (sub.status) {
                        SubmissionStatus.GRADED -> {
                            gradedCount++
                            earnedPoints += (sub.score ?: 0.0)
                            possiblePoints += assign.maxPoints
                        }
                        SubmissionStatus.MISSING -> {
                            missingCount++
                            possiblePoints += assign.maxPoints
                        }
                        SubmissionStatus.PENDING, SubmissionStatus.SUBMITTED, SubmissionStatus.LATE -> {
                            pendingCount++
                        }
                    }
                }
            }

            val percentage = if (termWeightConfig.isEnabled) {
                val hwQuizSubs = studentSubs.filter {
                    val a = assignmentMap[it.assignmentId]
                    a?.type == AssignmentType.HOMEWORK || a?.type == AssignmentType.QUIZ
                }
                val projectSubs = studentSubs.filter {
                    val a = assignmentMap[it.assignmentId]
                    a?.type == AssignmentType.PROJECT
                }
                val examSubs = studentSubs.filter {
                    val a = assignmentMap[it.assignmentId]
                    a?.type == AssignmentType.EXAM
                }

                fun calcCategoryPct(subs: List<SubmissionEntity>): Double? {
                    var pts = 0.0
                    var max = 0.0
                    subs.forEach { s ->
                        val a = assignmentMap[s.assignmentId]
                        if (a != null) {
                            if (s.status == SubmissionStatus.GRADED) {
                                pts += (s.score ?: 0.0)
                                max += a.maxPoints
                            } else if (s.status == SubmissionStatus.MISSING) {
                                max += a.maxPoints
                            }
                        }
                    }
                    return if (max > 0) (pts / max) * 100.0 else null
                }

                val t1Score = calcCategoryPct(hwQuizSubs)
                val t2Score = calcCategoryPct(projectSubs)
                val finalScore = calcCategoryPct(examSubs)

                NebGradingEngine.calculateWeightedCompositeScore(t1Score, t2Score, finalScore, termWeightConfig)
            } else {
                if (possiblePoints > 0) {
                    (earnedPoints / possiblePoints) * 100.0
                } else 100.0
            }

            val letterGrade = NebGradingEngine.calculateLetterGrade(percentage)
            val gpa = NebGradingEngine.calculateGpa(percentage)

            // Homework completion rate
            val hwTotal = studentHws.size
            val hwEarned = studentHws.sumOf { it.status.scoreMultiplier }
            val hwRate = if (hwTotal > 0) (hwEarned / hwTotal) * 100.0 else 100.0

            // Attendance rate
            val attTotal = studentAtt.size
            val attPresent = studentAtt.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.EXCUSED }
            val attRate = if (attTotal > 0) (attPresent.toDouble() / attTotal) * 100.0 else 100.0

            // Risk Assessment
            val riskReasons = mutableListOf<String>()
            if (percentage < 40.0 && possiblePoints > 0) riskReasons.add("Low Cumulative Grade (${"%.1f".format(percentage)}% - Grade ${letterGrade})")
            if (attRate < 85.0 && attTotal >= 3) riskReasons.add("Low Attendance (${"%.1f".format(attRate)}%)")
            if (missingCount >= 2) riskReasons.add("$missingCount Missing Submissions")
            if (hwRate < 70.0 && hwTotal >= 3) riskReasons.add("Low Homework Completion (${"%.1f".format(hwRate)}%)")

            // Enrichment Assessment
            val enrichmentReasons = mutableListOf<String>()
            if (percentage >= 80.0 && gradedCount >= 2) enrichmentReasons.add("High Academic Mastery (${"%.1f".format(percentage)}% - Grade ${letterGrade})")
            if (hwRate >= 90.0 && hwTotal >= 3) enrichmentReasons.add("Exceptional Homework Consistency (${"%.0f".format(hwRate)}%)")
            if (missingCount == 0 && gradedCount >= 3) enrichmentReasons.add("Zero Missing Assignments Streak")

            val tier = when {
                riskReasons.isNotEmpty() -> StudentTier.EXTRA_SUPPORT
                enrichmentReasons.isNotEmpty() && percentage >= 80.0 -> StudentTier.ENRICHMENT_NEEDED
                else -> StudentTier.ON_TRACK
            }

            StudentGradeSummary(
                student = student,
                earnedPoints = earnedPoints,
                possiblePoints = possiblePoints,
                percentage = (percentage * 10).roundToInt() / 10.0,
                letterGrade = letterGrade,
                gpa = (gpa * 100).roundToInt() / 100.0,
                submissions = studentSubs,
                gradedCount = gradedCount,
                missingCount = missingCount,
                pendingCount = pendingCount,
                homeworkCompletionRate = (hwRate * 10).roundToInt() / 10.0,
                attendanceRate = (attRate * 10).roundToInt() / 10.0,
                isAtRisk = tier == StudentTier.EXTRA_SUPPORT,
                isEnrichmentCandidate = tier == StudentTier.ENRICHMENT_NEEDED,
                riskReasons = riskReasons,
                enrichmentReasons = enrichmentReasons,
                tier = tier
            )
        }.sortedByDescending { it.percentage }
    }

    fun computeClassAnalytics(
        summaries: List<StudentGradeSummary>,
        assignments: List<AssignmentEntity>,
        submissions: List<SubmissionEntity>,
        attendance: List<AttendanceRecordEntity>,
        interventions: List<InterventionEntity>
    ): ClassroomAnalytics {
        if (summaries.isEmpty()) return ClassroomAnalytics()

        val avgScore = summaries.map { it.percentage }.average()
        val avgGpa = summaries.map { it.gpa }.average()
        val highest = summaries.maxOfOrNull { it.percentage } ?: 0.0
        val lowest = summaries.minOfOrNull { it.percentage } ?: 0.0

        var a = 0
        var b = 0
        var c = 0
        var d = 0
        var f = 0

        summaries.forEach { s ->
            when (s.letterGrade.firstOrNull()) {
                'A' -> a++
                'B' -> b++
                'C' -> c++
                'D' -> d++
                'E', 'F' -> f++
            }
        }

        val pending = submissions.count { it.status == SubmissionStatus.SUBMITTED || it.status == SubmissionStatus.LATE }
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val todayAtt = attendance.filter { it.date == todayStr }
        val todayRate = if (todayAtt.isNotEmpty()) {
            val presentCount = todayAtt.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.EXCUSED }
            (presentCount.toDouble() / todayAtt.size) * 100.0
        } else 100.0

        val atRisk = summaries.filter { it.tier == StudentTier.EXTRA_SUPPORT }
        val enrichment = summaries.filter { it.tier == StudentTier.ENRICHMENT_NEEDED }
        val onTrack = summaries.filter { it.tier == StudentTier.ON_TRACK }

        // 1. Chronological Timeline Points
        val sortedAssignments = assignments.sortedBy { it.dueDate }
        val timelinePoints = sortedAssignments.mapNotNull { assign ->
            val assignSubs = submissions.filter { it.assignmentId == assign.id && it.status == SubmissionStatus.GRADED }
            if (assignSubs.isEmpty()) null
            else {
                val scoreMap = mutableMapOf<Long, Double>()
                assignSubs.forEach { sub ->
                    val p = if (assign.maxPoints > 0) ((sub.score ?: 0.0) / assign.maxPoints) * 100.0 else 0.0
                    scoreMap[sub.studentId] = (p * 10).roundToInt() / 10.0
                }
                val avg = if (scoreMap.isNotEmpty()) scoreMap.values.average() else 0.0
                val high = scoreMap.values.maxOrNull() ?: 0.0
                val low = scoreMap.values.minOrNull() ?: 0.0
                val isDiff = avg < 75.0 || scoreMap.values.count { it < 70.0 } >= 2

                AssignmentTimelinePoint(
                    assignmentId = assign.id,
                    title = assign.title,
                    dueDate = assign.dueDate,
                    type = assign.type,
                    maxPoints = assign.maxPoints,
                    classAveragePercentage = (avg * 10).roundToInt() / 10.0,
                    highestPercentage = high,
                    lowestPercentage = low,
                    studentScores = scoreMap,
                    isDifficult = isDiff
                )
            }
        }

        // 2. Student Trajectories
        val studentTrajectories = summaries.map { summary ->
            val sId = summary.student.id
            val history = mutableListOf<Pair<String, Double>>()
            timelinePoints.forEach { point ->
                point.studentScores[sId]?.let { score ->
                    history.add(point.title to score)
                }
            }

            val trendDelta: Double
            val trendStatus: TrajectoryTrend
            if (history.size >= 2) {
                val mid = history.size / 2
                val earlier = history.take(mid.coerceAtLeast(1)).map { it.second }.average()
                val recent = history.takeLast(history.size - mid).map { it.second }.average()
                val diff = recent - earlier
                trendDelta = (diff * 10).roundToInt() / 10.0
                trendStatus = when {
                    trendDelta >= 3.0 -> TrajectoryTrend.IMPROVING
                    trendDelta <= -3.0 -> TrajectoryTrend.DECLINING
                    else -> TrajectoryTrend.STEADY
                }
            } else {
                trendDelta = 0.0
                trendStatus = TrajectoryTrend.STEADY
            }

            val studentInters = interventions.count { it.studentId == sId }
            val recommendations = mutableListOf<String>()
            if (summary.tier == StudentTier.EXTRA_SUPPORT) {
                recommendations.add("Schedule 1-on-1 Concept Review")
                recommendations.add("Provide Remedial Practice Packet")
                if (summary.homeworkCompletionRate < 75.0) recommendations.add("Guardian Contact for HW Accountability")
            } else if (summary.tier == StudentTier.ENRICHMENT_NEEDED) {
                recommendations.add("Assign Advanced Honors Extension Project")
                recommendations.add("Designate as Peer Study Group Leader")
            } else {
                recommendations.add("Maintain Current Milestone Checkpoints")
            }

            StudentProgressTrajectory(
                student = summary.student,
                summary = summary,
                trend = trendStatus,
                trendDelta = trendDelta,
                scoreHistory = history,
                homeworkRate = summary.homeworkCompletionRate,
                attendanceRate = summary.attendanceRate,
                tier = summary.tier,
                interventionsCount = studentInters,
                recommendations = recommendations
            )
        }

        val classTrendDelta = if (timelinePoints.size >= 2) {
            val mid = timelinePoints.size / 2
            val earlier = timelinePoints.take(mid).map { it.classAveragePercentage }.average()
            val recent = timelinePoints.takeLast(timelinePoints.size - mid).map { it.classAveragePercentage }.average()
            ((recent - earlier) * 10).roundToInt() / 10.0
        } else 0.0

        val classTrend = when {
            classTrendDelta >= 2.0 -> TrajectoryTrend.IMPROVING
            classTrendDelta <= -2.0 -> TrajectoryTrend.DECLINING
            else -> TrajectoryTrend.STEADY
        }

        // 3. Difficulty Areas
        val studentMap = summaries.map { it.student }.associateBy { it.id }
        val difficultyAreas = assignments.mapNotNull { assign ->
            val assignSubs = submissions.filter { it.assignmentId == assign.id }
            val gradedSubs = assignSubs.filter { it.status == SubmissionStatus.GRADED }
            if (gradedSubs.isEmpty()) null
            else {
                val percentages = gradedSubs.map { sub ->
                    if (assign.maxPoints > 0) ((sub.score ?: 0.0) / assign.maxPoints) * 100.0 else 0.0
                }
                val avg = percentages.average()
                val sortedP = percentages.sorted()
                val median = if (sortedP.isNotEmpty()) sortedP[sortedP.size / 2] else avg
                val failingSubs = gradedSubs.filter { sub ->
                    val p = if (assign.maxPoints > 0) ((sub.score ?: 0.0) / assign.maxPoints) * 100.0 else 0.0
                    p < 70.0
                }
                val missingCount = assignSubs.count { it.status == SubmissionStatus.MISSING }
                val strugglingStudents = failingSubs.mapNotNull { studentMap[it.studentId] }

                val severity = when {
                    avg < 75.0 || failingSubs.size >= 3 -> DifficultySeverity.HIGH_DIFFICULTY
                    avg < 84.0 || failingSubs.isNotEmpty() -> DifficultySeverity.MODERATE_CHALLENGE
                    else -> DifficultySeverity.WELL_MASTERED
                }

                val recommendation = when (severity) {
                    DifficultySeverity.HIGH_DIFFICULTY -> "High difficulty detected (${failingSubs.size} failing). Schedule a full class review lecture and reteach foundational prerequisites before the upcoming unit assessment."
                    DifficultySeverity.MODERATE_CHALLENGE -> "Moderate challenge. Offer optional office hours remediation and share step-by-step worked answer keys."
                    DifficultySeverity.WELL_MASTERED -> "Concepts solidly mastered by the class. Ready to transition to the next curriculum unit."
                }

                ClassDifficultyArea(
                    assignment = assign,
                    averagePercentage = (avg * 10).roundToInt() / 10.0,
                    medianPercentage = (median * 10).roundToInt() / 10.0,
                    failingCount = failingSubs.size,
                    missingCount = missingCount,
                    totalGraded = gradedSubs.size,
                    severity = severity,
                    keyConcept = assign.title,
                    teacherActionRecommendation = recommendation,
                    strugglingStudents = strugglingStudents
                )
            }
        }.sortedWith(compareBy<ClassDifficultyArea> {
            when (it.severity) {
                DifficultySeverity.HIGH_DIFFICULTY -> 0
                DifficultySeverity.MODERATE_CHALLENGE -> 1
                DifficultySeverity.WELL_MASTERED -> 2
            }
        }.thenBy { it.averagePercentage })

        val categoryMasteries = AssignmentType.entries.mapNotNull { type ->
            val typeAssigns = assignments.filter { it.type == type }
            val typeSubs = submissions.filter { sub ->
                typeAssigns.any { it.id == sub.assignmentId } && sub.status == SubmissionStatus.GRADED
            }
            if (typeSubs.isEmpty()) null
            else {
                val percentages = typeSubs.map { sub ->
                    val a = assignments.find { it.id == sub.assignmentId }
                    val maxP = a?.maxPoints ?: 100.0
                    if (maxP > 0) ((sub.score ?: 0.0) / maxP) * 100.0 else 0.0
                }
                val avg = (percentages.average() * 10).roundToInt() / 10.0
                val status = when {
                    avg >= 88.0 -> "Strong Performance"
                    avg >= 78.0 -> "Proficient"
                    else -> "Needs Focus"
                }
                CategoryMastery(
                    type = type,
                    averagePercentage = avg,
                    assignmentCount = typeAssigns.size,
                    gradedCount = typeSubs.size,
                    status = status
                )
            }
        }

        return ClassroomAnalytics(
            averagePercentage = (avgScore * 10).roundToInt() / 10.0,
            averageGpa = (avgGpa * 100).roundToInt() / 100.0,
            highestPercentage = highest,
            lowestPercentage = lowest,
            distribution = ClassGradeDistribution(a, b, c, d, f),
            totalStudents = summaries.size,
            totalAssignments = assignments.size,
            pendingGradingCount = pending,
            todayAttendanceRate = (todayRate * 10).roundToInt() / 10.0,
            atRiskStudents = atRisk,
            enrichmentStudents = enrichment,
            onTrackStudents = onTrack,
            timelinePoints = timelinePoints,
            studentTrajectories = studentTrajectories,
            difficultyAreas = difficultyAreas,
            categoryMasteries = categoryMasteries,
            classTrend = classTrend,
            classTrendDelta = classTrendDelta
        )
    }

    fun computeAttendanceReport(
        classroom: ClassroomEntity?,
        students: List<StudentEntity>,
        records: List<AttendanceRecordEntity>
    ): AttendanceReport {
        val dates = records.map { it.date }.distinct().sortedDescending()
        val totalDays = dates.size
        val totalRecords = records.size

        val totalPres = records.count { it.status == AttendanceStatus.PRESENT }
        val totalAbs = records.count { it.status == AttendanceStatus.ABSENT }
        val totalLate = records.count { it.status == AttendanceStatus.LATE }
        val totalExcus = records.count { it.status == AttendanceStatus.EXCUSED }

        val overallRate = if (totalRecords > 0) {
            ((totalPres + totalExcus).toDouble() / totalRecords) * 100.0
        } else 100.0

        val studentSummaries = students.map { s ->
            val sRecs = records.filter { it.studentId == s.id }
            val pres = sRecs.count { it.status == AttendanceStatus.PRESENT }
            val abs = sRecs.count { it.status == AttendanceStatus.ABSENT }
            val late = sRecs.count { it.status == AttendanceStatus.LATE }
            val excus = sRecs.count { it.status == AttendanceStatus.EXCUSED }
            val rate = if (sRecs.isNotEmpty()) {
                ((pres + excus).toDouble() / sRecs.size) * 100.0
            } else 100.0

            StudentAttendanceSummary(
                student = s,
                presentCount = pres,
                absentCount = abs,
                lateCount = late,
                excusedCount = excus,
                totalDays = sRecs.size,
                attendanceRate = (rate * 10).roundToInt() / 10.0
            )
        }.sortedByDescending { it.attendanceRate }

        val chronic = studentSummaries.filter { it.attendanceRate < 85.0 && it.totalDays >= 3 }
        val perfect = studentSummaries.filter { it.attendanceRate == 100.0 && it.totalDays > 0 }

        val dateRangeText = if (dates.isNotEmpty()) {
            "${dates.last()} to ${dates.first()} ($totalDays recorded days)"
        } else "No records logged yet"

        return AttendanceReport(
            classroomName = classroom?.name ?: "Classroom",
            subject = classroom?.subject ?: "",
            reportDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
            dateRangeText = dateRangeText,
            totalDaysRecorded = totalDays,
            totalRecords = totalRecords,
            overallAttendanceRate = (overallRate * 10).roundToInt() / 10.0,
            totalPresent = totalPres,
            totalAbsent = totalAbs,
            totalLate = totalLate,
            totalExcused = totalExcus,
            studentSummaries = studentSummaries,
            chronicAbsentees = chronic,
            perfectAttendance = perfect
        )
    }

    fun computeHomeworkDays(
        students: List<StudentEntity>,
        records: List<HomeworkRecordEntity>
    ): List<HomeworkCheckDay> {
        val grouped = records.groupBy { "${it.date}___${it.topic}" }
        val studentCount = students.size.coerceAtLeast(1)

        return grouped.map { (key, recs) ->
            val parts = key.split("___")
            val date = parts.getOrNull(0) ?: ""
            val topic = parts.getOrNull(1) ?: "Homework Check"
            val done = recs.count { it.status == HomeworkStatus.DONE }
            val partial = recs.count { it.status == HomeworkStatus.PARTIAL }
            val missing = recs.count { it.status == HomeworkStatus.MISSING }
            val excused = recs.count { it.status == HomeworkStatus.EXCUSED }
            val earnedScore = recs.sumOf { it.status.scoreMultiplier }
            val rate = if (recs.isNotEmpty()) (earnedScore / recs.size) * 100.0 else 0.0

            HomeworkCheckDay(
                date = date,
                topic = topic,
                records = recs,
                totalStudents = studentCount,
                doneCount = done,
                partialCount = partial,
                missingCount = missing,
                excusedCount = excused,
                completionPercentage = (rate * 10).roundToInt() / 10.0
            )
        }.sortedByDescending { it.date }
    }
}
