package com.example.tabel.data

import java.time.LocalDate
import java.time.YearMonth

/**
 * Добова ротація: відома доба та крок у днях.
 *
 * Таблиця закінчується останнім заповненим місяцем, але ротація детермінована,
 * тож графік можна порахувати на будь-який місяць уперед. Такі дні позначаються
 * як прогноз, щоб не видавати обчислене за дані з табеля.
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

    /** Найближча доба, починаючи з указаної дати включно. */
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

        /** Виводить ротацію з відпрацьованих діб місяця. */
        fun fromDoba(dobaDays: List<Int>, year: Int, month: Int): Rotation? {
            if (dobaDays.isEmpty()) return null
            val step = deriveStep(dobaDays)
            return Rotation(LocalDate.of(year, month, dobaDays.first()), step)
        }

        /**
         * Крок — найменший проміжок між сусідніми добами. Відпустка посеред
         * місяця дає великий розрив, тож беремо саме мінімум.
         */
        fun deriveStep(dobaDays: List<Int>): Int {
            if (dobaDays.size < 2) return DEFAULT_STEP
            val minGap = dobaDays.sorted().zipWithNext { a, b -> b - a }.minOrNull() ?: DEFAULT_STEP
            return if (minGap in 2..7) minGap else DEFAULT_STEP
        }
    }
}
