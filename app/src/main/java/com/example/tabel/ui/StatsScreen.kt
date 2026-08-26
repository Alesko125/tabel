package com.example.tabel.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tabel.StatsScope
import com.example.tabel.UiState
import com.example.tabel.data.Format
import com.example.tabel.data.PersonYearStats
import com.example.tabel.data.StatsResult
import java.time.LocalDate

/** За чим сортувати таблицю по людях. */
private enum class SortBy(val title: String) {
    OFF("Вихідні"),
    HOURS("Години"),
    VACATION("Відпустка"),
    NAME("Абетка")
}

@Composable
fun StatsScreen(state: UiState, onLoad: (StatsScope) -> Unit) {
    var sortBy by remember { mutableStateOf(SortBy.OFF) }

    LaunchedEffect(Unit) {
        if (state.stats == null && !state.statsLoading) onLoad(state.statsScope)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item { ScopePicker(state, onLoad) }

        if (state.statsLoading) {
            item { LoadingBlock(state) }
            return@LazyColumn
        }

        val stats = state.stats
        if (stats == null) {
            item { EmptyHint() }
            return@LazyColumn
        }

        val mine = stats.people.firstOrNull { it.name.startsWith(state.myName, true) }

        if (mine != null) {
            item { HeadlineCard(mine) }
            item { MyTiles(mine) }
            item { MyDetails(mine, stats) }
        }

        if (stats.partners.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader("З ким працюєш найчастіше")
            }
            items(stats.partners.take(8), key = { "partner_" + it.name }) { partner ->
                InfoRow(partner.name, "${partner.sharedShifts} змін")
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            SectionHeader("Колектив", "${stats.people.size} осіб")
            SortPicker(sortBy) { sortBy = it }
            Spacer(Modifier.height(4.dp))
            PeopleTableHeader(sortBy)
        }

        items(sorted(stats.people, sortBy), key = { "stat_" + it.name }) { person ->
            PersonStatsRow(person, sortBy, isMe = person.name.startsWith(state.myName, true))
        }

        item { CoverageNote(stats) }
    }
}

private fun sorted(people: List<PersonYearStats>, sortBy: SortBy): List<PersonYearStats> =
    when (sortBy) {
        SortBy.OFF -> people.sortedByDescending { it.offDays }
        SortBy.HOURS -> people.sortedByDescending { it.hours }
        SortBy.VACATION -> people.sortedByDescending { it.vacationDays }
        SortBy.NAME -> people.sortedBy { it.name }
    }

