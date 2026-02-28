package com.secureops.app.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.secureops.app.data.local.dao.RemediationHistoryDao
import com.secureops.app.data.remediation.RemediationLearner
import com.secureops.app.data.repository.PipelineRepository
import com.secureops.app.domain.model.BuildStatus
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Remediation Outcome Tracker Worker
 * 
 * Runs every 15 minutes to track outcomes of remediation attempts
 * Updates remediation history with actual build results
 */
class RemediationOutcomeTrackerWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val remediationHistoryDao: RemediationHistoryDao by inject()
    private val remediationLearner: RemediationLearner by inject()
    private val pipelineRepository: PipelineRepository by inject()

    companion object {
        const val WORK_NAME = "remediation_outcome_tracker"
        const val TAG = "RemediationTracker"
    }

    override suspend fun doWork(): Result {
        return try {
            Timber.d("[$TAG] Starting remediation outcome tracking")

            // Get pending remediations (not yet completed)
            val pendingRemediations = remediationHistoryDao.getPendingRemediations().first()

            if (pendingRemediations.isEmpty()) {
                Timber.d("[$TAG] No pending remediations to track")
                return Result.success()
            }

            Timber.i("[$TAG] Tracking ${pendingRemediations.size} pending remediations")

            var successfullyTracked = 0
            var failedToTrack = 0

            // Check each pending remediation
            for (remediation in pendingRemediations) {
                try {
                    trackRemediationOutcome(remediation)
                    successfullyTracked++
                } catch (e: Exception) {
                    Timber.e(e, "[$TAG] Failed to track remediation ${remediation.id}")
                    failedToTrack++
                }
            }

            Timber.i("[$TAG] Tracking complete: $successfullyTracked tracked, $failedToTrack failed")

            // Generate periodic statistics
            generateStatistics()

            Result.success()

        } catch (e: Exception) {
            Timber.e(e, "[$TAG] Remediation outcome tracking failed")
            Result.retry()
        }
    }

    /**
     * Track the outcome of a specific remediation attempt
     */
    private suspend fun trackRemediationOutcome(
        remediation: com.secureops.app.data.local.entity.RemediationHistoryEntity
    ) {
        // Get the latest pipeline state
        val pipelines = pipelineRepository.getAllPipelines().first()
        val pipeline = pipelines.find { it.id == remediation.pipelineId }

        if (pipeline == null) {
            Timber.w("[$TAG] Pipeline ${remediation.pipelineId} not found, skipping")
            return
        }

        // Check if remediation has been running for too long (timeout after 30 minutes)
        val elapsedTime = System.currentTimeMillis() - remediation.attemptedAt
        val timeoutMs = 30 * 60 * 1000L // 30 minutes

        if (elapsedTime > timeoutMs) {
            // Timeout - mark as failed
            Timber.w("[$TAG] Remediation ${remediation.id} timed out")
            remediationLearner.recordOutcome(
                historyId = remediation.id,
                wasSuccessful = false,
                outcome = "TIMEOUT",
                remediatedBuildNumber = pipeline.buildNumber,
                errorMessage = "Remediation timed out after 30 minutes",
                durationMs = elapsedTime
            )
            return
        }

        // Check if a new build has been triggered
        if (pipeline.buildNumber > remediation.buildNumber) {
            // New build detected - check its status
            when (pipeline.status) {
                BuildStatus.SUCCESS -> {
                    // Remediation successful!
                    Timber.i("[$TAG] Remediation ${remediation.id} succeeded - build #${pipeline.buildNumber} passed")
                    remediationLearner.recordOutcome(
                        historyId = remediation.id,
                        wasSuccessful = true,
                        outcome = "SUCCESS",
                        remediatedBuildNumber = pipeline.buildNumber,
                        errorMessage = null,
                        durationMs = elapsedTime
                    )
                }
                
                BuildStatus.FAILURE -> {
                    // Remediation failed - new build also failed
                    Timber.w("[$TAG] Remediation ${remediation.id} failed - build #${pipeline.buildNumber} failed")
                    remediationLearner.recordOutcome(
                        historyId = remediation.id,
                        wasSuccessful = false,
                        outcome = "FAILURE",
                        remediatedBuildNumber = pipeline.buildNumber,
                        errorMessage = "Remediation did not resolve the issue",
                        durationMs = elapsedTime
                    )
                }
                
                BuildStatus.RUNNING, BuildStatus.PENDING, BuildStatus.QUEUED, BuildStatus.UNKNOWN -> {
                    // Still running - check again later
                    Timber.d("[$TAG] Remediation ${remediation.id} - build #${pipeline.buildNumber} still running/unknown")
                }
                
                BuildStatus.CANCELED -> {
                    // Build was cancelled
                    Timber.i("[$TAG] Remediation ${remediation.id} - build #${pipeline.buildNumber} was cancelled")
                    remediationLearner.recordOutcome(
                        historyId = remediation.id,
                        wasSuccessful = false,
                        outcome = "CANCELLED",
                        remediatedBuildNumber = pipeline.buildNumber,
                        errorMessage = "Build was cancelled",
                        durationMs = elapsedTime
                    )
                }
                
                BuildStatus.SKIPPED -> {
                    // Partial success
                    Timber.i("[$TAG] Remediation ${remediation.id} - build #${pipeline.buildNumber} is skipped/unstable")
                    remediationLearner.recordOutcome(
                        historyId = remediation.id,
                        wasSuccessful = true, // Consider unstable as partially successful
                        outcome = "PARTIAL",
                        remediatedBuildNumber = pipeline.buildNumber,
                        errorMessage = "Build completed but is unstable/skipped",
                        durationMs = elapsedTime
                    )
                }
            }
        } else if (pipeline.buildNumber == remediation.buildNumber) {
            // Same build number - check if status changed from FAILURE
            if (pipeline.status == BuildStatus.SUCCESS || pipeline.status == BuildStatus.SKIPPED) {
                // Build recovered without new trigger (rare but possible)
                Timber.i("[$TAG] Remediation ${remediation.id} - original build recovered")
                remediationLearner.recordOutcome(
                    historyId = remediation.id,
                    wasSuccessful = true,
                    outcome = "RECOVERED",
                    remediatedBuildNumber = pipeline.buildNumber,
                    errorMessage = null,
                    durationMs = elapsedTime
                )
            }
        }
    }

    /**
     * Generate and log periodic statistics
     */
    private suspend fun generateStatistics() {
        try {
            val stats = remediationLearner.getStatistics()
            if (stats != null) {
                Timber.i("[$TAG] === Remediation Statistics ===")
                Timber.i("[$TAG] Total Attempts: ${stats.totalAttempts}")
                Timber.i("[$TAG] Successful: ${stats.successfulAttempts}")
                Timber.i("[$TAG] Failed: ${stats.failedAttempts}")
                Timber.i("[$TAG] Success Rate: ${String.format("%.1f%%", stats.overallSuccessRate * 100)}")
                Timber.i("[$TAG] Avg Duration: ${stats.avgDurationMs / 1000}s")
            }
        } catch (e: Exception) {
            Timber.e(e, "[$TAG] Failed to generate statistics")
        }
    }

    /**
     * Clean up old completed remediations (optional - could run less frequently)
     */
    private suspend fun cleanupOldHistory() {
        try {
            // Delete history older than 90 days
            val cutoffTime = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
            val deleted = remediationHistoryDao.deleteOldHistory(cutoffTime)
            if (deleted > 0) {
                Timber.i("[$TAG] Cleaned up $deleted old remediation records")
            }
        } catch (e: Exception) {
            Timber.e(e, "[$TAG] Failed to cleanup old history")
        }
    }
}
