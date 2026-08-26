package com.example.tabel

import com.example.tabel.data.MonthNames
import com.example.tabel.data.ScheduleChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleChangeTest {

    @Test
    fun `описує додані доби`() {
        val diff = ScheduleChange.Diff(
            year = 2026, month = 8,
            added = listOf(29), removed = emptyList(), total = 10
        )
        assertEquals("Додали 29 серпня (10 діб за місяць)", diff.describe())
    }

    @Test
    fun `описує зняті доби`() {
        val diff = ScheduleChange.Diff(
            year = 2026, month = 7,
            added = emptyList(), removed = listOf(6, 9, 12), total = 7
        )
        assertEquals("Зняли 6, 9, 12 липня (7 діб за місяць)", diff.describe())
    }

    @Test
    fun `описує обмін змінами`() {
        val diff = ScheduleChange.Diff(
            year = 2026, month = 8,
            added = listOf(30), removed = listOf(29), total = 10
        )
        val text = diff.describe()
        assertTrue(text.contains("Додали 30 серпня"))
        assertTrue(text.contains("зняли 29 серпня"))
    }

    @Test
    fun `місяць у родовому відмінку відповідає номеру`() {
        assertEquals("січня", MonthNames.UK_GENITIVE[0])
        assertEquals("грудня", MonthNames.UK_GENITIVE[11])
    }
}
