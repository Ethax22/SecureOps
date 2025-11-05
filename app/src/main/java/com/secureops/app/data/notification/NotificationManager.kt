package com.secureops.app.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.secureops.app.MainActivity
import com.secureops.app.R
import com.secureops.app.domain.model.*
import timber.log.Timber
import java.util.*

class NotificationManager(
    private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var preferences: NotificationPreferences = NotificationPreferences()

    init {
        createNotificationChannels()
    }

    /**
     * Update notification preferences
     */
    fun updatePreferences(prefs: NotificationPreferences) {
        preferences = prefs
        Timber.d("Notification preferences updated")
    }

    /**
     * Send build failure notification
     */
    fun notifyBuildFailure(pipeline: Pipeline) {
        if (!shouldNotify(NotificationChannelType.FAILURES)) return
        if (isQuietHours()) return

        val title = "❌ Build Failed"
        val message = "${pipeline.repositoryName} - Build #${pipeline.buildNumber}"

        sendNotification(
            title = title,
            message = message,
            channelId = "failures",
            priority = NotificationCompat.PRIORITY_HIGH,
            data = mapOf("pipelineId" to pipeline.id)
        )
    }

    /**
     * Send high-risk prediction notification
     */
    fun notifyHighRisk(pipeline: Pipeline, riskPercentage: Float) {
        if (!shouldNotify(NotificationChannelType.HIGH_RISK)) return
        if (riskPercentage < preferences.riskThreshold) return
        if (isQuietHours()) return

        val title = "⚠️ High Risk Detected"
        val message = "${pipeline.repositoryName} has ${riskPercentage.toInt()}% failure risk"

        sendNotification(
            title = title,
            message = message,
            channelId = "warnings",
            priority = NotificationCompat.PRIORITY_HIGH,
            data = mapOf("pipelineId" to pipeline.id, "risk" to riskPercentage.toString())
        )
    }

    /**
     * Send build success notification
     */
    fun notifyBuildSuccess(pipeline: Pipeline) {
        if (!shouldNotify(NotificationChannelType.SUCCESS)) return
        if (isQuietHours()) return

        val title = "✅ Build Successful"
        val message = "${pipeline.repositoryName} - Build #${pipeline.buildNumber}"

        sendNotification(
            title = title,
            message = message,
            channelId = "success",
            priority = NotificationCompat.PRIORITY_DEFAULT,
            data = mapOf("pipelineId" to pipeline.id)
        )
    }

    /**
     * Send build started notification
     */
    fun notifyBuildStarted(pipeline: Pipeline) {
        if (!shouldNotify(NotificationChannelType.BUILD_STARTED)) return
        if (isQuietHours()) return

        val title = "🔨 Build Started"
        val message = "${pipeline.repositoryName} - Build #${pipeline.buildNumber}"

        sendNotification(
            title = title,
            message = message,
            channelId = "default",
            priority = NotificationCompat.PRIORITY_LOW,
            data = mapOf("pipelineId" to pipeline.id)
        )
    }

    /**
     * Send custom alert based on rules
     */
    fun notifyCustomAlert(rule: AlertRule, pipeline: Pipeline) {
        if (!rule.enabled) return
        if (isQuietHours() && rule.action != AlertAction.NOTIFY_WITH_SOUND) return

        val title = "🔔 ${rule.name}"
        val message = "Alert triggered for ${pipeline.repositoryName}"

        val priority = when (rule.action) {
            AlertAction.NOTIFY_WITH_SOUND -> NotificationCompat.PRIORITY_HIGH
            AlertAction.NOTIFY -> NotificationCompat.PRIORITY_DEFAULT
            AlertAction.SILENT -> NotificationCompat.PRIORITY_LOW
            AlertAction.DO_NOT_DISTURB -> return
        }

        sendNotification(
            title = title,
            message = message,
            channelId = "custom",
            priority = priority,
            data = mapOf("ruleId" to rule.id, "pipelineId" to pipeline.id)
        )
    }

    /**
     * Check if notifications should be sent
     */
    private fun shouldNotify(channelType: NotificationChannelType): Boolean {
        if (preferences.alertOnCriticalOnly &&
            channelType != NotificationChannelType.FAILURES &&
            channelType != NotificationChannelType.HIGH_RISK
        ) {
            return false
        }
        return channelType in preferences.enabledChannels
    }

    /**
     * Check if currently in quiet hours
     */
    private fun isQuietHours(): Boolean {
        val quietHours = preferences.quietHours ?: return false
        if (!quietHours.enabled) return false

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)

        // Check if current day is in quiet hours days
        if (currentDay !in quietHours.daysOfWeek) return false

        // Convert to minutes for easier comparison
        val currentTime = currentHour * 60 + currentMinute
        val startTime = quietHours.startHour * 60 + quietHours.startMinute
        val endTime = quietHours.endHour * 60 + quietHours.endMinute

        return if (startTime < endTime) {
            currentTime in startTime..endTime
        } else {
            // Quiet hours span midnight
            currentTime >= startTime || currentTime <= endTime
        }
    }

    /**
     * Create notification channels
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    "failures",
                    "Build Failures",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for build failures"
                    if (preferences.soundEnabled) {
                        setSound(
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                            null
                        )
                    }
                    enableVibration(preferences.vibrationEnabled)
                    enableLights(preferences.ledEnabled)
                },
                NotificationChannel(
                    "success",
                    "Build Success",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for successful builds"
                },
                NotificationChannel(
                    "warnings",
                    "High Risk Warnings",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "High-risk deployment warnings"
                    if (preferences.soundEnabled) {
                        setSound(
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                            null
                        )
                    }
                    enableVibration(preferences.vibrationEnabled)
                },
                NotificationChannel(
                    "default",
                    "General",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "General notifications"
                },
                NotificationChannel(
                    "custom",
                    "Custom Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Custom alert rules"
                }
            )

            channels.forEach { notificationManager.createNotificationChannel(it) }
        }
    }

    /**
     * Send notification
     */
    private fun sendNotification(
        title: String,
        message: String,
        channelId: String,
        priority: Int,
        data: Map<String, String> = emptyMap()
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Apply preferences
        if (preferences.soundEnabled && priority >= NotificationCompat.PRIORITY_HIGH) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }

        if (preferences.vibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 250, 250, 250))
        }

        if (preferences.ledEnabled) {
            builder.setLights(0xFF0000FF.toInt(), 1000, 1000)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())

        Timber.d("Notification sent: $title - $message")
    }

    /**
     * Cancel all notifications
     */
    fun cancelAll() {
        notificationManager.cancelAll()
    }

    /**
     * Cancel specific notification
     */
    fun cancel(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
}
