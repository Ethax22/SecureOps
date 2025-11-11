package com.secureops.app.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secureops.app.data.executor.RemediationExecutor
import com.secureops.app.data.repository.PipelineRepository
import com.secureops.app.data.repository.AccountRepository
import com.secureops.app.data.remote.PipelineStreamService
import com.secureops.app.data.remote.LogEntry
import com.secureops.app.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.component.get
import timber.log.Timber
import android.content.Context

data class BuildDetailsUiState(
    val pipeline: Pipeline? = null,
    val logs: String? = null,
    val isLoading: Boolean = false,
    val isLoadingLogs: Boolean = false,
    val isExecutingAction: Boolean = false,
    val actionResult: ActionResult? = null,
    val error: String? = null,
    val logsError: String? = null,
    val isStreaming: Boolean = false,
    val streamingLogs: List<LogEntry> = emptyList(),
    val artifacts: List<BuildArtifact> = emptyList(),
    val isLoadingArtifacts: Boolean = false,
    val artifactsError: String? = null
)

class BuildDetailsViewModel(
    private val pipelineRepository: PipelineRepository,
    private val remediationExecutor: RemediationExecutor
) : ViewModel(), KoinComponent {

    private val accountRepository: AccountRepository by inject()
    private val pipelineStreamService: PipelineStreamService by inject()
    private val context: Context by inject()
    private var logStreamJob: Job? = null

    private val _uiState = MutableStateFlow(BuildDetailsUiState())
    val uiState: StateFlow<BuildDetailsUiState> = _uiState.asStateFlow()

    fun loadPipeline(pipelineId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val pipeline = pipelineRepository.getPipelineById(pipelineId)
                _uiState.value = _uiState.value.copy(
                    pipeline = pipeline,
                    isLoading = false
                )

                // Check if logs are already cached
                if (pipeline != null) {
                    if (pipeline.logs != null) {
                        // Use cached logs immediately
                        Timber.d("Using cached logs (${pipeline.logs.length} chars) from ${pipeline.logsCachedAt}")
                        _uiState.value = _uiState.value.copy(logs = pipeline.logs)
                    } else {
                        // Fetch logs if not cached
                        fetchLogs()
                    }
                    
                    // Load artifacts
                    loadArtifacts()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load pipeline: $pipelineId")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load pipeline: ${e.message}"
                )
            }
        }
    }

    /**
     * Load artifacts for the current pipeline
     */
    fun loadArtifacts() {
        val pipeline = _uiState.value.pipeline ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingArtifacts = true, artifactsError = null)

            try {
                val result = pipelineRepository.getArtifacts(pipeline)

                result.fold(
                    onSuccess = { artifacts ->
                        Timber.d("Loaded ${artifacts.size} artifacts")
                        _uiState.value = _uiState.value.copy(
                            artifacts = artifacts,
                            isLoadingArtifacts = false
                        )
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to load artifacts")
                        _uiState.value = _uiState.value.copy(
                            isLoadingArtifacts = false,
                            artifactsError = "Failed to load artifacts: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading artifacts")
                _uiState.value = _uiState.value.copy(
                    isLoadingArtifacts = false,
                    artifactsError = "Error: ${e.message}"
                )
            }
        }
    }

    /**
     * Download an artifact
     */
    fun downloadArtifact(artifact: BuildArtifact) {
        viewModelScope.launch {
            try {
                // Create downloads directory
                val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                val destination = java.io.File(downloadsDir, artifact.name)

                Timber.d("Downloading artifact: ${artifact.name}")

                val result = pipelineRepository.downloadArtifact(artifact, destination)

                result.fold(
                    onSuccess = { file ->
                        Timber.i("Artifact downloaded: ${file.absolutePath}")
                        _uiState.value = _uiState.value.copy(
                            actionResult = ActionResult(
                                success = true,
                                message = "Downloaded ${artifact.name}",
                                details = mapOf("path" to file.absolutePath)
                            )
                        )
                    },
                    onFailure = { error ->
                        Timber.e(error, "Download failed")
                        _uiState.value = _uiState.value.copy(
                            actionResult = ActionResult(
                                success = false,
                                message = "Download failed: ${error.message}",
                                details = emptyMap()
                            )
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error downloading artifact")
                _uiState.value = _uiState.value.copy(
                    actionResult = ActionResult(
                        success = false,
                        message = "Error: ${e.message}",
                        details = emptyMap()
                    )
                )
            }
        }
    }

    fun fetchLogs() {
        val pipeline = _uiState.value.pipeline ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingLogs = true, logsError = null)

            try {
                Timber.d("Fetching logs for pipeline: ${pipeline.id}")
                val result = pipelineRepository.fetchBuildLogs(pipeline)

                if (result.isSuccess) {
                    val logs = result.getOrNull() ?: "No logs available"
                    Timber.d("Successfully loaded ${logs.length} characters of logs")

                    // Update state with logs
                    _uiState.value = _uiState.value.copy(
                        logs = logs,
                        isLoadingLogs = false
                    )

                    // Save logs to cache in database
                    try {
                        val updatedPipeline = pipeline.copy(
                            logs = logs,
                            logsCachedAt = System.currentTimeMillis()
                        )
                        pipelineRepository.updatePipelineWithLogs(updatedPipeline)
                        Timber.d("Cached logs for pipeline: ${pipeline.id}")
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to cache logs, but continuing")
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to load logs"
                    Timber.w("Failed to load logs: $error")
                    _uiState.value = _uiState.value.copy(
                        logs = "Failed to load logs: $error",
                        logsError = error,
                        isLoadingLogs = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching logs")
                _uiState.value = _uiState.value.copy(
                    logs = "Error loading logs: ${e.message}",
                    logsError = e.message,
                    isLoadingLogs = false
                )
            }
        }
    }

    fun rerunBuild() {
        val pipeline = _uiState.value.pipeline ?: return

        // Use GlobalScope so the request continues even if the user navigates away
        GlobalScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isExecutingAction = true, actionResult = null)

                Timber.d("Starting build rerun for pipeline: ${pipeline.id}")

                val action = RemediationAction(
                    id = "rerun-${pipeline.id}-${System.currentTimeMillis()}",
                    type = ActionType.RERUN_PIPELINE,
                    pipeline = pipeline,
                    description = "Rerun build for ${pipeline.repositoryName} #${pipeline.buildNumber}",
                    requiresConfirmation = false,
                    parameters = emptyMap()
                )

                // Execute in IO dispatcher and wait for completion
                val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    remediationExecutor.executeRemediation(action)
                }

                Timber.d("Build rerun result: success=${result.success}, message=${result.message}")

                _uiState.value = _uiState.value.copy(
                    isExecutingAction = false,
                    actionResult = result
                )

                if (result.success) {
                    Timber.i("✅ Build rerun successfully: ${pipeline.id}")
                } else {
                    Timber.w("❌ Build rerun failed: ${result.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to rerun build: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isExecutingAction = false,
                    actionResult = ActionResult(
                        success = false,
                        message = "Failed to rerun build: ${e.message}",
                        details = emptyMap()
                    )
                )
            }
        }
    }

    fun cancelBuild() {
        val pipeline = _uiState.value.pipeline ?: return

        // Use GlobalScope so the request continues even if the user navigates away
        GlobalScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isExecutingAction = true, actionResult = null)

                val action = RemediationAction(
                    id = "cancel-${pipeline.id}-${System.currentTimeMillis()}",
                    type = ActionType.CANCEL_PIPELINE,
                    pipeline = pipeline,
                    description = "Cancel build for ${pipeline.repositoryName} #${pipeline.buildNumber}",
                    requiresConfirmation = false,
                    parameters = emptyMap()
                )

                val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    remediationExecutor.executeRemediation(action)
                }

                _uiState.value = _uiState.value.copy(
                    isExecutingAction = false,
                    actionResult = result
                )

                if (result.success) {
                    Timber.d("Build cancelled successfully: ${pipeline.id}")
                } else {
                    Timber.w("Build cancel failed: ${result.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to cancel build")
                _uiState.value = _uiState.value.copy(
                    isExecutingAction = false,
                    actionResult = ActionResult(
                        success = false,
                        message = "Failed to cancel build: ${e.message}",
                        details = emptyMap()
                    )
                )
            }
        }
    }

    fun clearActionResult() {
        _uiState.value = _uiState.value.copy(actionResult = null)
    }

    /**
     * Manually trigger ML prediction for current pipeline
     */
    fun runPrediction() {
        val pipeline = _uiState.value.pipeline ?: return

        viewModelScope.launch {
            try {
                Timber.i("🤖 Manually triggering prediction for pipeline: ${pipeline.id}")

                // Run prediction
                val updatedPipeline = pipelineRepository.predictFailure(pipeline)

                // Update UI with new prediction
                _uiState.value = _uiState.value.copy(
                    pipeline = updatedPipeline,
                    actionResult = ActionResult(
                        success = true,
                        message = "Prediction completed: ${updatedPipeline.failurePrediction?.riskPercentage?.toInt()}% risk",
                        details = emptyMap()
                    )
                )

                Timber.i("✅ Prediction completed successfully")
            } catch (e: Exception) {
                Timber.e(e, "Failed to run prediction")
                _uiState.value = _uiState.value.copy(
                    actionResult = ActionResult(
                        success = false,
                        message = "Prediction failed: ${e.message}",
                        details = emptyMap()
                    )
                )
            }
        }
    }

    /**
     * Start live log streaming
     */
    fun startLogStreaming() {
        val pipeline = _uiState.value.pipeline ?: return

        viewModelScope.launch {
            try {
                val token = accountRepository.getAccountToken(pipeline.accountId)
                if (token == null) {
                    Timber.w("Cannot start streaming: No token found for account ${pipeline.accountId}")
                    _uiState.value = _uiState.value.copy(
                        logsError = "Authentication token not found"
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    isStreaming = true,
                    streamingLogs = emptyList()
                )

                Timber.d("Starting log streaming for pipeline ${pipeline.id}")

                logStreamJob = launch {
                    try {
                        pipelineStreamService.streamBuildLogs(pipeline, token)
                            .collect { logEntry ->
                                val currentLogs = _uiState.value.streamingLogs
                                _uiState.value = _uiState.value.copy(
                                    streamingLogs = currentLogs + logEntry
                                )
                                Timber.v("Received log entry: ${logEntry.message.take(50)}...")
                            }
                    } catch (e: Exception) {
                        Timber.e(e, "Error during log streaming")
                        _uiState.value = _uiState.value.copy(
                            isStreaming = false,
                            logsError = "Streaming error: ${e.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to start log streaming")
                _uiState.value = _uiState.value.copy(
                    isStreaming = false,
                    logsError = "Failed to start streaming: ${e.message}"
                )
            }
        }
    }

    /**
     * Stop log streaming
     */
    fun stopLogStreaming() {
        Timber.d("Stopping log streaming")
        logStreamJob?.cancel()
        logStreamJob = null
        _uiState.value = _uiState.value.copy(isStreaming = false)
    }

    override fun onCleared() {
        super.onCleared()
        stopLogStreaming()
    }
}
