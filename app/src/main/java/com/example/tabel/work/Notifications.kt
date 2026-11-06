package com.example.tabel.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.tabel.MainActivity
import com.example.tabel.R

object Notifications {

    const val CHANNEL_SHIFT = "shift"
    const val CHANNEL_CHANGES = "changes"

    private const val ID_SHIFT = 1
    private const val ID_CHANGES = 2

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SHIFT,
                "Нагадування про добу",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Нагадує в день зміни" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CHANGES,
                "Зміни в графіку",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Повідомляє, коли твої доби в табелі зрушили" }
        )
    }

    fun shiftReminder(context: Context, title: String, text: String) =
        show(context, CHANNEL_SHIFT, ID_SHIFT, title, text)

    fun scheduleChanged(context: Context, title: String, text: String) =
        show(context, CHANNEL_CHANGES, ID_CHANGES, title, text)

    private fun show(context: Context, channel: String, id: Int, title: String, text: String) {
        if (!canPost(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    /** На Android 13+ без дозволу сповіщення просто не прийдуть. */
    fun canPost(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
}
