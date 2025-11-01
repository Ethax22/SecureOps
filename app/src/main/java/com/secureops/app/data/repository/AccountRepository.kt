package com.secureops.app.data.repository

import com.secureops.app.data.local.dao.AccountDao
import com.secureops.app.data.local.entity.toEntity
import com.secureops.app.data.local.entity.toDomain
import com.secureops.app.data.security.SecureTokenManager
import com.secureops.app.domain.model.Account
import com.secureops.app.domain.model.CIProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val tokenManager: SecureTokenManager
) {
    fun getActiveAccounts(): Flow<List<Account>> {
        return accountDao.getActiveAccounts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getAccountById(accountId: String): Account? {
        return accountDao.getAccountById(accountId)?.toDomain()
    }

    suspend fun addAccount(
        provider: CIProvider,
        name: String,
        baseUrl: String,
        token: String
    ): Result<Account> {
        return try {
            val accountId = UUID.randomUUID().toString()

            // Store token securely
            tokenManager.storeToken(accountId, token)

            // Create account with encrypted token reference
            val account = Account(
                id = accountId,
                provider = provider,
                name = name,
                baseUrl = baseUrl,
                tokenEncrypted = accountId, // Reference to encrypted token
                isActive = true
            )

            accountDao.insertAccount(account.toEntity())
            Timber.d("Account added successfully: $name")

            Result.success(account)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add account")
            Result.failure(e)
        }
    }

    suspend fun updateAccount(account: Account): Result<Unit> {
        return try {
            accountDao.updateAccount(account.toEntity())
            Timber.d("Account updated: ${account.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update account")
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(accountId: String): Result<Unit> {
        return try {
            accountDao.deleteAccountById(accountId)
            tokenManager.removeToken(accountId)
            Timber.d("Account deleted: $accountId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete account")
            Result.failure(e)
        }
    }

    suspend fun updateLastSyncTime(accountId: String) {
        accountDao.updateLastSyncTime(accountId, System.currentTimeMillis())
    }

    fun getAccountToken(accountId: String): String? {
        return tokenManager.getToken(accountId)
    }
}
