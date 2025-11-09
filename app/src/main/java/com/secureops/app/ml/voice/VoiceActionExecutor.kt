package com.secureops.app.ml.voice

import com.secureops.app.data.executor.RemediationExecutor
import com.secureops.app.data.repository.PipelineRepository
import com.secureops.app.data.repository.AccountRepository
import com.secureops.app.data.analytics.AnalyticsRepository
import com.secureops.app.domain.model.*
import com.secureops.app.ml.VoiceCommandProcessor
import kotlinx.coroutines.flow.first
import timber.log.Timber

class VoiceActionExecutor(
    private val pipelineRepository: PipelineRepository,
    private val voiceProcessor: VoiceCommandProcessor,
    private val remediationExecutor: RemediationExecutor,
    private val ttsManager: TextToSpeechManager,
    private val accountRepository: AccountRepository,
    private val analyticsRepository: AnalyticsRepository
) {

    /**
     * Process voice input and execute the command
     */
    suspend fun processAndExecute(voiceInput: String): VoiceExecutionResult {
        try {
            // Parse voice command
            val command = voiceProcessor.processVoiceInput(voiceInput)

            Timber.d("Voice command: ${command.intent}, params: ${command.parameters}")

            // Execute command
            val result = executeCommand(command)

            // Speak response
            ttsManager.speak(result.spokenResponse)

            return result
        } catch (e: Exception) {
            Timber.e(e, "Failed to process voice command")
            val errorResponse = "I encountered an error processing your request. ${e.message}"
            ttsManager.speak(errorResponse)
            return VoiceExecutionResult(
                success = false,
                message = errorResponse,
                spokenResponse = errorResponse
            )
        }
    }

    /**
     * Execute a parsed voice command
     */
    private suspend fun executeCommand(command: VoiceCommand): VoiceExecutionResult {
        return when (command.intent) {
            CommandIntent.QUERY_BUILD_STATUS -> queryBuildStatus(command)
            CommandIntent.EXPLAIN_FAILURE -> explainFailure(command)
            CommandIntent.CHECK_RISKY_DEPLOYMENTS -> checkRiskyDeployments(command)
            CommandIntent.RERUN_BUILD -> rerunBuild(command)
            CommandIntent.ROLLBACK_DEPLOYMENT -> rollbackDeployment(command)
            CommandIntent.NOTIFY_TEAM -> notifyTeam(command)
            CommandIntent.QUERY_ANALYTICS -> queryAnalytics(command)
            CommandIntent.LIST_REPOSITORIES -> listRepositories(command)
            CommandIntent.QUERY_REPOSITORY -> queryRepository(command)
            CommandIntent.LIST_ACCOUNTS -> listAccounts(command)
            CommandIntent.QUERY_ACCOUNT -> queryAccount(command)
            CommandIntent.ADD_ACCOUNT -> addAccountHelp(command)
            CommandIntent.SHOW_HELP -> showHelp(command)
            CommandIntent.GREETING -> greeting(command)
            CommandIntent.QUERY_DEPLOYMENT -> queryDeployment(command)
            CommandIntent.QUERY_BRANCH -> queryBranch(command)
            CommandIntent.QUERY_DURATION -> queryDuration(command)
            CommandIntent.QUERY_SUCCESS_RATE -> querySuccessRate(command)
            CommandIntent.QUERY_COMMIT -> queryCommit(command)
            CommandIntent.UNKNOWN -> VoiceExecutionResult(
                success = false,
                message = "I didn't understand that command",
                spokenResponse = "I didn't understand that command. Try asking about build status or pipeline failures."
            )
        }
    }

    /**
     * Query current build status
     */
    private suspend fun queryBuildStatus(command: VoiceCommand): VoiceExecutionResult {
        val pipelines = pipelineRepository.getAllPipelines().first()

        val totalBuilds = pipelines.size
        val failedBuilds = pipelines.count { it.status == BuildStatus.FAILURE }
        val runningBuilds = pipelines.count { it.status == BuildStatus.RUNNING }
        val successBuilds = pipelines.count { it.status == BuildStatus.SUCCESS }

        val response = voiceProcessor.generateResponse(
            CommandIntent.QUERY_BUILD_STATUS,
            mapOf(
                "totalBuilds" to totalBuilds,
                "failedBuilds" to failedBuilds,
                "runningBuilds" to runningBuilds,
                "successBuilds" to successBuilds
            )
        )

        return VoiceExecutionResult(
            success = true,
            message = response,
            spokenResponse = response,
            data = mapOf(
                "total" to totalBuilds,
                "failed" to failedBuilds,
                "running" to runningBuilds,
                "success" to successBuilds
            )
        )
    }

    /**
     * Explain a pipeline failure
     */
    private suspend fun explainFailure(command: VoiceCommand): VoiceExecutionResult {
        val buildNumber = command.parameters["buildNumber"]
        val pipelines = pipelineRepository.getAllPipelines().first()

        val failedPipeline = if (buildNumber != null) {
            pipelines.find { it.buildNumber.toString() == buildNumber && it.status == BuildStatus.FAILURE }
        } else {
            pipelines.lastOrNull { it.status == BuildStatus.FAILURE }
        }

        if (failedPipeline == null) {
            val response = "I couldn't find any failed builds to explain."
            return VoiceExecutionResult(
                success = false,
                message = response,
                spokenResponse = response
            )
        }

        // Use existing root cause analysis
        val explanation =
            "Build ${failedPipeline.buildNumber} failed in ${failedPipeline.repositoryName}. " +
                    "The failure occurred during the build process. Check the logs for more details."

        return VoiceExecutionResult(
            success = true,
            message = explanation,
            spokenResponse = explanation,
            data = mapOf("pipeline" to failedPipeline)
        )
    }

    /**
     * Check for risky deployments
     */
    private suspend fun checkRiskyDeployments(command: VoiceCommand): VoiceExecutionResult {
        val riskyPipelines = pipelineRepository.getHighRiskPipelines(threshold = 70f).first()

        val response = voiceProcessor.generateResponse(
            CommandIntent.CHECK_RISKY_DEPLOYMENTS,
            mapOf("riskyCount" to riskyPipelines.size)
        )

        return VoiceExecutionResult(
            success = true,
            message = response,
            spokenResponse = response,
            data = mapOf("riskyPipelines" to riskyPipelines)
        )
    }

    /**
     * Rerun a build
     */
    private suspend fun rerunBuild(command: VoiceCommand): VoiceExecutionResult {
        val buildNumber = command.parameters["buildNumber"]
        val target = command.parameters["target"]

        val pipelines = pipelineRepository.getAllPipelines().first()

        val pipelineToRerun = when {
            buildNumber != null -> pipelines.find { it.buildNumber.toString() == buildNumber }
            target == "last_failed" -> pipelines.lastOrNull { it.status == BuildStatus.FAILURE }
            else -> pipelines.lastOrNull()
        }

        if (pipelineToRerun == null) {
            val response = "I couldn't find a build to rerun."
            return VoiceExecutionResult(
                success = false,
                message = response,
                spokenResponse = response
            )
        }

        // Execute rerun action
        val action = RemediationAction(
            id = "voice_rerun_${System.currentTimeMillis()}",
            type = ActionType.RERUN_PIPELINE,
            pipeline = pipelineToRerun,
            description = "Rerun pipeline via voice command",
            requiresConfirmation = false
        )

        val actionResult = remediationExecutor.executeRemediation(action)

        val response = voiceProcessor.generateResponse(
            CommandIntent.RERUN_BUILD,
            mapOf("buildNumber" to pipelineToRerun.buildNumber.toString()),
            success = actionResult.success
        )

        return VoiceExecutionResult(
            success = actionResult.success,
            message = response,
            spokenResponse = response,
            data = mapOf("pipeline" to pipelineToRerun, "result" to actionResult)
        )
    }

    /**
     * Rollback deployment
     */
    private suspend fun rollbackDeployment(command: VoiceCommand): VoiceExecutionResult {
        val pipelines = pipelineRepository.getAllPipelines().first()
        val lastPipeline = pipelines.lastOrNull()

        if (lastPipeline == null) {
            val response = "I couldn't find any deployments to rollback."
            return VoiceExecutionResult(
                success = false,
                message = response,
                spokenResponse = response
            )
        }

        val action = RemediationAction(
            id = "voice_rollback_${System.currentTimeMillis()}",
            type = ActionType.ROLLBACK_DEPLOYMENT,
            pipeline = lastPipeline,
            description = "Rollback deployment via voice command",
            requiresConfirmation = true
        )

        val actionResult = remediationExecutor.executeRemediation(action)

        val response = voiceProcessor.generateResponse(
            CommandIntent.ROLLBACK_DEPLOYMENT,
            emptyMap(),
            success = actionResult.success
        )

        return VoiceExecutionResult(
            success = actionResult.success,
            message = response,
            spokenResponse = response,
            data = mapOf("pipeline" to lastPipeline, "result" to actionResult)
        )
    }

    /**
     * Notify team
     */
    private suspend fun notifyTeam(command: VoiceCommand): VoiceExecutionResult {
        val channel = command.parameters["channel"] ?: "team"

        val response = voiceProcessor.generateResponse(
            CommandIntent.NOTIFY_TEAM,
            mapOf("channel" to channel),
            success = true
        )

        return VoiceExecutionResult(
            success = true,
            message = response,
            spokenResponse = response
        )
    }

    /**
     * Query analytics data
     */
    private suspend fun queryAnalytics(command: VoiceCommand): VoiceExecutionResult {
        val pipelines = pipelineRepository.getAllPipelines().first()

        val totalBuilds = pipelines.size
        val failedBuilds = pipelines.count { it.status == BuildStatus.FAILURE }
        val successBuilds = pipelines.count { it.status == BuildStatus.SUCCESS }
        val failureRate = if (totalBuilds > 0) (failedBuilds.toFloat() / totalBuilds * 100) else 0f

        val avgDuration = pipelines.mapNotNull { it.duration }.average().toLong()

        val timeRange = command.parameters["timeRange"] ?: "all time"

        val response = voiceProcessor.generateResponse(
            CommandIntent.QUERY_ANALYTICS,
            mapOf(
                "totalBuilds" to totalBuilds,
                "failureRate" to failureRate,
                "avgDuration" to avgDuration,
                "timeRange" to timeRange
            )
        )

        return VoiceExecutionResult(
            success = true,
            message = response,
            spokenResponse = response,
            data = mapOf(
                "totalBuilds" to totalBuilds,
                "failedBuilds" to failedBuilds,
                "successBuilds" to successBuilds,
                "failureRate" to failureRate,
                "avgDuration" to avgDuration
            )
        )
    }

    /**
     * List all repositories
     */
    private suspend fun listRepositories(command: VoiceCommand): VoiceExecutionResult {
        val pipelines = pipelineRepository.getAllPipelines().first()
        val repositories = pipelines.map { it.repositoryName }.distinct().sorted()

        val response = voiceProcessor.generateResponse(
            CommandIntent.LIST_REPOSITORIES,
            mapOf(
                "repositories" to repositories,
                "count" to repositories.size
            )
        )

        return VoiceExecutionResult(
            success = true,
            message = response,
            spokenResponse = response,
            data = mapOf("repositories" to repositories)
        )
    }

    /**
     * Query specific repository
     */
    private suspend fun queryRepository(command: VoiceCommand): VoiceExecutionResult {
        val repoName = command.parameters["repository"]
        val pipelines = pipelineRepository.getAllPipelines().first()

        val repoBuilds = if (repoName != null) {
            pipelines.filter { it.repositoryName.contains(repoName, ignoreCase = true) }
        } else {
            pipelines
        }

        val buildCount = repoBuilds.size
        val failedBuilds = repoBuilds.count { it.status == BuildStatus.FAILURE }
        val failureRate = if (buildCount > 0) (failedBuilds.toFloat() / buildCount * 100) else 0f

        val response = voiceProcessor.generateResponse(
            CommandIntent.QUERY_REPOSITORY,
            mapOf(
                "repository" to (repoName ?: "all repositories"),
                "buildCount" to buildCount,
                "failureRate" to failureRate
            )
        )

        return VoiceExecutionResult(
            success = true,
            message = response,
            spokenResponse = response,
            data = mapOf<String, Any>(
                "repository" to (repoName ?: "all repositories"),
                "buildCount" to buildCount,
                "failureRate" to failureRate
            )
        )
    }

    /**
     * List all accounts
     */
    private suspend fun listAccounts(command: VoiceCommand): VoiceExecutionResult {
        val accounts = accountRepository.getAllAccounts().first()
        val accountNames = accounts.map { "${it.name} (${it.provider.displayName})" }

        val response = voiceProcessor.generateResponse(
            CommandIntent.LIST_ACCOUNTS,
            mapOf(
                "accounts" to accountNames,
                "count" to accounts.size
            )
        )

        return VoiceExecutionResult(
            success = true,
            message = response,
            spokenResponse = response,
            data = mapOf("accounts" to accounts)
        )
    }

    /**
     * Query specific account
     */
    private suspend fun queryAccount(command: VoiceCommand): VoiceExecutionResult {
        val providerParam = command.parameters["provider"]
        val accounts = accountRepository.getAllAccounts().first()

        val matchingAccounts = if (providerParam != null) {
            accounts.filter { it.provider.displayName.contains(providerParam, ignoreCase = true) }
        } else {
            accounts
        }

        if (matchingAccounts.isEmpty()) {
            val response = "No accounts found matching your query."
            return VoiceExecutionResult(
                success = false,
                message = response,
                spokenResponse = response
            )
        }

        val account = matchingAccounts.first()
        val pipelines = pipelineRepository.getPipelinesByAccount(account.id).first()

        val response = voiceProcessor.generateResponse(
            CommandIntent.QUERY_ACCOUNT,
            mapOf(
                "account" to account.name,
                "provider" to account.provider.displayName,
                "buildCount" to pipelines.size
            )
        )

        return VoiceExecutionResult(
            success = true,
            message = response,
            spokenResponse = response,
            data = mapOf("account" to account)
        )
    }

    /**
     * Show help for adding accounts
     */
    private suspend fun addAccountHelp(command: VoiceCommand): VoiceExecutionResult {
        val response = voiceProcessor.generateResponse(
            CommandIntent.ADD_ACCOUNT,
            emptyMap()
        )

        return VoiceExecutionResult(
            success = true,
            message = response,
            spokenResponse = response
        )
    }

    /**
     * Show help information
     */
    private suspend fun showHelp(command: VoiceCommand): VoiceExecutionResult {
        val response = voiceProcessor.generateResponse(
            CommandIntent.SHOW_HELP,
            emptyMap()
        )

        return VoiceExecutionResult(
            success = true,
            message = response,
            spokenResponse = response
        )
    }

    /**
     * Handle greetings
     */
    private suspend fun greeting(command: VoiceCommand): VoiceExecutionResult {
        val response = voiceProcessor.generateResponse(
            CommandIntent.GREETING,
            emptyMap()
        )

        return VoiceExecutionResult(
            success = true,
            message = response,
            spokenResponse = response
        )
    }

    /**
     * Query deployment information
     */
    private suspend fun queryDeployment(command: VoiceCommand): VoiceExecutionResult {
        val pipelines = pipelineRepository.getAllPipelines().first()
            .sortedByDescending { it.finishedAt ?: 0 }

        val lastDeployment = pipelines.firstOrNull()

        if (lastDeployment == null) {
            val response = "I couldn't find any deployments yet."
            return VoiceExecutionResult(
                success = false,
                message = response,
                spokenResponse = response
            )
        }

        val timeAgo = formatTimeAgo(lastDeployment.finishedAt ?: System.currentTimeMillis())
        val status = if (lastDeployment.status == BuildStatus.SUCCESS) "succeeded" else "failed"

        val response = voiceProcessor.generateResponse(
            CommandIntent.QUERY_DEPLOYMENT,
            mapOf(
                "lastDeployment" to lastDeployment.repositoryName,
                "time" to timeAgo,
                "status" to status
            )
        )

        return VoiceExecutionResult(
            success = true,
            message = response,
            spokenResponse = response,
            data = mapOf("deployment" to lastDeployment)
        )
    }

    /**
     * Query branch information
     */
    private suspend fun queryBranch(command: VoiceCommand): VoiceExecutionResult {
        val branchName = command.parameters["branch"] ?: "main"
        val pipelines = pipelineRepository.getAllPipelines().first()
            .filter { it.branch.equals(branchName, ignoreCase = true) }

        val buildCount = pipelines.size
        val failedCount = pipelines.count { it.status == BuildStatus.FAILURE }
        val status = if (failedCount > 0) "$failedCount failed builds" else "healthy"

        val response = voiceProcessor.generateResponse(
            CommandIntent.QUERY_BRANCH,
            mapOf(
                "branch" to branchName,
                "buildCount" to buildCount,
                "status" to status
            )
        )

        return VoiceExecutionResult(
            success = true,
            message = response,
            spokenResponse = response,
            data = mapOf(
                "branch" to branchName,
                "buildCount" to buildCount
            )
        )
    }

    /**
     * Query duration information
     */
    private suspend fun queryDuration(command: VoiceCommand): VoiceExecutionResult {
        val pipelines = pipelineRepository.getAllPipelines().first()
            .filter { it.duration != null && it.duration > 0 }

        if (pipelines.isEmpty()) {
            val response = "I don't have enough duration data yet."
            return VoiceExecutionResult(
                success = false,
                message = response,
                spokenResponse = response
            )
        }

        val avgDuration = pipelines.mapNotNull { it.duration }.average().toLong()
        val fastestDuration = pipelines.minOfOrNull { it.duration ?: Long.MAX_VALUE }
        val slowestDuration = pipelines.maxOfOrNull { it.duration ?: 0L }

        val response = voiceProcessor.generateResponse(
            CommandIntent.QUERY_DURATION,
            mapOf(
                "avgDuration" to avgDuration,
                "fastestDuration" to (fastestDuration as Any),
                "slowestDuration" to (slowestDuration as Any)
            )
        )

        return VoiceExecutionResult(
            success = true,
            message = response,
            spokenResponse = response,
            data = mapOf<String, Any>(
                "avgDuration" to avgDuration,
                "fastestDuration" to (fastestDuration ?: 0L),
                "slowestDuration" to (slowestDuration ?: 0L)
            )
        )
    }

    /**
     * Query success rate
     */
    private suspend fun querySuccessRate(command: VoiceCommand): VoiceExecutionResult {
        val pipelines = pipelineRepository.getAllPipelines().first()

        val totalBuilds = pipelines.size
        val successBuilds = pipelines.count { it.status == BuildStatus.SUCCESS }
        val successRate = if (totalBuilds > 0) (successBuilds.toFloat() / totalBuilds * 100) else 0f

        val response = voiceProcessor.generateResponse(
            CommandIntent.QUERY_SUCCESS_RATE,
            mapOf(
                "successRate" to successRate,
                "totalBuilds" to totalBuilds
            )
        )

        return VoiceExecutionResult(
            success = true,
            message = response,
            spokenResponse = response,
            data = mapOf(
                "successRate" to successRate,
                "totalBuilds" to totalBuilds
            )
        )
    }

    /**
     * Query commit information
     */
    private suspend fun queryCommit(command: VoiceCommand): VoiceExecutionResult {
        val pipelines = pipelineRepository.getAllPipelines().first()
            .sortedByDescending { it.startedAt ?: 0 }

        val lastPipeline = pipelines.firstOrNull()

        if (lastPipeline == null) {
            val response = "I couldn't find any commits yet."
            return VoiceExecutionResult(
                success = false,
                message = response,
                spokenResponse = response
            )
        }

        val response = voiceProcessor.generateResponse(
            CommandIntent.QUERY_COMMIT,
            mapOf(
                "commitHash" to lastPipeline.commitHash,
                "commitAuthor" to lastPipeline.commitAuthor,
                "commitMessage" to lastPipeline.commitMessage
            )
        )

        return VoiceExecutionResult(
            success = true,
            message = response,
            spokenResponse = response,
            data = mapOf("commit" to lastPipeline)
        )
    }

    /**
     * Format time ago
     */
    private fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "just now"
            diff < 3_600_000 -> "${diff / 60_000} minutes ago"
            diff < 86_400_000 -> "${diff / 3_600_000} hours ago"
            diff < 604_800_000 -> "${diff / 86_400_000} days ago"
            else -> "${diff / 604_800_000} weeks ago"
        }
    }

    /**
     * Stop speaking
     */
    fun stopSpeaking() {
        ttsManager.stop()
    }

    /**
     * Check if speaking
     */
    fun isSpeaking(): Boolean {
        return ttsManager.isSpeaking()
    }
}

/**
 * Result of voice command execution
 */
data class VoiceExecutionResult(
    val success: Boolean,
    val message: String,
    val spokenResponse: String,
    val data: Map<String, Any> = emptyMap()
)
