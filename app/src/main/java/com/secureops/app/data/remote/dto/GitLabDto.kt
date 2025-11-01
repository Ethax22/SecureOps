package com.secureops.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GitLabPipeline(
    @SerializedName("id") val id: Long,
    @SerializedName("sha") val sha: String,
    @SerializedName("ref") val ref: String,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("web_url") val webUrl: String,
    @SerializedName("source") val source: String?
)

data class GitLabPipelineDetails(
    @SerializedName("id") val id: Long,
    @SerializedName("sha") val sha: String,
    @SerializedName("ref") val ref: String,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("started_at") val startedAt: String?,
    @SerializedName("finished_at") val finishedAt: String?,
    @SerializedName("duration") val duration: Int?,
    @SerializedName("web_url") val webUrl: String,
    @SerializedName("user") val user: GitLabUser
)

data class GitLabUser(
    @SerializedName("name") val name: String,
    @SerializedName("username") val username: String
)

data class GitLabJob(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("status") val status: String,
    @SerializedName("stage") val stage: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("started_at") val startedAt: String?,
    @SerializedName("finished_at") val finishedAt: String?,
    @SerializedName("duration") val duration: Double?,
    @SerializedName("web_url") val webUrl: String
)

data class GitLabProject(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("name_with_namespace") val nameWithNamespace: String,
    @SerializedName("web_url") val webUrl: String
)

data class GitLabCommit(
    @SerializedName("id") val id: String,
    @SerializedName("short_id") val shortId: String,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("author_name") val authorName: String
)
