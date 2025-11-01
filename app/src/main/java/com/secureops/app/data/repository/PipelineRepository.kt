package com.secureops.app.data.repository

import com.secureops.app.data.local.dao.PipelineDao
import com.secureops.app.data.local.entity.toEntity
import com.secureops.app.data.local.entity.toDomain
import com.secureops.app.data.remote.api.GitHubService
import com.secureops.app.data.remote.api.GitLabService
import com.secureops.app.data.remote.mapper.PipelineMapper
import com.secureops.app.data.remote.mapper.PipelineMapper.toPipeline
import com.secureops.app.domain.model.*
import com.secureops.app.ml.FailurePredictionModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PipelineRepository @Inject constructor(
    private val pipelineDao: PipelineDao,
    private val gitHubService: GitHubService,
    private val gitLabService: GitLabService,
    private val accountRepository: AccountRepository,
    private val failurePredictionModel: FailurePredictionModel
) {
    fun getAllPipelines(): Flow<List<Pipeline>> {
        return pipelineDao.getAllPipelines().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getPipelinesByAccount(accountId: String): Flow<List<Pipeline>> {
        return pipelineDao.getPipelinesByAccount(accountId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getHighRiskPipelines(threshold: Float = 70f): Flow<List<Pipeline>> {
        return pipelineDao.getHighRiskPipelines(threshold).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getPipelineById(pipelineId: String): Pipeline? {
        return pipelineDao.getPipelineById(pipelineId)?.toDomain()
    }

    suspend fun syncPipelines(accountId: String): Result<List<Pipeline>> {
        return try {
            val account = accountRepository.getAccountById(accountId)
                ?: return Result.failure(Exception("Account not found"))

            val token = accountRepository.getAccountToken(accountId)
                ?: return Result.failure(Exception("Token not found"))

            val pipelines = when (account.provider) {
                CIProvider.GITHUB_ACTIONS -> fetchGitHubPipelines(account, token)
                CIProvider.GITLAB_CI -> fetchGitLabPipelines(account, token)
                else -> emptyList() // TODO: Implement other providers
            }

            // Cache pipelines locally
            pipelineDao.insertPipelines(pipelines.map { it.toEntity() })

            // Update last sync time
            accountRepository.updateLastSyncTime(accountId)

            Timber.d("Synced ${pipelines.size} pipelines for account: ${account.name}")
            Result.success(pipelines)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync pipelines")
            Result.failure(e)
        }
    }

    private suspend fun fetchGitHubPipelines(account: Account, token: String): List<Pipeline> {
        // For simplicity, this is a mock implementation
        // In production, you would parse the account.baseUrl to get owner/repo
        // and make actual API calls
        return emptyList()
    }

    private suspend fun fetchGitLabPipelines(account: Account, token: String): List<Pipeline> {
        // Mock implementation
        return emptyList()
    }

    suspend fun predictFailure(pipeline: Pipeline): Pipeline {
        return try {
            // Get historical data
            val repoName = pipeline.repositoryName
            val historyCount = 10

            // Mock commit diff and logs for prediction
            val commitDiff = ""
            val testHistory = emptyList<Boolean>()
            val logs = ""

            // Run prediction
            val (riskPercentage, confidence) = failurePredictionModel.predictFailure(
                commitDiff, testHistory, logs
            )

            // Get causal factors
            val causalFactors = failurePredictionModel.identifyCausalFactors(
                commitDiff, testHistory, logs
            )

            // Update pipeline with prediction
            val updatedPipeline = pipeline.copy(
                failurePrediction = FailurePrediction(
                    riskPercentage = riskPercentage,
                    confidence = confidence,
                    causalFactors = causalFactors
                )
            )

            // Cache updated pipeline
            pipelineDao.updatePipeline(updatedPipeline.toEntity())

            updatedPipeline
        } catch (e: Exception) {
            Timber.e(e, "Failed to predict failure")
            pipeline
        }
    }

    suspend fun cleanOldPipelines(daysToKeep: Int = 30) {
        val timestamp = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        pipelineDao.deleteOldPipelines(timestamp)
    }
}
