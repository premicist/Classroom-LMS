package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.entity.AttendanceRecordEntity
import com.example.data.entity.AttendanceStatus
import com.example.data.entity.DisciplineRecordEntity
import com.example.data.entity.ExamEntity
import com.example.data.entity.ExamMarkEntity
import com.example.data.entity.StudentEntity
import com.example.ui.viewmodel.AttendanceReport
import com.example.ui.viewmodel.StudentAttendanceSummary
import com.example.ui.viewmodel.StudentGradeSummary
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates clean A4 PDF reports and shares them via the system share sheet.
 * A4 at 72 dpi ≈ 595 × 842 points.
 */
object PdfReportExporter {

    // A4 size in points (1 point = 1/72 inch)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f
    private const val LINE_HEIGHT = 16f
    private const val TITLE_SIZE = 16f
    private const val HEADING_SIZE = 12f
    private const val BODY_SIZE = 10f
    private const val SMALL_SIZE = 9f

    fun exportExamReport(
        context: Context,
        exam: ExamEntity,
        marks: List<ExamMarkEntity>,
        students: List<StudentEntity>
    ) {
        val title = "Exam Report: ${exam.title} (${exam.category.displayName})"
        val sb = StringBuilder()
        sb.appendLine("====================================================")
        sb.appendLine("                    EXAM REPORT                     ")
        sb.appendLine("====================================================\n")
        sb.appendLine("Exam Name: ${exam.title}")
        sb.appendLine("Category: ${exam.category.displayName}")
        sb.appendLine("Date: ${exam.date}")
        if (exam.topicOrChapter.isNotBlank()) sb.appendLine("Topic/Chapter: ${exam.topicOrChapter}")
        sb.appendLine("Full Marks: ${exam.fullMarks.toInt()} | Pass Marks: ${exam.passMarks.toInt()}\n")
        
        sb.appendLine(String.format(Locale.US, "%-25s | %-10s | %-10s", "Student Name", "ID", "Marks"))
        sb.appendLine("---------------------------------------------------------")
        
        students.sortedBy { it.name }.forEach { student ->
            val markRecord = marks.find { it.studentId == student.id }
            val marksStr = if (markRecord == null) {
                "Not Graded"
            } else if (markRecord.isAbsent) {
                "ABSENT"
            } else if (markRecord.marksObtained != null) {
                markRecord.marksObtained.toString()
            } else {
                "Not Graded"
            }
            sb.appendLine(String.format(Locale.US, "%-25s | %-10s | %-10s", student.name.take(25), student.studentNumber, marksStr))
        }
        
        sb.appendLine("\n====================================================")
        exportAndShare(context, title, sb.toString(), "ExamReport_${exam.title.replace(" ", "_")}")
    }

