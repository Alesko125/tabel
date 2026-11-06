package com.example.tabel

import com.example.tabel.data.Rotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RotationTest {

    @Test
    fun `виводить ротацію з реального серпня 2026`() {
        val sheet = Fixtures.load("aug2026", 2026, 8)
        val me = sheet.person("Кобзар")!!
        val rotation = Rotation.fromDoba(me.dobaDays, 2026, 8)!!

        assertEquals(3, rotation.step)
        assertEquals(LocalDate.of(2026, 8, 2), rotation.anchor)
        assertEquals(me.dobaDays, rotation.dobaDaysIn(2026, 8))
    }

    @Test
    fun `прогнозує місяць, якого ще немає в таблиці`() {
        val sheet = Fixtures.load("aug2026", 2026, 8)
        val rotation = Rotation.fromDoba(sheet.person("Кобзар")!!.dobaDays, 2026, 8)!!

        // Серпень: 2,5,…,29. Далі 1 вересня — це наступний крок після 29 серпня.
        assertEquals(listOf(1, 4, 7, 10, 13, 16, 19, 22, 25, 28), rotation.dobaDaysIn(2026, 9))

        // Через рік крок тримається, але дати зсуваються: 365 не ділиться на 3.
        assertEquals(
            listOf(3, 6, 9, 12, 15, 18, 21, 24, 27, 30),
            rotation.dobaDaysIn(2027, 8)
        )
        assertFalse(rotation.isDobaOn(LocalDate.of(2027, 8, 2)))
    }

    @Test
    fun `відпустка посеред місяця не збиває крок`() {
        val sheet = Fixtures.load("jul2026", 2026, 7)
        val me = sheet.person("Кобзар")!!
        // Відроблені доби: 3, потім розрив на відпустку, далі 21,24,27,30.
        assertEquals(3, Rotation.deriveStep(me.dobaDays))
    }

    @Test
    fun `знаходить наступну добу`() {
        val rotation = Rotation(LocalDate.of(2026, 8, 2), 3)
        assertEquals(LocalDate.of(2026, 8, 26), rotation.nextDobaFrom(LocalDate.of(2026, 8, 24)))
        assertEquals(LocalDate.of(2026, 8, 26), rotation.nextDobaFrom(LocalDate.of(2026, 8, 26)))
        assertEquals(LocalDate.of(2026, 9, 1), rotation.nextDobaFrom(LocalDate.of(2026, 8, 30)))
    }

    @Test
    fun `без діб ротацію вивести неможливо`() {
        assertEquals(null, Rotation.fromDoba(emptyList(), 2026, 8))
        assertFalse(Rotation(LocalDate.of(2026, 8, 2), 0).isDobaOn(LocalDate.of(2026, 8, 2)))
    }
}
