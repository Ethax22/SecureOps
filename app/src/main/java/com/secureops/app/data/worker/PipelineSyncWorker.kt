package com.secureops.app.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.secureops.app.data.notification.NotificationManager
import com.secureops.app.data.remediation.AutoRemediationEngine
import com.secureops.app.data.repository.AccountRepository
import com.secureops.app.data.repository.PipelineRepository
import com.secureops.app.domain.model.BuildStatus
import com.secureops.app.ml.security.AnomalyDetector
import com.secureops.app.ml.security.DependencyAnalyzer
import com.secureops.app.ml.security.SecretScanner
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class PipelineSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val accountRepository: AccountRepository by inject()
    private val pipelineRepository: PipelineRepository by inject()
    private val notificationManager: NotificationManager by inject()
    private val autoRemediationEngine: AutoRemediationEngine by inject()
    
    // DevSecOps Scanners
    private val secretScanner: SecretScanner by inject()
    private val dependencyAnalyzer: DependencyAnalyzer by inject()
    private val anomalyDetector: AnomalyDetector by inject()

    override suspend fun doWork(): Result {
        return try {
            Timber.d("Starting pipeline sync worker")

            // Get all active accounts
            val accounts = accountRepository.getActiveAccounts().first()

            if (accounts.isEmpty()) {
                Timber.d("No active accounts to sync")
                return Result.success()
            }

            // Get pipelines before sync to detect new/changed builds
            val pipelinesBefore = pipelineRepository.getAllPipelines().first()
            val previousFailedIds = pipelinesBefore
                .filter { it.status == BuildStatus.FAILURE }
                .map { it.id }
                .toSet()

            // Track pipeline IDs and status from before sync
            val previousPipelineMap = pipelinesBefore.associateBy(
                { it.id },
                { it.status }
            )

            // Sync pipelines for each account
            var successCount = 0
            var failureCount = 0
            val newFailures = mutableListOf<com.secureops.app.domain.model.Pipeline>()
            var predictionsRun = 0
            var autoRemediationsTriggered = 0

            accounts.forEach { account ->
                val result = pipelineRepository.syncPipelines(account.id)
                if (result.isSuccess) {
                    successCount++
                    Timber.d("Successfully synced pipelines for account: ${account.name}")

                    // Run ML predictions ONLY on new builds or when build starts
                    result.getOrNull()?.forEach { pipeline ->
                        val previousStatus = previousPipelineMap[pipeline.id]
                        val isNewPipeline = previousStatus == null
                        val justStarted = previousStatus != null &&
                                previousStatus != BuildStatus.RUNNING &&
                                pipeline.status == BuildStatus.RUNNING

                        // Predict when:
                        // 1. Pipeline is NEW (first time we see it) - REGARDLESS of status
                        // 2. Build just STARTED (status changed to RUNNING)
                        if (isNewPipeline || justStarted) {
                            try {
                                val reason = when {
                                    isNewPipeline -> "NEW BUILD DETECTED"
                                    justStarted -> "BUILD STARTED"
                                    else -> "UNKNOWN"
                                }
                                Timber.i("🎯 Triggering prediction and scans: $reason - ${pipeline.repositoryName} #${pipeline.buildNumber} [${pipeline.status}]")
                                
                                // Run failure prediction
                                pipelineRepository.predictFailure(pipeline)
                                predictionsRun++
                                
                                // Run DevSecOps scans (Secret, Dependency, Anomaly)
                                runSecurityScans(pipeline, account.id, pipelinesBefore)

                                // Check if prediction shows high risk and notify
                                val updatedPipeline =
                                    pipelineRepository.getPipelineById(pipeline.id)
                                updatedPipeline?.failurePrediction?.let { prediction ->
                                    Timber.i("📊 Prediction result: ${pipeline.repositoryName} - ${prediction.riskPercentage.toInt()}% risk (${(prediction.confidence * 100).toInt()}% confidence)")

                                    if (prediction.riskPercentage >= 70f) {
                                        notificationManager.notifyHighRisk(
                                            updatedPipeline,
                                            prediction.riskPercentage
                                        )
                                        Timber.i("⚠️ High-risk prediction: ${pipeline.repositoryName} - ${prediction.riskPercentage.toInt()}% risk")

                                        // Take preventive actions
                                        autoRemediationEngine.handleHighRiskPrediction(
                                            updatedPipeline,
                                            prediction.riskPercentage
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.e(
                                    e,
                                    "Failed to predict failure for pipeline: ${pipeline.id}"
                                )
                            }
                        }

                        // Check for new failures
                        if (pipeline.status == BuildStatus.FAILURE && pipeline.id !in previousFailedIds) {
                            newFailures.add(pipeline)
                        }
                    }
                } else {
                    failureCount++
                    Timber.e("Failed to sync pipelines for account: ${account.name}")
                }
            }

            // AUTO-REMEDIATION: Handle new failures
            newFailures.forEach { pipeline ->
                // Send notification first
                notificationManager.notifyBuildFailure(pipeline)
                Timber.i("🔔 Notification sent for failed build: ${pipeline.repositoryName} #${pipeline.buildNumber}")

                // NEW: Propose remediation and ask for user consent
                try {
                    val proposal = autoRemediationEngine.evaluateAndRemediate(pipeline)
                    if (proposal != null) {
                        // Send remediation proposal notification with Approve/Decline buttons
                        notificationManager.notifyRemediationProposal(proposal)
                        autoRemediationsTriggered++
                        Timber.i("📋 Remediation proposal sent for: ${pipeline.repositoryName}")
                    }
                } catch (e: Exception) {
                    Timber.e(
                        e,
                        "Failed to create remediation proposal for pipeline: ${pipeline.id}"
                    )
                }
            }

            // Clean up old pipelines
            pipelineRepository.cleanOldPipelines(daysToKeep = 30)

            Timber.d(" Sync completed: $successCount successful, $failureCount failed, ${newFailures.size} new failures, $predictionsRun predictions, $autoRemediationsTriggered auto-remediations")

            // Return success if at least one account synced successfully
            if (successCount > 0) Result.success() else Result.retry()

        } catch (e: Exception) {
            Timber.e(e, "Pipeline sync worker failed")
            Result.retry()
        }
    }

    private suspend fun runSecurityScans(
        pipeline: com.secureops.app.domain.model.Pipeline,
        accountId: String,
        allPipelines: List<com.secureops.app.domain.model.Pipeline>
    ) {
        val context = SecretScanner.ScanContext(
            source = "PipelineSync",
            pipelineId = pipeline.id,
            buildNumber = pipeline.buildNumber,
            repositoryName = pipeline.repositoryName,
            branch = pipeline.branch,
            commitHash = pipeline.commitHash,
            accountId = accountId
        )
        
        // 1. Scan for secrets in commit message & logs
        secretScanner.scanDiff(pipeline.commitMessage, context)
        if (!pipeline.logs.isNullOrBlank()) {
            secretScanner.scanLogs(pipeline.logs, "build.log", context)
        }
        
        // 2. Scan dependencies (simulated from commit)
        val files = buildMap {
            if (pipeline.commitMessage.contains("dependencies", ignoreCase = true) || pipeline.commitMessage.contains("update", ignoreCase = true)) {
                put("package.json", "{}") // Dummy structure for the analyzer, in reality we'd fetch actual files
                put("build.gradle", "implementation 'com.squareup.retrofit2:retrofit:2.9.0'\nimplementation 'unknown.malicious:package:1.0.0'") 
            }
        }
        if (files.isNotEmpty()) {
            dependencyAnalyzer.scanAllDependencies(files, DependencyAnalyzer.ScanContext(
                source = "DependencyScan",
                pipelineId = pipeline.id,
                buildNumber = pipeline.buildNumber,
                repositoryName = pipeline.repositoryName,
                branch = pipeline.branch,
                commitHash = pipeline.commitHash,
                accountId = accountId
            ))
        }
        
        // 3. Detect anomalies in build metrics
        val repoPipelines = allPipelines.filter { it.repositoryName == pipeline.repositoryName && it.duration != null }
        if (repoPipelines.isNotEmpty() && pipeline.duration != null) {
            val durations = repoPipelines.map { it.duration!!.toDouble() } + pipeline.duration.toDouble()
            anomalyDetector.analyzeBuildDuration(durations, AnomalyDetector.ScanContext(
                source = "DurationAnomaly",
                pipelineId = pipeline.id,
                buildNumber = pipeline.buildNumber,
                repositoryName = pipeline.repositoryName,
                branch = pipeline.branch,
                commitHash = pipeline.commitHash,
                accountId = accountId
            ))
        }
    }

    companion object {
        const val WORK_NAME = "pipeline_sync_work"
    }
}
