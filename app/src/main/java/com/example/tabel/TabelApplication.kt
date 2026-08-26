package com.example.tabel

import android.app.Application
import com.example.tabel.data.Settings
import com.example.tabel.work.Notifications
import com.example.tabel.work.Scheduler

class TabelApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        Scheduler.scheduleAll(this, Settings(this))
    }
}
