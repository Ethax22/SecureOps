package com.secureops.app.data.remote.api

import com.secureops.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface CircleCIService {

    @GET("api/v2/project/{vcs-type}/{org}/{project}/pipeline")
    suspend fun getPipelines(
        @Path("vcs-type") vcsType: String = "github",
        @Path("org") org: String,
        @Path("project") project: String,
        @Query("page-token") pageToken: String? = null
    ): Response<CircleCIPipelinesResponse>

    @GET("api/v2/pipeline/{pipeline-id}")
    suspend fun getPipeline(
        @Path("pipeline-id") pipelineId: String
    ): Response<CircleCIPipeline>

    @GET("api/v2/pipeline/{pipeline-id}/workflow")
    suspend fun getWorkflows(
        @Path("pipeline-id") pipelineId: String
    ): Response<CircleCIWorkflowsResponse>

    @GET("api/v2/workflow/{workflow-id}/job")
    suspend fun getWorkflowJobs(
        @Path("workflow-id") workflowId: String
    ): Response<CircleCIJobsResponse>

    @POST("api/v2/workflow/{workflow-id}/rerun")
    suspend fun rerunWorkflow(
        @Path("workflow-id") workflowId: String
    ): Response<CircleCIRerunResponse>

    @POST("api/v2/workflow/{workflow-id}/cancel")
    suspend fun cancelWorkflow(
        @Path("workflow-id") workflowId: String
    ): Response<CircleCICancelResponse>
}
