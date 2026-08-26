package com.example.tabel.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tabel.UiState
import com.example.tabel.data.Format
import com.example.tabel.data.Kind
import com.example.tabel.data.MonthNames
import com.example.tabel.data.PersonMonth
import com.example.tabel.data.ShiftKind

@Composable
fun PeopleScreen(state: UiState, onSelectPerson: (String) -> Unit) {
    val sheet = state.sheet

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = MonthNames.title(state.date.year, state.date.monthValue),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            StateBanner(state.loading, state.error, state.fromCache)
        }

        if (sheet == null) {
            item {
                Text(
                    text = "Списку людей за цей місяць немає — його ще не внесли в таблицю.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
            return@LazyColumn
        }

        val byBlock = sheet.people.groupBy { it.block }
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
                    onClick = { onSelectPerson(person.name) }
                )
            }
        }
    }
}

@Composable
private fun PersonSummaryRow(
    person: PersonMonth,
    isMe: Boolean,
    overrideHour: Int?,
    onClick: () -> Unit
) {
    val doba = person.dobaDays.size
    val hours = person.days.values.mapNotNull { it.hours }.sum()
    val vacation = person.days.count { it.value.kind == Kind.VACATION }
    val longShifts = person.days.values.count { it.shift == ShiftKind.LONG }

    val summary = buildList {
        if (doba > 0) add(Format.shifts(doba))
        if (longShifts > 0) add("$longShifts × 11 год")
        if (hours > 0) add(Format.hours(hours))
        if (vacation > 0) add("відпустка ${Format.days(vacation)}")
        if (overrideHour != null) add("з %02d:00".format(overrideHour))
    }.joinToString(" · ")

    val color = when {
        doba > 0 -> shiftPalette().doba
        longShifts > 0 -> shiftPalette().long
        hours > 0 -> shiftPalette().day
        else -> shiftPalette().off
    }

    PersonRow(
        name = if (isMe) "${person.name} — це ти" else person.name,
        subtitle = summary.ifEmpty { "немає даних за місяць" },
        color = color,
        onClick = onClick
    )
}
