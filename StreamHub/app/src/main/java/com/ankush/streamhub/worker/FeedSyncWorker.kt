package com.ankush.streamhub.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ankush.streamhub.StreamHubApp

// ─────────────────────────────────────────────────────────────────────────────
// FeedSyncWorker — background periodic fetch + cache every 30 minutes
// ─────────────────────────────────────────────────────────────────────────────

class FeedSyncWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as StreamHubApp
            val success = app.repository.fetchAndCacheInBackground()
            if (success) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "feed_background_sync"
    }
}
