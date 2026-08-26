package com.example.tabel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabel.data.*
import com.example.tabel.widget.TabelWidget
import com.example.tabel.work.Scheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** За який період рахувати статистику. */
sealed interface StatsScope {
    data class Year(val year: Int) : StatsScope
    data object AllTime : StatsScope
}

/** Один місяць в історії зарплати. */
data class MonthPay(
    val year: Int,
    val month: Int,
    val salary: SalaryBreakdown?,
    val actual: Double?
)

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
    val personStartHours: Map<String, Int> = emptyMap(),
    val sheetId: String = Settings.DEFAULT_SHEET,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val rotation: Rotation? = null,
    /** Опис змін у моєму графіку з часу минулого перегляду. */
    val scheduleChange: String? = null,
    val selectedPerson: String? = null,
    val history: List<MonthPay> = emptyList(),
    val historyLoading: Boolean = false,
    val stats: StatsResult? = null,
    val statsLoading: Boolean = false,
    val statsProgress: Int = 0,
    val statsTotal: Int = 0,
    val statsScope: StatsScope = StatsScope.Year(LocalDate.now().year),
    val notifyShift: Boolean = true,
    val notifyChanges: Boolean = true,
    val reminderHour: Int = Settings.DEFAULT_REMINDER_HOUR,
    val remindOnShiftDay: Boolean = true
) {
    val me: PersonMonth? get() = sheet?.person(myName)
    val shiftTimes: ShiftTimes get() = ShiftTimes(dobaStart, dayStart, personStartHours)

    /** Місяця немає в таблиці, але ротацію можна порахувати. */
    val isProjection: Boolean get() = sheet == null && rotation != null

    /** Мої доби цього місяця — з табеля або з прогнозу. */
    val myDobaDays: List<Int>
        get() = me?.dobaDays
            ?: rotation?.dobaDaysIn(date.year, date.monthValue)
            ?: emptyList()

    val myEntry: DayEntry
        get() {
            me?.let { return it.day(date.dayOfMonth) }
            if (isProjection && rotation!!.isDobaOn(date)) return PROJECTED_DOBA
            return DayEntry.EMPTY
        }

    val salary: SalaryBreakdown?
        get() {
            val s = sheet ?: return null
            val m = me ?: return null
            return Salary.compute(s, m, base, bonusPool)
        }

    val person: PersonMonth? get() = selectedPerson?.let { sheet?.person(it) }

    companion object {
        /** Доба, обчислена з ротації, а не взята з табеля. */
        val PROJECTED_DOBA = DayEntry("24", 24.0, Kind.WORK)
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
            personStartHours = settings.personStartHours,
            sheetId = settings.sheetId,
            themeMode = settings.themeMode,
            rotation = storedRotation(),
            notifyShift = settings.notifyShift,
            notifyChanges = settings.notifyChanges,
            reminderHour = settings.reminderHour,
            remindOnShiftDay = settings.remindOnShiftDay
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        load(_state.value.date, force = false)
    }

    private fun storedRotation(): Rotation? {
        val anchor = settings.rotationAnchor
        val step = settings.rotationStep
        if (anchor == 0L || step <= 0) return null
        return Rotation(LocalDate.ofEpochDay(anchor), step)
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

            val nextYm = YearMonth.from(date).plusMonths(1)
            val next = runCatching {
                repo.loadMonth(nextYm.year, nextYm.monthValue, false).sheet
            }.getOrNull()

            // Зміну в графіку показуємо самі: якщо просто перезаписати знімок,
            // фоновий воркер уже не матиме з чим порівнювати.
            var change = _state.value.scheduleChange
            result.sheet?.let { sheet ->
                rememberRotation(sheet)
                ScheduleChange.checkAndStore(settings, sheet)?.let { change = it.describe() }
            }

            _state.value = _state.value.copy(
                sheet = result.sheet,
                nextSheet = next,
                loading = false,
                error = result.error,
                fromCache = result.fromCache,
                rotation = storedRotation(),
                scheduleChange = change
            )
            TabelWidget.refresh(getApplication())
        }
    }

    /** Ротацію запам'ятовуємо, щоб прогнозувати місяці, яких ще немає в таблиці. */
    private fun rememberRotation(sheet: MonthSheet) {
        val me = sheet.person(settings.myName) ?: return
        val doba = me.dobaDays
        if (doba.isEmpty()) return
        val anchor = LocalDate.of(sheet.year, sheet.month, doba.first())
        // Беремо найсвіжіший відомий місяць, щоб прогноз ішов від нього.
        if (anchor.toEpochDay() >= settings.rotationAnchor) {
            settings.rotationAnchor = anchor.toEpochDay()
            settings.rotationStep = Rotation.deriveStep(doba)
        }
    }

    fun dismissScheduleChange() {
        _state.value = _state.value.copy(scheduleChange = null)
    }

    // ---------- люди ----------

    fun selectPerson(name: String?) {
        _state.value = _state.value.copy(selectedPerson = name)
    }

    fun setPersonStartHour(name: String, hour: Int?) {
        settings.setPersonStartHour(name, hour)
        _state.value = _state.value.copy(personStartHours = settings.personStartHours)
    }

    // ---------- історія зарплати ----------

    fun loadHistory(months: Int = 12) {
        if (_state.value.historyLoading) return
        viewModelScope.launch {
            _state.value = _state.value.copy(historyLoading = true)
            val out = mutableListOf<MonthPay>()
            var ym = YearMonth.from(_state.value.date)
            repeat(months) {
                val sheet = repo.loadMonth(ym.year, ym.monthValue).sheet
                val me = sheet?.person(settings.myName)
                val salary = if (sheet != null && me != null) {
                    Salary.compute(sheet, me, settings.base, settings.bonusPool)
                } else null
                out += MonthPay(
                    year = ym.year,
                    month = ym.monthValue,
                    salary = salary,
                    actual = settings.actualPay(ym.year, ym.monthValue)
                )
                ym = ym.minusMonths(1)
            }
            _state.value = _state.value.copy(history = out, historyLoading = false)
        }
    }

    // ---------- статистика ----------

    /**
     * Рахує підсумки по людях. За весь час це близько 150 місяців — тягнемо
     * послідовно й показуємо прогрес, бо перший раз це помітно довго.
     * Далі місяці беруться з кешу.
     */
    fun loadStats(scope: StatsScope = _state.value.statsScope) {
        if (_state.value.statsLoading) return
        viewModelScope.launch {
            val months = monthsFor(scope)
            _state.value = _state.value.copy(
                statsLoading = true,
                statsScope = scope,
                statsProgress = 0,
                statsTotal = months.size,
                stats = null
            )

            val sheets = mutableListOf<MonthSheet>()
            for ((index, ym) in months.withIndex()) {
                repo.loadMonth(ym.year, ym.monthValue).sheet?.let { sheets += it }
                _state.value = _state.value.copy(statsProgress = index + 1)
            }

            _state.value = _state.value.copy(
                stats = YearStats.aggregate(sheets, settings.myName, months.size),
                statsLoading = false
            )
        }
    }

    private fun monthsFor(scope: StatsScope): List<YearMonth> {
        val now = YearMonth.now()
        return when (scope) {
            is StatsScope.Year -> (1..12)
                .map { YearMonth.of(scope.year, it) }
                .filter { !it.isAfter(now) }

            StatsScope.AllTime -> {
                val out = mutableListOf<YearMonth>()
                var ym = YearMonth.of(FIRST_YEAR_IN_SHEET, 1)
                while (!ym.isAfter(now)) {
                    out += ym
                    ym = ym.plusMonths(1)
                }
                out
            }
        }
    }

    fun setActualPay(year: Int, month: Int, value: Double?) {
        settings.setActualPay(year, month, value)
        _state.value = _state.value.copy(
            history = _state.value.history.map {
                if (it.year == year && it.month == month) it.copy(actual = value) else it
            }
        )
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

    fun setSheetId(v: String) {
        settings.sheetId = v
        _state.value = _state.value.copy(sheetId = v.trim(), sheet = null)
        refresh()
    }

    fun setThemeMode(v: ThemeMode) {
        settings.themeMode = v
        _state.value = _state.value.copy(themeMode = v)
    }

    fun setNotifyShift(v: Boolean) {
        settings.notifyShift = v
        _state.value = _state.value.copy(notifyShift = v)
        Scheduler.scheduleReminder(getApplication(), settings)
    }

    fun setNotifyChanges(v: Boolean) {
        settings.notifyChanges = v
        _state.value = _state.value.copy(notifyChanges = v)
        Scheduler.scheduleWatch(getApplication(), settings)
    }

    fun setReminderHour(v: Int) {
        settings.reminderHour = v
        _state.value = _state.value.copy(reminderHour = v.coerceIn(0, 23))
        Scheduler.scheduleReminder(getApplication(), settings)
    }

    fun setRemindOnShiftDay(v: Boolean) {
        settings.remindOnShiftDay = v
        _state.value = _state.value.copy(remindOnShiftDay = v)
        Scheduler.scheduleReminder(getApplication(), settings)
    }

    companion object {
        /** Найраніший рік у таблиці — далі назад аркушів немає. */
        const val FIRST_YEAR_IN_SHEET = 2014
    }
}
