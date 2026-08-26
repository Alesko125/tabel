package com.example.tabel.data

import java.time.LocalDate

/** Тип позначки в клітинці табеля. */
enum class Kind {
    WORK,       // відпрацьовані години (8, 11, 24, 4 …)
    EXPECTED,   // робочий день за графіком, години ще не проставлені
    OFF,        // В — вихідний
    VACATION,   // о/п — відпустка
    SICK,       // б/л — лікарняний
    UNPAID,     // НА — відпустка без збереження ЗП
    ABSENT,     // Н — відсутній
    OTHER,      // щось нерозпізнане, показуємо як є
    NONE        // порожня клітинка
}

/** Тип зміни, виведений з кількості годин. */
enum class ShiftKind { DOBA, LONG, DAY, PARTIAL, NONE }

data class DayEntry(
    val raw: String,
    val hours: Double?,
    val kind: Kind
) {
    val isWork: Boolean get() = kind == Kind.WORK

    val shift: ShiftKind
        get() = when {
            kind != Kind.WORK || hours == null -> ShiftKind.NONE
            hours >= 20 -> ShiftKind.DOBA
            hours >= 10 -> ShiftKind.LONG
            hours >= 7 -> ShiftKind.DAY
            else -> ShiftKind.PARTIAL
        }

    companion object {
        val EMPTY = DayEntry("", null, Kind.NONE)

        /**
         * У табелі вихідні позначають літерою «В», але чергових це не стосується:
         * у них вільні дні просто лишаються порожніми. Тому для добової ротації
         * порожня клітинка — це вихідний, а не «немає в графіку».
         */
        val IMPLICIT_OFF = DayEntry("", null, Kind.OFF)

        /**
         * У решти навпаки: вихідні проставлені літерою «В» наперед, а порожня
         * клітинка означає робочий день, у який ще не записали фактичні години.
         */
        val EXPECTED_WORK = DayEntry("", null, Kind.EXPECTED)

        fun parse(cell: String): DayEntry {
            val s = cell.trim()
            if (s.isEmpty()) return EMPTY

            // Число (у таблиці трапляється і "8", і "8.0", і "8,0")
            val num = s.replace(',', '.').toDoubleOrNull()
            if (num != null) {
                return if (num > 0) DayEntry(s, num, Kind.WORK) else EMPTY
            }

            val norm = s.lowercase().replace('\\', '/').replace(" ", "")
            return when {
                norm == "в" -> DayEntry(s, null, Kind.OFF)
                norm.startsWith("о/п") || norm == "оп" -> DayEntry(s, null, Kind.VACATION)
                norm.startsWith("б/л") || norm == "бл" -> DayEntry(s, null, Kind.SICK)
                norm == "на" -> DayEntry(s, null, Kind.UNPAID)
                norm == "н" -> DayEntry(s, null, Kind.ABSENT)
                else -> DayEntry(s, null, Kind.OTHER)
            }
        }
    }
}

/** Один рядок табеля: людина та її дні за місяць. */
data class PersonMonth(
    val name: String,
    /** Номер блоку в табелі — люди в одному блоці, як правило, з одного підрозділу. */
    val block: Int,
    val days: Map<Int, DayEntry>
) {
    /** Дні, коли людина відпрацювала добу (>= 20 год). */
    val dobaDays: List<Int> by lazy {
        days.filter { it.value.shift == ShiftKind.DOBA }.keys.sorted()
    }

    /** Черговий — той, хто цього місяця стоїть у добовій ротації. */
    val isOnDuty: Boolean by lazy { dobaDays.isNotEmpty() }

    /**
     * Порожня клітинка означає протилежне залежно від того, чий це рядок:
     * у чергового це вихідний між добами, у решти — робочий день за графіком,
     * якому ще не проставили фактичні години.
     */
    fun day(d: Int): DayEntry {
        val entry = days[d] ?: DayEntry.EMPTY
        if (entry.kind != Kind.NONE) return entry
        return if (isOnDuty) DayEntry.IMPLICIT_OFF else DayEntry.EXPECTED_WORK
    }

    /** Те, що реально стоїть у клітинці, без підстановки за правилами. */
    fun rawDay(d: Int): DayEntry = days[d] ?: DayEntry.EMPTY

    /**
     * Скільки годин у цієї людини зазвичай — найчастіше значення за місяць.
     * Потрібно, щоб підказати тривалість дня, у який години ще не записані.
     */
    val typicalHours: Double? by lazy {
        days.values.mapNotNull { it.hours }
            .filter { it in 1.0..24.0 }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }

    /**
     * У табелі прізвища записані як «Кобзар О.», а в налаштуваннях зберігається
     * саме прізвище — тому звіряємо за початком рядка, а не за точним збігом.
     */
    fun matches(query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return false
        return name.equals(q, ignoreCase = true) || name.startsWith(q, ignoreCase = true)
    }
}

data class MonthSheet(
    val year: Int,
    val month: Int,
    val people: List<PersonMonth>
) {
    val daysInMonth: Int get() = java.time.YearMonth.of(year, month).lengthOfMonth()

    fun person(name: String): PersonMonth? =
        people.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: people.firstOrNull { it.name.startsWith(name, ignoreCase = true) }
            ?: people.firstOrNull { it.name.contains(name, ignoreCase = true) }

    /**
     * Чи заповнений табель на цей день. Табельник проставляє дні поступово,
     * тож майбутні дати часто порожні. Чергових не рахуємо: у них порожня
     * клітинка — це вихідний, а не незаповнений день.
     */
    fun isFilledOn(day: Int): Boolean {
        val staff = people.filter { !it.isOnDuty }
        if (staff.isEmpty()) return true
        return staff.count { it.rawDay(day).kind != Kind.NONE } * 2 >= staff.size
    }

    /** Хто за графіком має працювати цього дня, але години ще не проставлені. */
    fun expectedOn(day: Int, exclude: String?): List<PersonMonth> =
        people.filter { person ->
            (exclude == null || !person.matches(exclude)) &&
                person.day(day).kind == Kind.EXPECTED
        }

    /** Усі, хто працює цього дня (крім вказаної людини). */
    fun workingOn(day: Int, exclude: String?): List<Pair<PersonMonth, DayEntry>> =
        people.mapNotNull { p ->
            if (exclude != null && p.matches(exclude)) return@mapNotNull null
            val e = p.day(day)
            if (e.isWork) p to e else null
        }

    fun contains(date: LocalDate): Boolean = date.year == year && date.monthValue == month
}
