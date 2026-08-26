package com.example.tabel

import com.example.tabel.data.Kind
import com.example.tabel.data.MonthSheet
import com.example.tabel.data.ShiftKind
import com.example.tabel.data.SheetParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Фікстури — справжній CSV-експорт аркушів із таблиці табеля. */
internal object Fixtures {
    fun load(name: String, year: Int, month: Int): MonthSheet {
        val csv = Fixtures::class.java.getResourceAsStream("/$name.csv")!!
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        return SheetParser.parse(csv, year, month)
    }
}

class SheetParserTest {

    @Test
    fun `знаходить усіх людей у серпні 2026`() {
        val sheet = Fixtures.load("aug2026", 2026, 8)
        val names = sheet.people.map { it.name }

        assertTrue("Кобзар має бути в табелі", names.any { it.startsWith("Кобзар") })
        assertTrue("Уколов має бути в табелі", names.any { it.startsWith("Уколов") })
        assertTrue("Шелепа має бути в табелі", names.any { it.startsWith("Шелепа") })

        // 19 людей у видимих блоках серпня 2026.
        assertEquals(19, sheet.people.size)

        // Службові рядки не мають потрапити в список людей.
        assertTrue(names.none { it.contains("Табельник") })
        assertTrue(names.none { it.contains("Прізвище") })
    }

    @Test
    fun `графік Кобзаря — доба через дві`() {
        val sheet = Fixtures.load("aug2026", 2026, 8)
        val me = sheet.person("Кобзар")
        assertNotNull(me)
        assertEquals(listOf(2, 5, 8, 11, 14, 17, 20, 23, 26, 29), me!!.dobaDays)
        assertEquals(ShiftKind.DOBA, me.day(2).shift)
        assertEquals(24.0, me.day(2).hours!!, 0.001)
        // Між добами в табелі порожньо — для чергового це вихідний.
        assertEquals(Kind.OFF, me.day(3).kind)
    }

    @Test
    fun `у чергового порожня клітинка означає вихідний`() {
        val sheet = Fixtures.load("aug2026", 2026, 8)
        val me = sheet.person("Кобзар")!!

        assertTrue(me.isOnDuty)
        // 26 серпня — доба, 27-го й 28-го в табелі порожньо, тобто вихідні.
        assertEquals(ShiftKind.DOBA, me.day(26).shift)
        assertEquals(Kind.OFF, me.day(27).kind)
        assertEquals(Kind.OFF, me.day(28).kind)
        // Кінець місяця після останньої доби 29-го — теж вихідні, а не «немає в графіку».
        assertEquals(Kind.OFF, me.day(31).kind)
    }

    @Test
    fun `у не-чергового порожня клітинка означає робочий день за графіком`() {
        val sheet = Fixtures.load("aug2026", 2026, 8)
        val zinchenko = sheet.person("Зiнченко")!!

        assertTrue(!zinchenko.isOnDuty)
        // Вихідні в нього проставлені літерою «В» наперед…
        assertEquals(Kind.OFF, zinchenko.day(1).kind)
        // …а порожня клітинка — робочий день, якому ще не записали години.
        assertEquals(Kind.EXPECTED, zinchenko.day(27).kind)
        // У сирому вигляді там і далі порожньо.
        assertEquals(Kind.NONE, zinchenko.rawDay(27).kind)
    }

    @Test
    fun `звичну тривалість дня видно з заповнених клітинок`() {
        val sheet = Fixtures.load("aug2026", 2026, 8)

        assertEquals(8.0, sheet.person("Зiнченко")!!.typicalHours!!, 0.001)
        assertEquals(11.0, sheet.person("Горбенко")!!.typicalHours!!, 0.001)
        assertEquals(24.0, sheet.person("Кобзар")!!.typicalHours!!, 0.001)
    }

    @Test
    fun `незаповнені дні табеля відрізняються від заповнених`() {
        val sheet = Fixtures.load("aug2026", 2026, 8)

        // Табель серпня заповнений по 26-те, далі у 8-годинних порожньо.
        assertTrue(sheet.isFilledOn(26))
        assertTrue(!sheet.isFilledOn(27))
    }

    @Test
    fun `відпустка чергового не плутається з вихідним`() {
        val sheet = Fixtures.load("jul2026", 2026, 7)
        val me = sheet.person("Кобзар")!!

        assertEquals(Kind.VACATION, me.day(10).kind)
        assertEquals(Kind.OFF, me.day(4).kind)
        assertEquals(ShiftKind.DOBA, me.day(3).shift)
    }

    @Test
    fun `розпізнає відпустку і лікарняний`() {
        val sheet = Fixtures.load("aug2026", 2026, 8)
        val gorbenko = sheet.person("Горбенко")!!
        assertEquals(Kind.VACATION, gorbenko.day(6).kind)

        val kornyshova = sheet.person("Корнишова")!!
        assertEquals(Kind.SICK, kornyshova.day(1).kind)

        val zinchenko = sheet.person("Зiнченко")!!
        assertEquals(Kind.OFF, zinchenko.day(1).kind)
    }

    @Test
    fun `хто працює разом із Кобзарем 2 серпня`() {
        val sheet = Fixtures.load("aug2026", 2026, 8)
        val together = sheet.workingOn(2, "Кобзар").map { it.first.name }.sorted()

        assertEquals(
            listOf("Бомбер К.", "Горбенко О.", "Морозова О.", "Ничипоренко Ю."),
            together
        )
    }

    @Test
    fun `11-годинна зміна розпізнається окремо від денної`() {
        val sheet = Fixtures.load("aug2026", 2026, 8)
        assertEquals(ShiftKind.LONG, sheet.person("Горбенко")!!.day(2).shift)
        assertEquals(ShiftKind.DAY, sheet.person("Бомбер")!!.day(2).shift)
    }

    @Test
    fun `пробіли в кінці прізвища прибираються`() {
        val sheet = Fixtures.load("aug2026", 2026, 8)
        // У таблиці записано «Ніколаєнко В. » із пробілом у кінці.
        assertNotNull(sheet.people.firstOrNull { it.name == "Ніколаєнко В." })
    }
}
