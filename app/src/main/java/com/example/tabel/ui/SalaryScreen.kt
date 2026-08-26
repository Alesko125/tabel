package com.example.tabel.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tabel.UiState
import com.example.tabel.data.Format
import com.example.tabel.data.MonthNames

@Composable
fun SalaryScreen(
    state: UiState,
    onShiftMonth: (Long) -> Unit,
    onLoadHistory: () -> Unit,
    onSetActual: (Int, Int, Double?) -> Unit
) {
    val salary = state.salary

    LaunchedEffect(Unit) {
        if (state.history.isEmpty() && !state.historyLoading) onLoadHistory()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .swipePages(onPrevious = { onShiftMonth(-1) }, onNext = { onShiftMonth(1) })
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
                text = MonthNames.title(state.date.year, state.date.monthValue),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { onShiftMonth(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Наступний місяць")
            }
        }

        StateBanner(state.loading, state.error.takeIf { !state.isProjection }, state.fromCache)
        Spacer(Modifier.height(8.dp))

        if (salary == null) {
            if (state.isProjection) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "Прогноз: ${Format.shifts(state.myDobaDays.size)} за графіком",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Якщо відпрацюєш усі доби — приблизно " +
                                "${Format.money(state.base + state.bonusPool)}. Цього місяця ще " +
                                "немає в таблиці, тож дати обчислені з ротації.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    text = "Немає даних за цей місяць.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            HistorySection(state.history, state.historyLoading, onSetActual)
            return@Column
        }

        if (!salary.onRotation) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Цього місяця ти не був у добовій ротації.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Розрахунок надбавки за доби не застосовується. " +
                            "Відпрацьовано ${Format.hours(salary.totalHours)}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Column
        }

        // Підсумкова сума.
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = "На руки",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = Format.money(salary.total),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${Format.shifts(salary.workedShifts)} з ${salary.plannedShifts} за графіком",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        SectionHeader("Розрахунок")
        InfoRow("Ставка", Format.money(salary.base))
        InfoRow(
            "Надбавка · ${salary.workedShifts} × ${Format.money2(salary.bonusPerShift)}",
            Format.money(salary.bonus)
        )
        HorizontalDivider(Modifier.padding(vertical = 6.dp))
        InfoRow("Разом", Format.money(salary.total), emphasise = true)

        if (salary.bonusLost > 0.5) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Недоотримано з надбавки: ${Format.money(salary.bonusLost)} " +
                    "(${Format.shifts(salary.plannedShifts - salary.workedShifts)} закрив хтось інший).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (salary.coveredForMe.isNotEmpty()) {
            val byPerson = salary.coveredForMe.groupBy { it.who }
            SectionHeader("Твої доби закрили")
            for ((who, covers) in byPerson) {
                InfoRow(
                    "$who · ${covers.joinToString(", ") { it.day.toString() }} ${MonthNames.UK_GENITIVE[salary.month - 1]}",
                    Format.money(salary.bonusPerShift * covers.size)
                )
            }
        }

        if (salary.coveredByMe.isNotEmpty()) {
            SectionHeader("Ти підміняв")
            for (cover in salary.coveredByMe) {
                InfoRow(
                    "${cover.who} · ${cover.day} ${MonthNames.UK_GENITIVE[salary.month - 1]}",
                    Format.money(salary.bonusPerShift)
                )
            }
        }

        SectionHeader("Місяць")
        InfoRow("Відпрацьовано годин", Format.hours(salary.totalHours))
        if (salary.vacationDays > 0) InfoRow("Відпустка", Format.days(salary.vacationDays))
        if (salary.sickDays > 0) InfoRow("Лікарняний", Format.days(salary.sickDays))

        Spacer(Modifier.height(20.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    text = "Як рахується",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Ставка ${Format.money(salary.base)} нараховується за повний місяць і не " +
                        "залежить від відпустки. Надбавка ${Format.money(salary.bonusPool)} — за роботу " +
                        "«доба через дві» замість норми «доба через три». Вона ділиться на " +
                        "${salary.plannedShifts} планових діб місяця (${Format.money2(salary.bonusPerShift)} за добу) " +
                        "і нараховується за фактично відпрацьовані доби. Решта йде тому, хто тебе замінив.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Це орієнтовний розрахунок на основі табеля — податки, премії та " +
                        "відпускні тут не враховані.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        HistorySection(state.history, state.historyLoading, onSetActual)
    }
}
