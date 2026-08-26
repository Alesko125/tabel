package com.example.tabel.data

/**
 * Перелік аркушів таблиці: «нормалізована назва місяця → gid».
 *
 * Google не дає списку аркушів через відкритий API, але сторінка /htmlview
 * містить його у вигляді викликів items.push({name: …, gid: …}).
 * Це єдиний надійний спосіб дістати gid, бо назви аркушів у таблиці
 * записані по-різному: «серпень 2026», «Листопад 2024», «травень 2024 ».
 */
object SheetIndex {

    private val ITEM = Regex("""items\.push\(\{name: "(.*?)", pageUrl: .*?gid: "(\d+)"""")

    fun parse(html: String): Map<String, String> =
        ITEM.findAll(html).associate { m ->
            MonthNames.normalize(m.groupValues[1]) to m.groupValues[2]
        }

    fun serialize(map: Map<String, String>): String =
        map.entries.joinToString("\n") { "${it.key}\t${it.value}" }

    fun deserialize(text: String): Map<String, String> =
        text.lineSequence()
            .mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size == 2 && parts[0].isNotBlank()) parts[0] to parts[1] else null
            }
            .toMap()
}
