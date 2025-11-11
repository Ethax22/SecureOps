package com.secureops.app.data.remediation

import com.secureops.app.data.executor.RemediationExecutor
import com.secureops.app.data.repository.PipelineRepository
import com.secureops.app.domain.model.*
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.math.pow

/**
 * Auto-Remediation Engine
 * Evaluates failures and PROPOSES remediation actions that require user consent
 */
class AutoRemediationEngine(
    private val remediationExecutor: RemediationExecutor,
    private val pipelineRepository: PipelineRepository
) {

    // Configuration
    private val maxAutoRetries = 3
    private val autoRemediationEnabled = true // Can be made user-configurable

    // Store pending remediation actions awaiting user consent
    private val pendingActions = mutableMapOf<String, RemediationProposal>()

    /**
     * Main entry point: Evaluate a failed pipeline and PROPOSE auto-remediation
     * Actions are NOT executed automatically - user consent required
     */
    suspend fun evaluateAndRemediate(pipeline: Pipeline): RemediationProposal? {
        if (!autoRemediationEnabled) {
            Timber.d("Auto-remediation is disabled")
            return null
        }

        if (pipeline.status != BuildStatus.FAILURE) {
            Timber.d("Pipeline is not in FAILURE status, skipping auto-remediation")
            return null
        }

        Timber.i("🤖 Evaluating auto-remediation for: ${pipeline.repositoryName} #${pipeline.buildNumber}")

        return try {
            // Classify the type of failure
            val failureType = classifyFailure(pipeline)
            Timber.d("Failure classified as: $failureType")

            // Generate remediation proposal based on failure type
            val proposal = when (failureType) {
                FailureType.TRANSIENT -> proposeTransientFailureRemediation(pipeline)
                FailureType.FLAKY_TEST -> proposeFlakyTestRemediation(pipeline)
                FailureType.DEPLOYMENT -> proposeDeploymentFailureRemediation(pipeline)
                FailureType.TIMEOUT -> proposeTimeoutRemediation(pipeline)
                FailureType.RESOURCE_LIMIT -> proposeResourceLimitRemediation(pipeline)
                FailureType.PERMANENT -> proposePermanentFailureRemediation(pipeline)
                FailureType.UNKNOWN -> proposeUnknownFailureRemediation(pipeline)
            }

            // Store proposal for user approval
            if (proposal != null) {
                pendingActions[pipeline.id] = proposal
                Timber.i("📋 Remediation proposal created for ${pipeline.repositoryName} - awaiting user consent")
            }

            proposal

        } catch (e: Exception) {
            Timber.e(e, "Auto-remediation evaluation failed for pipeline: ${pipeline.id}")
            null
        }
    }

    /**
     * Execute a previously proposed remediation after user approval
     */
    suspend fun executeWithConsent(pipelineId: String, approved: Boolean): RemediationResult {
        val proposal = pendingActions[pipelineId]
        if (proposal == null) {
            Timber.w("No pending remediation found for pipeline: $pipelineId")
            return RemediationResult(
                success = false,
                message = "No pending remediation found",
                actionsTaken = emptyList()
            )
        }

        if (!approved) {
            Timber.i("❌ User declined remediation for: ${proposal.pipeline.repositoryName}")
            pendingActions.remove(pipelineId)
            return RemediationResult(
                success = false,
                message = "User declined remediation",
                actionsTaken = emptyList()
            )
        }

        Timber.i("✅ User approved remediation for: ${proposal.pipeline.repositoryName}")
        pendingActions.remove(pipelineId)

        // Execute the approved actions
        return executeRemediationActions(proposal)
    }

    /**
     * Execute remediation actions with user consent
     */
    private suspend fun executeRemediationActions(proposal: RemediationProposal): RemediationResult {
        val actionsTaken = mutableListOf<ActionResult>()
        var overallSuccess = true

        proposal.actions.forEachIndexed { index, action ->
            try {
                Timber.i("🔧 Executing action ${index + 1}/${proposal.actions.size}: ${action.description}")

                val result = remediationExecutor.executeRemediation(action)
                actionsTaken.add(result)

                if (!result.success) {
                    overallSuccess = false
                    Timber.w("⚠️ Action failed: ${action.description}")
                }

                // Add delay between actions for retries
                if (action.type == ActionType.RERUN_PIPELINE && index < proposal.actions.size - 1) {
                    val delaySeconds = 2.0.pow((index + 1).toDouble()).toLong()
                    Timber.d("Waiting ${delaySeconds}s before next retry")
                    delay(delaySeconds * 1000)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to execute action: ${action.description}")
                overallSuccess = false
            }
        }

        return RemediationResult(
            success = overallSuccess,
            message = if (overallSuccess)
                "All remediation actions completed successfully"
            else
                "Some remediation actions failed",
            actionsTaken = actionsTaken
        )
    }

    /**
     * Classify failure type based on logs and context
     */
    private suspend fun classifyFailure(pipeline: Pipeline): FailureType {
        val logs = try {
            pipelineRepository.fetchBuildLogs(pipeline).getOrNull() ?: ""
        } catch (e: Exception) {
            ""
        }

        val logsLower = logs.lowercase()

        return when {
            // Transient failures (network, temporary service issues)
            logsLower.contains("connection refused") ||
                    logsLower.contains("connection timed out") ||
                    logsLower.contains("temporarily unavailable") ||
                    logsLower.contains("503 service unavailable") ||
                    logsLower.contains("502 bad gateway") -> {
                Timber.d("Detected transient failure (network/service)")
                FailureType.TRANSIENT
            }

            // Timeout failures
            logsLower.contains("timeout") ||
                    logsLower.contains("timed out") -> {
                Timber.d("Detected timeout failure")
                FailureType.TIMEOUT
            }

            // Flaky tests
            logsLower.contains("flaky") ||
                    (logsLower.contains("test") && logsLower.contains("intermittent")) -> {
                Timber.d("Detected flaky test")
                FailureType.FLAKY_TEST
            }

            // Resource limits (memory, disk space)
            logsLower.contains("out of memory") ||
                    logsLower.contains("oom") ||
                    logsLower.contains("no space left") ||
                    logsLower.contains("disk full") -> {
                Timber.d("Detected resource limit failure")
                FailureType.RESOURCE_LIMIT
            }

            // Deployment failures
            logsLower.contains("deployment") && logsLower.contains("failed") ||
                    logsLower.contains("rollout failed") -> {
                Timber.d("Detected deployment failure")
                FailureType.DEPLOYMENT
            }

            // Compilation/build errors (usually permanent)
            logsLower.contains("compilation failed") ||
                    logsLower.contains("build failed") ||
                    logsLower.contains("syntax error") -> {
                Timber.d("Detected permanent failure (compilation)")
                FailureType.PERMANENT
            }

            else -> FailureType.UNKNOWN
        }
    }

    /**
     * Propose remediation for transient failures
     */
    private fun proposeTransientFailureRemediation(pipeline: Pipeline): RemediationProposal {
        Timber.i("🔄 Proposing retry for transient failure")

        val actions = (1..maxAutoRetries).map { attempt ->
            RemediationAction(
                id = "auto-retry-${pipeline.id}-$attempt",
                type = ActionType.RERUN_PIPELINE,
                pipeline = pipeline,
                description = "Retry build (attempt $attempt/$maxAutoRetries) - Transient failure detected",
                requiresConfirmation = true,
                parameters = mapOf(
                    "auto_retry" to "true",
                    "attempt" to attempt.toString(),
                    "reason" to "Transient network/service failure"
                )
            )
        }

        return RemediationProposal(
            pipeline = pipeline,
            failureType = "Transient Failure",
            reason = "Network or service temporarily unavailable. Retrying may resolve the issue.",
            actions = actions,
            severity = RemediationSeverity.LOW,
            confidence = 0.85f,
            estimatedTime = "2-5 minutes"
        )
    }

    /**
     * Propose remediation for flaky tests
     */
    private fun proposeFlakyTestRemediation(pipeline: Pipeline): RemediationProposal {
        Timber.i("🧪 Proposing retry for flaky test")

        val action = RemediationAction(
            id = "flaky-retry-${pipeline.id}",
            type = ActionType.RERUN_PIPELINE,
            pipeline = pipeline,
            description = "Re-run flaky tests",
            requiresConfirmation = true,
            parameters = mapOf(
                "auto_retry" to "true",
                "reason" to "Flaky test detected"
            )
        )

        return RemediationProposal(
            pipeline = pipeline,
            failureType = "Flaky Test",
            reason = "Intermittent test failure detected. Re-running may pass the build.",
            actions = listOf(action),
            severity = RemediationSeverity.LOW,
            confidence = 0.7f,
            estimatedTime = "1-3 minutes"
        )
    }

    /**
     * Propose remediation for deployment failures
     */
    private fun proposeDeploymentFailureRemediation(pipeline: Pipeline): RemediationProposal {
        Timber.i("🚨 Proposing rollback for deployment failure")

        val action = RemediationAction(
            id = "rollback-${pipeline.id}",
            type = ActionType.ROLLBACK_DEPLOYMENT,
            pipeline = pipeline,
            description = "Rollback to last successful deployment",
            requiresConfirmation = true,
            parameters = mapOf(
                "reason" to "Deployment failure - rollback recommended"
            )
        )

        return RemediationProposal(
            pipeline = pipeline,
            failureType = "Deployment Failure",
            reason = "Deployment failed. Rolling back to the last stable version is recommended to restore service.",
            actions = listOf(action),
            severity = RemediationSeverity.CRITICAL,
            confidence = 0.95f,
            estimatedTime = "3-10 minutes",
            warning = "⚠️ This will revert your deployment to the previous version"
        )
    }

    /**
     * Propose remediation for timeout failures
     */
    private fun proposeTimeoutRemediation(pipeline: Pipeline): RemediationProposal {
        Timber.i("⏱️ Proposing retry for timeout")

        val actions = (1..2).map { attempt ->
            RemediationAction(
                id = "timeout-retry-${pipeline.id}-$attempt",
                type = ActionType.RERUN_PIPELINE,
                pipeline = pipeline,
                description = "Retry build with extended timeout (attempt $attempt/2)",
                requiresConfirmation = true,
                parameters = mapOf(
                    "auto_retry" to "true",
                    "attempt" to attempt.toString(),
                    "reason" to "Timeout - may succeed on retry"
                )
            )
        }

        return RemediationProposal(
            pipeline = pipeline,
            failureType = "Timeout",
            reason = "Build timed out. The issue may be transient or the build may need optimization.",
            actions = actions,
            severity = RemediationSeverity.MEDIUM,
            confidence = 0.6f,
            estimatedTime = "5-15 minutes"
        )
    }

    /**
     * Propose remediation for resource limit failures
     */
    private fun proposeResourceLimitRemediation(pipeline: Pipeline): RemediationProposal {
        Timber.i("💾 Resource limit detected - manual intervention needed")

        return RemediationProposal(
            pipeline = pipeline,
            failureType = "Resource Limit",
            reason = "Build failed due to resource limits (memory/disk). Manual configuration changes may be required.",
            actions = emptyList(),
            severity = RemediationSeverity.HIGH,
            confidence = 0.9f,
            estimatedTime = "Manual intervention required",
            warning = "⚠️ This requires infrastructure changes. Consider:\n" +
                    "• Increasing memory allocation\n" +
                    "• Adding disk space\n" +
                    "• Optimizing resource usage"
        )
    }

    /**
     * Propose remediation for permanent failures
     */
    private fun proposePermanentFailureRemediation(pipeline: Pipeline): RemediationProposal {
        Timber.i("❌ Permanent failure - code fix needed")

        return RemediationProposal(
            pipeline = pipeline,
            failureType = "Permanent Failure",
            reason = "Build failed due to compilation or code errors. Code changes required to fix.",
            actions = emptyList(),
            severity = RemediationSeverity.HIGH,
            confidence = 0.95f,
            estimatedTime = "Requires code fix",
            warning = "⚠️ Auto-remediation not possible. Please review and fix the code errors."
        )
    }

    /**
     * Propose remediation for unknown failures
     */
    private fun proposeUnknownFailureRemediation(pipeline: Pipeline): RemediationProposal {
        Timber.i("❓ Unknown failure type - suggesting conservative retry")

        val action = RemediationAction(
            id = "unknown-retry-${pipeline.id}",
            type = ActionType.RERUN_PIPELINE,
            pipeline = pipeline,
            description = "Retry build (unknown failure type)",
            requiresConfirmation = true,
            parameters = mapOf(
                "auto_retry" to "true",
                "reason" to "Unknown failure - conservative retry"
            )
        )

        return RemediationProposal(
            pipeline = pipeline,
            failureType = "Unknown",
            reason = "Unable to classify failure type. A single retry may help if the issue was transient.",
            actions = listOf(action),
            severity = RemediationSeverity.MEDIUM,
            confidence = 0.5f,
            estimatedTime = "2-5 minutes"
        )
    }

    /**
     * Preventive actions for high-risk predictions
     */
    suspend fun handleHighRiskPrediction(pipeline: Pipeline, riskPercentage: Float) {
        Timber.i("⚠️ High-risk prediction: ${riskPercentage.toInt()}% for ${pipeline.repositoryName}")

        when {
            riskPercentage >= 90f -> {
                // Critical risk - consider blocking deployment
                Timber.w("🚨 CRITICAL RISK (${riskPercentage.toInt()}%): Consider blocking deployment")
                // Could implement deployment hold here
            }

            riskPercentage >= 80f -> {
                // High risk - alert team
                Timber.w("⚠️ HIGH RISK (${riskPercentage.toInt()}%): Increased monitoring recommended")
                // Send alert to team
            }

            riskPercentage >= 70f -> {
                // Moderate risk - notify
                Timber.i("ℹ️ MODERATE RISK (${riskPercentage.toInt()}%): Watch closely")
            }
        }
    }

    /**
     * Remediation proposal awaiting user consent
     */
    data class RemediationProposal(
        val pipeline: Pipeline,
        val failureType: String,
        val reason: String,
        val actions: List<RemediationAction>,
        val severity: RemediationSeverity,
        val confidence: Float,
        val estimatedTime: String,
        val warning: String? = null
    )

    /**
     * Result of executing remediation actions
     */
    data class RemediationResult(
        val success: Boolean,
        val message: String,
        val actionsTaken: List<ActionResult>
    )

    /**
     * Severity levels for remediation proposals
     */
    enum class RemediationSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}

/**
 * Failure classification types
 */
enum class FailureType {
    TRANSIENT,      // Network issues, temporary service unavailability
    FLAKY_TEST,     // Intermittent test failures
    DEPLOYMENT,     // Deployment/rollout failures
    TIMEOUT,        // Timeout issues
    RESOURCE_LIMIT, // Memory, disk space issues
    PERMANENT,      // Compilation errors, code issues
    UNKNOWN         // Cannot determine type
}
