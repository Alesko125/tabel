package com.example.tabel.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tabel.data.MonthSheet
import com.example.tabel.data.Repository
import com.example.tabel.data.ScheduleChange
import com.example.tabel.data.Settings
import com.example.tabel.data.ShiftKind
import com.example.tabel.widget.TabelWidget
import java.time.LocalDate

/**
 * Нагадує про добу. Запускається раз на добу о вказаній годині й сам
 * переставляє себе на наступний день.
 */
class ShiftReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = Settings(applicationContext)
        if (!settings.notifyShift) return Result.success()

        // Нагадуємо або вранці в день зміни, або напередодні ввечері.
        val target = if (settings.remindOnShiftDay) LocalDate.now() else LocalDate.now().plusDays(1)
        if (settings.lastReminderDate == target.toString()) return Result.success()

        val repo = Repository(applicationContext, settings)
        val sheet = repo.loadMonth(target.year, target.monthValue).sheet ?: return Result.retry()
        val me = sheet.person(settings.myName) ?: return Result.success()
        val entry = me.day(target.dayOfMonth)
        if (entry.shift != ShiftKind.DOBA) return Result.success()

        val start = settings.dobaStartHour
        val others = sheet.workingOn(target.dayOfMonth, settings.myName)
        val withYou = if (others.isEmpty()) "Більше нікого в табелі на цей день немає."
        else "З тобою: " + others.joinToString(", ") { it.first.name }

        val title = if (settings.remindOnShiftDay) {
            "Сьогодні доба, з %02d:00".format(start)
        } else {
            "Завтра доба, з %02d:00".format(start)
        }
        Notifications.shiftReminder(applicationContext, title, withYou)
        settings.lastReminderDate = target.toString()
        TabelWidget.refresh(applicationContext)
        return Result.success()
    }
}

/**
 * Перечитує табель і повідомляє, якщо мої доби зрушили. Табель редагують
 * протягом місяця, тож зміну графіка інакше можна помітити лише випадково.
 */
class ScheduleWatchWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = Settings(applicationContext)
        val repo = Repository(applicationContext, settings)
        val today = LocalDate.now()

        val messages = mutableListOf<String>()

        for (offset in 0L..1L) {
            val date = today.plusMonths(offset)
            val sheet = repo.loadMonth(date.year, date.monthValue, forceRefresh = true).sheet
                ?: continue
            rememberRotation(sheet, settings)
            ScheduleChange.checkAndStore(settings, sheet)?.let { messages += it.describe() }
        }

        TabelWidget.refresh(applicationContext)

        if (messages.isNotEmpty() && settings.notifyChanges) {
            Notifications.scheduleChanged(
                applicationContext,
                "Графік змінився",
                messages.joinToString("\n")
            )
        }
        return Result.success()
    }

    /** Запам'ятовуємо ротацію, щоб рахувати прогноз за межами таблиці. */
    private fun rememberRotation(sheet: MonthSheet, settings: Settings) {
        val me = sheet.person(settings.myName) ?: return
        val doba = me.dobaDays
        if (doba.isEmpty()) return
        settings.rotationAnchor = LocalDate.of(sheet.year, sheet.month, doba.first()).toEpochDay()
        settings.rotationStep = com.example.tabel.data.Rotation.deriveStep(doba)
    }
}
