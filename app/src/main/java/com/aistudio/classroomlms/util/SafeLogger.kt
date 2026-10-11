package com.aistudio.classroomlms.util

import android.util.Log
import com.aistudio.classroomlms.BuildConfig

/**
 * Safe logging utility that redacts PII, authentication tokens, API keys, and sensitive payloads
 * before printing to system Logcat. In release builds, debug logs are suppressed entirely.
 */
object SafeLogger {

    private val TOKEN_REGEX = Regex("(?i)(bearer\\s+[a-zA-Z0-9_\\-\\.]+|ya29\\.[a-zA-Z0-9_\\-]+|AIza[0-9A-Za-z-_]{35})")
    private val EMAIL_REGEX = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    private val SPREADSHEET_ID_REGEX = Regex("/d/([a-zA-Z0-9-_]+)")

    fun sanitize(input: String?): String {
        if (input == null) return "null"
        var sanitized = input
        sanitized = TOKEN_REGEX.replace(sanitized, "[REDACTED_TOKEN]")
        sanitized = EMAIL_REGEX.replace(sanitized, "[REDACTED_EMAIL]")
        sanitized = SPREADSHEET_ID_REGEX.replace(sanitized, "/d/[REDACTED_SHEET_ID]")
        return sanitized
    }

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, sanitize(message))
        }
    }

    fun i(tag: String, message: String) {
        Log.i(tag, sanitize(message))
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val safeMessage = sanitize(message)
        if (throwable != null) {
            val safeErr = "${throwable::class.java.simpleName}: ${sanitize(throwable.message)}"
            Log.w(tag, "$safeMessage ($safeErr)")
        } else {
            Log.w(tag, safeMessage)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val safeMessage = sanitize(message)
        if (throwable != null) {
            val safeErr = "${throwable::class.java.simpleName}: ${sanitize(throwable.message)}"
            Log.e(tag, "$safeMessage ($safeErr)")
        } else {
            Log.e(tag, safeMessage)
        }
    }
}
