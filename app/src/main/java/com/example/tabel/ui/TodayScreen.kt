package com.example.tabel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tabel.UiState
import com.example.tabel.data.DayEntry
import com.example.tabel.data.Format
import com.example.tabel.data.MonthNames
import com.example.tabel.data.PersonMonth
import com.example.tabel.data.ShiftKind
import com.example.tabel.data.ShiftTimes
import java.time.LocalDate

@Composable
fun TodayScreen(
    state: UiState,
    onShiftDay: (Long) -> Unit,
    onToday: () -> Unit
) {
    val sheet = state.sheet
    val date = state.date
    val myEntry = state.myEntry

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item { DateNavigator(date, onShiftDay, onToday) }
        item { StateBanner(state.loading, state.error, state.fromCache) }

        if (sheet == null) {
            item {
                Text(
                    text = "Дані за цей місяць ще не завантажені.",
                    modifier = Modifier.padding(vertical = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@LazyColumn
        }

        if (state.me == null) {
            item { MissingPersonCard(state.myName) }
        } else {
            item { MyDayCard(myEntry, state.shiftTimes) }
        }

        val others = sheet.workingOn(date.dayOfMonth, state.myName)
            .sortedWith(
                compareByDescending<Pair<PersonMonth, DayEntry>> { it.second.hours ?: 0.0 }
                    .thenBy { it.first.name }
            )

        if (others.isEmpty()) {
            item {
                SectionHeader("Разом з тобою")
                Text(
                    text = "Цього дня в табелі більше нікого немає.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            val grouped = others.groupBy { it.second.shift }
            for (kind in listOf(ShiftKind.DOBA, ShiftKind.LONG, ShiftKind.DAY, ShiftKind.PARTIAL)) {
                val list = grouped[kind] ?: continue
                item { SectionHeader(groupTitle(kind), list.size.toString()) }
                items(list, key = { it.first.name + kind.name }) { pair ->
                    PersonRow(
                        name = pair.first.name,
                        subtitle = Format.shiftLabel(pair.second, state.shiftTimes),
                        color = shiftColor(pair.second)
                    )
                }
            }
        }

        if (myEntry.shift == ShiftKind.DOBA) {
            val previous = findDobaOn(state, date.minusDays(1))
            if (previous != null) {
                item {
                    SectionHeader("Зміну здає тобі")
                    PersonRow(previous, handoverAt(date, state.dobaStart), shiftPalette().doba)
                }
            }
            val handover = findDobaOn(state, date.plusDays(1))
            if (handover != null) {
                item {
                    SectionHeader("Зміну приймає")
                    PersonRow(handover, handoverAt(date.plusDays(1), state.dobaStart), shiftPalette().doba)
                }
            }
        }
    }
}

private fun handoverAt(date: LocalDate, hour: Int): String =
    "%d %s о %02d:00".format(date.dayOfMonth, MonthNames.UK_GENITIVE[date.monthValue - 1], hour)

private fun groupTitle(kind: ShiftKind): String = when (kind) {
    ShiftKind.DOBA -> "На добі"
    ShiftKind.LONG -> "Зміна 11 годин"
    ShiftKind.DAY -> "Денна зміна"
    ShiftKind.PARTIAL -> "Неповний день"
    ShiftKind.NONE -> "Інше"
}

/** Хто стоїть на добі вказаного дня. Наступний день може бути вже в іншому місяці. */
private fun findDobaOn(state: UiState, date: LocalDate): String? {
    val sheet = when {
        state.sheet?.contains(date) == true -> state.sheet
        state.nextSheet?.contains(date) == true -> state.nextSheet
        else -> null
    } ?: return null
    return sheet.people.firstOrNull { person ->
        !person.matches(state.myName) && person.day(date.dayOfMonth).shift == ShiftKind.DOBA
    }?.name
}

@Composable
private fun DateNavigator(date: LocalDate, onShiftDay: (Long) -> Unit, onToday: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onShiftDay(-1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Попередній день")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${date.dayOfMonth} ${MonthNames.UK_GENITIVE[date.monthValue - 1]}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = MonthNames.WEEKDAYS_FULL[date.dayOfWeek.value - 1] +
                        if (date.year != LocalDate.now().year) ", ${date.year}" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onShiftDay(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Наступний день")
            }
        }
        if (date != LocalDate.now()) {
            TextButton(onClick = onToday, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Сьогодні")
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun MissingPersonCard(myName: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "У табелі за цей місяць немає рядка «$myName». Перевір прізвище в налаштуваннях.",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun MyDayCard(entry: DayEntry, times: ShiftTimes) {
    val color = shiftColor(entry)
    val working = entry.isWork
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (working) color.copy(alpha = shiftPalette().fillAlpha) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(44.dp)
                    .background(color, RoundedCornerShape(3.dp))
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "Ти",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = (if (working) Format.shiftLabel(entry, times) else Format.statusLabel(entry))
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PersonRow(name: String, subtitle: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(color, 12)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
