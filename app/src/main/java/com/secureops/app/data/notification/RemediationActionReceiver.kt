package com.secureops.app.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.secureops.app.data.remediation.AutoRemediationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Broadcast receiver for remediation action buttons in notifications
 */
class RemediationActionReceiver : BroadcastReceiver(), KoinComponent {

    private val autoRemediationEngine: AutoRemediationEngine by inject()
    private val notificationManager: NotificationManager by inject()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val pipelineId = intent.getStringExtra("pipelineId") ?: return
        val action = intent.action ?: return

        Timber.d("Remediation action received: $action for pipeline: $pipelineId")

        when (action) {
            "com.secureops.app.APPROVE_REMEDIATION" -> {
                Timber.i("✅ User approved remediation for pipeline: $pipelineId")
                executeRemediation(pipelineId, approved = true)
            }

            "com.secureops.app.DECLINE_REMEDIATION" -> {
                Timber.i("❌ User declined remediation for pipeline: $pipelineId")
                executeRemediation(pipelineId, approved = false)
            }
        }

        // Dismiss the notification after action
        notificationManager.dismissRemediationNotification(pipelineId)
    }

    private fun executeRemediation(pipelineId: String, approved: Boolean) {
        // Use coroutine scope to execute async operation
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = autoRemediationEngine.executeWithConsent(pipelineId, approved)

                if (approved && result.success) {
                    Timber.i("✅ Remediation completed successfully")
                } else if (approved) {
                    Timber.w("⚠️ Remediation failed: ${result.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error executing remediation")
            }
        }
    }
}
