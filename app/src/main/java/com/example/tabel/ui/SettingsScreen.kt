package com.example.tabel.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tabel.UiState
import com.example.tabel.data.ThemeMode

@Composable
fun SettingsScreen(
    state: UiState,
    onThemeMode: (ThemeMode) -> Unit,
    onName: (String) -> Unit,
    onBase: (Double) -> Unit,
    onBonus: (Double) -> Unit,
    onDobaStart: (Int) -> Unit,
    onDayStart: (Int) -> Unit,
    onSheetId: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SectionHeader("Вигляд")
        ThemePicker(current = state.themeMode, onSelect = onThemeMode)

        SectionHeader("Хто я")
        SettingField(
            label = "Прізвище в табелі",
            initial = state.myName,
            supporting = "Достатньо прізвища — застосунок знайде рядок «Кобзар О.»",
            onChange = onName
        )

        SectionHeader("Зарплата")
        SettingField(
            label = "Ставка на руки, грн",
            initial = state.base.toInt().toString(),
            numeric = true,
            supporting = "Нараховується за повний місяць, не залежить від відпустки",
            onChange = { it.toDoubleOrNull()?.let(onBase) }
        )
        SettingField(
            label = "Надбавка за графік, грн",
            initial = state.bonusPool.toInt().toString(),
            numeric = true,
            supporting = "Ділиться пропорційно відпрацьованим добам",
            onChange = { it.toDoubleOrNull()?.let(onBonus) }
        )

        SectionHeader("Час змін")
        SettingField(
            label = "Початок доби, година",
            initial = state.dobaStart.toString(),
            numeric = true,
            supporting = "Для підписів на кшталт «доба · 09:00 → 09:00»",
            onChange = { it.toIntOrNull()?.let(onDobaStart) }
        )
        SettingField(
            label = "Початок денної зміни, година",
            initial = state.dayStart.toString(),
            numeric = true,
            supporting = "За замовчуванням 9 — зміна 9:00–18:00",
            onChange = { it.toIntOrNull()?.let(onDayStart) }
        )

        SectionHeader("Таблиця")
        SheetIdField(initial = state.sheetId, onApply = onSheetId)

        Spacer(Modifier.height(12.dp))
        Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
            Text("Оновити дані з таблиці")
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Застосунок читає таблицю лише на перегляд і зберігає копію на телефоні, " +
                "щоб графік був доступний без інтернету.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemePicker(current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.SYSTEM to "Як у системі",
        ThemeMode.LIGHT to "Світла",
        ThemeMode.DARK to "Темна"
    )
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = current == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SettingField(
    label: String,
    initial: String,
    supporting: String? = null,
    numeric: Boolean = false,
    onChange: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onChange(it)
        },
        label = { Text(label) },
        supportingText = supporting?.let { s -> { Text(s, style = MaterialTheme.typography.bodySmall) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text
        ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

/** Зміна ID таблиці скидає кеш, тому застосовуємо її лише за кнопкою. */
@Composable
private fun SheetIdField(initial: String, onApply: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("ID Google-таблиці") },
        supportingText = { Text("Частина посилання між /d/ та /edit", style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
    TextButton(
        onClick = { onApply(text) },
        enabled = text.isNotBlank() && text.trim() != initial
    ) {
        Text("Застосувати іншу таблицю")
    }
}
