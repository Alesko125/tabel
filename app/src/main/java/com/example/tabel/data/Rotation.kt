package com.example.tabel.data

import java.time.LocalDate
import java.time.YearMonth

/**
 * Добова ротація: відома доба і крок у днях.
 *
 * Таблиця кінчається останнім заповненим місяцем, але ротація детермінована,
 * так що графік можна порахувати на будь-який місяць наперед. Такі дні мічу
 * як прогноз, щоб не видавати пораховане за дані з табеля.
 */
data class Rotation(val anchor: LocalDate, val step: Int) {

    fun isDobaOn(date: LocalDate): Boolean {
        if (step <= 0) return false
        return (date.toEpochDay() - anchor.toEpochDay()).mod(step.toLong()) == 0L
    }

    fun dobaDaysIn(year: Int, month: Int): List<Int> {
        val ym = YearMonth.of(year, month)
        return (1..ym.lengthOfMonth()).filter { isDobaOn(ym.atDay(it)) }
    }

    /** Найближча доба, від указаної дати включно. */
    fun nextDobaFrom(date: LocalDate): LocalDate {
        if (step <= 0) return date
        var d = date
        repeat(step + 1) {
            if (isDobaOn(d)) return d
            d = d.plusDays(1)
        }
        return date
    }

    companion object {
        const val DEFAULT_STEP = 3

        /** Вгадую ротацію з відроблених діб місяця. */
        fun fromDoba(dobaDays: List<Int>, year: Int, month: Int): Rotation? {
            if (dobaDays.isEmpty()) return null
            val step = deriveStep(dobaDays)
            return Rotation(LocalDate.of(year, month, dobaDays.first()), step)
        }

        /**
         * Крок — найменший проміжок між сусідніми добами. Відпустка посеред
         * місяця дає великий розрив, так що беру саме мінімум.
         */
        fun deriveStep(dobaDays: List<Int>): Int {
            if (dobaDays.size < 2) return DEFAULT_STEP
            val minGap = dobaDays.sorted().zipWithNext { a, b -> b - a }.minOrNull() ?: DEFAULT_STEP
            return if (minGap in 2..7) minGap else DEFAULT_STEP
        }
    }
}
