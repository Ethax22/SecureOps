package com.secureops.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// CircleCI Pipelines Response
data class CircleCIPipelinesResponse(
    @SerializedName("items")
    val items: List<CircleCIPipeline>,

    @SerializedName("next_page_token")
    val nextPageToken: String?
)

data class CircleCIPipeline(
    @SerializedName("id")
    val id: String,

    @SerializedName("project_slug")
    val projectSlug: String,

    @SerializedName("number")
    val number: Int,

    @SerializedName("state")
    val state: String, // created, errored, setup-pending, setup, pending

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String?,

    @SerializedName("vcs")
    val vcs: CircleCIVcs?
)

data class CircleCIVcs(
    @SerializedName("branch")
    val branch: String?,

    @SerializedName("commit")
    val commit: CircleCICommit?,

    @SerializedName("origin_repository_url")
    val originRepositoryUrl: String?
)

data class CircleCICommit(
    @SerializedName("subject")
    val subject: String?,

    @SerializedName("body")
    val body: String?
)

// CircleCI Workflows Response
data class CircleCIWorkflowsResponse(
    @SerializedName("items")
    val items: List<CircleCIWorkflow>,

    @SerializedName("next_page_token")
    val nextPageToken: String?
)

data class CircleCIWorkflow(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("status")
    val status: String, // success, running, not_run, failed, error, failing, on_hold, canceled, unauthorized

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("stopped_at")
    val stoppedAt: String?,

    @SerializedName("pipeline_id")
    val pipelineId: String
)

// CircleCI Jobs Response
data class CircleCIJobsResponse(
    @SerializedName("items")
    val items: List<CircleCIJob>,

    @SerializedName("next_page_token")
    val nextPageToken: String?
)

data class CircleCIJob(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("type")
    val type: String, // build, approval

    @SerializedName("status")
    val status: String,

    @SerializedName("started_at")
    val startedAt: String?,

    @SerializedName("stopped_at")
    val stoppedAt: String?,

    @SerializedName("job_number")
    val jobNumber: Int?
)

// CircleCI Rerun Response
data class CircleCIRerunResponse(
    @SerializedName("workflow_id")
    val workflowId: String,

    @SerializedName("message")
    val message: String?
)

// CircleCI Cancel Response
data class CircleCICancelResponse(
    @SerializedName("message")
    val message: String
)
