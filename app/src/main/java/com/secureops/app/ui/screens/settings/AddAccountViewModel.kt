package com.secureops.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secureops.app.data.repository.AccountRepository
import com.secureops.app.data.repository.PipelineRepository
import com.secureops.app.data.auth.OAuth2Manager
import com.secureops.app.data.auth.OAuthToken
import com.secureops.app.domain.model.CIProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AddAccountViewModel(
    private val accountRepository: AccountRepository,
    private val pipelineRepository: PipelineRepository
) : ViewModel(), KoinComponent {

    private val oauth2Manager: OAuth2Manager by inject()

    private val _uiState = MutableStateFlow(AddAccountUiState())
    val uiState: StateFlow<AddAccountUiState> = _uiState.asStateFlow()

    fun startOAuthFlow(provider: CIProvider) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = oauth2Manager.authenticate(provider)

                result.fold(
                    onSuccess = { token ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            oauthToken = token
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "OAuth failed: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "OAuth error: ${e.message}"
                )
            }
        }
    }

    fun completeOAuthAccount(
        provider: CIProvider,
        name: String,
        baseUrl: String,
        token: OAuthToken
    ) {
        viewModelScope.launch {
            addAccount(
                provider = provider,
                name = name,
                baseUrl = baseUrl,
                token = token.accessToken
            )
        }
    }

    fun addAccount(
        provider: CIProvider,
        name: String,
        baseUrl: String,
        token: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            val result = accountRepository.addAccount(
                provider = provider,
                name = name,
                baseUrl = baseUrl,
                token = token
            )

            result.fold(
                onSuccess = { account ->
                    Timber.d("Account added successfully: ${account.id}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        error = null
                    )

                    // Trigger immediate sync for the new account
                    viewModelScope.launch {
                        Timber.d("Triggering initial sync for new account: ${account.name}")
                        val syncResult = pipelineRepository.syncPipelines(account.id)

                        syncResult.fold(
                            onSuccess = { pipelines ->
                                Timber.d("Initial sync completed successfully: ${pipelines.size} pipelines fetched")
                            },
                            onFailure = { error ->
                                Timber.e(
                                    error,
                                    "Failed to sync pipelines for new account: ${account.name}"
                                )
                                // Update UI state with sync error
                                _uiState.value = _uiState.value.copy(
                                    error = "Account added but failed to sync pipelines: ${error.message ?: "Unknown error"}"
                                )
                            }
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to add account: $name")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = false,
                        error = error.message ?: "Failed to add account"
                    )
                }
            )
        }
    }
}

data class AddAccountUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val oauthToken: OAuthToken? = null
)
