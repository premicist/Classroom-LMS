package com.aistudio.classroomlms.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.aistudio.classroomlms.domain.merit.StudentExamAggregate
import java.util.Locale

object ParentCommunicationHelper {

    /**
     * Cleans phone number by removing spaces, dashes, and ensuring valid format.
     */
    fun sanitizePhoneNumber(raw: String): String {
        return raw.replace(Regex("[^0-9+]"), "").trim()
    }

    /**
     * Generates a structured terminal exam progress report message.
     */
    fun formatExamReportMessage(
        studentName: String,
        studentRoll: String,
        examCategory: String,
        totalMarks: Double,
        fullMarks: Double,
        percentage: Double,
        gpa: Double,
        letterGrade: String,
        rank: Int,
        classroomName: String = ""
    ): String {
        val classInfo = if (classroomName.isNotBlank()) " ($classroomName)" else ""
        return buildString {
            append("📋 *Terminal Examination Report Card*\n\n")
            append("Dear Parent,\n")
            append("Here is the examination result for *${studentName.trim()}*${classInfo}:\n")
            append("• Roll No: $studentRoll\n")
            append("• Examination: $examCategory\n")
            append("• Total Score: ${String.format(Locale.US, "%.1f", totalMarks)} / ${String.format(Locale.US, "%.1f", fullMarks)}\n")
            append("• Percentage: ${String.format(Locale.US, "%.1f", percentage)}%\n")
            append("• GPA: ${String.format(Locale.US, "%.2f", gpa)} (Grade: $letterGrade)\n")
            if (rank > 0) {
                append("• Class Rank: Position #$rank\n")
            }
            append("\nPlease contact the school administration if you have questions.\n")
            append("Best regards,\nClass Teacher")
        }
    }

    /**
     * Generates an absenteeism alert message.
     */
    fun formatAbsenteeMessage(
        studentName: String,
        date: String,
        classroomName: String = ""
    ): String {
        val classInfo = if (classroomName.isNotBlank()) " in $classroomName" else ""
        return buildString {
            append("⚠️ *School Absentee Notice*\n\n")
            append("Dear Parent,\n")
            append("This is to inform you that *${studentName.trim()}* is marked ABSENT today ($date)${classInfo}.\n")
            append("If this absence was unexpected, please contact the school administration immediately.\n\n")
            append("Best regards,\nClass Teacher")
        }
    }

    /**
     * Launches WhatsApp directly with pre-filled message using standard wa.me URI.
     */
    fun launchWhatsApp(context: Context, phone: String, message: String): Boolean {
        val sanitized = sanitizePhoneNumber(phone)
        val cleanNumber = sanitized.removePrefix("+")
        return try {
            val encodedMessage = Uri.encode(message)
            val uri = if (cleanNumber.isNotBlank()) {
                Uri.parse("https://wa.me/$cleanNumber?text=$encodedMessage")
            } else {
                Uri.parse("https://wa.me/?text=$encodedMessage")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Launches SMS app with pre-filled recipient and message using smsto: URI.
     */
    fun launchSms(context: Context, phone: String, message: String): Boolean {
        val sanitized = sanitizePhoneNumber(phone)
        return try {
            val uri = Uri.parse("smsto:$sanitized")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
