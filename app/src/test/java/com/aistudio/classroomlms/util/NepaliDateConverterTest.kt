package com.aistudio.classroomlms.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Calendar

class NepaliDateConverterTest {

    @Test
    fun `anchor date 2013-04-14 converts to 2070 Baisakh 1`() {
        val cal = Calendar.getInstance().apply {
            set(2013, Calendar.APRIL, 14, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val bs = NepaliDateConverter.adToBs(cal)
        assertNotNull(bs)
        assertEquals(2070, bs!!.year)
        assertEquals(1, bs.month)
        assertEquals(1, bs.day)
        assertEquals("१ बैशाख २०७०", bs.formatDevanagari(includeWeekday = false))
    }

    @Test
    fun `toDevanagariDigits converts ascii digits accurately`() {
        assertEquals("२०८३ मंसिर १०", "२०८३ मंसिर " + NepaliDateConverter.toDevanagariDigits("10"))
        assertEquals("०१२३४५६७८९", NepaliDateConverter.toDevanagariDigits("0123456789"))
    }

    @Test
    fun `getTimeBasedGreeting matches hour ranges`() {
        assertEquals("Good Morning", NepaliDateConverter.getTimeBasedGreeting(8))
        assertEquals("Good Afternoon", NepaliDateConverter.getTimeBasedGreeting(14))
        assertEquals("Good Evening", NepaliDateConverter.getTimeBasedGreeting(19))
    }
}
