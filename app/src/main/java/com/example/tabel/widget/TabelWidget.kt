package com.example.tabel.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.action.actionStartActivity
import com.example.tabel.MainActivity
import com.example.tabel.data.DayEntry
import com.example.tabel.data.Format
import com.example.tabel.data.Kind
import com.example.tabel.data.MonthNames
import com.example.tabel.data.Repository
import com.example.tabel.data.Rotation
import com.example.tabel.data.Settings
import com.example.tabel.data.ShiftKind
import java.time.LocalDate

/** Те, що видно на віджеті: статус на сьогодні й коли наступна доба. */
private data class WidgetState(
    val today: String,
    val isDoba: Boolean,
    val next: String?
)

class TabelWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadState(context)
        provideContent {
            GlanceTheme {
                Content(state)
            }
        }
    }

    private suspend fun loadState(context: Context): WidgetState {
        val settings = Settings(context)
        val repo = Repository(context, settings)
        val today = LocalDate.now()

        val sheet = repo.loadMonth(today.year, today.monthValue).sheet
        val me = sheet?.person(settings.myName)
        val entry = me?.day(today.dayOfMonth) ?: DayEntry.EMPTY

        val todayText = when {
            entry.shift == ShiftKind.DOBA ->
                "Доба · з %02d:00".format(settings.dobaStartHour)

            entry.kind == Kind.NONE && me == null -> "Немає даних"
            else -> Format.statusLabel(entry).replaceFirstChar { it.uppercase() }
        }

        return WidgetState(
            today = todayText,
            isDoba = entry.shift == ShiftKind.DOBA,
            next = nextDobaText(settings, sheet?.person(settings.myName), today)
        )
    }

    private fun nextDobaText(
        settings: Settings,
        me: com.example.tabel.data.PersonMonth?,
        today: LocalDate
    ): String? {
        // Спершу пробуємо табель поточного місяця, далі — прогноз ротації.
        val fromSheet = me?.dobaDays
            ?.map { LocalDate.of(today.year, today.monthValue, it) }
            ?.firstOrNull { it.isAfter(today) }

        val next = fromSheet ?: run {
            val step = settings.rotationStep
            if (step <= 0 || settings.rotationAnchor == 0L) return null
            Rotation(LocalDate.ofEpochDay(settings.rotationAnchor), step)
                .nextDobaFrom(today.plusDays(1))
        }

        val days = next.toEpochDay() - today.toEpochDay()
        val date = "${next.dayOfMonth} ${MonthNames.UK_GENITIVE[next.monthValue - 1]}"
        return when (days) {
            1L -> "Наступна доба: завтра, $date"
            else -> "Наступна доба: $date, через ${Format.days(days.toInt())}"
        }
    }

    @androidx.compose.runtime.Composable
    private fun Content(state: WidgetState) {
        val accent = if (state.isDoba) Color(0xFFDD7E6B) else Color(0xFF7A828A)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(14.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = "Сьогодні",
                style = TextStyle(
                    fontSize = 11.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
            Text(
                text = state.today,
                style = TextStyle(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.glance.unit.ColorProvider(accent)
                ),
                modifier = GlanceModifier.padding(top = 2.dp, bottom = 6.dp)
            )
            if (state.next != null) {
                Text(
                    text = state.next,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurface
                    )
                )
            }
        }
    }

    companion object {
        suspend fun refresh(context: Context) {
            runCatching { TabelWidget().updateAll(context) }
        }
    }
}

class TabelWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TabelWidget()
}
