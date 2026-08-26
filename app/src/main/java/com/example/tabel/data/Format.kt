package com.example.tabel.data

import java.util.Locale

object Format {

    fun money(v: Double): String =
        String.format(Locale.US, "%,.0f", v).replace(',', ' ') + " грн"

    fun money2(v: Double): String =
        String.format(Locale.US, "%,.2f", v).replace(",", " ") + " грн"

    fun hours(v: Double): String =
        if (v % 1.0 == 0.0) "${v.toInt()} год" else String.format(Locale.US, "%.1f год", v)

    /** «10 діб», «1 доба», «2 доби» */
    fun shifts(n: Int): String {
        val mod100 = n % 100
        val mod10 = n % 10
        val word = when {
            mod100 in 11..14 -> "діб"
            mod10 == 1 -> "доба"
            mod10 in 2..4 -> "доби"
            else -> "діб"
        }
        return "$n $word"
    }

    fun days(n: Int): String {
        val mod100 = n % 100
        val mod10 = n % 10
        val word = when {
            mod100 in 11..14 -> "днів"
            mod10 == 1 -> "день"
            mod10 in 2..4 -> "дні"
            else -> "днів"
        }
        return "$n $word"
    }

    /** Короткий підпис зміни: «доба 09:00 → 09:00», «11 год · 09:00–20:00». */
    fun shiftLabel(entry: DayEntry, settings: ShiftTimes): String = when (entry.shift) {
        ShiftKind.DOBA -> "доба · %02d:00 → %02d:00".format(settings.dobaStart, settings.dobaStart)
        ShiftKind.LONG -> {
            val h = entry.hours?.toInt() ?: 11
            "$h год · %02d:00–%02d:00".format(settings.dayStart, (settings.dayStart + h) % 24)
        }
        ShiftKind.DAY -> {
            val h = entry.hours?.toInt() ?: 8
            // 8 робочих годин + година обіду
            "$h год · %02d:00–%02d:00".format(settings.dayStart, (settings.dayStart + h + 1) % 24)
        }
        ShiftKind.PARTIAL -> entry.hours?.let { hours(it) } ?: entry.raw
        ShiftKind.NONE -> statusLabel(entry)
    }

    fun statusLabel(entry: DayEntry): String = when (entry.kind) {
        Kind.OFF -> "вихідний"
        Kind.VACATION -> "відпустка"
        Kind.SICK -> "лікарняний"
        Kind.UNPAID -> "відпустка без збереження ЗП"
        Kind.ABSENT -> "відсутній"
        Kind.NONE -> "немає в графіку"
        Kind.OTHER -> entry.raw
        Kind.WORK -> entry.hours?.let { hours(it) } ?: entry.raw
    }

    /** Коротка позначка для клітинки календаря. */
    fun cellLabel(entry: DayEntry): String = when (entry.kind) {
        Kind.WORK -> entry.hours?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "•"
        Kind.OFF -> "В"
        Kind.VACATION -> "від"
        Kind.SICK -> "б/л"
        Kind.UNPAID -> "НА"
        Kind.ABSENT -> "Н"
        Kind.OTHER -> entry.raw.take(3)
        Kind.NONE -> ""
    }
}

data class ShiftTimes(val dobaStart: Int, val dayStart: Int)
