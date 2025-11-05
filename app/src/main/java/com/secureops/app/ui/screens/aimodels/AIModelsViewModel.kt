package com.secureops.app.ui.screens.aimodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.sdk.models.ModelInfo
import com.secureops.app.ml.RunAnywhereManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

data class ModelDownloadState(
    val modelId: String,
    val progress: Float
)

data class AIModelsUiState(
    val availableModels: List<ModelInfo> = emptyList(),
    val currentModelId: String? = null,
    val downloadingModels: Map<String, Float> = emptyMap(),
    val loadingModelId: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

class AIModelsViewModel(
    private val runAnywhereManager: RunAnywhereManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIModelsUiState())
    val uiState: StateFlow<AIModelsUiState> = _uiState.asStateFlow()

    init {
        loadModels()
        updateCurrentModel()
    }

    private fun loadModels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val models = runAnywhereManager.getAvailableModels()
                _uiState.update {
                    it.copy(
                        availableModels = models,
                        isLoading = false
                    )
                }
                Timber.d("Loaded ${models.size} models")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load models")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load models: ${e.message}"
                    )
                }
            }
        }
    }

    private fun updateCurrentModel() {
        val currentModelId = runAnywhereManager.getCurrentModelId()
        _uiState.update { it.copy(currentModelId = currentModelId) }
    }

    fun downloadModel(modelId: String) {
        viewModelScope.launch {
            try {
                Timber.i("Starting download for model: $modelId")

                // Add to downloading map
                _uiState.update {
                    it.copy(
                        downloadingModels = it.downloadingModels + (modelId to 0f),
                        error = null
                    )
                }

                runAnywhereManager.downloadModel(modelId).collect { progress ->
                    _uiState.update {
                        it.copy(
                            downloadingModels = it.downloadingModels + (modelId to progress)
                        )
                    }
                    Timber.d("Download progress for $modelId: ${(progress * 100).toInt()}%")
                }

                // Download completed - remove from downloading map and refresh models
                _uiState.update {
                    it.copy(
                        downloadingModels = it.downloadingModels - modelId,
                        successMessage = "Model downloaded successfully!"
                    )
                }

                // Refresh model list to show updated download status
                refreshModels()

                Timber.i("Model $modelId downloaded successfully")

            } catch (e: Exception) {
                Timber.e(e, "Failed to download model: $modelId")
                _uiState.update {
                    it.copy(
                        downloadingModels = it.downloadingModels - modelId,
                        error = "Download failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun loadModel(modelId: String) {
        viewModelScope.launch {
            try {
                Timber.i("Loading model: $modelId")
                _uiState.update {
                    it.copy(loadingModelId = modelId, error = null, successMessage = null)
                }

                val success = runAnywhereManager.loadModel(modelId)

                if (success) {
                    _uiState.update {
                        it.copy(
                            currentModelId = modelId,
                            loadingModelId = null,
                            successMessage = "Model loaded successfully!"
                        )
                    }
                    Timber.i("Model $modelId loaded successfully")
                } else {
                    _uiState.update {
                        it.copy(
                            loadingModelId = null,
                            error = "Failed to load model"
                        )
                    }
                    Timber.e("Failed to load model: $modelId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading model: $modelId")
                _uiState.update {
                    it.copy(
                        loadingModelId = null,
                        error = "Error loading model: ${e.message}"
                    )
                }
            }
        }
    }

    fun unloadModel() {
        viewModelScope.launch {
            try {
                Timber.i("Unloading current model")
                runAnywhereManager.unloadModel()
                _uiState.update {
                    it.copy(
                        currentModelId = null,
                        successMessage = "Model unloaded"
                    )
                }
                Timber.i("Model unloaded successfully")
            } catch (e: Exception) {
                Timber.e(e, "Failed to unload model")
                _uiState.update {
                    it.copy(error = "Failed to unload model: ${e.message}")
                }
            }
        }
    }

    fun refreshModels() {
        viewModelScope.launch {
            try {
                // Scan for downloaded models first
                runAnywhereManager.scanForModels()

                // Then refresh the list
                loadModels()
                updateCurrentModel()
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh models")
                _uiState.update {
                    it.copy(error = "Failed to refresh: ${e.message}")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
