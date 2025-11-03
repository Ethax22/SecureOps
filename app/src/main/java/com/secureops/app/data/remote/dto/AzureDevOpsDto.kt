package com.secureops.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// Azure DevOps Builds Response
data class AzureBuildsResponse(
    @SerializedName("count")
    val count: Int,

    @SerializedName("value")
    val value: List<AzureBuild>
)

data class AzureBuild(
    @SerializedName("id")
    val id: Int,

    @SerializedName("buildNumber")
    val buildNumber: String,

    @SerializedName("status")
    val status: String, // inProgress, completed, cancelling, postponed, notStarted

    @SerializedName("result")
    val result: String?, // succeeded, partiallySucceeded, failed, canceled

    @SerializedName("queueTime")
    val queueTime: String,

    @SerializedName("startTime")
    val startTime: String?,

    @SerializedName("finishTime")
    val finishTime: String?,

    @SerializedName("sourceBranch")
    val sourceBranch: String?,

    @SerializedName("sourceVersion")
    val sourceVersion: String?,

    @SerializedName("repository")
    val repository: AzureRepository?,

    @SerializedName("requestedFor")
    val requestedFor: AzureIdentity?,

    @SerializedName("definition")
    val definition: AzureDefinition?
)

data class AzureRepository(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("url")
    val url: String?,

    @SerializedName("type")
    val type: String? // TfsGit, GitHub, etc.
)

data class AzureIdentity(
    @SerializedName("displayName")
    val displayName: String,

    @SerializedName("uniqueName")
    val uniqueName: String?,

    @SerializedName("id")
    val id: String
)

data class AzureDefinition(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("path")
    val path: String?
)

// Azure DevOps Logs Response
data class AzureLogsResponse(
    @SerializedName("count")
    val count: Int,

    @SerializedName("value")
    val value: List<AzureLogRecord>
)

data class AzureLogRecord(
    @SerializedName("id")
    val id: Int,

    @SerializedName("type")
    val type: String,

    @SerializedName("url")
    val url: String,

    @SerializedName("lineCount")
    val lineCount: Int?
)

// Azure DevOps Cancel Request
data class AzureCancelRequest(
    @SerializedName("status")
    val status: String = "cancelling"
)
