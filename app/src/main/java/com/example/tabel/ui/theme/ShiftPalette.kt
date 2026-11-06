package com.example.tabel.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Кольори типів змін. Світлі відтінки взяв з самого табеля, так що графік
 * у застосунку читається так само, як у таблиці. Для темної теми вони
 * освітлені — пастель на темному тлі не розбереш.
 */
data class ShiftPalette(
    val doba: Color,
    val long: Color,
    val day: Color,
    val partial: Color,
    val expected: Color,
    val off: Color,
    val vacation: Color,
    val sick: Color,
    val none: Color,
    /** Прозорість заливки карток і клітинок календаря. */
    val fillAlpha: Float
)

val LightShiftPalette = ShiftPalette(
    doba = Color(0xFFDD7E6B),
    long = Color(0xFF7FB0DA),
    day = Color(0xFF8FC47A),
    partial = Color(0xFFC98CA9),
    expected = Color(0xFF9FB8C9),
    off = Color(0xFF9AA0A6),
    vacation = Color(0xFFF0A340),
    sick = Color(0xFFDE6C6C),
    none = Color(0xFFBDBDBD),
    fillAlpha = 0.20f
)

val DarkShiftPalette = ShiftPalette(
    doba = Color(0xFFF0937E),
    long = Color(0xFF93BEE4),
    day = Color(0xFF9FD08C),
    partial = Color(0xFFDCA2BE),
    expected = Color(0xFF8FA6B6),
    off = Color(0xFF868C93),
    vacation = Color(0xFFF5BC6A),
    sick = Color(0xFFEF8A8A),
    none = Color(0xFF6E7379),
    fillAlpha = 0.26f
)

val LocalShiftPalette = staticCompositionLocalOf { LightShiftPalette }
