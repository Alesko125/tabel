package com.example.tabel

import com.example.tabel.data.DayEntry
import com.example.tabel.data.Format
import com.example.tabel.data.ShiftTimes
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Часу початку зміни в табелі нема, так що винятки забиваю вручну.
 * Перевіряю, що індивідуальна година перекриває загальну.
 */
class PersonTimesTest {

    private val times = ShiftTimes(
        dobaStart = 8,
        dayStart = 9,
        perPerson = mapOf("Зiнченко О." to 8, "Савіна Т." to 11)
    )

    @Test
    fun `без винятку береться загальний початок зміни`() {
        assertEquals(
            "8 год · 09:00–18:00",
            Format.shiftLabel(DayEntry.parse("8"), times, "Жовток Б.")
        )
    }

    @Test
    fun `виняток зсуває і початок, і кінець`() {
        assertEquals(
            "8 год · 08:00–17:00",
            Format.shiftLabel(DayEntry.parse("8"), times, "Зiнченко О.")
        )
        assertEquals(
            "8 год · 11:00–20:00",
            Format.shiftLabel(DayEntry.parse("8"), times, "Савіна Т.")
        )
    }

    @Test
    fun `одинадцятигодинна зміна теж враховує виняток`() {
        assertEquals(
            "11 год · 09:00–21:00",
            Format.shiftLabel(DayEntry.parse("11"), times, "Горбенко О.")
        )
        assertEquals(
            "11 год · 08:00–20:00",
            Format.shiftLabel(DayEntry.parse("11"), times, "Зiнченко О.")
        )
    }

    @Test
    fun `доба не залежить від індивідуального часу денної зміни`() {
        assertEquals(
            "доба · 08:00 → 08:00",
            Format.shiftLabel(DayEntry.parse("24"), times, "Зiнченко О.")
        )
    }
}
