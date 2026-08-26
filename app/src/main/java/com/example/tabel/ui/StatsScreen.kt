package com.example.tabel.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tabel.StatsScope
import com.example.tabel.UiState
import com.example.tabel.data.Format
import com.example.tabel.data.PersonYearStats
import java.time.LocalDate

@Composable
fun StatsScreen(
    state: UiState,
    onLoad: (StatsScope) -> Unit
) {
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
            item {
                Text(
                    text = "Натисни період, щоб порахувати.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
            return@LazyColumn
        }

        val mine = stats.people.firstOrNull { it.name.startsWith(state.myName, true) }
        if (mine != null) {
            item { MyTotals(mine, state) }
        }

        if (stats.partners.isNotEmpty()) {
            item { SectionHeader("Найчастіші напарники") }
            items(stats.partners.take(8), key = { "partner_" + it.name }) { partner ->
                InfoRow(partner.name, "${partner.sharedShifts} змін разом")
            }
        }

        item {
            SectionHeader("Вихідні по людях", "${stats.people.size}")
            Text(
                text = "Порахували ${stats.monthsLoaded} з ${stats.monthsRequested} місяців " +
                    "— решти немає в таблиці.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(stats.people, key = { "stat_" + it.name }) { person ->
            PersonStatsRow(person, isMe = person.name.startsWith(state.myName, true))
        }
    }
}

@Composable
private fun ScopePicker(state: UiState, onLoad: (StatsScope) -> Unit) {
    val thisYear = LocalDate.now().year
    val years = listOf(thisYear, thisYear - 1)

    Column {
        SectionHeader("Період")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (year in years) {
                val selected = state.statsScope == StatsScope.Year(year)
                FilterChip(
                    selected = selected,
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
        Spacer(Modifier.height(8.dp))
    }
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
private fun MyTotals(mine: PersonYearStats, state: UiState) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = "Відпрацьовано",
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

    if (mine.dobaCount > 0) InfoRow("Діб", Format.shifts(mine.dobaCount))
    InfoRow("Робочих днів", Format.days(mine.workedDays))
    InfoRow("Вихідних", Format.days(mine.offDays))
    if (mine.vacationDays > 0) InfoRow("Відпустка", Format.days(mine.vacationDays))
    if (mine.sickDays > 0) InfoRow("Лікарняний", Format.days(mine.sickDays))
    InfoRow("Місяців у табелі", spanText(mine))

    SectionHeader("Вихідні та зміни")
    InfoRow("Вихідних на суботу-неділю", Format.days(state.stats?.weekendsOff ?: 0))
    InfoRow("Змін на суботу-неділю", "${state.stats?.weekendsWorked ?: 0}")
    InfoRow("Найдовша перерва поспіль", Format.days(state.stats?.longestBreak ?: 0))
}

/** 165 годин — приблизна місячна норма при 40-годинному тижні. */
private fun fullTimeEquivalent(hours: Double): String {
    if (hours <= 0) return "немає даних"
    val shifts = (hours / 8).toInt()
    val months = hours / 165.0
    val years = months / 12
    return buildString {
        append("це $shifts змін по 8 годин")
        if (years >= 1) {
            append(" · ${"%.1f".format(years)} року повної зайнятості")
        } else {
            append(" · ${"%.1f".format(months)} місяця повної зайнятості")
        }
    }
}

private fun spanText(mine: PersonYearStats): String {
    val years = mine.spanMonths / 12
    val months = mine.spanMonths % 12
    val span = when {
        years == 0 -> "$months міс."
        months == 0 -> "$years р."
        else -> "$years р. $months міс."
    }
    return "${mine.monthsPresent} з $span"
}

@Composable
private fun PersonStatsRow(person: PersonYearStats, isMe: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
                    if (person.dobaCount > 0) add(Format.shifts(person.dobaCount))
                    add(Format.hours(person.hours))
                    if (person.vacationDays > 0) add("відпустка ${person.vacationDays}")
                    if (person.sickDays > 0) add("б/л ${person.sickDays}")
                }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = Format.days(person.offDays),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
    HorizontalDivider()
}
