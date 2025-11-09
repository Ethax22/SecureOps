package com.secureops.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secureops.app.data.repository.AccountRepository
import com.secureops.app.data.repository.PipelineRepository
import com.secureops.app.domain.model.Account
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
        // Trigger initial sync
        refreshPipelines()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Observe accounts and trigger sync when accounts change
                accountRepository.getActiveAccounts().collect { accounts ->
                    val previousAccounts = _uiState.value.accounts
                    _uiState.update { it.copy(accounts = accounts) }

                    // If new accounts were added, trigger sync
                    if (accounts.size > previousAccounts.size) {
                        Timber.d("New accounts detected, triggering sync")
                        refreshPipelines()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading accounts")
                _uiState.update { it.copy(error = e.message) }
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
            _uiState.update { it.copy(isRefreshing = true) }

            try {
                val accounts = _uiState.value.accounts
                accounts.forEach { account ->
                    pipelineRepository.syncPipelines(account.id)
                }
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        error = null
                    )
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
