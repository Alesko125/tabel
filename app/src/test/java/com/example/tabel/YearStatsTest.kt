package com.example.tabel

import com.example.tabel.data.YearStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YearStatsTest {

    private val sheets = listOf(
        Fixtures.load("jan2026", 2026, 1),
        Fixtures.load("jul2026", 2026, 7),
        Fixtures.load("aug2026", 2026, 8)
    )

    @Test
    fun `зводить підсумки по всіх людях`() {
        val result = YearStats.aggregate(sheets, myName = "Кобзар")

        assertEquals(3, result.monthsLoaded)
        assertTrue(result.people.size > 15)
        assertTrue(result.people.any { it.name.startsWith("Кобзар") })
    }

    @Test
    fun `у чергового вихідні рахуються з порожніх клітинок`() {
        val result = YearStats.aggregate(sheets, myName = "Кобзар")
        val me = result.people.first { it.name.startsWith("Кобзар") }

        // Січень 6 діб, липень 5, серпень 10 — разом 21.
        assertEquals(21, me.dobaCount)
        assertEquals(21, me.workedDays)
        assertEquals(21 * 24.0, me.hours, 0.01)
        // Решта днів трьох місяців — вихідні чи відпустка, «немає даних» не лишається.
        val totalDays = 31 + 31 + 31
        assertEquals(totalDays, me.workedDays + me.offDays + me.vacationDays + me.sickDays)
    }

    @Test
    fun `рахує напарників за спільними змінами`() {
        val result = YearStats.aggregate(sheets, myName = "Кобзар")

        assertTrue("Мають бути напарники", result.partners.isNotEmpty())
        // Найчастіший напарник має більше спільних змін, ніж найрідший.
        assertTrue(result.partners.first().sharedShifts >= result.partners.last().sharedShifts)
        assertTrue(result.partners.none { it.name.startsWith("Кобзар") })
    }

    @Test
    fun `рахує вихідні та зміни на суботу-неділю`() {
        val result = YearStats.aggregate(sheets, myName = "Кобзар")

        assertTrue(result.weekendsOff > 0)
        assertTrue(result.weekendsWorked > 0)
        // Відпустка в липні дає довгу перерву.
        assertTrue("Найдовша перерва мала б бути помітною", result.longestBreak >= 14)
    }

    @Test
    fun `період охоплює перший і останній місяць`() {
        val result = YearStats.aggregate(sheets, myName = "Кобзар")
        val me = result.people.first { it.name.startsWith("Кобзар") }

        assertEquals(2026, me.firstYear)
        assertEquals(2026, me.lastYear)
        // Січень..серпень — вісім місяців у діапазоні, з них є три.
        assertEquals(8, me.spanMonths)
        assertEquals(3, me.monthsPresent)
    }
}
