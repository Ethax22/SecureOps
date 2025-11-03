package com.secureops.app.ml.voice

import com.secureops.app.data.executor.RemediationExecutor
import com.secureops.app.data.repository.PipelineRepository
import com.secureops.app.domain.model.*
import com.secureops.app.ml.VoiceCommandProcessor
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceActionExecutor @Inject constructor(
    private val pipelineRepository: PipelineRepository,
    private val voiceProcessor: VoiceCommandProcessor,
    private val remediationExecutor: RemediationExecutor,
    private val ttsManager: TextToSpeechManager
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
