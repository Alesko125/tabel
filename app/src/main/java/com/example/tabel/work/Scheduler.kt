package com.example.tabel.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.tabel.data.Settings
import java.util.concurrent.TimeUnit

object Scheduler {

    private const val WORK_WATCH = "schedule_watch"

    private val networkRequired = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun scheduleAll(context: Context, settings: Settings) {
        scheduleReminder(context, settings)
        scheduleWatch(context, settings)
    }

    /** Нагадування о точній годині — будильником, див. [ReminderAlarm]. */
    fun scheduleReminder(context: Context, settings: Settings) {
        ReminderAlarm.schedule(context, settings)
    }

    /** Перевірка змін у графіку точного часу не потребує — вистачить кожні 6 годин. */
    fun scheduleWatch(context: Context, settings: Settings) {
        val manager = WorkManager.getInstance(context)
        if (!settings.notifyChanges) {
            manager.cancelUniqueWork(WORK_WATCH)
            return
        }

        val request = PeriodicWorkRequestBuilder<ScheduleWatchWorker>(6, TimeUnit.HOURS)
            .setConstraints(networkRequired)
            .build()

        manager.enqueueUniquePeriodicWork(WORK_WATCH, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
