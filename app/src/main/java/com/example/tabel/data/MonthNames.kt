package com.example.tabel.data

import java.time.LocalDate

object MonthNames {
    val UK = listOf(
        "січень", "лютий", "березень", "квітень", "травень", "червень",
        "липень", "серпень", "вересень", "жовтень", "листопад", "грудень"
    )

    /** Родовий відмінок — для заголовків на кшталт «26 серпня». */
    val UK_GENITIVE = listOf(
        "січня", "лютого", "березня", "квітня", "травня", "червня",
        "липня", "серпня", "вересня", "жовтня", "листопада", "грудня"
    )

    val WEEKDAYS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Нд")
    val WEEKDAYS_FULL = listOf(
        "понеділок", "вівторок", "середа", "четвер", "п'ятниця", "субота", "неділя"
    )

    fun title(year: Int, month: Int): String = "${UK[month - 1].replaceFirstChar { it.uppercase() }} $year"

    fun dayTitle(date: LocalDate): String =
        "${date.dayOfMonth} ${UK_GENITIVE[date.monthValue - 1]}, ${WEEKDAYS_FULL[date.dayOfWeek.value - 1]}"

    /**
     * Назви аркушів у таблиці неконсистентні: «серпень 2026», «Листопад 2024»,
     * «травень 2024 » з пробілом у кінці, «грудень2022» без пробілу.
     * Нормалізуємо для зіставлення.
     */
    fun normalize(s: String): String =
        s.lowercase().replace(Regex("\\s+"), "").trim()

    fun sheetKey(year: Int, month: Int): String = normalize("${UK[month - 1]}$year")
}
