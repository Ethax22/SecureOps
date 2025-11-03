package com.secureops.app.data.repository

import com.secureops.app.data.local.dao.PipelineDao
import com.secureops.app.data.local.entity.toEntity
import com.secureops.app.data.local.entity.toDomain
import com.secureops.app.data.remote.api.*
import com.secureops.app.data.remote.mapper.PipelineMapper
import com.secureops.app.data.remote.mapper.PipelineMapper.toPipeline
import com.secureops.app.domain.model.*
import com.secureops.app.ml.FailurePredictionModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PipelineRepository @Inject constructor(
    private val pipelineDao: PipelineDao,
    private val gitHubService: GitHubService,
    private val gitLabService: GitLabService,
    private val jenkinsService: JenkinsService,
    private val circleCIService: CircleCIService,
    private val azureDevOpsService: AzureDevOpsService,
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
                CIProvider.JENKINS -> fetchJenkinsPipelines(account, token)
                CIProvider.CIRCLE_CI -> fetchCircleCIPipelines(account, token)
                CIProvider.AZURE_DEVOPS -> fetchAzureDevOpsPipelines(account, token)
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
        return try {
            // Parse owner/repo from baseUrl or account metadata
            // For demo, using mock data structure
            val urlParts = account.baseUrl.split("/")
            val owner = urlParts.getOrNull(urlParts.size - 2) ?: "owner"
            val repo = urlParts.getOrNull(urlParts.size - 1) ?: "repo"

            val response = gitHubService.getWorkflowRuns(owner, repo)

            if (response.isSuccessful && response.body() != null) {
                response.body()!!.workflowRuns.map { run ->
                    Pipeline(
                        id = run.id.toString(),
                        accountId = account.id,
                        repositoryName = repo,
                        repositoryUrl = account.baseUrl,
                        branch = run.headBranch ?: "main",
                        buildNumber = run.runNumber,
                        status = mapGitHubStatus(run.status, run.conclusion),
                        commitHash = run.headSha,
                        commitMessage = run.headCommit?.message ?: "",
                        commitAuthor = run.headCommit?.author?.name ?: "",
                        startedAt = parseDate(run.createdAt),
                        finishedAt = parseDate(run.updatedAt),
                        duration = 0L, // Calculate from timestamps
                        triggeredBy = run.headCommit?.author?.name ?: "",
                        webUrl = run.htmlUrl ?: account.baseUrl,
                        provider = CIProvider.GITHUB_ACTIONS
                    )
                }
            } else {
                Timber.w("GitHub API call failed: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch GitHub pipelines")
            emptyList()
        }
    }

    private suspend fun fetchGitLabPipelines(account: Account, token: String): List<Pipeline> {
        return try {
            // Parse project ID from account metadata
            val projectId = account.baseUrl.substringAfterLast("/")

            val response = gitLabService.getPipelines(projectId)

            if (response.isSuccessful && response.body() != null) {
                response.body()!!.map { pipeline ->
                    Pipeline(
                        id = pipeline.id.toString(),
                        accountId = account.id,
                        repositoryName = projectId,
                        repositoryUrl = account.baseUrl,
                        branch = pipeline.ref ?: "main",
                        buildNumber = pipeline.id.toInt(),
                        status = mapGitLabStatus(pipeline.status),
                        commitHash = pipeline.sha ?: "",
                        commitMessage = "",
                        commitAuthor = "",
                        startedAt = parseDate(pipeline.createdAt),
                        finishedAt = parseDate(pipeline.updatedAt),
                        duration = 0L,
                        triggeredBy = "",
                        webUrl = pipeline.webUrl ?: account.baseUrl,
                        provider = CIProvider.GITLAB_CI
                    )
                }
            } else {
                Timber.w("GitLab API call failed: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch GitLab pipelines")
            emptyList()
        }
    }

    private suspend fun fetchJenkinsPipelines(account: Account, token: String): List<Pipeline> {
        return try {
            val response = jenkinsService.getJobs()

            if (response.isSuccessful && response.body() != null) {
                response.body()!!.jobs.mapNotNull { job ->
                    job.lastBuild?.let { build ->
                        Pipeline(
                            id = "${job.name}-${build.number}",
                            accountId = account.id,
                            repositoryName = job.name,
                            repositoryUrl = job.url,
                            branch = "main",
                            buildNumber = build.number,
                            status = mapJenkinsStatus(job.color, build.result),
                            commitHash = "",
                            commitMessage = "",
                            commitAuthor = "",
                            startedAt = build.timestamp,
                            finishedAt = build.timestamp + build.duration,
                            duration = build.duration,
                            triggeredBy = "",
                            webUrl = job.url,
                            provider = CIProvider.JENKINS
                        )
                    }
                }
            } else {
                Timber.w("Jenkins API call failed: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch Jenkins pipelines")
            emptyList()
        }
    }

    private suspend fun fetchCircleCIPipelines(account: Account, token: String): List<Pipeline> {
        return try {
            // Parse org/project from account metadata
            val parts = account.baseUrl.split("/")
            val org = parts.getOrNull(parts.size - 2) ?: "org"
            val project = parts.getOrNull(parts.size - 1) ?: "project"

            val response = circleCIService.getPipelines("github", org, project)

            if (response.isSuccessful && response.body() != null) {
                response.body()!!.items.map { pipeline ->
                    Pipeline(
                        id = pipeline.id,
                        accountId = account.id,
                        repositoryName = project,
                        repositoryUrl = account.baseUrl,
                        branch = pipeline.vcs?.branch ?: "main",
                        buildNumber = pipeline.number,
                        status = mapCircleCIStatus(pipeline.state),
                        commitHash = "",
                        commitMessage = pipeline.vcs?.commit?.subject ?: "",
                        commitAuthor = "",
                        startedAt = parseDate(pipeline.createdAt),
                        finishedAt = parseDate(pipeline.updatedAt),
                        duration = 0L,
                        triggeredBy = "",
                        webUrl = account.baseUrl,
                        provider = CIProvider.CIRCLE_CI
                    )
                }
            } else {
                Timber.w("CircleCI API call failed: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch CircleCI pipelines")
            emptyList()
        }
    }

    private suspend fun fetchAzureDevOpsPipelines(account: Account, token: String): List<Pipeline> {
        return try {
            // Parse organization/project from account metadata
            val parts = account.baseUrl.split("/")
            val organization = parts.getOrNull(parts.size - 2) ?: "org"
            val project = parts.getOrNull(parts.size - 1) ?: "project"

            val response = azureDevOpsService.getBuilds(organization, project)

            if (response.isSuccessful && response.body() != null) {
                response.body()!!.value.map { build ->
                    Pipeline(
                        id = build.id.toString(),
                        accountId = account.id,
                        repositoryName = build.repository?.name ?: project,
                        repositoryUrl = build.repository?.url ?: account.baseUrl,
                        branch = build.sourceBranch?.substringAfterLast("/") ?: "main",
                        buildNumber = build.id,
                        status = mapAzureStatus(build.status, build.result),
                        commitHash = build.sourceVersion ?: "",
                        commitMessage = "",
                        commitAuthor = build.requestedFor?.displayName ?: "",
                        startedAt = parseDate(build.startTime),
                        finishedAt = parseDate(build.finishTime),
                        duration = 0L,
                        triggeredBy = build.requestedFor?.displayName ?: "",
                        webUrl = account.baseUrl,
                        provider = CIProvider.AZURE_DEVOPS
                    )
                }
            } else {
                Timber.w("Azure DevOps API call failed: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch Azure DevOps pipelines")
            emptyList()
        }
    }

    // Status mapping functions
    private fun mapGitHubStatus(status: String, conclusion: String?): BuildStatus {
        return when {
            status == "in_progress" || status == "queued" -> BuildStatus.RUNNING
            conclusion == "success" -> BuildStatus.SUCCESS
            conclusion == "failure" -> BuildStatus.FAILURE
            conclusion == "cancelled" -> BuildStatus.CANCELED
            else -> BuildStatus.PENDING
        }
    }

    private fun mapGitLabStatus(status: String): BuildStatus {
        return when (status) {
            "success" -> BuildStatus.SUCCESS
            "failed" -> BuildStatus.FAILURE
            "running" -> BuildStatus.RUNNING
            "pending" -> BuildStatus.PENDING
            "canceled" -> BuildStatus.CANCELED
            else -> BuildStatus.PENDING
        }
    }

    private fun mapJenkinsStatus(color: String, result: String?): BuildStatus {
        return when {
            color.contains("anime") -> BuildStatus.RUNNING
            result == "SUCCESS" || color == "blue" -> BuildStatus.SUCCESS
            result == "FAILURE" || color == "red" -> BuildStatus.FAILURE
            result == "ABORTED" -> BuildStatus.CANCELED
            else -> BuildStatus.PENDING
        }
    }

    private fun mapCircleCIStatus(state: String): BuildStatus {
        return when (state) {
            "success" -> BuildStatus.SUCCESS
            "failed", "error" -> BuildStatus.FAILURE
            "running" -> BuildStatus.RUNNING
            "canceled" -> BuildStatus.CANCELED
            else -> BuildStatus.PENDING
        }
    }

    private fun mapAzureStatus(status: String, result: String?): BuildStatus {
        return when {
            status == "inProgress" -> BuildStatus.RUNNING
            result == "succeeded" -> BuildStatus.SUCCESS
            result == "failed" -> BuildStatus.FAILURE
            result == "canceled" -> BuildStatus.CANCELED
            else -> BuildStatus.PENDING
        }
    }

    private fun parseDate(dateString: String?): Long {
        if (dateString == null) return System.currentTimeMillis()
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
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
