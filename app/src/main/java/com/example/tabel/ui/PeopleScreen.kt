package com.example.tabel.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tabel.UiState
import com.example.tabel.data.Format
import com.example.tabel.data.Kind
import com.example.tabel.data.MonthNames
import com.example.tabel.data.MonthSheet
import com.example.tabel.data.PersonMonth
import com.example.tabel.data.ShiftKind

@Composable
fun PeopleScreen(
    state: UiState,
    onShiftMonth: (Long) -> Unit,
    onSelectPerson: (String) -> Unit
) {
    // Якщо місяця ще немає в таблиці, показуємо склад із попереднього —
    // люди навряд чи зміняться, а для чергових графік можна порахувати.
    val sheet = state.sheet
    val fallback = if (sheet == null) state.prevSheet else null
    val shown = sheet ?: fallback

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .swipePages(onPrevious = { onShiftMonth(-1) }, onNext = { onShiftMonth(1) }),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            MonthNavigator(state, onShiftMonth)
            // Відсутній аркуш — не помилка, якщо є що показати замість нього.
            StateBanner(state.loading, state.error.takeIf { shown == null }, state.fromCache)
        }

        if (shown == null) {
            item {
                Text(
                    text = "Складу за цей місяць немає — ні в таблиці, ні в сусідніх місяцях.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
            return@LazyColumn
        }

        if (fallback != null) {
            item { ForecastNote(state, fallback) }
        }

        val byBlock = shown.people.groupBy { it.block }
        for ((block, people) in byBlock.toSortedMap()) {
            item {
                SectionHeader(
                    if (byBlock.size > 1) "Група ${block + 1}" else "Усі",
                    people.size.toString()
                )
            }
            items(people, key = { "p_${it.block}_${it.name}" }) { person ->
                PersonSummaryRow(
                    person = person,
                    isMe = person.matches(state.myName),
                    overrideHour = state.personStartHours[person.name],
                    projectedDoba = if (fallback == null) null
                    else state.rotation
                        ?.takeIf { person.isOnDuty && person.matches(state.myName) }
                        ?.dobaDaysIn(state.date.year, state.date.monthValue)?.size,
                    isForecast = fallback != null,
                    onClick = { onSelectPerson(person.name) }
                )
            }
        }
    }
}

@Composable
private fun MonthNavigator(state: UiState, onShiftMonth: (Long) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onShiftMonth(-1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Попередній місяць")
        }
        Text(
            text = MonthNames.title(state.date.year, state.date.monthValue),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = { onShiftMonth(1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Наступний місяць")
        }
    }
}

@Composable
private fun ForecastNote(state: UiState, fallback: MonthSheet) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = "Цього місяця ще немає в таблиці. Показано склад за " +
                "${MonthNames.title(fallback.year, fallback.month).lowercase()} — " +
                "хто саме і в які дні працюватиме, стане відомо, коли табельник " +
                "заведе аркуш. Порахувати наперед можна лише твої доби: " +
                "${Format.shifts(state.myDobaDays.size)} за ротацією.",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PersonSummaryRow(
    person: PersonMonth,
    isMe: Boolean,
    overrideHour: Int?,
    projectedDoba: Int?,
    isForecast: Boolean,
    onClick: () -> Unit
) {
    val doba = person.dobaDays.size
    val hours = person.days.values.mapNotNull { it.hours }.sum()
    val vacation = person.days.count { it.value.kind == Kind.VACATION }
    val longShifts = person.days.values.count { it.shift == ShiftKind.LONG }
    val expected = (1..31).count { person.day(it).kind == Kind.EXPECTED }

    val summary = when {
        projectedDoba != null -> "${Format.shifts(projectedDoba)} за прогнозом"
        isForecast -> "склад за минулий місяць"
        else -> buildList {
            if (doba > 0) add(Format.shifts(doba))
            if (longShifts > 0) add("$longShifts × 11 год")
            if (hours > 0) add(Format.hours(hours))
            if (expected > 0) add("$expected дн. без годин")
            if (vacation > 0) add("відпустка ${Format.days(vacation)}")
            if (overrideHour != null) add("з %02d:00".format(overrideHour))
        }.joinToString(" · ")
    }

    val color = when {
        doba > 0 -> shiftPalette().doba
        longShifts > 0 -> shiftPalette().long
        hours > 0 -> shiftPalette().day
        expected > 0 -> shiftPalette().expected
        else -> shiftPalette().off
    }

    PersonRow(
        name = if (isMe) "${person.name} — це ти" else person.name,
        subtitle = summary.ifEmpty { "немає даних за місяць" },
        color = color,
        onClick = onClick
    )
}
