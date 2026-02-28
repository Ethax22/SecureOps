package com.secureops.app.data.worker

import android.content.Context
import androidx.work.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * WorkManager Initializer
 * 
 * Schedules periodic background tasks for SecureOps
 */
object WorkManagerInitializer {
    
    /**
     * Initialize all background workers
     */
    fun initialize(context: Context) {
        // Schedule remediation outcome tracker (every 15 minutes)
        scheduleRemediationOutcomeTracker(context)
        
        // Schedule pipeline sync worker (if not already scheduled)
        schedulePipelineSync(context)
        
        Timber.i("WorkManager initialized - all workers scheduled")
    }
    
    /**
     * Schedule remediation outcome tracker
     * Runs every 15 minutes to check remediation results
     */
    private fun scheduleRemediationOutcomeTracker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<RemediationOutcomeTrackerWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(RemediationOutcomeTrackerWorker.TAG)
            .setInitialDelay(2, TimeUnit.MINUTES) // Start after 2 minutes
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            RemediationOutcomeTrackerWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        
        Timber.i("Scheduled RemediationOutcomeTrackerWorker (every 15 minutes)")
    }
    
    /**
     * Schedule pipeline sync worker
     * Runs every 30 minutes to sync pipeline data
     */
    private fun schedulePipelineSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<PipelineSyncWorker>(
            30, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag("PipelineSync")
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "pipeline_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        
        Timber.i("Scheduled PipelineSyncWorker (every 30 minutes)")
    }
    
    /**
     * Trigger immediate remediation outcome check
     */
    fun triggerImmediateRemediationCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val workRequest = OneTimeWorkRequestBuilder<RemediationOutcomeTrackerWorker>()
            .setConstraints(constraints)
            .addTag(RemediationOutcomeTrackerWorker.TAG)
            .build()
        
        WorkManager.getInstance(context).enqueue(workRequest)
        
        Timber.d("Triggered immediate remediation outcome check")
    }
    
    /**
     * Cancel all workers (useful for testing or cleanup)
     */
    fun cancelAllWorkers(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
        Timber.w("Cancelled all WorkManager tasks")
    }
    
    /**
     * Get worker status info
     */
    fun getWorkerInfo(context: Context): String {
        val workManager = WorkManager.getInstance(context)
        
        val remediationWork = workManager
            .getWorkInfosForUniqueWork(RemediationOutcomeTrackerWorker.WORK_NAME)
            .get()
        
        val pipelineWork = workManager
            .getWorkInfosForUniqueWork("pipeline_sync")
            .get()
        
        return buildString {
            appendLine("WorkManager Status:")
            appendLine("- Remediation Tracker: ${remediationWork.firstOrNull()?.state}")
            appendLine("- Pipeline Sync: ${pipelineWork.firstOrNull()?.state}")
        }
    }
}
