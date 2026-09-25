package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
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
}
