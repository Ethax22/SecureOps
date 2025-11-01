package com.secureops.app.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.secureops.app.MainActivity
import com.secureops.app.R
import timber.log.Timber

class SecureOpsMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM Token refreshed: $token")
        // TODO: Send token to server if needed
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Timber.d("Message received from: ${message.from}")

        message.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "SecureOps",
                body = notification.body ?: "New pipeline event"
            )
        }

        message.data.isNotEmpty().let {
            handleDataPayload(message.data)
        }
    }

    private fun handleDataPayload(data: Map<String, String>) {
        val pipelineId = data["pipelineId"]
        val status = data["status"]
        val buildNumber = data["buildNumber"]

        Timber.d("Pipeline $pipelineId - Build #$buildNumber: $status")

        // Show notification based on status
        when (status) {
            "failure" -> {
                showNotification(
                    title = "❌ Build Failed",
                    body = "Build #$buildNumber has failed. Tap to see details.",
                    channelId = "failures"
                )
            }

            "success" -> {
                showNotification(
                    title = "✅ Build Successful",
                    body = "Build #$buildNumber completed successfully.",
                    channelId = "success"
                )
            }

            "high_risk" -> {
                showNotification(
                    title = "⚠️ High Risk Detected",
                    body = "Build #$buildNumber has high failure risk. Review recommended.",
                    channelId = "warnings"
                )
            }
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        channelId: String = "default"
    ) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelId.replaceFirstChar { it.uppercase() },
                when (channelId) {
                    "failures" -> NotificationManager.IMPORTANCE_HIGH
                    "warnings" -> NotificationManager.IMPORTANCE_HIGH
                    else -> NotificationManager.IMPORTANCE_DEFAULT
                }
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent for notification tap
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
