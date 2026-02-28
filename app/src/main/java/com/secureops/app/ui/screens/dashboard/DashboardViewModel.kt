package com.secureops.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secureops.app.data.repository.AccountRepository
import com.secureops.app.data.repository.PipelineRepository
import com.secureops.app.domain.model.Account
import com.secureops.app.domain.model.BuildStatus
import com.secureops.app.domain.model.Pipeline
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

data class DashboardUiState(
    val accounts: List<Account> = emptyList(),
    val pipelines: List<Pipeline> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false
)

class DashboardViewModel(
    private val accountRepository: AccountRepository,
    private val pipelineRepository: PipelineRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Observe accounts — trigger sync whenever accounts are loaded
                accountRepository.getActiveAccounts().collect { accounts ->
                    _uiState.update { it.copy(accounts = accounts) }

                    // Always sync on the first emission (accounts loaded from DB)
                    if (accounts.isNotEmpty()) {
                        Timber.d("Accounts loaded (${accounts.size}), triggering sync")
                        syncAllAccounts(accounts)
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading accounts")
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }

        viewModelScope.launch {
            try {
                // Observe pipelines
                pipelineRepository.getAllPipelines().collect { pipelines ->
                    _uiState.update {
                        it.copy(
                            pipelines = pipelines,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading pipelines")
                _uiState.update {
                    it.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun refreshPipelines() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            try {
                // Read accounts directly from the repository
                val accounts = accountRepository.getActiveAccounts().first()

                if (accounts.isEmpty()) {
                    Timber.w("No active accounts found - nothing to sync")
                    _uiState.update { it.copy(isRefreshing = false) }
                    return@launch
                }

                syncAllAccounts(accounts)

                _uiState.update {
                    it.copy(isRefreshing = false) // Keep the error if it was set
                }
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing pipelines")
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        error = e.message
                    )
                }
            }
        }
    }

    private suspend fun syncAllAccounts(accounts: List<Account>) {
        // Get pipelines before sync to detect new builds
        val pipelinesBefore = _uiState.value.pipelines
        val previousPipelineMap = pipelinesBefore.associateBy(
            { it.id },
            { it.status }
        )

        // Sync each account
        accounts.forEach { account ->
            Timber.i("Syncing account: ${account.name} (${account.provider}) — ${account.baseUrl}")
            val result = pipelineRepository.syncPipelines(account.id)

            result.onSuccess { pipelines ->
                Timber.i("Synced ${pipelines.size} pipelines from ${account.name}")

                // Run predictions for new or RUNNING builds
                pipelines.forEach { pipeline ->
                    val previousStatus = previousPipelineMap[pipeline.id]
                    val isNewPipeline = previousStatus == null
                    val justStarted = previousStatus != null &&
                            previousStatus != BuildStatus.RUNNING &&
                            pipeline.status == BuildStatus.RUNNING

                    if (isNewPipeline || justStarted) {
                        try {
                            val reason = when {
                                isNewPipeline -> "NEW BUILD DETECTED"
                                justStarted -> "BUILD STARTED"
                                else -> "UNKNOWN"
                            }
                            Timber.i("🎯 Triggering prediction: $reason - ${pipeline.repositoryName} #${pipeline.buildNumber} [${pipeline.status}]")
                            pipelineRepository.predictFailure(pipeline)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to predict failure for pipeline: ${pipeline.id}")
                        }
                    }
                }
            }

            result.onFailure { error ->
                Timber.e(error, "Failed to sync account: ${account.name}")
                _uiState.update { it.copy(error = "Sync failed for ${account.name}: ${error.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
