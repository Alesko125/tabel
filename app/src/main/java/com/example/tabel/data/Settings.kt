package com.example.tabel.data

import android.content.Context

class Settings(context: Context) {

    private val prefs = context.getSharedPreferences("tabel", Context.MODE_PRIVATE)

    var myName: String
        get() = prefs.getString(KEY_NAME, DEFAULT_NAME) ?: DEFAULT_NAME
        set(v) = prefs.edit().putString(KEY_NAME, v.trim()).apply()

    var sheetId: String
        get() = prefs.getString(KEY_SHEET, DEFAULT_SHEET) ?: DEFAULT_SHEET
        set(v) = prefs.edit().putString(KEY_SHEET, v.trim()).apply()

    /** Ставка на руки за повний місяць, без надбавки. */
    var base: Double
        get() = prefs.getFloat(KEY_BASE, DEFAULT_BASE.toFloat()).toDouble()
        set(v) = prefs.edit().putFloat(KEY_BASE, v.toFloat()).apply()

    /** Надбавка за роботу «доба через дві» замість норми «доба через три». */
    var bonusPool: Double
        get() = prefs.getFloat(KEY_BONUS, DEFAULT_BONUS.toFloat()).toDouble()
        set(v) = prefs.edit().putFloat(KEY_BONUS, v.toFloat()).apply()

    /** Година початку доби (0..23). */
    var dobaStartHour: Int
        get() = prefs.getInt(KEY_DOBA_START, DEFAULT_DOBA_START)
        set(v) = prefs.edit().putInt(KEY_DOBA_START, v.coerceIn(0, 23)).apply()

    /** Година початку денної зміни за замовчуванням. */
    var dayShiftStartHour: Int
        get() = prefs.getInt(KEY_DAY_START, DEFAULT_DAY_START)
        set(v) = prefs.edit().putInt(KEY_DAY_START, v.coerceIn(0, 23)).apply()

