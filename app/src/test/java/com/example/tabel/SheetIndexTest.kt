package com.example.tabel

import com.example.tabel.data.MonthNames
import com.example.tabel.data.SheetIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetIndexTest {

    private val html: String by lazy {
        SheetIndexTest::class.java.getResourceAsStream("/htmlview.html")!!
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    @Test
    fun `знаходить усі аркуші зі сторінки htmlview`() {
        val index = SheetIndex.parse(html)
        assertTrue("Мало бути понад 150 аркушів, знайдено ${index.size}", index.size > 150)
    }

    @Test
    fun `зіставляє місяць з gid`() {
        val index = SheetIndex.parse(html)
        assertEquals("2107932632", index[MonthNames.sheetKey(2026, 8)])
        assertEquals("1335216778", index[MonthNames.sheetKey(2026, 7)])
        assertEquals("520514574", index[MonthNames.sheetKey(2026, 1)])
    }

    @Test
    fun `нормалізація прибирає різнобій у назвах аркушів`() {
        val index = SheetIndex.parse(html)

        // «Листопад 2024» — з великої літери.
        assertTrue(index.containsKey(MonthNames.sheetKey(2024, 11)))
        // «травень 2024 » — з пробілом у кінці.
        assertTrue(index.containsKey(MonthNames.sheetKey(2024, 5)))
        // «січень  2024» — з подвійним пробілом.
        assertTrue(index.containsKey(MonthNames.sheetKey(2024, 1)))
    }

    @Test
    fun `кеш перелiку аркушiв переживає серіалізацію`() {
        val index = SheetIndex.parse(html)
        val restored = SheetIndex.deserialize(SheetIndex.serialize(index))
        assertEquals(index, restored)
    }
}
