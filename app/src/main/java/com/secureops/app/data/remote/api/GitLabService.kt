package com.secureops.app.data.remote.api

import com.secureops.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface GitLabService {
    @GET("projects/{id}/pipelines")
    suspend fun getPipelines(
        @Path("id") projectId: String,
        @Query("per_page") perPage: Int = 50,
        @Query("page") page: Int = 1
    ): Response<List<GitLabPipeline>>

    @GET("projects/{id}/pipelines/{pipeline_id}")
    suspend fun getPipeline(
        @Path("id") projectId: String,
        @Path("pipeline_id") pipelineId: Long
    ): Response<GitLabPipelineDetails>

    @GET("projects/{id}/pipelines/{pipeline_id}/jobs")
    suspend fun getPipelineJobs(
        @Path("id") projectId: String,
        @Path("pipeline_id") pipelineId: Long
    ): Response<List<GitLabJob>>

    @GET("projects/{id}/jobs/{job_id}/trace")
    suspend fun getJobTrace(
        @Path("id") projectId: String,
        @Path("job_id") jobId: Long
    ): Response<String>

    @POST("projects/{id}/pipelines/{pipeline_id}/retry")
    suspend fun retryPipeline(
        @Path("id") projectId: String,
        @Path("pipeline_id") pipelineId: Long
    ): Response<GitLabPipeline>

    @POST("projects/{id}/pipelines/{pipeline_id}/cancel")
    suspend fun cancelPipeline(
        @Path("id") projectId: String,
        @Path("pipeline_id") pipelineId: Long
    ): Response<GitLabPipeline>

    @GET("projects")
    suspend fun getProjects(
        @Query("membership") membership: Boolean = true,
        @Query("per_page") perPage: Int = 50
    ): Response<List<GitLabProject>>

    @GET("projects/{id}/repository/commits/{sha}")
    suspend fun getCommit(
        @Path("id") projectId: String,
        @Path("sha") sha: String
    ): Response<GitLabCommit>
}
