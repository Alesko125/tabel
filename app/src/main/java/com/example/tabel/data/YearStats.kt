package com.example.tabel.data

/** Підсумок по людині за вибраний період. */
data class PersonYearStats(
    val name: String,
    val offDays: Int,
    val vacationDays: Int,
    val sickDays: Int,
    val workedDays: Int,
    /** Робочі дні за графіком, яким ще не проставили фактичні години. */
    val expectedDays: Int,
    val dobaCount: Int,
    val hours: Double,
    /** Скільки місяців цієї людини знайшлося в таблиці. */
    val monthsPresent: Int,
    /** Перший і останній місяць у вигляді year * 12 + (month - 1). */
    val firstMonthIndex: Int,
    val lastMonthIndex: Int
) {
    val firstYear: Int get() = firstMonthIndex / 12
    val lastYear: Int get() = lastMonthIndex / 12

    /** Стаж у табелі: скільки років і місяців охоплюють записи. */
    val spanMonths: Int get() = lastMonthIndex - firstMonthIndex + 1
}

/** Скільки змін я відпрацював разом із конкретною людиною. */
data class Partner(val name: String, val sharedShifts: Int)

data class StatsResult(
    val people: List<PersonYearStats>,
    val monthsLoaded: Int,
    val monthsRequested: Int,
    /** З ким найчастіше збігалися зміни. */
    val partners: List<Partner> = emptyList(),
    /** Скільки моїх вихідних випало на суботу чи неділю. */
    val weekendsOff: Int = 0,
    /** Скільки моїх змін випало на суботу чи неділю. */
    val weekendsWorked: Int = 0,
    /** Найдовший ланцюжок днів поспіль без роботи. */
    val longestBreak: Int = 0
)

object YearStats {

    /**
     * Зводить місячні аркуші в підсумок по людях.
     *
     * Порожні клітинки означають різне залежно від рядка: у чергових це
     * вихідний між добами, у решти — робочий день без проставлених годин.
     * [PersonMonth.day] це вже враховує, тож такі дні йдуть у вихідні
     * та в заплановані відповідно, а не губляться.
     */
    fun aggregate(
        sheets: List<MonthSheet>,
        myName: String = "",
        requested: Int = sheets.size
    ): StatsResult {
        val acc = mutableMapOf<String, MutableStats>()
        val shared = mutableMapOf<String, Int>()
        var weekendsOff = 0
        var weekendsWorked = 0
        var longestBreak = 0
        var currentBreak = 0

        for (sheet in sheets.sortedBy { it.year * 12 + it.month }) {
            val index = sheet.year * 12 + (sheet.month - 1)

            val me = if (myName.isBlank()) null else sheet.person(myName)
            if (me != null) {
                for (day in 1..sheet.daysInMonth) {
                    val mine = me.day(day)
                    val isWeekend = java.time.LocalDate.of(sheet.year, sheet.month, day)
                        .dayOfWeek.value >= 6

                    if (mine.isWork) {
                        if (isWeekend) weekendsWorked++
                        currentBreak = 0
                        for (other in sheet.people) {
                            if (other.matches(myName)) continue
                            if (other.day(day).isWork) {
                                shared[other.name] = (shared[other.name] ?: 0) + 1
                            }
                        }
                    } else if (mine.kind == Kind.EXPECTED) {
                        // День за графіком робочий — перерву він не продовжує.
                        currentBreak = 0
                    } else if (mine.kind != Kind.NONE) {
                        if (isWeekend && mine.kind == Kind.OFF) weekendsOff++
                        currentBreak++
                        longestBreak = maxOf(longestBreak, currentBreak)
                    }
                }
            }

            for (person in sheet.people) {
                val s = acc.getOrPut(person.name) { MutableStats() }
                s.months++
                s.first = minOf(s.first, index)
                s.last = maxOf(s.last, index)
                for (day in 1..sheet.daysInMonth) {
                    val entry = person.day(day)
                    when (entry.kind) {
                        Kind.EXPECTED -> s.expected++
                        Kind.OFF -> s.off++
                        Kind.VACATION -> s.vacation++
                        Kind.SICK -> s.sick++
                        Kind.WORK -> {
                            s.worked++
                            s.hours += entry.hours ?: 0.0
                            if (entry.shift == ShiftKind.DOBA) s.doba++
                        }
                        else -> Unit
                    }
                }
            }
        }

        val people = acc.map { (name, s) ->
            PersonYearStats(
                name = name,
                offDays = s.off,
                vacationDays = s.vacation,
                sickDays = s.sick,
                workedDays = s.worked,
                expectedDays = s.expected,
                dobaCount = s.doba,
                hours = s.hours,
                monthsPresent = s.months,
                firstMonthIndex = s.first,
                lastMonthIndex = s.last
            )
        }.sortedByDescending { it.offDays }

        return StatsResult(
            people = people,
            monthsLoaded = sheets.size,
            monthsRequested = requested,
            partners = shared.map { Partner(it.key, it.value) }
                .sortedByDescending { it.sharedShifts },
            weekendsOff = weekendsOff,
            weekendsWorked = weekendsWorked,
            longestBreak = longestBreak
        )
    }

    private class MutableStats {
        var off = 0
        var vacation = 0
        var sick = 0
        var worked = 0
        var expected = 0
        var doba = 0
        var hours = 0.0
        var months = 0
        var first = Int.MAX_VALUE
        var last = Int.MIN_VALUE
    }
}
