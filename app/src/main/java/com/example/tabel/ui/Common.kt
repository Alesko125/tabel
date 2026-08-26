package com.example.tabel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tabel.data.DayEntry
import com.example.tabel.data.Kind
import com.example.tabel.data.ShiftKind
import com.example.tabel.ui.theme.LocalShiftPalette
import com.example.tabel.ui.theme.ShiftPalette

@Composable
@ReadOnlyComposable
fun shiftPalette(): ShiftPalette = LocalShiftPalette.current

@Composable
@ReadOnlyComposable
fun shiftColor(entry: DayEntry): Color {
    val p = LocalShiftPalette.current
    return when (entry.kind) {
        Kind.WORK -> when (entry.shift) {
            ShiftKind.DOBA -> p.doba
            ShiftKind.LONG -> p.long
            ShiftKind.DAY -> p.day
            else -> p.partial
        }
        Kind.OFF -> p.off
        Kind.VACATION -> p.vacation
        Kind.SICK -> p.sick
        Kind.UNPAID -> p.vacation
        Kind.ABSENT -> p.sick
        else -> p.none
    }
}

@Composable
fun SectionHeader(text: String, trailing: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun Dot(color: Color, size: Int = 10) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(color, RoundedCornerShape(50))
    )
}

@Composable
fun InfoRow(label: String, value: String, emphasise: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (emphasise) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (emphasise) MaterialTheme.typography.titleMedium
            else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasise) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun StateBanner(loading: Boolean, error: String?, fromCache: Boolean) {
    when {
        loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        error != null -> Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (fromCache) "Показано збережені дані. $error" else error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
