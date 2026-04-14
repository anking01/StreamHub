package com.ankush.streamhub.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ankush.streamhub.R
import com.ankush.streamhub.StreamHubApp
import com.ankush.streamhub.ui.MainActivity

// ─────────────────────────────────────────────────────────────────────────────
// DigestWorker — runs daily at ~8 AM, shows top 5 stories notification
// ─────────────────────────────────────────────────────────────────────────────

class DigestWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as StreamHubApp

        // Respect user preference
        if (!app.preferences.digestEnabled) return Result.success()

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return Result.success()
        }

        val topItems = app.repository.getTopCachedItems(5)
        if (topItems.isEmpty()) return Result.success()

        val bulletPoints = topItems.joinToString("\n") { "• ${it.title.take(70)}" }

        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, tapIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bookmark_outline)
            .setContentTitle("📰 StreamHub Daily Digest")
            .setContentText("${topItems.size} top stories for you today")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bulletPoints))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIF_ID, notification)
        return Result.success()
    }

    companion object {
        const val WORK_NAME  = "daily_digest"
        const val CHANNEL_ID = "digest_channel"
        const val NOTIF_ID   = 1001
    }
}
