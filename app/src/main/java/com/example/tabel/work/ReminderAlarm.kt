package com.example.tabel.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.tabel.data.Settings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Щоденне нагадування ставлю будильником, а не періодичною задачею.
 *
 * WorkManager не вміє в точну годину — вікно запуску в нього занадто широке,
 * а одноразова задача, яка переставляє сама себе під тим самим унікальним
 * іменем, зрубує власний запуск.
 */
object ReminderAlarm {

    private const val ACTION_REMIND = "com.example.tabel.REMIND"
    private const val REQUEST_CODE = 1001
    private const val WORK_NAME = "shift_reminder_run"

    fun schedule(context: Context, settings: Settings) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent(context)

        if (!settings.notifyShift) {
            manager.cancel(pending)
            return
        }

        val at = nextTrigger(settings.reminderHour)
        // Неточному будильнику не треба окремого дозволу, і він спрацьовує
        // навіть у режимі очікування — для нагадування цього досить.
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTrigger(hour: Int): Long {
        val now = LocalDateTime.now()
        var target = LocalDateTime.of(LocalDate.now(), LocalTime.of(hour.coerceIn(0, 23), 0))
        if (!target.isAfter(now)) target = target.plusDays(1)
        return target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /** Сама перевірка графіка лізе в мережу, так що роблю її задачею. */
    fun enqueueCheck(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ShiftReminderWorker>().build()
        )
    }
}

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val settings = Settings(context)
        // Будильники не переживають перезавантаження — ставлю наново.
        ReminderAlarm.schedule(context, settings)

        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            ReminderAlarm.enqueueCheck(context)
        }
    }
}
