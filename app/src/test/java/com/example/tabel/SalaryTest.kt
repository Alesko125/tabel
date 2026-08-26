package com.example.tabel

import com.example.tabel.data.Salary
import com.example.tabel.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SalaryTest {

    private val base = Settings.DEFAULT_BASE      // 16700
    private val bonus = Settings.DEFAULT_BONUS    // 4000

    @Test
    fun `повний місяць дає заявлені 20700 на руки`() {
        val sheet = Fixtures.load("aug2026", 2026, 8)
        val result = Salary.compute(sheet, sheet.person("Кобзар")!!, base, bonus)

        assertTrue(result.onRotation)
        assertEquals(10, result.plannedShifts)
        assertEquals(10, result.workedShifts)
        assertEquals(4000.0, result.bonus, 0.01)
        assertEquals(20700.0, result.total, 0.01)
        assertEquals(0.0, result.bonusLost, 0.01)
    }

    @Test
    fun `липень 2026 — половина діб у відпустці`() {
        val sheet = Fixtures.load("jul2026", 2026, 7)
        val result = Salary.compute(sheet, sheet.person("Кобзар")!!, base, bonus)

        assertEquals(10, result.plannedShifts)
        assertEquals(5, result.workedShifts)
        assertEquals(400.0, result.bonusPerShift, 0.01)
        assertEquals(2000.0, result.bonus, 0.01)
        assertEquals(18700.0, result.total, 0.01)
        assertEquals(2000.0, result.bonusLost, 0.01)
        assertEquals(14, result.vacationDays)
    }

    @Test
    fun `липень 2026 — доби Кобзаря закрив Нещеретний`() {
        val sheet = Fixtures.load("jul2026", 2026, 7)
        val result = Salary.compute(sheet, sheet.person("Кобзар")!!, base, bonus)

        assertEquals(listOf(6, 9, 12, 15, 18), result.coveredForMe.map { it.day })
        assertTrue(result.coveredForMe.all { it.who.startsWith("Нещеретний") })
    }

    @Test
    fun `січень 2026 — місяць з 11 плановими добами`() {
        val sheet = Fixtures.load("jan2026", 2026, 1)
        val result = Salary.compute(sheet, sheet.person("Кобзар")!!, base, bonus)

        // Графік починається 1 січня, крок 3 дні: 1,4,…,31 — це 11 діб.
        assertEquals(11, result.plannedShifts)
        assertEquals(6, result.workedShifts)
        assertEquals(4000.0 / 11, result.bonusPerShift, 0.01)
        assertEquals(4000.0 / 11 * 6, result.bonus, 0.01)
        assertEquals(listOf(4, 7, 10, 13, 16), result.coveredForMe.map { it.day })
    }

    @Test
    fun `частки надбавки Кобзаря і заміни разом дають повну надбавку`() {
        val sheet = Fixtures.load("jul2026", 2026, 7)
        val result = Salary.compute(sheet, sheet.person("Кобзар")!!, base, bonus)

        val mine = result.bonus
        val theirs = result.bonusPerShift * result.coveredForMe.size
        assertEquals(bonus, mine + theirs, 0.01)
    }
}