    var themeMode: ThemeMode
        get() = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!)
        }.getOrDefault(ThemeMode.SYSTEM)
        set(v) = prefs.edit().putString(KEY_THEME, v.name).apply()

    // ---------- індивідуальний час зміни ----------

    /**
     * Час зміни в табелі не закодований: легенда з варіантами 8:00-17:00,
     * 9:00-18:00 та 11:00-20:00 не має кольорових плашок, а кольори прізвищ
     * групують людей не за часом. Тому початок зміни для окремих людей
     * задається вручну й зберігається тут.
     */
    var personStartHours: Map<String, Int>
        get() = decodeMap(prefs.getString(KEY_PERSON_HOURS, "")).mapNotNull { (k, v) ->
            v.toIntOrNull()?.let { k to it }
        }.toMap()
        set(v) = prefs.edit()
            .putString(KEY_PERSON_HOURS, encodeMap(v.mapValues { it.value.toString() }))
            .apply()

    fun setPersonStartHour(name: String, hour: Int?) {
        val map = personStartHours.toMutableMap()
        if (hour == null) map.remove(name) else map[name] = hour.coerceIn(0, 23)
        personStartHours = map
    }

    // ---------- сповіщення ----------

    var notifyShift: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_SHIFT, true)
        set(v) = prefs.edit().putBoolean(KEY_NOTIFY_SHIFT, v).apply()

    var notifyChanges: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_CHANGES, true)
        set(v) = prefs.edit().putBoolean(KEY_NOTIFY_CHANGES, v).apply()

    /** Година нагадування про добу. */
    var reminderHour: Int
        get() = prefs.getInt(KEY_REMINDER_HOUR, DEFAULT_REMINDER_HOUR)
        set(v) = prefs.edit().putInt(KEY_REMINDER_HOUR, v.coerceIn(0, 23)).apply()

    /** true — нагадувати вранці в день зміни, false — напередодні ввечері. */
    var remindOnShiftDay: Boolean
        get() = prefs.getBoolean(KEY_REMIND_DAY_OF, true)
        set(v) = prefs.edit().putBoolean(KEY_REMIND_DAY_OF, v).apply()

    /** Дата, за яку вже надіслано нагадування — щоб не повторювати. */
    var lastReminderDate: String
        get() = prefs.getString(KEY_LAST_REMINDER, "") ?: ""
        set(v) = prefs.edit().putString(KEY_LAST_REMINDER, v).apply()

    // ---------- відстеження змін у графіку ----------

    /** Знімок моїх діб за місяцем: «2026-8» → «2,5,8,…». */
    fun snapshot(year: Int, month: Int): String =
        prefs.getString(snapshotKey(year, month), "") ?: ""

    fun saveSnapshot(year: Int, month: Int, days: List<Int>) {
        prefs.edit().putString(snapshotKey(year, month), days.joinToString(",")).apply()
    }

    private fun snapshotKey(year: Int, month: Int) = "snap_${year}_$month"

    // ---------- ротація для прогнозу ----------

    /** Остання відома доба (epochDay) і крок ротації — для прогнозу поза таблицею. */
    var rotationAnchor: Long
        get() = prefs.getLong(KEY_ROT_ANCHOR, 0L)
        set(v) = prefs.edit().putLong(KEY_ROT_ANCHOR, v).apply()

    var rotationStep: Int
        get() = prefs.getInt(KEY_ROT_STEP, 0)
        set(v) = prefs.edit().putInt(KEY_ROT_STEP, v).apply()

    // ---------- фактична зарплата ----------

    /** Скільки прийшло насправді: «2026-8» → сума. */
    fun actualPay(year: Int, month: Int): Double? {
        val v = prefs.getFloat(payKey(year, month), -1f)
        return if (v < 0) null else v.toDouble()
    }

    fun setActualPay(year: Int, month: Int, value: Double?) {
        val editor = prefs.edit()
        if (value == null) editor.remove(payKey(year, month))
        else editor.putFloat(payKey(year, month), value.toFloat())
        editor.apply()
    }

    private fun payKey(year: Int, month: Int) = "pay_${year}_$month"

    // ---------- службове ----------
    // Пари «прізвище=година» зберігаємо рядками: у прізвищах бувають крапки,
    // апострофи й пробіли, але ніколи — знак рівності чи перенесення рядка.

    private fun encodeMap(map: Map<String, String>): String =
        map.entries.joinToString("\n") { it.key + "=" + it.value }

    private fun decodeMap(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.lineSequence().mapNotNull { line ->
            val i = line.lastIndexOf('=')
            if (i <= 0) null else line.substring(0, i) to line.substring(i + 1)
        }.toMap()
    }

    companion object {
        const val DEFAULT_NAME = "Кобзар"
        const val DEFAULT_SHEET = "1QJ-OIwoZb-aUXiw2jP0OY1ju3_O9o_4jenNqx9JxglU"
        const val DEFAULT_BASE = 16700.0
        const val DEFAULT_BONUS = 4000.0
        const val DEFAULT_DOBA_START = 8
        const val DEFAULT_DAY_START = 9
        const val DEFAULT_REMINDER_HOUR = 7

        private const val KEY_NAME = "my_name"
        private const val KEY_SHEET = "sheet_id"
        private const val KEY_BASE = "base"
        private const val KEY_BONUS = "bonus"
        private const val KEY_DOBA_START = "doba_start"
        private const val KEY_DAY_START = "day_start"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_PERSON_HOURS = "person_hours"
        private const val KEY_NOTIFY_SHIFT = "notify_shift"
        private const val KEY_NOTIFY_CHANGES = "notify_changes"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMIND_DAY_OF = "remind_day_of"
        private const val KEY_LAST_REMINDER = "last_reminder"
        private const val KEY_ROT_ANCHOR = "rotation_anchor"
        private const val KEY_ROT_STEP = "rotation_step"
    }
}
