package com.winzone.companion

import com.winzone.companion.util.Time
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ClockParserTest {

    @Test
    fun `test MM_SS formatting and parsing`() {
        assertEquals(90, Time.parseClockString("01:30"))
        assertEquals(75, Time.parseClockString("1:15"))
        assertEquals(0, Time.parseClockString("00:00"))
        assertEquals(2700, Time.parseClockString("45:00"))
        assertEquals(5400, Time.parseClockString("90:00"))
    }

    @Test
    fun `test apostrophe clock format`() {
        assertEquals(90, Time.parseClockString("01'30"))
        assertEquals(2700, Time.parseClockString("45'00"))
        assertEquals(2700, Time.parseClockString("45'"))
    }

    @Test
    fun `test seconds only and raw integers`() {
        assertEquals(120, Time.parseClockString("120"))
        assertEquals(45, Time.parseClockString("45"))
    }

    @Test
    fun `test invalid or unreadable clock returns null`() {
        assertNull(Time.parseClockString("INVALID"))
        assertNull(Time.parseClockString(""))
        assertNull(Time.parseClockString("abc:def"))
    }

    @Test
    fun `test formatClockSeconds formatting`() {
        assertEquals("01:30", Time.formatClockSeconds(90))
        assertEquals("45:00", Time.formatClockSeconds(2700))
        assertEquals("00:05", Time.formatClockSeconds(5))
        assertEquals("--:--", Time.formatClockSeconds(null))
        assertEquals("--:--", Time.formatClockSeconds(-1))
    }
}
