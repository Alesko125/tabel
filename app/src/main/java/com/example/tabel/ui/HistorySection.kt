package com.example.tabel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.tabel.MonthPay
import com.example.tabel.data.Format
import com.example.tabel.data.MonthNames
import kotlin.math.abs

@Composable
fun HistorySection(
    history: List<MonthPay>,
    loading: Boolean,
    onSetActual: (Int, Int, Double?) -> Unit
) {
    SectionHeader("Історія", if (history.isEmpty()) null else "${history.size} міс.")

    if (loading && history.isEmpty()) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Читаю попередні місяці з таблиці…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    if (history.isEmpty()) return

    val counted = history.mapNotNull { it.salary }.filter { it.onRotation }
    if (counted.isNotEmpty()) {
        val total = counted.sumOf { it.total }
        val shifts = counted.sumOf { it.workedShifts }
        InfoRow("Разом за ${counted.size} міс.", Format.money(total))
        InfoRow("Відпрацьовано", Format.shifts(shifts))
        Spacer(Modifier.height(4.dp))
        PayChart(history)
        Spacer(Modifier.height(8.dp))
    }

    for (item in history) {
        MonthRow(item, onSetActual)
    }
}

/**
 * Стовпчики нарахувань по місяцях.
 *
 * Суми різняться на кілька відсотків, так що шкала від нуля дала б
 * рівний блок. Замість цього розтягую діапазон між найменшим і найбільшим
 * місяцем — так видно, які місяці просіли.
 */
@Composable
private fun PayChart(history: List<MonthPay>) {
    val values = history.reversed().map { it.salary?.total ?: 0.0 }
    val paid = values.filter { it > 0 }
    if (paid.size < 2) return

    val hi = paid.max()
    val lo = paid.min()
    val range = (hi - lo).takeIf { it > 1.0 } ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        for (v in values) {
            // Найнижчий місяць лишаю помітним, а не нульовим.
            val fraction = if (v <= 0) 0.06f
            else (0.25 + 0.75 * (v - lo) / range).coerceIn(0.06, 1.0).toFloat()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction)
                    .background(
                        if (v > 0) shiftPalette().doba.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                    )
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = Format.money(lo),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = Format.money(hi),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MonthRow(item: MonthPay, onSetActual: (Int, Int, Double?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val salary = item.salary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = MonthNames.title(item.year, item.month),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = when {
                        salary == null -> "немає даних"
                        !salary.onRotation -> "не в добовій ротації"
                        else -> "${Format.shifts(salary.workedShifts)} з ${salary.plannedShifts}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (salary?.onRotation == true) Format.money(salary.total) else "—",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val actual = item.actual
                if (actual != null && salary?.onRotation == true) {
                    val diff = actual - salary.total
                    Text(
                        text = if (abs(diff) < 1) "збіглося"
                        else (if (diff > 0) "+" else "−") + Format.money(abs(diff)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (abs(diff) < 1) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Сховати" else "Факт")
            }
        }

        if (expanded) {
            ActualPayField(item, onSetActual)
        }
        HorizontalDivider()
    }
}

@Composable
private fun ActualPayField(item: MonthPay, onSetActual: (Int, Int, Double?) -> Unit) {
    var text by remember(item.year, item.month) {
        mutableStateOf(item.actual?.toInt()?.toString() ?: "")
    }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onSetActual(item.year, item.month, it.toDoubleOrNull())
        },
        label = { Text("Скільки прийшло насправді, грн") },
        supportingText = {
            Text(
                "Порожньо — не звіряти цей місяць",
                style = MaterialTheme.typography.bodySmall
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    )
}
