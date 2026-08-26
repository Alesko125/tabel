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

    var dayShiftStartHour: Int
        get() = prefs.getInt(KEY_DAY_START, DEFAULT_DAY_START)
        set(v) = prefs.edit().putInt(KEY_DAY_START, v.coerceIn(0, 23)).apply()

    var themeMode: ThemeMode
        get() = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!)
        }.getOrDefault(ThemeMode.SYSTEM)
        set(v) = prefs.edit().putString(KEY_THEME, v.name).apply()

    companion object {
        const val DEFAULT_NAME = "Кобзар"
        const val DEFAULT_SHEET = "1QJ-OIwoZb-aUXiw2jP0OY1ju3_O9o_4jenNqx9JxglU"
        const val DEFAULT_BASE = 16700.0
        const val DEFAULT_BONUS = 4000.0
        const val DEFAULT_DOBA_START = 9
        const val DEFAULT_DAY_START = 9

        private const val KEY_NAME = "my_name"
        private const val KEY_SHEET = "sheet_id"
        private const val KEY_BASE = "base"
        private const val KEY_BONUS = "bonus"
        private const val KEY_DOBA_START = "doba_start"
        private const val KEY_DAY_START = "day_start"
        private const val KEY_THEME = "theme_mode"
    }
}