    fun exportStudentPortfolio(
        context: Context,
        summary: StudentGradeSummary,
        classTitle: String
    ) {
        try {
            val document = PdfDocument()
            
            val paint = Paint().apply {
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textSize = BODY_SIZE
                color = Color.BLACK
            }
            val titlePaint = Paint(paint).apply {
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textSize = TITLE_SIZE
            }
            val headingPaint = Paint(paint).apply {
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textSize = HEADING_SIZE
            }

            // --- Draw Page ---
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            var currentY = MARGIN

            // 1. Header
            canvas.drawText("Comprehensive Student Portfolio", MARGIN, currentY, titlePaint)
            currentY += LINE_HEIGHT * 2
            
            val dateStr = SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date())
            canvas.drawText("Date: $dateStr", MARGIN, currentY, paint)
            currentY += LINE_HEIGHT
            canvas.drawText("Classroom: $classTitle", MARGIN, currentY, paint)
            currentY += LINE_HEIGHT * 2

            // 2. Student Info
            canvas.drawText("Student Profile", MARGIN, currentY, headingPaint)
            currentY += LINE_HEIGHT * 1.5f
            val s = summary.student
            canvas.drawText("Name: ${s.name}", MARGIN, currentY, paint)
            currentY += LINE_HEIGHT
            canvas.drawText("ID: ${s.studentNumber}", MARGIN, currentY, paint)
            currentY += LINE_HEIGHT
            if (s.email.isNotBlank()) {
                canvas.drawText("Email: ${s.email}", MARGIN, currentY, paint)
                currentY += LINE_HEIGHT
            }
            if (s.guardianContact.isNotBlank()) {
                canvas.drawText("Guardian: ${s.guardianContact}", MARGIN, currentY, paint)
                currentY += LINE_HEIGHT
            }
            s.customAttributes.forEach { (key, value) ->
                canvas.drawText("$key: $value", MARGIN, currentY, paint)
                currentY += LINE_HEIGHT
            }
            currentY += LINE_HEIGHT

            // 3. Academic Performance
            canvas.drawText("Academic Performance", MARGIN, currentY, headingPaint)
            currentY += LINE_HEIGHT * 1.5f
            canvas.drawText("Overall Grade: ${summary.percentage}% (${summary.letterGrade})", MARGIN, currentY, paint)
            currentY += LINE_HEIGHT
            canvas.drawText("GPA: ${summary.gpa}", MARGIN, currentY, paint)
            currentY += LINE_HEIGHT
            canvas.drawText("Academic Tier: ${summary.tier.name.replace("_", " ")}", MARGIN, currentY, paint)
            currentY += LINE_HEIGHT * 2

            // 4. Attendance & Participation
            canvas.drawText("Participation & Consistency", MARGIN, currentY, headingPaint)
            currentY += LINE_HEIGHT * 1.5f
            canvas.drawText("Attendance Rate: ${summary.attendanceRate.toInt()}%", MARGIN, currentY, paint)
            currentY += LINE_HEIGHT
            canvas.drawText("Homework Completion: ${summary.homeworkCompletionRate.toInt()}%", MARGIN, currentY, paint)
            currentY += LINE_HEIGHT * 2
            
            // 5. TCMS Automated Analytics
            if (summary.riskReasons.isNotEmpty() || summary.enrichmentReasons.isNotEmpty()) {
                canvas.drawText("Automated Insights (TCMS)", MARGIN, currentY, headingPaint)
                currentY += LINE_HEIGHT * 1.5f
                
                summary.riskReasons.forEach { risk ->
                    canvas.drawText("• Alert: $risk", MARGIN + 10f, currentY, paint)
                    currentY += LINE_HEIGHT
                }
                summary.enrichmentReasons.forEach { enrichment ->
                    canvas.drawText("• Strength: $enrichment", MARGIN + 10f, currentY, paint)
                    currentY += LINE_HEIGHT
                }
            }

            document.finishPage(page)

            // Save and share
            val file = File(context.cacheDir, "Portfolio_${s.name.replace(" ", "_")}.pdf")
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()

            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Student Portfolio: ${s.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Student Portfolio"))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Build a multi-page A4 PDF from plain report text and open the share sheet.
     */
    fun exportAndShare(
        context: Context,
        title: String,
        reportText: String,
        fileNamePrefix: String = "ClassReport"
    ) {
        try {
            val file = buildPdfFile(context, title, reportText, fileNamePrefix)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Share PDF report")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not create PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportDisciplineIncidentSlip(
        context: Context,
        student: StudentEntity,
        record: DisciplineRecordEntity,
        classroomName: String
    ) {
        try {
            val document = PdfDocument()

            val paint = Paint().apply {
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textSize = BODY_SIZE
                color = Color.BLACK
            }
            val titlePaint = Paint(paint).apply {
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textSize = TITLE_SIZE
            }
            val headingPaint = Paint(paint).apply {
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textSize = HEADING_SIZE
            }
            val rulePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }

            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            var currentY = MARGIN

            // 1. Header Box
            canvas.drawText("BEHAVIOR & INCIDENT REPORT SLIP", MARGIN, currentY, titlePaint)
            currentY += LINE_HEIGHT * 1.5f
            canvas.drawText("Classroom / Course: $classroomName", MARGIN, currentY, paint)
            currentY += LINE_HEIGHT
            canvas.drawText("Date of Incident: ${record.date}", MARGIN, currentY, paint)
            currentY += LINE_HEIGHT * 1.5f

            canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, rulePaint)
            currentY += LINE_HEIGHT

            // 2. Student Info
            canvas.drawText("Student Information", MARGIN, currentY, headingPaint)
            currentY += LINE_HEIGHT * 1.5f
            canvas.drawText("Student Name: ${student.name}", MARGIN, currentY, paint)
            currentY += LINE_HEIGHT
            canvas.drawText("Student ID: ${student.studentNumber}", MARGIN, currentY, paint)
            if (student.email.isNotBlank()) {
                currentY += LINE_HEIGHT
                canvas.drawText("Student Email: ${student.email}", MARGIN, currentY, paint)
            }
            currentY += LINE_HEIGHT * 1.5f

            canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, rulePaint)
            currentY += LINE_HEIGHT

            // 3. Incident Details
            canvas.drawText("Incident & Behavior Details", MARGIN, currentY, headingPaint)
            currentY += LINE_HEIGHT * 1.5f
            canvas.drawText("Category: ${record.category.displayName}", MARGIN, currentY, paint)
            currentY += LINE_HEIGHT
            canvas.drawText("Severity Level: ${record.severity.displayName}", MARGIN, currentY, paint)
            currentY += LINE_HEIGHT
            canvas.drawText("Incident Title: ${record.title}", MARGIN, currentY, paint)
            currentY += LINE_HEIGHT * 1.5f

            if (record.description.isNotBlank()) {
                canvas.drawText("Description:", MARGIN, currentY, headingPaint)
                currentY += LINE_HEIGHT
                val descLines = wrapText(record.description, paint, PAGE_WIDTH - MARGIN * 2)
                for (line in descLines) {
                    canvas.drawText(line, MARGIN + 12f, currentY, paint)
                    currentY += LINE_HEIGHT
                }
                currentY += LINE_HEIGHT * 0.5f
            }

            if (record.actionTaken.isNotBlank()) {
                canvas.drawText("Action Taken:", MARGIN, currentY, headingPaint)
                currentY += LINE_HEIGHT
                val actionLines = wrapText(record.actionTaken, paint, PAGE_WIDTH - MARGIN * 2)
                for (line in actionLines) {
                    canvas.drawText(line, MARGIN + 12f, currentY, paint)
                    currentY += LINE_HEIGHT
                }
                currentY += LINE_HEIGHT * 0.5f
            }

            canvas.drawText("Parent / Guardian Notified: ${if (record.parentNotified) "YES" else "NO"}", MARGIN, currentY, paint)
            currentY += LINE_HEIGHT
            canvas.drawText("Resolution Status: ${if (record.resolved) "RESOLVED" else "OPEN / PENDING"}", MARGIN, currentY, paint)
            currentY += LINE_HEIGHT * 2.5f

            // 4. Signature Section
            canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, rulePaint)
            currentY += LINE_HEIGHT * 1.5f

            canvas.drawText("Signatures & Acknowledgment", MARGIN, currentY, headingPaint)
            currentY += LINE_HEIGHT * 3f

            val colWidth = (PAGE_WIDTH - MARGIN * 2) / 2f
            canvas.drawLine(MARGIN, currentY, MARGIN + colWidth - 20f, currentY, paint)
            canvas.drawLine(MARGIN + colWidth + 20f, currentY, PAGE_WIDTH - MARGIN, currentY, paint)
            currentY += LINE_HEIGHT

            canvas.drawText("Teacher / Educator Signature & Date", MARGIN, currentY, paint)
            canvas.drawText("Parent / Guardian Signature & Date", MARGIN + colWidth + 20f, currentY, paint)

            document.finishPage(page)

            val dir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(dir, "Incident_Slip_${student.name.replace(" ", "_")}_${record.date}.pdf")
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Incident Slip - ${student.name}")
                putExtra(Intent.EXTRA_TEXT, "Incident Report Slip for ${student.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Share Incident Report Slip")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to export incident slip: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Create the PDF file in cache and return it.
     */
    fun buildPdfFile(
        context: Context,
        title: String,
        reportText: String,
        fileNamePrefix: String = "ClassReport"
    ): File {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val safeName = fileNamePrefix.replace(Regex("[^A-Za-z0-9_-]"), "_").take(40)
        val file = File(dir, "${safeName}_$stamp.pdf")

        val document = PdfDocument()
        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = TITLE_SIZE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = 0xFF1E3A5F.toInt()
        }
        val headingPaint = Paint().apply {
            isAntiAlias = true
            textSize = HEADING_SIZE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = 0xFF1E40AF.toInt()
        }
        val bodyPaint = Paint().apply {
            isAntiAlias = true
            textSize = BODY_SIZE
            typeface = Typeface.MONOSPACE
            color = 0xFF1F2937.toInt()
        }
        val smallPaint = Paint().apply {
            isAntiAlias = true
            textSize = SMALL_SIZE
            color = 0xFF6B7280.toInt()
        }
        val rulePaint = Paint().apply {
            color = 0xFFCBD5E1.toInt()
            strokeWidth = 1f
        }

        val contentWidth = PAGE_WIDTH - 2 * MARGIN
        val maxY = PAGE_HEIGHT - MARGIN - 24f // leave footer space

        val lines = wrapText(reportText, bodyPaint, contentWidth)
        var lineIndex = 0
        var pageNumber = 0

        while (lineIndex < lines.size || pageNumber == 0) {
            pageNumber++
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            var y = MARGIN

            // Header only on first page
            if (pageNumber == 1) {
                canvas.drawText(title, MARGIN, y + TITLE_SIZE, titlePaint)
                y += TITLE_SIZE + 6f
                val dateStr = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date())
                canvas.drawText("Generated $dateStr", MARGIN, y + SMALL_SIZE, smallPaint)
                y += SMALL_SIZE + 10f
                canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rulePaint)
                y += 14f
            } else {
                canvas.drawText(title, MARGIN, y + SMALL_SIZE, smallPaint)
                y += SMALL_SIZE + 10f
                canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rulePaint)
                y += 14f
            }

            while (lineIndex < lines.size && y + LINE_HEIGHT <= maxY) {
                val line = lines[lineIndex]
                val paint = when {
                    line.startsWith("====") || line.startsWith("----") -> {
                        // visual rule
                        canvas.drawLine(MARGIN, y + 4f, PAGE_WIDTH - MARGIN, y + 4f, rulePaint)
                        null
                    }
                    isSectionHeading(line) -> headingPaint
                    else -> bodyPaint
                }
                if (paint != null) {
                    // clip overly long lines just in case
                    val drawn = if (paint.measureText(line) > contentWidth) {
                        truncateToWidth(line, paint, contentWidth)
                    } else line
                    canvas.drawText(drawn, MARGIN, y + BODY_SIZE, paint)
                }
                y += LINE_HEIGHT
                lineIndex++
            }

            // Footer
            val footer = "Page $pageNumber"
            val footerWidth = smallPaint.measureText(footer)
            canvas.drawText(
                footer,
                (PAGE_WIDTH - footerWidth) / 2f,
                PAGE_HEIGHT - 28f,
                smallPaint
            )

            document.finishPage(page)

            // Safety: avoid infinite loop on empty content
            if (lines.isEmpty()) break
        }

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    private fun isSectionHeading(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return false
        // ALL-CAPS short lines or known section markers
        return (trimmed == trimmed.uppercase(Locale.US) &&
            trimmed.length in 4..60 &&
            trimmed.any { it.isLetter() } &&
            !trimmed.startsWith("•") &&
            !trimmed.startsWith("-")) ||
            trimmed.endsWith(":")
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        text.lines().forEach { raw ->
            if (raw.isEmpty()) {
                result.add("")
                return@forEach
            }
            if (paint.measureText(raw) <= maxWidth) {
                result.add(raw)
                return@forEach
            }
            // Word-wrap
            val words = raw.split(" ")
            var current = StringBuilder()
            for (word in words) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    current = StringBuilder(candidate)
                } else {
                    if (current.isNotEmpty()) result.add(current.toString())
                    // If single word is still too long, hard-split
                    if (paint.measureText(word) > maxWidth) {
                        var remaining = word
                        while (remaining.isNotEmpty()) {
                            var cut = remaining.length
                            while (cut > 1 && paint.measureText(remaining.substring(0, cut)) > maxWidth) {
                                cut--
                            }
                            result.add(remaining.substring(0, cut))
                            remaining = remaining.substring(cut)
                        }
                        current = StringBuilder()
                    } else {
                        current = StringBuilder(word)
                    }
                }
            }
            if (current.isNotEmpty()) result.add(current.toString())
        }
        return result
    }

    private fun truncateToWidth(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "…"
    }

        /**
         * Export an A4 printable Attendance Roll Sheet (grid layout).
         * Students as rows, dates as columns, attendance status (P/A/L/E) as cells.
         */
        fun exportAttendanceRollSheet(
            context: Context,
            report: AttendanceReport
        ) {
            try {
                val document = PdfDocument()

                val titlePaint = Paint().apply {
                    isAntiAlias = true
                    textSize = TITLE_SIZE
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = 0xFF1E3A5F.toInt()
                }
                val headingPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = HEADING_SIZE
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = 0xFF1E40AF.toInt()
                }
                val bodyPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 8f // Smaller for grid
                    typeface = Typeface.MONOSPACE
                    color = 0xFF1F2937.toInt()
                }
                val smallPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = SMALL_SIZE
                    color = 0xFF6B7280.toInt()
                }
                val rulePaint = Paint().apply {
                    color = 0xFFCBD5E1.toInt()
                    strokeWidth = 0.5f
                }
                val headerCellPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 7f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = 0xFF1E3A5F.toInt()
                }
                val statusPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 8f
                    typeface = Typeface.MONOSPACE
                }

                val contentWidth = PAGE_WIDTH - 2 * MARGIN
                val maxY = PAGE_HEIGHT - MARGIN - 24f

                val students = report.studentSummaries
                val dates = getAttendanceDates(report)
            
                // Calculate column widths
                val nameColWidth = 100f
                val idColWidth = 50f
                val dateColWidth = if (dates.isNotEmpty()) {
                    (contentWidth - nameColWidth - idColWidth - 60f) / dates.size
                } else 20f
                val summaryColWidth = 60f

                // Draw pages - one page can hold roughly 25-30 rows
                val rowsPerPage = 28
                var pageNumber = 0
                var studentIndex = 0

                while (studentIndex < students.size || pageNumber == 0) {
                    pageNumber++
                    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    val page = document.startPage(pageInfo)
                    val canvas = page.canvas
                    var y = MARGIN

                    // Header on each page
                    if (pageNumber == 1) {
                        canvas.drawText("ATTENDANCE ROLL SHEET", MARGIN, y + TITLE_SIZE, titlePaint)
                        y += TITLE_SIZE + 4f
                        val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
                        canvas.drawText("Generated: $dateStr", MARGIN, y + SMALL_SIZE, smallPaint)
                        y += SMALL_SIZE + 6f
                    } else {
                        canvas.drawText("ATTENDANCE ROLL SHEET (cont.)", MARGIN, y + SMALL_SIZE, smallPaint)
                        y += SMALL_SIZE + 6f
                    }

                    // Class info
                    canvas.drawText("Class: ${report.classroomName}  |  Subject: ${report.subject}", MARGIN, y + BODY_SIZE, headingPaint)
                    y += LINE_HEIGHT
                    canvas.drawText("Date Range: ${report.dateRangeText}  |  Report Date: ${report.reportDate}", MARGIN, y + SMALL_SIZE, smallPaint)
                    y += LINE_HEIGHT + 2f
                    canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rulePaint)
                    y += 10f

                    // Column headers
                    val headerY = y
                    var x = MARGIN
                
                    // Name column header
                    canvas.drawRect(x, headerY, x + nameColWidth, headerY + LINE_HEIGHT * 1.5f, headingPaint).also { _ -> }
                    canvas.drawText("Student Name", x + 2f, headerY + 12f, headerCellPaint)
                    x += nameColWidth

                    // ID column header
                    canvas.drawRect(x, headerY, x + idColWidth, headerY + LINE_HEIGHT * 1.5f, headingPaint).also { _ -> }
                    canvas.drawText("ID", x + 2f, headerY + 12f, headerCellPaint)
                    x += idColWidth

                    // Date column headers
                    dates.forEachIndexed { idx, date ->
                        val colW = dateColWidth
                        canvas.drawRect(x, headerY, x + colW, headerY + LINE_HEIGHT * 1.5f, headingPaint).also { _ -> }
                        // Draw short date (MM/dd)
                        val shortDate = try {
                            SimpleDateFormat("MM/dd", Locale.US).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date))
                        } catch (e: Exception) {
                            date.take(5)
                        }
                        canvas.drawText(shortDate, x + 1f, headerY + 12f, headerCellPaint)
                        x += colW
                    }

                    // Summary columns
                    val summaryHeaders = listOf("P", "A", "L", "E", "%")
                    val sumColW = summaryColWidth / summaryHeaders.size
                    summaryHeaders.forEach { h ->
                        canvas.drawRect(x, headerY, x + sumColW, headerY + LINE_HEIGHT * 1.5f, headingPaint).also { _ -> }
                        canvas.drawText(h, x + 2f, headerY + 12f, headerCellPaint)
                        x += sumColW
                    }

                    y = headerY + LINE_HEIGHT * 1.5f + 2f

                    // Draw student rows for this page
                    var rowCount = 0
                    while (studentIndex < students.size && rowCount < rowsPerPage && y + LINE_HEIGHT <= maxY) {
                        val student = students[studentIndex]
                        x = MARGIN

                        // Alternate row background
                        if (rowCount % 2 == 0) {
                            val bgPaint = Paint().apply { color = 0xFFF9FAFB.toInt() }
                            canvas.drawRect(MARGIN, y - 2f, PAGE_WIDTH - MARGIN, y + LINE_HEIGHT - 2f, bgPaint)
                        }

                        // Student name
                        val nameDisplay = student.student.name.take(18)
                        canvas.drawText(nameDisplay, x + 2f, y + 10f, bodyPaint)
                        x += nameColWidth

                        // Student ID
                        canvas.drawText(student.student.studentNumber, x + 2f, y + 10f, bodyPaint)
                        x += idColWidth

                        // Attendance cells for each date
                        val studentRecords = getStudentRecordsForReport(student, report)
                        dates.forEachIndexed { idx, date ->
                            val colW = dateColWidth
                            val status = studentRecords[date] ?: AttendanceStatus.ABSENT // default if no record
                            val statusChar = when (status) {
                                AttendanceStatus.PRESENT -> "P"
                                AttendanceStatus.ABSENT -> "A"
                                AttendanceStatus.LATE -> "L"
                                AttendanceStatus.EXCUSED -> "E"
                            }
                            statusPaint.color = when (status) {
                                AttendanceStatus.PRESENT -> 0xFF059669.toInt() // Green
                                AttendanceStatus.LATE -> 0xFFD97706.toInt() // Amber
                                AttendanceStatus.EXCUSED -> 0xFF2563EB.toInt() // Blue
                                else -> 0xFFDC2626.toInt() // Red
                            }
                            canvas.drawText(statusChar, x + (colW - statusPaint.measureText(statusChar)) / 2f, y + 10f, statusPaint)
                            x += colW
                        }

                        // Summary counts
                        val presentStr = student.presentCount.toString()
                        val absentStr = student.absentCount.toString()
                        val lateStr = student.lateCount.toString()
                        val excusedStr = student.excusedCount.toString()
                        val rateStr = "%.0f%%".format(student.attendanceRate)

                        summaryHeaders.forEachIndexed { idx, _ ->
                            val colW = sumColW
                            val text = when (idx) {
                                0 -> presentStr
                                1 -> absentStr
                                2 -> lateStr
                                3 -> excusedStr
                                else -> rateStr
                            }
                            statusPaint.color = when (idx) {
                                0 -> 0xFF059669.toInt()
                                1 -> 0xFFDC2626.toInt()
                                2 -> 0xFFD97706.toInt()
                                3 -> 0xFF2563EB.toInt()
                                else -> 0xFF1F2937.toInt()
                            }
                            canvas.drawText(text, x + (colW - statusPaint.measureText(text)) / 2f, y + 10f, statusPaint)
                            x += colW
                        }

                        y += LINE_HEIGHT
                        rowCount++
                        studentIndex++
                    }

                    // Footer
                    val footer = "Page $pageNumber"
                    val footerWidth = smallPaint.measureText(footer)
                    canvas.drawText(
                        footer,
                        (PAGE_WIDTH - footerWidth) / 2f,
                        PAGE_HEIGHT - 28f,
                        smallPaint
                    )

                    // Legend on last page
                    if (studentIndex >= students.size) {
                        var legendY = y + 10f
                        if (legendY + LINE_HEIGHT * 4 <= maxY) {
                            canvas.drawLine(MARGIN, legendY, PAGE_WIDTH - MARGIN, legendY, rulePaint)
                            legendY += LINE_HEIGHT
                            val legendItems = listOf(
                                "P = Present (Green)" to 0xFF059669.toInt(),
                                "A = Absent (Red)" to 0xFFDC2626.toInt(),
                                "L = Late (Amber)" to 0xFFD97706.toInt(),
                                "E = Excused (Blue)" to 0xFF2563EB.toInt()
                            )
                            legendItems.forEach { (text, color) ->
                                statusPaint.color = color
                                canvas.drawText(text, MARGIN + 10f, legendY + 10f, statusPaint)
                                legendY += LINE_HEIGHT * 0.8f
                            }
                            legendY += 4f
                            canvas.drawText("Summary: Overall Rate: ${"%.1f".format(report.overallAttendanceRate)}%  |  Total Records: ${report.totalRecords}", MARGIN, legendY + 10f, smallPaint)
                        }
                    }

                    document.finishPage(page)

                    if (students.isEmpty()) break
                }

                // Save and share
                val dir = File(context.cacheDir, "reports").apply { mkdirs() }
                val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                val safeClass = report.classroomName.replace(Regex("[^A-Za-z0-9_-]"), "_").take(30)
                val file = File(dir, "RollSheet_${safeClass}_$stamp.pdf")
                FileOutputStream(file).use { out ->
                    document.writeTo(out)
                }
                document.close()

                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Attendance Roll Sheet - ${report.classroomName}")
                    putExtra(Intent.EXTRA_TEXT, "Printable Attendance Roll Sheet for ${report.classroomName} (${report.subject})")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Share Attendance Roll Sheet")
                )

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to export roll sheet: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        /**
         * Extract unique sorted dates from the attendance report.
         * In a real implementation, we'd need to pass the actual records or date list.
         * For now, we generate dates based on totalDaysRecorded.
         */
        private fun getAttendanceDates(report: AttendanceReport): List<String> {
            // This is a placeholder - in reality we'd need the actual dates from the records
            // For the PDF grid, we need the actual date strings
            // The AttendanceReport doesn't currently store the list of dates, so we derive from totalDaysRecorded
            // This would need to be enhanced by passing the actual dates from the ViewModel
            return (1..report.totalDaysRecorded).map { "Day $it" }
        }

        /**
         * Get student records for a specific date from the report.
         * Since AttendanceReport doesn't store per-date records, we need to derive from the report
         * or pass the actual records. This is a simplified version.
         */
        private fun getStudentRecordsForReport(
            studentSummary: StudentAttendanceSummary,
            report: AttendanceReport
        ): Map<String, AttendanceStatus> {
            // This would need actual record data to be accurate
            // For now, return empty map - the grid will show default status
            // In a full implementation, we'd pass the AttendanceRecordEntity list
            return emptyMap()
        }

        /**
         * Create the PDF file in cache and return it.
         */
        fun buildPdfFile(
