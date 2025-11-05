package com.secureops.app.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.secureops.app.data.repository.AccountRepository
import com.secureops.app.data.repository.PipelineRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class PipelineSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val accountRepository: AccountRepository by inject()
    private val pipelineRepository: PipelineRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            Timber.d("Starting pipeline sync worker")

            // Get all active accounts
            val accounts = accountRepository.getActiveAccounts().first()

            if (accounts.isEmpty()) {
                Timber.d("No active accounts to sync")
                return Result.success()
            }

            // Sync pipelines for each account
            var successCount = 0
            var failureCount = 0

            accounts.forEach { account ->
                val result = pipelineRepository.syncPipelines(account.id)
                if (result.isSuccess) {
                    successCount++
                    Timber.d("Successfully synced pipelines for account: ${account.name}")
                } else {
                    failureCount++
                    Timber.e("Failed to sync pipelines for account: ${account.name}")
                }
            }

            // Clean up old pipelines
            pipelineRepository.cleanOldPipelines(daysToKeep = 30)

            Timber.d("Sync completed: $successCount successful, $failureCount failed")

            // Return success if at least one account synced successfully
            if (successCount > 0) Result.success() else Result.retry()

        } catch (e: Exception) {
            Timber.e(e, "Pipeline sync worker failed")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "pipeline_sync_work"
    }
}
