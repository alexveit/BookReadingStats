package com.bookstats.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for background sync operations.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "SyncWorker"
        private const val UNIQUE_WORK_NAME = "book_stats_sync"
        private const val PERIODIC_WORK_NAME = "book_stats_periodic_sync"
        
        /**
         * Schedule an immediate one-time sync.
         */
        fun scheduleImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )
            
            Log.d(TAG, "Immediate sync scheduled")
        }
        
        /**
         * Schedule periodic sync (every 15 minutes when connected).
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val periodicSync = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicSync
                )
            
            Log.d(TAG, "Periodic sync scheduled")
        }
        
        /**
         * Cancel all sync work.
         */
        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
            Log.d(TAG, "Sync cancelled")
        }
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting sync work...")
        
        return try {
            val success = syncManager.syncAll()
            if (success) {
                Log.d(TAG, "Sync work completed successfully")
                Result.success()
            } else {
                Log.d(TAG, "Sync work completed with no changes or offline")
                Result.success() // Still success, just nothing to do
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync work failed", e)
            Result.retry()
        }
    }
}
