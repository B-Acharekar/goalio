package com.goalio.scores

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object GoalioMatchNotifier {
    private const val CHANNEL_ID = "goalio_live_match_alerts"
    private const val CHANNEL_NAME = "Live match alerts"

    fun notifyBackgroundEvents(context: Context, events: List<MatchNotificationEvent>) {
        if (events.isEmpty() || GoalioAppVisibility.isForeground) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)
        val appContext = context.applicationContext
        val launchIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val manager = NotificationManagerCompat.from(appContext)
        events.forEach { event ->
            val contentText = event.kickoffEpochMillis?.let { kickoff ->
                "${matchClockText(kickoff)} • ${event.message}"
            } ?: event.message
            val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(event.title)
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
            event.kickoffEpochMillis?.let { kickoff ->
                builder
                    .setWhen(kickoff)
                    .setShowWhen(true)
                    .setUsesChronometer(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && kickoff > System.currentTimeMillis()) {
                    builder.setChronometerCountDown(true)
                }
            }
            val notification = builder.build()
            manager.notify(event.id.hashCode(), notification)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alerts when tracked football matches start or the score changes."
            }
        )
    }

    private fun matchClockText(kickoffEpochMillis: Long): String {
        val diffSeconds = ((kickoffEpochMillis - System.currentTimeMillis()) / 1000L)
        val prefix = if (diffSeconds >= 0) "Starts in" else "Live"
        val total = kotlin.math.abs(diffSeconds)
        val hours = total / 3600L
        val minutes = (total % 3600L) / 60L
        val seconds = total % 60L
        return "$prefix %02d:%02d:%02d".format(hours, minutes, seconds)
    }
}
