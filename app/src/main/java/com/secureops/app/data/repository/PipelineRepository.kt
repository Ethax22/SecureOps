package com.secureops.app.data.repository

import com.secureops.app.data.local.dao.PipelineDao
import com.secureops.app.data.local.entity.toEntity
import com.secureops.app.data.local.entity.toDomain
import com.secureops.app.data.remote.api.*
import com.secureops.app.data.remote.mapper.PipelineMapper
import com.secureops.app.data.remote.mapper.PipelineMapper.toPipeline
import com.secureops.app.domain.model.*
import com.secureops.app.ml.FailurePredictionModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import com.google.gson.Gson
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import java.io.File

class PipelineRepository(
    private val pipelineDao: PipelineDao,
    private val gitHubService: GitHubService,
    private val gitLabService: GitLabService,
    private val jenkinsService: JenkinsService,
    private val circleCIService: CircleCIService,
    private val azureDevOpsService: AzureDevOpsService,
    private val accountRepository: AccountRepository,
    private val failurePredictionModel: FailurePredictionModel,
    private val gson: Gson
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

    suspend fun updatePipelineWithLogs(pipeline: Pipeline) {
        try {
            pipelineDao.updatePipeline(pipeline.toEntity())
            Timber.d("Updated pipeline ${pipeline.id} with cached logs")
        } catch (e: Exception) {
            Timber.e(e, "Failed to update pipeline with logs")
            throw e
        }
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

            // Get existing pipelines from database to preserve predictions
            val existingPipelines = pipelineDao.getAllPipelines()
                .map { entities -> entities.map { it.toDomain() } }
                .first()

            val existingPredictions = existingPipelines
                .associate { it.id to it.failurePrediction }

            // Merge predictions into new pipeline data
            val pipelinesWithPredictions = pipelines.map { pipeline ->
                val existingPrediction = existingPredictions[pipeline.id]
                if (existingPrediction != null) {
                    pipeline.copy(failurePrediction = existingPrediction)
                } else {
                    pipeline
                }
            }

            // Cache pipelines locally with preserved predictions
            pipelineDao.insertPipelines(pipelinesWithPredictions.map { it.toEntity() })

            // Update last sync time
            accountRepository.updateLastSyncTime(accountId)

            Timber.d("Synced ${pipelines.size} pipelines for account: ${account.name} (${account.provider})")
            Result.success(pipelinesWithPredictions)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync pipelines for account ID: $accountId")
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
            // Create a dynamic Jenkins service with the correct base URL and authentication
            val jenkinsServiceDynamic = createJenkinsService(account.baseUrl, token)
            
            val response = jenkinsServiceDynamic.getJobs()

            if (response.isSuccessful && response.body() != null) {
                val jobsResponse = response.body()!!
                Timber.d("Jenkins API response: ${jobsResponse.jobs.size} jobs found")

                jobsResponse.jobs.mapNotNull { job ->
                    try {
                        job.lastBuild?.let { build ->
                            Pipeline(
                                id = "${job.name}-${build.number}",
                                accountId = account.id,
                                repositoryName = job.name,
                                repositoryUrl = job.url,
                                branch = "main", // Jenkins doesn't have branch info in this API call
                                buildNumber = build.number,
                                status = mapJenkinsStatus(job.color, build.result),
                                commitHash = "",
                                commitMessage = "", // We could fetch more detailed info from individual job API
                                commitAuthor = "",
                                startedAt = if (build.timestamp > 0) build.timestamp else null,
                                finishedAt = if (build.timestamp > 0 && build.duration > 0)
                                    build.timestamp + build.duration else null,
                                duration = if (build.duration > 0) build.duration else null,
                                triggeredBy = "", // We could get this from build details API
                                webUrl = job.url,
                                provider = CIProvider.JENKINS
                            )
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to process Jenkins job: ${job.name}")
                        null
                    }
                }.also {
                    Timber.d("Fetched ${it.size} Jenkins pipelines from ${account.baseUrl}")
                }
            } else {
                Timber.w("Jenkins API call failed: ${response.code()} - ${response.message()}")
                Timber.w("Response body: ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch Jenkins pipelines for account ${account.name}")
            emptyList()
        }
    }

    private fun mapJenkinsStatus(color: String, result: String?): BuildStatus {
        return when {
            color.contains("anime") -> BuildStatus.RUNNING // Anime indicates running job in Jenkins
            result == "SUCCESS" -> BuildStatus.SUCCESS
            result == "FAILURE" -> BuildStatus.FAILURE
            result == "UNSTABLE" -> BuildStatus.FAILURE // Treat unstable as failure
            result == "ABORTED" -> BuildStatus.CANCELED
            color == "blue" -> BuildStatus.SUCCESS // Blue means success in Jenkins
            color == "red" -> BuildStatus.FAILURE // Red means failure in Jenkins
            color == "yellow" -> BuildStatus.FAILURE // Yellow means unstable in Jenkins
            else -> {
                Timber.w("Unknown Jenkins status: color=$color, result=$result")
                BuildStatus.UNKNOWN
            }
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

    /**
     * Create a dynamic Jenkins service with proper base URL normalization and Basic authentication.
     */
    private fun createJenkinsService(baseUrl: String, token: String): JenkinsService {
        // Normalize base URL to ensure it ends with a /
        val normalizedBaseUrl = when {
            baseUrl.isEmpty() -> throw IllegalArgumentException("Jenkins baseUrl must not be empty.")
            baseUrl.endsWith("/") -> baseUrl
            else -> "$baseUrl/"
        }

        // Handle Basic Auth: encode token if it's not already
        val base64Token = if (token.contains(":")) {
            android.util.Base64.encodeToString(token.toByteArray(), android.util.Base64.NO_WRAP)
        } else {
            token // Assume token already encoded
        }

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Authorization", "Basic $base64Token")
                .method(original.method, original.body)
            val request = requestBuilder.build()
            chain.proceed(request)
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (com.secureops.app.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(JenkinsService::class.java)
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
            Timber.d("🤖 Running ML prediction for pipeline: ${pipeline.id}")

            // Fetch real data instead of empty strings
            
            // 1. Fetch build logs
            val logs = try {
                val logsResult = fetchBuildLogs(pipeline)
                logsResult.getOrNull() ?: ""
            } catch (e: Exception) {
                Timber.w("Could not fetch logs for prediction: ${e.message}")
                ""
            }

            // 2. Get test history from database (last 20 builds)
            val testHistory = try {
                withContext(Dispatchers.IO) {
                    val allPipelines = pipelineDao.getAllPipelines()
                        .map { entities -> entities.map { it.toDomain() } }
                        .first()

                    allPipelines
                        .filter { p -> p.repositoryName == pipeline.repositoryName && p.accountId == pipeline.accountId }
                        .sortedByDescending { p -> p.startedAt ?: 0 }
                        .take(20)
                        .map { p -> p.status == BuildStatus.SUCCESS }
                }
            } catch (e: Exception) {
                Timber.w("Could not fetch test history for prediction: ${e.message}")
                emptyList()
            }

            // 3. Commit diff (simplified - use commit message as proxy)
            // In production, you'd fetch actual diff from Git API
            val commitDiff = pipeline.commitMessage

            Timber.d("Prediction inputs - Logs: ${logs.length} chars, History: ${testHistory.size} builds, Commit: ${commitDiff.length} chars")

            // Run prediction with real data
            val (riskPercentage, confidence) = failurePredictionModel.predictFailure(
                commitDiff, testHistory, logs
            )

            // Get causal factors
            val causalFactors = failurePredictionModel.identifyCausalFactors(
                commitDiff, testHistory, logs
            )

            Timber.i("🎯 Prediction result: ${riskPercentage.toInt()}% risk (${(confidence * 100).toInt()}% confidence)")
            if (causalFactors.isNotEmpty()) {
                Timber.d("Causal factors: ${causalFactors.joinToString(", ")}")
            }

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
            Timber.e(e, "Failed to predict failure for pipeline: ${pipeline.id}")
            pipeline
        }
    }

    suspend fun cleanOldPipelines(daysToKeep: Int = 30) {
        val timestamp = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        pipelineDao.deleteOldPipelines(timestamp)
    }

    /**
     * Fetch build logs for a specific pipeline
     */
    suspend fun fetchBuildLogs(pipeline: Pipeline): Result<String> {
        return try {
            when (pipeline.provider) {
                CIProvider.JENKINS -> fetchJenkinsBuildLogs(pipeline)
                else -> Result.success("Logs not yet implemented for ${pipeline.provider}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch build logs for pipeline: ${pipeline.id}")
            Result.failure(e)
        }
    }

    /**
     * Fetch Jenkins build logs
     */
    private suspend fun fetchJenkinsBuildLogs(pipeline: Pipeline): Result<String> {
        return try {
            val account = accountRepository.getAccountById(pipeline.accountId)
                ?: return Result.failure(Exception("Account not found"))

            val token = accountRepository.getAccountToken(pipeline.accountId)
                ?: return Result.failure(Exception("Token not found"))

            // Create dynamic Jenkins service with account credentials
            val jenkinsServiceDynamic = createJenkinsService(account.baseUrl, token)

            // Extract job name from repository name
            val jobName = pipeline.repositoryName
            val buildNumber = pipeline.buildNumber

            Timber.d("Fetching logs for Jenkins job: $jobName #$buildNumber")
            Timber.d("Jenkins URL: ${account.baseUrl}")

            try {
                val response = jenkinsServiceDynamic.getBuildLog(jobName, buildNumber)

                Timber.d("Log fetch response: code=${response.code()}, isSuccessful=${response.isSuccessful}")

                if (response.isSuccessful && response.body() != null) {
                    val logs = response.body()!!
                    Timber.d("Successfully fetched ${logs.length} characters of logs")
                    Result.success(logs)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg =
                        "Failed to fetch logs: ${response.code()} - ${response.message()}"
                    Timber.w("$errorMsg, errorBody: $errorBody")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: java.net.SocketTimeoutException) {
                val errorMsg =
                    "Timeout fetching logs (> 120s). Log file might be too large or network is slow."
                Timber.e(e, errorMsg)
                Result.failure(Exception(errorMsg))
            } catch (e: java.io.IOException) {
                val errorMsg = "Network error fetching logs: ${e.message}"
                Timber.e(e, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching Jenkins build logs")
            Result.failure(e)
        }
    }

    /**
     * Get artifacts for a pipeline
     */
    suspend fun getArtifacts(pipeline: Pipeline): Result<List<BuildArtifact>> {
        return try {
            val account = accountRepository.getAccountById(pipeline.accountId)
                ?: return Result.failure(Exception("Account not found"))

            val token = accountRepository.getAccountToken(pipeline.accountId)
                ?: return Result.failure(Exception("Token not found"))

            when (pipeline.provider) {
                CIProvider.GITHUB_ACTIONS -> fetchGitHubArtifacts(pipeline, token)
                CIProvider.JENKINS -> fetchJenkinsArtifacts(pipeline, token)
                CIProvider.GITLAB_CI -> fetchGitLabArtifacts(pipeline, token)
                CIProvider.CIRCLE_CI -> fetchCircleCIArtifacts(pipeline, token)
                CIProvider.AZURE_DEVOPS -> fetchAzureDevOpsArtifacts(pipeline, token)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch artifacts")
            Result.failure(e)
        }
    }

    /**
     * Download artifact to file
     */
    suspend fun downloadArtifact(
        artifact: BuildArtifact,
        destination: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val response = gitHubService.downloadArtifact(artifact.downloadUrl)

            if (response.isSuccessful && response.body() != null) {
                response.body()!!.byteStream().use { input ->
                    destination.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Timber.d("Downloaded artifact to: ${destination.absolutePath}")
                Result.success(destination)
            } else {
                Result.failure(Exception("Download failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Artifact download failed")
            Result.failure(e)
        }
    }

    private suspend fun fetchGitHubArtifacts(
        pipeline: Pipeline,
        token: String
    ): Result<List<BuildArtifact>> {
        return try {
            val parts = pipeline.repositoryUrl.split("/")
            val owner = parts.getOrNull(parts.size - 2) ?: return Result.success(emptyList())
            val repo = parts.getOrNull(parts.size - 1) ?: return Result.success(emptyList())

            val response = gitHubService.getArtifacts(owner, repo, pipeline.id.toLong())

            if (response.isSuccessful && response.body() != null) {
                val artifacts = response.body()!!.artifacts.map { artifact ->
                    BuildArtifact(
                        id = artifact.id.toString(),
                        name = artifact.name,
                        size = artifact.sizeInBytes,
                        downloadUrl = artifact.archiveDownloadUrl,
                        contentType = "application/zip",
                        createdAt = parseDate(artifact.createdAt)
                    )
                }
                Timber.d("Fetched ${artifacts.size} artifacts for pipeline ${pipeline.id}")
                Result.success(artifacts)
            } else {
                Result.failure(Exception("Failed to fetch artifacts: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch GitHub artifacts")
            Result.failure(e)
        }
    }

    private suspend fun fetchJenkinsArtifacts(
        pipeline: Pipeline,
        token: String
    ): Result<List<BuildArtifact>> {
        return try {
            // Jenkins artifacts would be fetched from /job/{name}/{build}/api/json
            // For now, return empty list as placeholder
            Timber.d("Jenkins artifacts not yet implemented")
            Result.success(emptyList())
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch Jenkins artifacts")
            Result.failure(e)
        }
    }

    private suspend fun fetchGitLabArtifacts(
        pipeline: Pipeline,
        token: String
    ): Result<List<BuildArtifact>> {
        return try {
            // GitLab artifacts endpoint: /projects/{id}/jobs/{job_id}/artifacts
            Timber.d("GitLab artifacts not yet implemented")
            Result.success(emptyList())
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch GitLab artifacts")
            Result.failure(e)
        }
    }

    private suspend fun fetchCircleCIArtifacts(
        pipeline: Pipeline,
        token: String
    ): Result<List<BuildArtifact>> {
        return try {
            // CircleCI artifacts endpoint
            Timber.d("CircleCI artifacts not yet implemented")
            Result.success(emptyList())
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch CircleCI artifacts")
            Result.failure(e)
        }
    }

    private suspend fun fetchAzureDevOpsArtifacts(
        pipeline: Pipeline,
        token: String
    ): Result<List<BuildArtifact>> {
        return try {
            // Azure DevOps artifacts endpoint
            Timber.d("Azure DevOps artifacts not yet implemented")
            Result.success(emptyList())
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch Azure DevOps artifacts")
            Result.failure(e)
        }
    }
}
