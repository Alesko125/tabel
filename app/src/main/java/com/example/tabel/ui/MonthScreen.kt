package com.example.tabel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MonthScreen(
    state: UiState,
    onShiftMonth: (Long) -> Unit,
    onSelectDate: (LocalDate) -> Unit
) {
    val date = state.date
    val ym = YearMonth.of(date.year, date.monthValue)
    val me = state.me

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onShiftMonth(-1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Попередній місяць")
            }
            Text(
                text = MonthNames.title(date.year, date.monthValue),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { onShiftMonth(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Наступний місяць")
            }
        }

        StateBanner(state.loading, state.error, state.fromCache)
        Spacer(Modifier.height(12.dp))

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

        // Порожні клітинки до першого дня місяця (тиждень починається з понеділка).
        val leading = ym.atDay(1).dayOfWeek.value - 1
        val cells = leading + ym.lengthOfMonth()
        val weeks = (cells + 6) / 7
        var dayCounter = 1

        for (w in 0 until weeks) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    val index = w * 7 + i
                    if (index < leading || dayCounter > ym.lengthOfMonth()) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val day = dayCounter++
                        val projected = state.isProjection && state.rotation!!.isDobaOn(ym.atDay(day))
                        val entry = when {
                            me != null -> me.day(day)
                            projected -> UiState.PROJECTED_DOBA
                            else -> DayEntry.EMPTY
                        }
                        DayCell(
                            day = day,
                            entry = entry,
                            isToday = LocalDate.now() == ym.atDay(day),
                            isSelected = date.dayOfMonth == day,
                            isProjected = me == null && projected,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectDate(ym.atDay(day)) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        if (me != null) {
            val doba = me.days.values.count { it.shift == ShiftKind.DOBA }
            val hours = me.days.values.mapNotNull { it.hours }.sum()
            val vacation = me.days.count { it.value.kind == Kind.VACATION }
            val sick = me.days.count { it.value.kind == Kind.SICK }

            SectionHeader("Підсумок місяця")
            if (doba > 0) InfoRow("Доби", Format.shifts(doba))
            InfoRow("Усього годин", Format.hours(hours))
            if (vacation > 0) InfoRow("Відпустка", Format.days(vacation))
            if (sick > 0) InfoRow("Лікарняний", Format.days(sick))
        } else if (state.isProjection) {
            SectionHeader("Прогноз")
            InfoRow("Діб за графіком", Format.shifts(state.myDobaDays.size))
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Цього місяця ще немає в таблиці. Дати обчислені з ротації " +
                    "«доба через ${state.rotation!!.step - 1}» — коли аркуш з'явиться, " +
                    "застосунок візьме дані з нього.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader("Позначки")
        Legend()
    }
}

@Composable
private fun DayCell(
    day: Int,
    entry: DayEntry,
    isToday: Boolean,
    isSelected: Boolean,
    isProjected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val color = shiftColor(entry)
    val filled = entry.kind != Kind.NONE && entry.kind != Kind.OFF
    // Прогноз показуємо блідішим — це обчислене, а не взяте з табеля.
    val alpha = if (isProjected) shiftPalette().fillAlpha * 0.55f else shiftPalette().fillAlpha

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (filled) color.copy(alpha = alpha)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                else if (isToday) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
            val label = Format.cellLabel(entry)
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (filled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (entry.shift == ShiftKind.DOBA) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun Legend() {
    val p = shiftPalette()
    val items = listOf(
        p.doba to "24 — доба",
        p.long to "11 — зміна 11 годин",
        p.day to "8 — денна зміна",
        p.off to "В — вихідний",
        p.vacation to "від — відпустка",
        p.sick to "б/л — лікарняний"
    )
    Column {
        for ((color, text) in items) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Dot(color, 10)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
