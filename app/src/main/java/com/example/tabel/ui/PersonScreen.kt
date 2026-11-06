package com.example.tabel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tabel.UiState
import com.example.tabel.data.DayEntry
import com.example.tabel.data.Format
import com.example.tabel.data.Kind
import com.example.tabel.data.MonthNames
import com.example.tabel.data.ShiftKind
import java.time.YearMonth

@Composable
fun PersonScreen(
    state: UiState,
    onSetStartHour: (String, Int?) -> Unit
) {
    val person = state.person
    val me = state.me

    if (person == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Цієї людини немає в табелі за ${MonthNames.title(state.date.year, state.date.monthValue)}.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val ym = YearMonth.of(state.date.year, state.date.monthValue)
    val shared = if (me == null) emptyList() else (1..ym.lengthOfMonth()).filter { day ->
        me.day(day).isWork && person.day(day).isWork
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Прізвище вже зверху в панелі, тут лишаю тільки місяць.
        Text(
            text = MonthNames.title(state.date.year, state.date.monthValue),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(12.dp))
        PersonCalendar(state, ym)

        Spacer(Modifier.height(16.dp))
        SectionHeader("Місяць")
        val doba = person.dobaDays.size
        val hours = person.days.values.mapNotNull { it.hours }.sum()
        if (doba > 0) InfoRow("Доби", Format.shifts(doba))
        InfoRow("Усього годин", Format.hours(hours))
        val vacation = person.days.count { it.value.kind == Kind.VACATION }
        val sick = person.days.count { it.value.kind == Kind.SICK }
        if (vacation > 0) InfoRow("Відпустка", Format.days(vacation))
        if (sick > 0) InfoRow("Лікарняний", Format.days(sick))

        if (me != null && !person.matches(state.myName)) {
            SectionHeader("Разом із тобою", shared.size.toString())
            if (shared.isEmpty()) {
                Text(
                    text = "Цього місяця ваші зміни не перетинаються.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = shared.joinToString(", ") +
                        " ${MonthNames.UK_GENITIVE[state.date.monthValue - 1]}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        StartHourPicker(
            name = person.name,
            current = state.personStartHours[person.name],
            fallback = state.dayStart,
            onSet = onSetStartHour
        )
    }
}

@Composable
private fun PersonCalendar(state: UiState, ym: YearMonth) {
    val person = state.person ?: return

    Row(modifier = Modifier.fillMaxWidth()) {
        for (wd in MonthNames.WEEKDAYS) {
            Text(
                text = wd,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(Modifier.height(6.dp))

    val leading = ym.atDay(1).dayOfWeek.value - 1
    val weeks = (leading + ym.lengthOfMonth() + 6) / 7
    var day = 1

    for (w in 0 until weeks) {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (i in 0 until 7) {
                val index = w * 7 + i
                if (index < leading || day > ym.lengthOfMonth()) {
                    Spacer(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    )
                } else {
                    val d = day++
                    val entry = person.day(d)
                    val sharedWithMe = state.me?.day(d)?.isWork == true && entry.isWork
                    DayChip(d, entry, sharedWithMe, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DayChip(
    day: Int,
    entry: DayEntry,
    shared: Boolean,
    modifier: Modifier = Modifier
) {
    val color = shiftColor(entry)
    val filled = entry.kind != Kind.NONE && entry.kind != Kind.OFF

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (filled) color.copy(alpha = shiftPalette().fillAlpha)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.labelMedium,
                // Спільні зміни жирним — це головне, за чим сюди й заходять.
                fontWeight = if (shared) FontWeight.Bold else FontWeight.Normal,
                color = if (shared) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
            val label = Format.cellLabel(entry)
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (entry.shift == ShiftKind.DOBA) FontWeight.Bold
                    else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun StartHourPicker(
    name: String,
    current: Int?,
    fallback: Int,
    onSet: (String, Int?) -> Unit
) {
    var text by remember(name) { mutableStateOf(current?.toString() ?: "") }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = "Початок зміни",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "У табелі часу початку немає — легенда з варіантами 8:00, 9:00 та 11:00 " +
                    "не прив'язана до людей. Якщо ця людина починає не о %02d:00, вкажи годину тут."
                        .format(fallback),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    onSet(name, it.toIntOrNull())
                },
                label = { Text("Година (порожньо — як у всіх)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
