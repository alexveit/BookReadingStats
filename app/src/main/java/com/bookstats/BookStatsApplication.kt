package com.bookstats

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.bookstats.data.sync.SyncWorker
import com.bookstats.service.TimerService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for Book Reading Stats.
 */
@HiltAndroidApp
class BookStatsApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // Schedule periodic sync
        SyncWorker.schedulePeriodicSync(this)
    }
    
    /**
     * WorkManager configuration for Hilt injection.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    
    /**
     * Create notification channel for timer foreground service.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TimerService.CHANNEL_ID,
                "Reading Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active reading timer"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