@Composable
private fun ScopePicker(state: UiState, onLoad: (StatsScope) -> Unit) {
    val thisYear = LocalDate.now().year
    Column {
        SectionHeader("Період")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (year in listOf(thisYear, thisYear - 1)) {
                FilterChip(
                    selected = state.statsScope == StatsScope.Year(year),
                    onClick = { onLoad(StatsScope.Year(year)) },
                    label = { Text(year.toString()) }
                )
            }
            FilterChip(
                selected = state.statsScope == StatsScope.AllTime,
                onClick = { onLoad(StatsScope.AllTime) },
                label = { Text("За весь час") }
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SortPicker(current: SortBy, onSelect: (SortBy) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (option in SortBy.entries) {
            FilterChip(
                selected = current == option,
                onClick = { onSelect(option) },
                label = { Text(option.title, style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}

@Composable
private fun EmptyHint() {
    Text(
        text = "Обери період, щоб порахувати.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 24.dp)
    )
}

@Composable
private fun LoadingBlock(state: UiState) {
    Column(Modifier.padding(vertical = 24.dp)) {
        if (state.statsTotal > 0) {
            LinearProgressIndicator(
                progress = { state.statsProgress.toFloat() / state.statsTotal },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Читаю табель: ${state.statsProgress} з ${state.statsTotal} місяців. " +
                "Перший раз це довго, далі беруться з пам'яті телефона.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HeadlineCard(mine: PersonYearStats) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = "Ти відпрацював",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = Format.hours(mine.hours),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = fullTimeEquivalent(mine.hours),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/** Головні числа плитками — так їх видно з першого погляду, а не списком. */
@Composable
private fun MyTiles(mine: PersonYearStats) {
    val tiles = buildList {
        if (mine.dobaCount > 0) add("Діб" to mine.dobaCount.toString())
        add("Робочих днів" to mine.workedDays.toString())
        add("Вихідних" to mine.offDays.toString())
        if (mine.vacationDays > 0) add("Відпустка" to "${mine.vacationDays} дн.")
        if (mine.sickDays > 0) add("Лікарняний" to "${mine.sickDays} дн.")
        if (mine.expectedDays > 0) add("Ще не проставлено" to "${mine.expectedDays} дн.")
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in tiles.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((label, value) in row) {
                    StatTile(label, value, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 14.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MyDetails(mine: PersonYearStats, stats: StatsResult) {
    Spacer(Modifier.height(8.dp))
    SectionHeader("Як лягли вихідні")
    InfoRow("Вихідних на суботу-неділю", "${stats.weekendsOff} з ${mine.offDays}")
    InfoRow("Змін на суботу-неділю", "${stats.weekendsWorked} з ${mine.workedDays}")
    InfoRow("Найдовша перерва поспіль", Format.days(stats.longestBreak))

    Spacer(Modifier.height(8.dp))
    SectionHeader("Стаж у табелі")
    InfoRow("Місяців із записами", "${mine.monthsPresent}")
    InfoRow("Період", spanText(mine))
}

/** 165 годин — приблизна місячна норма при 40-годинному тижні. */
private fun fullTimeEquivalent(hours: Double): String {
    if (hours <= 0) return "немає даних"
    val shifts = (hours / 8).toInt()
    val months = hours / 165.0
    return if (months >= 12) {
        "це $shifts змін по 8 годин · ${"%.1f".format(months / 12)} року повної зайнятості"
    } else {
        "це $shifts змін по 8 годин · ${"%.1f".format(months)} місяця повної зайнятості"
    }
}

private fun spanText(mine: PersonYearStats): String {
    val years = mine.spanMonths / 12
    val months = mine.spanMonths % 12
    return when {
        years == 0 -> "$months міс."
        months == 0 -> "$years р."
        else -> "$years р. $months міс."
    }
}

@Composable
private fun PeopleTableHeader(sortBy: SortBy) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "Прізвище",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = sortBy.title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(90.dp)
        )
    }
    HorizontalDivider()
}

@Composable
private fun PersonStatsRow(person: PersonYearStats, sortBy: SortBy, isMe: Boolean) {
    val highlighted = when (sortBy) {
        SortBy.OFF -> Format.days(person.offDays)
        SortBy.HOURS -> Format.hours(person.hours)
        SortBy.VACATION -> Format.days(person.vacationDays)
        SortBy.NAME -> Format.hours(person.hours)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isMe) "${person.name} — це ти" else person.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isMe) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = buildList {
                    if (person.dobaCount > 0) add("${person.dobaCount} діб")
                    add("${person.workedDays} роб.")
                    add("${person.offDays} вих.")
                    if (person.vacationDays > 0) add("${person.vacationDays} відп.")
                    if (person.sickDays > 0) add("${person.sickDays} б/л")
                }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = highlighted,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(90.dp)
        )
    }
    HorizontalDivider()
}

@Composable
private fun CoverageNote(stats: StatsResult) {
    Spacer(Modifier.height(12.dp))
    Text(
        text = "Пораховано ${stats.monthsLoaded} з ${stats.monthsRequested} місяців — " +
            "решти в таблиці немає. Години рахуються лише з проставлених у табелі; " +
            "дні, яким ще не записали фактичний прихід, ідуть окремо.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
