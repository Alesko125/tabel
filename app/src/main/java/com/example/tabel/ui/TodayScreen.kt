package com.example.tabel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.tabel.data.Kind
import com.example.tabel.data.MonthNames
import com.example.tabel.data.PersonMonth
import com.example.tabel.data.ShiftKind
import com.example.tabel.data.ShiftTimes
import java.time.LocalDate

@Composable
fun TodayScreen(
    state: UiState,
    onShiftDay: (Long) -> Unit,
    onToday: () -> Unit,
    onSelectPerson: (String) -> Unit,
    onDismissChange: () -> Unit
) {
    val sheet = state.sheet
    val date = state.date
    val myEntry = state.myEntry

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .swipePages(onPrevious = { onShiftDay(-1) }, onNext = { onShiftDay(1) }),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item { DateNavigator(date, onShiftDay, onToday) }
        item { StateBanner(state.loading, state.error.takeIf { !state.isProjection }, state.fromCache) }

        state.scheduleChange?.let { change ->
            item { ScheduleChangeCard(change, onDismissChange) }
        }

        if (sheet == null) {
            item { ProjectionCard(state) }
            return@LazyColumn
        }

        if (state.me == null) {
            item { MissingPersonCard(state.myName) }
        } else {
            item { MyDayCard(myEntry, state.shiftTimes) }
        }

        if (!sheet.isFilledOn(date.dayOfMonth)) {
            item { NotFilledYetCard() }
        }

        val working = sheet.workingOn(date.dayOfMonth, state.myName)
            .sortedWith(
                compareByDescending<Pair<PersonMonth, DayEntry>> { it.second.hours ?: 0.0 }
                    .thenBy { it.first.name }
            )

        val grouped = working.groupBy { it.second.shift }
        for (kind in listOf(ShiftKind.DOBA, ShiftKind.LONG, ShiftKind.DAY, ShiftKind.PARTIAL)) {
            val list = grouped[kind] ?: continue
            item { SectionHeader(groupTitle(kind), list.size.toString()) }
            items(list, key = { it.first.name + kind.name }) { pair ->
                PersonRow(
                    name = pair.first.name,
                    subtitle = Format.shiftLabel(pair.second, state.shiftTimes, pair.first.name),
                    color = shiftColor(pair.second),
                    onClick = { onSelectPerson(pair.first.name) }
                )
            }
        }

        // Дні без забитих годин — за графіком робочі, так що показую окремо.
        val expected = sheet.expectedOn(date.dayOfMonth, state.myName)
            .sortedBy { it.name }

        if (expected.isNotEmpty()) {
            item { SectionHeader("Мають працювати", expected.size.toString()) }
            items(expected, key = { "exp_" + it.name }) { person ->
                PersonRow(
                    name = person.name,
                    subtitle = Format.expectedLabel(
                        state.typicalHoursFor(person),
                        state.shiftTimes,
                        person.name
                    ),
                    color = shiftPalette().expected,
                    onClick = { onSelectPerson(person.name) }
                )
            }
        }

        // Відсутніх теж показую — треба бачити, кого сьогодні нема.
        val absent = sheet.people
            .filter { !it.matches(state.myName) }
            .mapNotNull { person ->
                val entry = person.day(date.dayOfMonth)
                if (entry.kind in ABSENT_KINDS) person to entry else null
            }
            .sortedWith(compareBy({ it.second.kind.ordinal }, { it.first.name }))

        if (absent.isNotEmpty()) {
            item { SectionHeader("Немає на зміні", absent.size.toString()) }
            items(absent, key = { "absent_" + it.first.name }) { pair ->
                PersonRow(
                    name = pair.first.name,
                    subtitle = Format.statusLabel(pair.second),
                    color = MaterialTheme.colorScheme.error,
                    nameColor = MaterialTheme.colorScheme.error,
                    onClick = { onSelectPerson(pair.first.name) }
                )
            }
        }

        if (myEntry.shift == ShiftKind.DOBA) {
            val previous = findDobaOn(state, date.minusDays(1))
            if (previous != null) {
                item {
                    SectionHeader("Зміну здає тобі")
                    PersonRow(
                        previous,
                        handoverAt(date, state.dobaStart),
                        shiftPalette().doba,
                        onClick = { onSelectPerson(previous) }
                    )
                }
            }
            val handover = findDobaOn(state, date.plusDays(1))
            if (handover != null) {
                item {
                    SectionHeader("Зміну приймає")
                    PersonRow(
                        handover,
                        handoverAt(date.plusDays(1), state.dobaStart),
                        shiftPalette().doba,
                        onClick = { onSelectPerson(handover) }
                    )
                }
            }
        }
    }
}

private val ABSENT_KINDS = setOf(Kind.VACATION, Kind.SICK, Kind.UNPAID, Kind.ABSENT, Kind.OFF)

private fun handoverAt(date: LocalDate, hour: Int): String =
    "%d %s о %02d:00".format(date.dayOfMonth, MonthNames.UK_GENITIVE[date.monthValue - 1], hour)

private fun groupTitle(kind: ShiftKind): String = when (kind) {
    ShiftKind.DOBA -> "На добі"
    ShiftKind.LONG -> "Зміна 11 годин"
    ShiftKind.DAY -> "Денна зміна"
    ShiftKind.PARTIAL -> "Неповний день"
    ShiftKind.NONE -> "Інше"
}

private fun findDobaOn(state: UiState, date: LocalDate): String? {
    val sheet = when {
        state.sheet?.contains(date) == true -> state.sheet
        state.nextSheet?.contains(date) == true -> state.nextSheet
        state.prevSheet?.contains(date) == true -> state.prevSheet
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

/** Місяця ще нема в таблиці — показую прогноз ротації. */
@Composable
private fun ProjectionCard(state: UiState) {
    if (!state.isProjection) {
        Text(
            text = "Дані за цей місяць ще не завантажені.",
            modifier = Modifier.padding(vertical = 24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val isDoba = state.rotation?.isDobaOn(state.date) == true
    Column {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDoba) shiftPalette().doba.copy(alpha = shiftPalette().fillAlpha)
            else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(44.dp)
                        .background(
                            if (isDoba) shiftPalette().doba else shiftPalette().off,
                            RoundedCornerShape(3.dp)
                        )
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Прогноз",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isDoba) "Доба · %02d:00 → %02d:00".format(
                            state.dobaStart, state.dobaStart
                        ) else "Вихідний",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Text(
            text = "Цього місяця ще немає в таблиці. Дата порахована з ротації " +
                "«доба через ${state.rotation!!.step - 1}», тож хто буде на зміні — невідомо.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScheduleChangeCard(text: String, onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Графік змінився",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            TextButton(onClick = onDismiss) { Text("Ясно") }
        }
    }
}

@Composable
private fun NotFilledYetCard() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "Фактичні години на цей день ще не проставлені — їх записують " +
                "день у день. Хто має бути на зміні, видно з графіка.",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
        color = if (working) color.copy(alpha = shiftPalette().fillAlpha)
        else MaterialTheme.colorScheme.surfaceVariant,
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
internal fun PersonRow(
    name: String,
    subtitle: String,
    color: Color,
    nameColor: Color? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(color, 12)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = nameColor ?: MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = nameColor?.copy(alpha = 0.8f) ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
