package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.AssignmentType
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorBg
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningBg
import com.example.ui.viewmodel.AssignmentTimelinePoint
import com.example.ui.viewmodel.CategoryMastery
import com.example.ui.viewmodel.ClassDifficultyArea
import com.example.ui.viewmodel.ClassGradeDistribution
import com.example.ui.viewmodel.DifficultySeverity
import com.example.ui.viewmodel.StudentProgressTrajectory
import com.example.ui.viewmodel.StudentTier
import com.example.ui.viewmodel.TrajectoryTrend

@Composable
fun GradeDistributionBarChart(
    distribution: ClassGradeDistribution,
    totalStudents: Int,
    modifier: Modifier = Modifier
) {
    val grades = listOf(
        Triple("A+/A (80-100)", distribution.aCount, StatusSuccess),
        Triple("B+/B (60-79)", distribution.bCount, Color(0xFF0284C7)),
        Triple("C+/C (40-59)", distribution.cCount, Color(0xFFD97706)),
        Triple("D/E (<40)", distribution.dCount + distribution.fCount, StatusError)
    )
    val maxCount = grades.maxOf { it.second }.coerceAtLeast(1)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            grades.forEach { (label, count, color) ->
                val heightFraction = (count.toFloat() / maxCount).coerceIn(0.10f, 1f)
                val animatedHeight by animateFloatAsState(
                    targetValue = heightFraction,
                    animationSpec = tween(durationMillis = 600),
                    label = "barHeight"
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = "$count",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .height((88 * animatedHeight).dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(if (count > 0) color else MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = label.substringBefore(" "),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CircularProgressGauge(
    percentage: Double,
    title: String,
    modifier: Modifier = Modifier,
    accentColor: Color = StatusSuccess,
    trackColor: Color = Color(0xFFE2E8F0),
    sizeDp: Int = 80,
    strokeWidthDp: Int = 8
) {
    val progress = (percentage / 100.0).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "circularProgress"
    )

    Box(
        modifier = modifier.size(sizeDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp.dp)) {
            val stroke = strokeWidthDp.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(diameter, diameter)

            // Background Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // Active Progress
            drawArc(
                color = accentColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${percentage.toInt()}%",
                fontSize = (sizeDp * 0.22).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    fontSize = (sizeDp * 0.12).sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TieredHorizontalProgressBar(
    done: Int,
    partial: Int,
    missing: Int,
    excused: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val t = total.coerceAtLeast(1).toFloat()
    val doneFrac = done / t
    val partialFrac = partial / t
    val missingFrac = missing / t
    val excusedFrac = excused / t

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(Color(0xFFE2E8F0))
        ) {
            if (doneFrac > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(doneFrac)
                        .background(StatusSuccess)
                )
            }
            if (partialFrac > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(partialFrac)
                        .background(StatusWarning)
                )
            }
            if (missingFrac > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(missingFrac)
                        .background(StatusError)
                )
            }
            if (excusedFrac > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(excusedFrac)
                        .background(StatusInfo)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LegendItem(color = StatusSuccess, label = "Done ($done)")
            LegendItem(color = StatusWarning, label = "Partial ($partial)")
            LegendItem(color = StatusError, label = "Missing ($missing)")
            LegendItem(color = StatusInfo, label = "Excused ($excused)")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentTierDistributionBar(
    supportCount: Int,
    onTrackCount: Int,
    enrichmentCount: Int,
    totalStudents: Int,
    modifier: Modifier = Modifier
) {
    val total = totalStudents.coerceAtLeast(1).toFloat()
    val sFrac = (supportCount / total).coerceIn(0f, 1f)
    val oFrac = (onTrackCount / total).coerceIn(0f, 1f)
    val eFrac = (enrichmentCount / total).coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape)
                .background(Color(0xFFE2E8F0))
        ) {
            if (sFrac > 0.001f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(sFrac)
                        .background(StatusError)
                )
            }
            if (oFrac > 0.001f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(oFrac)
                        .background(Color(0xFF0284C7))
                )
            }
            if (eFrac > 0.001f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(eFrac)
                        .background(StatusSuccess)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LegendItem(color = StatusError, label = "Support: $supportCount (${(sFrac * 100).toInt()}%)")
            LegendItem(color = Color(0xFF0284C7), label = "On Track: $onTrackCount (${(oFrac * 100).toInt()}%)")
            LegendItem(color = StatusSuccess, label = "Enrichment: $enrichmentCount (${(eFrac * 100).toInt()}%)")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// --- PERFORMANCE OVER TIME INTERACTIVE CANVAS CHART ---
@Composable
fun PerformanceTimelineChart(
    points: List<AssignmentTimelinePoint>,
    selectedStudentId: Long? = null,
    selectedStudentName: String? = null,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("No graded assignments recorded yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = Color(0xFFEA580C)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Class Average Score (%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryColor)
            }
            if (selectedStudentId != null && selectedStudentName != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(secondaryColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(selectedStudentName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = secondaryColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Canvas Line Chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(top = 8.dp, bottom = 28.dp, start = 8.dp, end = 8.dp)
        ) {
            val width = size.width
            val height = size.height
            val n = points.size

            val minScore = 0f
            val maxScore = 100f
            val scoreRange = maxScore - minScore
            val innerYPadding = 12.dp.toPx()
            val drawableHeight = (height - 2 * innerYPadding).coerceAtLeast(1f)

            fun getY(score: Float): Float {
                val normalized = ((score - minScore) / scoreRange).coerceIn(0f, 1f)
                return (height - innerYPadding) - (normalized * drawableHeight)
            }

            // Draw horizontal benchmark grid lines (40%, 60%, 80%)
            val benchmarks = listOf(40f, 60f, 80f)
            benchmarks.forEach { bm ->
                val y = getY(bm)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            val stepX = if (n > 1) width / (n - 1) else width / 2

            // 1. Build Class Average Path
            val classPath = Path()
            val fillPath = Path()
            fillPath.moveTo(0f, height)

            val classOffsets = mutableListOf<Offset>()

            points.forEachIndexed { idx, point ->
                val x = if (n > 1) idx * stepX else width / 2
                val y = getY(point.classAveragePercentage.toFloat().coerceIn(minScore, maxScore))
                classOffsets.add(Offset(x, y))

                if (idx == 0) {
                    classPath.moveTo(x, y)
                    fillPath.lineTo(x, y)
                } else {
                    val prev = classOffsets[idx - 1]
                    val cx = (prev.x + x) / 2
                    classPath.cubicTo(cx, prev.y, cx, y, x, y)
                    fillPath.cubicTo(cx, prev.y, cx, y, x, y)
                }
            }

            fillPath.lineTo(width, height)
            fillPath.close()

            // Draw Class Gradient Fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw Class Line
            drawPath(
                path = classPath,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw Class Points
            classOffsets.forEachIndexed { idx, offset ->
                val point = points[idx]
                val dotColor = if (point.isDifficult) StatusError else primaryColor
                drawCircle(
                    color = Color.White,
                    radius = 6.dp.toPx(),
                    center = offset
                )
                drawCircle(
                    color = dotColor,
                    radius = 4.dp.toPx(),
                    center = offset
                )
            }

            // 2. Draw Selected Student Line if active
            if (selectedStudentId != null) {
                val studentOffsets = mutableListOf<Offset>()
                val studentPath = Path()

                points.forEachIndexed { idx, point ->
                    val studentScore = point.studentScores[selectedStudentId]
                    if (studentScore != null) {
                        val x = if (n > 1) idx * stepX else width / 2
                        val y = getY(studentScore.toFloat().coerceIn(minScore, maxScore))
                        studentOffsets.add(Offset(x, y))
                    }
                }

                if (studentOffsets.isNotEmpty()) {
                    studentOffsets.forEachIndexed { idx, offset ->
                        if (idx == 0) studentPath.moveTo(offset.x, offset.y)
                        else {
                            val prev = studentOffsets[idx - 1]
                            val cx = (prev.x + offset.x) / 2
                            studentPath.cubicTo(cx, prev.y, cx, offset.y, offset.x, offset.y)
                        }
                    }
                    drawPath(
                        path = studentPath,
                        color = secondaryColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    studentOffsets.forEach { offset ->
                        drawCircle(color = Color.White, radius = 5.dp.toPx(), center = offset)
                        drawCircle(color = secondaryColor, radius = 3.5.dp.toPx(), center = offset)
                    }
                }
            }
        }

        // Timeline Assignment Labels Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { point ->
                Column(
                    modifier = Modifier.width(64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${point.classAveragePercentage.toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (point.isDifficult) StatusError else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = point.title,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// --- COMMON AREAS OF DIFFICULTY CARD ---
@Composable
fun ClassDifficultyAreaCard(
    area: ClassDifficultyArea,
    modifier: Modifier = Modifier,
    onReviewClick: () -> Unit
) {
    val (badgeBg, badgeText, badgeIcon) = when (area.severity) {
        DifficultySeverity.HIGH_DIFFICULTY -> Triple(StatusErrorBg, StatusError, Icons.Default.Warning)
        DifficultySeverity.MODERATE_CHALLENGE -> Triple(StatusWarningBg, StatusWarning, Icons.Default.Psychology)
        DifficultySeverity.WELL_MASTERED -> Triple(StatusSuccessBg, StatusSuccess, Icons.Default.CheckCircle)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssignmentTypeBadge(type = area.assignment.type)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = area.assignment.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = badgeText,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = area.severity.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Score Metrics Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Class Average", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${area.averagePercentage}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = badgeText)
                }
                Column {
                    Text("Median Score", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${area.medianPercentage}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Column {
                    Text("Below Passing (<70%)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${area.failingCount} students", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (area.failingCount > 0) StatusError else StatusSuccess)
                }
            }

            // Struggling students pill list
            if (area.strugglingStudents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Students Needing Remediation (${area.strugglingStudents.size}):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    area.strugglingStudents.take(4).forEach { student ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StatusErrorBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = student.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = StatusError
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Teacher Action Plan Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Action Recommendation",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = area.teacherActionRecommendation,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onReviewClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Remedial Plan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- STUDENT TRAJECTORY CARD ---
@Composable
fun StudentTrajectoryCard(
    trajectory: StudentProgressTrajectory,
    modifier: Modifier = Modifier,
    onStudentClick: () -> Unit,
    onLogIntervention: () -> Unit
) {
    val student = trajectory.student
    val summary = trajectory.summary
    val trend = trajectory.trend

    val (trendColor, trendIcon) = when (trend) {
        TrajectoryTrend.IMPROVING -> StatusSuccess to Icons.AutoMirrored.Filled.TrendingUp
        TrajectoryTrend.STEADY -> Color(0xFF0284C7) to Icons.AutoMirrored.Filled.TrendingFlat
        TrajectoryTrend.DECLINING -> StatusError to Icons.AutoMirrored.Filled.TrendingDown
    }

    val (tierBg, tierText) = when (trajectory.tier) {
        StudentTier.EXTRA_SUPPORT -> StatusErrorBg to StatusError
        StudentTier.ENRICHMENT_NEEDED -> Color(0xFFF3E8FF) to Color(0xFF7E22CE)
        StudentTier.ON_TRACK -> StatusSuccessBg to StatusSuccess
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onStudentClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Student Title & Trajectory Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudentAvatar(
                        name = student.name,
                        colorHex = student.avatarColorHex,
                        photoPath = student.photoPath,
                        sizeDp = 42
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = student.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${summary.letterGrade} (${summary.percentage}%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(tierBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = trajectory.tier.shortLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tierText
                                )
                            }
                        }
                    }
                }

                // Trend Direction Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(trendColor.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = trendIcon,
                        contentDescription = trend.label,
                        tint = trendColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${if (trajectory.trendDelta >= 0) "+" else ""}${trajectory.trendDelta}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = trendColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-metrics row: HW, Attendance, Interventions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("HW: ${trajectory.homeworkRate.toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Attendance: ${trajectory.attendanceRate.toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Interventions: ${trajectory.interventionsCount}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }

            // Key Focus / Recommendation
            if (trajectory.recommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "• ${trajectory.recommendations.first()}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Actions row: Log Intervention button & View Portfolio button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onLogIntervention() },
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddComment,
                            contentDescription = "Log Intervention",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Log Action Note",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onStudentClick() },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Profile & Report",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// --- CATEGORY MASTERY BREAKDOWN BAR ---
@Composable
fun CategoryMasteryBreakdown(
    masteries: List<CategoryMastery>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        masteries.forEach { cat ->
            val color = when (cat.type) {
                AssignmentType.HOMEWORK -> Color(0xFF2563EB)
                AssignmentType.QUIZ -> Color(0xFF7C3AED)
                AssignmentType.PROJECT -> Color(0xFF059669)
                AssignmentType.EXAM -> Color(0xFFE11D48)
            }
            val frac = (cat.averagePercentage / 100.0).toFloat().coerceIn(0f, 1f)

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${cat.type.displayName} (${cat.gradedCount} graded)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${cat.averagePercentage}% • ${cat.status}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(frac)
                            .background(color)
                    )
                }
            }
        }
    }
}
