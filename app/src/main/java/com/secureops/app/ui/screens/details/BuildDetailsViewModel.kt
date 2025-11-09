package com.secureops.app.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secureops.app.data.executor.RemediationExecutor
import com.secureops.app.data.repository.PipelineRepository
import com.secureops.app.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import timber.log.Timber

data class BuildDetailsUiState(
    val pipeline: Pipeline? = null,
    val isLoading: Boolean = false,
    val isExecutingAction: Boolean = false,
    val actionResult: ActionResult? = null,
    val error: String? = null
)

class BuildDetailsViewModel(
    private val pipelineRepository: PipelineRepository,
    private val remediationExecutor: RemediationExecutor
) : ViewModel() {

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
            } catch (e: Exception) {
                Timber.e(e, "Failed to load pipeline: $pipelineId")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load pipeline: ${e.message}"
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
}
