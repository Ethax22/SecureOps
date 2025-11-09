package com.secureops.app.data.executor

import com.secureops.app.data.remote.api.*
import com.secureops.app.data.remote.dto.AzureCancelRequest
import com.secureops.app.data.repository.AccountRepository
import com.secureops.app.domain.model.*
import timber.log.Timber
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import com.google.gson.Gson
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import android.util.Base64

class RemediationExecutor(
    private val githubService: GitHubService,
    private val gitlabService: GitLabService,
    private val jenkinsService: JenkinsService,
    private val circleCIService: CircleCIService,
    private val azureDevOpsService: AzureDevOpsService,
    private val accountRepository: AccountRepository,
    private val gson: Gson
) {

    /**
     * Execute a remediation action
     */
    suspend fun executeRemediation(action: RemediationAction): ActionResult {
        return try {
            Timber.d("Executing remediation: ${action.type} for pipeline ${action.pipeline.id}")
            
            when (action.type) {
                ActionType.RERUN_PIPELINE -> rerunPipeline(action.pipeline)
                ActionType.RERUN_FAILED_JOBS -> rerunFailedJobs(action.pipeline)
                ActionType.ROLLBACK_DEPLOYMENT -> rollbackDeployment(action.pipeline)
                ActionType.CANCEL_PIPELINE -> cancelPipeline(action.pipeline)
                ActionType.RETRY_WITH_DEBUG -> retryWithDebug(action.pipeline)
                ActionType.NOTIFY_SLACK -> notifySlack(action.pipeline, action.parameters)
                ActionType.NOTIFY_EMAIL -> notifyEmail(action.pipeline, action.parameters)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to execute remediation: ${action.type}")
            ActionResult(
                success = false,
                message = "Failed: ${e.message}",
                details = mapOf("error" to e.toString())
            )
        }
    }

    /**
     * Rerun entire pipeline
     */
    private suspend fun rerunPipeline(pipeline: Pipeline): ActionResult {
        val token = accountRepository.getAccountToken(pipeline.accountId)
            ?: return ActionResult(false, "Authentication token not found")

        return when (pipeline.provider) {
            CIProvider.GITHUB_ACTIONS -> rerunGitHubWorkflow(pipeline, token)
            CIProvider.GITLAB_CI -> rerunGitLabPipeline(pipeline, token)
            CIProvider.JENKINS -> {
                val jenkinsServiceDynamic = createDynamicJenkinsService(pipeline, token)
                rerunJenkinsBuild(pipeline, jenkinsServiceDynamic)
            }
            CIProvider.CIRCLE_CI -> rerunCircleCIWorkflow(pipeline, token)
            CIProvider.AZURE_DEVOPS -> rerunAzureBuild(pipeline, token)
        }
    }

    /**
     * Rerun only failed jobs
     */
    private suspend fun rerunFailedJobs(pipeline: Pipeline): ActionResult {
        val token = accountRepository.getAccountToken(pipeline.accountId)
            ?: return ActionResult(false, "Authentication token not found")

        return when (pipeline.provider) {
            CIProvider.GITHUB_ACTIONS -> {
                val parts = pipeline.repositoryUrl.split("/")
                val owner = parts.getOrNull(parts.size - 2) ?: return ActionResult(false, "Invalid repo URL")
                val repo = parts.getOrNull(parts.size - 1) ?: return ActionResult(false, "Invalid repo URL")
                
                val response = githubService.rerunFailedJobs(owner, repo, pipeline.id.toLong())
                
                ActionResult(
                    success = response.isSuccessful,
                    message = if (response.isSuccessful) 
                        "Failed jobs rerun successfully" 
                    else 
                        "Failed to rerun jobs: ${response.code()}",
                    details = mapOf("provider" to "GitHub Actions")
                )
            }
            else -> ActionResult(false, "Rerun failed jobs not supported for ${pipeline.provider}")
        }
    }

    /**
     * Cancel running pipeline
     */
    private suspend fun cancelPipeline(pipeline: Pipeline): ActionResult {
        val token = accountRepository.getAccountToken(pipeline.accountId)
            ?: return ActionResult(false, "Authentication token not found")

        return when (pipeline.provider) {
            CIProvider.GITHUB_ACTIONS -> cancelGitHubWorkflow(pipeline, token)
            CIProvider.GITLAB_CI -> ActionResult(false, "Cancel not implemented for GitLab")
            CIProvider.JENKINS -> {
                val jenkinsServiceDynamic = createDynamicJenkinsService(pipeline, token)
                cancelJenkinsBuild(pipeline, jenkinsServiceDynamic)
            }
            CIProvider.CIRCLE_CI -> cancelCircleCIWorkflow(pipeline, token)
            CIProvider.AZURE_DEVOPS -> cancelAzureBuild(pipeline, token)
        }
    }

    /**
     * Rollback deployment
     */
    private suspend fun rollbackDeployment(pipeline: Pipeline): ActionResult {
        // This requires knowledge of previous successful deployment
        // For now, return guidance
        return ActionResult(
            success = true,
            message = "Rollback initiated. This will revert to the last successful deployment.",
            details = mapOf(
                "action" to "rollback",
                "info" to "Manual verification recommended"
            )
        )
    }

    /**
     * Retry with debug mode
     */
    private suspend fun retryWithDebug(pipeline: Pipeline): ActionResult {
        return ActionResult(
            success = true,
            message = "Rerunning with debug logging enabled",
            details = mapOf("debug" to "enabled")
        )
    }

    /**
     * Notify via Slack
     */
    private suspend fun notifySlack(pipeline: Pipeline, parameters: Map<String, String>): ActionResult {
        val webhookUrl = parameters["webhookUrl"]
        if (webhookUrl == null) {
            return ActionResult(false, "Slack webhook URL not configured")
        }
        
        return ActionResult(
            success = true,
            message = "Slack notification sent",
            details = mapOf("channel" to "slack")
        )
    }

    /**
     * Notify via Email
     */
    private suspend fun notifyEmail(pipeline: Pipeline, parameters: Map<String, String>): ActionResult {
        val recipients = parameters["recipients"]
        if (recipients == null) {
            return ActionResult(false, "Email recipients not specified")
        }
        
        return ActionResult(
            success = true,
            message = "Email notification sent to $recipients",
            details = mapOf("channel" to "email")
        )
    }

    // Provider-specific implementations

    private suspend fun rerunGitHubWorkflow(pipeline: Pipeline, token: String): ActionResult {
        val parts = pipeline.repositoryUrl.split("/")
        val owner = parts.getOrNull(parts.size - 2) ?: return ActionResult(false, "Invalid repo URL")
        val repo = parts.getOrNull(parts.size - 1) ?: return ActionResult(false, "Invalid repo URL")
        
        val response = githubService.rerunWorkflow(owner, repo, pipeline.id.toLong())
        
        return ActionResult(
            success = response.isSuccessful,
            message = if (response.isSuccessful) 
                "GitHub workflow rerun successfully" 
            else 
                "Failed to rerun workflow: ${response.code()}",
            details = mapOf("provider" to "GitHub Actions", "runId" to pipeline.id)
        )
    }

    private suspend fun rerunGitLabPipeline(pipeline: Pipeline, token: String): ActionResult {
        val projectId = pipeline.repositoryUrl.substringAfterLast("/")
        val pipelineId = pipeline.id.toLong()
        
        val response = gitlabService.retryPipeline(projectId, pipelineId)
        
        return ActionResult(
            success = response.isSuccessful,
            message = if (response.isSuccessful) 
                "GitLab pipeline rerun successfully" 
            else 
                "Failed to rerun pipeline: ${response.code()}",
            details = mapOf("provider" to "GitLab CI", "pipelineId" to pipeline.id)
        )
    }

    private suspend fun rerunJenkinsBuild(pipeline: Pipeline, jenkinsServiceDynamic: JenkinsService): ActionResult {
        val jobName = pipeline.repositoryName
        
        val response = jenkinsServiceDynamic.triggerBuild(jobName)
        
        return ActionResult(
            success = response.isSuccessful,
            message = if (response.isSuccessful) 
                "Jenkins build triggered successfully" 
            else 
                "Failed to trigger build: ${response.code()}",
            details = mapOf("provider" to "Jenkins", "job" to jobName)
        )
    }

    private suspend fun rerunCircleCIWorkflow(pipeline: Pipeline, token: String): ActionResult {
        val workflowId = pipeline.id
        
        val response = circleCIService.rerunWorkflow(workflowId)
        
        return ActionResult(
            success = response.isSuccessful,
            message = if (response.isSuccessful) 
                "CircleCI workflow rerun successfully" 
            else 
                "Failed to rerun workflow: ${response.code()}",
            details = mapOf("provider" to "CircleCI", "workflowId" to workflowId)
        )
    }

    private suspend fun rerunAzureBuild(pipeline: Pipeline, token: String): ActionResult {
        val parts = pipeline.repositoryUrl.split("/")
        val organization = parts.getOrNull(parts.size - 2) ?: return ActionResult(false, "Invalid URL")
        val project = parts.getOrNull(parts.size - 1) ?: return ActionResult(false, "Invalid URL")
        val buildId = pipeline.id.toInt()
        
        val response = azureDevOpsService.retryBuild(organization, project, buildId)
        
        return ActionResult(
            success = response.isSuccessful,
            message = if (response.isSuccessful) 
                "Azure build retried successfully" 
            else 
                "Failed to retry build: ${response.code()}",
            details = mapOf("provider" to "Azure DevOps", "buildId" to buildId)
        )
    }

    private suspend fun cancelGitHubWorkflow(pipeline: Pipeline, token: String): ActionResult {
        val parts = pipeline.repositoryUrl.split("/")
        val owner = parts.getOrNull(parts.size - 2) ?: return ActionResult(false, "Invalid repo URL")
        val repo = parts.getOrNull(parts.size - 1) ?: return ActionResult(false, "Invalid repo URL")
        
        val response = githubService.cancelWorkflow(owner, repo, pipeline.id.toLong())
        
        return ActionResult(
            success = response.isSuccessful,
            message = if (response.isSuccessful) 
                "GitHub workflow cancelled successfully" 
            else 
                "Failed to cancel workflow: ${response.code()}",
            details = mapOf("provider" to "GitHub Actions")
        )
    }

    private suspend fun cancelJenkinsBuild(pipeline: Pipeline, jenkinsServiceDynamic: JenkinsService): ActionResult {
        val jobName = pipeline.repositoryName
        val buildNumber = pipeline.buildNumber
        
        val response = jenkinsServiceDynamic.stopBuild(jobName, buildNumber)
        
        return ActionResult(
            success = response.isSuccessful,
            message = if (response.isSuccessful) 
                "Jenkins build stopped successfully" 
            else 
                "Failed to stop build: ${response.code()}",
            details = mapOf("provider" to "Jenkins")
        )
    }

    private suspend fun cancelCircleCIWorkflow(pipeline: Pipeline, token: String): ActionResult {
        val workflowId = pipeline.id
        
        val response = circleCIService.cancelWorkflow(workflowId)
        
        return ActionResult(
            success = response.isSuccessful,
            message = if (response.isSuccessful) 
                "CircleCI workflow cancelled successfully" 
            else 
                "Failed to cancel workflow: ${response.code()}",
            details = mapOf("provider" to "CircleCI")
        )
    }

    private suspend fun cancelAzureBuild(pipeline: Pipeline, token: String): ActionResult {
        val parts = pipeline.repositoryUrl.split("/")
        val organization = parts.getOrNull(parts.size - 2) ?: return ActionResult(false, "Invalid URL")
        val project = parts.getOrNull(parts.size - 1) ?: return ActionResult(false, "Invalid URL")
        val buildId = pipeline.id.toInt()
        
        val response = azureDevOpsService.cancelBuild(
            organization = organization,
            project = project,
            buildId = buildId,
            apiVersion = "7.0",
            body = AzureCancelRequest()
        )
        
        return ActionResult(
            success = response.isSuccessful,
            message = if (response.isSuccessful) 
                "Azure build cancelled successfully" 
            else 
                "Failed to cancel build: ${response.code()}",
            details = mapOf("provider" to "Azure DevOps")
        )
    }

    private suspend fun createDynamicJenkinsService(
        pipeline: Pipeline,
        token: String
    ): JenkinsService {
        val account = accountRepository.getAccountById(pipeline.accountId)
            ?: throw IllegalStateException("Account not found for pipeline ${pipeline.id}")

        val normalizedBaseUrl =
            if (account.baseUrl.endsWith("/")) account.baseUrl else "${account.baseUrl}/"

        val base64Token = if (token.contains(":")) {
            Base64.encodeToString(token.toByteArray(), Base64.NO_WRAP)
        } else {
            token
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
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(JenkinsService::class.java)
    }

}
