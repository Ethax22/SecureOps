package com.secureops.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secureops.app.data.repository.AccountRepository
import com.secureops.app.data.security.SecureTokenManager
import com.secureops.app.domain.model.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class EditAccountUiState(
    val account: Account? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class EditAccountViewModel(
    private val accountRepository: AccountRepository,
    private val tokenManager: SecureTokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditAccountUiState())
    val uiState: StateFlow<EditAccountUiState> = _uiState.asStateFlow()

    fun loadAccount(accountId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val account = accountRepository.getAccountById(accountId)

                if (account != null) {
                    _uiState.value = _uiState.value.copy(
                        account = account,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Account not found"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load account")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load account: ${e.message}"
                )
            }
        }
    }

    fun updateAccount(
        accountId: String,
        name: String,
        baseUrl: String,
        newToken: String? = null
    ) {
        viewModelScope.launch {
            try {
                _uiState.value =
                    _uiState.value.copy(isLoading = true, error = null, isSuccess = false)

                val currentAccount = accountRepository.getAccountById(accountId)

                if (currentAccount == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Account not found"
                    )
                    return@launch
                }

                // Update token if provided (not empty)
                if (!newToken.isNullOrBlank()) {
                    tokenManager.storeToken(accountId, newToken)
                    Timber.d("Token updated for account: $accountId")
                }

                // Update account details
                val updatedAccount = currentAccount.copy(
                    name = name.trim(),
                    baseUrl = baseUrl.trim()
                )

                val result = accountRepository.updateAccount(updatedAccount)

                if (result.isSuccess) {
                    Timber.i("Account updated successfully: $name")
                    _uiState.value = _uiState.value.copy(
                        account = updatedAccount,
                        isLoading = false,
                        isSuccess = true
                    )
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to update account"
                    Timber.e("Failed to update account: $error")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating account")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }
}
