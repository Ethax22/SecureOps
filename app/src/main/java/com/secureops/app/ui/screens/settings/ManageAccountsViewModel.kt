package com.secureops.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secureops.app.data.repository.AccountRepository
import com.secureops.app.domain.model.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class ManageAccountsViewModel(
    private val accountRepository: AccountRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ManageAccountsUiState())
    val uiState: StateFlow<ManageAccountsUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            try {
                accountRepository.getAllAccounts().collect { accounts ->
                    _uiState.value = _uiState.value.copy(
                        accounts = accounts,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load accounts")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load accounts: ${e.message}"
                )
            }
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            accountRepository.deleteAccount(accountId).fold(
                onSuccess = {
                    Timber.d("Account deleted successfully")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Account deleted successfully"
                    )
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to delete account")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to delete account: ${error.message}"
                    )
                }
            )
        }
    }

    fun toggleAccountStatus(account: Account) {
        viewModelScope.launch {
            val updatedAccount = account.copy(isActive = !account.isActive)
            
            accountRepository.updateAccount(updatedAccount).fold(
                onSuccess = {
                    Timber.d("Account status updated")
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to update account status")
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to update account: ${error.message}"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}

data class ManageAccountsUiState(
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)
