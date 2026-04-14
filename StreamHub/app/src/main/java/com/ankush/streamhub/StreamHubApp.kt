package com.ankush.streamhub

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.*
import com.ankush.streamhub.ai.SummarizationService
import com.ankush.streamhub.data.local.AppDatabase
import com.ankush.streamhub.data.remote.FeedRepository
import com.ankush.streamhub.util.Analytics
import com.ankush.streamhub.worker.DigestWorker
import com.ankush.streamhub.worker.FeedSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.TimeUnit

class StreamHubApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob())

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    val repository: FeedRepository by lazy {
        FeedRepository(
            bookmarkDao     = database.bookmarkDao(),
            cachedFeedDao   = database.cachedFeedDao(),
            watchHistoryDao = database.watchHistoryDao(),
            collectionDao   = database.collectionDao(),
            appScope        = applicationScope
        )
    }

    val preferences: AppPreferences by lazy { AppPreferences(this) }

    val summarizationService: SummarizationService by lazy { SummarizationService(preferences) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Analytics.init(this)
        createNotificationChannel()
        scheduleBackgroundWork()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DigestWorker.CHANNEL_ID,
                "Daily Digest",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Daily top stories from StreamHub" }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun scheduleBackgroundWork() {
        val workManager = WorkManager.getInstance(this)

        // Background feed sync every 30 minutes (needs network)
        val syncRequest = PeriodicWorkRequestBuilder<FeedSyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            FeedSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        // Daily digest at ~8 AM (initial delay calculated from now)
        val digestRequest = PeriodicWorkRequestBuilder<DigestWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(computeInitialDigestDelay(), TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            DigestWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            digestRequest
        )
    }

    /** Returns milliseconds until the next 8:00 AM */
    private fun computeInitialDigestDelay(): Long {
        val now = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 8)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) target.add(java.util.Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis - now.timeInMillis
    }

    companion object {
        lateinit var instance: StreamHubApp
            private set
    }
}
