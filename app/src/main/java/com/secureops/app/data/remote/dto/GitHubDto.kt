package com.secureops.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GitHubWorkflowRunsResponse(
    @SerializedName("workflow_runs") val workflowRuns: List<GitHubWorkflowRun>
)

data class GitHubWorkflowRun(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("head_branch") val headBranch: String,
    @SerializedName("head_sha") val headSha: String,
    @SerializedName("run_number") val runNumber: Int,
    @SerializedName("status") val status: String,
    @SerializedName("conclusion") val conclusion: String?,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("run_started_at") val runStartedAt: String?,
    @SerializedName("repository") val repository: GitHubRepository,
    @SerializedName("head_commit") val headCommit: GitHubCommit
)

data class GitHubRepository(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("html_url") val htmlUrl: String
)

data class GitHubCommit(
    @SerializedName("id") val id: String,
    @SerializedName("message") val message: String,
    @SerializedName("author") val author: GitHubAuthor
)

data class GitHubAuthor(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String
)

data class GitHubJobsResponse(
    @SerializedName("jobs") val jobs: List<GitHubJob>
)

data class GitHubJob(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("status") val status: String,
    @SerializedName("conclusion") val conclusion: String?,
    @SerializedName("started_at") val startedAt: String?,
    @SerializedName("completed_at") val completedAt: String?
)

data class GitHubLogsResponse(
    @SerializedName("logs") val logs: String
)
