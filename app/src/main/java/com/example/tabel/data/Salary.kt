package com.example.tabel.data

/** Хто закривав добу за мене (або я за когось). */
data class Cover(val day: Int, val who: String)

data class SalaryBreakdown(
    val year: Int,
    val month: Int,
    /** Скільки діб мало бути за графіком «доба через дві» цього місяця. */
    val plannedShifts: Int,
    /** Скільки діб я реально відробив. */
    val workedShifts: Int,
    /** Доби понад мій графік — коли я підміняв когось. */
    val extraShifts: Int,
    val base: Double,
    val bonusPool: Double,
    val bonusPerShift: Double,
    val bonus: Double,
    val total: Double,
    /** Мої доби, які закрив хтось інший (моя частка надбавки пішла йому). */
    val coveredForMe: List<Cover>,
    /** Доби, які я закрив за когось. */
    val coveredByMe: List<Cover>,
    val vacationDays: Int,
    val sickDays: Int,
    val totalHours: Double,
    /** false — якщо людина цього місяця не в добовій ротації (напр. робила 8 год). */
    val onRotation: Boolean
) {
    /** Скільки надбавки недодали через відпустку/лікарняний. */
    val bonusLost: Double get() = (bonusPool - bonus).coerceAtLeast(0.0)
}

object Salary {

    fun compute(
        sheet: MonthSheet,
        me: PersonMonth,
        base: Double,
        bonusPool: Double
    ): SalaryBreakdown {
        val myDoba = me.dobaDays
        val daysInMonth = sheet.daysInMonth

        val vacationDays = me.days.count { it.value.kind == Kind.VACATION }
        val sickDays = me.days.count { it.value.kind == Kind.SICK }
        val totalHours = me.days.values.mapNotNull { it.hours }.sum()

        if (myDoba.isEmpty()) {
            return SalaryBreakdown(
                year = sheet.year, month = sheet.month,
                plannedShifts = 0, workedShifts = 0, extraShifts = 0,
                base = base, bonusPool = bonusPool, bonusPerShift = 0.0,
                bonus = 0.0, total = base,
                coveredForMe = emptyList(), coveredByMe = emptyList(),
                vacationDays = vacationDays, sickDays = sickDays,
                totalHours = totalHours, onRotation = false
            )
        }

        val step = deriveStep(myDoba)
        val anchor = myDoba.first()
        val plannedDays = (1..daysInMonth).filter { (it - anchor).mod(step) == 0 }
        val plannedSet = plannedDays.toSet()

        val worked = myDoba.size
        val extra = myDoba.count { it !in plannedSet }

        // Мої планові доби, які я не відробив → дивлюсь, хто стояв на добі того дня.
        val coveredForMe = plannedDays
            .filter { day -> !me.day(day).let { it.shift == ShiftKind.DOBA } }
            .mapNotNull { day ->
                val who = sheet.people.firstOrNull {
                    !it.name.equals(me.name, true) && it.day(day).shift == ShiftKind.DOBA
                }
                who?.let { Cover(day, it.name) }
            }

        // Доби, які я відробив поза своїм графіком → я когось підміняв.
        val coveredByMe = myDoba
            .filter { it !in plannedSet }
            .map { day ->
                val whose = sheet.people.firstOrNull { p ->
                    !p.name.equals(me.name, true) &&
                        p.day(day).kind.let { it == Kind.VACATION || it == Kind.SICK }
                }
                Cover(day, whose?.name ?: "—")
            }

        val plan = plannedDays.size
        val bonusPerShift = if (plan > 0) bonusPool / plan else 0.0
        val bonus = bonusPerShift * worked

        return SalaryBreakdown(
            year = sheet.year, month = sheet.month,
            plannedShifts = plan, workedShifts = worked, extraShifts = extra,
            base = base, bonusPool = bonusPool, bonusPerShift = bonusPerShift,
            bonus = bonus, total = base + bonus,
            coveredForMe = coveredForMe, coveredByMe = coveredByMe,
            vacationDays = vacationDays, sickDays = sickDays,
            totalHours = totalHours, onRotation = true
        )
    }

    private fun deriveStep(doba: List<Int>): Int = Rotation.deriveStep(doba)
}
