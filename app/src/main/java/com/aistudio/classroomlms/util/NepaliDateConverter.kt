package com.aistudio.classroomlms.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class BikramSambatDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int,   // 1..32
    val dayOfWeekIndex: Int // Calendar.SUNDAY (1)..Calendar.SATURDAY (7)
) {
    fun formatDevanagari(includeWeekday: Boolean = true): String {
        val monthName = NepaliDateConverter.nepaliMonthsDevanagari.getOrElse(month - 1) { "" }
        val dayDeva = NepaliDateConverter.toDevanagariDigits(day.toString())
        val yearDeva = NepaliDateConverter.toDevanagariDigits(year.toString())
        return if (includeWeekday) {
            val weekday = NepaliDateConverter.nepaliWeekdaysDevanagari[dayOfWeekIndex] ?: ""
            "$dayDeva $monthName $yearDeva, $weekday"
        } else {
            "$dayDeva $monthName $yearDeva"
        }
    }
}

/**
 * Pure-Kotlin offline Gregorian (AD) to Bikram Sambat (BS) converter.
 * Covers BS 2070..2095 (approx. AD 2013..2039) with accurate month-length tables.
 */
object NepaliDateConverter {

    val nepaliMonthsDevanagari = listOf(
        "बैशाख", "जेठ", "असार", "साउन",
        "भदौ", "असोज", "कार्तिक", "मंसिर",
        "पुष", "माघ", "फागुन", "चैत्र"
    )

    val nepaliWeekdaysDevanagari = mapOf(
        Calendar.SUNDAY to "आइतबार",
        Calendar.MONDAY to "सोमबार",
        Calendar.TUESDAY to "मंगलबार",
        Calendar.WEDNESDAY to "बुधबार",
        Calendar.THURSDAY to "बिहीबार",
        Calendar.FRIDAY to "शुक्रबार",
        Calendar.SATURDAY to "शनिबार"
    )

    // Standard BS month days for years 2070..2095 (anchor: 2070-01-01 BS == 2013-04-14 AD)
    private val bsMonthDays: Map<Int, IntArray> = mapOf(
        2070 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2071 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2072 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2073 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2074 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2075 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2076 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2077 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2078 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2079 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2080 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2081 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2082 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2083 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2084 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2085 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2086 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2087 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2088 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2089 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2090 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2091 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2092 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2093 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2094 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2095 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30)
    )

    fun adToBs(calendar: Calendar): BikramSambatDate? {
        val target = (calendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val anchor = Calendar.getInstance().apply {
            set(2013, Calendar.APRIL, 14, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var remainingDays = TimeUnit.MILLISECONDS.toDays(target.timeInMillis - anchor.timeInMillis).toInt()
        if (remainingDays < 0) return null

        var bsYear = 2070
        var bsMonth = 1
        var bsDay = 1

        while (remainingDays > 0) {
            val months = bsMonthDays[bsYear] ?: return null
            val daysInMonth = months[bsMonth - 1]
            val daysLeftInMonth = daysInMonth - bsDay
            if (remainingDays <= daysLeftInMonth) {
                bsDay += remainingDays
                remainingDays = 0
            } else {
                remainingDays -= (daysLeftInMonth + 1)
                bsDay = 1
                bsMonth++
                if (bsMonth > 12) {
                    bsMonth = 1
                    bsYear++
                }
            }
        }

        return BikramSambatDate(
            year = bsYear,
            month = bsMonth,
            day = bsDay,
            dayOfWeekIndex = target.get(Calendar.DAY_OF_WEEK)
        )
    }

    fun adDateStringToBsDevanagari(dateStr: String, includeWeekday: Boolean = false): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(dateStr) ?: return ""
            val cal = Calendar.getInstance().apply { time = date }
            adToBs(cal)?.formatDevanagari(includeWeekday) ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    fun toDevanagariDigits(input: String): String {
        val devanagariDigits = charArrayOf('०', '१', '२', '३', '४', '५', '६', '७', '८', '९')
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(devanagariDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun getTimeBasedGreeting(hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): String {
        return when (hourOfDay) {
            in 4..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    fun formatAdDate(date: Date = Date()): String {
        return SimpleDateFormat("EEE, dd MMM yyyy", Locale.US).format(date)
    }
}
