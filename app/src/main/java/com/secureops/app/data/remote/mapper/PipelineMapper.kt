package com.secureops.app.data.remote.mapper

import com.secureops.app.data.remote.dto.*
import com.secureops.app.domain.model.*
import java.text.SimpleDateFormat
import java.util.*

object PipelineMapper {
    private val iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun GitHubWorkflowRun.toPipeline(accountId: String): Pipeline {
        val status = mapGitHubStatus(this.status, this.conclusion)
        val startedAt = runStartedAt?.let { parseTimestamp(it) }
        val updatedAt = parseTimestamp(updatedAt)
        val duration = if (startedAt != null && updatedAt != null && status in listOf(
                BuildStatus.SUCCESS,
                BuildStatus.FAILURE,
                BuildStatus.CANCELED
            )
        ) {
            updatedAt - startedAt
        } else null

        return Pipeline(
            id = "github_${this.id}",
            accountId = accountId,
            provider = CIProvider.GITHUB_ACTIONS,
            repositoryName = repository.fullName,
            repositoryUrl = repository.htmlUrl,
            branch = headBranch,
            buildNumber = runNumber,
            status = status,
            commitHash = headSha,
            commitMessage = headCommit.message,
            commitAuthor = headCommit.author.name,
            startedAt = startedAt,
            finishedAt = if (status in listOf(
                    BuildStatus.SUCCESS,
                    BuildStatus.FAILURE,
                    BuildStatus.CANCELED
                )
            ) updatedAt else null,
            duration = duration,
            triggeredBy = headCommit.author.name,
            webUrl = htmlUrl
        )
    }

    fun GitLabPipelineDetails.toPipeline(
        accountId: String,
        projectName: String,
        commit: GitLabCommit?
    ): Pipeline {
        val status = mapGitLabStatus(this.status)
        val startedAt = startedAt?.let { parseTimestamp(it) }
        val finishedAt = finishedAt?.let { parseTimestamp(it) }

        return Pipeline(
            id = "gitlab_${this.id}",
            accountId = accountId,
            provider = CIProvider.GITLAB_CI,
            repositoryName = projectName,
            repositoryUrl = webUrl.substringBeforeLast("/-/"),
            branch = ref,
            buildNumber = id.toInt(),
            status = status,
            commitHash = sha,
            commitMessage = commit?.title ?: sha,
            commitAuthor = user.name,
            startedAt = startedAt,
            finishedAt = finishedAt,
            duration = duration?.toLong()?.times(1000),
            triggeredBy = user.name,
            webUrl = webUrl
        )
    }

    fun GitHubJob.toJob(): Job {
        val status = mapGitHubStatus(this.status, this.conclusion)
        val startedAt = startedAt?.let { parseTimestamp(it) }
        val completedAt = completedAt?.let { parseTimestamp(it) }
        val duration = if (startedAt != null && completedAt != null) {
            completedAt - startedAt
        } else null

        return Job(
            id = "github_job_${this.id}",
            name = name,
            status = status,
            startedAt = startedAt,
            finishedAt = completedAt,
            duration = duration
        )
    }

    fun GitLabJob.toJob(): Job {
        val status = mapGitLabStatus(this.status)
        val startedAt = startedAt?.let { parseTimestamp(it) }
        val finishedAt = finishedAt?.let { parseTimestamp(it) }
        val duration = this.duration?.toLong()?.times(1000)

        return Job(
            id = "gitlab_job_${this.id}",
            name = "$stage: $name",
            status = status,
            startedAt = startedAt,
            finishedAt = finishedAt,
            duration = duration
        )
    }

    private fun mapGitHubStatus(status: String, conclusion: String?): BuildStatus {
        return when {
            status == "queued" -> BuildStatus.QUEUED
            status == "in_progress" -> BuildStatus.RUNNING
            conclusion == "success" -> BuildStatus.SUCCESS
            conclusion == "failure" -> BuildStatus.FAILURE
            conclusion == "cancelled" -> BuildStatus.CANCELED
            conclusion == "skipped" -> BuildStatus.SKIPPED
            else -> BuildStatus.UNKNOWN
        }
    }

    private fun mapGitLabStatus(status: String): BuildStatus {
        return when (status.lowercase()) {
            "created", "pending" -> BuildStatus.PENDING
            "running" -> BuildStatus.RUNNING
            "success" -> BuildStatus.SUCCESS
            "failed" -> BuildStatus.FAILURE
            "canceled" -> BuildStatus.CANCELED
            "skipped" -> BuildStatus.SKIPPED
            else -> BuildStatus.UNKNOWN
        }
    }

    private fun parseTimestamp(timestamp: String): Long {
        return try {
            iso8601Format.parse(timestamp)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
