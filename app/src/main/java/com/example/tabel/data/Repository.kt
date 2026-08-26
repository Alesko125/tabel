package com.example.tabel.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class LoadResult(
    val sheet: MonthSheet?,
    val fromCache: Boolean,
    val error: String? = null
)

/**
 * Тягне табель напряму з Google Sheets і кешує на диску,
 * щоб графік був доступний і без інтернету.
 */
class Repository(private val context: Context, private val settings: Settings) {

    private val cacheDir: File get() = File(context.filesDir, "tabel").apply { mkdirs() }

    private fun indexFile() = File(cacheDir, "index_${settings.sheetId}.tsv")
    private fun monthFile(y: Int, m: Int) = File(cacheDir, "m_${settings.sheetId}_${y}_$m.csv")

    // ---------- перелік аркушів ----------

    private suspend fun sheetIndex(forceRefresh: Boolean): Map<String, String> =
        withContext(Dispatchers.IO) {
            val cached = indexFile()
            if (!forceRefresh && cached.exists() && cached.length() > 0) {
                return@withContext SheetIndex.deserialize(cached.readText())
            }
            try {
                val html = httpGet("https://docs.google.com/spreadsheets/d/${settings.sheetId}/htmlview")
                val map = SheetIndex.parse(html)
                if (map.isNotEmpty()) cached.writeText(SheetIndex.serialize(map))
                map
            } catch (e: Exception) {
                if (cached.exists()) SheetIndex.deserialize(cached.readText()) else emptyMap()
            }
        }

    // ---------- місяць ----------

    suspend fun loadMonth(year: Int, month: Int, forceRefresh: Boolean = false): LoadResult =
        withContext(Dispatchers.IO) {
            val file = monthFile(year, month)

            if (!forceRefresh && file.exists() && file.length() > 0) {
                parseCached(file, year, month)?.let { return@withContext LoadResult(it, fromCache = true) }
            }

            val gid = sheetIndex(forceRefresh)[MonthNames.sheetKey(year, month)]
                ?: return@withContext fallback(
                    file, year, month,
                    "Аркуш «${MonthNames.title(year, month)}» не знайдено в таблиці"
                )

            try {
                val csv = httpGet(
                    "https://docs.google.com/spreadsheets/d/${settings.sheetId}/export?format=csv&gid=$gid"
                )
                if (csv.isBlank()) return@withContext fallback(file, year, month, "Порожня відповідь від Google")
                val sheet = SheetParser.parse(csv, year, month)
                if (sheet.people.isEmpty()) {
                    return@withContext fallback(file, year, month, "Не вдалося розібрати аркуш")
                }
                file.writeText(csv)
                LoadResult(sheet, fromCache = false)
            } catch (e: Exception) {
                fallback(file, year, month, e.message ?: "Немає зв'язку")
            }
        }

    private fun parseCached(file: File, year: Int, month: Int): MonthSheet? =
        runCatching { SheetParser.parse(file.readText(), year, month) }
            .getOrNull()
            ?.takeIf { it.people.isNotEmpty() }

    private fun fallback(file: File, year: Int, month: Int, error: String): LoadResult {
        if (file.exists() && file.length() > 0) {
            parseCached(file, year, month)?.let {
                return LoadResult(it, fromCache = true, error = error)
            }
        }
        return LoadResult(null, fromCache = false, error = error)
    }

    // ---------- HTTP ----------

    private fun httpGet(url: String, redirects: Int = 0): String {
        if (redirects > 5) throw IllegalStateException("Забагато перенаправлень")
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android) TabelApp")
        }
        try {
            val code = conn.responseCode
            if (code in 300..399) {
                val next = conn.getHeaderField("Location")
                    ?: throw IllegalStateException("Перенаправлення без адреси")
                conn.disconnect()
                return httpGet(URL(URL(url), next).toString(), redirects + 1)
            }
            if (code != 200) throw IllegalStateException("Google повернув HTTP $code")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
