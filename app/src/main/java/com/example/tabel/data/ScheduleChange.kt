package com.example.tabel.data

/**
 * Порівнюю мої доби з тим, що застосунок бачив минулого разу.
 *
 * Табель правлять протягом місяця — інакше зміну в графіку помітиш
 * хіба випадково. Перевіряю і фоновим воркером, і самим застосунком при
 * завантаженні: бо інакше відкрив застосунок — і він мовчки затер би знімок,
 * і сповіщення вже б не спрацювало.
 */
object ScheduleChange {

    data class Diff(
        val year: Int,
        val month: Int,
        val added: List<Int>,
        val removed: List<Int>,
        val total: Int
    ) {
        fun describe(): String {
            val genitive = MonthNames.UK_GENITIVE[month - 1]
            val parts = mutableListOf<String>()
            if (added.isNotEmpty()) parts += "додали ${added.joinToString(", ")} $genitive"
            if (removed.isNotEmpty()) parts += "зняли ${removed.joinToString(", ")} $genitive"
            if (parts.isEmpty()) parts += "графік перебудували"
            return parts.joinToString("; ").replaceFirstChar { it.uppercase() } +
                " (${Format.shifts(total)} за місяць)"
        }
    }

    /**
     * Звіряю і одразу зберігаю новий знімок. Повертаю null, якщо змін нема
     * або якщо це перше завантаження місяця — тоді ще нема з чим порівнювати.
     */
    fun checkAndStore(settings: Settings, sheet: MonthSheet): Diff? {
        val me = sheet.person(settings.myName) ?: return null
        val days = me.dobaDays
        if (days.isEmpty()) return null

        val previous = settings.snapshot(sheet.year, sheet.month)
        settings.saveSnapshot(sheet.year, sheet.month, days)

        if (previous.isBlank()) return null

        val before = previous.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
        val after = days.toSet()
        if (before == after) return null

        return Diff(
            year = sheet.year,
            month = sheet.month,
            added = (after - before).sorted(),
            removed = (before - after).sorted(),
            total = after.size
        )
    }
}
