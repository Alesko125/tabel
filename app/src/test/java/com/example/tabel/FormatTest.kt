package com.example.tabel

import com.example.tabel.data.DayEntry
import com.example.tabel.data.Format
import com.example.tabel.data.ShiftTimes
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    private val times = ShiftTimes(dobaStart = 9, dayStart = 9)

    @Test
    fun `у табелі фактичні години, на зміні на годину довше через обід`() {
        assertEquals("8 год · 09:00–18:00", Format.shiftLabel(DayEntry.parse("8"), times))
        assertEquals("11 год · 09:00–21:00", Format.shiftLabel(DayEntry.parse("11"), times))
    }

    @Test
    fun `коротка зміна рахується без обіду`() {
        assertEquals("4 год", Format.shiftLabel(DayEntry.parse("4"), times))
    }

    @Test
    fun `доба показується як перехід через добу`() {
        assertEquals("доба · 09:00 → 09:00", Format.shiftLabel(DayEntry.parse("24"), times))
    }

    @Test
    fun `інший початок зміни зсуває кінець`() {
        val early = ShiftTimes(dobaStart = 8, dayStart = 8)
        assertEquals("8 год · 08:00–17:00", Format.shiftLabel(DayEntry.parse("8"), early))
        assertEquals("11 год · 08:00–20:00", Format.shiftLabel(DayEntry.parse("11"), early))
    }

    @Test
    fun `статуси підписуються словами`() {
        assertEquals("вихідний", Format.statusLabel(DayEntry.parse("В")))
        assertEquals("відпустка", Format.statusLabel(DayEntry.parse("о/п")))
        assertEquals("лікарняний", Format.statusLabel(DayEntry.parse("б/л")))
    }

    @Test
    fun `множина для діб і днів`() {
        assertEquals("1 доба", Format.shifts(1))
        assertEquals("2 доби", Format.shifts(2))
        assertEquals("10 діб", Format.shifts(10))
        assertEquals("11 діб", Format.shifts(11))
        assertEquals("1 день", Format.days(1))
        assertEquals("14 днів", Format.days(14))
    }
}
