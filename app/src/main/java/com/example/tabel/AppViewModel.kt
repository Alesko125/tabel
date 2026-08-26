package com.example.tabel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabel.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class UiState(
    val date: LocalDate = LocalDate.now(),
    val sheet: MonthSheet? = null,
    /** Наступний місяць — потрібен, коли доба переходить через кінець місяця. */
    val nextSheet: MonthSheet? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val fromCache: Boolean = false,
    val myName: String = Settings.DEFAULT_NAME,
    val base: Double = Settings.DEFAULT_BASE,
    val bonusPool: Double = Settings.DEFAULT_BONUS,
    val dobaStart: Int = Settings.DEFAULT_DOBA_START,
    val dayStart: Int = Settings.DEFAULT_DAY_START,
    val sheetId: String = Settings.DEFAULT_SHEET,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
) {
    val me: PersonMonth? get() = sheet?.person(myName)
    val myEntry: DayEntry get() = me?.day(date.dayOfMonth) ?: DayEntry.EMPTY
    val shiftTimes: ShiftTimes get() = ShiftTimes(dobaStart, dayStart)

    val salary: SalaryBreakdown?
        get() {
            val s = sheet ?: return null
            val m = me ?: return null
            return Salary.compute(s, m, base, bonusPool)
        }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = Settings(app)
    private val repo = Repository(app, settings)

    private val _state = MutableStateFlow(
        UiState(
            myName = settings.myName,
            base = settings.base,
            bonusPool = settings.bonusPool,
            dobaStart = settings.dobaStartHour,
            dayStart = settings.dayShiftStartHour,
            sheetId = settings.sheetId,
            themeMode = settings.themeMode
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        load(_state.value.date, force = false)
    }

    fun selectDate(date: LocalDate) {
        val old = _state.value.date
        _state.value = _state.value.copy(date = date)
        if (old.year != date.year || old.monthValue != date.monthValue || _state.value.sheet == null) {
            load(date, force = false)
        }
    }

    fun shiftDay(delta: Long) = selectDate(_state.value.date.plusDays(delta))

    fun shiftMonth(delta: Long) {
        val ym = YearMonth.from(_state.value.date).plusMonths(delta)
        val day = _state.value.date.dayOfMonth.coerceAtMost(ym.lengthOfMonth())
        selectDate(ym.atDay(day))
    }

    fun refresh() = load(_state.value.date, force = true)

    private fun load(date: LocalDate, force: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val result = repo.loadMonth(date.year, date.monthValue, force)

            // Доба з останнього дня місяця перетікає в наступний — підвантажуємо і його.
            val nextYm = YearMonth.from(date).plusMonths(1)
            val next = runCatching {
                repo.loadMonth(nextYm.year, nextYm.monthValue, false).sheet
            }.getOrNull()

            _state.value = _state.value.copy(
                sheet = result.sheet ?: _state.value.sheet.takeIf { it?.contains(date) == true },
                nextSheet = next,
                loading = false,
                error = result.error,
                fromCache = result.fromCache
            )
        }
    }

    // ---------- налаштування ----------

    fun setMyName(v: String) {
        settings.myName = v
        _state.value = _state.value.copy(myName = v.trim())
    }

    fun setBase(v: Double) {
        settings.base = v
        _state.value = _state.value.copy(base = v)
    }

    fun setBonus(v: Double) {
        settings.bonusPool = v
        _state.value = _state.value.copy(bonusPool = v)
    }

    fun setDobaStart(v: Int) {
        settings.dobaStartHour = v
        _state.value = _state.value.copy(dobaStart = v.coerceIn(0, 23))
    }

    fun setDayStart(v: Int) {
        settings.dayShiftStartHour = v
        _state.value = _state.value.copy(dayStart = v.coerceIn(0, 23))
    }

    fun setThemeMode(v: ThemeMode) {
        settings.themeMode = v
        _state.value = _state.value.copy(themeMode = v)
    }

    fun setSheetId(v: String) {
        settings.sheetId = v
        _state.value = _state.value.copy(sheetId = v.trim(), sheet = null)
        refresh()
    }
}
