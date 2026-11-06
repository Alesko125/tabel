package com.example.tabel.data

/**
 * Розбираю CSV-експорт одного місячного аркуша табеля.
 *
 * Структура аркуша (перевірив на даних 2014–2026):
 *  - аркуш з кількох блоків (підрозділів);
 *  - кожен блок починається рядком-шапкою з числами місяця 1..31;
 *  - у рядках під шапкою: колонка «Прізвище» — ім'я, далі колонки днів.
 * Рядки «пливуть» від місяця до місяця (люди приходять і йдуть),
 * так що нічого не в'яжу до фіксованих номерів рядків.
 */
object SheetParser {

    private const val DEFAULT_NAME_COL = 2
    private const val FIRST_DAY_COL = 4

    fun parse(csv: String, year: Int, month: Int): MonthSheet {
        val rows = CsvParser.parse(csv)
        val nameCol = detectNameColumn(rows)
        val people = mutableListOf<PersonMonth>()
        var block = -1
        var dayCols: Map<Int, Int> = emptyMap()

        for (row in rows) {
            val header = readDayHeader(row)
            if (header != null) {
                block++
                dayCols = header
                continue
            }
            if (dayCols.isEmpty()) continue

            val name = row.getOrNull(nameCol)?.trim().orEmpty()
            if (name.isEmpty() || !looksLikeName(name)) continue

            val days = mutableMapOf<Int, DayEntry>()
            for ((col, day) in dayCols) {
                val entry = DayEntry.parse(row.getOrNull(col).orEmpty())
                if (entry.kind != Kind.NONE) days[day] = entry
            }
            if (days.isEmpty()) continue

            people += PersonMonth(name = normalizeName(name), block = block, days = days)
        }

        return MonthSheet(year = year, month = month, people = people)
    }

    /**
     * Рядок-шапка: у колонках десь з ~E стоять послідовні числа 1,2,3…
     * Повертаю мапу «індекс колонки → число місяця».
     */
    private fun readDayHeader(row: List<String>): Map<Int, Int>? {
        if (row.size <= FIRST_DAY_COL + 10) return null
        val map = mutableMapOf<Int, Int>()
        var expected = 1
        for (col in FIRST_DAY_COL until row.size) {
            val v = row[col].trim().removeSuffix(".0").toIntOrNull() ?: continue
            if (v == expected && v <= 31) {
                map[col] = v
                expected++
            }
        }
        // За шапку беру рядок, де знайшлось хоч 28 послідовних днів.
        return if (map.size >= 28) map else null
    }

    private fun detectNameColumn(rows: List<List<String>>): Int {
        for (row in rows) {
            for ((i, cell) in row.withIndex()) {
                if (cell.contains("Прізвище", ignoreCase = true)) return i
            }
        }
        return DEFAULT_NAME_COL
    }

    /** Відсіюю службові рядки (підписи, легенду) від справжніх прізвищ. */
    private fun looksLikeName(s: String): Boolean {
        if (s.length < 3) return false
        if (s.any { it.isDigit() }) return false
        if (s.contains('_')) return false
        val lower = s.lowercase()
        val junk = listOf(
            "прізвище", "ім'я", "по-батькові", "табельник", "начальник",
            "майстер", "разом", "усього", "всього", "підпис", "лікарняний",
            "відпустка", "відсутн", "премія", "час роботи", "робочий день"
        )
        if (junk.any { lower.contains(it) }) return false
        return s.any { it.isLetter() }
    }

    /** «Ніколаєнко В. » → «Ніколаєнко В.» */
    private fun normalizeName(s: String): String = s.trim().replace(Regex("\\s+"), " ")
}
